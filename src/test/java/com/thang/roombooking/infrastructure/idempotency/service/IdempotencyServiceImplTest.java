package com.thang.roombooking.infrastructure.idempotency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.roombooking.common.exception.AppException;
import com.thang.roombooking.infrastructure.i18n.Translator;
import com.thang.roombooking.infrastructure.idempotency.dto.IdempotencyResponseDTO;
import com.thang.roombooking.infrastructure.idempotency.entity.IdempotencyKey;
import com.thang.roombooking.infrastructure.idempotency.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceImplTest {

    @Mock private IdempotencyRepository repository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotencyServiceImpl service;

    @BeforeAll
    static void setupTranslator() {
        // I18nUtils.get() is called as an argument to AppException in this service.
        // Without a Spring context, Translator.messageSource is null → NPE before AppException is
        // constructed. Initializing with useCodeAsDefaultMessage=true returns the key string,
        // which is sufficient to create the AppException and assert its errorCode in tests.
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        ms.setUseCodeAsDefaultMessage(true);
        new Translator(ms);
    }

    private static final String KEY = "test-idempotency-key-abc123";
    private static final String PATH = "/api/v1/bookings";

    // ─── validate: no existing record ────────────────────────────────────────

    @Test
    void should_save_new_processing_record_when_key_not_found() {
        when(repository.findByKeyHash(KEY)).thenReturn(Optional.empty());

        Optional<IdempotencyResponseDTO> result = service.validate(KEY, PATH, "body");

        assertThat(result).isEmpty();
        verify(repository).save(argThat(entity ->
                entity.getKeyHash().equals(KEY) &&
                entity.getResourcePath().equals(PATH)
        ));
    }

    // ─── validate: expired record recycled ───────────────────────────────────

    @Test
    void should_recycle_expired_processing_record_and_create_new_one() {
        IdempotencyKey expired = IdempotencyKey.builder()
                .keyHash(KEY)
                .resourcePath(PATH)
                .requestFingerprint("empty_fingerprint")
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(expired));

        Optional<IdempotencyResponseDTO> result = service.validate(KEY, PATH, "body");

        assertThat(result).isEmpty();
        verify(repository).delete(expired);
        verify(repository).save(any(IdempotencyKey.class));
    }

    // ─── validate: path mismatch ──────────────────────────────────────────────

    @Test
    void should_throw_AppException_when_key_used_with_different_path() {
        IdempotencyKey existing = IdempotencyKey.builder()
                .keyHash(KEY)
                .resourcePath("/api/v1/other-endpoint")
                .requestFingerprint("empty_fingerprint")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(existing));

        assertThrows(AppException.class, () -> service.validate(KEY, PATH, "body"));
    }

    // ─── validate: fingerprint mismatch ──────────────────────────────────────

    @Test
    void should_throw_AppException_when_key_used_with_different_request_body() {
        IdempotencyKey existing = IdempotencyKey.builder()
                .keyHash(KEY)
                .resourcePath(PATH)
                .requestFingerprint("completely_different_fingerprint")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(existing));

        assertThrows(AppException.class, () -> service.validate(KEY, PATH, "body"));
    }

    // ─── validate: request still processing ──────────────────────────────────

    @Test
    void should_throw_AppException_when_response_not_yet_saved() {
        IdempotencyKey processing = IdempotencyKey.builder()
                .keyHash(KEY)
                .resourcePath(PATH)
                .requestFingerprint("empty_fingerprint")
                .responseBody(null)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(processing));

        assertThrows(AppException.class, () -> service.validate(KEY, PATH, "body"));
    }

    // ─── validate: cached response returned ──────────────────────────────────

    @Test
    void should_return_cached_response_when_key_already_processed() {
        IdempotencyKey done = IdempotencyKey.builder()
                .keyHash(KEY)
                .resourcePath(PATH)
                .requestFingerprint("empty_fingerprint")
                .responseCode(200)
                .responseBody("{\"id\":1}")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(done));

        Optional<IdempotencyResponseDTO> result = service.validate(KEY, PATH, "body");

        assertThat(result).isPresent();
        assertThat(result.get().statusCode()).isEqualTo(200);
        assertThat(result.get().body()).isEqualTo("{\"id\":1}");
    }

    // ─── saveResponse ─────────────────────────────────────────────────────────

    @Test
    void should_update_entity_with_status_code_and_body_when_saveResponse_called() throws Exception {
        IdempotencyKey processing = IdempotencyKey.builder()
                .keyHash(KEY).resourcePath(PATH).requestFingerprint("empty_fingerprint")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        when(repository.findByKeyHash(KEY)).thenReturn(Optional.of(processing));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"result\":\"ok\"}");

        service.saveResponse(KEY, 200, new Object());

        verify(repository).save(argThat(entity ->
                entity.getResponseCode() == 200 &&
                "{\"result\":\"ok\"}".equals(entity.getResponseBody())
        ));
    }

    @Test
    void should_not_save_when_key_not_found_in_saveResponse() {
        when(repository.findByKeyHash(KEY)).thenReturn(Optional.empty());

        service.saveResponse(KEY, 200, new Object());

        verify(repository, never()).save(any());
    }

    // ─── deleteKey ────────────────────────────────────────────────────────────

    @Test
    void should_call_deleteByKeyHash_when_deleteKey_is_invoked() {
        service.deleteKey(KEY);

        verify(repository).deleteByKeyHash(KEY);
    }
}
