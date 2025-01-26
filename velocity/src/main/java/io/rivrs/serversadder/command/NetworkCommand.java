package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("network")
@Description("Show network information")
@CommandPermission("serversadder.network")
public class NetworkCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    public void onDefault(CommandSource source) {
        source.sendMessage(Component.text("Network information:", NamedTextColor.AQUA));
        this.plugin.getRedis()
                .getGroups()
                .forEach((group, servers) -> {
                    int serversCount = servers.size();
                    int playersCount = servers.stream()
                            .mapToInt(server -> this.plugin.getRedis().getPlayersByServer(server.id()).size())
                            .sum();

                    source.sendMessage(Component.text(" • ", NamedTextColor.GRAY)
                            .append(Component.text(group, NamedTextColor.AQUA))
                            .append(Component.text(" - (", NamedTextColor.GRAY))
                            .append(Component.text(serversCount + " servers", NamedTextColor.AQUA))
                            .append(Component.text(", ", NamedTextColor.GRAY))
                            .append(Component.text(playersCount + " players", NamedTextColor.AQUA))
                            .append(Component.text(")", NamedTextColor.GRAY))
                    );
                });
    }

    @Subcommand("servers")
    @Description("Show servers information")
    @CommandPermission("serversadder.network.servers")
    public void onServers(CommandSource source) {
        source.sendMessage(Component.text("Servers information:", NamedTextColor.AQUA));
        this.plugin.getRedis()
                .pullFromCache()
                .forEach(server -> {
                    int playersCount = this.plugin.getRedis().getPlayersByServer(server.id()).size();
                    source.sendMessage(Component.text(" • ", NamedTextColor.GRAY)
                            .append(Component.text(server.id(), NamedTextColor.AQUA))
                            .append(Component.text(" - ", NamedTextColor.GRAY))
                            .append(Component.text(playersCount + " players", NamedTextColor.AQUA))
                    );
                });
    }
}
