package com.thang.roombooking.infrastructure.storage.supabase;

public interface SupabaseClient {
    String upload(byte[] bytes, String path, String contentType);
    void delete(String path);
}
