package noppes.npcs.controllers.data;

import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import noppes.npcs.NBTTags;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.controllers.BankController;

public class BankData {
	public HashMap itemSlots = new HashMap();
	public HashMap upgradedSlots = new HashMap();
	public int unlockedSlots = 0;
	public int bankId = -1;

	public BankData() {
		for (int i = 0; i < 6; ++i) {
			this.itemSlots.put(i, new NpcMiscInventory(54));
			this.upgradedSlots.put(i, false);
		}
	}

	public void readNBT(NBTTagCompound nbttagcompound) {
		this.bankId = nbttagcompound.getInteger("DataBankId");
		this.unlockedSlots = nbttagcompound.getInteger("UnlockedSlots");
		this.itemSlots = this.getItemSlots(nbttagcompound.getTagList("BankInv", 10));
		this.upgradedSlots = NBTTags.getBooleanList(nbttagcompound.getTagList("UpdatedSlots", 10));
	}

	private HashMap getItemSlots(NBTTagList tagList) {
		HashMap list = new HashMap();

		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound nbttagcompound = tagList.getCompoundTagAt(i);
			int slot = nbttagcompound.getInteger("Slot");
			NpcMiscInventory inv = new NpcMiscInventory(54);
			inv.setFromNBT(nbttagcompound.getCompoundTag("BankItems"));
			list.put(slot, inv);
		}

		return list;
	}

	public void writeNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setInteger("DataBankId", this.bankId);
		nbttagcompound.setInteger("UnlockedSlots", this.unlockedSlots);
		nbttagcompound.setTag("UpdatedSlots", NBTTags.nbtBooleanList(this.upgradedSlots));
		nbttagcompound.setTag("BankInv", this.nbtItemSlots(this.itemSlots));
	}

	private NBTTagList nbtItemSlots(HashMap items) {
		NBTTagList list = new NBTTagList();
		Iterator iterator = items.keySet().iterator();

		while (iterator.hasNext()) {
			int slot = (Integer) iterator.next();
			NBTTagCompound compound = new NBTTagCompound();
			compound.setInteger("Slot", slot);
			compound.setTag("BankItems", ((NpcMiscInventory) items.get(slot)).getToNBT());
			list.appendTag(compound);
		}

		return list;
	}

	public boolean isUpgraded(Bank bank, int slot) {
		if (bank.isUpgraded(slot)) {
			return true;
		}

		return bank.canBeUpgraded(slot) && (Boolean) this.upgradedSlots.get(slot);
	}
}
