package dev.tokenpass.tokenpasswhitelist;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
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
            if (Files.notExists(configPath)) {
                Files.createDirectories(configPath.getParent());
                Files.copy(ConfigFile.class.getResourceAsStream("/config.yml"), configPath);
            }

            ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            CommentedConfigurationNode node = loader.load(ConfigurationOptions.defaults());

            String ip = node.node("ip").getString("0.0.0.0");
            int port = node.node("port").getInt(5000);
            String apiSecret = node.node("api_secret").getString("REPLACE_ME");
            String websiteDomain = node.node("website_domain").getString("example.com");
            String whitelistCommand = node.node("whitelist_command").getString("whitelist");
            String adminPassword = node.node("admin_password").getString("your_super_secret_password");

            return new ConfigFile(ip, port, apiSecret, websiteDomain, whitelistCommand, adminPassword);
        } catch (IOException e) {
            throw new RuntimeException("[TokenPassWhitelist] Failed to load or create config.yml", e);
        }
    }
}