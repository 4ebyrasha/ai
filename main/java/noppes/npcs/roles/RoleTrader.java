package noppes.npcs.roles;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import net.minecraft.world.World;
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
	public boolean sharedLimits = true;

	private static final int MAX_CACHED_MARKETS = 128;

	private static final Map<String, CachedMarketPreset> MARKET_PRESET_CACHE =
			new LinkedHashMap<String, CachedMarketPreset>(16, 0.75F, true) {
				@Override
				protected boolean removeEldestEntry(
						Map.Entry<String, CachedMarketPreset> eldest
				) {
					return size() > MAX_CACHED_MARKETS;
				}
			};

	private long tradeConfigRevision;

	private static final class CachedMarketPreset {

		private final NBTTagCompound data;
		private final long lastModified;
		private final long fileLength;

		private CachedMarketPreset(
				NBTTagCompound data,
				File file
		) {
			this.data = data.copy();
			this.lastModified = file.lastModified();
			this.fileLength = file.length();
		}

		private boolean matches(File file) {
			return lastModified == file.lastModified()
					&& fileLength == file.length();
		}
	}

	public RoleTrader(EntityNPCInterface npc) {
		super(npc);
	}

	public long getTradeConfigRevision() {
		return tradeConfigRevision;
	}

	public void markTradeConfigChanged() {
		tradeConfigRevision++;
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
			markTradeConfigChanged();
		}
	}

	public boolean removeDynamicOffer(String id) {
		if (id == null) {
			return false;
		}

		for (int i = 0; i < dynamicOffers.size(); i++) {
			if (id.equals(dynamicOffers.get(i).getId())) {
				dynamicOffers.remove(i);
				markTradeConfigChanged();
				return true;
			}
		}

		return false;
	}

	public void clearDynamicOffers() {
		if (!dynamicOffers.isEmpty()) {
			dynamicOffers.clear();
			markTradeConfigChanged();
		}
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
		markTradeConfigChanged();
		return true;
	}

	public boolean removeBuybackOffer(String id) {
		if (id == null) {
			return false;
		}

		for (int i = 0; i < buybackOffers.size(); i++) {
			if (id.equals(buybackOffers.get(i).getId())) {
				buybackOffers.remove(i);
				markTradeConfigChanged();
				return true;
			}
		}

		return false;
	}

	public void clearBuybackOffers() {
		if (!buybackOffers.isEmpty()) {
			buybackOffers.clear();
			markTradeConfigChanged();
		}
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
		marketName = normalizeMarketName(
				tag.getString("TraderMarket")
		);

		readNBT(tag);

		if (npc.world.isRemote || marketName.isEmpty()) {
			return;
		}

		String name = marketName;
		boolean savedSharedLimits = sharedLimits;

		File file = getFile(name);

		if (file == null) {
			return;
		}

		if (file.exists()) {
			load(this, name);
		} else {
			save(this, name);
		}

		marketName = name;
		sharedLimits = savedSharedLimits;
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
		tag.setBoolean("TraderSharedLimits", sharedLimits);

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
		sharedLimits = !tag.hasKey("TraderSharedLimits", 1)
				|| tag.getBoolean("TraderSharedLimits");
		markTradeConfigChanged();
	}

	/**
	 * Загрузка данных, пришедших от редактора через RoleSave.
	 * Не перезагружает старый пресет поверх только что изменённых
	 * пользователем предложений.
	 */
	public void readFromNBTForSave(NBTTagCompound tag) {
		marketName = tag.getString("TraderMarket");
		readNBT(tag);
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

	private void readMarketPresetNBT(NBTTagCompound tag) {
		readLegacyNBT(tag);

		if (tag.hasKey("DynamicTraderOffers", 9)) {
			dynamicOffers.clear();

			NBTTagList offersTag =
					tag.getTagList("DynamicTraderOffers", 10);

			for (int i = 0; i < offersTag.tagCount(); i++) {
				NpcTradeConfig offer = NpcTradeConfig.readFromNBT(
						offersTag.getCompoundTagAt(i)
				);

				if (offer != null) {
					addDynamicOffer(offer);
				}
			}
		}

		if (tag.hasKey("TraderBuybackOffers", 9)) {
			buybackOffers.clear();

			NBTTagList buybackTag =
					tag.getTagList("TraderBuybackOffers", 10);

			for (int i = 0; i < buybackTag.tagCount(); i++) {
				NpcBuybackConfig offer = NpcBuybackConfig.readFromNBT(
						buybackTag.getCompoundTagAt(i)
				);

				if (offer != null) {
					addBuybackOffer(offer);
				}
			}
		}

		markTradeConfigChanged();
	}

	// Стандартные методы RoleTrader.
	public void interact(EntityPlayer player) {
		// Не читаем JSON при каждом открытии магазина

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
		setMarket(npc, name);
	}

	public String getMarket() {
		return marketName;
	}

	public static void refreshLinkedMarkets(
			World world,
			String marketName
	) {
		if (world == null
				|| world.isRemote
				|| marketName == null
				|| marketName.trim().isEmpty()) {
			return;
		}

		for (Entity entity : new ArrayList<>(world.loadedEntityList)) {
			if (!(entity instanceof EntityNPCInterface)) {
				continue;
			}

			EntityNPCInterface linkedNpc =
					(EntityNPCInterface) entity;

			if (!(linkedNpc.roleInterface instanceof RoleTrader)) {
				continue;
			}

			RoleTrader linkedRole =
					(RoleTrader) linkedNpc.roleInterface;

			if (linkedRole.marketName == null
					|| !linkedRole.marketName.equalsIgnoreCase(marketName)) {
				continue;
			}

			boolean savedSharedLimits = linkedRole.sharedLimits;

			load(linkedRole, marketName);

			linkedRole.sharedLimits = savedSharedLimits;
			linkedNpc.updateClient = true;
		}
	}

	public static void save(RoleTrader role, String name) {
		if (role == null) {
			return;
		}

		String normalizedName = normalizeMarketName(name);

		if (normalizedName.isEmpty()) {
			return;
		}

		File target = getFile(normalizedName);

		if (target == null) {
			return;
		}

		File directory = target.getParentFile();
		File temporary = null;

		try {
			if (!directory.exists() && !directory.mkdirs()) {
				throw new IOException(
						"Cannot create market directory: " + directory
				);
			}

			temporary = File.createTempFile(
					target.getName() + ".",
					".tmp",
					directory
			);

			NBTTagCompound marketTag =
					role.writeNBT(new NBTTagCompound());

			// Этот параметр относится к конкретному NPC,
			// а не к общему набору товаров.
			marketTag.removeTag("TraderSharedLimits");

			NBTJsonUtil.SaveFile(temporary, marketTag);

			// Перед заменой проверяем, что временный JSON можно прочитать.
			NBTTagCompound validation =
					NBTJsonUtil.LoadFile(temporary);

			if (validation == null) {
				throw new IOException(
						"Saved market preset is empty: " + normalizedName
				);
			}

			try {
				Files.move(
						temporary.toPath(),
						target.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE
				);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(
						temporary.toPath(),
						target.toPath(),
						StandardCopyOption.REPLACE_EXISTING
				);
			}

			// Заполняем кэш только после успешной замены файла.
			cacheMarketPreset(target, marketTag);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (temporary != null && temporary.exists()) {
				try {
					Files.deleteIfExists(temporary.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void load(RoleTrader role, String name) {
		if (role == null
				|| role.npc.world.isRemote
				|| name == null
				|| name.trim().isEmpty()) {
			return;
		}

		File file = getFile(name);

		if (file == null || !file.isFile()) {
			return;
		}

		try {
			NBTTagCompound marketTag = readCachedMarketPreset(file);

			if (marketTag != null) {
				role.readMarketPresetNBT(marketTag);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static File getFile(String name) {
		String normalizedName = normalizeMarketName(name);

		if (normalizedName.isEmpty()) {
			return null;
		}

		File worldDirectory = CustomNpcs.getWorldSaveDirectory();

		if (worldDirectory == null) {
			return null;
		}

		File directory = new File(worldDirectory, "markets");

		if (!directory.exists() && !directory.mkdirs()) {
			return null;
		}

		File file = new File(
				directory,
				normalizedName.toLowerCase(Locale.ROOT) + ".json"
		);

		try {
			String directoryPath =
					directory.getCanonicalPath() + File.separator;

			if (!file.getCanonicalPath().startsWith(directoryPath)) {
				return null;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return file;
	}

	private static String normalizeMarketName(String name) {
		if (name == null) {
			return "";
		}

		String input = name.trim();

		if (input.isEmpty()) {
			return "";
		}

		// Ограничение длины названия.
		if (input.length() > 64) {
			input = input.substring(0, 64);
		}

		StringBuilder safe = new StringBuilder(input.length());

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);

			if (Character.isLetterOrDigit(c)
					|| c == ' '
					|| c == '_'
					|| c == '-'
					|| c == '.') {
				safe.append(c);
			} else if (safe.length() == 0
					|| safe.charAt(safe.length() - 1) != '_') {
				safe.append('_');
			}
		}

		String result = safe.toString().trim();

		while (!result.isEmpty() && result.charAt(0) == '.') {
			result = result.substring(1).trim();
		}

		while (!result.isEmpty()
				&& result.charAt(result.length() - 1) == '.') {
			result = result.substring(0, result.length() - 1).trim();
		}

		return result;
	}

	private static NBTTagCompound readCachedMarketPreset(File file)
			throws IOException, NBTJsonUtil.JsonException {

		String key = file.getCanonicalPath();

		synchronized (MARKET_PRESET_CACHE) {
			CachedMarketPreset cached = MARKET_PRESET_CACHE.get(key);

			if (cached != null && cached.matches(file)) {
				return cached.data.copy();
			}
		}

		NBTTagCompound data = NBTJsonUtil.LoadFile(file);

		cacheMarketPreset(file, data);

		return data.copy();
	}

	private static void cacheMarketPreset(
			File file,
			NBTTagCompound data
	) {
		if (file == null || data == null) {
			return;
		}

		try {
			String key = file.getCanonicalPath();
			CachedMarketPreset cached = new CachedMarketPreset(data, file);

			synchronized (MARKET_PRESET_CACHE) {
				MARKET_PRESET_CACHE.put(key, cached);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void setMarket(
			EntityNPCInterface npc,
			String marketName
	) {
		if (npc == null
				|| !(npc.roleInterface instanceof RoleTrader)
				|| npc.world.isRemote) {
			return;
		}

		RoleTrader role = (RoleTrader) npc.roleInterface;

		String name = normalizeMarketName(marketName);

		role.marketName = name;

		if (name.isEmpty()) {
			npc.updateClient = true;
			return;
		}

		File file = getFile(name);

		if (file == null) {
			npc.updateClient = true;
			return;
		}

		if (!file.exists()) {
			save(role, name);
		}

		load(role, name);

		npc.updateClient = true;
	}

}
