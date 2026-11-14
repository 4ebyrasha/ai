
package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.util.ValueUtil;

public class QuestItem extends QuestInterface {

	public NpcMiscInventory items = new NpcMiscInventory(9);
	public boolean leaveItems = false;
	public boolean ignoreDamage = false;
	public boolean ignoreNBT = false;

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.items.setFromNBT(compound.getCompoundTag("Items"));
		this.leaveItems = compound.getBoolean("LeaveItems");
		this.ignoreDamage = compound.getBoolean("IgnoreDamage");
		this.ignoreNBT = compound.getBoolean("IgnoreNBT");
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setTag("Items", this.items.getToNBT());
		compound.setBoolean("LeaveItems", this.leaveItems);
		compound.setBoolean("IgnoreDamage", this.ignoreDamage);
		compound.setBoolean("IgnoreNBT", this.ignoreNBT);
	}

	public boolean isCompleted(EntityPlayer player) {
		List<ItemStack> questItems = NoppesUtilPlayer.countStacks(
				this.items, this.ignoreDamage, this.ignoreNBT);

		for (ItemStack requiredItem : questItems) {
			if (!NoppesUtilPlayer.compareItems(
					player,
					requiredItem,
					this.ignoreDamage,
					this.ignoreNBT)) {
				return false;
			}
		}

		return true;
	}

	public Map<ItemStack, Integer> getProgressSet(EntityPlayer player) {
		Map<ItemStack, Integer> map = new HashMap<>();

		List<ItemStack> questItems = NoppesUtilPlayer.countStacks(
				this.items, this.ignoreDamage, this.ignoreNBT);

		for (ItemStack questItem : questItems) {
			if (!NoppesUtilServer.IsItemStackNull(questItem)) {
				map.put(questItem, 0);
			}
		}

		for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
			ItemStack item = player.inventory.getStackInSlot(i);

			if (NoppesUtilServer.IsItemStackNull(item)) {
				continue;
			}

			for (Map.Entry<ItemStack, Integer> questItem : map.entrySet()) {
				if (NoppesUtilPlayer.compareItems(
						questItem.getKey(),
						item,
						this.ignoreDamage,
						this.ignoreNBT)) {
					questItem.setValue(
							questItem.getValue() + item.getCount());
				}
			}
		}

		return map;
	}

	public void handleComplete(EntityPlayer player) {
		if (this.leaveItems) {
			return;
		}

		for (int questSlot = 0;
			 questSlot < this.items.getSizeInventory();
			 ++questSlot) {

			ItemStack requiredItem = this.items.getStackInSlot(questSlot);

			if (NoppesUtilServer.IsItemStackNull(requiredItem)
					|| requiredItem.isEmpty()) {
				continue;
			}

			int remaining = requiredItem.getCount();

			for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
				ItemStack item = player.inventory.getStackInSlot(i);

				if (NoppesUtilServer.IsItemStackNull(item)
						|| !NoppesUtilPlayer.compareItems(
						item,
						requiredItem,
						this.ignoreDamage,
						this.ignoreNBT)) {
					continue;
				}

				int stackSize = item.getCount();

				if (stackSize <= remaining) {
					player.inventory.setInventorySlotContents(
							i, ItemStack.EMPTY);
					remaining -= stackSize;
				} else {
					item.splitStack(remaining);
					remaining = 0;
				}

				if (remaining <= 0) {
					break;
				}
			}
		}
	}

	public IQuestObjective[] getObjectives(EntityPlayer player) {
		List<IQuestObjective> objectives = new ArrayList<>();

		List<ItemStack> questItems = NoppesUtilPlayer.countStacks(
				this.items, this.ignoreDamage, this.ignoreNBT);

		for (ItemStack stack : questItems) {
			if (!stack.isEmpty()) {
				objectives.add(new QuestItemObjective(player, stack));
			}
		}

		return objectives.toArray(new IQuestObjective[0]);
	}

	private class QuestItemObjective implements IQuestObjective {

		private final EntityPlayer player;
		private final ItemStack questItem;

		public QuestItemObjective(EntityPlayer player, ItemStack item) {
			this.player = player;
			this.questItem = item;
		}

		public int getProgress() {
			int count = 0;

			for (int i = 0;
				 i < this.player.inventory.getSizeInventory();
				 ++i) {

				ItemStack item = this.player.inventory.getStackInSlot(i);

				if (!NoppesUtilServer.IsItemStackNull(item)
						&& NoppesUtilPlayer.compareItems(
						this.questItem,
						item,
						QuestItem.this.ignoreDamage,
						QuestItem.this.ignoreNBT)) {
					count += item.getCount();
				}
			}

			return ValueUtil.CorrectInt(
					count, 0, this.questItem.getCount());
		}

		public void setProgress(int progress) {
			throw new CustomNPCsException(
					"Cant set the progress of ItemQuests");
		}

		public int getMaxProgress() {
			return this.questItem.getCount();
		}

		public boolean isCompleted() {
			return NoppesUtilPlayer.compareItems(
					this.player,
					this.questItem,
					QuestItem.this.ignoreDamage,
					QuestItem.this.ignoreNBT);
		}

		public String getText() {
			return this.questItem.getDisplayName()
					+ ": " + this.getProgress()
					+ "/" + this.getMaxProgress();
		}
	}
}
