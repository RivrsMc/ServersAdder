package io.rivrs.serversadder.command.context;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.VelocityCommandExecutionContext;
import co.aikar.commands.contexts.ContextResolver;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.model.ProxyPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProxyPlayerContextResolver implements ContextResolver<ProxyPlayer, VelocityCommandExecutionContext> {

    private final ServersAdder plugin;

    @Override
    public ProxyPlayer getContext(VelocityCommandExecutionContext context) throws InvalidCommandArgument {
        String name = context.popFirstArg();

        return plugin.getRedis()
                .getPlayerByUsername(name)
                .orElseThrow(() -> new InvalidCommandArgument("Player not found"));
    }
}
