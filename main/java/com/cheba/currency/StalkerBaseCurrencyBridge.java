package com.cheba.currency;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

public final class StalkerBaseCurrencyBridge
        implements TraderCurrencyBridge {

    private static final String STORAGE_CLASS =
            "com.cheba.stalkerbase.currency.storage.CurrencyStorage";

    private static final String BALANCE_SENDER_CLASS =
            "com.cheba.stalkerbase.currency.network.TraderBalancePacketSender";

    private Method getBalanceMethod;
    private Method withdrawMethod;
    private Method depositMethod;
    private Method sendBalanceUpdateMethod;

    private boolean available;
    private boolean runtimeErrorLogged;
    private boolean senderErrorLogged;

    public StalkerBaseCurrencyBridge() {
        loadCurrencyMethods();
        loadBalanceSenderMethod();
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
                    "[TraderCurrency] Хранилище валюты подключено"
            );

        } catch (ReflectiveOperationException
                 | LinkageError
                 | SecurityException e) {

            available = false;

            logRuntimeError(
                    "Не удалось подключить CurrencyStorage: "
                            + e.getMessage()
            );
        }
    }

    private void loadBalanceSenderMethod() {
        try {
            Class<?> sender = Class.forName(
                    BALANCE_SENDER_CLASS,
                    false,
                    StalkerBaseCurrencyBridge.class.getClassLoader()
            );

            sendBalanceUpdateMethod = sender.getMethod(
                    "sendBalanceUpdate",
                    EntityPlayerMP.class,
                    long.class,
                    long.class
            );

        } catch (ReflectiveOperationException
                 | LinkageError
                 | SecurityException e) {

            sendBalanceUpdateMethod = null;

            logSenderError(
                    "Не найден отправитель обновления баланса: "
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
                    "CurrencyStorage.getBalance вернул неожиданный тип"
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
    public boolean withdraw(EntityPlayer player, long amount) {
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
    public boolean deposit(EntityPlayer player, long amount) {
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

        if (sendBalanceUpdateMethod == null) {
            logSenderError(
                    "Обновление HUD не отправлено: "
                            + "отправитель баланса недоступен"
            );
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        long newBalance = getBalance(player);

        try {
            sendBalanceUpdateMethod.invoke(
                    null,
                    serverPlayer,
                    delta,
                    newBalance
            );

        } catch (ReflectiveOperationException
                 | RuntimeException e) {

            logSenderError(
                    "Не удалось отправить обновление HUD: "
                            + e.getMessage()
            );
        }
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

    private void logSenderError(String message) {
        if (senderErrorLogged) {
            return;
        }

        senderErrorLogged = true;

        System.err.println(
                "[TraderCurrency] " + message
        );
    }
}
