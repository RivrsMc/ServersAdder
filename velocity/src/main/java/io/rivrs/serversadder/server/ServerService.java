package io.rivrs.serversadder.server;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.GameServer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ServerService {

    private final ServersAdder plugin;
    private final Map<String, Long> keepAlive = new ConcurrentHashMap<>();
    private ScheduledTask healthCheckTask;

    public void startHealthCheck() {
        this.healthCheckTask = this.plugin.getServer()
                .getScheduler()
                .buildTask(this.plugin, () -> {
                    List<String> deadServers = this.deadServers(10, TimeUnit.SECONDS);
                    deadServers.forEach(id -> {
                        this.plugin.getLogger().warn("Server {} didn't respond to keep alive, removing...", id);
                        this.unregister(id);
                    });
                })
                .repeat(10, TimeUnit.SECONDS)
                .delay(5, TimeUnit.SECONDS)
                .schedule();
    }

    public void stopHealthCheck() {
        if (this.healthCheckTask != null)
            this.healthCheckTask.cancel();
    }

    public void register(GameServer server) {
        if (this.isRegistered(server.id())) {
            this.plugin.getLogger().warn("Server {} tried to register but it's already registered!", server.id());
            return;
        }

        // Register in velocity
        this.plugin.getServer()
                .registerServer(new ServerInfo(
                        server.id(),
                        InetSocketAddress.createUnresolved(server.host(), server.port())
                ));

        // Keep alive
        this.update(server.id());

        this.plugin.getLogger().info("Registered server {}!", server.id());
    }

    public void update(String id) {
        keepAlive.put(id, System.currentTimeMillis());
    }

    public void unregister(String id) {
        // Remove from keep alive
        keepAlive.remove(id);

        // Remove from velocity
        this.plugin.getServer()
                .getServer(id)
                .ifPresentOrElse(server -> {
                    this.plugin.getServer().unregisterServer(server.getServerInfo());

                    this.plugin.getLogger().info("Unregistered server {}!", id);
                }, () -> this.plugin.getLogger().warn("Server {} tried to unregister but it's not registered!", id));
    }

    public List<String> deadServers(long time, TimeUnit unit) {
        return keepAlive.entrySet()
                .stream()
                .filter(entry -> System.currentTimeMillis() - entry.getValue() > unit.toMillis(time))
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean isRegistered(String id) {
        return this.plugin.getServer()
                .getServer(id)
                .isPresent();
    }

}
