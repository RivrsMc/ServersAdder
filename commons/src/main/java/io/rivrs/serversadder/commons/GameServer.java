package io.rivrs.serversadder.commons;

public record GameServer(String id, String host, int port) {

    public String toRedisString() {
        return "%s:%s:%s".formatted(id, host, port);
    }

    public static GameServer fromString(String data) {
        String[] split = data.split(":");
        if (split.length != 3)
            throw new IllegalArgumentException("Unable to parse GameServer from string: " + data);

        return new GameServer(split[0], split[1], Integer.parseInt(split[2]));
    }
}
