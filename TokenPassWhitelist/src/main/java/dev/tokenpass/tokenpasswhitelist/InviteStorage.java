package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InviteStorage {

    public static class InviteEntry {
        public final UUID inviterUUID;       // Can be null for web-generated
        public final String inviterName;
        public final String targetName;      // null until claimed
        public final long createdAt;

        public InviteEntry(UUID inviterUUID, String inviterName, String targetName) {
            this(inviterUUID, inviterName, targetName, System.currentTimeMillis());
        }

        public InviteEntry(UUID inviterUUID, String inviterName, String targetName, long createdAt) {
            this.inviterUUID = inviterUUID;
            this.inviterName = inviterName;
            this.targetName = targetName;
            this.createdAt = createdAt;
        }
    }

    private static final Map<String, InviteEntry> invites = new ConcurrentHashMap<>();
    private static ConfigurationLoader<CommentedConfigurationNode> loader;

    public static void loadFromDisk(Path path) {
        loader = YamlConfigurationLoader.builder().path(path).build();
        invites.clear();

        try {
            if (!Files.exists(path)) return;

            ConfigurationNode root = loader.load();
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.childrenMap().entrySet()) {
                String token = entry.getKey().toString();
                ConfigurationNode node = entry.getValue();

                UUID inviterUUID = node.node("inviterUUID").get(UUID.class);
                String inviterName = node.node("inviterName").getString();
                String targetName = node.node("targetName").getString(null);
                long createdAt = node.node("createdAt").getLong(System.currentTimeMillis());

                invites.put(token, new InviteEntry(inviterUUID, inviterName, targetName, createdAt));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToDisk() {
        if (loader == null) return;

        try {
            ConfigurationNode root = loader.createNode();
            for (Map.Entry<String, InviteEntry> entry : invites.entrySet()) {
                ConfigurationNode node = root.node(entry.getKey());
                InviteEntry invite = entry.getValue();

                node.node("inviterUUID").set(invite.inviterUUID);
                node.node("inviterName").set(invite.inviterName);
                node.node("targetName").set(invite.targetName);
                node.node("createdAt").set(invite.createdAt);
            }
            loader.save(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Called from /invite
    public static String generateToken(CommandSource source) {
        UUID inviterUUID = null;
        String inviterName = "[console]";

        if (source instanceof Player player) {
            inviterUUID = player.getUniqueId();
            inviterName = player.getUsername();
        }

        String token = UUID.randomUUID().toString();
        invites.put(token, new InviteEntry(inviterUUID, inviterName, null));
        saveToDisk();
        return token;
    }

    public static boolean isValidToken(String token) {
        return invites.containsKey(token);
    }

    // Called when user submits the form
    public static InviteEntry useToken(String token, String username) {
        InviteEntry original = invites.remove(token);
        if (original == null) return null;

        saveToDisk();
        return new InviteEntry(
                original.inviterUUID,
                original.inviterName,
                username,
                original.createdAt
        );
    }

    public static List<Map.Entry<String, InviteEntry>> getInvitesBy(UUID inviterUUID) {
        return invites.entrySet().stream()
                .filter(e -> inviterUUID.equals(e.getValue().inviterUUID))
                .collect(Collectors.toList());
    }

    // Used by admin panel (without a Minecraft UUID)
    public static void inviteFromWeb(String token, String inviterName, String targetName) {
        invites.put(token, new InviteEntry(null, inviterName, targetName));
        saveToDisk();
    }
}
