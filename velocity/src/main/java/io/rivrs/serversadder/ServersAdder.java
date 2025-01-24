package io.rivrs.serversadder;

import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import co.aikar.commands.VelocityCommandManager;
import io.rivrs.serversadder.commands.MaintenanceCommand;
import io.rivrs.serversadder.maintenance.MaintenanceConfiguration;
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
    private MaintenanceConfiguration maintenance;
    private VelocityCommandManager commands;

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

        // Maintenance
        this.maintenance = new MaintenanceConfiguration(this);
        this.maintenance.load();

        // Commands
        this.commands = new VelocityCommandManager(this.server, this);
        this.commands.registerCommand(new MaintenanceCommand());

        // Start health check
        this.service.startHealthCheck();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Stop health check
        this.service.stopHealthCheck();

        // Commands
        this.commands.unregisterCommands();

        // Maintenance
        try {
            this.maintenance.save();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save maintenance configuration", e);
        }

        // Redis
        this.redis.close();
    }

    @Subscribe
    public void onLogin(PreLoginEvent e) {
        if (this.maintenance.isEnabled() && !this.maintenance.isAllowed(e.getUsername())) {
            e.setResult(PreLoginEvent.PreLoginComponentResult.denied(this.maintenance.getKickMessage()));
        }
    }
}
