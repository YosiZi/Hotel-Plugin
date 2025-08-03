package com.yourname.hotelplugin.managers;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import net.milkbowl.vault.economy.Economy;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class HotelManager {

    private final HotelPlugin plugin;
    private Map<String, HotelDoor> hotelDoors;
    private File doorsFile;
    private FileConfiguration doorsConfig;
    private final Economy economy;

    public HotelManager(HotelPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.hotelDoors = new HashMap<>();
        this.doorsFile = new File(plugin.getDataFolder(), "doors.yml");
        this.doorsConfig = YamlConfiguration.loadConfiguration(doorsFile);
    }

    public void initialize() {
        loadDoors();
    }

    public void loadDoors() {
        hotelDoors.clear();
        if (!doorsFile.exists()) {
            plugin.getLogger().info("doors.yml does not exist. No doors to load.");
            return;
        }
        try {
            doorsConfig.load(doorsFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            plugin.getLogger().severe("Could not load doors.yml: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        if (doorsConfig.isConfigurationSection("rooms")) {
            for (String roomId : doorsConfig.getConfigurationSection("rooms").getKeys(false)) {
                try {
                    // Load first door location
                    String worldName = doorsConfig.getString("rooms." + roomId + ".doorLocation.world");
                    double x = doorsConfig.getDouble("rooms." + roomId + ".doorLocation.x");
                    double y = doorsConfig.getDouble("rooms." + roomId + ".doorLocation.y");
                    double z = doorsConfig.getDouble("rooms." + roomId + ".doorLocation.z");
                    if (Bukkit.getWorld(worldName) == null) {
                        plugin.getLogger().warning("World '" + worldName + "' for room " + roomId + " not found. Skipping. (This might happen during early server startup)");
                        continue;
                    }
                    Location doorLoc = new Location(Objects.requireNonNull(Bukkit.getWorld(worldName)), x, y, z);

                    // NEW: Load second door location (can be null)
                    Location secondDoorLoc = null;
                    if (doorsConfig.isSet("rooms." + roomId + ".secondDoorLocation")) {
                        String secondDoorWorldName = doorsConfig.getString("rooms." + roomId + ".secondDoorLocation.world");
                        double sx2 = doorsConfig.getDouble("rooms." + roomId + ".secondDoorLocation.x");
                        double sy2 = doorsConfig.getDouble("rooms." + roomId + ".secondDoorLocation.y");
                        double sz2 = doorsConfig.getDouble("rooms." + roomId + ".secondDoorLocation.z");
                        if (Bukkit.getWorld(secondDoorWorldName) != null) {
                            secondDoorLoc = new Location(Objects.requireNonNull(Bukkit.getWorld(secondDoorWorldName)), sx2, sy2, sz2);
                        } else {
                            plugin.getLogger().warning("World '" + secondDoorWorldName + "' for second door of room " + roomId + " not found. Second door will not be loaded.");
                        }
                    }

                    // Load sign location
                    String signWorld = doorsConfig.getString("rooms." + roomId + ".signLocation.world");
                    double sx = doorsConfig.getDouble("rooms." + roomId + ".signLocation.x");
                    double sy = doorsConfig.getDouble("rooms." + roomId + ".signLocation.y");
                    double sz = doorsConfig.getDouble("rooms." + roomId + ".signLocation.z");
                    if (Bukkit.getWorld(signWorld) == null) {
                        plugin.getLogger().warning("World '" + signWorld + "' for sign of room " + roomId + " not found. Skipping. (This might happen during early server startup)");
                        continue;
                    }
                    Location signLoc = new Location(Objects.requireNonNull(Bukkit.getWorld(signWorld)), sx, sy, sz);

                    double price = doorsConfig.getDouble("rooms." + roomId + ".price");

                    String currencyTypeStr = doorsConfig.getString("rooms." + roomId + ".currencyType");
                    CurrencyType currencyType = null;
                    if (currencyTypeStr != null) {
                        try {
                            currencyType = CurrencyType.valueOf(currencyTypeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid currency type '" + currencyTypeStr + "' for room " + roomId + ". Setting to default (CurrencyType.VAULT).");
                            currencyType = CurrencyType.VAULT;
                        }
                    } else {
                        plugin.getLogger().warning("No currency type found for room " + roomId + ". Setting to default (CurrencyType.VAULT).");
                        currencyType = CurrencyType.VAULT;
                    }

                    String ownerUuidStr = doorsConfig.getString("rooms." + roomId + ".owner", null);
                    UUID owner = ownerUuidStr != null ? UUID.fromString(ownerUuidStr) : null;

                    String description = doorsConfig.getString("rooms." + roomId + ".description", "");
                    String signFacingStr = doorsConfig.getString("rooms." + roomId + ".signFacing");
                    BlockFace signFacing = (signFacingStr != null) ? BlockFace.valueOf(signFacingStr) : BlockFace.NORTH;

                    // Pass both door locations to the constructor
                    HotelDoor hotelDoor = new HotelDoor(roomId, doorLoc, secondDoorLoc, signLoc, price, currencyType, owner, description, signFacing);

                    hotelDoors.put(roomId, hotelDoor);
                    updateSign(hotelDoor);
                } catch (Exception e) {
                    plugin.getLogger().severe("Error loading room " + roomId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        plugin.getLogger().info("Loaded " + hotelDoors.size() + " hotel doors.");
    }

    public void saveDoors() {
        doorsConfig.set("rooms", null); // Clear old data before saving new
        for (HotelDoor door : hotelDoors.values()) {
            String path = "rooms." + door.getRoomID();
            // Save first door location
            doorsConfig.set(path + ".doorLocation.world", door.getDoorLocation().getWorld().getName());
            doorsConfig.set(path + ".doorLocation.x", door.getDoorLocation().getX());
            doorsConfig.set(path + ".doorLocation.y", door.getDoorLocation().getY());
            doorsConfig.set(path + ".doorLocation.z", door.getDoorLocation().getZ());

            // NEW: Save second door location if it exists
            if (door.getSecondDoorLocation() != null) {
                doorsConfig.set(path + ".secondDoorLocation.world", door.getSecondDoorLocation().getWorld().getName());
                doorsConfig.set(path + ".secondDoorLocation.x", door.getSecondDoorLocation().getX());
                doorsConfig.set(path + ".secondDoorLocation.y", door.getSecondDoorLocation().getY());
                doorsConfig.set(path + ".secondDoorLocation.z", door.getSecondDoorLocation().getZ());
            } else {
                doorsConfig.set(path + ".secondDoorLocation", null); // Ensure it's not present if null
            }

            doorsConfig.set(path + ".signLocation.world", door.getSignLocation().getWorld().getName());
            doorsConfig.set(path + ".signLocation.x", door.getSignLocation().getX());
            doorsConfig.set(path + ".signLocation.y", door.getSignLocation().getY());
            doorsConfig.set(path + ".signLocation.z", door.getSignLocation().getZ());

            doorsConfig.set(path + ".price", door.getPrice());
            doorsConfig.set(path + ".currencyType", door.getCurrencyType().name());
            doorsConfig.set(path + ".owner", door.getOwner() != null ? door.getOwner().toString() : null);
            doorsConfig.set(path + ".description", door.getDescription());
            doorsConfig.set(path + ".signFacing", door.getSignFacing().name());
        }
        try {
            doorsConfig.save(doorsFile);
            plugin.getLogger().info("Saved " + hotelDoors.size() + " hotel doors.");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save doors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addHotelDoor(HotelDoor door) {
        hotelDoors.put(door.getRoomID(), door);
        saveDoors();
        updateSign(door);
    }

    public HotelDoor getHotelDoor(String roomId) {
        return hotelDoors.get(roomId);
    }

    public HotelDoor getHotelDoorBySignLocation(Location signLoc) {
        for (HotelDoor door : hotelDoors.values()) {
            if (door.getSignLocation() != null &&
                    door.getSignLocation().getBlockX() == signLoc.getBlockX() &&
                    door.getSignLocation().getBlockY() == signLoc.getBlockY() &&
                    door.getSignLocation().getBlockZ() == signLoc.getBlockZ() &&
                    door.getSignLocation().getWorld().equals(signLoc.getWorld())) {
                return door;
            }
        }
        return null;
    }

    /**
     * Finds a HotelDoor associated with a specific door block location (handles double doors and two separate doors).
     * @param doorLoc The location of the door block clicked.
     * @return The HotelDoor object, or null if no door is linked to this block.
     */
    public HotelDoor getHotelDoorByDoorLocation(Location doorLoc) {
        for (HotelDoor door : hotelDoors.values()) {
            // Check the first door location
            if (door.getDoorLocation() != null &&
                    ( (door.getDoorLocation().getBlockX() == doorLoc.getBlockX() &&
                            door.getDoorLocation().getBlockY() == doorLoc.getBlockY() &&
                            door.getDoorLocation().getBlockZ() == doorLoc.getBlockZ() &&
                            door.getDoorLocation().getWorld().equals(doorLoc.getWorld())) ||
                            // Check for the upper half of the first double door
                            (door.getDoorLocation().clone().add(0, 1, 0).getBlockX() == doorLoc.getBlockX() &&
                                    door.getDoorLocation().clone().add(0, 1, 0).getBlockY() == doorLoc.getBlockY() &&
                                    door.getDoorLocation().clone().add(0, 1, 0).getBlockZ() == doorLoc.getBlockZ() &&
                                    door.getDoorLocation().clone().add(0, 1, 0).getWorld().equals(doorLoc.getWorld()))
                    )
            ) {
                return door;
            }

            // NEW: Check the second door location
            if (door.getSecondDoorLocation() != null &&
                    ( (door.getSecondDoorLocation().getBlockX() == doorLoc.getBlockX() &&
                            door.getSecondDoorLocation().getBlockY() == doorLoc.getBlockY() &&
                            door.getSecondDoorLocation().getBlockZ() == doorLoc.getBlockZ() &&
                            door.getSecondDoorLocation().getWorld().equals(doorLoc.getWorld())) ||
                            // Check for the upper half of the second double door
                            (door.getSecondDoorLocation().clone().add(0, 1, 0).getBlockX() == doorLoc.getBlockX() &&
                                    door.getSecondDoorLocation().clone().add(0, 1, 0).getBlockY() == doorLoc.getBlockY() &&
                                    door.getSecondDoorLocation().clone().add(0, 1, 0).getBlockZ() == doorLoc.getBlockZ() &&
                                    door.getSecondDoorLocation().clone().add(0, 1, 0).getWorld().equals(doorLoc.getWorld()))
                    )
            ) {
                return door; // Return the same HotelDoor if the second door matches
            }
        }
        return null;
    }

    public void removeHotelDoor(String roomId) {
        HotelDoor door = hotelDoors.get(roomId);
        if (door != null) {
            removeSign(door.getSignLocation());
            hotelDoors.remove(roomId);
            saveDoors();
        }
    }

    public Collection<HotelDoor> getAllHotelDoors() {
        return hotelDoors.values();
    }

    @SuppressWarnings("deprecation")
    public void updateSign(HotelDoor door) {
        Location signLoc = door.getSignLocation();
        plugin.getLogger().info("DEBUG: updateSign - Called for room " + door.getRoomID() + " at " + (signLoc != null ? signLoc.toString() : "null"));
        if (signLoc == null || signLoc.getWorld() == null || !signLoc.getChunk().isLoaded()) {
            plugin.getLogger().info("DEBUG: updateSign - Initial check failed for room " + door.getRoomID() + " (SignLoc: " + (signLoc == null ? "null" : signLoc.toString()) + ", World: " + (signLoc != null && signLoc.getWorld() == null ? "null" : "loaded") + ", Chunk Loaded: " + (signLoc != null && signLoc.getChunk() != null ? signLoc.getChunk().isLoaded() : "N/A") + ")");
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("DEBUG: updateSign - Running scheduled task for room " + door.getRoomID() + " at " + signLoc.toString());
            Block block = signLoc.getBlock();
            BlockFace attachedFace = door.getSignFacing();

            if (attachedFace == null) {
                plugin.getLogger().warning("DEBUG: updateSign - Sign facing not set for room " + door.getRoomID() + ". Defaulting to NORTH.");
                attachedFace = BlockFace.NORTH;
            }

            Block blockToAttachTo = block.getRelative(attachedFace.getOppositeFace());
            plugin.getLogger().info("DEBUG: updateSign - Sign block type: " + block.getType().name() + ", Block to attach to type: " + blockToAttachTo.getType().name());

            if (!blockToAttachTo.getType().isSolid()) {
                plugin.getLogger().warning("DEBUG: updateSign - Block to attach sign to at " + blockToAttachTo.getLocation().toString() + " for room " + door.getRoomID() + " is not solid (" + blockToAttachTo.getType().name() + "). Cannot create wall sign.");
                return;
            }

            boolean isCorrectSign = (block.getState() instanceof Sign) && block.getType() == Material.OAK_WALL_SIGN;
            if (isCorrectSign && block.getBlockData() instanceof WallSign) {
                WallSign currentWallSignData = (WallSign) block.getBlockData();
                if (currentWallSignData.getFacing() != attachedFace) {
                    isCorrectSign = false;
                }
            } else {
                isCorrectSign = false;
            }

            if (!isCorrectSign) {
                if (!block.getType().isAir()) {
                    plugin.getLogger().warning("DEBUG: updateSign - Sign location " + signLoc.toString() + " for room " + door.getRoomID() + " is occupied by a non-air block (" + block.getType().name() + ") which is not the correct sign type/facing. Attempting to overwrite.");
                }

                block.setType(Material.OAK_WALL_SIGN);
                plugin.getLogger().info("DEBUG: updateSign - Set block type to OAK_WALL_SIGN for room " + door.getRoomID());
                if (block.getBlockData() instanceof WallSign) {
                    WallSign wallSignData = (WallSign) block.getBlockData();
                    wallSignData.setFacing(attachedFace);
                    block.setBlockData(wallSignData);
                    plugin.getLogger().info("DEBUG: updateSign - Set wall sign data for room " + door.getRoomID() + " facing " + attachedFace.name());
                } else {
                    plugin.getLogger().warning("DEBUG: updateSign - Failed to cast blockData to WallSign at " + signLoc.toString() + " for room " + door.getRoomID() + ". Material " + block.getType().name() + " might not support WallSign data.");
                    return;
                }
            }

            if (!(block.getState() instanceof Sign)) {
                plugin.getLogger().warning("DEBUG: updateSign - Sign state could not be retrieved at " + signLoc.toString() + " for room " + door.getRoomID() + " after setting block type. Aborting sign update.");
                return;
            }

            Sign sign = (Sign) block.getState();
            plugin.getLogger().info("DEBUG: updateSign - Retrieved sign state for room " + door.getRoomID());

            sign.setLine(0, ChatColor.DARK_BLUE + "[Hotel Room]");
            sign.setLine(1, ChatColor.GOLD + "ID: " + door.getRoomID());

            if (door.isOccupied()) {
                String ownerName = "Unknown";
                if (door.getOwner() != null) {
                    OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(door.getOwner());
                    if (ownerPlayer != null && ownerPlayer.hasPlayedBefore()) {
                        ownerName = ownerPlayer.getName();
                    } else {
                        ownerName = door.getOwner().toString().substring(0, 8) + "...";
                    }
                }
                sign.setLine(2, ChatColor.RED + "OWNED BY:");
                sign.setLine(3, ChatColor.RED + ownerName);
            } else {
                String description = door.getDescription();
                String[] descLines = splitDescription(description, 15);

                if (descLines.length > 0 && !descLines[0].isEmpty()) {
                    sign.setLine(2, ChatColor.LIGHT_PURPLE + descLines[0]);
                    if (descLines.length > 1 && !descLines[1].isEmpty()) {
                        sign.setLine(3, ChatColor.LIGHT_PURPLE + descLines[1]);
                    } else {
                        sign.setLine(3, ChatColor.GREEN + "Available!");
                    }
                } else {
                    // Corrected currency display logic
                    String formattedPrice = (economy != null && door.getCurrencyType() == CurrencyType.VAULT)
                            ? economy.format(door.getPrice())
                            : String.valueOf((int) door.getPrice()) + " " + door.getCurrencyType().getDisplayName(); // Assuming getDisplayName() exists

                    sign.setLine(2, ChatColor.GOLD + "Price: " + formattedPrice);
                    sign.setLine(3, ChatColor.GREEN + "Available!");
                }
            }

            sign.update(true);
            plugin.getLogger().info("DEBUG: updateSign - Sign update called for room " + door.getRoomID());
        }, 1L);
    }

    public void removeSign(Location signLoc) {
        if (signLoc == null || signLoc.getWorld() == null || !signLoc.getChunk().isLoaded()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (signLoc.getBlock().getState() instanceof Sign) {
                signLoc.getBlock().setType(Material.AIR);
            }
        }, 1L);
    }

    public Economy getEconomy() {
        return economy;
    }

    private String[] splitDescription(String description, int maxLenPerLine) {
        if (description == null || description.isEmpty()) {
            return new String[]{};
        }
        if (description.length() <= maxLenPerLine) {
            return new String[]{description};
        }

        int breakPoint = -1;
        for (int i = maxLenPerLine; i >= 0; i--) {
            if (i < description.length() && description.charAt(i) == ' ') {
                breakPoint = i;
                break;
            }
        }

        if (breakPoint == -1) {
            String firstLine = description.substring(0, Math.min(maxLenPerLine, description.length()));
            String secondLine = (description.length() > maxLenPerLine) ? description.substring(maxLenPerLine) : "";
            return new String[]{firstLine, secondLine};
        } else {
            return new String[]{description.substring(0, breakPoint), description.substring(breakPoint + 1)};
        }
    }

    /**
     * Opens all physical doors associated with a HotelDoor room.
     * @param door The HotelDoor object.
     */
    public void openRoomDoors(HotelDoor door) {
        // Open primary door
        if (door.getDoorLocation() != null) {
            setDoorOpenState(door.getDoorLocation(), true);
        }
        // Open secondary door if it exists
        if (door.getSecondDoorLocation() != null) {
            setDoorOpenState(door.getSecondDoorLocation(), true);
        }
    }

    /**
     * Closes all physical doors associated with a HotelDoor room.
     * @param door The HotelDoor object.
     */
    public void closeRoomDoors(HotelDoor door) {
        // Close primary door
        if (door.getDoorLocation() != null) {
            setDoorOpenState(door.getDoorLocation(), false);
        }
        // Close secondary door if it exists
        if (door.getSecondDoorLocation() != null) {
            setDoorOpenState(door.getSecondDoorLocation(), false);
        }
    }

    /**
     * Helper method to set the open/closed state of a single door block.
     * @param loc The location of the door's bottom half block.
     * @param open True to open the door, false to close.
     */
    private void setDoorOpenState(Location loc, boolean open) {
        // Ensure the chunk is loaded before interacting with the block
        if (loc == null || loc.getWorld() == null || !loc.getChunk().isLoaded()) {
            plugin.getLogger().warning("Attempted to set door state for unloaded or null location: " + (loc != null ? loc.toString() : "null"));
            return;
        }

        Block block = loc.getBlock();
        if (block.getBlockData() instanceof Door) {
            Door doorData = (Door) block.getBlockData();

            // Only act on the bottom half of the door to avoid issues with double-height doors
            // Bukkit typically handles the top half automatically if you modify the bottom.
            if (doorData.getHalf() == Bisected.Half.BOTTOM) {
                if (doorData.isOpen() != open) { // Only update if state needs to change
                    doorData.setOpen(open);
                    block.setBlockData(doorData, true); // Use true for physics
                    plugin.getLogger().info("DEBUG: Door at " + loc.toString() + " set to " + (open ? "OPEN" : "CLOSED"));
                }
            } else {
                // If it's the top half of a door, we should ensure we're targeting the bottom half.
                // This scenario shouldn't happen if getHotelDoorByDoorLocation correctly finds the bottom half.
                // However, as a safeguard, you could log it or try to find the bottom half here
                plugin.getLogger().warning("Attempted to set state on top half of door at " + loc.toString() + ". Ensure you target the bottom half.");
            }
        } else {
            plugin.getLogger().warning("Block at " + loc.toString() + " is not a door type (" + block.getType().name() + "). Cannot set door state.");
        }
    }

    // --- NEW METHODS FOR RESERVATION MANAGEMENT ---

    /**
     * Checks if a room is currently available for reservation.
     * A room is considered available if it has no owner.
     * @param roomId The ID of the room to check.
     * @return true if the room exists and is available, false otherwise.
     */
    public boolean isRoomAvailable(String roomId) {
        HotelDoor door = getHotelDoor(roomId);
        // A room is available if it exists and is not currently occupied
        return door != null && !door.isOccupied();
    }

    /**
     * Marks a room as reserved for a specific player.
     * @param roomId The ID of the room to book.
     * @param guestUUID The UUID of the player who the room is being booked for.
     */
    public void setRoomBooked(String roomId, UUID guestUUID) {
        HotelDoor door = getHotelDoor(roomId);
        if (door != null) {
            door.setOwner(guestUUID);
            updateSign(door);
            saveDoors();
        }
    }

    /**
     * Marks a room as available again by clearing its owner.
     * @param roomId The ID of the room to free up.
     */
    public void setRoomAvailable(String roomId) {
        HotelDoor door = getHotelDoor(roomId);
        if (door != null) {
            door.setOwner(null);
            updateSign(door);
            saveDoors();
        }
    }
}
