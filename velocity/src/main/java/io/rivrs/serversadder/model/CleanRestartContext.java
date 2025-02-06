package io.rivrs.serversadder.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.Nullable;

import com.velocitypowered.api.command.CommandSource;

import io.rivrs.serversadder.commons.GameServer;
import lombok.Getter;
import net.kyori.adventure.text.Component;

@Getter
public class CleanRestartContext {

    private final CommandSource executor;
    private final List<GameServer> sourceServers;
    private final String sourceGroup;
    private final String targetGroup;
    private final String reason;
    private final long startTime = System.currentTimeMillis();

    private int currentServerIndex = 0;
    private GameServer currentServer;
    private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.ONLINE);

    public CleanRestartContext(CommandSource executor, List<GameServer> sourceServers, String sourceGroup, String targetGroup, String reason) {
        this.executor = executor;
        this.sourceServers = new ArrayList<>(sourceServers);
        this.sourceGroup = sourceGroup;
        this.targetGroup = targetGroup;
        this.reason = reason;
        this.currentServer = sourceServers.get(0);
    }

    public @Nullable GameServer next() {
        this.currentServerIndex++;
        if (this.currentServerIndex >= this.sourceServers.size())
            return null;
        
        this.currentServer = this.sourceServers.get(this.currentServerIndex);
        this.status.set(ServerStatus.ONLINE);

        return this.currentServer;
    }

    public boolean isDone() {
        return this.currentServerIndex >= this.sourceServers.size()
               && this.status.get() == ServerStatus.RESTARTED;
    }

    public void message(Component component) {
        if (this.executor != null)
            this.executor.sendMessage(component);
    }
}
