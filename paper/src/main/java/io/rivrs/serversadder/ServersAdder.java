package io.rivrs.serversadder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.rivrs.serversadder.redis.RedisManager;

public final class ServersAdder extends JavaPlugin {

    private RedisManager redis;

    @Override
    public void onEnable() {
        this.redis = new RedisManager(this);
        this.redis.load();

        // Register server
        Bukkit.getScheduler().runTask(this, () -> this.redis.registerServer());

        // Keep alive every 5 seconds
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> this.redis.update(), 0, 100);
    }

    @Override
    public void onDisable() {
        // Unregister server
        this.redis.unregisterServer();

        // Cancel all tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Close redis
        this.redis.close();
    }


}
