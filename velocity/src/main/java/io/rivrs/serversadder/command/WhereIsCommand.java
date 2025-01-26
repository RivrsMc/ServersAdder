package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.model.ProxyPlayer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandAlias("whereis")
@Description("Find a player's location")
@CommandPermission("serversadder.whereis")
public class WhereIsCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    @Syntax("<player>")
    @CommandCompletion("@proxyPlayers")
    public void onDefault(CommandSource source, ProxyPlayer player) {
        source.sendMessage(this.plugin.getMessages().get(
                "whereis",
                Placeholder.parsed("player", player.getUsername()),
                Placeholder.parsed("server", player.getServer() == null ? "Unknown" : player.getServer()),
                Placeholder.parsed("proxy", player.getProxy())
        ));
    }
}
