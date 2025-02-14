package io.rivrs.serversadder.task;

import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.commons.MessageType;
import io.rivrs.serversadder.commons.RedisChannel;
import io.rivrs.serversadder.model.CleanRestartContext;
import io.rivrs.serversadder.model.ProxyActionType;
import io.rivrs.serversadder.model.ProxyPlayer;
import io.rivrs.serversadder.model.ServerStatus;
import io.rivrs.serversadder.redis.RedisManager;
import io.rivrs.serversadder.server.RestartService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

@RequiredArgsConstructor
public class CleanRestartTask implements Runnable {

    private final ServersAdder plugin;
    private final RestartService service;

    @Override
    public void run() {
        RedisManager redis = this.plugin.getRedis();

        CleanRestartContext context = this.service.getContext();
        if (context == null) {
            // Stop the task
            this.service.shutdown();

            // Notify
            this.plugin.getLogger().warn("Clean restart task has been stopped due to the context is null.");
            return;
        } else if (context.isDone()) {
            // Stop the task
            this.service.shutdown();

            // Notify
            this.plugin.getLogger().info("Clean restart task has been completed.");
            context.message(this.plugin.getMessages().get("clean-restart-done", Placeholder.parsed("group", context.getSourceGroup())));

            // Mark as done
            plugin.getRedis().markGroupAsNotRestarting(context.getSourceGroup());

            return;
        }

        GameServer server = context.getCurrentServer();
        switch (context.getStatus().get()) {
            case ONLINE -> {
                this.plugin.getLogger().info("Pre-shutting down server {}...", server.id());
                redis.send(RedisChannel.RESTART, MessageType.PRE_SHUTDOWN, server.id(), context.getReason());

                context.getStatus().set(ServerStatus.PRE_SHUTDOWN);
                context.setLastActionTime(System.currentTimeMillis());
            }
            case PRE_SHUTDOWN_ACK -> {
                this.plugin.getLogger().info("Shutting down server {}...", server.id());

                // Send all players to another server from the other group
                List<ProxyPlayer> players = redis.getPlayersByServer(context.getCurrentServer().id());
                List<CompletableFuture<ConnectionRequestBuilder.Result>> completableFutures = new ArrayList<>();
                for (ProxyPlayer player : players) {
                    // Find emptiest server from the other group
                    redis.findEmptiestServerInGroup(context.getTargetGroup())
                            .ifPresent(serverInfo -> {
                                if (player.isOnline(plugin)) {
                                    completableFutures.add(player.getPlayer(this.plugin)
                                            .createConnectionRequest(serverInfo)
                                            .connect());
                                    return;
                                }

                                redis.sendProxyAction(ProxyActionType.SEND_PLAYER, player.getUniqueId().toString(), serverInfo.getServerInfo().getName());
                            });
                }

                CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
                        .thenRun(() -> {
                            redis.send(RedisChannel.RESTART, MessageType.SHUTDOWN, server.id(), context.getReason());
                            context.getStatus().set(ServerStatus.SHUTDOWN);

                            context.setLastActionTime(System.currentTimeMillis());
                        });
            }
            case SHUTDOWN_ACK -> {
                this.plugin.getLogger().info("{} is rebooting...", server.id());
                context.getStatus().set(ServerStatus.OFFLINE);

                context.setLastActionTime(System.currentTimeMillis());

                // Go to next server after 1m30s
                plugin.getServer().getScheduler()
                        .buildTask(plugin, () -> {
                            GameServer next = context.next();
                            if (next == null)
                                return;

                            context.getStatus().set(ServerStatus.ONLINE);
                        })
                        .delay(90, TimeUnit.SECONDS)
                        .schedule();
            }
            case RESTARTED -> {
                context.message(this.plugin.getMessages().get(
                        "server-rebooted",
                        Placeholder.parsed("server", server.id()),
                        Placeholder.parsed("rebooted", String.valueOf(context.getCurrentServerIndex() + 1)),
                        Placeholder.parsed("total", String.valueOf(context.getSourceServers().size()))
                ));
                this.plugin.getLogger().info("Server {} has been restarted.", server.id());
            }
        }
    }

}
