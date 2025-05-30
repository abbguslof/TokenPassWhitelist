package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InviteStorage {

    // Represents a single invite
    public static class InviteEntry {
        public final UUID inviterUUID;       // Can be null for web-generated
        public final String inviterName;
        public final String targetName;      // null until claimed
        public final long createdAt;

        public InviteEntry(UUID inviterUUID, String inviterName, String targetName) {
            this.inviterUUID = inviterUUID;
            this.inviterName = inviterName;
            this.targetName = targetName;
            this.createdAt = System.currentTimeMillis();
        }
    }

    // token -> InviteEntry
    private static final Map<String, InviteEntry> invites = new ConcurrentHashMap<>();

    // Called from /invite
    public static String generateToken(CommandSource source) {
        UUID inviterUUID = null;
        String inviterName = "[console]";

        if (source instanceof Player) {
            Player player = (Player) source;
            inviterUUID = player.getUniqueId();
            inviterName = player.getUsername();
        }

        String token = UUID.randomUUID().toString();
        invites.put(token, new InviteEntry(inviterUUID, inviterName, null));
        return token;
    }

    public static boolean isValidToken(String token) {
        return invites.containsKey(token);
    }

    // Called when user submits the form
    public static InviteEntry useToken(String token, String username) {
        InviteEntry original = invites.remove(token);
        if (original == null) return null;

        return new InviteEntry(
                original.inviterUUID,
                original.inviterName,
                username // now we record who claimed it
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
    }
}
