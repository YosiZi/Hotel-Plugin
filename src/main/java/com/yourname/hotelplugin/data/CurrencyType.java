package com.yourname.hotelplugin.data;

import org.bukkit.Material; // Make sure this import is present!

public enum CurrencyType {
    VAULT("Vault", null), // Explicitly set material to null for VAULT
    DIAMOND("Diamond", Material.DIAMOND),
    GOLD_INGOT("Gold Ingot", Material.GOLD_INGOT);

    private final String displayName;
    private final Material material;

    // Constructor for currencies with no associated material (like VAULT)
    CurrencyType(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }

    // Constructor (kept for backward compatibility if needed, but the two-arg one is better)
    // You could remove this if you ensure all enums use the two-arg constructor
    // CurrencyType(String displayName) {
    //     this(displayName, null);
    // }

    public String getDisplayName() {
        return displayName;
    }

    // <--- THIS METHOD IS LIKELY MISSING OR HAS A TYPO IN YOUR FILE --->
    public Material getMaterial() {
        return material;
    }
}