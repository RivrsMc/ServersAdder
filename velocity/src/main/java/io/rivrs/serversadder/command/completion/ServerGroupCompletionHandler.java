package io.rivrs.serversadder.command.completion;

import java.util.Collection;

import co.aikar.commands.CommandCompletions;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.VelocityCommandCompletionContext;
import io.rivrs.serversadder.ServersAdder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerGroupCompletionHandler implements CommandCompletions.AsyncCommandCompletionHandler<VelocityCommandCompletionContext> {

    private final ServersAdder plugin;

    @Override
    public Collection<String> getCompletions(VelocityCommandCompletionContext context) throws InvalidCommandArgument {
        return this.plugin.getRedis().getGroupNames();
    }
}
