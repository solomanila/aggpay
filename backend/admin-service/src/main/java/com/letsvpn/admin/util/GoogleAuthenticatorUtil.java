package com.letsvpn.admin.util;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base32;

/**
 * 简化版 Google Authenticator 工具，支持生成密钥和校验 TOTP。
 */
public final class GoogleAuthenticatorUtil {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final Base32 BASE32 = new Base32();

    private GoogleAuthenticatorUtil() {
    }

    public static String generateSecret() {
        byte[] buffer = new byte[20];
        try {
            SecureRandom.getInstanceStrong().nextBytes(buffer);
        } catch (NoSuchAlgorithmException e) {
            new SecureRandom().nextBytes(buffer);
        }
        return BASE32.encodeToString(buffer).replace("=", "");
    }

    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        long timeWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        try {
            for (int i = -1; i <= 1; i++) {
                String candidate = generateCode(secret, timeWindow + i);
                if (code.equals(candidate)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    public static String buildOtpAuthUrl(String account, String issuer, String secret) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                issuer, account, secret, issuer);
    }

    private static String generateCode(String secret, long timeWindow) throws Exception {
        byte[] key = BASE32.decode(secret.toUpperCase());
        byte[] data = ByteBuffer.allocate(8).putLong(timeWindow).array();
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(data);
        int offset = hash[hash.length - 1] & 0x0F;
        long truncatedHash = (hash[offset] & 0x7F) << 24
                | (hash[offset + 1] & 0xFF) << 16
                | (hash[offset + 2] & 0xFF) << 8
                | (hash[offset + 3] & 0xFF);
        long otp = truncatedHash % (int) Math.pow(10, CODE_DIGITS);
        return String.format("%0" + CODE_DIGITS + "d", otp);
    }
}
