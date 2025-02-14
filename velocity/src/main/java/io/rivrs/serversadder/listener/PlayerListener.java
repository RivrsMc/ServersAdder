package io.rivrs.serversadder.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.model.ProxyPlayer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlayerListener {

    private final ServersAdder plugin;

    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();

        this.plugin.getRedis().registerPlayer(new ProxyPlayer(
                player.getUniqueId(),
                player.getUsername(),
                this.plugin.getConfiguration().getIdentifier(),
                null
        ));
    }

    @Subscribe
    public void onServerChange(ServerConnectedEvent e) {
        Player player = e.getPlayer();
        RegisteredServer server = e.getServer();
        if (player == null
            || server == null
            || server.getServerInfo() == null)
            return;


        this.plugin.getRedis().editPlayer(player.getUniqueId(), proxyPlayer -> proxyPlayer.setServer(server.getServerInfo().getName()));
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        this.plugin.getRedis().unregisterPlayer(event.getPlayer());
    }
}
