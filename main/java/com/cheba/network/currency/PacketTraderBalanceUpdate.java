
package com.cheba.network.currency;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Method;

public class PacketTraderBalanceUpdate implements IMessage {

    private long delta;
    private long balance;

    public PacketTraderBalanceUpdate() {
    }

    public PacketTraderBalanceUpdate(
            long delta,
            long balance
    ) {
        this.delta = delta;
        this.balance = balance;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        delta = buf.readLong();
        balance = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(delta);
        buf.writeLong(balance);
    }

    public static class Handler implements
            IMessageHandler<PacketTraderBalanceUpdate, IMessage> {

        @Override
        public IMessage onMessage(
                final PacketTraderBalanceUpdate message,
                MessageContext context
        ) {
            if (context.side != Side.CLIENT) {
                return null;
            }

            try {
                Class<?> minecraftClass = Class.forName(
                        "net.minecraft.client.Minecraft"
                );

                final Object minecraft = minecraftClass
                        .getMethod("getMinecraft")
                        .invoke(null);

                Method scheduleMethod = minecraftClass.getMethod(
                        "addScheduledTask",
                        Runnable.class
                );

                scheduleMethod.invoke(
                        minecraft,
                        new Runnable() {
                            @Override
                            public void run() {
                                updateHud(
                                        message.delta,
                                        message.balance
                                );
                            }
                        }
                );

            } catch (ReflectiveOperationException
                     | RuntimeException e) {
                System.err.println(
                        "[TraderCurrency] Не удалось передать обновление "
                                + "в клиентский поток: "
                                + e.getMessage()
                );
            }

            return null;
        }

        private void updateHud(
                long delta,
                long balance
        ) {
            String hudClassName =
                    "com.cheba.stalkerbase.client.hud.CurrencyHUD";

            try {
                Class<?> hudClass = Class.forName(hudClassName);

                boolean changeUpdated = false;
                boolean balanceUpdated = false;

                try {
                    Method changeMethod = hudClass.getMethod(
                            "addChangeAmount",
                            long.class
                    );

                    changeMethod.invoke(null, delta);
                    changeUpdated = true;

                } catch (ReflectiveOperationException
                         | RuntimeException e) {
                    System.err.println(
                            "[TraderCurrency] Не удалось вызвать "
                                    + "CurrencyHUD.addChangeAmount: "
                                    + e.getMessage()
                    );
                }

                try {
                    Method balanceMethod = hudClass.getMethod(
                            "updateBalance",
                            long.class
                    );

                    balanceMethod.invoke(null, balance);
                    balanceUpdated = true;

                } catch (ReflectiveOperationException
                         | RuntimeException e) {
                    System.err.println(
                            "[TraderCurrency] Не удалось вызвать "
                                    + "CurrencyHUD.updateBalance: "
                                    + e.getMessage()
                    );
                }

                if (changeUpdated && balanceUpdated) {
                    System.out.println(
                            "[TraderCurrency] HUD обновлён. Баланс: "
                                    + balance
                                    + ", изменение: "
                                    + delta
                    );
                }

            } catch (ReflectiveOperationException
                     | LinkageError
                     | SecurityException e) {
                System.err.println(
                        "[TraderCurrency] CurrencyHUD не найден. "
                                + "Проверь наличие клиентского мода валюты. "
                                + e.getMessage()
                );
            }
        }
    }
}
