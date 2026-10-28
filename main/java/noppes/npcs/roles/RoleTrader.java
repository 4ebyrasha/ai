package noppes.npcs.roles;

import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.CustomNpcs;
import noppes.npcs.NoppesUtilPlayer;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.role.IRoleTrader;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.util.NBTJsonUtil;

import com.cheba.trader.NpcTradeConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.NBTTagList;

public class RoleTrader extends RoleInterface implements IRoleTrader {
	public String marketName = "";
	public NpcMiscInventory inventoryCurrency = new NpcMiscInventory(36);
	public NpcMiscInventory inventorySold = new NpcMiscInventory(18);
	private final List<NpcTradeConfig> dynamicOffers = new ArrayList<>();
	public boolean ignoreDamage = false;
	public boolean ignoreNBT = false;
	public boolean toSave = false;

	public RoleTrader(EntityNPCInterface npc) {
		super(npc);
	}

	public List<NpcTradeConfig> getDynamicOffers() {
		return Collections.unmodifiableList(dynamicOffers);
	}

	public NpcTradeConfig getDynamicOffer(String id) {
		if (id == null) {
			return null;
		}

		for (NpcTradeConfig offer : dynamicOffers) {
			if (id.equals(offer.getId())) {
				return offer;
			}
		}

		return null;
	}

	public void addDynamicOffer(NpcTradeConfig offer) {
		if (offer == null) {
			return;
		}

		// Не добавляем второй экземпляр предложения с тем же ID.
		if (getDynamicOffer(offer.getId()) == null) {
			dynamicOffers.add(offer);
		}
	}

	public boolean removeDynamicOffer(String id) {
		if (id == null) {
			return false;
		}

		for (int i = 0; i < dynamicOffers.size(); i++) {
			if (id.equals(dynamicOffers.get(i).getId())) {
				dynamicOffers.remove(i);
				return true;
			}
		}

		return false;
	}

	public void clearDynamicOffers() {
		dynamicOffers.clear();
	}

	public NBTTagCompound writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setString("TraderMarket", this.marketName);
		this.writeNBT(nbttagcompound);
		if (this.toSave && !this.npc.isRemote()) {
			save(this, this.marketName);
		}

		this.toSave = false;
		return nbttagcompound;
	}

	public void readFromNBT(NBTTagCompound nbttagcompound) {
		this.marketName = nbttagcompound.getString("TraderMarket");
		this.readNBT(nbttagcompound);
	}

	public NBTTagCompound writeNBT(NBTTagCompound tag) {
		writeLegacyNBT(tag);

		NBTTagList offersTag = new NBTTagList();

		for (NpcTradeConfig offer : dynamicOffers) {
			if (offer != null) {
				offersTag.appendTag(offer.writeToNBT());
			}
		}

		tag.setTag("DynamicTraderOffers", offersTag);
		return tag;
	}

	public void readNBT(NBTTagCompound tag) {
		readLegacyNBT(tag);

		// Старые JSON-файлы не должны очищать новые NBT-предложения.
		if (tag.hasKey("DynamicTraderOffers", 9)) {
			dynamicOffers.clear();

			NBTTagList offersTag =
					tag.getTagList("DynamicTraderOffers", 10);

			for (int i = 0; i < offersTag.tagCount(); i++) {
				NpcTradeConfig offer =
						NpcTradeConfig.readFromNBT(
								offersTag.getCompoundTagAt(i)
						);

				if (offer != null) {
					addDynamicOffer(offer);
				}
			}
		}
	}

	private NBTTagCompound writeLegacyNBT(NBTTagCompound tag) {
		tag.setTag("TraderCurrency", inventoryCurrency.getToNBT());
		tag.setTag("TraderSold", inventorySold.getToNBT());
		tag.setBoolean("TraderIgnoreDamage", ignoreDamage);
		tag.setBoolean("TraderIgnoreNBT", ignoreNBT);

		return tag;
	}

	private void readLegacyNBT(NBTTagCompound tag) {
		inventoryCurrency.setFromNBT(
				tag.getCompoundTag("TraderCurrency")
		);

		inventorySold.setFromNBT(
				tag.getCompoundTag("TraderSold")
		);

		ignoreDamage = tag.getBoolean("TraderIgnoreDamage");
		ignoreNBT = tag.getBoolean("TraderIgnoreNBT");
	}

	public void interact(EntityPlayer player) {
		this.npc.say(player, this.npc.advanced.getInteractLine());

		NoppesUtilServer.sendOpenGui(
				player,
				EnumGuiType.DynamicTraderMenu,
				this.npc
		);
	}

	public boolean hasCurrency(ItemStack itemstack) {
		if (itemstack == null) {
			return false;
		} else {
			Iterator var2 = this.inventoryCurrency.items.iterator();

			ItemStack item;
			do {
				if (!var2.hasNext()) {
					return false;
				}

				item = (ItemStack) var2.next();
			} while (item.isEmpty()
					|| !NoppesUtilPlayer.compareItems(item, itemstack, this.ignoreDamage, this.ignoreNBT));

			return true;
		}
	}

	public IItemStack getSold(int slot) {
		return NpcAPI.Instance().getIItemStack(this.inventorySold.getStackInSlot(slot));
	}

	public IItemStack getCurrency1(int slot) {
		return NpcAPI.Instance().getIItemStack(this.inventoryCurrency.getStackInSlot(slot));
	}

	public IItemStack getCurrency2(int slot) {
		return NpcAPI.Instance().getIItemStack(this.inventoryCurrency.getStackInSlot(slot + 18));
	}

	public void set(int slot, IItemStack currency, IItemStack currency2, IItemStack sold) {
		if (sold == null) {
			throw new CustomNPCsException("Sold item was null", new Object[0]);
		} else if (slot < 18 && slot >= 0) {
			if (currency == null) {
				currency = currency2;
				currency2 = null;
			}

			if (currency != null) {
				this.inventoryCurrency.items.set(slot, currency.getMCItemStack());
			} else {
				this.inventoryCurrency.items.set(slot, ItemStack.EMPTY);
			}

			if (currency2 != null) {
				this.inventoryCurrency.items.set(slot + 18, currency2.getMCItemStack());
			} else {
				this.inventoryCurrency.items.set(slot + 18, ItemStack.EMPTY);
			}

			this.inventorySold.items.set(slot, sold.getMCItemStack());
		} else {
			throw new CustomNPCsException("Invalid slot: " + slot, new Object[0]);
		}
	}

	public void remove(int slot) {
		if (slot < 18 && slot >= 0) {
			this.inventoryCurrency.items.set(slot, ItemStack.EMPTY);
			this.inventoryCurrency.items.set(slot + 18, ItemStack.EMPTY);
			this.inventorySold.items.set(slot, ItemStack.EMPTY);
		} else {
			throw new CustomNPCsException("Invalid slot: " + slot, new Object[0]);
		}
	}

	public void setMarket(String name) {
		this.marketName = name;
		load(this, name);
	}

	public String getMarket() {
		return this.marketName;
	}

	public static void save(RoleTrader r, String name) {
		if (!name.isEmpty()) {
			File file = getFile(name + "_new");
			File file1 = getFile(name);

			try {
				NBTJsonUtil.SaveFile(file, r.writeLegacyNBT(new NBTTagCompound()));
				if (file1.exists()) {
					file1.delete();
				}

				file.renameTo(file1);
			} catch (Exception var5) {
			}

		}
	}

	public static void load(RoleTrader role, String name) {
		if (!role.npc.world.isRemote) {
			File file = getFile(name);
			if (file.exists()) {
				try {
					role.readLegacyNBT(NBTJsonUtil.LoadFile(file));
				} catch (Exception var4) {
				}

			}
		}
	}

	private static File getFile(String name) {
		File dir = new File(CustomNpcs.getWorldSaveDirectory(), "markets");
		if (!dir.exists()) {
			dir.mkdir();
		}

		return new File(dir, name.toLowerCase() + ".json");
	}

	public static void setMarket(EntityNPCInterface npc, String marketName) {
		if (!marketName.isEmpty()) {
			if (!getFile(marketName).exists()) {
				save((RoleTrader) npc.roleInterface, marketName);
			}

			load((RoleTrader) npc.roleInterface, marketName);
		}
	}
}
