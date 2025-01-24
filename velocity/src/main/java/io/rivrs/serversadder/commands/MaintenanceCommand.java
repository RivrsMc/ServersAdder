package io.rivrs.serversadder.commands;

import java.io.IOException;

import com.velocitypowered.api.command.CommandSource;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import io.rivrs.serversadder.ServersAdder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@CommandAlias("maintenance|whitelist")
@Description("Manage the maintenance mode")
@CommandPermission("serversadder.maintenance")
public class MaintenanceCommand extends BaseCommand {

    @Dependency
    private ServersAdder plugin;

    @Default
    public void onDefault(CommandSource source) {
        Component status = plugin.getMaintenance().isEnabled() ? Component.text("enabled", NamedTextColor.GREEN) : Component.text("disabled", NamedTextColor.RED);
        source.sendMessage(Component.text("Maintenance mode is currently ", NamedTextColor.GRAY)
                .append(status));
    }

    @Subcommand("on|enable")
    @Description("Enable maintenance mode")
    public void onEnable(CommandSource source) {
        plugin.getMaintenance().setEnabled(true);
        source.sendMessage(Component.text("Maintenance mode is now enabled", NamedTextColor.GREEN));

        this.save(source);
    }

    @Subcommand("off|disable")
    @Description("Disable maintenance mode")
    public void onDisable(CommandSource source) {
        plugin.getMaintenance().setEnabled(false);
        source.sendMessage(Component.text("Maintenance mode is now disabled", NamedTextColor.GREEN));

        this.save(source);
    }

    @Subcommand("add")
    @Description("Add a player to the whitelist")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onAdd(CommandSource source, String username) {
        if (this.plugin.getMaintenance().isAllowed(username)) {
            source.sendMessage(Component.text("Player is already whitelisted", NamedTextColor.RED));
            return;
        }

        this.plugin.getMaintenance().addAllowedPlayer(username);
        source.sendMessage(Component.text("Player has been added to the whitelist", NamedTextColor.GREEN));

        this.save(source);
    }

    @Subcommand("remove")
    @Description("Remove a player from the whitelist")
    @Syntax("<player>")
    @CommandCompletion("@players")
    public void onRemove(CommandSource source, String username) {
        if (!this.plugin.getMaintenance().isAllowed(username)) {
            source.sendMessage(Component.text("Player is not whitelisted", NamedTextColor.RED));
            return;
        }

        this.plugin.getMaintenance().removeAllowedPlayer(username);
        source.sendMessage(Component.text("Player has been removed from the whitelist", NamedTextColor.GREEN));

        this.save(source);
    }

    private void save(CommandSource source) {
        try {
            plugin.getMaintenance().save();
        } catch (IOException e) {
            source.sendMessage(Component.text("Failed to save maintenance mode status", NamedTextColor.RED));
            source.sendMessage(Component.text("Please check the console for more information", NamedTextColor.RED));
            source.sendMessage(Component.text("Maintenance is still enabled but won't be saved at restart!", NamedTextColor.GRAY));
            e.printStackTrace();
        }
    }
}
