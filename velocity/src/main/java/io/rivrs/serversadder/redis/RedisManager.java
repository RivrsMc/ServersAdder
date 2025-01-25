package io.rivrs.serversadder.redis;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.AbstractRedisManager;
import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.commons.RedisCredentials;
import java.util.List;
import java.util.Optional;
import redis.clients.jedis.Jedis;

public class RedisManager extends AbstractRedisManager {

    private final ServersAdder plugin;
    private Thread thread;

    public RedisManager(ServersAdder plugin) {
        super(plugin.getLogger());
        this.plugin = plugin;
    }

    @Override
    public RedisCredentials loadCredentials() {
        return this.plugin.getConfiguration().getRedisCredentials();
    }

    @Override
    public void postLoad() {
        this.plugin.getLogger().info("Registering pub/sub...");

        // Launch pub/sub thread
        this.thread = new Thread(this::registerPubSub);
        this.thread.setName("ServersAdder");
        this.thread.setDaemon(true);
        this.thread.start();

        // Pull data from cache
        this.plugin.getLogger().info("Registering servers from cache...");
        for (GameServer gameServer : this.pullFromCache()) {
            this.plugin.getService().register(gameServer);
        }
    }

    private void registerPubSub() {
        try (Jedis jedis = this.getResource()) {
            jedis.subscribe(new MessagePubSub(this.plugin.getLogger(), this.plugin.getService()), "serversadder");
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to register pub/sub", e);
        }
    }

    public List<GameServer> pullFromCache() {
        try (Jedis jedis = this.getResource()) {
            return jedis.hgetAll("serversadder:cache")
                    .values()
                    .stream()
                    .map(GameServer::fromRedisString)
                    .toList();
        }
    }

    public Optional<GameServer> pullFromCache(String id) {
        try (Jedis jedis = this.getResource()) {
            String data = jedis.hget("serversadder:cache", id);
            if (data == null || data.isEmpty())
                return Optional.empty();

            return Optional.of(GameServer.fromRedisString(data));
        }
    }

    public void invalidateCache(String id) {
        try (Jedis jedis = this.getResource()) {
            jedis.hdel("serversadder:cache", id);
        }
    }

    @Override
    public void close() {
        // Stop thread
        if (this.thread != null)
            this.thread.interrupt();

        super.close();
    }
}
