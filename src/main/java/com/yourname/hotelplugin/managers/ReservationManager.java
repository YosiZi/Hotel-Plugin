package com.yourname.hotelplugin.managers;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.Reservation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

public class ReservationManager {
    private final HotelPlugin plugin;
    private final File reservationsFile;
    private final YamlConfiguration reservationsConfig;
    private final List<Reservation> activeReservations = new ArrayList();

    public ReservationManager(HotelPlugin plugin) {
        this.plugin = plugin;
        this.reservationsFile = new File(plugin.getDataFolder(), "reservations.yml");
        this.reservationsConfig = YamlConfiguration.loadConfiguration(this.reservationsFile);
        this.loadReservations();
        this.startExpirationChecker();
    }

    private void loadReservations() {
        if (this.reservationsFile.exists()) {
            if (this.reservationsConfig.contains("reservations")) {
                for(String key : this.reservationsConfig.getConfigurationSection("reservations").getKeys(false)) {
                    String roomName = this.reservationsConfig.getString("reservations." + key + ".roomName");
                    UUID guestUUID = UUID.fromString(key);
                    long expirationTime = this.reservationsConfig.getLong("reservations." + key + ".expirationTime");
                    this.activeReservations.add(new Reservation(roomName, guestUUID, expirationTime));
                }
            }

        }
    }

    public void saveReservations() {
        this.reservationsConfig.set("reservations", (Object)null);

        for(Reservation reservation : this.activeReservations) {
            String path = "reservations." + reservation.getGuestUUID().toString();
            this.reservationsConfig.set(path + ".roomName", reservation.getRoomName());
            this.reservationsConfig.set(path + ".expirationTime", reservation.getExpirationTime());
        }

        try {
            this.reservationsConfig.save(this.reservationsFile);
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save reservations file!");
            e.printStackTrace();
        }

    }

    private void startExpirationChecker() {
        (new BukkitRunnable() {
            public void run() {
                ReservationManager.this.checkAndExpireReservations();
            }
        }).runTaskTimer(this.plugin, 1200L, 1200L);
    }

    private void checkAndExpireReservations() {
        Iterator<Reservation> iterator = this.activeReservations.iterator();

        while(iterator.hasNext()) {
            Reservation reservation = (Reservation)iterator.next();
            if (System.currentTimeMillis() > reservation.getExpirationTime()) {
                this.plugin.getHotelManager().setRoomAvailable(reservation.getRoomName());
                OfflinePlayer guest = Bukkit.getOfflinePlayer(reservation.getGuestUUID());
                if (guest.isOnline()) {
                    guest.getPlayer().sendMessage("§cYour reservation for room " + reservation.getRoomName() + " has expired.");
                }

                Logger var10000 = this.plugin.getLogger();
                String var10001 = reservation.getRoomName();
                var10000.info("Expired reservation for room " + var10001 + " (Guest: " + guest.getName() + ")");
                iterator.remove();
            }
        }

        this.saveReservations();
    }

    public boolean bookRoom(String roomName, UUID guestUUID, long durationMillis) {
        if (!this.plugin.getHotelManager().isRoomAvailable(roomName)) {
            return false;
        } else {
            long expirationTime = System.currentTimeMillis() + durationMillis;
            Reservation newReservation = new Reservation(roomName, guestUUID, expirationTime);
            this.activeReservations.add(newReservation);
            this.plugin.getHotelManager().setRoomBooked(roomName, guestUUID);
            this.saveReservations();
            return true;
        }
    }

    public boolean cancelReservation(String roomName) {
        Iterator<Reservation> iterator = this.activeReservations.iterator();

        while(iterator.hasNext()) {
            Reservation reservation = (Reservation)iterator.next();
            if (reservation.getRoomName().equalsIgnoreCase(roomName)) {
                iterator.remove();
                this.plugin.getHotelManager().setRoomAvailable(roomName);
                this.saveReservations();
                return true;
            }
        }

        return false;
    }

    public Reservation getReservation(String roomName) {
        for(Reservation reservation : this.activeReservations) {
            if (reservation.getRoomName().equalsIgnoreCase(roomName)) {
                return reservation;
            }
        }

        return null;
    }
}
