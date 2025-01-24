package io.rivrs.serversadder.commons;

public record RedisCredentials(String host, int port, String password) {

    public RedisCredentials {
        if (host == null)
            throw new IllegalArgumentException("Host cannot be null");
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("Port must be between 0 and 65535");
    }

}
