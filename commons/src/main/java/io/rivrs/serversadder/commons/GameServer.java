package io.rivrs.serversadder.commons;

public record GameServer(String id, String host, int port, String group) {

    public String toRedisString() {
        return "%s:%s:%s:%s".formatted(id, host, port, group);
    }

    public static GameServer fromRedisString(String redisString) {
        String[] parts = redisString.split(":");
        if (parts.length != 4)
            return null;

        return new GameServer(parts[0], parts[1], Integer.parseInt(parts[2]), parts[3]);
    }
}
