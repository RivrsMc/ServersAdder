package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("unregisterserver")
@Description("Unregisters a server from the network")
@CommandPermission("serversadder.unregisterserver")
public class UnregisterServerCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    @Syntax("<id>")
    @CommandCompletion("@servers")
    public void onUnregister(CommandSource source, String id) {
        if (!this.plugin.getService().isRegistered(id)) {
            source.sendMessage(Component.text(
                    "Server " + id + " tried to unregister but it's not registered!",
                    NamedTextColor.RED
            ));
            return;
        }

        this.plugin.getRedis().invalidate(id);
        this.plugin.getService().unregister(id);
        source.sendMessage(Component.text("Server " + id + " unregistered!", NamedTextColor.GREEN));
    }
}
