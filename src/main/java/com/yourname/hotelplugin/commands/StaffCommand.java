package com.yourname.hotelplugin.commands;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.Reservation;
import com.yourname.hotelplugin.managers.ReservationManager;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StaffCommand implements CommandExecutor {
    private final HotelPlugin plugin;
    private final ReservationManager reservationManager;

    public StaffCommand(HotelPlugin plugin) {
        this.plugin = plugin;
        this.reservationManager = plugin.getReservationManager();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "This command can only be used by players for now.");
            return true;
        } else if (!player.hasPermission("hotel.staff")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to use staff commands.");
            return true;
        } else if (args.length == 0) {
            this.showHelp(player);
            return true;
        } else {
            switch (args[0].toLowerCase()) {
                case "book" -> this.handleBookCommand(player, args);
                case "cancel" -> this.handleCancelCommand(player, args);
                case "check" -> this.handleCheckCommand(player, args);
                default -> this.showHelp(player);
            }

            return true;
        }
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
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to book rooms for others.");
        } else if (args.length < 4) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotelstaff book <player> <room> <duration>");
        } else {
            String playerName = args[1];
            String roomName = args[2];
            String durationString = args[3];
            OfflinePlayer guest = Bukkit.getOfflinePlayer(playerName);
            if (guest != null && guest.hasPlayedBefore()) {
                long durationMillis = this.parseDuration(durationString);
                if (durationMillis <= 0L) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Invalid duration format. Use a number followed by 'h' for hours or 'd' for days (e.g., 5h, 1d).");
                } else {
                    if (this.reservationManager.bookRoom(roomName, guest.getUniqueId(), durationMillis)) {
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Successfully booked room " + roomName + " for " + playerName + ".");
                        if (guest.isOnline()) {
                            Player var10000 = guest.getPlayer();
                            String var9 = String.valueOf(ChatColor.GREEN);
                            var10000.sendMessage(var9 + "A staff member has booked room " + roomName + " for you.");
                        }
                    } else {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Failed to book room. It may not exist or is already reserved.");
                    }

                }
            } else {
                String var10001 = String.valueOf(ChatColor.RED);
                player.sendMessage(var10001 + "Player '" + playerName + "' has not played on this server before.");
            }
        }
    }

    private void handleCancelCommand(Player player, String[] args) {
        if (!player.hasPermission("hotel.staff.cancel")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to cancel reservations.");
        } else if (args.length < 2) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotelstaff cancel <room>");
        } else {
            String roomName = args[1];
            if (this.reservationManager.cancelReservation(roomName)) {
                String var10001 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10001 + "Successfully cancelled the reservation for room " + roomName + ".");
            } else {
                String var4 = String.valueOf(ChatColor.RED);
                player.sendMessage(var4 + "No active reservation found for room " + roomName + ".");
            }

        }
    }

    private void handleCheckCommand(Player player, String[] args) {
        if (!player.hasPermission("hotel.staff.check")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to check room status.");
        } else if (args.length < 2) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotelstaff check <room>");
        } else {
            String roomName = args[1];
            Reservation reservation = this.reservationManager.getReservation(roomName);
            if (reservation != null) {
                OfflinePlayer guest = Bukkit.getOfflinePlayer(reservation.getGuestUUID());
                long remainingTime = reservation.getExpirationTime() - System.currentTimeMillis();
                String var10001 = String.valueOf(ChatColor.YELLOW);
                player.sendMessage(var10001 + "--- Room " + roomName + " Status ---");
                var10001 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10001 + "Status: " + String.valueOf(ChatColor.DARK_GREEN) + "Reserved");
                var10001 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10001 + "Guest: " + String.valueOf(ChatColor.YELLOW) + guest.getName());
                var10001 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var10001 + "Time remaining: " + String.valueOf(ChatColor.YELLOW) + this.formatTime(remainingTime));
            } else {
                String var11 = String.valueOf(ChatColor.YELLOW);
                player.sendMessage(var11 + "--- Room " + roomName + " Status ---");
                var11 = String.valueOf(ChatColor.GREEN);
                player.sendMessage(var11 + "Status: " + String.valueOf(ChatColor.GREEN) + "Available");
            }

        }
    }

    private long parseDuration(String durationString) {
        if (durationString != null && !durationString.isEmpty()) {
            String unit = durationString.substring(durationString.length() - 1).toLowerCase();
            String numberString = durationString.substring(0, durationString.length() - 1);

            long number;
            try {
                number = Long.parseLong(numberString);
            } catch (NumberFormatException var8) {
                return -1L;
            }

            switch (unit) {
                case "h" -> {
                    return TimeUnit.HOURS.toMillis(number);
                }
                case "d" -> {
                    return TimeUnit.DAYS.toMillis(number);
                }
                default -> {
                    return -1L;
                }
            }
        } else {
            return -1L;
        }
    }

    private String formatTime(long millis) {
        if (millis <= 0L) {
            return "Expired";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(millis);
            millis -= TimeUnit.DAYS.toMillis(days);
            long hours = TimeUnit.MILLISECONDS.toHours(millis);
            millis -= TimeUnit.HOURS.toMillis(hours);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
            long var10000 = millis - TimeUnit.MINUTES.toMillis(minutes);
            StringBuilder sb = new StringBuilder();
            if (days > 0L) {
                sb.append(days).append("d ");
            }

            if (hours > 0L) {
                sb.append(hours).append("h ");
            }

            if (minutes > 0L) {
                sb.append(minutes).append("m ");
            }

            return sb.toString().trim();
        }
    }
}
