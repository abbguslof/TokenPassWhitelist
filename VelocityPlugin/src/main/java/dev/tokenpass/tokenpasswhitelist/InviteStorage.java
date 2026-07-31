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

/**
 * Handles all in-memory and persistent storage for invites and permanent links.
 * Invites are stored in memory and asynchronously flushed to disk on modification.
 */
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

    public static class PermanentLink {
        public final String creatorName;
        public final String passwordHash; // null if no password
        public final long createdAt;

        public PermanentLink(String creatorName, String passwordHash, long createdAt) {
            this.creatorName = creatorName;
            this.passwordHash = passwordHash;
            this.createdAt = createdAt;
        }
    }

    private static final Map<String, InviteEntry> invites = new ConcurrentHashMap<>();
    private static final Map<String, PermanentLink> permanentLinks = new ConcurrentHashMap<>();
    
    private static ConfigurationLoader<CommentedConfigurationNode> loader;
    private static ConfigurationLoader<CommentedConfigurationNode> permanentLoader;

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

    public static void loadPermanentLinksFromDisk(Path path) {
        permanentLoader = YamlConfigurationLoader.builder().path(path).build();
        permanentLinks.clear();

        try {
            if (!Files.exists(path)) return;

            ConfigurationNode root = permanentLoader.load();
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : root.childrenMap().entrySet()) {
                String id = entry.getKey().toString();
                ConfigurationNode node = entry.getValue();

                String creatorName = node.node("creatorName").getString();
                String passwordHash = node.node("passwordHash").getString(null);
                long createdAt = node.node("createdAt").getLong(System.currentTimeMillis());

                permanentLinks.put(id, new PermanentLink(creatorName, passwordHash, createdAt));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Single-threaded executor used to run disk write operations in the background
     * without blocking the main Velocity proxy threads.
     */
    private static final java.util.concurrent.ExecutorService saveExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();

    /**
     * Serializes the current in-memory invite cache to invites.yml on disk.
     * Executes asynchronously.
     */
    public static void saveToDisk() {
        if (loader == null) return;

        saveExecutor.submit(() -> {
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
        });
    }

    public static void savePermanentLinksToDisk() {
        if (permanentLoader == null) return;

        saveExecutor.submit(() -> {
            try {
                ConfigurationNode root = permanentLoader.createNode();
                for (Map.Entry<String, PermanentLink> entry : permanentLinks.entrySet()) {
                    ConfigurationNode node = root.node(entry.getKey());
                    PermanentLink link = entry.getValue();

                    node.node("creatorName").set(link.creatorName);
                    node.node("passwordHash").set(link.passwordHash);
                    node.node("createdAt").set(link.createdAt);
                }
                permanentLoader.save(root);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Creates a new permanent link in memory and schedules a disk save.
     * 
     * @param id The unique identifier for the URL (e.g. public-invite/id).
     * @param creatorName The name attached to the link (e.g. Discord, ServerOwner).
     * @param passwordHash An optional hashed password to restrict the link.
     */
    public static void createPermanentLink(String id, String creatorName, String passwordHash) {
        permanentLinks.put(id, new PermanentLink(creatorName, passwordHash, System.currentTimeMillis()));
        savePermanentLinksToDisk();
    }

    /**
     * Retrieves a permanent link by its ID.
     */
    public static PermanentLink getPermanentLink(String id) {
        return permanentLinks.get(id);
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
        InviteEntry entry = invites.get(token);
        return entry != null && entry.targetName == null;
    }

    // Called when user submits the form
    public static InviteEntry useToken(String token, String username) {
        InviteEntry original = invites.get(token);
        if (original == null || original.targetName != null) return null;

        InviteEntry claimed = new InviteEntry(
                original.inviterUUID,
                original.inviterName,
                username,
                original.createdAt
        );
        invites.put(token, claimed);
        saveToDisk();
        return claimed;
    }

    public static List<Map.Entry<String, InviteEntry>> getInvitesBy(UUID inviterUUID) {
        return invites.entrySet().stream()
                .filter(e -> inviterUUID.equals(e.getValue().inviterUUID))
                .collect(Collectors.toList());
    }

    public static Map<String, InviteEntry> getAllInvites() {
        return Collections.unmodifiableMap(invites);
    }

    public static void deleteInvite(String token) {
        invites.remove(token);
        saveToDisk();
    }

    public static Map<String, PermanentLink> getAllPermanentLinks() {
        return Collections.unmodifiableMap(permanentLinks);
    }

    // Used by admin panel (without a Minecraft UUID)
    public static void inviteFromWeb(String token, String inviterName, String targetName) {
        invites.put(token, new InviteEntry(null, inviterName, targetName));
        saveToDisk();
    }
}
