package io.rivrs.serversadder.command;

import java.util.List;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandAlias("cleanrestart")
@Description("Restarts a server group flawlessly")
@CommandPermission("serversadder.command.cleanrestart")
public class CleanRestartCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    @CommandCompletion("@serverGroups @serverGroups @nothing")
    @Syntax("<source group> <target group> [reason]")
    public void onDefault(CommandSource source, String sourceGroup, String targetGroup, @Optional String reason) {
        List<String> groupNames = this.plugin.getRedis().getGroupNames();

        // Check if the source group exists
        if (!groupNames.contains(sourceGroup.toLowerCase())) {
            source.sendMessage(this.plugin.getMessages().get("group-not-found", Placeholder.parsed("group", sourceGroup)));
            return;
        }

        // Check if the target group exists
        if (!groupNames.contains(targetGroup.toLowerCase())) {
            source.sendMessage(this.plugin.getMessages().get("group-not-found", Placeholder.parsed("group", targetGroup)));
            return;
        }

        this.plugin.getRestartService().start(plugin, source, sourceGroup, targetGroup, reason);
    }

}
