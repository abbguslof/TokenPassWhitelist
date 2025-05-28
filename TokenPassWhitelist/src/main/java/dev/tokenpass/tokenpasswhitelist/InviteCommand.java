package dev.tokenpass.tokenpasswhitelist;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InviteCommand implements SimpleCommand {

    private final TokenPassWhitelist plugin;

    public InviteCommand(TokenPassWhitelist plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("[TokenPassWhitelist] Only players can use this command.", NamedTextColor.RED));
            return;
        }

        Player player = (Player) source;
        UUID playerUUID = player.getUniqueId();

        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            List<Map.Entry<String, InviteStorage.InviteEntry>> invites = InviteStorage.getInvitesBy(playerUUID);

            if (invites.isEmpty()) {
                player.sendMessage(Component.text("[TokenPassWhitelist] You haven’t generated any invites yet.", NamedTextColor.GRAY));
                return;
            }

            player.sendMessage(Component.text("[TokenPassWhitelist] Your active invites:", NamedTextColor.GREEN));
            for (Map.Entry<String, InviteStorage.InviteEntry> entry : invites) {
                String token = entry.getKey();
                String link = "https://" + plugin.getConfig().websiteDomain + "/invite/" + token;

                player.sendMessage(
                        Component.text("- ", NamedTextColor.YELLOW)
                                .append(Component.text(link, NamedTextColor.AQUA))
                );
            }

        } else if (args.length == 0) {
            String token = InviteStorage.generateToken(player);
            String inviteLink = "https://" + plugin.getConfig().websiteDomain + "/invite/" + token;

            player.sendMessage(Component.text("[TokenPassWhitelist] Invite link: ", NamedTextColor.GREEN)
                    .append(Component.text(inviteLink, NamedTextColor.AQUA).clickEvent(
                            net.kyori.adventure.text.event.ClickEvent.openUrl(inviteLink)
                    ).hoverEvent(
                            net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Click to open"))
                    )));
        } else {
            player.sendMessage(Component.text("[TokenPassWhitelist] Usage: /invite [list]", NamedTextColor.RED));
        }
    }
}
