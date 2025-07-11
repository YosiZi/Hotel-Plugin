package com.yourname.hotelplugin;

import com.yourname.hotelplugin.commands.HotelCommand;
import com.yourname.hotelplugin.listeners.DoorSignListener;
import com.yourname.hotelplugin.managers.HotelManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit; // Import Bukkit for scheduler
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class HotelPlugin extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private Economy econ = null;
    private HotelManager hotelManager;

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();

        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found or no economy provider!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return; // Crucially, return here if setupEconomy fails
        }

        // Initialize HotelManager (constructor no longer calls loadDoors)
        this.hotelManager = new HotelManager(this, econ);

        // Schedule the actual loading of doors for the next tick
        // This ensures all worlds are loaded before trying to get locations
        Bukkit.getScheduler().runTaskLater(this, () -> {
            this.hotelManager.initialize(); // Call the new initialize method
        }, 1L); // 1L means 1 tick later

        // Register commands and listeners
        getCommand("hotel").setExecutor(new HotelCommand(this));
        getServer().getPluginManager().registerEvents(new DoorSignListener(this), this);

        log.info(String.format("[%s] Version %s Enabled!", getDescription().getName(), getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        // Save all hotel doors data when the plugin disables
        // Only try to save if hotelManager was successfully initialized
        if (this.hotelManager != null) {
            this.hotelManager.saveDoors();
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
}