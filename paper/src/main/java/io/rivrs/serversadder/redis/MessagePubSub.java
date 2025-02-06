package io.rivrs.serversadder.redis;

import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.slf4j.Logger;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.MessageType;
import io.rivrs.serversadder.commons.RedisChannel;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class MessagePubSub extends JedisPubSub {

    private final Logger logger;
    private final ServersAdder plugin;
    private final RedisManager redis;

    public MessagePubSub(ServersAdder plugin, RedisManager redis) {
        this.logger = plugin.getSLF4JLogger();
        this.plugin = plugin;
        this.redis = redis;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(RedisChannel.RESTART.getChannel())) {
            // Format: type:data (e.g. ADD:server)
            String[] split = message.split(":");
            if (split.length < 3) {
                this.logger.warn("Received invalid restart message: {}", message);
                return;
            }

            // Parse message type
            MessageType type = MessageType.valueOf(split[0].toUpperCase());

            // Parse message data
            String serverId = split[1];
            String reason = split[2];

            // Check if the message is for this server
            if (!this.plugin.getRedis().getServer().id().equals(serverId))
                return;

            switch (type) {
                case PRE_SHUTDOWN -> {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        Bukkit.setWhitelist(true);

                        AtomicInteger countdown = new AtomicInteger(30);
                        Bukkit.getScheduler().runTaskTimer(this.plugin, (task) -> {

                            // We're done
                            if (countdown.decrementAndGet() <= 0) {
                                this.redis.send(RedisChannel.RESTART, MessageType.PRE_SHUTDOWN_ACK, serverId);
                                task.cancel();
                                return;
                            }

                            // Announce to players
                            Bukkit.broadcast(Component.text("Le serveur va redémarrer dans %d secondes.".formatted(countdown.get()), NamedTextColor.RED, TextDecoration.BOLD));
                        }, 0, 20);

                    });
                }
                case SHUTDOWN -> {
                    this.redis.send(RedisChannel.RESTART, MessageType.SHUTDOWN_ACK, serverId);
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        Bukkit.setWhitelist(false);
                        Bukkit.shutdown();
                    });
                }
                default -> this.logger.warn("Received invalid restart message: {}", message);
            }


        } else {
            this.logger.warn("Received invalid channel: {}", message);
        }
    }
}
