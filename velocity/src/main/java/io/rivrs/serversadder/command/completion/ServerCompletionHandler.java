package io.rivrs.serversadder.command.completion;

import co.aikar.commands.CommandCompletions;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.VelocityCommandCompletionContext;
import io.rivrs.serversadder.ServersAdder;
import java.util.Collection;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerCompletionHandler implements CommandCompletions.AsyncCommandCompletionHandler<VelocityCommandCompletionContext> {

    private final ServersAdder plugin;

    @Override
    public Collection<String> getCompletions(VelocityCommandCompletionContext context) throws InvalidCommandArgument {
        return this.plugin.getServer()
                .getAllServers()
                .stream()
                .map(server -> server.getServerInfo().getName())
                .toList();
    }
}
