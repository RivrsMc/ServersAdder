package io.rivrs.serversadder.commons;

public record GameServer(String id, String host, int port) {

    public String toRedisString() {
        return "%s:%s:%s".formatted(id, host, port);
    }
}
