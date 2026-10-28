package com.cheba.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

public class TraderSellContainer extends Container {

    private final EntityNPCInterface npc;
    private final InventoryBasic escrow =
            new InventoryBasic("Trader sell buffer", false, 25);

    private boolean returned;

    public TraderSellContainer(EntityPlayer player, EntityNPCInterface npc) {
        this.npc = npc;

        // Буфер продажи: 5 рядов по 5 слотов.
        for (int i = 0; i < 25; i++) {
            int x = 23 + (i % 5) * 18;
            int y = 10 + (i / 5) * 18;

            this.addSlotToContainer(new Slot(escrow, i, x, y));
        }

        // Инвентарь игрока: 27 слотов.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                this.addSlotToContainer(
                        new Slot(player.inventory, index,
                                13 + col * 18, 122 + row * 18)
                );
            }
        }

        // Хотбар игрока.
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(
                    new Slot(player.inventory, col,
                            13 + col * 18, 180)
            );
        }
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        if (player == null || player.isDead || npc == null || npc.isDead) {
            return false;
        }

        if (player.world != npc.world) {
            return false;
        }

        return player.getDistanceSq(npc) <= 64.0D;
    }

    @Override
    public void onContainerClosed(@Nonnull EntityPlayer player) {
        super.onContainerClosed(player);

        // Клиент не должен самостоятельно возвращать предметы.
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
            escrow.setInventorySlotContents(i, ItemStack.EMPTY);

            player.inventory.addItemStackToInventory(remaining);

            if (!remaining.isEmpty()) {
                player.dropItem(remaining, false);
            }
        }
    }

    public IInventory getEscrowInventory() {
        return escrow;
    }

    public Entity getNpc() {
        return npc;
    }
}
