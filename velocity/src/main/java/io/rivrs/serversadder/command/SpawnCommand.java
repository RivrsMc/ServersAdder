package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("spawn")
@Description("Teleport to the spawn location")
public class SpawnCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    public void onDefault(CommandSource source) {
        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
            return;
        }

        this.plugin.getRedis()
                .findEmptiestServerInGroup(this.plugin.getConfiguration().getSpawnGroup())
                .ifPresentOrElse(server -> {
                    player.sendMessage(this.plugin.getMessages().get("sending-to-spawn"));
                    player.createConnectionRequest(server)
                            .connect()
                            .whenCompleteAsync((result, throwable) -> {
                                if (throwable != null) {
                                    player.sendMessage(this.plugin.getMessages().get("error-sending-to-spawn"));
                                    throwable.printStackTrace();
                                    return;
                                }

                                player.sendMessage(this.plugin.getMessages().get("sent-to-spawn"));
                            });
                }, () -> player.sendMessage(this.plugin.getMessages().get("error-sending-to-spawn")));
    }

}
