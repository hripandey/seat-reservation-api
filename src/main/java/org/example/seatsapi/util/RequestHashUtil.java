package org.example.seatsapi.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class RequestHashUtil {
    private RequestHashUtil() {
    }

    public static String hashSeats(List<String> seats) {
        String canonicalRequest = seats.stream()
                .sorted()
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            StringBuilder res = new StringBuilder();
            for (byte b : hash) {
                res.append(String.format("%02x", b));
            }
            return res.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
