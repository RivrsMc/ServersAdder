package io.rivrs.serversadder.configuration;

import com.moandjiezana.toml.Toml;
import io.rivrs.serversadder.ServersAdder;
import io.rivrs.serversadder.commons.RedisCredentials;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;

@Getter
public class Configuration {

    private final ServersAdder plugin;
    private final Path path;

    // Redis
    private RedisCredentials redisCredentials;

    // Server related
    private String identifier;
    private String name;

    public Configuration(ServersAdder plugin) {
        this.plugin = plugin;
        this.path = plugin.getDataDirectory().resolve("config.toml");
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
            try (InputStream inputStream = ServersAdder.class.getClassLoader().getResourceAsStream("config.toml")) {
                if (inputStream == null)
                    throw new IllegalStateException("config.toml not found in resources");
                Files.copy(inputStream, this.path);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Load
        Toml toml = new Toml().read(this.path.toFile());

        // Redis
        Toml redis = toml.getTable("redis");

        String host = redis.getString("host");
        int port = redis.getLong("port").intValue();
        String password = redis.getString("password");

        this.redisCredentials = new RedisCredentials(host, port, password);

        // Server related
        Toml identifier = toml.getTable("identifier");
        this.identifier = identifier.getString("id");
        this.name = identifier.getString("name");
    }
}
