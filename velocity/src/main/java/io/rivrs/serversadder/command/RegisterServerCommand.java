package io.rivrs.serversadder.command;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.GameServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("registerserver")
@Description("Registers a server to the network")
@CommandPermission("serversadder.registerserver")
public class RegisterServerCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    @Syntax("<id> <host> <port> <group>")
    @CommandCompletion("@nothing @nothing @nothing @nothing")
    public void onRegister(CommandSource source, String id, String host, int port, String group) {
        if (this.plugin.getService().isRegistered(id)) {
            source.sendMessage(Component.text(
                    "Server " + id + " tried to register but it's already registered!",
                    NamedTextColor.RED
            ));
            return;
        }

        GameServer server = new GameServer(id, host, port, group);
        this.plugin.getService().register(server);

        source.sendMessage(Component.text("Server " + id + " registered!", NamedTextColor.GREEN));
    }
}
