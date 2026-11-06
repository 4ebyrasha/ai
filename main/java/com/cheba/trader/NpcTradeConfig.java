package com.cheba.trader;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NpcTradeConfig {

    private static final int NBT_VERSION = 1;

    private final String id;

    // Предмет, который получает покупатель.
    private ItemStack result = ItemStack.EMPTY;

    // Предметы, требуемые для покупки.
    private final List<ItemStack> priceItems = new ArrayList<>();

    // Денежная цена, по которой игрок покупает товар.
    private long priceMoney = 0L;

    // Денежная цена, по которой NPC покупает товар у игрока.
    private long sellPriceMoney = 0L;

    // -1 означает отсутствие ограничения.
    private int maxPerDay = -1;
    private int dailyStock = -1;

    public NpcTradeConfig() {
        this(UUID.randomUUID().toString());
    }

    public NpcTradeConfig(String id) {
        this.id = id == null || id.trim().isEmpty()
                ? UUID.randomUUID().toString()
                : id;
    }

    public String getId() {
        return id;
    }

    public boolean matchesResult(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !result.isEmpty()
                && ItemStack.areItemsEqual(result, stack)
                && ItemStack.areItemStackTagsEqual(result, stack);
    }

    public ItemStack getResult() {
        return result.isEmpty()
                ? ItemStack.EMPTY
                : result.copy();
    }

    public void setResult(ItemStack result) {
        this.result = result == null || result.isEmpty()
                ? ItemStack.EMPTY
                : result.copy();
    }

    public List<ItemStack> getPriceItems() {
        List<ItemStack> copy = new ArrayList<>();

        for (ItemStack stack : priceItems) {
            copy.add(stack.copy());
        }

        return Collections.unmodifiableList(copy);
    }

    public int getPriceItemCount() {
        return priceItems.size();
    }

    public ItemStack getPriceItem(int index) {
        if (index < 0 || index >= priceItems.size()) {
            return ItemStack.EMPTY;
        }

        return priceItems.get(index).copy();
    }

    public void addPriceItem(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            priceItems.add(stack.copy());
        }
    }

    public void setPriceItem(int index, ItemStack stack) {
        if (index < 0 || index >= priceItems.size()) {
            return;
        }

        if (stack == null || stack.isEmpty()) {
            priceItems.remove(index);
        } else {
            priceItems.set(index, stack.copy());
        }
    }

    public void removePriceItem(int index) {
        if (index >= 0 && index < priceItems.size()) {
            priceItems.remove(index);
        }
    }

    public void clearPriceItems() {
        priceItems.clear();
    }

    public long getPriceMoney() {
        return priceMoney;
    }

    public void setPriceMoney(long priceMoney) {
        this.priceMoney = Math.max(0L, priceMoney);
    }

    public long getSellPriceMoney() {
        return sellPriceMoney;
    }

    public void setSellPriceMoney(long sellPriceMoney) {
        this.sellPriceMoney = Math.max(0L, sellPriceMoney);
    }

    public int getMaxPerDay() {
        return maxPerDay;
    }

    public void setMaxPerDay(int maxPerDay) {
        this.maxPerDay = Math.max(-1, maxPerDay);
    }

    public int getDailyStock() {
        return dailyStock;
    }

    public void setDailyStock(int dailyStock) {
        this.dailyStock = Math.max(-1, dailyStock);
    }

    public boolean hasPrice() {
        return priceMoney > 0L || !priceItems.isEmpty();
    }

    public boolean isValid() {
        return !result.isEmpty() && hasPrice();
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("Version", NBT_VERSION);
        tag.setString("Id", id);

        tag.setTag(
                "Result",
                result.writeToNBT(new NBTTagCompound())
        );

        tag.setLong("PriceMoney", priceMoney);
        tag.setLong("SellPriceMoney", sellPriceMoney);
        tag.setInteger("MaxPerDay", maxPerDay);
        tag.setInteger("DailyStock", dailyStock);

        NBTTagList items = new NBTTagList();

        for (ItemStack stack : priceItems) {
            if (!stack.isEmpty()) {
                items.appendTag(
                        stack.writeToNBT(new NBTTagCompound())
                );
            }
        }

        tag.setTag("PriceItems", items);

        return tag;
    }

    public static NpcTradeConfig readFromNBT(NBTTagCompound tag) {
        NpcTradeConfig config =
                new NpcTradeConfig(tag.getString("Id"));

        if (tag.hasKey("Result", 10)) {
            config.result = new ItemStack(
                    tag.getCompoundTag("Result")
            );
        }

        config.priceMoney = Math.max(
                0L,
                tag.getLong("PriceMoney")
        );

        config.sellPriceMoney = Math.max(
                0L,
                tag.getLong("SellPriceMoney")
        );

        if (tag.hasKey("MaxPerDay", 3)) {
            config.maxPerDay = Math.max(
                    -1,
                    tag.getInteger("MaxPerDay")
            );
        }

        if (tag.hasKey("DailyStock", 3)) {
            config.dailyStock = Math.max(
                    -1,
                    tag.getInteger("DailyStock")
            );
        }

        if (tag.hasKey("PriceItems", 9)) {
            NBTTagList items =
                    tag.getTagList("PriceItems", 10);

            for (int i = 0; i < items.tagCount(); i++) {
                ItemStack stack = new ItemStack(
                        items.getCompoundTagAt(i)
                );

                if (!stack.isEmpty()) {
                    config.priceItems.add(stack);
                }
            }
        }

        return config;
    }
}
