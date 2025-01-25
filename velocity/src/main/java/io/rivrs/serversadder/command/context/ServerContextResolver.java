package io.rivrs.serversadder.command.context;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.VelocityCommandExecutionContext;
import co.aikar.commands.contexts.ContextResolver;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.rivrs.serversadder.ServersAdder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerContextResolver implements ContextResolver<RegisteredServer, VelocityCommandExecutionContext> {

    private final ServersAdder plugin;

    @Override
    public RegisteredServer getContext(VelocityCommandExecutionContext context) throws InvalidCommandArgument {
        String name = context.popFirstArg();

        return plugin.getServer()
                .getServer(name)
                .orElseThrow(() -> new InvalidCommandArgument("Server not found"));
    }
}
