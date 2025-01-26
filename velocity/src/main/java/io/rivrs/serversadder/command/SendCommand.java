package io.rivrs.serversadder.command;

import java.util.concurrent.CompletableFuture;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.model.ProxyActionType;
import io.rivrs.serversadder.model.ProxyPlayer;
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

        // Broadcast the action
        this.plugin.getRedis().sendProxyAction(ProxyActionType.SEND_ALL, server.getServerInfo().getName());

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
    @CommandCompletion("@proxyPlayers @servers")
    @CommandPermission("serversadder.send.player")
    public void onSendPlayer(CommandSource source, ProxyPlayer player, RegisteredServer server) {
        TagResolver playerPlaceholder = Placeholder.unparsed("player", player.getUsername());
        TagResolver serverPlaceholder = Placeholder.unparsed("server", server.getServerInfo().getName());

        // Check if the player is already on the server
        if (player.getServer().equals(server.getServerInfo().getName())) {
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

        // Do it remotely
        if (!player.isOnline(this.plugin)) {
            this.plugin.getRedis().sendProxyAction(ProxyActionType.SEND_PLAYER, player.getUniqueId().toString(), server.getServerInfo().getName());
            return;
        }

        // Send the player to the server locally
        player.getPlayer(this.plugin)
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

        // Broadcast the action
        this.plugin.getRedis().sendProxyAction(ProxyActionType.SEND_SERVER, sourceServer.getServerInfo().getName(), targetServer.getServerInfo().getName());

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
