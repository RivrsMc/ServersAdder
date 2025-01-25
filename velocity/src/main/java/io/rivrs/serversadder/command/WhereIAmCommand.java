package io.rivrs.serversadder.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@CommandAlias("whereiam")
@Description("Check where you are")
public class WhereIAmCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    public void onDefault(CommandSource source) {
        if (!(source instanceof Player player))
            return;

        Component component = plugin.getMessages().get("whereiam",
                Placeholder.unparsed(
                        "server", player.getCurrentServer()
                                .map(ServerConnection::getServerInfo)
                                .map(ServerInfo::getName)
                                .orElse("Unknown")
                ),
                Placeholder.unparsed(
                        "proxy",
                        this.plugin.getConfiguration().getName()
                )
        );

        player.sendMessage(component);
    }
}
