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
import com.velocitypowered.api.proxy.server.RegisteredServer;

import co.aikar.commands.VelocityCommandManager;
import io.rivrs.serversadder.command.SendCommand;
import io.rivrs.serversadder.command.WhereAmICommand;
import io.rivrs.serversadder.command.WhereIsCommand;
import io.rivrs.serversadder.command.completion.ProxyPlayerCompletionHandler;
import io.rivrs.serversadder.command.completion.ServerCompletionHandler;
import io.rivrs.serversadder.command.context.ProxyPlayerContextResolver;
import io.rivrs.serversadder.command.context.ServerContextResolver;
import io.rivrs.serversadder.configuration.Configuration;
import io.rivrs.serversadder.configuration.MessageConfiguration;
import io.rivrs.serversadder.listener.PlayerListener;
import io.rivrs.serversadder.model.ProxyPlayer;
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

    private Configuration configuration;
    private MessageConfiguration messages;
    private RedisManager redis;
    private ServerService service;
    private VelocityCommandManager commands;

    @Inject
    public ServersAdder(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Configuration
        this.configuration = new Configuration(this);
        this.messages = new MessageConfiguration(this);
        this.configuration.load();
        this.messages.load();

        // Service
        this.service = new ServerService(this);

        // Redis
        this.redis = new RedisManager(this);
        this.redis.load();

        // Start health check
        this.service.startHealthCheck();

        // Listeners
        server.getEventManager().register(this, new PlayerListener(this));

        // Commands
        this.commands = new VelocityCommandManager(this.server, this);
        this.commands.getCommandCompletions().registerAsyncCompletion("servers", new ServerCompletionHandler(this));
        this.commands.getCommandCompletions().registerAsyncCompletion("proxyPlayers", new ProxyPlayerCompletionHandler(this));
        this.commands.getCommandContexts().registerContext(RegisteredServer.class, new ServerContextResolver(this));
        this.commands.getCommandContexts().registerContext(ProxyPlayer.class, new ProxyPlayerContextResolver(this));
        this.commands.registerCommand(new WhereAmICommand());
        this.commands.registerCommand(new WhereIsCommand());
        this.commands.registerCommand(new SendCommand());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        // Stop health check
        this.service.stopHealthCheck();

        // Redis
        this.redis.close();

        // Commands
        this.commands.unregisterCommands();
    }
}
