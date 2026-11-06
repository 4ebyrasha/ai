
package com.cheba.currency;

import net.minecraft.entity.player.EntityPlayer;

public interface TraderCurrencyBridge {

    boolean isAvailable();

    long getBalance(EntityPlayer player);

    boolean withdraw(EntityPlayer player, long amount);

    boolean deposit(EntityPlayer player, long amount);

    /**
     * Сообщает об изменении баланса.
     * Мост валюты может реализовать отправку обновления HUD.
     */
    default void notifyBalanceChange(
            EntityPlayer player,
            long delta
    ) {
    }
}
