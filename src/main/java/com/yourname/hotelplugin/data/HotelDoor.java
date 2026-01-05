package com.yourname.hotelplugin.data;

import java.io.Serializable;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class HotelDoor implements Serializable {
    private static final long serialVersionUID = 1L;
    private String roomID;
    private Location doorLocation;
    private Location secondDoorLocation;
    private Location signLocation;
    private double price;
    private CurrencyType currencyType;
    private UUID owner;
    private String description;
    private BlockFace signFacing;
    private boolean occupied;
    private long rentExpiry;
    private String hotelId;

    public HotelDoor(String hotelId, String roomID, Location doorLocation, Location secondDoorLocation, Location signLocation, double price, CurrencyType currencyType, UUID owner, String description, BlockFace signFacing) {
        this.hotelId = hotelId;
        this.roomID = roomID;
        this.doorLocation = doorLocation;
        this.secondDoorLocation = secondDoorLocation;
        this.signLocation = signLocation;
        this.price = price;
        this.currencyType = currencyType;
        this.owner = owner;
        this.description = description;
        this.signFacing = signFacing;
        this.occupied = owner != null;
        this.rentExpiry = 0L;
    }

    public String getRoomID() {
        return this.roomID;
    }

    public Location getDoorLocation() {
        return this.doorLocation;
    }

    public Location getSecondDoorLocation() {
        return this.secondDoorLocation;
    }

    public Location getSignLocation() {
        return this.signLocation;
    }

    public double getPrice() {
        return this.price;
    }

    public CurrencyType getCurrencyType() {
        return this.currencyType;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public String getDescription() {
        return this.description;
    }

    public BlockFace getSignFacing() {
        return this.signFacing;
    }

    public String getHotelId() {
        return this.hotelId;
    }

    public boolean isOccupied() {
        return this.occupied;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.occupied = owner != null;
    }

    public void claim(UUID ownerUUID) {
        this.setOwner(ownerUUID);
    }

    public void unclaim() {
        this.owner = null;
        this.occupied = false;
        this.rentExpiry = 0L;
    }
}
