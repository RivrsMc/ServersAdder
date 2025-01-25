package io.rivrs.serversadder;

import co.aikar.commands.VelocityCommandManager;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.rivrs.serversadder.command.SendCommand;
import io.rivrs.serversadder.command.WhereIAmCommand;
import io.rivrs.serversadder.command.completion.ServerCompletionHandler;
import io.rivrs.serversadder.command.context.ServerContextResolver;
import io.rivrs.serversadder.configuration.Configuration;
import io.rivrs.serversadder.configuration.MessageConfiguration;
import io.rivrs.serversadder.redis.RedisManager;
import io.rivrs.serversadder.server.ServerService;
import java.nio.file.Path;
import lombok.Getter;
import org.slf4j.Logger;

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

        // Commands
        this.commands = new VelocityCommandManager(this.server, this);
        this.commands.getCommandCompletions().registerAsyncCompletion("servers", new ServerCompletionHandler(this));
        this.commands.getCommandContexts().registerContext(RegisteredServer.class, new ServerContextResolver(this));
        this.commands.registerCommand(new WhereIAmCommand());
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
