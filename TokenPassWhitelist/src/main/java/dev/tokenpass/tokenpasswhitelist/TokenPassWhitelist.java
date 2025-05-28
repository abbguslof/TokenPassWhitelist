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

    private ConfigFile config;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    @Inject
    public TokenPassWhitelist(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("[TokenPassWhitelist] Loading configuration...");
        this.config = ConfigFile.load(dataDirectory.resolve("config.yml"));
        logger.info("Config loaded: IP=" + config.ip + ", Port=" + config.port);

        logger.info("[TokenPassWhitelist] Starting HTTP server...");
        InternalHttpServer.start(this, config);

        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("invite").plugin(this).build(),
                new InviteCommand(this)
        );

        logger.info("[TokenPassWhitelist] Plugin successfully initialized.");
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

    public ConfigFile getConfig() {
        return config;
    }
}