package com.cheba.trader;

import com.cheba.container.TraderSellContainer;
import com.cheba.currency.TraderCurrencyBridge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

public final class TraderSellService {

    private final TraderCurrencyBridge currency;

    public TraderSellService(TraderCurrencyBridge currency) {
        this.currency = currency;
    }

    public enum Status {
        SUCCESS,
        INVALID_CONTAINER,
        INVALID_NPC,
        TOO_FAR,
        NOT_TRADER,
        EMPTY,
        ITEM_NOT_BUYABLE,
        CURRENCY_UNAVAILABLE,
        PAYMENT_FAILED,
        INVALID_AMOUNT
    }

    public static final class Outcome {

        private final Status status;
        private final long amount;

        public Outcome(Status status, long amount) {
            this.status = status;
            this.amount = amount;
        }

        public Status getStatus() {
            return status;
        }

        public long getAmount() {
            return amount;
        }
    }

    public Outcome sell(EntityPlayerMP player, int npcEntityId) {
        if (player == null
                || !(player.openContainer instanceof TraderSellContainer)) {
            return new Outcome(Status.INVALID_CONTAINER, 0L);
        }

        TraderSellContainer container =
                (TraderSellContainer) player.openContainer;

        if (!container.canInteractWith(player)) {
            return new Outcome(Status.TOO_FAR, 0L);
        }

        Entity entity = container.getNpc();

        if (!(entity instanceof EntityNPCInterface)) {
            return new Outcome(Status.INVALID_NPC, 0L);
        }

        EntityNPCInterface npc = (EntityNPCInterface) entity;

        if (npc.getEntityId() != npcEntityId
                || npc.isDead
                || npc.world != player.world) {
            return new Outcome(Status.INVALID_NPC, 0L);
        }

        if (!(npc.roleInterface instanceof RoleTrader)) {
            return new Outcome(Status.NOT_TRADER, 0L);
        }

        RoleTrader role = (RoleTrader) npc.roleInterface;
        IInventory escrow = container.getEscrowInventory();

        long total = 0L;
        boolean hasItems = false;

        // Сначала проверяем каждый предмет ничего не удаляя
        for (int i = 0; i < escrow.getSizeInventory(); i++) {
            ItemStack stack = escrow.getStackInSlot(i);

            if (stack.isEmpty()) {
                continue;
            }

            hasItems = true;

            long unitPrice = findSellPrice(role, stack);

            if (unitPrice <= 0L) {
                return new Outcome(
                        Status.ITEM_NOT_BUYABLE,
                        0L
                );
            }

            long count = stack.getCount();

            if (count > (Long.MAX_VALUE - total) / unitPrice) {
                return new Outcome(
                        Status.INVALID_AMOUNT,
                        0L
                );
            }

            total += unitPrice * count;
        }

        if (!hasItems) {
            return new Outcome(Status.EMPTY, 0L);
        }

        if (total <= 0L) {
            return new Outcome(Status.INVALID_AMOUNT, 0L);
        }

        if (currency == null || !currency.isAvailable()) {
            return new Outcome(
                    Status.CURRENCY_UNAVAILABLE,
                    0L
            );
        }

        // Если начисление не удалось предметы остаются в буфере
        if (!currency.deposit(player, total)) {
            return new Outcome(Status.PAYMENT_FAILED, 0L);
        }

        // Удаление предметов только после успешного начисления
        container.clearEscrowAfterSale();

        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();

        return new Outcome(Status.SUCCESS, total);
    }

    private long findSellPrice(
            RoleTrader role,
            ItemStack stack
    ) {
        for (NpcTradeConfig offer : role.getDynamicOffers()) {
            ItemStack configured = offer.getResult();

            if (configured.isEmpty()) {
                continue;
            }

            boolean sameItemAndMetadata =
                    ItemStack.areItemsEqual(stack, configured);

            boolean sameNbt =
                    ItemStack.areItemStackTagsEqual(stack, configured);

            if (sameItemAndMetadata
                    && sameNbt
                    && offer.getSellPriceMoney() > 0L) {
                return offer.getSellPriceMoney();
            }
        }

        return 0L;
    }
}
