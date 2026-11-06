package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nonnull;

public class TraderRuntimeState extends WorldSavedData {

    private static final String DATA_NAME = "ChebaTraderRuntime";

    private long gameDay = -1L;

    private NBTTagCompound stockSales = new NBTTagCompound();
    private NBTTagCompound playerPurchases = new NBTTagCompound();

    public TraderRuntimeState(String name) {
        super(name);
    }

    public static TraderRuntimeState get(World world) {
        if (world == null || world.isRemote) {
            return null;
        }

        MapStorage storage = world.getPerWorldStorage();

        WorldSavedData savedData = storage.getOrLoadData(
                TraderRuntimeState.class,
                DATA_NAME
        );

        TraderRuntimeState state;

        if (savedData instanceof TraderRuntimeState) {
            state = (TraderRuntimeState) savedData;
        } else {
            state = new TraderRuntimeState(DATA_NAME);
            storage.setData(DATA_NAME, state);
        }

        long currentDay = world.getTotalWorldTime() / 24000L;

        state.ensureDay(currentDay);

        return state;
    }

    private void ensureDay(long currentDay) {
        if (gameDay != currentDay) {
            gameDay = currentDay;

            stockSales = new NBTTagCompound();
            playerPurchases = new NBTTagCompound();

            markDirty();
        }
    }

    public long getSoldCount(String key) {
        if (key == null || key.isEmpty()) {
            return 0L;
        }

        return stockSales.getLong(key);
    }

    public long getPlayerPurchaseCount(String key) {
        if (key == null || key.isEmpty()) {
            return 0L;
        }

        return playerPurchases.getLong(key);
    }

    public void recordPurchase(
            String stockKey,
            String playerKey,
            long quantity
    ) {
        if (quantity <= 0L) {
            return;
        }

        boolean changed = false;

        if (stockKey != null && !stockKey.isEmpty()) {
            long current = stockSales.getLong(stockKey);

            stockSales.setLong(
                    stockKey,
                    addSaturated(current, quantity)
            );

            changed = true;
        }

        if (playerKey != null && !playerKey.isEmpty()) {
            long current = playerPurchases.getLong(playerKey);

            playerPurchases.setLong(
                    playerKey,
                    addSaturated(current, quantity)
            );

            changed = true;
        }

        if (changed) {
            markDirty();
        }
    }

    private long addSaturated(long current, long increment) {
        if (increment > 0L && current > Long.MAX_VALUE - increment) {
            return Long.MAX_VALUE;
        }

        return current + increment;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        gameDay = tag.hasKey("GameDay", 4)
                ? tag.getLong("GameDay")
                : -1L;

        stockSales = tag.hasKey("StockSales", 10)
                ? tag.getCompoundTag("StockSales").copy()
                : new NBTTagCompound();

        playerPurchases = tag.hasKey("PlayerPurchases", 10)
                ? tag.getCompoundTag("PlayerPurchases").copy()
                : new NBTTagCompound();
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setLong("GameDay", gameDay);
        tag.setTag("StockSales", stockSales.copy());
        tag.setTag("PlayerPurchases", playerPurchases.copy());

        return tag;
    }
}
