package io.rivrs.serversadder.server;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.scheduler.ScheduledTask;

import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.GameServer;
import io.rivrs.serversadder.model.CleanRestartContext;
import io.rivrs.serversadder.redis.RedisManager;
import io.rivrs.serversadder.task.CleanRestartTask;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class RestartService {

    private final ServersAdder plugin;
    private final Logger logger;

    @Getter
    private CleanRestartContext context;
    private ScheduledTask task;

    public RestartService(ServersAdder plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isRestarting(String serverId) {
        return this.context != null && this.context.getCurrentServer().id().equals(serverId);
    }

    public void start(ServersAdder plugin, CommandSource source, String sourceGroup, String targetGroup, @Nullable String reason) {
        RedisManager redis = plugin.getRedis();

        List<GameServer> sourceServers = redis.getServersByGroup(sourceGroup);
        List<GameServer> targetServers = redis.getServersByGroup(targetGroup);

        // Check if the source group exists
        if (sourceServers.isEmpty()) {
            source.sendMessage(this.plugin.getMessages().get("group-empty", Placeholder.parsed("group", sourceGroup)));
            return;
        }

        // Check if the target group exists
        if (targetServers.isEmpty()) {
            source.sendMessage(this.plugin.getMessages().get("group-empty", Placeholder.parsed("group", targetGroup)));
            return;
        }

        // Check if group is already restarting
        if (plugin.getRedis().isGroupRestarting(sourceGroup)) {
            source.sendMessage(this.plugin.getMessages().get("group-already-restarting", Placeholder.parsed("group", sourceGroup)));
            return;
        }

        // Notify console & command source
        this.logger.info("Starting clean restart from {} to {} with reason {}", sourceGroup, targetGroup, reason);
        source.sendMessage(this.plugin.getMessages().get("clean-restart-started", Placeholder.parsed("source", sourceGroup), Placeholder.parsed("target", targetGroup)));

        // Mark as restarting
        plugin.getRedis().markGroupAsRestarting(sourceGroup);

        // Create context
        this.context = new CleanRestartContext(source, sourceServers, sourceGroup, targetGroup, reason);

        // Start clean restart task
        this.task = this.plugin.getServer()
                .getScheduler()
                .buildTask(this.plugin, new CleanRestartTask(ServersAdder.get(), this))
                .repeat(10, TimeUnit.SECONDS)
                .schedule();
    }

    public void shutdown() {
        this.context = null;

        if (this.task == null)
            return;

        this.task.cancel();
        this.task = null;
    }
}
