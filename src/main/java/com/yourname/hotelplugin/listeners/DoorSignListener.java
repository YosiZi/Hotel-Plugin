package com.yourname.hotelplugin.listeners;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Bisected.Half;
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
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            Player player = event.getPlayer();
            if (clickedBlock != null) {
                if (clickedBlock.getState() instanceof Sign) {
                    HotelDoor hotelDoor = this.plugin.getHotelManager().getHotelDoorBySignLocation(clickedBlock.getLocation());
                    if (hotelDoor != null) {
                        event.setCancelled(true);
                        if (hotelDoor.isOccupied()) {
                            String ownerName = "Unknown";
                            if (hotelDoor.getOwner() != null) {
                                OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(hotelDoor.getOwner());
                                if (ownerPlayer != null && ownerPlayer.hasPlayedBefore()) {
                                    ownerName = ownerPlayer.getName();
                                } else {
                                    String var10000 = hotelDoor.getOwner().toString();
                                    ownerName = var10000.substring(0, 8) + "...";
                                }
                            }

                            String var12 = String.valueOf(ChatColor.RED);
                            player.sendMessage(var12 + "This room (" + hotelDoor.getRoomID() + ") is occupied by " + ownerName + ".");
                        } else {
                            String formattedPrice;
                            if (hotelDoor.getCurrencyType() == CurrencyType.VAULT && this.plugin.getEconomy() != null) {
                                formattedPrice = this.plugin.getEconomy().format(hotelDoor.getPrice());
                            } else {
                                int var11 = (int)hotelDoor.getPrice();
                                formattedPrice = var11 + " " + hotelDoor.getCurrencyType().getDisplayName();
                            }

                            String var13 = String.valueOf(ChatColor.YELLOW);
                            player.sendMessage(var13 + "This room (" + hotelDoor.getRoomID() + ") is available for " + formattedPrice + ".");
                            var13 = String.valueOf(ChatColor.YELLOW);
                            player.sendMessage(var13 + "Use " + String.valueOf(ChatColor.GOLD) + "/hotel buy " + hotelDoor.getRoomID() + String.valueOf(ChatColor.YELLOW) + " to claim it!");
                        }

                        return;
                    }
                }

                if (clickedBlock.getBlockData() instanceof Door) {
                    Door doorData = (Door)clickedBlock.getBlockData();
                    Block actualDoorBlock = clickedBlock;
                    if (doorData.getHalf() == Half.TOP) {
                        actualDoorBlock = clickedBlock.getRelative(0, -1, 0);
                        if (!(actualDoorBlock.getBlockData() instanceof Door)) {
                            return;
                        }
                    }

                    HotelDoor hotelDoor = this.plugin.getHotelManager().getHotelDoorByDoorLocation(actualDoorBlock.getLocation());
                    if (hotelDoor != null) {
                        if (hotelDoor.isOccupied()) {
                            if (!hotelDoor.getOwner().equals(player.getUniqueId()) && !player.hasPermission("hotelplugin.admin")) {
                                player.sendMessage(String.valueOf(ChatColor.RED) + "This room is owned by someone else.");
                                event.setCancelled(true);
                            }
                        } else {
                            String var10001 = String.valueOf(ChatColor.YELLOW);
                            player.sendMessage(var10001 + "This room is available. Claim it with " + String.valueOf(ChatColor.GOLD) + "/hotel buy " + hotelDoor.getRoomID() + String.valueOf(ChatColor.YELLOW) + "!");
                            event.setCancelled(true);
                        }
                    }
                }

            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        Player player = event.getPlayer();
        if (brokenBlock.getState() instanceof Sign) {
            HotelDoor door = this.plugin.getHotelManager().getHotelDoorBySignLocation(brokenBlock.getLocation());
            if (door != null) {
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "You cannot break a hotel sign!");
                    event.setCancelled(true);
                } else {
                    String var10001 = String.valueOf(ChatColor.YELLOW);
                    player.sendMessage(var10001 + "Hotel sign broken by admin. Remember to remove the room with /hotel remove " + door.getRoomID() + " to prevent data issues.");
                }
            }
        } else if (brokenBlock.getBlockData() instanceof Door) {
            Door doorData = (Door)brokenBlock.getBlockData();
            Block actualDoorBlock = brokenBlock;
            if (doorData.getHalf() == Half.TOP) {
                actualDoorBlock = brokenBlock.getRelative(0, -1, 0);
                if (!(actualDoorBlock.getBlockData() instanceof Door)) {
                    return;
                }
            }

            HotelDoor door = this.plugin.getHotelManager().getHotelDoorByDoorLocation(actualDoorBlock.getLocation());
            if (door != null) {
                if (!player.hasPermission("hotelplugin.admin")) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "You cannot break a hotel door!");
                    event.setCancelled(true);
                } else {
                    String var8 = String.valueOf(ChatColor.YELLOW);
                    player.sendMessage(var8 + "Hotel door broken by admin. Remember to remove the room with /hotel remove " + door.getRoomID() + " to prevent data issues.");
                }
            }
        }

    }
}
