package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import noppes.npcs.entity.EntityNPCInterface;

/** Empty container: shop contents are managed independently of NPCs. */
public class ContainerNPCTraderSetup extends Container {
    public ContainerNPCTraderSetup(EntityNPCInterface npc, EntityPlayer player) {
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return player != null && !player.isDead;
    }
}
