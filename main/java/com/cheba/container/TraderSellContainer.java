package com.cheba.container;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
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

            addSlotToContainer(new Slot(escrow, i, x, y));
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

    /**
     * Перенос предметов по Shift.
     */
    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull
            EntityPlayer player,
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
            // Из инвентаря игрока в буфер продажи.
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

    /**
     * Возвращает содержимое буфера при закрытии окна.
     */
    @Override
    public void onContainerClosed(@Nonnull EntityPlayer player) {
        super.onContainerClosed(player);

        if (player.world.isRemote || returned) {
            return;
        }

        returned = true;

        for (int i = 0; i < escrow.getSizeInventory(); i++) {
            ItemStack stack = escrow.getStackInSlot(i);

            if (stack.isEmpty()) {
                continue;
            }

            ItemStack remaining = stack.copy();

            escrow.setInventorySlotContents(
                    i,
                    ItemStack.EMPTY
            );

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
}
