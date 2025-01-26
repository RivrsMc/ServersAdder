package io.rivrs.serversadder.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RedisChannel {
    SERVERS("serversadder"),
    PROXIES("serversadder:proxies");

    private final String channel;
}
