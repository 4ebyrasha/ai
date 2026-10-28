package com.cheba.network;

import com.cheba.currency.StalkerBaseCurrencyBridge;
import com.cheba.trader.TraderTradeService;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import java.util.Map;
import java.util.WeakHashMap;

public final class TraderPacketService {

    private static final TraderTradeService TRADE_SERVICE =
            new TraderTradeService(new StalkerBaseCurrencyBridge());

    private static final Map<EntityPlayerMP, LastRequest> LAST_REQUESTS =
            new WeakHashMap<>();

    private TraderPacketService() {
    }

    public static void handleBuy(
            EntityPlayerMP player,
            int npcEntityId,
            String offerId
    ) {
        if (player == null || player.world.isRemote) {
            return;
        }

        if (offerId == null || offerId.trim().isEmpty()
                || offerId.length() > 128) {
            sendMessage(player, "Некорректное предложение.");
            return;
        }

        Entity entity = player.world.getEntityByID(npcEntityId);

        if (!(entity instanceof EntityNPCInterface)) {
            sendMessage(player, "Торговец не найден.");
            return;
        }

        EntityNPCInterface npc = (EntityNPCInterface) entity;

        if (!(npc.roleInterface instanceof RoleTrader)) {
            sendMessage(player, "Этот NPC не является торговцем.");
            return;
        }

        if (player.getDistanceSq(npc) > 64.0D) {
            sendMessage(player, "Вы слишком далеко от торговца.");
            return;
        }

        String requestKey = npcEntityId + "|" + offerId;
        long currentTick = player.world.getTotalWorldTime();

        LastRequest previous = LAST_REQUESTS.get(player);

        if (previous != null
                && previous.key.equals(requestKey)
                && currentTick - previous.tick < 2L) {
            return;
        }

        LAST_REQUESTS.put(
                player,
                new LastRequest(requestKey, currentTick)
        );

        TraderTradeService.Result result = TRADE_SERVICE.buy(
                player,
                (RoleTrader) npc.roleInterface,
                offerId
        );

        sendResult(player, result);

        if (result == TraderTradeService.Result.REFUND_FAILED) {
            System.err.println(
                    "[ChebaTrader] Не удалось вернуть оплату игроку "
                            + player.getName()
                            + " при покупке у NPC "
                            + npc.getEntityId()
            );
        }
    }

    private static void sendResult(
            EntityPlayerMP player,
            TraderTradeService.Result result
    ) {
        if (result == null) {
            sendMessage(player, "Не удалось выполнить покупку.");
            return;
        }

        switch (result) {
            case SUCCESS:
                sendMessage(player, "Покупка выполнена.");
                break;

            case SERVER_ONLY:
                sendMessage(player, "Покупка доступна только на сервере.");
                break;

            case TOO_FAR:
                sendMessage(player, "Вы слишком далеко от торговца.");
                break;

            case INVALID_OFFER:
                sendMessage(player, "Предложение больше недоступно.");
                break;

            case CURRENCY_UNAVAILABLE:
                sendMessage(player, "Валютная система недоступна.");
                break;

            case NOT_ENOUGH_MONEY:
                sendMessage(player, "Недостаточно денег.");
                break;

            case PAYMENT_FAILED:
                sendMessage(player, "Не удалось списать деньги.");
                break;

            case NOT_ENOUGH_ITEMS:
                sendMessage(player, "Недостаточно предметов для оплаты.");
                break;

            case OUT_OF_STOCK:
                sendMessage(player, "Товар закончился.");
                break;

            case DAILY_LIMIT_REACHED:
                sendMessage(player, "Достигнут дневной лимит покупки.");
                break;

            case INVENTORY_CHANGED:
                sendMessage(player, "Инвентарь изменился. Повторите покупку.");
                break;

            case REFUND_FAILED:
                sendMessage(
                        player,
                        "Ошибка возврата оплаты. Обратитесь к администратору."
                );
                break;

            default:
                sendMessage(player, "Покупка не выполнена.");
                break;
        }
    }

    private static void sendMessage(EntityPlayerMP player, String message) {
        player.sendMessage(new TextComponentString(message));
    }

    private static final class LastRequest {
        private final String key;
        private final long tick;

        private LastRequest(String key, long tick) {
            this.key = key;
            this.tick = tick;
        }
    }
}
