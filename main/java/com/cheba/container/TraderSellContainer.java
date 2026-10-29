package com.cheba.container;

import com.cheba.trader.TraderItemProtection;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

public class TraderSellContainer extends Container {

    private static final int ESCROW_SIZE = 25;

    private final EntityNPCInterface npc;

    private final InventoryBasic escrow =
            new InventoryBasic("Trader sell buffer", false, ESCROW_SIZE);

    private boolean returned;

    public TraderSellContainer(
            EntityPlayer player,
            EntityNPCInterface npc
    ) {
        this.npc = npc;

        // 25 слотов буфера продажи.
        for (int i = 0; i < ESCROW_SIZE; i++) {
            int x = 23 + (i % 5) * 18;
            int y = 10 + (i / 5) * 18;

            addSlotToContainer(new EscrowSlot(escrow, i, x, y, player));
        }

        // Основной инвентарь игрока.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int inventoryIndex = col + row * 9 + 9;

                addSlotToContainer(
                        new Slot(
                                player.inventory,
                                inventoryIndex,
                                13 + col * 18,
                                122 + row * 18
                        )
                );
            }
        }

        // Панель быстрого доступа.
        for (int col = 0; col < 9; col++) {
            addSlotToContainer(
                    new Slot(
                            player.inventory,
                            col,
                            13 + col * 18,
                            180
                    )
            );
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        if (player.isDead || npc == null || npc.isDead) {
            return false;
        }

        if (player.world != npc.world) {
            return false;
        }

        return player.getDistanceSq(npc) <= 64.0D;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(
            @Nonnull EntityPlayer player,
            int index
    ) {
        if (index < 0 || index >= inventorySlots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = inventorySlots.get(index);

        if (slot == null || !slot.getHasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();

        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = stack.copy();

        if (index < ESCROW_SIZE) {
            // Из буфера продажи в инвентарь игрока.
            if (!mergeItemStack(
                    stack,
                    ESCROW_SIZE,
                    inventorySlots.size(),
                    true
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            // В буфер через Shift разрешены только обычные предметы.
            if (TraderItemProtection.isProtected(player, stack)) {
                player.sendMessage(new TextComponentTranslation(
                        "cheba.trader.message.protected_item"
                ));
                return ItemStack.EMPTY;
            }

            if (!mergeItemStack(
                    stack,
                    0,
                    ESCROW_SIZE,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.putStack(ItemStack.EMPTY);
        } else {
            slot.onSlotChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stack);

        return original;
    }

    @Override
    public void onContainerClosed(@Nonnull EntityPlayer player) {
        /*
         * Сначала сохраняем личный предмет, если он остался на курсоре.
         * Базовый Container при закрытии выбрасывает предмет с курсора,
         * поэтому нельзя передавать ему непустой защищенный предмет
         */
        ItemStack cursor = player.inventory.getItemStack();

        if (TraderItemProtection.isProtected(player, cursor)) {
            ItemStack remaining = cursor.copy();

            player.inventory.setItemStack(ItemStack.EMPTY);
            player.inventory.addItemStackToInventory(remaining);

            /*
             * Если инвентарь заполнен, сохраняем остаток на курсоре.
             * Намеренно не вызываем super с защищенным предметом
             */
            if (!remaining.isEmpty()) {
                player.inventory.setItemStack(remaining);

                if (!player.world.isRemote) {
                    player.sendMessage(new TextComponentTranslation(
                            "cheba.trader.message.protected_item_return_failed"
                    ));
                }
            }
        }

        /*
         * Вызываем базовый метод только тогда, когда курсор пуст
         * или на нем нет защищенного предмета
         */
        if (!TraderItemProtection.isProtected(
                player,
                player.inventory.getItemStack()
        )) {
            super.onContainerClosed(player);
        }

        if (player.world.isRemote || returned) {
            return;
        }

        returned = true;

        /*
         * Возвращаем все, что осталось в буфере продажи
         */
        for (int i = 0; i < escrow.getSizeInventory(); i++) {
            ItemStack stack = escrow.getStackInSlot(i);

            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remaining = stack.copy();

            escrow.setInventorySlotContents(i, ItemStack.EMPTY);

            player.inventory.addItemStackToInventory(remaining);

            if (!remaining.isEmpty()) {
                player.dropItem(remaining, false);
            }
        }
    }

    public void clearEscrowAfterSale() {
        for (int i = 0; i < escrow.getSizeInventory(); i++) {
            escrow.setInventorySlotContents(i, ItemStack.EMPTY);
        }

        detectAndSendChanges();
    }

    public IInventory getEscrowInventory() {
        return escrow;
    }

    public Entity getNpc() {
        return npc;
    }

    private static class EscrowSlot extends Slot {

        private final EntityPlayer player;

        public EscrowSlot(
                IInventory inventory,
                int index,
                int x,
                int y,
                EntityPlayer player
        ) {
            super(inventory, index, x, y);
            this.player = player;
        }

        @Override
        public boolean isItemValid(@Nonnull ItemStack stack) {
            if (TraderItemProtection.isProtected(player, stack)) {
                return false;
            }

            return super.isItemValid(stack);
        }
    }

    @Nonnull
    @Override
    public ItemStack slotClick(
            int slotId,
            int dragType, @Nonnull
            ClickType clickTypeIn,
            EntityPlayer player
    ) {
        ItemStack cursor = player.inventory.getItemStack();

        /*
         * Если GP уже находится на курсоре, разрешаем только
         * вернуть его в свободный слот инвентаря игрока.
         * Выбросить его или положить в буфер продажи нельзя
         */
        if (TraderItemProtection.isProtected(player, cursor)) {
            if (clickTypeIn == ClickType.PICKUP
                    && dragType == 0
                    && slotId >= ESCROW_SIZE
                    && slotId < inventorySlots.size()) {

                Slot target = inventorySlots.get(slotId);

                if (target != null) {
                    if (!target.getHasStack()) {
                        return super.slotClick(
                                slotId,
                                dragType,
                                clickTypeIn,
                                player
                        );
                    }

                    ItemStack targetStack = target.getStack();

                    boolean sameStack =
                            ItemStack.areItemsEqual(cursor, targetStack)
                                    && ItemStack.areItemStackTagsEqual(
                                    cursor,
                                    targetStack
                            );

                    int limit = Math.min(
                            target.getSlotStackLimit(),
                            cursor.getMaxStackSize()
                    );

                    if (sameStack && targetStack.getCount() < limit) {
                        return super.slotClick(
                                slotId,
                                dragType,
                                clickTypeIn,
                                player
                        );
                    }
                }
            }

            return ItemStack.EMPTY;
        }

        /*
         * Нельзя брать GP-предмет из любого слота этого контейнера:
         * обычным кликом, Shift-кликом, выбрасыванием или клонированием.
         */
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot clicked = inventorySlots.get(slotId);

            if (clicked != null
                    && clicked.getHasStack()
                    && TraderItemProtection.isProtected(
                    player,
                    clicked.getStack()
            )) {
                return ItemStack.EMPTY;
            }
        }

        /*
         * Запрещаем перемещение GP-предмета клавишей выбора
         * слота быстрого доступа (1–9).
         */
        if (clickTypeIn == ClickType.SWAP
                && dragType >= 0
                && dragType < 9
                && TraderItemProtection.isProtected(
                player,
                player.inventory.getStackInSlot(dragType)
        )) {
            return ItemStack.EMPTY;
        }

        return super.slotClick(
                slotId,
                dragType,
                clickTypeIn,
                player
        );
    }
}
