package io.rivrs.serversadder.configuration;

import com.moandjiezana.toml.Toml;
import io.rivrs.serversadder.ServersAdder;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MessageConfiguration {

    public static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Path path;
    private final Map<String, String> messages = new HashMap<>();

    public MessageConfiguration(ServersAdder plugin) {
        this.path = plugin.getDataDirectory().resolve("messages.toml");
    }

    public void load() {
        // Create parent
        if (!Files.isDirectory(this.path.getParent())) {
            try {
                Files.createDirectories(this.path.getParent());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Copy from resources
        if (!Files.exists(this.path)) {
            try (InputStream inputStream = ServersAdder.class.getClassLoader().getResourceAsStream("messages.toml")) {
                if (inputStream == null)
                    throw new IllegalStateException("messages.toml not found in resources");
                Files.copy(inputStream, this.path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Load
        Toml toml = new Toml().read(this.path.toFile());
        toml.toMap().forEach((key, value) -> this.messages.put(key, value.toString()));
    }

    public String raw(String key) {
        return this.messages.getOrDefault(key, key);
    }

    public Component get(String key, TagResolver... resolvers) {
        return MINI_MESSAGE.deserialize(this.raw(key), resolvers);
    }
}
