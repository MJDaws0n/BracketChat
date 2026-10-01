package net.mjdawson.bracketchat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BracketChat extends JavaPlugin implements Listener {
    private boolean publicTeamMessages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        publicTeamMessages = getConfig().getBoolean("broadcast-private-messages", true);
        for (String name : List.of("global", "bsay", "bmsg", "bme", "bteammsg")) {
            var command = getCommand(name);
            if (command == null) throw new IllegalStateException("Missing command: " + name);
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Whispers: PRIVATE. Team delivery: " + (publicTeamMessages ? "GLOBAL" : "TEAM"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        // Cancel the original packet path so recipients see exactly one system message.
        event.setCancelled(true);
        Player sender = event.getPlayer();
        Component message = event.message();
        Runnable send = () -> {
            if (sender.isOnline() && allowed(sender, "chat")) broadcast(format(sender, message));
        };
        if (event.isAsynchronous()) Bukkit.getScheduler().runTask(this, send);
        else send.run();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String replacement = CommandInput.replacement(event.getMessage());
        if (replacement != null) event.setMessage("/" + replacement);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        String replacement = CommandInput.replacement(event.getCommand());
        if (replacement != null) event.setCommand(replacement);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        onConsoleCommand(event);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (publicTeamMessages && getConfig().getBoolean("public-message-notice", true))
            event.getPlayer().sendMessage(Component.text(
                "[BracketChat] Team chat is PUBLIC on this server. Whispers are private.",
                NamedTextColor.YELLOW));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String type = command.getName();
        String permission = switch (type) {
            case "bsay" -> "say";
            case "bmsg" -> "whisper";
            case "bme" -> "me";
            case "bteammsg" -> "team";
            default -> "chat";
        };
        if (!allowed(sender, permission)) return true;
        String text = String.join(" ", args).strip();
        if (text.isBlank()) return false;
        switch (type) {
            case "bmsg" -> whisper(sender, text);
            case "bteammsg" -> team(sender, text);
            // Actions use the same exact [username] text format, as requested.
            default -> broadcast(format(sender, Component.text(text)));
        }
        return true;
    }

    private void whisper(CommandSender sender, String arguments) {
        String[] parts = CommandInput.firstArgument(arguments);
        if (parts[0].isBlank() || parts[1].isBlank()) {
            error(sender, "Usage: /w <online player or selector> <message>");
            return;
        }
        Set<Player> targets = new LinkedHashSet<>();
        if (parts[0].startsWith("@")) {
            if (!sender.hasPermission("minecraft.command.selector")) {
                error(sender, "You do not have permission to use target selectors.");
                return;
            }
            try {
                Bukkit.selectEntities(sender, parts[0]).stream()
                    .filter(Player.class::isInstance).map(Player.class::cast).forEach(targets::add);
            } catch (IllegalArgumentException exception) {
                error(sender, "Invalid player selector.");
                return;
            }
        } else {
            Player target = Bukkit.getPlayerExact(parts[0]);
            if (target != null) targets.add(target);
        }
        if (targets.isEmpty()) { error(sender, "No matching online players."); return; }
        Component message = Component.text("(W)[" + sender.getName() + "] " + parts[1],
            NamedTextColor.GRAY).decorate(TextDecoration.ITALIC);
        // Whispers are always private, including when upgrading with the old config.
        targets.forEach(player -> player.sendMessage(message));
        if (!(sender instanceof Player player) || !targets.contains(player)) sender.sendMessage(message);
    }

    private void team(CommandSender sender, String text) {
        if (!(sender instanceof Player player)) {
            error(sender, "Team chat requires a player.");
            return;
        }
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(player.getName());
        if (team == null) { error(sender, "You are not on a team."); return; }
        Component message = format(sender, Component.text(text));
        if (publicTeamMessages) broadcast(message);
        else for (Player recipient : Bukkit.getOnlinePlayers())
            if (team.hasEntry(recipient.getName())) recipient.sendMessage(message);
    }

    private Component format(CommandSender sender, Component message) {
        return Component.text("[" + sender.getName() + "] ", NamedTextColor.WHITE).append(message);
    }

    private void broadcast(Component message) {
        // Explicit recipients: every online player across all worlds, plus console once.
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }

    private boolean allowed(CommandSender sender, String permission) {
        if (sender.hasPermission("bracketchat." + permission)) return true;
        error(sender, "You do not have permission to use this chat command.");
        return false;
    }

    private void error(CommandSender sender, String text) {
        sender.sendMessage(Component.text(text, NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equals("bmsg") && args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .filter(player -> !(sender instanceof Player viewer) || viewer.canSee(player))
                .map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        return List.of();
    }
}
