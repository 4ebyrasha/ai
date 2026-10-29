package noppes.npcs.roles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

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

import com.cheba.trader.NpcBuybackConfig;
import com.cheba.trader.NpcTradeConfig;

public class RoleTrader extends RoleInterface implements IRoleTrader {

	public String marketName = "";

	public NpcMiscInventory inventoryCurrency = new NpcMiscInventory(36);
	public NpcMiscInventory inventorySold = new NpcMiscInventory(18);

	private final List<NpcTradeConfig> dynamicOffers = new ArrayList<>();
	private final List<NpcBuybackConfig> buybackOffers = new ArrayList<>();

	public boolean ignoreDamage = false;
	public boolean ignoreNBT = false;
	public boolean toSave = false;

	public RoleTrader(EntityNPCInterface npc) {
		super(npc);
	}


	// Основная витрина: предметы, которые игрок покупает у NPC


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


	// Отдельный список: предметы, которые NPC покупает у игрока


	public List<NpcBuybackConfig> getBuybackOffers() {
		return Collections.unmodifiableList(buybackOffers);
	}

	public NpcBuybackConfig getBuybackOffer(String id) {
		if (id == null) {
			return null;
		}

		for (NpcBuybackConfig offer : buybackOffers) {
			if (id.equals(offer.getId())) {
				return offer;
			}
		}

		return null;
	}

	/**
	 * Добавляет предмет в список выкупа.
	 * Дубликаты одинаковых предметов не добавляются
	 */
	public boolean addBuybackOffer(NpcBuybackConfig offer) {
		if (offer == null || offer.getItem().isEmpty()) {
			return false;
		}

		for (NpcBuybackConfig existing : buybackOffers) {
			if (existing.matches(offer.getItem())) {
				return false;
			}
		}

		buybackOffers.add(offer);
		return true;
	}

	public boolean removeBuybackOffer(String id) {
		if (id == null) {
			return false;
		}

		for (int i = 0; i < buybackOffers.size(); i++) {
			if (id.equals(buybackOffers.get(i).getId())) {
				buybackOffers.remove(i);
				return true;
			}
		}

		return false;
	}

	public void clearBuybackOffers() {
		buybackOffers.clear();
	}


	// Сохранение и загрузка.


	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setString("TraderMarket", marketName);

		writeNBT(tag);

		if (toSave && !npc.isRemote()) {
			save(this, marketName);
		}

		toSave = false;
		return tag;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		marketName = tag.getString("TraderMarket");
		readNBT(tag);
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

		NBTTagList buybackTag = new NBTTagList();

		for (NpcBuybackConfig offer : buybackOffers) {
			if (offer != null) {
				buybackTag.appendTag(offer.writeToNBT());
			}
		}

		tag.setTag("TraderBuybackOffers", buybackTag);

		return tag;
	}

	public void readNBT(NBTTagCompound tag) {
		readLegacyNBT(tag);

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

		buybackOffers.clear();

		if (tag.hasKey("TraderBuybackOffers", 9)) {
			NBTTagList buybackTag =
					tag.getTagList("TraderBuybackOffers", 10);

			for (int i = 0; i < buybackTag.tagCount(); i++) {
				NpcBuybackConfig offer =
						NpcBuybackConfig.readFromNBT(
								buybackTag.getCompoundTagAt(i)
						);

				if (offer != null) {
					addBuybackOffer(offer);
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

	// Стандартные методы RoleTrader.
	public void interact(EntityPlayer player) {
		npc.say(player, npc.advanced.getInteractLine());

		NoppesUtilServer.sendOpenGui(
				player,
				EnumGuiType.DynamicTraderMenu,
				npc
		);
	}

	public boolean hasCurrency(ItemStack itemstack) {
		if (itemstack == null) {
			return false;
		}

		Iterator<ItemStack> iterator = inventoryCurrency.items.iterator();

		while (iterator.hasNext()) {
			ItemStack item = iterator.next();

			if (!item.isEmpty()
					&& NoppesUtilPlayer.compareItems(
					item,
					itemstack,
					ignoreDamage,
					ignoreNBT
			)) {
				return true;
			}
		}

		return false;
	}

	public IItemStack getSold(int slot) {
		return NpcAPI.Instance().getIItemStack(
				inventorySold.getStackInSlot(slot)
		);
	}

	public IItemStack getCurrency1(int slot) {
		return NpcAPI.Instance().getIItemStack(
				inventoryCurrency.getStackInSlot(slot)
		);
	}

	public IItemStack getCurrency2(int slot) {
		return NpcAPI.Instance().getIItemStack(
				inventoryCurrency.getStackInSlot(slot + 18)
		);
	}

	public void set(
			int slot,
			IItemStack currency,
			IItemStack currency2,
			IItemStack sold
	) {
		if (sold == null) {
			throw new CustomNPCsException(
					"Sold item was null",
					new Object[0]
			);
		}

		if (slot < 0 || slot >= 18) {
			throw new CustomNPCsException(
					"Invalid slot: " + slot,
					new Object[0]
			);
		}

		if (currency == null) {
			currency = currency2;
			currency2 = null;
		}

		inventoryCurrency.items.set(
				slot,
				currency == null
						? ItemStack.EMPTY
						: currency.getMCItemStack()
		);

		inventoryCurrency.items.set(
				slot + 18,
				currency2 == null
						? ItemStack.EMPTY
						: currency2.getMCItemStack()
		);

		inventorySold.items.set(
				slot,
				sold.getMCItemStack()
		);
	}

	public void remove(int slot) {
		if (slot < 0 || slot >= 18) {
			throw new CustomNPCsException(
					"Invalid slot: " + slot,
					new Object[0]
			);
		}

		inventoryCurrency.items.set(slot, ItemStack.EMPTY);
		inventoryCurrency.items.set(slot + 18, ItemStack.EMPTY);
		inventorySold.items.set(slot, ItemStack.EMPTY);
	}

	public void setMarket(String name) {
		marketName = name;
		load(this, name);
	}

	public String getMarket() {
		return marketName;
	}

	public static void save(RoleTrader role, String name) {
		if (name == null || name.isEmpty()) {
			return;
		}

		File file = getFile(name + "_new");
		File oldFile = getFile(name);

		try {
			NBTJsonUtil.SaveFile(
					file,
					role.writeLegacyNBT(new NBTTagCompound())
			);

			if (oldFile.exists()) {
				oldFile.delete();
			}

			file.renameTo(oldFile);
		} catch (Exception ignored) {
		}
	}

	public static void load(RoleTrader role, String name) {
		if (name == null
				|| name.isEmpty()
				|| role.npc.world.isRemote) {
			return;
		}

		File file = getFile(name);

		if (file.exists()) {
			try {
				role.readLegacyNBT(NBTJsonUtil.LoadFile(file));
			} catch (Exception ignored) {
			}
		}
	}

	private static File getFile(String name) {
		File dir = new File(
				CustomNpcs.getWorldSaveDirectory(),
				"markets"
		);

		if (!dir.exists()) {
			dir.mkdir();
		}

		return new File(dir, name.toLowerCase() + ".json");
	}

	public static void setMarket(
			EntityNPCInterface npc,
			String marketName
	) {
		if (marketName == null || marketName.isEmpty()) {
			return;
		}

		if (!getFile(marketName).exists()) {
			save((RoleTrader) npc.roleInterface, marketName);
		}

		load((RoleTrader) npc.roleInterface, marketName);
	}
}
