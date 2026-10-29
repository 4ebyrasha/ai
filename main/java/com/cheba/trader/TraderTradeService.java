package com.cheba.trader;

import com.cheba.currency.TraderCurrencyBridge;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import noppes.npcs.roles.RoleTrader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TraderTradeService {

    private static final int MAX_MULTIPLIER = 64;

    private final TraderCurrencyBridge currency;

    public TraderTradeService(TraderCurrencyBridge currency) {
        this.currency = currency;
    }

    public enum Result {
        SUCCESS,
        SERVER_ONLY,
        TOO_FAR,
        INVALID_OFFER,
        PROTECTED_ITEM,
        CURRENCY_UNAVAILABLE,
        NOT_ENOUGH_MONEY,
        PAYMENT_FAILED,
        NOT_ENOUGH_ITEMS,
        OUT_OF_STOCK,
        DAILY_LIMIT_REACHED,
        INVENTORY_CHANGED,
        INVENTORY_FULL,
        REFUND_FAILED
    }

    private static final class Removal {
        private final int slot;
        private final ItemStack template;
        private final int count;

        private Removal(int slot, ItemStack template, int count) {
            this.slot = slot;
            this.template = template.copy();
            this.count = count;
        }
    }

    private static final class Requirement {
        private final ItemStack template;
        private long count;

        private Requirement(ItemStack template) {
            this.template = template.copy();
            this.count = template.getCount();
        }
    }

    /** Старый вызов остаётся совместимым: один комплект. */
    public Result buy(
            EntityPlayer player,
            RoleTrader role,
            String offerId
    ) {
        return buy(player, role, offerId, 1);
    }

    /**
     * Покупает multiplier комплектов предложения.
     * Например, 5 предметов в предложении при multiplier=10
     * превращаются в 50 предметов.
     */
    public Result buy(
            EntityPlayer player,
            RoleTrader role,
            String offerId,
            int multiplier
    ) {
        if (player == null
                || role == null
                || offerId == null
                || multiplier < 1
                || multiplier > MAX_MULTIPLIER) {
            return Result.INVALID_OFFER;
        }

        World world = player.world;

        if (world.isRemote || role.npc.world.isRemote) {
            return Result.SERVER_ONLY;
        }

        if (player.world != role.npc.world
                || player.getDistanceSq(role.npc) > 64.0D) {
            return Result.TOO_FAR;
        }

        NpcTradeConfig offer = findOffer(role, offerId);

        if (offer == null || !offer.isValid()) {
            return Result.INVALID_OFFER;
        }

        ItemStack product = offer.getResult();

        if (product.isEmpty()) {
            return Result.INVALID_OFFER;
        }

        long outputCountLong =
                (long) product.getCount() * (long) multiplier;

        if (outputCountLong <= 0L
                || outputCountLong > Integer.MAX_VALUE) {
            return Result.INVALID_OFFER;
        }

        product.setCount((int) outputCountLong);

        long moneyPrice;

        try {
            moneyPrice = Math.multiplyExact(
                    offer.getPriceMoney(),
                    (long) multiplier
            );
        } catch (ArithmeticException e) {
            return Result.INVALID_OFFER;
        }

        List<ItemStack> scaledPriceItems = scalePriceItems(
                offer.getPriceItems(),
                multiplier
        );

        if (scaledPriceItems == null) {
            return Result.INVALID_OFFER;
        }

        TraderRuntimeState state = TraderRuntimeState.get(world);

        if (state == null) {
            return Result.SERVER_ONLY;
        }

        String marketKey = getMarketKey(role);
        String stockKey = marketKey + "|offer:" + offer.getId();
        String playerKey = stockKey + "|" + player.getUniqueID();

        long outputCount = outputCountLong;

        int dailyStock = offer.getDailyStock();
        int maxPerDay = offer.getMaxPerDay();

        if (dailyStock >= 0
                && state.getSoldCount(stockKey) + outputCount > dailyStock) {
            return Result.OUT_OF_STOCK;
        }

        if (maxPerDay >= 0
                && state.getPlayerPurchaseCount(playerKey) + outputCount
                > maxPerDay) {
            return Result.DAILY_LIMIT_REACHED;
        }

        List<Removal> removals = buildRemovalPlan(
                player,
                scaledPriceItems
        );

        if (removals == null) {
            return Result.NOT_ENOUGH_ITEMS;
        }

        if (moneyPrice > 0L) {
            if (currency == null || !currency.isAvailable()) {
                return Result.CURRENCY_UNAVAILABLE;
            }

            if (currency.getBalance(player) < moneyPrice) {
                return Result.NOT_ENOUGH_MONEY;
            }
        }

        if (isRemovalPlanInvalid(player, removals)) {
            return Result.INVENTORY_CHANGED;
        }

        if (!canFitProduct(player, removals, product)) {
            return Result.INVENTORY_FULL;
        }

        NonNullList<ItemStack> inventoryBackup =
                copyMainInventory(player);

        boolean moneyWithdrawn = false;

        if (moneyPrice > 0L) {
            if (!currency.withdraw(player, moneyPrice)) {
                return Result.PAYMENT_FAILED;
            }

            moneyWithdrawn = true;
        }

        if (isRemovalPlanInvalid(player, removals)) {
            if (moneyWithdrawn
                    && !currency.deposit(player, moneyPrice)) {
                currency.notifyBalanceChange(player, -moneyPrice);
                return Result.REFUND_FAILED;
            }

            return Result.INVENTORY_CHANGED;
        }

        if (!canFitProduct(player, removals, product)) {
            if (moneyWithdrawn
                    && !currency.deposit(player, moneyPrice)) {
                currency.notifyBalanceChange(player, -moneyPrice);
                return Result.REFUND_FAILED;
            }

            return Result.INVENTORY_FULL;
        }

        applyRemovalPlan(player, removals);

        if (!deliverProduct(player, product)) {
            restoreMainInventory(player, inventoryBackup);

            if (moneyWithdrawn
                    && !currency.deposit(player, moneyPrice)) {
                currency.notifyBalanceChange(player, -moneyPrice);
                return Result.REFUND_FAILED;
            }

            return Result.INVENTORY_CHANGED;
        }

        state.recordPurchase(
                stockKey,
                playerKey,
                outputCount
        );

        if (moneyPrice > 0L) {
            currency.notifyBalanceChange(player, -moneyPrice);
        }

        return Result.SUCCESS;
    }

    private NpcTradeConfig findOffer(
            RoleTrader role,
            String offerId
    ) {
        for (NpcTradeConfig offer : role.getDynamicOffers()) {
            if (offerId.equals(offer.getId())) {
                return offer;
            }
        }

        return null;
    }

    private String getMarketKey(RoleTrader role) {
        String market = role.getMarket();

        if (market != null && !market.trim().isEmpty()) {
            return "market:" + market.trim().toLowerCase(Locale.ROOT);
        }

        return "npc:" + role.npc.getUniqueID();
    }

    private List<ItemStack> scalePriceItems(
            List<ItemStack> priceItems,
            int multiplier
    ) {
        List<ItemStack> scaled = new ArrayList<>();

        for (ItemStack original : priceItems) {
            if (original == null || original.isEmpty()) {
                continue;
            }

            if (TraderItemProtection.hasProtectedNbt(original)) {
                // Нельзя использовать защищённый предмет как бартерную оплату.
                return null;
            }

            long scaledCount =
                    (long) original.getCount() * multiplier;

            if (scaledCount <= 0L || scaledCount > Integer.MAX_VALUE) {
                return null;
            }

            ItemStack copy = original.copy();
            copy.setCount((int) scaledCount);
            scaled.add(copy);
        }

        return scaled;
    }

    private List<Removal> buildRemovalPlan(
            EntityPlayer player,
            List<ItemStack> requirements
    ) {
        List<Requirement> required = new ArrayList<>();

        for (ItemStack stack : requirements) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            Requirement found = null;

            for (Requirement requirement : required) {
                if (sameItemAndTag(requirement.template, stack)) {
                    found = requirement;
                    break;
                }
            }

            if (found == null) {
                required.add(new Requirement(stack));
            } else {
                found.count += stack.getCount();
            }
        }

        List<Removal> plan = new ArrayList<>();
        NonNullList<ItemStack> inventory = player.inventory.mainInventory;

        for (Requirement requirement : required) {
            long remaining = requirement.count;
            long available = 0L;

            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()
                        && sameItemAndTag(stack, requirement.template)) {
                    available += stack.getCount();
                }
            }

            if (available < remaining) {
                return null;
            }

            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);

                if (stack.isEmpty()
                        || !sameItemAndTag(stack, requirement.template)) {
                    continue;
                }

                int take = (int) Math.min(
                        remaining,
                        stack.getCount()
                );

                if (take > 0) {
                    plan.add(new Removal(slot, stack, take));
                    remaining -= take;
                }

                if (remaining == 0L) {
                    break;
                }
            }
        }

        return plan;
    }

    private boolean isRemovalPlanInvalid(
            EntityPlayer player,
            List<Removal> plan
    ) {
        for (Removal removal : plan) {
            if (removal.slot < 0
                    || removal.slot >= player.inventory.mainInventory.size()) {
                return true;
            }

            ItemStack stack = player.inventory.getStackInSlot(removal.slot);

            if (!sameItemAndTag(stack, removal.template)
                    || stack.getCount() < removal.count) {
                return true;
            }
        }

        return false;
    }

    private void applyRemovalPlan(
            EntityPlayer player,
            List<Removal> plan
    ) {
        for (Removal removal : plan) {
            ItemStack stack = player.inventory.getStackInSlot(removal.slot);

            if (stack.getCount() == removal.count) {
                player.inventory.setInventorySlotContents(
                        removal.slot,
                        ItemStack.EMPTY
                );
            } else {
                stack.shrink(removal.count);
            }
        }

        player.inventory.markDirty();
    }

    private boolean canFitProduct(
            EntityPlayer player,
            List<Removal> removals,
            ItemStack product
    ) {
        NonNullList<ItemStack> simulated = copyMainInventory(player);

        for (Removal removal : removals) {
            if (removal.slot < 0 || removal.slot >= simulated.size()) {
                return false;
            }

            ItemStack stack = simulated.get(removal.slot);

            if (!sameItemAndTag(stack, removal.template)
                    || stack.getCount() < removal.count) {
                return false;
            }

            if (stack.getCount() == removal.count) {
                simulated.set(removal.slot, ItemStack.EMPTY);
            } else {
                stack.shrink(removal.count);
            }
        }

        return insertProductIntoInventory(simulated, product);
    }

    private boolean deliverProduct(EntityPlayer player, ItemStack product) {
        boolean inserted = insertProductIntoInventory(
                player.inventory.mainInventory,
                product
        );

        if (inserted) {
            player.inventory.markDirty();
            player.inventoryContainer.detectAndSendChanges();
        }

        return inserted;
    }

    private boolean insertProductIntoInventory(
            NonNullList<ItemStack> inventory,
            ItemStack product
    ) {
        if (product == null || product.isEmpty()) {
            return true;
        }

        ItemStack remaining = product.copy();

        for (int slot = 0;
             slot < inventory.size() && !remaining.isEmpty();
             slot++) {

            ItemStack stack = inventory.get(slot);

            if (stack.isEmpty() || !sameItemAndTag(stack, remaining)) {
                continue;
            }

            int maxSize = Math.min(
                    stack.getMaxStackSize(),
                    remaining.getMaxStackSize()
            );

            int space = Math.max(0, maxSize - stack.getCount());
            int moved = Math.min(space, remaining.getCount());

            if (moved > 0) {
                stack.grow(moved);
                remaining.shrink(moved);
            }
        }

        for (int slot = 0;
             slot < inventory.size() && !remaining.isEmpty();
             slot++) {

            if (!inventory.get(slot).isEmpty()) {
                continue;
            }

            int maxSize = remaining.getMaxStackSize();

            if (maxSize <= 0) {
                return false;
            }

            int moved = Math.min(remaining.getCount(), maxSize);
            ItemStack inserted = remaining.copy();
            inserted.setCount(moved);

            inventory.set(slot, inserted);
            remaining.shrink(moved);
        }

        return remaining.isEmpty();
    }

    private boolean sameItemAndTag(
            ItemStack first,
            ItemStack second
    ) {
        return first != null
                && second != null
                && !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private NonNullList<ItemStack> copyMainInventory(EntityPlayer player) {
        NonNullList<ItemStack> copy = NonNullList.withSize(
                player.inventory.mainInventory.size(),
                ItemStack.EMPTY
        );

        for (int slot = 0; slot < copy.size(); slot++) {
            copy.set(
                    slot,
                    player.inventory.mainInventory.get(slot).copy()
            );
        }

        return copy;
    }

    private void restoreMainInventory(
            EntityPlayer player,
            NonNullList<ItemStack> backup
    ) {
        for (int slot = 0;
             slot < player.inventory.mainInventory.size()
                     && slot < backup.size();
             slot++) {

            player.inventory.setInventorySlotContents(
                    slot,
                    backup.get(slot).copy()
            );
        }

        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
