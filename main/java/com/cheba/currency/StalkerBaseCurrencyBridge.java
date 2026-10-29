
package com.cheba.currency;

import com.cheba.network.currency.TraderCurrencyNetwork;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
    private boolean runtimeErrorLogged;

    public StalkerBaseCurrencyBridge() {
        loadCurrencyMethods();
    }

    private void loadCurrencyMethods() {
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

            System.out.println(
                    "[TraderCurrency] Хранилище валюты подключено."
            );

        } catch (ReflectiveOperationException
                 | LinkageError
                 | SecurityException e) {

            available = false;

            System.err.println(
                    "[TraderCurrency] Не удалось подключить валюту. "
                            + "Класс или методы CurrencyStorage недоступны: "
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

            if (result instanceof Number) {
                return ((Number) result).longValue();
            }

            logRuntimeError(
                    "CurrencyStorage.getBalance вернул неожиданный тип."
            );

            return 0L;

        } catch (ReflectiveOperationException
                 | RuntimeException e) {
            logRuntimeError(
                    "Ошибка чтения баланса: " + e.getMessage()
            );

            return 0L;
        }
    }

    @Override
    public boolean withdraw(
            EntityPlayer player,
            long amount
    ) {
        if (amount < 0L || !available || player == null) {
            return false;
        }

        if (amount == 0L) {
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
            logRuntimeError(
                    "Ошибка списания денег: " + e.getMessage()
            );

            return false;
        }
    }

    @Override
    public boolean deposit(
            EntityPlayer player,
            long amount
    ) {
        if (amount < 0L || !available || player == null) {
            return false;
        }

        if (amount == 0L) {
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
            logRuntimeError(
                    "Ошибка начисления денег: " + e.getMessage()
            );

            return false;
        }
    }

    @Override
    public void notifyBalanceChange(
            EntityPlayer player,
            long delta
    ) {
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        long newBalance = getBalance(player);

        TraderCurrencyNetwork.sendBalanceUpdate(
                serverPlayer,
                delta,
                newBalance
        );
    }

    private MinecraftServer getServer(EntityPlayer player) {
        if (player.world == null) {
            return null;
        }

        return player.world.getMinecraftServer();
    }

    private void logRuntimeError(String message) {
        if (runtimeErrorLogged) {
            return;
        }

        runtimeErrorLogged = true;

        System.err.println(
                "[TraderCurrency] " + message
        );
    }
}
