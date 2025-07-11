package com.yourname.hotelplugin.data;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.Serializable;
import java.util.UUID;
import java.util.List; // Keep this if you use it elsewhere, or remove if not needed.

public class HotelDoor implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomID;
    private Location doorLocation; // This will be the first door's location
    private Location secondDoorLocation; // NEW: Field for the second door's location
    private Location signLocation;
    private double price;
    private CurrencyType currencyType;
    private UUID owner;
    private String description;
    private BlockFace signFacing;

    private boolean occupied;
    private long rentExpiry;

    // Updated Constructor to include secondDoorLocation
    public HotelDoor(String roomID, Location doorLocation, Location secondDoorLocation, Location signLocation, double price, CurrencyType currencyType, UUID owner, String description, BlockFace signFacing) {
        this.roomID = roomID;
        this.doorLocation = doorLocation;
        this.secondDoorLocation = secondDoorLocation; // Assign the new field
        this.signLocation = signLocation;
        this.price = price;
        this.currencyType = currencyType;
        this.owner = owner;
        this.description = description;
        this.signFacing = signFacing;

        this.occupied = (owner != null);
        this.rentExpiry = 0;
    }

    // --- Getters ---
    public String getRoomID() { return roomID; }
    public Location getDoorLocation() { return doorLocation; }
    public Location getSecondDoorLocation() { return secondDoorLocation; } // NEW Getter
    public Location getSignLocation() { return signLocation; }
    public double getPrice() { return price; }
    public CurrencyType getCurrencyType() { return currencyType; }
    public UUID getOwner() { return owner; }
    public String getDescription() { return description; }
    public BlockFace getSignFacing() { return signFacing; }

    // --- Setters (if needed for the second door) ---
    public void setSecondDoorLocation(Location secondDoorLocation) {
        this.secondDoorLocation = secondDoorLocation;
    }

    // ... (rest of your existing getters and setters) ...

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public long getRentExpiry() {
        return rentExpiry;
    }

    public void setRentExpiry(long rentExpiry) {
        this.rentExpiry = rentExpiry;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.occupied = (owner != null);
    }

    public void release() {
        this.owner = null;
        this.occupied = false;
        this.rentExpiry = 0;
    }

    public void claim(UUID ownerUUID) {
        setOwner(ownerUUID);
    }

    public void unclaim() {
        release();
    }
}