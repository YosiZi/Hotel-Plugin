package com.yourname.hotelplugin;

import com.yourname.hotelplugin.commands.HotelCommand;
import com.yourname.hotelplugin.commands.StaffCommand;
import com.yourname.hotelplugin.listeners.DoorSignListener;
import com.yourname.hotelplugin.managers.HotelManager;
import com.yourname.hotelplugin.managers.ReservationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class HotelPlugin extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private Economy econ = null;
    private HotelManager hotelManager;
    private ReservationManager reservationManager; // Add the new ReservationManager

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found or no economy provider!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        this.hotelManager = new HotelManager(this, econ);
        // Initialize the new ReservationManager, passing the main plugin instance
        this.reservationManager = new ReservationManager(this);

        // Schedule the actual loading of doors for the next tick
        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.hotelManager.initialize();
        }, 1L);

        // Register commands and listeners
        getCommand("hotel").setExecutor(new HotelCommand(this));
        // Register the new StaffCommand
        getCommand("hotelstaff").setExecutor(new StaffCommand(this));
        getServer().getPluginManager().registerEvents(new DoorSignListener(this), this);

        log.info(String.format("[%s] Version %s Enabled!", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        if (this.hotelManager != null) {
            this.hotelManager.saveDoors();
        }
        // Save reservations when the plugin disables
        if (this.reservationManager != null) {
            this.reservationManager.saveReservations();
        }
        log.info(String.format("[%s] Version %s Disabled!", getDescription().getName(), getDescription().getVersion()));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    public HotelManager getHotelManager() {
        return hotelManager;
    }

    // Add a public getter for the ReservationManager
    public ReservationManager getReservationManager() {
        return reservationManager;
    }
}