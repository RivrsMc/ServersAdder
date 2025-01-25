package io.rivrs.serversadder.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandAlias("setslot")
@Description("Set the slot of the server")
@CommandPermission("serversadder.setslot")
public class SetSlotCommand extends BaseCommand {

    @Default
    public void onDefault(CommandSender sender, int slots) {
        Bukkit.setMaxPlayers(slots);
        sender.sendMessage(Component.text("Set the slot of the server to ", NamedTextColor.GRAY)
                .append(Component.text(slots, NamedTextColor.AQUA)));
    }
}
