package io.rivrs.serversadder.commons;

import org.slf4j.Logger;

import lombok.RequiredArgsConstructor;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@RequiredArgsConstructor
public abstract class AbstractRedisManager {

    private final Logger logger;
    private JedisPool jedisPool;

    public void load() {
        RedisCredentials credentials = loadCredentials();
        if (credentials == null) {
            logger.error("Failed to load Redis credentials");
            return;
        }

        logger.info("Connecting to Redis...");
        try {
            if (credentials.password() != null)
                jedisPool = new JedisPool(new HostAndPort(credentials.host(), credentials.port()), DefaultJedisClientConfig.builder()
                        .password(credentials.password())
                        .build());
            else
                jedisPool = new JedisPool(credentials.host(), credentials.port());


            // Check if the connection is successful
            try (Jedis jedis = jedisPool.getResource()) {
                if (jedis.ping() == null)
                    throw new Exception("Unable to get a response from Redis");
            }

            postLoad();
            logger.info("Connected to Redis");
        } catch (Exception e) {
            logger.error("Failed to connect to Redis", e);
        }
    }

    public abstract RedisCredentials loadCredentials();

    public abstract void postLoad();

    public boolean isConnected() {
        return jedisPool != null;
    }

    public Jedis getResource() {
        return jedisPool.getResource();
    }

    public void close() {
        if (jedisPool == null)
            return;
        jedisPool.close();
        jedisPool = null;
    }
}
