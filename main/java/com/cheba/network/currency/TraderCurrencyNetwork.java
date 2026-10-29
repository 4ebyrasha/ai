
package com.cheba.network.currency;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class TraderCurrencyNetwork {

    public static final String CHANNEL_NAME = "cheba_trade_money";

    private static SimpleNetworkWrapper INSTANCE;

    private TraderCurrencyNetwork() {
    }

    public static synchronized void init() {
        if (INSTANCE != null) {
            return;
        }

        INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(
                CHANNEL_NAME
        );

        INSTANCE.registerMessage(
                PacketTraderBalanceUpdate.Handler.class,
                PacketTraderBalanceUpdate.class,
                0,
                Side.CLIENT
        );

        System.out.println(
                "[TraderCurrency] Канал "
                        + CHANNEL_NAME
                        + " инициализирован."
        );
    }

    public static void sendBalanceUpdate(
            EntityPlayerMP player,
            long delta,
            long balance
    ) {
        if (player == null) {
            return;
        }

        if (INSTANCE == null) {
            System.err.println(
                    "[TraderCurrency] Канал не инициализирован. "
                            + "Баланс не отправлен."
            );
            return;
        }

        INSTANCE.sendTo(
                new PacketTraderBalanceUpdate(delta, balance),
                player
        );

        System.out.println(
                "[TraderCurrency] Обновление отправлено игроку "
                        + player.getName()
                        + ": delta="
                        + delta
                        + ", balance="
                        + balance
        );
    }
}
