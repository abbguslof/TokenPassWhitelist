package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InviteStorage {

    public static class InviteEntry {
        public final UUID inviterUUID;
        public final String inviterName;
        public final String targetName; // will be null until used
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

    public static String generateToken(CommandSource inviter) {
        UUID inviterUUID = null;
        String inviterName = "[console]";

        if (inviter instanceof com.velocitypowered.api.proxy.Player) {
            com.velocitypowered.api.proxy.Player player = (com.velocitypowered.api.proxy.Player) inviter;
            inviterUUID = player.getUniqueId();
            inviterName = player.getUsername();
        }

        String token = UUID.randomUUID().toString();
        invites.put(token, new InviteEntry(inviterUUID, inviterName, null)); // null targetName
        return token;
    }

    public static boolean isValidToken(String token) {
        return invites.containsKey(token);
    }

    public static InviteEntry useToken(String token) {
        return invites.remove(token); // Removes the token once it's used
    }

    public static List<Map.Entry<String, InviteEntry>> getInvitesBy(UUID inviterUUID) {
        return invites.entrySet().stream()
                .filter(e -> inviterUUID.equals(e.getValue().inviterUUID))
                .collect(Collectors.toList());
    }

    public static void inviteFromWeb(String token, String inviterName, String targetName) {
        invites.put(token, new InviteEntry(null, inviterName, targetName));
    }
}