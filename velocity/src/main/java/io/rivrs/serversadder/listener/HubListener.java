package io.rivrs.serversadder.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;

import io.rivrs.serversadder.ServersAdder;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HubListener {

    private final ServersAdder plugin;

    @Subscribe
    public void onServerKick(KickedFromServerEvent e) {
        Player player = e.getPlayer();

        if (this.plugin.getService().isRegistered(e.getServer().getServerInfo().getName()))
            this.plugin.getRedis()
                    .findEmptiestServerInGroup(this.plugin.getConfiguration().getFallbackGroup())
                    .ifPresentOrElse(server -> e.setResult(KickedFromServerEvent.RedirectPlayer.create(server)),
                            () -> this.plugin.getLogger().warn("No fallback server found for player {}", player.getUsername()));
    }
}
