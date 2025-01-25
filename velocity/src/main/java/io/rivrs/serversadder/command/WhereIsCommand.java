package io.rivrs.serversadder.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandAlias("whereis")
@Description("Find a player's location")
@CommandPermission("serversadder.whereis")
public class WhereIsCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onDefault(CommandSource source, OnlinePlayer player) {
        source.sendMessage(this.plugin.getMessages().get(
                "whereis",
                Placeholder.parsed("player", player.getPlayer().getUsername()),
                Placeholder.parsed("server", player.getPlayer().getCurrentServer().map(s -> s.getServerInfo().getName()).orElse("unknown")),
                Placeholder.parsed("proxy", this.plugin.getConfiguration().getName())
        ));
    }
}
