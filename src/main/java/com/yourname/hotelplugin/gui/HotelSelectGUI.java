package com.yourname.hotelplugin.gui;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.HotelDoor;
import com.yourname.hotelplugin.managers.HotelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HotelSelectGUI {

    private final HotelPlugin plugin;
    private final HotelManager hotelManager;

    // Fixed order of hotels
    private static final String[] HOTEL_IDS = new String[] {
            "disneyland_hotel",  // Disneyland Hotel
            "new_york",          // Disney Hotel New York – The Art of Marvel
            "sequoia",           // Sequoia Lodge
            "newport_bay",       // Newport Bay Club
            "cheyenne",          // Cheyenne
            "santa_fe"           // Santa Fe
    };

    public HotelSelectGUI(HotelPlugin plugin, HotelManager hotelManager) {
        this.plugin = plugin;
        this.hotelManager = hotelManager;
    }

    public void open(Player player) {

        // DEBUG: how many HotelDoor objects are actually loaded?
        int totalDoors = hotelManager.getAllHotelDoors().size();
        player.sendMessage(ChatColor.YELLOW + "[HotelDebug] Loaded HotelDoor count: " + totalDoors);

        int displayed = HOTEL_IDS.length;
        int size = 9;
        if (displayed > 9) size = 18;
        if (displayed > 18) size = 27; // plenty for 6

        Inventory inv = Bukkit.createInventory(
                null,
                size,
                ChatColor.DARK_BLUE + "Disneyland Paris Hotels"
        );

        int slot = 0;
        for (String hotelId : HOTEL_IDS) {

            // All doors for this hotelId
            List<HotelDoor> doorsForHotel = hotelManager.getAllHotelDoors().stream()
                    .filter(hd -> hd.getHotelId() != null &&
                            hd.getHotelId().equalsIgnoreCase(hotelId))
                    .collect(Collectors.toList());

            long roomCount = doorsForHotel.size();

            // Rooms THIS player owns in that hotel
            long ownedCount = doorsForHotel.stream()
                    .filter(HotelDoor::isOccupied)
                    .filter(hd -> player.getUniqueId().equals(hd.getOwner()))
                    .count();

            ItemStack item = new ItemStack(getHotelIcon(hotelId));
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + formatHotelName(hotelId));

                // This is what the click listener will use
                meta.setLocalizedName(hotelId.toLowerCase(Locale.ROOT));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Total Rooms: " + ChatColor.AQUA + roomCount);

                if (ownedCount > 0) {
                    lore.add(ChatColor.GREEN + "✔ You own " + ChatColor.WHITE + ownedCount +
                            ChatColor.GREEN + " room(s) here!");
                } else {
                    lore.add(ChatColor.RED + "✘ You don't own a room here.");
                }

                lore.add("");
                lore.add(ChatColor.YELLOW + "» Click to view rooms");
                meta.setLore(lore);

                item.setItemMeta(meta);
            }

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    private Material getHotelIcon(String hotelId) {
        String id = hotelId.toLowerCase(Locale.ROOT);
        if (id.contains("disneyland_hotel")) return Material.PINK_CONCRETE;
        if (id.contains("new_york")) return Material.RED_NETHER_BRICKS;
        if (id.contains("newport") || id.contains("bay")) return Material.LAPIS_BLOCK;
        if (id.contains("sequoia")) return Material.STRIPPED_SPRUCE_LOG;
        if (id.contains("cheyenne")) return Material.ORANGE_TERRACOTTA;
        if (id.contains("santa_fe")) return Material.YELLOW_TERRACOTTA;
        return Material.PAINTING;
    }

    public static String formatHotelName(String hotelId) {
        if (hotelId == null || hotelId.isEmpty()) return "Unknown Hotel";
        switch (hotelId.toLowerCase(Locale.ROOT)) {
            case "disneyland_hotel":
                return "Disneyland Hotel";
            case "new_york":
                return "Disney Hotel New York";
            case "sequoia":
                return "Sequoia Lodge";
            case "newport_bay":
                return "Newport Bay Club";
            case "cheyenne":
                return "Hotel Cheyenne";
            case "santa_fe":
                return "Hotel Santa Fe";
            default:
                String[] words = hotelId.replace("_", " ").split(" ");
                StringBuilder sb = new StringBuilder();
                for (String w : words) {
                    if (!w.isEmpty()) {
                        sb.append(Character.toUpperCase(w.charAt(0)))
                                .append(w.substring(1).toLowerCase())
                                .append(" ");
                    }
                }
                return sb.toString().trim();
        }
    }
}
