
package com.cheba.currency;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

public final class StalkerBaseCurrencyBridge
        implements TraderCurrencyBridge {

    private static final String STORAGE_CLASS =
            "com.cheba.stalkerbase.currency.storage.CurrencyStorage";

    private static final String HUD_HANDLER_CLASS =
            "com.cheba.em.network.EMPacketHandler";

    private Method getBalanceMethod;
    private Method withdrawMethod;
    private Method depositMethod;

    private Method moneyNotificationMethod;
    private Method balanceUpdateMethod;

    private boolean available;

    public StalkerBaseCurrencyBridge() {
        loadCurrencyMethods();
        loadHudMethods();
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

    private void loadHudMethods() {
        try {
            Class<?> handler = Class.forName(
                    HUD_HANDLER_CLASS,
                    false,
                    StalkerBaseCurrencyBridge.class.getClassLoader()
            );

            try {
                moneyNotificationMethod = handler.getMethod(
                        "sendMoneyPickupNotification",
                        EntityPlayerMP.class,
                        long.class,
                        long.class
                );
            } catch (ReflectiveOperationException
                     | SecurityException ignored) {
                moneyNotificationMethod = null;
            }

            try {
                balanceUpdateMethod = handler.getMethod(
                        "sendBalanceUpdate",
                        EntityPlayerMP.class,
                        long.class
                );
            } catch (ReflectiveOperationException
                     | SecurityException ignored) {
                balanceUpdateMethod = null;
            }

        } catch (ReflectiveOperationException
                 | LinkageError
                 | SecurityException ignored) {

            moneyNotificationMethod = null;
            balanceUpdateMethod = null;
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

        /*
         * Основной вариант: отправляем изменение суммы
         * и новый баланс, чтобы обновить HUD и уведомление
         */
        if (moneyNotificationMethod != null) {
            try {
                moneyNotificationMethod.invoke(
                        null,
                        serverPlayer,
                        delta,
                        newBalance
                );

                return;

            } catch (ReflectiveOperationException
                     | RuntimeException ignored) {
                // Пробуем запасной способ ниже.
            }
        }

        /*
         * Запасной вариант: отправляем только новый баланс
         */
        if (balanceUpdateMethod != null) {
            try {
                balanceUpdateMethod.invoke(
                        null,
                        serverPlayer,
                        newBalance
                );

            } catch (ReflectiveOperationException
                     | RuntimeException ignored) {
                // Уведомление недоступно; покупка уже обработана
            }
        }
    }

    private MinecraftServer getServer(EntityPlayer player) {
        if (player.world == null) {
            return null;
        }

        return player.world.getMinecraftServer();
    }
}
