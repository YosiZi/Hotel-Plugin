package com.yourname.hotelplugin.commands;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected; // Import for Bisected.Half
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class HotelCommand implements CommandExecutor {

    private final HotelPlugin plugin;

    public HotelCommand(HotelPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can execute this command!");
            return true;
        }

        Player player = (Player) sender;
        Economy econ = plugin.getHotelManager().getEconomy();

        if (econ == null) { // Check if Vault Economy is available
            player.sendMessage(ChatColor.RED + "Economy plugin not found! Hotel features requiring money are disabled.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                // Usage: /hotel create <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> [Description...]
                // (This command will now handle 1 or 2 doors based on arguments provided)
                if (args.length < 7) { // Minimum for 1 door: create, RoomID, Price, CurrencyType, D1X, D1Y, D1Z
                    player.sendMessage(ChatColor.RED + "Usage (One Door): /hotel create <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> [Description...]");
                    player.sendMessage(ChatColor.RED + "Usage (Two Doors): /hotel create <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> <Door2X> <Door2Y> <Door2Z> [Description...]");
                    player.sendMessage(ChatColor.YELLOW + "  (Aim at the block where the sign will be placed for room information.)");
                    return true;
                }

                String roomId = args[1];
                if (plugin.getHotelManager().getHotelDoor(roomId) != null) {
                    player.sendMessage(ChatColor.RED + "A room with this ID already exists!");
                    return true;
                }

                double price;
                try {
                    price = Double.parseDouble(args[2]);
                    if (price < 0) {
                        player.sendMessage(ChatColor.RED + "Price cannot be negative.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid price. Must be a number.");
                    return true;
                }

                CurrencyType currencyType;
                try {
                    currencyType = CurrencyType.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatColor.RED + "Invalid currency type: '" + args[3] + "'. Must be one of: " + ChatColor.YELLOW + java.util.Arrays.toString(CurrencyType.values()));
                    return true;
                }

                // --- Start Door 1 Validation & Adjustment (More Flexible) ---
                Location door1Loc; // This will be the final, adjusted bottom-half location
                try {
                    int door1X = Integer.parseInt(args[4]);
                    int door1Y = Integer.parseInt(args[5]);
                    int door1Z = Integer.parseInt(args[6]);
                    Location initialDoor1InputLoc = new Location(player.getWorld(), door1X, door1Y, door1Z);

                    door1Loc = getValidDoorBottomHalf(initialDoor1InputLoc);

                    if (door1Loc == null) {
                        player.sendMessage(ChatColor.RED + "Door 1 location " + formatLocation(initialDoor1InputLoc) + " or adjacent block is not a valid door.");
                        player.sendMessage(ChatColor.RED + "Please ensure the coordinates point to either the top or bottom half of a door.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid coordinates for Door 1 (X, Y, Z).");
                    return true;
                }
                // --- End Door 1 Validation & Adjustment ---

                Location door2Loc = null; // Initialize as null for one-door rooms
                int descriptionStartIndex;

                // Check if enough arguments are provided for a second door
                if (args.length >= 10) { // Check for 10 arguments for two-door usage
                    // --- Start Door 2 Validation & Adjustment (More Flexible) ---
                    try {
                        int door2X = Integer.parseInt(args[7]);
                        int door2Y = Integer.parseInt(args[8]);
                        int door2Z = Integer.parseInt(args[9]);
                        Location initialDoor2InputLoc = new Location(player.getWorld(), door2X, door2Y, door2Z);

                        door2Loc = getValidDoorBottomHalf(initialDoor2InputLoc);

                        if (door2Loc == null) {
                            player.sendMessage(ChatColor.RED + "Door 2 location " + formatLocation(initialDoor2InputLoc) + " or adjacent block is not a valid door.");
                            player.sendMessage(ChatColor.RED + "Please ensure the coordinates point to either the top or bottom half of a door.");
                            return true;
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid coordinates for Door 2 (X, Y, Z). If you intended for one door, ensure your arguments are correct.");
                        return true;
                    }
                    // --- End Door 2 Validation & Adjustment ---

                    // Check if Door 1 and Door 2 locations are the same (after adjustment to bottom half)
                    if (door1Loc.equals(door2Loc)) {
                        player.sendMessage(ChatColor.RED + "Door 1 and Door 2 locations cannot be the same.");
                        return true;
                    }
                    descriptionStartIndex = 10; // Description starts after 10 arguments for two-door usage
                } else {
                    descriptionStartIndex = 7; // Description starts after 7 arguments for one-door usage
                }

                StringBuilder descriptionBuilder = new StringBuilder();
                for (int i = descriptionStartIndex; i < args.length; i++) {
                    descriptionBuilder.append(args[i]).append(" ");
                }
                String description = descriptionBuilder.toString().trim();

                // Get sign location from targeted block (remains the same)
                Block targetBlock = player.getTargetBlockExact(5);
                if (targetBlock == null) {
                    player.sendMessage(ChatColor.RED + "You must be looking at a block where the sign will be placed.");
                    return true;
                }

                Location signLocation = targetBlock.getLocation();
                BlockFace signFacing = player.getTargetBlockFace(5);

                if (signFacing == null || !signFacing.isCartesian()) { // Check for non-null and Cartesian (a direction like NORTH, EAST etc.)
                    player.sendMessage(ChatColor.RED + "Could not determine the wall you are looking at. Please look directly at the side of a solid block.");
                    return true;
                }

                // Validate sign placement: must be air or an existing sign, and have a solid block behind it
                if (!(targetBlock.getType().isAir() || (targetBlock.getState() instanceof Sign))) {
                    player.sendMessage(ChatColor.RED + "The block you are looking at (" + targetBlock.getType().name() + ") is not empty or an existing sign. Please aim at an empty space on a wall.");
                    return true;
                }
                Block blockBehindSign = targetBlock.getRelative(signFacing.getOppositeFace());
                if (!blockBehindSign.getType().isSolid()) {
                    player.sendMessage(ChatColor.RED + "There must be a solid block behind the sign location for it to attach to. Block found: " + blockBehindSign.getType().name());
                    return true;
                }

                if (plugin.getHotelManager().getHotelDoorBySignLocation(signLocation) != null) {
                    player.sendMessage(ChatColor.RED + "There is already a hotel room linked to this sign location.");
                    return true;
                }

                // Create the HotelDoor object with primary and (conditionally null) secondary door location
                HotelDoor newDoor = new HotelDoor(roomId, door1Loc, door2Loc, signLocation, price, currencyType, null, description, signFacing);
                plugin.getHotelManager().addHotelDoor(newDoor); // This saves and updates the sign

                player.sendMessage(ChatColor.GREEN + "Hotel room " + roomId + " created successfully!");
                player.sendMessage(ChatColor.YELLOW + "Primary Door: " + formatLocation(door1Loc));
                if (door2Loc != null) {
                    player.sendMessage(ChatColor.YELLOW + "Secondary Door: " + formatLocation(door2Loc));
                } else {
                    player.sendMessage(ChatColor.YELLOW + "This room has one door.");
                }
                player.sendMessage(ChatColor.YELLOW + "Sign at " + formatLocation(signLocation) + " facing " + signFacing.name() + ".");
                break;

            case "remove":
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(ChatColor.RED + "Wuh Oh! Looks like you aren't a hotel manager and are not allowed to perform this!");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /hotel remove <RoomID>");
                    return true;
                }
                roomId = args[1];
                HotelDoor doorToRemove = plugin.getHotelManager().getHotelDoor(roomId);
                if (doorToRemove == null) {
                    player.sendMessage(ChatColor.RED + "Room with ID '" + roomId + "' does not exist!");
                    return true;
                }
                // When a room is removed, ensure the doors are closed if it was occupied
                if (doorToRemove.isOccupied()) {
                    plugin.getHotelManager().closeRoomDoors(doorToRemove); // Call the close doors method
                }
                plugin.getHotelManager().removeHotelDoor(roomId);
                player.sendMessage(ChatColor.GREEN + "Hotel room '" + roomId + "' removed successfully.");
                break;


            case "claim": // Aliased to buy for simplicity
            case "buy":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /hotel buy <RoomID>");
                    return true;
                }
                String buyRoomId = args[1];
                HotelDoor doorToClaim = plugin.getHotelManager().getHotelDoor(buyRoomId);
                if (doorToClaim == null) {
                    player.sendMessage(ChatColor.RED + "Room with ID '" + buyRoomId + "' does not exist!");
                    return true;
                }
                if (doorToClaim.getDoorLocation() == null) { // A room must have at least one door to be claimable
                    player.sendMessage(ChatColor.RED + "This room has not had its primary door set yet. Contact an admin.");
                    return true;
                }
                if (doorToClaim.isOccupied()) {
                    player.sendMessage(ChatColor.RED + "This room is already occupied!");
                    return true;
                }

                double cost = doorToClaim.getPrice();

                // Handle free rooms for Vault (no transaction needed)
                if (cost <= 0 && doorToClaim.getCurrencyType() == CurrencyType.VAULT) {
                    player.sendMessage(ChatColor.GREEN + "This room is free! Claiming...");
                } else if (cost < 0) { // Prevent claiming rooms with negative price
                    player.sendMessage(ChatColor.RED + "This room has an invalid price. Cannot be bought.");
                    return true;
                }


                if (doorToClaim.getCurrencyType() == CurrencyType.VAULT) {
                    if (econ.getBalance(player) < cost) {
                        player.sendMessage(ChatColor.RED + "You are too broke. You need " + econ.format(cost) + ".");
                        return true;
                    }
                    EconomyResponse r = econ.withdrawPlayer(player, cost);
                    if (r.transactionSuccess()) {
                        player.sendMessage(ChatColor.GREEN + "You have successfully bought room '" + buyRoomId + "' for " + econ.format(r.amount) + "!");
                    } else {
                        player.sendMessage(ChatColor.RED + "An error occurred while processing your payment. Please contact staff: " + r.errorMessage);
                        return true;
                    }
                } else {
                    // Handle item-based currency
                    Material requiredItem = doorToClaim.getCurrencyType().getMaterial();
                    int requiredAmount = (int) cost; // For items, price is usually integer amount

                    if (requiredItem == null) {
                        player.sendMessage(ChatColor.RED + "This room is configured with an invalid item currency. Contact an admin.");
                        return true;
                    }
                    if (!player.getInventory().contains(requiredItem, requiredAmount)) {
                        player.sendMessage(ChatColor.RED + "You need " + requiredAmount + " " + doorToClaim.getCurrencyType().getDisplayName() + " to buy this room.");
                        return true;
                    }

                    // Remove items from inventory
                    player.getInventory().removeItem(new org.bukkit.inventory.ItemStack(requiredItem, requiredAmount));
                    player.sendMessage(ChatColor.GREEN + "You have successfully bought room '" + buyRoomId + "' for " + requiredAmount + " " + doorToClaim.getCurrencyType().getDisplayName() + "!");
                }

                doorToClaim.claim(player.getUniqueId());
                plugin.getHotelManager().saveDoors();
                plugin.getHotelManager().updateSign(doorToClaim);
                plugin.getHotelManager().openRoomDoors(doorToClaim); // Open doors on claim!
                break;

            case "unclaim":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /hotel unclaim <RoomID>");
                    return true;
                }
                roomId = args[1];
                HotelDoor doorToUnclaim = plugin.getHotelManager().getHotelDoor(roomId);
                if (doorToUnclaim == null) {
                    player.sendMessage(ChatColor.RED + "Room with ID '" + roomId + "' does not exist!");
                    return true;
                }
                if (!doorToUnclaim.isOccupied()) {
                    player.sendMessage(ChatColor.RED + "This room is not currently occupied.");
                    return true;
                }
                if (Objects.equals(doorToUnclaim.getOwner(), player.getUniqueId()) || player.hasPermission("hotelplugin.admin")) {
                    doorToUnclaim.unclaim();
                    plugin.getHotelManager().saveDoors();
                    plugin.getHotelManager().updateSign(doorToUnclaim);
                    plugin.getHotelManager().closeRoomDoors(doorToUnclaim); // Close doors on unclaim!
                    player.sendMessage(ChatColor.GREEN + "Room " + roomId + " has been unclaimed.");
                } else {
                    player.sendMessage(ChatColor.RED + "yeah i don't think this is yours");
                }
                break;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /hotel info <RoomID>");
                    return true;
                }
                roomId = args[1];
                HotelDoor doorInfo = plugin.getHotelManager().getHotelDoor(roomId);
                if (doorInfo == null) {
                    player.sendMessage(ChatColor.RED + "Room with ID '" + roomId + "' does not exist!");
                    return true;
                }

                player.sendMessage(ChatColor.YELLOW + "--- Room Info: " + doorInfo.getRoomID() + " ---");
                player.sendMessage(ChatColor.YELLOW + "Primary Door: " + (doorInfo.getDoorLocation() != null ? formatLocation(doorInfo.getDoorLocation()) : ChatColor.GRAY + "Not Set Yet"));

                if (doorInfo.getSecondDoorLocation() != null) {
                    player.sendMessage(ChatColor.YELLOW + "Secondary Door: " + formatLocation(doorInfo.getSecondDoorLocation()));
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Secondary Door: " + ChatColor.GRAY + "None (One-door room)");
                }
                player.sendMessage(ChatColor.YELLOW + "Sign Location: " + formatLocation(doorInfo.getSignLocation()));


                player.sendMessage(ChatColor.YELLOW + "Price: " + (doorInfo.getCurrencyType() == CurrencyType.VAULT ? econ.format(doorInfo.getPrice()) : (int) doorInfo.getPrice() + " " + doorInfo.getCurrencyType().getDisplayName()));
                player.sendMessage(ChatColor.YELLOW + "Currency: " + doorInfo.getCurrencyType().name());
                player.sendMessage(ChatColor.YELLOW + "Description: " + (doorInfo.getDescription().isEmpty() ? ChatColor.GRAY + "None" : ChatColor.WHITE + doorInfo.getDescription()));
                player.sendMessage(ChatColor.YELLOW + "Status: " + (doorInfo.isOccupied() ? ChatColor.RED + "Occupied" : ChatColor.GREEN + "Available"));
                if (doorInfo.isOccupied()) {
                    UUID ownerUUID = doorInfo.getOwner();
                    String ownerName = "Unknown";
                    if (ownerUUID != null) {
                        OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer(ownerUUID);
                        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                            ownerName = offlinePlayer.getName();
                        } else {
                            ownerName = ownerUUID.toString().substring(0, 8) + "..."; // Shorten UUID
                        }
                    }
                    player.sendMessage(ChatColor.YELLOW + "Owner: " + ChatColor.GOLD + ownerName);
                }
                player.sendMessage(ChatColor.YELLOW + "Sign Facing: " + doorInfo.getSignFacing().name());
                break;

            case "list":
                player.sendMessage(ChatColor.YELLOW + "--- All Hotel Rooms ---");
                if (plugin.getHotelManager().getAllHotelDoors().isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "No rooms registered yet.");
                } else {
                    for (HotelDoor hd : plugin.getHotelManager().getAllHotelDoors()) {
                        String status = hd.isOccupied() ? ChatColor.RED + "Occupied" : ChatColor.GREEN + "Available";
                        String priceDisplay = (hd.getCurrencyType() == CurrencyType.VAULT ? econ.format(hd.getPrice()) : (int) hd.getPrice() + " " + hd.getCurrencyType().getDisplayName());
                        player.sendMessage(ChatColor.AQUA + hd.getRoomID() + ChatColor.RESET + " - Price: " + priceDisplay + " - Status: " + status);
                    }
                }
                break;

            case "reload":
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                plugin.reloadConfig();
                plugin.getHotelManager().loadDoors();
                player.sendMessage(ChatColor.GREEN + "Hotel plugin reloaded.");
                break;

            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    /**
     * Attempts to find the bottom half of a door given an initial location.
     * It checks the given location, and also the block above/below it.
     * Returns the Location of the door's bottom half, or null if no valid door is found.
     */
    private Location getValidDoorBottomHalf(Location initialLoc) {
        if (initialLoc == null || initialLoc.getWorld() == null) {
            return null;
        }

        // 1. Check the initial location
        Block block = initialLoc.getBlock();
        if (block.getBlockData() instanceof Door) {
            Door doorData = (Door) block.getBlockData();
            if (doorData.getHalf() == Bisected.Half.BOTTOM) {
                return initialLoc; // Found bottom half at initial location
            } else { // It's the top half
                Location potentialBottom = initialLoc.clone().subtract(0, 1, 0);
                Block bottomBlock = potentialBottom.getBlock();
                if (bottomBlock.getBlockData() instanceof Door) {
                    Door bottomDoorData = (Door) bottomBlock.getBlockData();
                    if (bottomDoorData.getHalf() == Bisected.Half.BOTTOM) {
                        return potentialBottom; // Found bottom half by looking down
                    }
                }
            }
        }

        // 2. If initial was not bottom half or not a door, check the block above the initial location
        Location locAbove = initialLoc.clone().add(0, 1, 0);
        Block blockAbove = locAbove.getBlock();
        if (blockAbove.getBlockData() instanceof Door) {
            Door doorDataAbove = (Door) blockAbove.getBlockData();
            if (doorDataAbove.getHalf() == Bisected.Half.BOTTOM) {
                return locAbove; // Found bottom half by looking up from initial
            }
        }

        // 3. If neither worked, try checking the block below the initial location
        // (This covers cases where input was an air block just above the bottom half of a door)
        Location locBelow = initialLoc.clone().subtract(0, 1, 0);
        Block blockBelow = locBelow.getBlock();
        if (blockBelow.getBlockData() instanceof Door) {
            Door doorDataBelow = (Door) blockBelow.getBlockData();
            if (doorDataBelow.getHalf() == Bisected.Half.BOTTOM) {
                return locBelow; // Found bottom half by looking down from initial (already covered somewhat, but robust)
            }
        }

        return null; // No valid door bottom half found nearby
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ChatColor.YELLOW + "--- Hotel Plugin Commands ---");
        player.sendMessage(ChatColor.YELLOW + "/hotel create <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> [Description...]");
        player.sendMessage(ChatColor.YELLOW + "  (For two doors: add <Door2X> <Door2Y> <Door2Z> before Description)");
        player.sendMessage(ChatColor.YELLOW + "  (CurrencyType must be one of: " + ChatColor.AQUA + java.util.Arrays.toString(CurrencyType.values()) + ChatColor.YELLOW + ")");
        player.sendMessage(ChatColor.YELLOW + "  (Aim at the block where the sign will be placed for room information.)");
        player.sendMessage(ChatColor.YELLOW + "/hotel remove <RoomID>");
        player.sendMessage(ChatColor.YELLOW + "/hotel buy <RoomID>");
        player.sendMessage(ChatColor.YELLOW + "/hotel unclaim <RoomID>");
        player.sendMessage(ChatColor.YELLOW + "/hotel info <RoomID>");
        player.sendMessage(ChatColor.YELLOW + "/hotel list");
        if (player.hasPermission("hotelplugin.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/hotel reload - Reload plugin data (Admin only).");
        }
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "N/A";
        // Use Integer.parseInt to format coordinates as whole numbers for display
        return String.format("%s, %d, %d, %d", loc.getWorld().getName(), (int) loc.getX(), (int) loc.getY(), (int) loc.getZ());
    }
}