package com.cheba.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import javax.annotation.Nonnull;

public class TraderMenuContainer extends Container {

    public TraderMenuContainer(EntityPlayer player) {
    }

    @Override
    public boolean canInteractWith(@Nonnull EntityPlayer player) {
        return player != null && !player.isDead;
    }
}
