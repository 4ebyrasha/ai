package com.cheba.trader;

import com.cheba.currency.TraderCurrencyBridge;
import net.minecraft.entity.item.EntityItem;
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

import net.minecraft.nbt.NBTTagCompound;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
     * Вызывать на серверной стороне.
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

        String marketKey = getMarketKey(role);
        String stockKey = marketKey + "|" + getStockProductKey(product);

        String playerKey = stockKey
                + "|" + player.getUniqueID();

        int dailyStock = offer.getDailyStock();
        int maxPerDay = offer.getMaxPerDay();

        long quantity = product.getCount();

        if (dailyStock >= 0
                && state.getSoldCount(stockKey) + quantity > dailyStock) {
            return Result.OUT_OF_STOCK;
        }

        if (maxPerDay >= 0
                && state.getPlayerPurchaseCount(playerKey) + quantity > maxPerDay) {
            return Result.DAILY_LIMIT_REACHED;
        }

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
         * Повторная проверка непосредственно перед списанием.
         */
        if (isRemovalPlanInvalid(player, removals)) {
            return Result.INVENTORY_CHANGED;
        }

        boolean moneyWithdrawn = false;

        if (moneyPrice > 0L) {
            if (!currency.withdraw(player, moneyPrice)) {
                return Result.PAYMENT_FAILED;
            }

            moneyWithdrawn = true;
        }

        /*
         * Если инвентарь изменился, пытаемся вернуть деньги.
         * На обычном серверном потоке ситуация маловероятна,
         * так как план уже проверен.
         */
        if (isRemovalPlanInvalid(player, removals)) {
            if (moneyWithdrawn
                    && !currency.deposit(player, moneyPrice)) {
                return Result.REFUND_FAILED;
            }

            return Result.INVENTORY_CHANGED;
        }

        applyRemovalPlan(player, removals);

        /*
         * Запас и дневные лимиты обновляются только после оплаты
         * и фактического списания предметов
         */
        state.recordPurchase(stockKey, playerKey, quantity);

        deliverProduct(player, product);

        return Result.SUCCESS;
    }

    private String getStockProductKey(ItemStack product) {
        ItemStack normalized = product.copy();
        normalized.setCount(1);

        String productData = normalized
                .writeToNBT(new NBTTagCompound())
                .toString();

        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(productData.getBytes(StandardCharsets.UTF_8));

            StringBuilder key = new StringBuilder(hash.length * 2);

            for (byte value : hash) {
                int unsignedValue = value & 0xFF;

                if (unsignedValue < 16) {
                    key.append('0');
                }

                key.append(Integer.toHexString(unsignedValue));
            }

            return "product:" + key;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    e
            );
        }
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
     * Возвращает null, если предметов недостаточно.
     * Метаданные и NBT не учитываются: сравнивается Item.
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
                    plan.add(new Removal(slot, requiredItem, take));
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
     * Сначала пытаемся положить покупку в курсор.
     * Остаток помещаем в инвентарь, а при нехватке места
     * выбрасываем рядом с игроком.
     */
    private void deliverProduct(
            EntityPlayer player,
            ItemStack product
    ) {
        ItemStack remaining = product.copy();
        ItemStack cursor = player.inventory.getItemStack();

        if (cursor.isEmpty()) {
            int toCursor = Math.min(
                    remaining.getCount(),
                    remaining.getMaxStackSize()
            );

            ItemStack cursorStack = remaining.copy();
            cursorStack.setCount(toCursor);

            player.inventory.setItemStack(cursorStack);
            remaining.shrink(toCursor);

        } else if (ItemStack.areItemsEqual(cursor, remaining)
                && ItemStack.areItemStackTagsEqual(cursor, remaining)) {

            int space = Math.max(
                    0,
                    cursor.getMaxStackSize() - cursor.getCount()
            );

            int toMerge = Math.min(space, remaining.getCount());

            if (toMerge > 0) {
                cursor.grow(toMerge);
                remaining.shrink(toMerge);
            }
        }

        if (!remaining.isEmpty()) {
            player.inventory.addItemStackToInventory(remaining);

            if (!remaining.isEmpty()) {
                EntityItem dropped = player.dropItem(remaining, false);

                if (dropped == null) {
                    player.world.spawnEntity(
                            new EntityItem(
                                    player.world,
                                    player.posX,
                                    player.posY + 0.5D,
                                    player.posZ,
                                    remaining.copy()
                            )
                    );
                }
            }
        }

        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
    }
}
