package com.cheba.trader;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

public class NpcBuybackConfig {

    private final String id;
    private ItemStack item = ItemStack.EMPTY;
    private long priceMoney = 0L;

    public NpcBuybackConfig() {
        this(UUID.randomUUID().toString());
    }

    public NpcBuybackConfig(String id) {
        this.id = id == null || id.trim().isEmpty()
                ? UUID.randomUUID().toString()
                : id;
    }

    public NpcBuybackConfig(ItemStack item, long priceMoney) {
        this();
        setItem(item);
        setPriceMoney(priceMoney);
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return item.isEmpty() ? ItemStack.EMPTY : item.copy();
    }

    public void setItem(ItemStack item) {
        if (item == null || item.isEmpty()) {
            this.item = ItemStack.EMPTY;
            return;
        }

        this.item = item.copy();
        this.item.setCount(1);
    }

    public long getPriceMoney() {
        return priceMoney;
    }

    public void setPriceMoney(long priceMoney) {
        this.priceMoney = Math.max(0L, priceMoney);
    }

    public boolean matches(ItemStack stack) {
        if (stack == null || stack.isEmpty() || item.isEmpty()) {
            return false;
        }

        return ItemStack.areItemsEqual(item, stack)
                && ItemStack.areItemStackTagsEqual(item, stack);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setString("Id", id);
        tag.setTag("Item", item.writeToNBT(new NBTTagCompound()));
        tag.setLong("PriceMoney", priceMoney);

        return tag;
    }

    public static NpcBuybackConfig readFromNBT(NBTTagCompound tag) {
        NpcBuybackConfig config =
                new NpcBuybackConfig(tag.getString("Id"));

        if (tag.hasKey("Item", 10)) {
            config.setItem(new ItemStack(tag.getCompoundTag("Item")));
        }

        config.setPriceMoney(tag.getLong("PriceMoney"));

        return config;
    }
}
