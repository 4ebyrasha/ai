
package noppes.npcs.containers;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;

public class ContainerNPCInv extends Container {

	public ContainerNPCInv(EntityNPCInterface npc, EntityPlayer player) {
		this.addSlotToContainer(new SlotNPCArmor(npc.inventory, 0, 9, 22, EntityEquipmentSlot.HEAD));
		this.addSlotToContainer(new SlotNPCArmor(npc.inventory, 1, 9, 40, EntityEquipmentSlot.CHEST));
		this.addSlotToContainer(new SlotNPCArmor(npc.inventory, 2, 9, 58, EntityEquipmentSlot.LEGS));
		this.addSlotToContainer(new SlotNPCArmor(npc.inventory, 3, 9, 76, EntityEquipmentSlot.FEET));

		this.addSlotToContainer(new Slot(npc.inventory, 4, 81, 22));
		this.addSlotToContainer(new Slot(npc.inventory, 5, 81, 40));
		this.addSlotToContainer(new Slot(npc.inventory, 6, 81, 58));

		int rowsPerColumn = DataInventory.DROP_SLOT_COUNT / 2;

		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			int column = group / rowsPerColumn;
			int row = group % rowsPerColumn;
			int baseX = column == 0 ? 191 : 331;
			int y = 16 + row * 21;

			for (int itemSlot = 0; itemSlot < DataInventory.DROP_ITEMS_PER_SLOT; ++itemSlot) {
				int inventorySlot = 7 + group * DataInventory.DROP_ITEMS_PER_SLOT + itemSlot;
				int x = baseX + itemSlot * 18;

				this.addSlotToContainer(new Slot(npc.inventory, inventorySlot, x, y));
			}
		}

		for (int j = 0; j < 3; ++j) {
			for (int i = 0; i < 9; ++i) {
				this.addSlotToContainer(new Slot(player.inventory, i + j * 9 + 9, i * 18 + 8, 113 + j * 18));
			}
		}

		for (int i = 0; i < 9; ++i) {
			this.addSlotToContainer(new Slot(player.inventory, i, i * 18 + 8, 171));
		}
	}

	@Nonnull
	public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
		return ItemStack.EMPTY;
	}

	public boolean canInteractWith(@Nonnull EntityPlayer player) {
		return true;
	}
}
