package io.rivrs.serversadder.redis;

import org.slf4j.Logger;

import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.commons.MessageType;
import io.rivrs.serversadder.server.ServerService;
import lombok.RequiredArgsConstructor;
import redis.clients.jedis.JedisPubSub;

@RequiredArgsConstructor
public class MessagePubSub extends JedisPubSub {

    private final Logger logger;
    private final ServerService service;

    @Override
    public void onMessage(String channel, String message) {
        if (!channel.equals("serversadder"))
            return;

        // Format: type:data (e.g. ADD:server)
        String[] split = message.split(":");
        if (split.length < 2) {
            this.logger.warn("Received invalid message: {}", message);
            return;
        }

        // Parse message type
        MessageType type = MessageType.valueOf(split[0].toUpperCase());

        switch (type) {
            case REGISTER -> {
                if (split.length != 4) {
                    this.logger.warn("Received invalid server registration message: {}", message);
                    return;
                }

                String id = split[1];
                String host = split[2];
                int port = Integer.parseInt(split[3]);
                if (id == null || host == null || port == 0) {
                    this.logger.warn("Unable to parse server registration message: {}", message);
                    return;
                }

                this.service.register(new GameServer(id, host, port));
            }
            case UPDATE -> this.service.update(split[1]);
            case UNREGISTER -> this.service.unregister(split[1]);
            default -> this.logger.warn("Received unknown message type: {} | {}", type, message);
        }
    }
}
