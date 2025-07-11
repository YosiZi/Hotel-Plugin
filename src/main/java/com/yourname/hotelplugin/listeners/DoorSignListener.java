package com.yourname.hotelplugin.listeners;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class DoorSignListener implements Listener {

    private final HotelPlugin plugin;

    public DoorSignListener(HotelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        Player player = event.getPlayer();

        if (clickedBlock == null) {
            return;
        }

        // Check if it's a hotel sign
        if (clickedBlock.getState() instanceof org.bukkit.block.Sign) {
            HotelDoor hotelDoor = plugin.getHotelManager().getHotelDoorBySignLocation(clickedBlock.getLocation());
            if (hotelDoor != null) {
                event.setCancelled(true); // Prevent players from editing signs directly

                if (hotelDoor.isOccupied()) {
                    String ownerName = "Unknown";
                    if (hotelDoor.getOwner() != null) {
                        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(hotelDoor.getOwner());
                        if (ownerPlayer != null && ownerPlayer.hasPlayedBefore()) {
                            ownerName = ownerPlayer.getName();
                        } else {
                            ownerName = hotelDoor.getOwner().toString().substring(0, 8) + "..."; // Shorten UUID if name unknown
                        }
                    }
                    player.sendMessage(ChatColor.RED + "This room (" + hotelDoor.getRoomID() + ") is occupied by " + ownerName + ".");
                } else {
                    String formattedPrice;
                    // FIX: Use 'plugin.getEconomy()' instead of 'HotelPlugin.getEconomy()'
                    if (hotelDoor.getCurrencyType() == CurrencyType.VAULT && plugin.getEconomy() != null) {
                        formattedPrice = plugin.getEconomy().format(hotelDoor.getPrice());
                    } else {
                        formattedPrice = (int) hotelDoor.getPrice() + " " + hotelDoor.getCurrencyType().getDisplayName();
                    }
                    player.sendMessage(ChatColor.YELLOW + "This room (" + hotelDoor.getRoomID() + ") is available for " + formattedPrice + ".");
                    player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/hotel buy " + hotelDoor.getRoomID() + ChatColor.YELLOW + " to claim it!");
                }
                return; // Handled a sign interaction, so return
            }
        }

        // Check if it's a hotel door
        if (clickedBlock.getBlockData() instanceof Door) {
            Door doorData = (Door) clickedBlock.getBlockData();
            Block actualDoorBlock = clickedBlock;
            // Get the bottom half of the door for consistent lookup
            if (doorData.getHalf() == Door.Half.TOP) {
                actualDoorBlock = clickedBlock.getRelative(0, -1, 0);
                if (!(actualDoorBlock.getBlockData() instanceof Door)) {
                    return; // The bottom half isn't a door, something is wrong
                }
            }

            HotelDoor hotelDoor = plugin.getHotelManager().getHotelDoorByDoorLocation(actualDoorBlock.getLocation());

            if (hotelDoor != null) {
                if (hotelDoor.isOccupied()) {
                    // If owned, allow owner or admin to open
                    if (hotelDoor.getOwner().equals(player.getUniqueId()) || player.hasPermission("hotelplugin.admin")) {
                        // Allowed to open - do nothing special, let Minecraft handle the door open/close
                    } else {
                        player.sendMessage(ChatColor.RED + "This room is owned by someone else.");
                        event.setCancelled(true); // Prevent non-owners from opening
                    }
                } else {
                    player.sendMessage(ChatColor.YELLOW + "This room is available. Claim it with " + ChatColor.GOLD + "/hotel buy " + hotelDoor.getRoomID() + ChatColor.YELLOW + "!");
                    event.setCancelled(true); // Prevent opening an un-owned room directly
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();

        // Check if it's a hotel sign
        if (brokenBlock.getState() instanceof org.bukkit.block.Sign) {
            HotelDoor door = plugin.getHotelManager().getHotelDoorBySignLocation(brokenBlock.getLocation());
            if (door != null) {
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(ChatColor.RED + "You cannot break a hotel sign!");
                    event.setCancelled(true);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Hotel sign broken by admin. Remember to remove the room with /hotel remove " + door.getRoomID() + " to prevent data issues.");
                    // Don't cancel if admin, let it break, but warn them to use the command
                }
            }
        }
        // Check if it's a hotel door
        else if (brokenBlock.getBlockData() instanceof Door) {
            Door doorData = (Door) brokenBlock.getBlockData();
            Block actualDoorBlock = brokenBlock;
            if (doorData.getHalf() == Door.Half.TOP) {
                actualDoorBlock = brokenBlock.getRelative(0, -1, 0);
                if (!(actualDoorBlock.getBlockData() instanceof Door)) {
                    return;
                }
            }

            HotelDoor door = plugin.getHotelManager().getHotelDoorByDoorLocation(actualDoorBlock.getLocation());
            if (door != null) {
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(ChatColor.RED + "You cannot break a hotel door!");
                    event.setCancelled(true);
                } else {
                    player.sendMessage(ChatColor.YELLOW + "Hotel door broken by admin. Remember to remove the room with /hotel remove " + door.getRoomID() + " to prevent data issues.");
                    // Don't cancel if admin, let it break, but warn them to use the command
                }
            }
        }
    }
}