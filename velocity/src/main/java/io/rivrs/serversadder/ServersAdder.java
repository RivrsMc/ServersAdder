package io.rivrs.serversadder;

import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import io.rivrs.serversadder.redis.RedisManager;
import io.rivrs.serversadder.server.ServerService;
import lombok.Getter;

@Plugin(
        id = "serversadder",
        name = "ServersAdder",
        version = "1.0-SNAPSHOT"
)
@Getter
public class ServersAdder {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private RedisManager redis;
    private ServerService service;

    @Inject
    public ServersAdder(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Service
        this.service = new ServerService(this);

        // Redis
        this.redis = new RedisManager(this);
        this.redis.load();

        // Start health check
        this.service.startHealthCheck();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Stop health check
        this.service.stopHealthCheck();

        // Redis
        this.redis.close();
    }
}
