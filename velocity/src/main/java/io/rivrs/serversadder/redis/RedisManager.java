package io.rivrs.serversadder.redis;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.*;
import io.rivrs.serversadder.model.ProxyActionType;
import io.rivrs.serversadder.model.ProxyPlayer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

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
        this.plugin.getLogger().info("Registered {} servers from cache", this.plugin.getServer().getAllServers().size());

        // Invalidate players cache
        this.invalidatePlayersCache();
    }

    private void registerPubSub() {
        try (Jedis jedis = this.getResource()) {
            jedis.subscribe(new MessagePubSub(this.plugin), RedisChannel.SERVERS.getChannel(), RedisChannel.PROXIES.getChannel(), RedisChannel.POKE_CORE.getChannel(), RedisChannel.RESTART.getChannel());
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to register pub/sub", e);
        }
    }

    public List<GameServer> getServersByGroup(String group) {
        return this.plugin.getRedis()
                .pullFromCache()
                .stream()
                .filter(server -> server.group().equals(group))
                .toList();
    }

    public List<String> getGroupNames() {
        return this.plugin.getRedis()
                .pullFromCache()
                .stream()
                .map(GameServer::group)
                .distinct()
                .toList();
    }

    public Map<String, List<GameServer>> getGroups() {
        return this.plugin.getRedis()
                .pullFromCache()
                .stream()
                .collect(Collectors.groupingBy(GameServer::group));
    }

    public List<GameServer> pullFromCache() {
        try (Jedis jedis = this.getResource()) {
            return jedis.hgetAll("serversadder:cache")
                    .values()
                    .stream()
                    .map(GameServer::fromRedisString)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to pull servers from cache", e);
            return List.of();
        }
    }

    public void invalidate(String id) {
        try (Jedis jedis = this.getResource()) {
            jedis.hdel("serversadder:cache", id);
        }
    }

    public void registerPlayer(ProxyPlayer player) {
        try (Jedis jedis = this.getResource()) {
            jedis.hset("serversadder:players", player.getUniqueId().toString(), player.toRedisString());
            jedis.hset("serversadder:player_names", player.getUsername(), player.getUniqueId().toString());
        }
    }

    public List<ProxyPlayer> getPlayersByServer(String server) {
        try (Jedis jedis = this.getResource()) {
            return jedis.hgetAll("serversadder:players")
                    .values()
                    .stream()
                    .map(ProxyPlayer::fromRedisString)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(player -> server.equals(player.getServer()))
                    .toList();
        }
    }

    public void send(RedisChannel channel, MessageType type, String... data) {
        try (Jedis jedis = this.getResource()) {
            jedis.publish(channel.getChannel(), "%s:%s".formatted(type, String.join(":", data)));
        }
    }

    public void sendProxyAction(ProxyActionType type, String... data) {
        try (Jedis jedis = this.getResource()) {
            jedis.publish(RedisChannel.PROXIES.getChannel(), "%s:%s".formatted(type, String.join(":", data)));
        }
    }

    public Optional<ProxyPlayer> getPlayerByUniqueId(UUID uniqueId) {
        try (Jedis jedis = this.getResource()) {
            String redisString = jedis.hget("serversadder:players", uniqueId.toString());
            return Optional.ofNullable(redisString)
                    .flatMap(ProxyPlayer::fromRedisString);
        }
    }

    public Optional<ProxyPlayer> getPlayerByUsername(String username) {
        try (Jedis jedis = this.getResource()) {
            String uniqueId = jedis.hget("serversadder:player_names", username);
            if (uniqueId == null)
                return Optional.empty();

            String redisString = jedis.hget("serversadder:players", uniqueId);
            return Optional.ofNullable(redisString)
                    .flatMap(ProxyPlayer::fromRedisString);
        }
    }

    public Optional<RegisteredServer> findEmptiestServerInGroup(String group) {
        return this.getServersByGroup(group)
                .stream()
                .filter(server -> !ServersAdder.get().getRestartService().isRestarting(server.id()))
                .min(Comparator.comparingInt(server -> getPlayersByServer(server.id()).size()))
                .flatMap(server -> this.plugin.getServer().getServer(server.id()));
    }

    public List<ProxyPlayer> getPlayersByProxy(String id) {
        try (Jedis jedis = this.getResource()) {
            return jedis.hgetAll("serversadder:players")
                    .values()
                    .stream()
                    .map(ProxyPlayer::fromRedisString)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(player -> id.equals(player.getProxy()))
                    .toList();
        }
    }

    public Set<String> getPlayerNames() {
        try (Jedis jedis = this.getResource()) {
            return jedis.hkeys("serversadder:player_names");
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to get player names", e);
            return Set.of();
        }
    }

    public void editPlayer(UUID uniqueId, Consumer<ProxyPlayer> player) {
        try (Jedis jedis = this.getResource()) {
            String redisString = jedis.hget("serversadder:players", uniqueId.toString());
            if (redisString == null)
                return;

            ProxyPlayer proxyPlayer = ProxyPlayer.fromRedisString(redisString).orElse(null);
            if (proxyPlayer == null)
                return;

            player.accept(proxyPlayer);

            jedis.hset("serversadder:players", uniqueId.toString(), proxyPlayer.toRedisString());
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to edit player", e);
        }
    }

    public void unregisterPlayer(Player player) {
        try (Jedis jedis = this.getResource()) {
            jedis.hdel("serversadder:players", player.getUniqueId().toString());
            jedis.hdel("serversadder:player_names", player.getUsername());
        } catch (Exception e) {
            this.plugin.getLogger().error("Failed to unregister player", e);
        }
    }


    public void invalidatePlayersCache() {
        try (Jedis jedis = this.getResource()) {
            Transaction transaction = jedis.multi();

            this.getPlayersByProxy(this.plugin.getConfiguration().getIdentifier())
                    .forEach(player -> {
                        transaction.hdel("serversadder:players", player.getUniqueId().toString());
                        transaction.hdel("serversadder:player_names", player.getUsername());
                    });

            transaction.exec();
        }
    }

    @Override
    public void close() {
        // Expire all players from this proxy
        this.invalidatePlayersCache();

        // Stop thread
        if (this.thread != null)
            this.thread.interrupt();

        // Stop redis
        super.close();
    }
}
