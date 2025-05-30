package dev.tokenpass.tokenpasswhitelist;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigFile {
    public final String ip;
    public final int port;
    public final String apiSecret;
    public final String websiteDomain;
    public final String whitelistCommand;
    public final String adminPassword;

    public ConfigFile(String ip, int port, String apiSecret, String websiteDomain, String whitelistCommand, String adminPassword) {
        this.ip = ip;
        this.port = port;
        this.apiSecret = apiSecret;
        this.websiteDomain = websiteDomain;
        this.whitelistCommand = whitelistCommand;
        this.adminPassword = adminPassword;
    }

    public static ConfigFile load(Path configPath) {
        try {
            // If config doesn't exist, copy from plugin jar
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.getParent());
                try (InputStream in = ConfigFile.class.getResourceAsStream("/config.yml")) {
                    if (in == null) throw new IOException("Missing default config.yml in resources!");
                    Files.copy(in, configPath);
                }
            }

            ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            CommentedConfigurationNode node = loader.load(ConfigurationOptions.defaults());

            return new ConfigFile(
                    node.node("ip").getString("0.0.0.0"),
                    node.node("port").getInt(5000),
                    node.node("api_secret").getString("REPLACE_ME"),
                    node.node("website_domain").getString("example.com"),
                    node.node("whitelist_command").getString("whitelist add "),
                    node.node("admin_password").getString("change_me_now")
            );

        } catch (IOException e) {
            throw new RuntimeException("[TokenPassWhitelist] Failed to load or create config.yml", e);
        }
    }
}
