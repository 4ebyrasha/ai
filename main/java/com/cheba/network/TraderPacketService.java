package com.cheba.network;

import com.cheba.currency.StalkerBaseCurrencyBridge;
import com.cheba.trader.TraderSellService;
import com.cheba.trader.TraderTradeService;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import java.util.Map;
import java.util.WeakHashMap;

public final class TraderPacketService {

    private static final StalkerBaseCurrencyBridge CURRENCY =
            new StalkerBaseCurrencyBridge();

    private static final TraderTradeService TRADE_SERVICE =
            new TraderTradeService(CURRENCY);

    private static final TraderSellService SELL_SERVICE =
            new TraderSellService(CURRENCY);

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
            sendMessage(player, "cheba.trader.message.invalid_offer");
            return;
        }

        Entity entity = player.world.getEntityByID(npcEntityId);

        if (!(entity instanceof EntityNPCInterface)) {
            sendMessage(player, "cheba.trader.message.not_found");
            return;
        }

        EntityNPCInterface npc = (EntityNPCInterface) entity;

        if (!(npc.roleInterface instanceof RoleTrader)) {
            sendMessage(player, "cheba.trader.message.not_trader");
            return;
        }

        if (player.getDistanceSq(npc) > 64.0D) {
            sendMessage(player, "cheba.trader.message.too_far");
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

        sendBuyResult(player, result);

        if (result == TraderTradeService.Result.REFUND_FAILED) {
            System.err.println(
                    "[ChebaTrader] Failed to refund payment to player "
                            + player.getName()
                            + " for NPC "
                            + npc.getEntityId()
            );
        }
    }

    public static void handleSell(
            EntityPlayerMP player,
            int npcEntityId
    ) {
        if (player == null || player.world.isRemote) {
            return;
        }

        TraderSellService.Outcome outcome =
                SELL_SERVICE.sell(player, npcEntityId);

        switch (outcome.getStatus()) {
            case SUCCESS:
                player.sendMessage(new TextComponentTranslation(
                        "cheba.trader.message.sell_success",
                        outcome.getAmount()
                ));
                break;

            case INVALID_CONTAINER:
                sendMessage(
                        player,
                        "cheba.trader.message.sell.invalid_container"
                );
                break;

            case INVALID_NPC:
                sendMessage(
                        player,
                        "cheba.trader.message.not_found"
                );
                break;

            case TOO_FAR:
                sendMessage(
                        player,
                        "cheba.trader.message.too_far"
                );
                break;

            case NOT_TRADER:
                sendMessage(
                        player,
                        "cheba.trader.message.not_trader"
                );
                break;

            case EMPTY:
                sendMessage(
                        player,
                        "cheba.trader.message.sell.empty"
                );
                break;

            case ITEM_NOT_BUYABLE:
                sendMessage(
                        player,
                        "cheba.trader.message.sell.item_not_buyable"
                );
                break;

            case CURRENCY_UNAVAILABLE:
                sendMessage(
                        player,
                        "cheba.trader.message.currency_unavailable"
                );
                break;

            case PAYMENT_FAILED:
                sendMessage(
                        player,
                        "cheba.trader.message.sell.payment_failed"
                );
                break;

            case INVALID_AMOUNT:
            default:
                sendMessage(
                        player,
                        "cheba.trader.message.generic_failure"
                );
                break;
        }
    }

    private static void sendBuyResult(
            EntityPlayerMP player,
            TraderTradeService.Result result
    ) {
        if (result == null) {
            sendMessage(
                    player,
                    "cheba.trader.message.generic_failure"
            );
            return;
        }

        switch (result) {
            case SUCCESS:
                sendMessage(
                        player,
                        "cheba.trader.message.buy_success"
                );
                break;

            case SERVER_ONLY:
                sendMessage(
                        player,
                        "cheba.trader.message.server_only"
                );
                break;

            case TOO_FAR:
                sendMessage(
                        player,
                        "cheba.trader.message.too_far"
                );
                break;

            case INVALID_OFFER:
                sendMessage(
                        player,
                        "cheba.trader.message.offer_unavailable"
                );
                break;

            case CURRENCY_UNAVAILABLE:
                sendMessage(
                        player,
                        "cheba.trader.message.currency_unavailable"
                );
                break;

            case NOT_ENOUGH_MONEY:
                sendMessage(
                        player,
                        "cheba.trader.message.not_enough_money"
                );
                break;

            case PAYMENT_FAILED:
                sendMessage(
                        player,
                        "cheba.trader.message.payment_failed"
                );
                break;

            case NOT_ENOUGH_ITEMS:
                sendMessage(
                        player,
                        "cheba.trader.message.not_enough_items"
                );
                break;

            case OUT_OF_STOCK:
                sendMessage(
                        player,
                        "cheba.trader.message.out_of_stock"
                );
                break;

            case DAILY_LIMIT_REACHED:
                sendMessage(
                        player,
                        "cheba.trader.message.daily_limit"
                );
                break;

            case INVENTORY_CHANGED:
                sendMessage(
                        player,
                        "cheba.trader.message.inventory_changed"
                );
                break;

            case REFUND_FAILED:
                sendMessage(
                        player,
                        "cheba.trader.message.refund_failed"
                );
                break;

            default:
                sendMessage(
                        player,
                        "cheba.trader.message.generic_failure"
                );
                break;
        }
    }

    private static void sendMessage(
            EntityPlayerMP player,
            String translationKey
    ) {
        player.sendMessage(
                new TextComponentTranslation(translationKey)
        );
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
