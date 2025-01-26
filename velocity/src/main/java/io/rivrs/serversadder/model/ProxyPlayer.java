package io.rivrs.serversadder.model;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.velocitypowered.api.proxy.Player;

import io.rivrs.serversadder.ServersAdder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProxyPlayer {

    private final UUID uniqueId;
    private final String username;
    private final String proxy;
    private String server;

    public boolean isOnline(ServersAdder plugin) {
        return this.proxy.equals(plugin.getConfiguration().getIdentifier());
    }

    public @Nullable Player getPlayer(ServersAdder plugin) {
        return plugin.getServer().getPlayer(this.uniqueId).orElse(null);
    }

    public String toRedisString() {
        return "%s:%s:%s:%s".formatted(this.uniqueId, this.username, this.proxy, this.server);
    }

    public static ProxyPlayer fromRedisString(String redisString) {
        String[] split = redisString.split(":");
        if (split.length != 4)
            return null;

        return new ProxyPlayer(UUID.fromString(split[0]), split[1], split[2], split[3]);
    }


}
