
package com.cheba.trader;

import com.cheba.currency.TraderCurrencyBridge;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import noppes.npcs.roles.RoleTrader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TraderTradeService {

    private final TraderCurrencyBridge currency;

    public TraderTradeService(TraderCurrencyBridge currency) {
        this.currency = currency;
    }

    public enum Result {
        SUCCESS,
        SERVER_ONLY,
        TOO_FAR,
        INVALID_OFFER,
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
        private final Item item;
        private final int count;

        private Removal(int slot, Item item, int count) {
            this.slot = slot;
            this.item = item;
            this.count = count;
        }
    }

    /**
     * Покупает предложение по его ID.
     * Вызывать только на серверной стороне.
     */
    public Result buy(
            EntityPlayer player,
            RoleTrader role,
            String offerId
    ) {
        if (player == null || role == null || offerId == null) {
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

        TraderRuntimeState state = TraderRuntimeState.get(world);

        if (state == null) {
            return Result.SERVER_ONLY;
        }

        /*
         * Запас и лимит привязаны к ID конкретного предложения.
         * Одинаковые товары в разных предложениях независимы.
         */
        String marketKey = getMarketKey(role);

        String stockKey = marketKey
                + "|offer:"
                + offer.getId();

        String playerKey = stockKey
                + "|"
                + player.getUniqueID();

        int dailyStock = offer.getDailyStock();
        int maxPerDay = offer.getMaxPerDay();

        long quantity = product.getCount();

        if (dailyStock >= 0
                && state.getSoldCount(stockKey) + quantity > dailyStock) {
            return Result.OUT_OF_STOCK;
        }

        if (maxPerDay >= 0
                && state.getPlayerPurchaseCount(playerKey) + quantity
                > maxPerDay) {
            return Result.DAILY_LIMIT_REACHED;
        }

        /*
         * Создаём план списания предметов оплаты.
         */
        List<Removal> removals = buildRemovalPlan(
                player,
                offer.getPriceItems()
        );

        if (removals == null) {
            return Result.NOT_ENOUGH_ITEMS;
        }

        long moneyPrice = offer.getPriceMoney();

        if (moneyPrice > 0L) {
            if (currency == null || !currency.isAvailable()) {
                return Result.CURRENCY_UNAVAILABLE;
            }

            if (currency.getBalance(player) < moneyPrice) {
                return Result.NOT_ENOUGH_MONEY;
            }
        }

        /*
         * Проверяем план списания и вместимость до оплаты.
         * Основной инвентарь проверяется с учётом освобождения
         * слотов предметами, которые игрок отдаёт в оплату.
         */
        if (isRemovalPlanInvalid(player, removals)) {
            return Result.INVENTORY_CHANGED;
        }

        if (!canFitProduct(player, removals, product)) {
            return Result.INVENTORY_FULL;
        }

        /*
         * Резервная копия основного инвентаря на случай
         * неожиданной ошибки при выдаче.
         */
        NonNullList<ItemStack> inventoryBackup =
                copyMainInventory(player);

        boolean moneyWithdrawn = false;

        if (moneyPrice > 0L) {
            if (!currency.withdraw(player, moneyPrice)) {
                return Result.PAYMENT_FAILED;
            }

            moneyWithdrawn = true;
        }

        /*
         * Повторно проверяем инвентарь после списания денег.
         */
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

        /*
         * Списываем предметы оплаты.
         */
        applyRemovalPlan(player, removals);

        /*
         * Выдаём товар только в основной инвентарь.
         * Если выдача не удалась, восстанавливаем инвентарь.
         */
        if (!deliverProduct(player, product)) {
            restoreMainInventory(player, inventoryBackup);

            if (moneyWithdrawn
                    && !currency.deposit(player, moneyPrice)) {
                currency.notifyBalanceChange(player, -moneyPrice);
                return Result.REFUND_FAILED;
            }

            return Result.INVENTORY_CHANGED;
        }

        /*
         * Лимиты изменяются только после успешной выдачи товара.
         */
        state.recordPurchase(stockKey, playerKey, quantity);

        /*
         * Уведомляем HUD об успешном списании денег.
         */
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

    /**
     * Создаёт план списания предметов.
     * Сравнивает Item, как и исходная реализация.
     */
    private List<Removal> buildRemovalPlan(
            EntityPlayer player,
            List<ItemStack> requirements
    ) {
        Map<Item, Long> requiredItems = new LinkedHashMap<>();

        for (ItemStack required : requirements) {
            if (required == null || required.isEmpty()) {
                continue;
            }

            Item item = required.getItem();

            long current = requiredItems.getOrDefault(item, 0L);

            requiredItems.put(
                    item,
                    current + required.getCount()
            );
        }

        List<Removal> plan = new ArrayList<>();

        NonNullList<ItemStack> inventory =
                player.inventory.mainInventory;

        for (Map.Entry<Item, Long> entry : requiredItems.entrySet()) {
            Item requiredItem = entry.getKey();
            long remaining = entry.getValue();

            long available = 0L;

            for (ItemStack stack : inventory) {
                if (!stack.isEmpty()
                        && stack.getItem() == requiredItem) {
                    available += stack.getCount();
                }
            }

            if (available < remaining) {
                return null;
            }

            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.get(slot);

                if (stack.isEmpty()
                        || stack.getItem() != requiredItem) {
                    continue;
                }

                int take = (int) Math.min(
                        remaining,
                        stack.getCount()
                );

                if (take > 0) {
                    plan.add(new Removal(
                            slot,
                            requiredItem,
                            take
                    ));

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

            ItemStack stack = player.inventory.getStackInSlot(
                    removal.slot
            );

            if (stack.isEmpty()
                    || stack.getItem() != removal.item
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
            ItemStack stack = player.inventory.getStackInSlot(
                    removal.slot
            );

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

    /**
     * Проверяет возможность разместить товар после списания цены.
     * Реальный инвентарь при проверке не изменяется.
     */
    private boolean canFitProduct(
            EntityPlayer player,
            List<Removal> removals,
            ItemStack product
    ) {
        NonNullList<ItemStack> simulated =
                copyMainInventory(player);

        for (Removal removal : removals) {
            if (removal.slot < 0
                    || removal.slot >= simulated.size()) {
                return false;
            }

            ItemStack stack = simulated.get(removal.slot);

            if (stack.isEmpty()
                    || stack.getItem() != removal.item
                    || stack.getCount() < removal.count) {
                return false;
            }

            if (stack.getCount() == removal.count) {
                simulated.set(removal.slot, ItemStack.EMPTY);
            } else {
                stack.shrink(removal.count);
            }
        }

        return insertProductIntoInventory(
                simulated,
                product
        );
    }

    /**
     * Выдаёт товар в основной инвентарь.
     * Не использует курсор и не выбрасывает предметы.
     */
    private boolean deliverProduct(
            EntityPlayer player,
            ItemStack product
    ) {
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

    /**
     * Сначала объединяет совместимые стаки, затем использует
     * пустые слоты. Изменяет переданный список инвентаря.
     */
    private boolean insertProductIntoInventory(
            NonNullList<ItemStack> inventory,
            ItemStack product
    ) {
        if (product == null || product.isEmpty()) {
            return true;
        }

        ItemStack remaining = product.copy();

        /*
         * Сначала пополняем существующие стаки.
         */
        for (int slot = 0;
             slot < inventory.size() && !remaining.isEmpty();
             slot++) {

            ItemStack stack = inventory.get(slot);

            if (stack.isEmpty() || !canStack(stack, remaining)) {
                continue;
            }

            int maxStackSize = Math.min(
                    stack.getMaxStackSize(),
                    remaining.getMaxStackSize()
            );

            int space = Math.max(
                    0,
                    maxStackSize - stack.getCount()
            );

            int moved = Math.min(
                    space,
                    remaining.getCount()
            );

            if (moved > 0) {
                stack.grow(moved);
                remaining.shrink(moved);
            }
        }

        /*
         * Затем размещаем остаток в пустых слотах.
         */
        for (int slot = 0;
             slot < inventory.size() && !remaining.isEmpty();
             slot++) {

            if (!inventory.get(slot).isEmpty()) {
                continue;
            }

            int maxStackSize = remaining.getMaxStackSize();

            if (maxStackSize <= 0) {
                return false;
            }

            int moved = Math.min(
                    remaining.getCount(),
                    maxStackSize
            );

            ItemStack inserted = remaining.copy();
            inserted.setCount(moved);

            inventory.set(slot, inserted);
            remaining.shrink(moved);
        }

        return remaining.isEmpty();
    }

    private boolean canStack(
            ItemStack first,
            ItemStack second
    ) {
        return !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
    }

    private NonNullList<ItemStack> copyMainInventory(
            EntityPlayer player
    ) {
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
