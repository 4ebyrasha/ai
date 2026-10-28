package com.cheba.currency;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

public final class StalkerBaseCurrencyBridge
        implements TraderCurrencyBridge {

    private static final String STORAGE_CLASS =
            "com.cheba.stalkerbase.currency.storage.CurrencyStorage";

    private Method getBalanceMethod;
    private Method withdrawMethod;
    private Method depositMethod;

    private boolean available;

    public StalkerBaseCurrencyBridge() {
        try {
            Class<?> storage = Class.forName(
                    STORAGE_CLASS,
                    false,
                    StalkerBaseCurrencyBridge.class.getClassLoader()
            );

            getBalanceMethod = storage.getMethod(
                    "getBalance",
                    MinecraftServer.class,
                    String.class
            );

            withdrawMethod = storage.getMethod(
                    "tryWithdrawBalance",
                    MinecraftServer.class,
                    String.class,
                    long.class
            );

            depositMethod = storage.getMethod(
                    "tryAddBalance",
                    MinecraftServer.class,
                    String.class,
                    long.class
            );

            available = true;

        } catch (ReflectiveOperationException
                 | LinkageError
                 | SecurityException e) {

            available = false;

            System.err.println(
                    "[Trader] Не удалось подключить валюту StalkerBase: "
                            + e.getMessage()
            );
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public long getBalance(EntityPlayer player) {
        if (!available || player == null) {
            return 0L;
        }

        MinecraftServer server = getServer(player);

        if (server == null) {
            return 0L;
        }

        try {
            Object result = getBalanceMethod.invoke(
                    null,
                    server,
                    player.getName()
            );

            return result instanceof Number
                    ? ((Number) result).longValue()
                    : 0L;

        } catch (ReflectiveOperationException
                 | RuntimeException e) {
            return 0L;
        }
    }

    @Override
    public boolean withdraw(EntityPlayer player, long amount) {
        if (amount < 0 || !available || player == null) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        MinecraftServer server = getServer(player);

        if (server == null) {
            return false;
        }

        try {
            Object result = withdrawMethod.invoke(
                    null,
                    server,
                    player.getName(),
                    amount
            );

            return Boolean.TRUE.equals(result);

        } catch (ReflectiveOperationException
                 | RuntimeException e) {
            return false;
        }
    }

    @Override
    public boolean deposit(EntityPlayer player, long amount) {
        if (amount < 0 || !available || player == null) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        MinecraftServer server = getServer(player);

        if (server == null) {
            return false;
        }

        try {
            Object result = depositMethod.invoke(
                    null,
                    server,
                    player.getName(),
                    amount
            );

            return Boolean.TRUE.equals(result);

        } catch (ReflectiveOperationException
                 | RuntimeException e) {
            return false;
        }
    }

    private MinecraftServer getServer(EntityPlayer player) {
        if (player.world == null) {
            return null;
        }

        return player.world.getMinecraftServer();
    }
}
