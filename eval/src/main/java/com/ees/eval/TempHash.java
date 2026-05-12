package com.ees.eval;

public class TempHash {
    public static void main(String[] args) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = 
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        
        String raw = "1234";
        String hash = encoder.encode(raw);
        System.out.println("=== HASH RESULT ===");
        System.out.println("Password: " + raw);
        System.out.println("Hash: " + hash);
        System.out.println("Verify: " + encoder.matches(raw, hash));
        
        // Also verify the hash we've been using
        String oldHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgdtS7N6p2S5MaH2R.LyOn9S8vOy";
        System.out.println("Old hash matches '1234': " + encoder.matches(raw, oldHash));
    }
}
