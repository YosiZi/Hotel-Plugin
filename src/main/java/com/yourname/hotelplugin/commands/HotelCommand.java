package com.yourname.hotelplugin.commands;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import com.yourname.hotelplugin.gui.HotelSelectGUI;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class HotelCommand implements CommandExecutor {
    private final HotelPlugin plugin;
    private final HotelSelectGUI hotelSelectGUI;

    public HotelCommand(HotelPlugin plugin) {
        this.plugin = plugin;
        this.hotelSelectGUI = new HotelSelectGUI(plugin, plugin.getHotelManager());
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Only players can execute this command!");
            return true;
        } else {
            Economy econ = this.plugin.getHotelManager().getEconomy();
            if (econ == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Economy plugin not found! Hotel features requiring money are disabled.");
                return true;
            } else if (args.length == 0) {
                this.hotelSelectGUI.open(player);
                return true;
            } else {
                switch (args[0].toLowerCase()) {
                    case "create":
                        if (!player.hasPermission("hotelplugin.admin")) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command.");
                            return true;
                        }

                        if (args.length < 8) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage (One Door): /hotel create <HotelId> <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> [Description...]");
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage (Two Doors): /hotel create <HotelId> <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> <Door2X> <Door2Y> <Door2Z> [Description...]");
                            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "  (Aim at the block where the sign will be placed for room information.)");
                            return true;
                        }

                        String hotelId = args[1].toLowerCase();
                        String roomId = args[2];
                        if (this.plugin.getHotelManager().getHotelDoor(hotelId, roomId) != null) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "A room with this ID already exists!");
                            return true;
                        }

                        double price;
                        try {
                            price = Double.parseDouble(args[3]);
                            if (price < (double)0.0F) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "Price cannot be negative.");
                                return true;
                            }
                        } catch (NumberFormatException var28) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Invalid price. Must be a number.");
                            return true;
                        }

                        CurrencyType currencyType;
                        try {
                            currencyType = CurrencyType.valueOf(args[4].toUpperCase());
                        } catch (IllegalArgumentException var26) {
                            String var76 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var76 + "Invalid currency type: '" + args[4] + "'. Must be one of: " + String.valueOf(ChatColor.YELLOW) + Arrays.toString(CurrencyType.values()));
                            return true;
                        }

                        Location door1Loc;
                        try {
                            int door1X = Integer.parseInt(args[5]);
                            int door1Y = Integer.parseInt(args[6]);
                            int door1Z = Integer.parseInt(args[7]);
                            Location initialDoor1InputLoc = new Location(player.getWorld(), (double)door1X, (double)door1Y, (double)door1Z);
                            door1Loc = this.getValidDoorBottomHalf(initialDoor1InputLoc);
                            if (door1Loc == null) {
                                String var83 = String.valueOf(ChatColor.RED);
                                player.sendMessage(var83 + "Door 1 location " + this.formatLocation(initialDoor1InputLoc) + " or adjacent block is not a valid door.");
                                player.sendMessage(String.valueOf(ChatColor.RED) + "Please ensure the coordinates point to either the top or bottom half of a door.");
                                return true;
                            }
                        } catch (NumberFormatException var25) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Invalid coordinates for Door 1 (X, Y, Z).");
                            return true;
                        }

                        Location door2Loc = null;
                        int descriptionStartIndex;
                        if (args.length < 11) {
                            descriptionStartIndex = 8;
                        } else {
                            try {
                                int door2X = Integer.parseInt(args[8]);
                                int door2Y = Integer.parseInt(args[9]);
                                int door2Z = Integer.parseInt(args[10]);
                                Location initialDoor2InputLoc = new Location(player.getWorld(), (double)door2X, (double)door2Y, (double)door2Z);
                                door2Loc = this.getValidDoorBottomHalf(initialDoor2InputLoc);
                                if (door2Loc == null) {
                                    String var77 = String.valueOf(ChatColor.RED);
                                    player.sendMessage(var77 + "Door 2 location " + this.formatLocation(initialDoor2InputLoc) + " or adjacent block is not a valid door.");
                                    player.sendMessage(String.valueOf(ChatColor.RED) + "Please ensure the coordinates point to either the top or bottom half of a door.");
                                    return true;
                                }
                            } catch (NumberFormatException var27) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "Invalid coordinates for Door 2 (X, Y, Z). If you intended for one door, ensure your arguments are correct.");
                                return true;
                            }

                            if (door1Loc.equals(door2Loc)) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "Door 1 and Door 2 locations cannot be the same.");
                                return true;
                            }

                            descriptionStartIndex = 11;
                        }

                        StringBuilder descriptionBuilder = new StringBuilder();

                        for(int i = descriptionStartIndex; i < args.length; ++i) {
                            descriptionBuilder.append(args[i]).append(" ");
                        }

                        String description = descriptionBuilder.toString().trim();
                        Block targetBlock = player.getTargetBlockExact(5);
                        if (targetBlock == null) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "You must be looking at a block where the sign will be placed.");
                            return true;
                        }

                        Location signLocation = targetBlock.getLocation();
                        BlockFace signFacing = player.getTargetBlockFace(5);
                        if (signFacing == null || !signFacing.isCartesian()) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Could not determine the wall you are looking at. Please look directly at the side of a solid block.");
                            return true;
                        }

                        if (!targetBlock.getType().isAir() && !(targetBlock.getState() instanceof Sign)) {
                            String var82 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var82 + "The block you are looking at (" + targetBlock.getType().name() + ") is not empty or an existing sign. Please aim at an empty space on a wall.");
                            return true;
                        }

                        Block blockBehindSign = targetBlock.getRelative(signFacing.getOppositeFace());
                        if (!blockBehindSign.getType().isSolid()) {
                            String var81 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var81 + "There must be a solid block behind the sign location for it to attach to. Block found: " + blockBehindSign.getType().name());
                            return true;
                        }

                        if (this.plugin.getHotelManager().getHotelDoorBySignLocation(signLocation) != null) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "There is already a hotel room linked to this sign location.");
                            return true;
                        }

                        HotelDoor newDoor = new HotelDoor(hotelId, roomId, door1Loc, door2Loc, signLocation, price, currencyType, (UUID)null, description, signFacing);
                        this.plugin.getHotelManager().addHotelDoor(newDoor);
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Room " + roomId + " created in hotel " + hotelId + "!");
                        String var78 = String.valueOf(ChatColor.YELLOW);
                        player.sendMessage(var78 + "Primary Door: " + this.formatLocation(door1Loc));
                        if (door2Loc != null) {
                            var78 = String.valueOf(ChatColor.YELLOW);
                            player.sendMessage(var78 + "Secondary Door: " + this.formatLocation(door2Loc));
                        } else {
                            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "This room has one door.");
                        }

                        var78 = String.valueOf(ChatColor.YELLOW);
                        player.sendMessage(var78 + "Sign at " + this.formatLocation(signLocation) + " facing " + signFacing.name() + ".");
                        break;
                    case "remove":
                        if (!player.hasPermission("hotelplugin.admin")) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Wuh Oh! Looks like you aren't a hotel manager and are not allowed to perform this!");
                            return true;
                        }

                        if (args.length < 2) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotel remove <RoomID>");
                            return true;
                        }

                        String roomIdToRemove = args[1];
                        HotelDoor doorToRemove = this.plugin.getHotelManager().getHotelDoor(roomIdToRemove);
                        if (doorToRemove == null) {
                            String var75 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var75 + "Room with ID '" + roomIdToRemove + "' does not exist!");
                            return true;
                        }

                        if (doorToRemove.isOccupied()) {
                            this.plugin.getHotelManager().closeRoomDoors(doorToRemove);
                        }

                        this.plugin.getHotelManager().removeHotelDoor(roomIdToRemove);
                        String var74 = String.valueOf(ChatColor.GREEN);
                        player.sendMessage(var74 + "Hotel room '" + roomIdToRemove + "' removed successfully.");
                        break;
                    case "claim":
                    case "buy":
                        if (args.length < 2) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotel buy <RoomID>");
                            return true;
                        }

                        String buyRoomId = args[1];
                        HotelDoor doorToClaim = this.plugin.getHotelManager().getHotelDoor(buyRoomId);
                        if (doorToClaim == null) {
                            String var73 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var73 + "Room with ID '" + buyRoomId + "' does not exist!");
                            return true;
                        }

                        if (doorToClaim.getDoorLocation() == null) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "This room has not had its primary door set yet. Contact an admin.");
                            return true;
                        }

                        if (doorToClaim.isOccupied()) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "This room is already occupied!");
                            return true;
                        }

                        double cost = doorToClaim.getPrice();
                        if (cost <= (double)0.0F && doorToClaim.getCurrencyType() == CurrencyType.VAULT) {
                            player.sendMessage(String.valueOf(ChatColor.GREEN) + "This room is free! Claiming...");
                        } else if (cost < (double)0.0F) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "This room has an invalid price. Cannot be bought.");
                            return true;
                        }

                        if (doorToClaim.getCurrencyType() == CurrencyType.VAULT) {
                            if (econ.getBalance(player) < cost) {
                                String var72 = String.valueOf(ChatColor.RED);
                                player.sendMessage(var72 + "You are too broke. You need " + econ.format(cost) + ".");
                                return true;
                            }

                            EconomyResponse r = econ.withdrawPlayer(player, cost);
                            if (!r.transactionSuccess()) {
                                String var71 = String.valueOf(ChatColor.RED);
                                player.sendMessage(var71 + "An error occurred while processing your payment. Please contact staff: " + r.errorMessage);
                                return true;
                            }

                            player.sendMessage(String.valueOf(ChatColor.GREEN) + "You have successfully bought room '" + buyRoomId + "' for " + econ.format(r.amount) + "!");
                        } else {
                            Material requiredItem = doorToClaim.getCurrencyType().getMaterial();
                            int requiredAmount = (int)cost;
                            if (requiredItem == null) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "This room is configured with an invalid item currency. Contact an admin.");
                                return true;
                            }

                            if (!player.getInventory().contains(requiredItem, requiredAmount)) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "You need " + requiredAmount + " " + doorToClaim.getCurrencyType().getDisplayName() + " to buy this room.");
                                return true;
                            }

                            player.getInventory().removeItem(new ItemStack[]{new ItemStack(requiredItem, requiredAmount)});
                            player.sendMessage(String.valueOf(ChatColor.GREEN) + "You have successfully bought room '" + buyRoomId + "' for " + requiredAmount + " " + doorToClaim.getCurrencyType().getDisplayName() + "!");
                        }

                        doorToClaim.claim(player.getUniqueId());
                        this.plugin.getHotelManager().saveDoors();
                        this.plugin.getHotelManager().updateSign(doorToClaim);
                        this.plugin.getHotelManager().openRoomDoors(doorToClaim);
                        break;
                    case "unclaim":
                        if (args.length < 2) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotel unclaim <RoomID>");
                            return true;
                        }

                        String unclaimRoomId = args[1];
                        HotelDoor doorToUnclaim = this.plugin.getHotelManager().getHotelDoor(unclaimRoomId);
                        if (doorToUnclaim == null) {
                            String var70 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var70 + "Room with ID '" + unclaimRoomId + "' does not exist!");
                            return true;
                        }

                        if (!doorToUnclaim.isOccupied()) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "This room is not currently occupied.");
                            return true;
                        }

                        if (!Objects.equals(doorToUnclaim.getOwner(), player.getUniqueId()) && !player.hasPermission("hotelplugin.admin")) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "yeah i don't think this is yours");
                        } else {
                            doorToUnclaim.unclaim();
                            this.plugin.getHotelManager().saveDoors();
                            this.plugin.getHotelManager().updateSign(doorToUnclaim);
                            this.plugin.getHotelManager().closeRoomDoors(doorToUnclaim);
                            String var69 = String.valueOf(ChatColor.GREEN);
                            player.sendMessage(var69 + "Room " + unclaimRoomId + " has been unclaimed.");
                        }
                        break;

                    case "info":
                        if (args.length < 2) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /hotel info <RoomID>");
                            return true;
                        }

                        String infoRoomId = args[1];
                        HotelDoor doorInfo = this.plugin.getHotelManager().getHotelDoor(infoRoomId);
                        if (doorInfo == null) {
                            String var68 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var68 + "Room with ID '" + infoRoomId + "' does not exist!");
                            return true;
                        }

                        String var57 = String.valueOf(ChatColor.YELLOW);
                        player.sendMessage(var57 + "--- Room Info: " + doorInfo.getRoomID() + " ---");
                        // rest of the code stays the same (it uses doorInfo, not roomId)
                        break;

                    case "list":
                        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "--- All Hotel Rooms ---");
                        if (this.plugin.getHotelManager().getAllHotelDoors().isEmpty()) {
                            player.sendMessage(String.valueOf(ChatColor.GRAY) + "No rooms registered yet.");
                        } else {
                            for(HotelDoor hd : this.plugin.getHotelManager().getAllHotelDoors()) {
                                String status = hd.isOccupied() ? String.valueOf(ChatColor.RED) + "Occupied" : String.valueOf(ChatColor.GREEN) + "Available";
                                String priceDisplay = hd.getCurrencyType() == CurrencyType.VAULT ? econ.format(hd.getPrice()) : (int)hd.getPrice() + " " + hd.getCurrencyType().getDisplayName();
                                String var10001 = String.valueOf(ChatColor.AQUA);
                                player.sendMessage(var10001 + hd.getRoomID() + String.valueOf(ChatColor.RESET) + " - Price: " + priceDisplay + " - Status: " + status);
                            }
                        }
                        break;
                    case "reload":
                        if (!player.hasPermission("hotelplugin.admin")) {
                            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command.");
                            return true;
                        }

                        this.plugin.reloadConfig();
                        this.plugin.getHotelManager().loadDoors();
                        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Hotel plugin reloaded.");
                        break;
                    default:
                        this.sendHelpMessage(player);
                }

                return true;
            }
        }
    }

    private Location getValidDoorBottomHalf(Location initialLoc) {
        if (initialLoc != null && initialLoc.getWorld() != null) {
            Block block = initialLoc.getBlock();
            if (block.getBlockData() instanceof Door) {
                Door doorData = (Door)block.getBlockData();
                if (doorData.getHalf() == Half.BOTTOM) {
                    return initialLoc;
                }

                Location potentialBottom = initialLoc.clone().subtract((double)0.0F, (double)1.0F, (double)0.0F);
                Block bottomBlock = potentialBottom.getBlock();
                if (bottomBlock.getBlockData() instanceof Door) {
                    Door bottomDoorData = (Door)bottomBlock.getBlockData();
                    if (bottomDoorData.getHalf() == Half.BOTTOM) {
                        return potentialBottom;
                    }
                }
            }

            Location locAbove = initialLoc.clone().add((double)0.0F, (double)1.0F, (double)0.0F);
            Block blockAbove = locAbove.getBlock();
            if (blockAbove.getBlockData() instanceof Door) {
                Door doorDataAbove = (Door)blockAbove.getBlockData();
                if (doorDataAbove.getHalf() == Half.BOTTOM) {
                    return locAbove;
                }
            }

            Location locBelow = initialLoc.clone().subtract((double)0.0F, (double)1.0F, (double)0.0F);
            Block blockBelow = locBelow.getBlock();
            if (blockBelow.getBlockData() instanceof Door) {
                Door doorDataBelow = (Door)blockBelow.getBlockData();
                if (doorDataBelow.getHalf() == Half.BOTTOM) {
                    return locBelow;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "--- Hotel Plugin Commands ---");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel create <HotelId> <RoomID> <Price> <CurrencyType> <Door1X> <Door1Y> <Door1Z> [Description...]");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "  (For two doors: add <Door2X> <Door2Y> <Door2Z> before Description)");
        String var10001 = String.valueOf(ChatColor.YELLOW);
        player.sendMessage(var10001 + "  (CurrencyType must be one of: " + String.valueOf(ChatColor.AQUA) + Arrays.toString(CurrencyType.values()) + String.valueOf(ChatColor.YELLOW) + ")");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "  (Aim at the block where the sign will be placed for room information.)");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel remove <RoomID>");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel buy <HotelId> <RoomID>");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel unclaim <HotelId> <RoomID>");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel info <HotelId> <RoomID>");
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel list");
        if (player.hasPermission("hotelplugin.admin")) {
            player.sendMessage(String.valueOf(ChatColor.YELLOW) + "/hotel reload - Reload plugin data (Admin only).");
        }

    }

    private String formatLocation(Location loc) {
        return loc == null ? "N/A" : String.format("%s, %d, %d, %d", loc.getWorld().getName(), (int)loc.getX(), (int)loc.getY(), (int)loc.getZ());
    }
}
