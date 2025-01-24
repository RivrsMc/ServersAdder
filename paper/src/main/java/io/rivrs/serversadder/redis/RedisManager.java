package io.rivrs.serversadder.redis;

import org.bukkit.configuration.ConfigurationSection;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.AbstractRedisManager;
import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.commons.MessageType;
import io.rivrs.serversadder.commons.RedisCredentials;
import redis.clients.jedis.Jedis;

public class RedisManager extends AbstractRedisManager {

    private final ServersAdder plugin;
    private GameServer server;

    public RedisManager(ServersAdder plugin) {
        super(plugin.getSLF4JLogger());
        this.plugin = plugin;
    }

    private void send(MessageType type, String message) {
        try (Jedis jedis = getResource()) {
            jedis.publish("serversadder", "%s:%s".formatted(type.name().toLowerCase(), message));
        }
    }

    public void registerServer() {
        this.send(MessageType.REGISTER, server.toRedisString());
    }

    public void sendKeepAlive() {
        this.send(MessageType.UPDATE, server.id());
    }

    public void unregisterServer() {
        this.send(MessageType.UNREGISTER, server.id());
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
        if (serverName == null || serverHost == null || serverPort == 0)
            throw new IllegalArgumentException("Server name or host or port not found in config");

        this.server = new GameServer(serverName, serverHost, serverPort);

        return new RedisCredentials(host, port, password);
    }

    @Override
    public void postLoad() {

    }
}
