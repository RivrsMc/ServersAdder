package io.rivrs.serversadder.redis;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum RedisChannel {
    SERVERS("serversadder"),
    PROXIES("serversadder:proxies"),
    RESTART("serversadder:restart"),
    POKE_CORE("serversadder:poke-core");

    private final String channel;
}
