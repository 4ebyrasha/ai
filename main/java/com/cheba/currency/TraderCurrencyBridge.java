
package com.cheba.currency;

import net.minecraft.entity.player.EntityPlayer;

public interface TraderCurrencyBridge {

    boolean isAvailable();

    long getBalance(EntityPlayer player);

    boolean withdraw(EntityPlayer player, long amount);

    boolean deposit(EntityPlayer player, long amount);

    /**
     * Уведомляет клиент об изменении баланса.
     * Реализация по умолчанию ничего не делает
     */
    default void notifyBalanceChange(
            EntityPlayer player,
            long delta
    ) {
    }
}
