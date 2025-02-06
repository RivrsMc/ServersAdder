package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@CommandAlias("alert")
@Description("Alerts all players on the network.")
@CommandPermission("serversadder.command.alert")
public class AlertCommand extends BaseCommand {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Dependency
    private ServersAdder plugin;

    @Default
    public void onDefault(CommandSource source, String message) {
        Component component = MINI_MESSAGE.deserialize(message);

        this.plugin.getServer().sendMessage(component);
    }
}
