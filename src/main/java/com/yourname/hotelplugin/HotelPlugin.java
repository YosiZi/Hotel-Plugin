package com.yourname.hotelplugin;

import com.yourname.hotelplugin.commands.HotelCommand;
import com.yourname.hotelplugin.commands.StaffCommand;
import com.yourname.hotelplugin.gui.HotelGUIListener;
import com.yourname.hotelplugin.listeners.DoorSignListener;
import com.yourname.hotelplugin.managers.HotelManager;
import com.yourname.hotelplugin.managers.ReservationManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class HotelPlugin extends JavaPlugin {

    private Economy econ = null;
    private HotelManager hotelManager;
    private ReservationManager reservationManager;

    @Override
    public void onEnable() {
        // Load config
        saveDefaultConfig();

        // Vault / Economy setup
        if (!setupEconomy()) {
            getLogger().severe(String.format(
                    "[%s] - Disabled due to no Vault dependency found or no economy provider!",
                    getDescription().getName()
            ));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Managers
        this.hotelManager = new HotelManager(this, this.econ);
        this.reservationManager = new ReservationManager(this);

        // Delay door load by 1 tick so worlds are ready
        Bukkit.getScheduler().runTaskLater(this, () -> this.hotelManager.initialize(), 1L);

        // Commands
        getCommand("hotel").setExecutor(new HotelCommand(this));
        getCommand("hotelstaff").setExecutor(new StaffCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new DoorSignListener(this), this);
        getServer().getPluginManager().registerEvents(new HotelGUIListener(this), this);

        getLogger().info(String.format(
                "[%s] Version %s Enabled!",
                getDescription().getName(),
                getDescription().getVersion()
        ));
    }

    @Override
    public void onDisable() {
        if (this.hotelManager != null) {
            this.hotelManager.saveDoors();
        }

        if (this.reservationManager != null) {
            this.reservationManager.saveReservations();
        }

        getLogger().info(String.format(
                "[%s] Version %s Disabled!",
                getDescription().getName(),
                getDescription().getVersion()
        ));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) {
            return false;
        }

        this.econ = rsp.getProvider();
        return this.econ != null;
    }

    public Economy getEconomy() {
        return this.econ;
    }

    public HotelManager getHotelManager() {
        return this.hotelManager;
    }

    public ReservationManager getReservationManager() {
        return this.reservationManager;
    }
}