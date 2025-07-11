package com.yourname.hotelplugin.data;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.io.Serializable; // Consider if you're saving to flat files (e.g., config.yml)

public class Room implements Serializable { // Serializable for easier saving if using config.yml/Object streams

    private static final long serialVersionUID = 1L; // For Serializable

    private String roomName;
    private Location doorLocation;
    private BlockFace doorFacing;
    // Add other room properties as needed (e.g., price, owner, rented status, boundaries)

    public Room(String roomName, Location doorLocation, BlockFace doorFacing) {
        this.roomName = roomName;
        this.doorLocation = doorLocation;
        this.doorFacing = doorFacing;
    }

    // Getters
    public String getRoomName() {
        return roomName;
    }

    public Location getDoorLocation() {
        return doorLocation;
    }

    public BlockFace getDoorFacing() {
        return doorFacing;
    }

    // Setters (if rooms can be modified after creation)
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setDoorLocation(Location doorLocation) {
        this.doorLocation = doorLocation;
    }

    public void setDoorFacing(BlockFace doorFacing) {
        this.doorFacing = doorFacing;
    }

    // You'll need to implement your own serialization/deserialization logic
    // if saving to config.yml, as Location and BlockFace aren't directly savable.
    // For example, convert Location to string: "world,x,y,z" and BlockFace to string: "NORTH"
}