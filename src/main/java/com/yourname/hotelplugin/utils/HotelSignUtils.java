package com.yourname.hotelplugin.utils;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class HotelSignUtils {

    public static Location getSignLocationNextToDoor(Location doorLocation, BlockFace doorFacing) {
        BlockFace front = doorFacing.getOppositeFace();
        return doorLocation.getBlock().getRelative(front).getLocation();
    }

    public static BlockFace getSignFacingDirection(BlockFace doorFacing) {
        return doorFacing;
    }

    public static BlockFace getRightHandDirection(BlockFace face) {
        if (face == null) {
            return BlockFace.NORTH;
        }

        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
            default:
                // For UP, DOWN, etc. – pick a sensible default
                return BlockFace.NORTH;
        }
    }

    public static float getYawFromBlockFace(BlockFace face) {
        if (face == null) {
            return 0.0F;
        }

        switch (face) {
            case NORTH:
                return 180.0F;
            case EAST:
                return -90.0F;
            case SOUTH:
                return 0.0F;
            case WEST:
                return 90.0F;
            default:
                // For other directions, just default to 0
                return 0.0F;
        }
    }
}
