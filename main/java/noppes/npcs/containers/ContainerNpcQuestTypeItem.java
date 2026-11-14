package noppes.npcs.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.quests.QuestItem;

import javax.annotation.Nonnull;

public class ContainerNpcQuestTypeItem extends Container {

	public ContainerNpcQuestTypeItem(EntityPlayer player) {
		Quest quest = NoppesUtilServer.getEditingQuest(player);
		QuestItem questItem = (QuestItem) quest.questInterface;

		for (int row = 0; row < 3; ++row) {
			for (int column = 0; column < 3; ++column) {
				int slot = column + row * 3;

				this.addSlotToContainer(new Slot(
						questItem.items,
						slot,
						105 + column * 18,
						17 + row * 18
				));
			}
		}

		for (int row = 0; row < 3; ++row) {
			for (int column = 0; column < 9; ++column) {
				this.addSlotToContainer(new Slot(
						player.inventory,
						column + row * 9 + 9,
						8 + column * 18,
						84 + row * 18
				));
			}
		}

		for (int column = 0; column < 9; ++column) {
			this.addSlotToContainer(new Slot(
					player.inventory,
					column,
					8 + column * 18,
					142
			));
		}
	}

	public ItemStack transferStackInSlot(@Nonnull EntityPlayer player, int index) {
		return null;
	}

	public boolean canInteractWith(@Nonnull EntityPlayer player) {
		return true;
	}
}
