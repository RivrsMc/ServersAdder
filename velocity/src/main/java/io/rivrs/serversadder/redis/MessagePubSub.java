package io.rivrs.serversadder.redis;

import java.util.UUID;

import org.slf4j.Logger;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.commons.MessageType;
import io.rivrs.serversadder.model.ProxyActionType;
import io.rivrs.serversadder.server.ServerService;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class MessagePubSub extends JedisPubSub {

    private final Logger logger;
    private final ServersAdder plugin;
    private final ServerService service;

    public MessagePubSub(ServersAdder plugin) {
        this.logger = plugin.getLogger();
        this.service = plugin.getService();
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(RedisChannel.SERVERS.getChannel())) {
            // Format: type:data (e.g. ADD:server)
            String[] split = message.split(":");
            if (split.length < 2) {
                this.logger.warn("Received invalid servers message: {}", message);
                return;
            }

            // Parse message type
            MessageType type = MessageType.valueOf(split[0].toUpperCase());

            switch (type) {
                case REGISTER -> {
                    if (split.length != 5) {
                        this.logger.warn("Received invalid server registration message: {}", message);
                        return;
                    }

                    String id = split[1];
                    String host = split[2];
                    int port = Integer.parseInt(split[3]);
                    String group = split[4];
                    if (id == null
                        || host == null
                        || port == 0
                        || group == null) {
                        this.logger.warn("Unable to parse server registration message: {}", message);
                        return;
                    }

                    this.service.register(new GameServer(id, host, port, group));
                }
                case UPDATE -> this.service.update(split[1]);
                case UNREGISTER -> this.service.unregister(split[1]);
                default -> this.logger.warn("Received unknown message type: {} | {}", type, message);
            }
        } else if (channel.equals(RedisChannel.PROXIES.getChannel())) {
            String[] split = message.split(":");
            if (split.length < 2) {
                this.logger.warn("Received invalid proxies message: {}", message);
                return;
            }


            // Parse action type
            ProxyActionType type = ProxyActionType.valueOf(split[0].toUpperCase());

            switch (type) {
                case SEND_ALL -> {
                    String serverId = split[1];
                    this.logger.info("Sending all players to server: {}", serverId);

                    this.plugin.getServer()
                            .getServer(serverId)
                            .ifPresentOrElse(server -> this.plugin.getServer()
                                    .getAllPlayers()
                                    .stream()
                                    .filter(player -> !player.getCurrentServer().map(s -> s.getServerInfo().getName().equals(serverId)).orElse(false))
                                    .forEach(player -> player.createConnectionRequest(server).connect()), () -> this.logger.warn("Server {} not found", serverId));
                }
                case SEND_PLAYER -> {
                    String playerId = split[1];
                    String serverId = split[2];
                    this.logger.info("Sending player {} to server: {}", playerId, serverId);

                    // Find player
                    Player player = this.plugin.getServer()
                            .getPlayer(UUID.fromString(playerId))
                            .orElse(null);
                    if (player == null) {
                        this.logger.warn("Player {} not found", playerId);
                        return;
                    }

                    // Find server
                    this.plugin.getServer()
                            .getServer(serverId)
                            .ifPresentOrElse(server -> player.createConnectionRequest(server).connect(), () -> this.logger.warn("Server {} not found", serverId));
                }
                case SEND_SERVER -> {
                    String sourceId = split[1];
                    String targetId = split[2];
                    this.logger.info("Sending players from {} to server: {}", sourceId, targetId);

                    // Find source server
                    RegisteredServer source = this.plugin.getServer()
                            .getServer(sourceId)
                            .orElse(null);
                    if (source == null) {
                        this.logger.warn("Source server {} not found", sourceId);
                        return;
                    }

                    // Find target server
                    RegisteredServer target = this.plugin.getServer()
                            .getServer(targetId)
                            .orElse(null);
                    if (target == null) {
                        this.logger.warn("Target server {} not found", targetId);
                        return;
                    }

                    // Send players
                    this.plugin.getServer()
                            .getAllPlayers()
                            .stream()
                            .filter(player -> player.getCurrentServer().map(s -> s.getServerInfo().equals(source.getServerInfo())).orElse(false))
                            .forEach(player -> player.createConnectionRequest(target).connect());
                }
            }
        } else if (channel.equals(RedisChannel.POKE_CORE.getChannel())) {
            String[] split = message.split(":");
            if (split.length < 2) {
                this.logger.warn("Received invalid core message: {}", message);
                return;
            }

            UUID playerId = UUID.fromString(split[0]);
            String serverId = split[1];

            this.logger.debug("PokeCore request received for player {} to server {}", playerId, serverId);
            this.plugin.getServer()
                    .getPlayer(playerId)
                    .ifPresentOrElse(player -> this.plugin.getServer().getServer(serverId)
                                    .ifPresentOrElse(server -> player.createConnectionRequest(server).fireAndForget(),
                                            () -> this.logger.warn("[POKECORE] Server {} not found", serverId)),
                            () -> this.logger.warn("[POKECORE] Player {} not found", playerId));
        } else {
            this.logger.warn("Received invalid channel: {}", message);
        }
    }
}
