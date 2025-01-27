package io.rivrs.serversadder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import co.aikar.commands.PaperCommandManager;
import io.rivrs.serversadder.command.SetSlotCommand;
import io.rivrs.serversadder.redis.RedisManager;

public final class ServersAdder extends JavaPlugin {

    private RedisManager redis;
    private PaperCommandManager commands;

    @Override
    public void onEnable() {
        // Redis
        this.redis = new RedisManager(this);
        this.redis.load();

        // Commands
        this.commands = new PaperCommandManager(this);
        this.commands.registerCommand(new SetSlotCommand());

        // Register server
        Bukkit.getScheduler().runTask(this, () -> this.redis.registerServer());

        // Keep alive every 2 seconds
        Bukkit.getScheduler().runTaskTimer(this, () -> this.redis.update(), 0, 40);
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
