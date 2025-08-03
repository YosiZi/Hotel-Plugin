package com.yourname.hotelplugin.managers;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.Reservation;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages all room reservations, including loading, saving, and expiration.
 */
public class ReservationManager {

    private final HotelPlugin plugin;
    private final File reservationsFile;
    private final YamlConfiguration reservationsConfig;
    private final List<Reservation> activeReservations = new ArrayList<>();

    public ReservationManager(HotelPlugin plugin) {
        this.plugin = plugin;
        this.reservationsFile = new File(plugin.getDataFolder(), "reservations.yml");
        this.reservationsConfig = YamlConfiguration.loadConfiguration(reservationsFile);
        loadReservations();
        startExpirationChecker();
    }

    /**
     * Loads all reservations from the reservations.yml file.
     */
    private void loadReservations() {
        if (!reservationsFile.exists()) {
            return;
        }

        // Using Bukkit's configuration API to load the reservations
        if (reservationsConfig.contains("reservations")) {
            for (String key : reservationsConfig.getConfigurationSection("reservations").getKeys(false)) {
                String roomName = reservationsConfig.getString("reservations." + key + ".roomName");
                UUID guestUUID = UUID.fromString(key);
                long expirationTime = reservationsConfig.getLong("reservations." + key + ".expirationTime");

                activeReservations.add(new Reservation(roomName, guestUUID, expirationTime));
            }
        }
    }

    /**
     * Saves all active reservations to the reservations.yml file.
     */
    public void saveReservations() {
        reservationsConfig.set("reservations", null); // Clear existing data
        for (Reservation reservation : activeReservations) {
            String path = "reservations." + reservation.getGuestUUID().toString();
            reservationsConfig.set(path + ".roomName", reservation.getRoomName());
            reservationsConfig.set(path + ".expirationTime", reservation.getExpirationTime());
        }
        try {
            reservationsConfig.save(reservationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save reservations file!");
            e.printStackTrace();
        }
    }

    /**
     * Starts the scheduled task to check for expired reservations every minute.
     */
    private void startExpirationChecker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkAndExpireReservations();
            }
        }.runTaskTimer(plugin, 20 * 60L, 20 * 60L); // 20 ticks * 60 seconds = 1 minute
    }

    /**
     * Checks all active reservations and removes any that have expired.
     */
    private void checkAndExpireReservations() {
        Iterator<Reservation> iterator = activeReservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (System.currentTimeMillis() > reservation.getExpirationTime()) {
                // The reservation has expired, so we should free up the room
                plugin.getHotelManager().setRoomAvailable(reservation.getRoomName());

                OfflinePlayer guest = Bukkit.getOfflinePlayer(reservation.getGuestUUID());
                if (guest.isOnline()) {
                    guest.getPlayer().sendMessage("§cYour reservation for room " + reservation.getRoomName() + " has expired.");
                }

                plugin.getLogger().info("Expired reservation for room " + reservation.getRoomName() + " (Guest: " + guest.getName() + ")");
                iterator.remove(); // Remove the expired reservation from the list
            }
        }
        saveReservations(); // Save the changes
    }

    /**
     * Books a room for a player.
     * @param roomName The name of the room to book.
     * @param guestUUID The UUID of the player to book for.
     * @param durationMillis The duration of the reservation in milliseconds.
     * @return true if the booking was successful, false otherwise.
     */
    public boolean bookRoom(String roomName, UUID guestUUID, long durationMillis) {
        // You'll need to implement logic to check if the room is available
        // This is a placeholder for that logic
        if (!plugin.getHotelManager().isRoomAvailable(roomName)) {
            return false;
        }

        long expirationTime = System.currentTimeMillis() + durationMillis;
        Reservation newReservation = new Reservation(roomName, guestUUID, expirationTime);
        activeReservations.add(newReservation);
        plugin.getHotelManager().setRoomBooked(roomName, guestUUID); // Assume this method exists
        saveReservations();
        return true;
    }

    /**
     * Cancels an existing reservation for a room.
     * @param roomName The name of the room to cancel the reservation for.
     * @return true if the reservation was found and cancelled, false otherwise.
     */
    public boolean cancelReservation(String roomName) {
        Iterator<Reservation> iterator = activeReservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getRoomName().equalsIgnoreCase(roomName)) {
                iterator.remove();
                plugin.getHotelManager().setRoomAvailable(roomName);
                saveReservations();
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the reservation for a given room.
     * @param roomName The name of the room to check.
     * @return The Reservation object if found, otherwise null.
     */
    public Reservation getReservation(String roomName) {
        for (Reservation reservation : activeReservations) {
            if (reservation.getRoomName().equalsIgnoreCase(roomName)) {
                return reservation;
            }
        }
        return null;
    }
}