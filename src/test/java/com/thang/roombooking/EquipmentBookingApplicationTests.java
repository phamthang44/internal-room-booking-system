package com.thang.roombooking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Integration test — requires live Postgres, Redis, and RabbitMQ. Run with 'make infra' first.")
class EquipmentBookingApplicationTests {

    @Test
    void contextLoads() {
    }

}
