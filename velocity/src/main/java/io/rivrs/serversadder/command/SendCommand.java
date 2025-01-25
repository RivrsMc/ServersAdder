package io.rivrs.serversadder.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import io.rivrs.serversadder.ServersAdder;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

@CommandAlias("send")
@Description("Send a player to a server")
@CommandPermission("serversadder.send")
public class SendCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Subcommand("all")
    @Description("Send all players to a server")
    @Syntax("<server>")
    @CommandCompletion("@servers")
    @CommandPermission("serversadder.send.all")
    public void onSendAll(CommandSource source, RegisteredServer server) {
        TagResolver serverPlaceholder = Placeholder.unparsed("server", server.getServerInfo().getName());

        CompletableFuture<ConnectionRequestBuilder.Result>[] futures = this.plugin.getServer()
                .getAllPlayers()
                .stream()
                .filter(player -> !player.getCurrentServer().map(s -> s.getServerInfo().equals(server.getServerInfo())).orElse(false))
                .map(player -> player.createConnectionRequest(server).connect())
                .toArray(CompletableFuture[]::new);

        source.sendMessage(this.plugin.getMessages().get(
                "send-all-players",
                serverPlaceholder,
                Placeholder.parsed("players", String.valueOf(futures.length))
        ));

        CompletableFuture.allOf(futures)
                .whenCompleteAsync((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        source.sendMessage(this.plugin.getMessages().get(
                                "send-all-players-fail",
                                serverPlaceholder
                        ));
                        return;
                    }

                    source.sendMessage(this.plugin.getMessages().get(
                            "send-all-players-success",
                            serverPlaceholder
                    ));
                });
    }

    @Subcommand("player")
    @Description("Send a player to a server")
    @Syntax("<player> <server>")
    @CommandCompletion("@players @servers")
    @CommandPermission("serversadder.send.player")
    public void onSendPlayer(CommandSource source, OnlinePlayer player, RegisteredServer server) {
        TagResolver playerPlaceholder = Placeholder.unparsed("player", player.getPlayer().getUsername());
        TagResolver serverPlaceholder = Placeholder.unparsed("server", server.getServerInfo().getName());

        // Check if the player is already on the server
        if (player.getPlayer()
                .getCurrentServer()
                .map(ServerConnection::getServerInfo)
                .map(s -> s.equals(server.getServerInfo()))
                .orElse(false)) {
            source.sendMessage(this.plugin.getMessages().get(
                    "player-already-on-server",
                    playerPlaceholder,
                    serverPlaceholder
            ));
            return;
        }

        source.sendMessage(this.plugin.getMessages().get(
                "sending-player",
                playerPlaceholder,
                serverPlaceholder
        ));

        // Send the player to the server
        player.getPlayer()
                .createConnectionRequest(server)
                .connect()
                .whenCompleteAsync((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        source.sendMessage(this.plugin.getMessages().get(
                                "player-sent-fail",
                                playerPlaceholder,
                                serverPlaceholder
                        ));
                        return;
                    }

                    source.sendMessage(this.plugin.getMessages().get(
                            "player-sent-success",
                            playerPlaceholder,
                            serverPlaceholder
                    ));
                });
    }

    @Subcommand("server")
    @Description("Send all players to a server")
    @Syntax("<source> <target>")
    @CommandCompletion("@servers @servers")
    @CommandPermission("serversadder.send.server")
    public void onSendServer(CommandSource source, RegisteredServer sourceServer, RegisteredServer targetServer) {
        TagResolver sourcePlaceholder = Placeholder.unparsed("source", sourceServer.getServerInfo().getName());
        TagResolver targetPlaceholder = Placeholder.unparsed("target", targetServer.getServerInfo().getName());

        CompletableFuture<ConnectionRequestBuilder.Result>[] futures = sourceServer.getPlayersConnected()
                .stream()
                .filter(player -> !player.getCurrentServer().map(s -> s.getServerInfo().equals(targetServer)).orElse(false))
                .map(player -> player.createConnectionRequest(targetServer).connect())
                .toArray(CompletableFuture[]::new);

        source.sendMessage(this.plugin.getMessages().get(
                "send-all-players-from-server",
                sourcePlaceholder,
                targetPlaceholder,
                Placeholder.parsed("players", String.valueOf(sourceServer.getPlayersConnected().size()))
        ));

        CompletableFuture.allOf(futures)
                .whenCompleteAsync((result, throwable) -> {
                    if (throwable != null) {
                        throwable.printStackTrace();
                        source.sendMessage(this.plugin.getMessages().get(
                                "send-all-players-from-server-fail",
                                sourcePlaceholder,
                                targetPlaceholder
                        ));
                        return;
                    }

                    source.sendMessage(this.plugin.getMessages().get(
                            "send-all-players-from-server-success",
                            sourcePlaceholder,
                            targetPlaceholder
                    ));
                });
    }


}
