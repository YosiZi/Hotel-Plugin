package com.yourname.hotelplugin.data;

import java.io.Serializable;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;
    private String roomName;
    private Location doorLocation;
    private BlockFace doorFacing;

    public Room(String roomName, Location doorLocation, BlockFace doorFacing) {
        this.roomName = roomName;
        this.doorLocation = doorLocation;
        this.doorFacing = doorFacing;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public Location getDoorLocation() {
        return this.doorLocation;
    }

    public BlockFace getDoorFacing() {
        return this.doorFacing;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setDoorLocation(Location doorLocation) {
        this.doorLocation = doorLocation;
    }

    public void setDoorFacing(BlockFace doorFacing) {
        this.doorFacing = doorFacing;
    }
}
