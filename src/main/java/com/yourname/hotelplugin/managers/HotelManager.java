package com.yourname.hotelplugin.managers;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class HotelManager {
    private final HotelPlugin plugin;
    private final Economy economy;
    private Map<String, HotelDoor> hotelDoors;
    private File doorsFile;
    private FileConfiguration doorsConfig;

    public HotelManager(HotelPlugin plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.hotelDoors = new HashMap<>();
        this.doorsFile = new File(plugin.getDataFolder(), "doors.yml");
        this.doorsConfig = YamlConfiguration.loadConfiguration(this.doorsFile);
    }

    private String makeKey(String hotelId, String roomId) {
        return (hotelId + "|" + roomId).toLowerCase(Locale.ROOT);
    }

    public void initialize() {
        this.loadDoors();
    }

    public void loadDoors() {
        this.hotelDoors.clear();
        if (!this.doorsFile.exists()) {
            this.plugin.getLogger().info("doors.yml does not exist. No doors to load.");
        } else {
            try {
                this.doorsConfig.load(this.doorsFile);
            } catch (InvalidConfigurationException | IOException e) {
                this.plugin.getLogger().severe("Could not load doors.yml: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            if (this.doorsConfig.isConfigurationSection("rooms")) {
                Set<String> nodeKeys = this.doorsConfig.getConfigurationSection("rooms").getKeys(false);
                int nodeCount = nodeKeys.size();
                this.plugin.getLogger().info("[HotelDebug] Found " + nodeCount + " room nodes in doors.yml.");

                int duplicates = 0;

                for (String nodeKey : nodeKeys) {
                    try {
                        String hotelId;
                        String roomId;

                        // nodeKey may be "hotelId|roomId" (new format) or just "roomId" (old format)
                        if (nodeKey.contains("|")) {
                            String[] parts = nodeKey.split("\\|", 2);
                            hotelId = parts[0];
                            roomId = parts[1];
                        } else {
                            roomId = nodeKey;
                            String storedHotelId = this.doorsConfig.getString("rooms." + nodeKey + ".hotelId", null);
                            hotelId = storedHotelId != null ? storedHotelId : "disneyland_hotel";
                        }

                        String basePath = "rooms." + nodeKey;

                        // Door location (required)
                        String worldName = this.doorsConfig.getString(basePath + ".doorLocation.world");
                        double x = this.doorsConfig.getDouble(basePath + ".doorLocation.x");
                        double y = this.doorsConfig.getDouble(basePath + ".doorLocation.y");
                        double z = this.doorsConfig.getDouble(basePath + ".doorLocation.z");

                        World doorWorld = Bukkit.getWorld(worldName);
                        if (doorWorld == null) {
                            this.plugin.getLogger().warning("World '" + worldName + "' for room " + roomId + " not found. Skipping. (This might happen during early server startup)");
                            continue;
                        }

                        Location doorLoc = new Location(Objects.requireNonNull(doorWorld), x, y, z);

                        // Optional second door location
                        Location secondDoorLoc = null;
                        if (this.doorsConfig.isSet(basePath + ".secondDoorLocation")) {
                            String secondDoorWorldName = this.doorsConfig.getString(basePath + ".secondDoorLocation.world");
                            double sx2 = this.doorsConfig.getDouble(basePath + ".secondDoorLocation.x");
                            double sy2 = this.doorsConfig.getDouble(basePath + ".secondDoorLocation.y");
                            double sz2 = this.doorsConfig.getDouble(basePath + ".secondDoorLocation.z");
                            World secondDoorWorld = Bukkit.getWorld(secondDoorWorldName);
                            if (secondDoorWorld != null) {
                                secondDoorLoc = new Location(Objects.requireNonNull(secondDoorWorld), sx2, sy2, sz2);
                            } else {
                                this.plugin.getLogger().warning("World '" + secondDoorWorldName + "' for second door of room " + roomId + " not found. Second door will not be loaded.");
                            }
                        }

                        // Sign location – now robust, we DON'T skip the whole room if sign world is missing
                        String signWorldName = this.doorsConfig.getString(basePath + ".signLocation.world", worldName);
                        double sx = this.doorsConfig.getDouble(basePath + ".signLocation.x", x);
                        double sy = this.doorsConfig.getDouble(basePath + ".signLocation.y", y + 1); // default one block above door
                        double sz = this.doorsConfig.getDouble(basePath + ".signLocation.z", z);

                        World signWorld = Bukkit.getWorld(signWorldName);
                        if (signWorld == null) {
                            this.plugin.getLogger().warning("World '" + signWorldName + "' for sign of room " + roomId
                                    + " not found. Using door world '" + worldName + "' instead.");
                            signWorld = doorWorld;
                        }

                        Location signLoc = new Location(Objects.requireNonNull(signWorld), sx, sy, sz);

                        // Price / currency
                        double price = this.doorsConfig.getDouble(basePath + ".price");
                        String currencyTypeStr = this.doorsConfig.getString(basePath + ".currencyType");
                        CurrencyType currencyType;
                        if (currencyTypeStr != null) {
                            try {
                                currencyType = CurrencyType.valueOf(currencyTypeStr.toUpperCase());
                            } catch (IllegalArgumentException ex) {
                                this.plugin.getLogger().warning("Invalid currency type '" + currencyTypeStr + "' for room " + roomId + ". Setting to default (CurrencyType.VAULT).");
                                currencyType = CurrencyType.VAULT;
                            }
                        } else {
                            this.plugin.getLogger().warning("No currency type found for room " + roomId + ". Setting to default (CurrencyType.VAULT).");
                            currencyType = CurrencyType.VAULT;
                        }

                        // Owner / description / sign facing
                        String ownerUuidStr = this.doorsConfig.getString(basePath + ".owner", null);
                        UUID owner = ownerUuidStr != null ? UUID.fromString(ownerUuidStr) : null;
                        String description = this.doorsConfig.getString(basePath + ".description", "");
                        String signFacingStr = this.doorsConfig.getString(basePath + ".signFacing");
                        BlockFace signFacing = signFacingStr != null ? BlockFace.valueOf(signFacingStr) : BlockFace.NORTH;

                        // Create and store the HotelDoor
                        HotelDoor hotelDoor = new HotelDoor(hotelId, roomId, doorLoc, secondDoorLoc, signLoc, price, currencyType, owner, description, signFacing);

                        String mapKey = this.makeKey(hotelId, roomId);
                        if (this.hotelDoors.containsKey(mapKey)) {
                            duplicates++;
                            this.plugin.getLogger().warning("[HotelDebug] Duplicate room key (hotelId|roomId): "
                                    + mapKey + " (node '" + nodeKey + "'). Previous entry will be OVERWRITTEN.");
                        }

                        this.hotelDoors.put(mapKey, hotelDoor);

                        // Update sign in-world (will safely no-op if chunk not loaded, etc.)
                        this.updateSign(hotelDoor);

                    } catch (Exception e) {
                        this.plugin.getLogger().severe("Error loading room node " + nodeKey + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                this.plugin.getLogger().info("[HotelDebug] Loaded " + this.hotelDoors.size()
                        + " hotel doors. (YAML nodes: " + nodeCount
                        + ", duplicates: " + duplicates + ")");
            } else {
                this.plugin.getLogger().warning("doors.yml has no 'rooms' section.");
            }
        }
    }

    public void saveDoors() {
        this.doorsConfig.set("rooms", null);

        for (HotelDoor door : this.hotelDoors.values()) {
            String nodeKey = this.makeKey(door.getHotelId(), door.getRoomID());
            String path = "rooms." + nodeKey;

            this.doorsConfig.set(path + ".hotelId", door.getHotelId());

            this.doorsConfig.set(path + ".doorLocation.world", door.getDoorLocation().getWorld().getName());
            this.doorsConfig.set(path + ".doorLocation.x", door.getDoorLocation().getX());
            this.doorsConfig.set(path + ".doorLocation.y", door.getDoorLocation().getY());
            this.doorsConfig.set(path + ".doorLocation.z", door.getDoorLocation().getZ());

            if (door.getSecondDoorLocation() != null) {
                this.doorsConfig.set(path + ".secondDoorLocation.world", door.getSecondDoorLocation().getWorld().getName());
                this.doorsConfig.set(path + ".secondDoorLocation.x", door.getSecondDoorLocation().getX());
                this.doorsConfig.set(path + ".secondDoorLocation.y", door.getSecondDoorLocation().getY());
                this.doorsConfig.set(path + ".secondDoorLocation.z", door.getSecondDoorLocation().getZ());
            } else {
                this.doorsConfig.set(path + ".secondDoorLocation", null);
            }

            if (door.getSignLocation() != null && door.getSignLocation().getWorld() != null) {
                this.doorsConfig.set(path + ".signLocation.world", door.getSignLocation().getWorld().getName());
                this.doorsConfig.set(path + ".signLocation.x", door.getSignLocation().getX());
                this.doorsConfig.set(path + ".signLocation.y", door.getSignLocation().getY());
                this.doorsConfig.set(path + ".signLocation.z", door.getSignLocation().getZ());
            }

            this.doorsConfig.set(path + ".price", door.getPrice());
            this.doorsConfig.set(path + ".currencyType", door.getCurrencyType().name());
            this.doorsConfig.set(path + ".owner", door.getOwner() != null ? door.getOwner().toString() : null);
            this.doorsConfig.set(path + ".description", door.getDescription());
            this.doorsConfig.set(path + ".signFacing", door.getSignFacing().name());
        }

        try {
            this.doorsConfig.save(this.doorsFile);
            this.plugin.getLogger().info("Saved " + this.hotelDoors.size() + " hotel doors.");
        } catch (IOException e) {
            this.plugin.getLogger().severe("Could not save doors: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addHotelDoor(HotelDoor door) {
        this.hotelDoors.put(this.makeKey(door.getHotelId(), door.getRoomID()), door);
        this.saveDoors();
        this.updateSign(door);
    }

    public HotelDoor getHotelDoor(String hotelId, String roomId) {
        return hotelId != null && roomId != null ? this.hotelDoors.get(this.makeKey(hotelId, roomId)) : null;
    }

    public HotelDoor getHotelDoor(String roomId) {
        if (roomId == null) {
            return null;
        } else {
            for (HotelDoor door : this.hotelDoors.values()) {
                if (door.getRoomID().equalsIgnoreCase(roomId)) {
                    return door;
                }
            }
            return this.hotelDoors.get(roomId.toLowerCase(Locale.ROOT));
        }
    }

    public HotelDoor getHotelDoorBySignLocation(Location signLoc) {
        for (HotelDoor door : this.hotelDoors.values()) {
            if (door.getSignLocation() != null
                    && door.getSignLocation().getBlockX() == signLoc.getBlockX()
                    && door.getSignLocation().getBlockY() == signLoc.getBlockY()
                    && door.getSignLocation().getBlockZ() == signLoc.getBlockZ()
                    && door.getSignLocation().getWorld().equals(signLoc.getWorld())) {
                return door;
            }
        }
        return null;
    }

    public HotelDoor getHotelDoorByDoorLocation(Location doorLoc) {
        for (HotelDoor door : this.hotelDoors.values()) {
            if (door.getDoorLocation() == null
                    || (door.getDoorLocation().getBlockX() != doorLoc.getBlockX()
                    || door.getDoorLocation().getBlockY() != doorLoc.getBlockY()
                    || door.getDoorLocation().getBlockZ() != doorLoc.getBlockZ()
                    || !door.getDoorLocation().getWorld().equals(doorLoc.getWorld()))
                    && (door.getDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockX() != doorLoc.getBlockX()
                    || door.getDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockY() != doorLoc.getBlockY()
                    || door.getDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockZ() != doorLoc.getBlockZ()
                    || !door.getDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getWorld().equals(doorLoc.getWorld()))) {

                if (door.getSecondDoorLocation() == null
                        || (door.getSecondDoorLocation().getBlockX() != doorLoc.getBlockX()
                        || door.getSecondDoorLocation().getBlockY() != doorLoc.getBlockY()
                        || door.getSecondDoorLocation().getBlockZ() != doorLoc.getBlockZ()
                        || !door.getSecondDoorLocation().getWorld().equals(doorLoc.getWorld()))
                        && (door.getSecondDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockX() != doorLoc.getBlockX()
                        || door.getSecondDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockY() != doorLoc.getBlockY()
                        || door.getSecondDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getBlockZ() != doorLoc.getBlockZ()
                        || !door.getSecondDoorLocation().clone().add(0.0F, 1.0F, 0.0F).getWorld().equals(doorLoc.getWorld()))) {
                    continue;
                }
                return door;
            }
            return door;
        }
        return null;
    }

    public void removeHotelDoor(String roomId) {
        HotelDoor door = this.getHotelDoor(roomId);
        if (door != null) {
            this.removeSign(door.getSignLocation());
            this.hotelDoors.remove(this.makeKey(door.getHotelId(), door.getRoomID()));
            this.saveDoors();
        }
    }

    public Collection<HotelDoor> getAllHotelDoors() {
        return this.hotelDoors.values();
    }

    public void updateSign(HotelDoor door) {
        Location signLoc = door.getSignLocation();
        Logger logger = this.plugin.getLogger();

        logger.info("DEBUG: updateSign - Called for room " + door.getRoomID()
                + " at " + (signLoc != null ? signLoc.toString() : "null"));

        if (signLoc == null || signLoc.getWorld() == null || !signLoc.getChunk().isLoaded()) {
            logger.info("DEBUG: updateSign - Initial check failed for room " + door.getRoomID());
            return;
        }

        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            logger.info("DEBUG: updateSign - Running scheduled task for room " + door.getRoomID()
                    + " at " + signLoc.toString());

            Block block = signLoc.getBlock();
            BlockFace attachedFace = door.getSignFacing();
            if (attachedFace == null) {
                logger.warning("DEBUG: updateSign - Sign facing not set for room "
                        + door.getRoomID() + ". Defaulting to NORTH.");
                attachedFace = BlockFace.NORTH;
            }

            Block blockToAttachTo = block.getRelative(attachedFace.getOppositeFace());
            logger.info("DEBUG: updateSign - Sign block type: " + block.getType().name()
                    + ", Block to attach to type: " + blockToAttachTo.getType().name());

            if (!blockToAttachTo.getType().isSolid()) {
                logger.warning("DEBUG: updateSign - Block to attach sign to at "
                        + blockToAttachTo.getLocation() + " for room "
                        + door.getRoomID() + " is not solid.");
                return;
            }

            boolean isCorrectSign = block.getState() instanceof Sign
                    && block.getType() == Material.OAK_WALL_SIGN;

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
                    logger.warning("DEBUG: updateSign - Sign location " + signLoc
                            + " for room " + door.getRoomID()
                            + " is occupied by " + block.getType().name());
                }

                block.setType(Material.OAK_WALL_SIGN);
                if (!(block.getBlockData() instanceof WallSign)) {
                    logger.warning("DEBUG: updateSign - Failed to cast blockData to WallSign at " + signLoc);
                    return;
                }

                WallSign wallSignData = (WallSign) block.getBlockData();
                wallSignData.setFacing(attachedFace);
                block.setBlockData(wallSignData);
            }

            if (!(block.getState() instanceof Sign)) {
                logger.warning("DEBUG: updateSign - Sign state could not be retrieved at " + signLoc);
                return;
            }

            Sign sign = (Sign) block.getState();
            sign.setLine(0, ChatColor.DARK_BLUE + "[Hotel Room]");
            sign.setLine(1, ChatColor.GOLD + "ID: " + door.getRoomID());

            if (door.isOccupied()) {
                String ownerName = "Unknown";
                if (door.getOwner() != null) {
                    OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(door.getOwner());
                    if (ownerPlayer != null && ownerPlayer.hasPlayedBefore()) {
                        ownerName = ownerPlayer.getName();
                    } else {
                        String uuidStr = door.getOwner().toString();
                        ownerName = uuidStr.substring(0, 8) + "...";
                    }
                }

                sign.setLine(2, ChatColor.RED + "OWNED BY:");
                sign.setLine(3, ChatColor.RED + ownerName);
            } else {
                String description = door.getDescription();
                String[] descLines = this.splitDescription(description, 15);

                if (descLines.length > 0 && !descLines[0].isEmpty()) {
                    sign.setLine(2, ChatColor.LIGHT_PURPLE + descLines[0]);
                    if (descLines.length > 1 && !descLines[1].isEmpty()) {
                        sign.setLine(3, ChatColor.LIGHT_PURPLE + descLines[1]);
                    } else {
                        sign.setLine(3, ChatColor.GREEN + "Available!");
                    }
                } else {
                    String formattedPrice;
                    if (this.economy != null && door.getCurrencyType() == CurrencyType.VAULT) {
                        formattedPrice = this.economy.format(door.getPrice());
                    } else {
                        formattedPrice = (int) door.getPrice() + " " + door.getCurrencyType().getDisplayName();
                    }

                    sign.setLine(2, ChatColor.GOLD + "Price: " + formattedPrice);
                    sign.setLine(3, ChatColor.GREEN + "Available!");
                }
            }

            sign.update(true);
        }, 1L);
    }

    public void removeSign(Location signLoc) {
        if (signLoc != null && signLoc.getWorld() != null && signLoc.getChunk().isLoaded()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                if (signLoc.getBlock().getState() instanceof Sign) {
                    signLoc.getBlock().setType(Material.AIR);
                }
            }, 1L);
        }
    }

    public Economy getEconomy() {
        return this.economy;
    }

    private String[] splitDescription(String description, int maxLenPerLine) {
        if (description != null && !description.isEmpty()) {
            if (description.length() <= maxLenPerLine) {
                return new String[]{description};
            } else {
                int breakPoint = -1;

                for (int i = maxLenPerLine; i >= 0; --i) {
                    if (i < description.length() && description.charAt(i) == ' ') {
                        breakPoint = i;
                        break;
                    }
                }

                if (breakPoint == -1) {
                    String firstLine = description.substring(0, Math.min(maxLenPerLine, description.length()));
                    String secondLine = description.length() > maxLenPerLine ? description.substring(maxLenPerLine) : "";
                    return new String[]{firstLine, secondLine};
                } else {
                    return new String[]{description.substring(0, breakPoint), description.substring(breakPoint + 1)};
                }
            }
        } else {
            return new String[0];
        }
    }

    public void openRoomDoors(HotelDoor door) {
        if (door.getDoorLocation() != null) {
            this.setDoorOpenState(door.getDoorLocation(), true);
        }

        if (door.getSecondDoorLocation() != null) {
            this.setDoorOpenState(door.getSecondDoorLocation(), true);
        }
    }

    public void closeRoomDoors(HotelDoor door) {
        if (door.getDoorLocation() != null) {
            this.setDoorOpenState(door.getDoorLocation(), false);
        }

        if (door.getSecondDoorLocation() != null) {
            this.setDoorOpenState(door.getSecondDoorLocation(), false);
        }
    }

    private void setDoorOpenState(Location loc, boolean open) {
        if (loc != null && loc.getWorld() != null && loc.getChunk().isLoaded()) {
            Block block = loc.getBlock();
            if (block.getBlockData() instanceof Door) {
                Door doorData = (Door) block.getBlockData();
                if (doorData.getHalf() == Half.BOTTOM) {
                    if (doorData.isOpen() != open) {
                        doorData.setOpen(open);
                        block.setBlockData(doorData, true);
                        this.plugin.getLogger().info("DEBUG: Door at " + loc + " set to " + (open ? "OPEN" : "CLOSED"));
                    }
                } else {
                    this.plugin.getLogger().warning("Attempted to set state on top half of door at " + loc + ". Ensure you target the bottom half.");
                }
            } else {
                this.plugin.getLogger().warning("Block at " + loc + " is not a door type (" + block.getType().name() + "). Cannot set door state.");
            }

        } else {
            this.plugin.getLogger().warning("Attempted to set door state for unloaded or null location: " + (loc != null ? loc.toString() : "null"));
        }
    }

    public boolean isRoomAvailable(String roomId) {
        HotelDoor door = this.getHotelDoor(roomId);
        return door != null && !door.isOccupied();
    }

    public void setRoomBooked(String roomId, UUID guestUUID) {
        HotelDoor door = this.getHotelDoor(roomId);
        if (door != null) {
            door.setOwner(guestUUID);
            this.updateSign(door);
            this.saveDoors();
        }
    }

    public void setRoomAvailable(String roomId) {
        HotelDoor door = this.getHotelDoor(roomId);
        if (door != null) {
            door.setOwner(null);
            this.updateSign(door);
            this.saveDoors();
        }
    }

    public Map<String, Integer> getHotelRoomCounts() {
        Map<String, Integer> hotels = new HashMap<>();

        for (HotelDoor door : this.hotelDoors.values()) {
            String hid = door.getHotelId() != null ? door.getHotelId() : "unknown";
            hotels.put(hid, hotels.getOrDefault(hid, 0) + 1);
        }

        return hotels;
    }
}
