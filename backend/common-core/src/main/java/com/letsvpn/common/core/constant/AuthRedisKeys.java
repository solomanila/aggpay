package com.letsvpn.common.core.constant;

public final class AuthRedisKeys {

    private AuthRedisKeys() {
    }

    public static final String ADMIN_SESSION_PREFIX = "auth:admin:session:";
    public static final String ADMIN_TOKEN_BLACKLIST_PREFIX = "auth:admin:black:";

    public static final String USER_SESSION_PREFIX = "auth:user:session:";
    public static final String USER_TOKEN_BLACKLIST_PREFIX = "auth:user:black:";
}
