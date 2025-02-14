package io.rivrs.serversadder.redis;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.*;
import lombok.Getter;
import redis.clients.jedis.Jedis;

public class RedisManager extends AbstractRedisManager {

    private final ServersAdder plugin;
    @Getter
    private GameServer server;

    public RedisManager(ServersAdder plugin) {
        super(plugin.getSLF4JLogger());
        this.plugin = plugin;
    }

    public void send(RedisChannel channel, MessageType type, String message) {
        try (Jedis jedis = getResource()) {
            jedis.publish(channel.getChannel(), "%s:%s".formatted(type.name().toLowerCase(), message));
        }
    }

    public void send(MessageType type, String message) {
        this.send(RedisChannel.SERVERS, type, message);
    }

    public void registerServer() {
        this.send(MessageType.REGISTER, server.toRedisString());
    }

    public void update() {
        this.send(MessageType.UPDATE, server.id());

        // Add to cache
        try (Jedis jedis = getResource()) {
            jedis.hset("serversadder:cache", server.id(), server.toRedisString());
            jedis.expire("serversadder:cache", 60);
        }
    }

    public void unregisterServer() {
        this.send(MessageType.UNREGISTER, server.id());

        // Remove from cache
        try (Jedis jedis = getResource()) {
            jedis.hdel("serversadder:cache", server.id());
        }
    }

    @Override
    public RedisCredentials loadCredentials() {
        this.plugin.saveDefaultConfig();

        // Load redis info
        ConfigurationSection redis = plugin.getConfig().getConfigurationSection("redis");
        if (redis == null)
            throw new IllegalArgumentException("Redis section not found in config");

        String host = redis.getString("host");
        int port = redis.getInt("port");
        String password = redis.getString("password");

        // Load server info
        ConfigurationSection server = plugin.getConfig().getConfigurationSection("server");
        if (server == null)
            throw new IllegalArgumentException("Server section not found in config");

        String serverName = server.getString("name");
        String serverHost = server.getString("host");
        int serverPort = server.getInt("port");
        String group = server.getString("group");
        if (serverName == null || serverHost == null || serverPort == 0 || group == null)
            throw new IllegalArgumentException("Server name or host or port or group not found in config");

        this.server = new GameServer(serverName, serverHost, serverPort, group);

        return new RedisCredentials(host, port, password);
    }

    @Override
    public void postLoad() {
        // Register pub/sub
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Jedis jedis = this.getResource()) {
                jedis.subscribe(new MessagePubSub(this.plugin, this), RedisChannel.RESTART.getChannel());
            } catch (Exception e) {
                this.plugin.getSLF4JLogger().error("Failed to register pub/sub", e);
            }
        });
    }
}
