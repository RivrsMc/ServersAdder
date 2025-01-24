package io.rivrs.serversadder.commons;

public record GameServer(String id, String host, int port) {

    public String toRedisString() {
        return "%s:%s:%s".formatted(id, host, port);
    }

    public static GameServer fromRedisString(String redisString) {
        String[] parts = redisString.split(":");
        if (parts.length != 3)
            throw new IllegalArgumentException("Invalid redis string: " + redisString);

        return new GameServer(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}
