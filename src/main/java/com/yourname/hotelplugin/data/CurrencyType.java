package com.yourname.hotelplugin.data;

import org.bukkit.Material;

public enum CurrencyType {
    VAULT("Vault", (Material)null),
    DIAMOND("Diamond", Material.DIAMOND),
    GOLD_INGOT("Gold Ingot", Material.GOLD_INGOT);

    private final String displayName;
    private final Material material;

    private CurrencyType(String displayName, Material material) {
        this.displayName = displayName;
        this.material = material;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Material getMaterial() {
        return this.material;
    }
}
