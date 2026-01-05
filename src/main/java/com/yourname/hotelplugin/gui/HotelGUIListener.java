package com.yourname.hotelplugin.gui;

import com.yourname.hotelplugin.HotelPlugin;
import com.yourname.hotelplugin.data.CurrencyType;
import com.yourname.hotelplugin.data.HotelDoor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class HotelGUIListener implements Listener {

    private static final String MAIN_TITLE_PLAIN = "Disneyland Paris Hotels";
    private static final String ROOMS_SUFFIX = " Rooms";

    // Page settings
    private static final int INVENTORY_SIZE = 54; // 6 rows
    private static final int PAGE_SIZE = 45;      // 5 rows of doors, last row for controls

    // Bottom row slots for controls
    private static final int SLOT_PREV = 45;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 53;

    private final HotelPlugin plugin;

    public HotelGUIListener(HotelPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player)) {
            return;
        }
        Player player = (Player) clicker;

        String rawTitle = event.getView().getTitle();
        String strippedTitle = ChatColor.stripColor(rawTitle);

        // --- MAIN HOTEL SELECTION GUI ---
        if (strippedTitle.equals(MAIN_TITLE_PLAIN)) {
            // Only react to clicks in the TOP inventory (the GUI itself)
            if (event.getClickedInventory() == null ||
                    !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }

            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) {
                return;
            }

            String hotelId = meta.getLocalizedName(); // set in HotelSelectGUI

            if (hotelId == null || hotelId.isEmpty()) {
                // Fallback: try to guess from the visible name (shouldn't be needed,
                // but nice safety).
                String displayName = ChatColor.stripColor(meta.getDisplayName());
                hotelId = nameToHotelId(displayName);
            }

            if (hotelId == null || hotelId.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Could not determine which hotel you clicked.");
                return;
            }

            // Debug so you can see that the click is recognized
            player.sendMessage(ChatColor.AQUA + "[HotelDebug] You clicked hotel: " + hotelId);

            // Open first page (page index 0)
            openHotelRoomsGUI(player, hotelId, 0);
            return;
        }

        // --- ROOMS GUI (paged) ---
        if (strippedTitle.endsWith(ROOMS_SUFFIX)) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
                return;
            }

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String locName = meta.getLocalizedName();
            if (locName == null || locName.isEmpty()) {
                return;
            }

            // Navigation buttons use localizedName starting with "nav:"
            if (locName.startsWith("nav:")) {
                // Format: nav:prev:hotelId:pageIndex  or nav:next:hotelId:pageIndex
                String[] parts = locName.split(":", 4);
                if (parts.length < 4) return;

                String dir = parts[1];
                String hotelId = parts[2];
                int page;
                try {
                    page = Integer.parseInt(parts[3]);
                } catch (NumberFormatException ex) {
                    return;
                }

                int newPage = page;
                if ("next".equalsIgnoreCase(dir)) {
                    newPage = page + 1;
                } else if ("prev".equalsIgnoreCase(dir)) {
                    newPage = page - 1;
                }

                openHotelRoomsGUI(player, hotelId, newPage);
            } else {
                // --- TELEPORT TO ROOM ON DOOR CLICK ---
                // locName format for doors: "hotelId|roomId"
                String[] keyParts = locName.split("\\|", 2);
                if (keyParts.length != 2) {
                    return;
                }
                String hotelId = keyParts[0];
                String roomId = keyParts[1];

                HotelDoor door = plugin.getHotelManager().getHotelDoor(hotelId, roomId);
                if (door == null) {
                    player.sendMessage(ChatColor.RED + "Could not find data for room " + roomId + ".");
                    return;
                }

                if (door.getDoorLocation() == null || door.getDoorLocation().getWorld() == null) {
                    player.sendMessage(ChatColor.RED + "Door location for room " + roomId + " is not set.");
                    return;
                }

                // Make sure the chunk is loaded
                door.getDoorLocation().getWorld().getChunkAt(
                        door.getDoorLocation().getBlockX() >> 4,
                        door.getDoorLocation().getBlockZ() >> 4
                );

                // Teleport slightly to the center of the block
                player.closeInventory();
                player.teleport(door.getDoorLocation().clone().add(0.5, 0, 0.5));
                player.sendMessage(ChatColor.GREEN + "Teleported to room " + roomId + ".");
            }
        }
    }

    /**
     * Tiny fallback mapper from readable name to hotelId.
     * Normally we use localizedName, so this is mostly just safety.
     */
    private String nameToHotelId(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("disneyland hotel")) return "disneyland_hotel";
        if (n.contains("new york")) return "new_york";
        if (n.contains("sequoia")) return "sequoia";
        if (n.contains("newport")) return "newport_bay";
        if (n.contains("cheyenne")) return "cheyenne";
        if (n.contains("santa fe")) return "santa_fe";
        return null;
    }

    /**
     * Open a GUI showing all available rooms in this hotel + rooms owned by the player.
     * Paged: 45 rooms per page (slots 0-44); bottom row for nav / info.
     */
    private void openHotelRoomsGUI(Player player, String hotelId, int pageIndex) {
        List<HotelDoor> rooms = new ArrayList<>();
        UUID uuid = player.getUniqueId();

        for (HotelDoor door : plugin.getHotelManager().getAllHotelDoors()) {
            if (door.getHotelId() == null) continue;
            if (!door.getHotelId().equalsIgnoreCase(hotelId)) continue;

            // Show if room is free OR owned by this player
            if (!door.isOccupied() || uuid.equals(door.getOwner())) {
                rooms.add(door);
            }
        }

        // Sort by room ID for nicer ordering
        rooms.sort(Comparator.comparing(HotelDoor::getRoomID, String.CASE_INSENSITIVE_ORDER));

        if (rooms.isEmpty()) {
            player.sendMessage(ChatColor.RED +
                    "There are no available rooms or owned rooms in this hotel right now.");
            return;
        }

        int totalRooms = rooms.size();
        int totalPages = (totalRooms + PAGE_SIZE - 1) / PAGE_SIZE;
        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= totalPages) pageIndex = totalPages - 1;

        int startIndex = pageIndex * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, totalRooms);

        String niceHotelName = HotelSelectGUI.formatHotelName(hotelId);

        Inventory inv = Bukkit.createInventory(
                null,
                INVENTORY_SIZE,
                ChatColor.DARK_BLUE + niceHotelName + ROOMS_SUFFIX
        );

        // Fill room items for this page (slots 0-44)
        int slot = 0;
        for (int i = startIndex; i < endIndex && slot < PAGE_SIZE; i++, slot++) {
            HotelDoor door = rooms.get(i);

            ItemStack item = new ItemStack(Material.OAK_DOOR);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.AQUA + "Room " + door.getRoomID());

                // store hotelId|roomId so we can teleport on click
                meta.setLocalizedName(
                        (door.getHotelId() + "|" + door.getRoomID()).toLowerCase(Locale.ROOT)
                );

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Hotel: " + ChatColor.YELLOW + niceHotelName);
                lore.add(ChatColor.GRAY + "Room ID: " + ChatColor.AQUA + door.getRoomID());

                String priceDisplay = (door.getCurrencyType() == CurrencyType.VAULT
                        ? plugin.getHotelManager().getEconomy().format(door.getPrice())
                        : (int) door.getPrice() + " " + door.getCurrencyType().getDisplayName());
                lore.add(ChatColor.GRAY + "Price: " + ChatColor.GOLD + priceDisplay);

                if (door.isOccupied()) {
                    lore.add(ChatColor.RED + "Status: Occupied");
                    lore.add(ChatColor.DARK_RED + "You already own this room.");
                } else {
                    lore.add(ChatColor.GREEN + "Status: Available");
                    lore.add(ChatColor.YELLOW + "Use /hotel buy " + door.getRoomID() + " to claim.");
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }

            inv.setItem(slot, item);
        }

        // --- Navigation / info row (bottom row) ---

        // Previous page button
        if (pageIndex > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta m = prev.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.YELLOW + "Previous page");
                m.setLocalizedName("nav:prev:" + hotelId.toLowerCase(Locale.ROOT) + ":" + pageIndex);
                prev.setItemMeta(m);
            }
            inv.setItem(SLOT_PREV, prev);
        }

        // Page info in the middle
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.AQUA + "Page " + (pageIndex + 1) + " / " + totalPages);
            infoMeta.setLocalizedName("nav:info:" + hotelId.toLowerCase(Locale.ROOT) + ":" + pageIndex);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Total rooms: " + ChatColor.YELLOW + totalRooms);
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(SLOT_INFO, info);

        // Next page button
        if (pageIndex < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta m = next.getItemMeta();
            if (m != null) {
                m.setDisplayName(ChatColor.YELLOW + "Next page");
                m.setLocalizedName("nav:next:" + hotelId.toLowerCase(Locale.ROOT) + ":" + pageIndex);
                next.setItemMeta(m);
            }
            inv.setItem(SLOT_NEXT, next);
        }

        player.openInventory(inv);

        // Debug info
        player.sendMessage(ChatColor.YELLOW + "[HotelDebug] Found " + totalRooms
                + " rooms for hotel '" + hotelId + "' (free or owned by you).");
        player.sendMessage(ChatColor.YELLOW + "[HotelDebug] Showing page "
                + (pageIndex + 1) + "/" + totalPages + " (" + (endIndex - startIndex) + " rooms on this page).");
    }
}
