package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Привязка NPC к самостоятельному торговому профилю.
 * <p>
 * Не зависит от RoleTrader и может существовать у NPC
 * с любой другой ролью или вообще без роли.
 */
public class TraderBinding {

    private final String traderId;

    public TraderBinding(String traderId) {
        if (traderId == null || traderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trader ID cannot be empty");
        }

        this.traderId = traderId.trim();
    }

    public String getTraderId() {
        return traderId;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("TraderId", traderId);
        return tag;
    }

    public static TraderBinding readFromNBT(NBTTagCompound tag) {
        String id = tag.getString("TraderId");

        if (id.trim().isEmpty()) {
            return null;
        }

        return new TraderBinding(id);
    }
}
