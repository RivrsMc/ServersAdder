package io.rivrs.serversadder.maintenance;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import io.rivrs.serversadder.ServersAdder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class MaintenanceConfiguration {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Path path;
    private Toml toml;

    // Configuration values
    @Getter
    @Setter
    private boolean enabled;
    @Getter
    @Setter
    private Component kickMessage;
    private final List<String> allowedPlayers = new ArrayList<>();

    public MaintenanceConfiguration(ServersAdder plugin) {
        this.path = plugin.getDataDirectory().resolve("maintenance.toml");
    }

    public boolean load() {
        // Create datafolder
        if (!Files.isDirectory(path.getParent())) {
            try {
                Files.createDirectories(path.getParent());
            } catch (Exception e) {
                return false;
            }
        }

        // Create config file
        if (!Files.exists(path)) {
            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("maintenance.toml")) {
                if (inputStream == null) {
                    return false;
                }
                Files.copy(inputStream, path);
            } catch (Exception e) {
                return false;
            }
        }

        // Load toml
        this.toml = new Toml().read(path.toFile());
        if (toml == null || toml.isEmpty())
            return false;

        // Load values
        this.enabled = toml.getBoolean("enabled", true);
        this.kickMessage = MINI_MESSAGE.deserialize(toml.getString("kick-message", "<red>The server is currently down for maintenance. Please try again later."));
        this.allowedPlayers.addAll(toml.getList("allowed-players", List.of("LordKiwix", "Kookrix")));

        return true;
    }

    public void save() throws IOException {
        if (toml == null)
            return;

        Map<String, Object> map = toml.toMap();
        map.put("enabled", enabled);
        map.put("kick-message", MINI_MESSAGE.serialize(kickMessage));
        map.put("allowed-players", allowedPlayers);

        TomlWriter writer = new TomlWriter();
        writer.write(map, path.toFile());
    }

    public boolean isAllowed(String playerName) {
        return allowedPlayers.contains(playerName);
    }

    public void addAllowedPlayer(String playerName) {
        allowedPlayers.add(playerName);
    }

    public void removeAllowedPlayer(String playerName) {
        allowedPlayers.remove(playerName);
    }
}
