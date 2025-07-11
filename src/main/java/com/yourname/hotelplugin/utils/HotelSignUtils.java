package com.yourname.hotelplugin.utils;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class HotelSignUtils {

    /**
     * Calculates the appropriate location for a wall sign in front of a door.
     * The sign is placed one block "outside" the door (based on its facing).
     *
     * @param doorLocation The location of the bottom half of the door block.
     * @param doorFacing The direction the door is facing.
     * @return The Location of the block where the sign should be placed.
     */
    public static Location getSignLocationNextToDoor(Location doorLocation, BlockFace doorFacing) {
        // Sign goes in front of the door — the direction the door opens toward
        BlockFace front = doorFacing.getOppositeFace(); // Opposite of inside = outside
        return doorLocation.getBlock().getRelative(front).getLocation();
    }

    /**
     * Returns the BlockFace a wall sign should have, facing toward the player.
     * Wall signs are mounted on solid blocks, and the face determines which side of that block the sign is on.
     *
     * @param doorFacing The direction the door is facing (from inside to outside).
     * @return The BlockFace the sign should use to face out.
     */
    public static BlockFace getSignFacingDirection(BlockFace doorFacing) {
        // Sign should face same way as the door does (so players see it when walking to the door)
        return doorFacing;
    }

    /**
     * Returns the BlockFace 90 degrees clockwise from the input.
     * Example: NORTH → EAST, EAST → SOUTH, etc.
     *
     * @param face The input direction.
     * @return The clockwise direction.
     */
    public static BlockFace getRightHandDirection(BlockFace face) {
        switch (face) {
            case NORTH: return BlockFace.EAST;
            case EAST: return BlockFace.SOUTH;
            case SOUTH: return BlockFace.WEST;
            case WEST: return BlockFace.NORTH;
            default: return BlockFace.NORTH; // Default fallback
        }
    }

    /**
     * Converts a BlockFace to a yaw angle for setting player or entity direction.
     * @param face The cardinal direction.
     * @return Yaw in degrees.
     */
    public static float getYawFromBlockFace(BlockFace face) {
        switch (face) {
            case NORTH: return 180.0f;
            case SOUTH: return 0.0f;
            case WEST: return 90.0f;
            case EAST: return -90.0f;
            default: return 0.0f;
        }
    }
}
