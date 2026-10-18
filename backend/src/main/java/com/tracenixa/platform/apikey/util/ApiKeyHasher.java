package com.tracenixa.platform.apikey.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class ApiKeyHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PREFIX = "trx_live_";

    private ApiKeyHasher() {}

    public static String generatePlaintextKey() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return PREFIX + HexFormat.of().formatHex(bytes);
    }

    public static String extractPrefix(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.length() < 16) {
            return "trx_live_...";
        }
        return plaintextKey.substring(0, 16) + "...";
    }

    public static String hashKey(String plaintextKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plaintextKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
