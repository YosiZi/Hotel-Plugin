package com.yourname.hotelplugin.commands;

import com.yourname.hotelplugin.data.Reservation; // Add this import statement

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.managers.ReservationManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

public class StaffCommand implements CommandExecutor {

    private final HotelPlugin plugin;
    private final ReservationManager reservationManager;

    public StaffCommand(HotelPlugin plugin) {
        this.plugin = plugin;
        this.reservationManager = plugin.getReservationManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            // This is a staff command, but we should handle console for automation
            sender.sendMessage(ChatColor.RED + "This command can only be used by players for now.");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hotel.staff")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use staff commands.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "book":
                handleBookCommand(player, args);
                break;
            case "cancel":
                handleCancelCommand(player, args);
                break;
            case "check":
                handleCheckCommand(player, args);
                break;
            default:
                showHelp(player);
                break;
        }

        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a--- Hotel Staff Commands ---"));
        if (player.hasPermission("hotel.staff.book")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/hotelstaff book <player> <room> <duration> &7- Books a room for a player."));
        }
        if (player.hasPermission("hotel.staff.cancel")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/hotelstaff cancel <room> &7- Cancels a room reservation."));
        }
        if (player.hasPermission("hotel.staff.check")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/hotelstaff check <room> &7- Checks the status of a room."));
        }
    }

    private void handleBookCommand(Player player, String[] args) {
        if (!player.hasPermission("hotel.staff.book")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to book rooms for others.");
            return;
        }

        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Usage: /hotelstaff book <player> <room> <duration>");
            return;
        }

        String playerName = args[1];
        String roomName = args[2];
        String durationString = args[3];

        OfflinePlayer guest = Bukkit.getOfflinePlayer(playerName);
        if (guest == null || !guest.hasPlayedBefore()) {
            player.sendMessage(ChatColor.RED + "Player '" + playerName + "' has not played on this server before.");
            return;
        }

        long durationMillis = parseDuration(durationString);
        if (durationMillis <= 0) {
            player.sendMessage(ChatColor.RED + "Invalid duration format. Use a number followed by 'h' for hours or 'd' for days (e.g., 5h, 1d).");
            return;
        }

        if (reservationManager.bookRoom(roomName, guest.getUniqueId(), durationMillis)) {
            player.sendMessage(ChatColor.GREEN + "Successfully booked room " + roomName + " for " + playerName + ".");
            if (guest.isOnline()) {
                guest.getPlayer().sendMessage(ChatColor.GREEN + "A staff member has booked room " + roomName + " for you.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "Failed to book room. It may not exist or is already reserved.");
        }
    }

    private void handleCancelCommand(Player player, String[] args) {
        if (!player.hasPermission("hotel.staff.cancel")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to cancel reservations.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /hotelstaff cancel <room>");
            return;
        }

        String roomName = args[1];
        if (reservationManager.cancelReservation(roomName)) {
            player.sendMessage(ChatColor.GREEN + "Successfully cancelled the reservation for room " + roomName + ".");
        } else {
            player.sendMessage(ChatColor.RED + "No active reservation found for room " + roomName + ".");
        }
    }

    private void handleCheckCommand(Player player, String[] args) {
        if (!player.hasPermission("hotel.staff.check")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to check room status.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /hotelstaff check <room>");
            return;
        }

        String roomName = args[1];
        Reservation reservation = reservationManager.getReservation(roomName);

        if (reservation != null) {
            OfflinePlayer guest = Bukkit.getOfflinePlayer(reservation.getGuestUUID());
            long remainingTime = reservation.getExpirationTime() - System.currentTimeMillis();

            player.sendMessage(ChatColor.YELLOW + "--- Room " + roomName + " Status ---");
            player.sendMessage(ChatColor.GREEN + "Status: " + ChatColor.DARK_GREEN + "Reserved");
            player.sendMessage(ChatColor.GREEN + "Guest: " + ChatColor.YELLOW + guest.getName());
            player.sendMessage(ChatColor.GREEN + "Time remaining: " + ChatColor.YELLOW + formatTime(remainingTime));
        } else {
            player.sendMessage(ChatColor.YELLOW + "--- Room " + roomName + " Status ---");
            player.sendMessage(ChatColor.GREEN + "Status: " + ChatColor.GREEN + "Available");
        }
    }

    /**
     * Helper method to parse a duration string like "5h" or "1d".
     */
    private long parseDuration(String durationString) {
        if (durationString == null || durationString.isEmpty()) {
            return -1;
        }

        String unit = durationString.substring(durationString.length() - 1).toLowerCase();
        String numberString = durationString.substring(0, durationString.length() - 1);
        long number;
        try {
            number = Long.parseLong(numberString);
        } catch (NumberFormatException e) {
            return -1;
        }

        switch (unit) {
            case "h": // Hours
                return TimeUnit.HOURS.toMillis(number);
            case "d": // Days
                return TimeUnit.DAYS.toMillis(number);
            default:
                return -1;
        }
    }

    /**
     * Helper method to format milliseconds into a readable string (e.g., "1d 5h 30m").
     */
    private String formatTime(long millis) {
        if (millis <= 0) {
            return "Expired";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");

        return sb.toString().trim();
    }
}