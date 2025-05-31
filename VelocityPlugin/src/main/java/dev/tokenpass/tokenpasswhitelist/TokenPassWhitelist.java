package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(
        id = "tokenpasswhitelist",
        name = "TokenPassWhitelist",
        version = "1.0-SNAPSHOT",
        authors = {"Gustav"}
)
public class TokenPassWhitelist {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigFile config;

    @Inject
    public TokenPassWhitelist(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("[TokenPassWhitelist] Initializing...");

        // Load configuration
        this.config = ConfigFile.load(dataDirectory.resolve("config.yml"));
        logger.info("[TokenPassWhitelist] Configuration loaded.");

        // Start internal HTTP server
        InternalHttpServer.start(this, config);

        // Register /invite command
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("invite").plugin(this).build(),
                new InviteCommand(this)
        );

        // Save invites to yml file
        InviteStorage.loadFromDisk(getInvitesFile());

        logger.info("[TokenPassWhitelist] Plugin initialized successfully.");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Path getInvitesFile() {
        return dataDirectory.resolve("invites.yml");
    }

    public ConfigFile getConfig() {
        return config;
    }
}