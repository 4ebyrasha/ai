
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.common.ForgeHooks;
import noppes.npcs.NBTTags;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.data.INPCInventory;
import noppes.npcs.api.event.NpcEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.entity.EntityNPCInterface;

public class DataInventory implements IInventory, INPCInventory {
	public static final int DROP_SLOT_COUNT = 18;
	public static final int DROP_ITEMS_PER_SLOT = 5;
	public static final int DROP_ITEM_COUNT = DROP_SLOT_COUNT * DROP_ITEMS_PER_SLOT;

	public static final int DROP_CHANCE_SCALE = 1000;
	public static final int MAX_DROP_CHANCE = 100 * DROP_CHANCE_SCALE;

	private static final int DROP_INVENTORY_OFFSET = 7;

	// Primary drop items. Keys remain 0-17 for compatibility.
	public Map<Integer, IItemStack> drops = new HashMap<>();

	// Additional items. Keys: group * 4 + additional item index.
	public Map<Integer, IItemStack> additionalDrops = new HashMap<>();

	// Legacy chance values in whole percentages.
	public Map<Integer, Integer> dropchance = new HashMap<>();

	// Precise chance values in thousandths of a percent.
	public Map<Integer, Integer> dropChanceMilli = new HashMap<>();

	public Map<Integer, IItemStack> weapons = new HashMap<>();
	public Map<Integer, IItemStack> armor = new HashMap<>();

	private int minExp = 0;
	private int maxExp = 0;

	public int lootMode = 0;

	private final EntityNPCInterface npc;

	public DataInventory(EntityNPCInterface npc) {
		this.npc = npc;
	}

	private static Map<Integer, IItemStack> readItemStackMap(NBTTagList tagList) {
		Map<Integer, IItemStack> result = new HashMap<>();
		Map<?, ?> loaded = NBTTags.getIItemStackMap(tagList);

		for (Map.Entry<?, ?> entry : loaded.entrySet()) {
			if (entry.getKey() instanceof Integer && entry.getValue() instanceof IItemStack) {
				result.put((Integer) entry.getKey(), (IItemStack) entry.getValue());
			}
		}

		return result;
	}

	private static Map<Integer, Integer> readIntegerMap(NBTTagList tagList) {
		Map<Integer, Integer> result = new HashMap<>();
		Map<?, ?> loaded = NBTTags.getIntegerIntegerMap(tagList);

		for (Map.Entry<?, ?> entry : loaded.entrySet()) {
			if (entry.getKey() instanceof Integer && entry.getValue() instanceof Integer) {
				result.put((Integer) entry.getKey(), (Integer) entry.getValue());
			}
		}

		return result;
	}

	private static int clampChance(int chance) {
		return Math.max(0, Math.min(MAX_DROP_CHANCE, chance));
	}

	private static int toLegacyChance(int chanceMilli) {
		return Math.max(1, Math.min(100, Math.round(chanceMilli / (float) DROP_CHANCE_SCALE)));
	}

	private void validateDropSlot(int slot) {
		if (slot < 0 || slot >= DROP_SLOT_COUNT) {
			throw new CustomNPCsException("Bad slot number: " + slot);
		}
	}

	private void validateDropItemSlot(int itemSlot) {
		if (itemSlot < 0 || itemSlot >= DROP_ITEMS_PER_SLOT) {
			throw new CustomNPCsException("Bad drop item slot number: " + itemSlot);
		}
	}

	private Map<Integer, IItemStack> getDropMap(int slot, int itemSlot) {
		return itemSlot == 0 ? this.drops : this.additionalDrops;
	}

	private int getDropMapSlot(int slot, int itemSlot) {
		return itemSlot == 0 ? slot : slot * 4 + itemSlot - 1;
	}

	private void clearDropGroup(int slot) {
		this.drops.remove(slot);

		int firstAdditionalSlot = slot * 4;

		for (int i = 0; i < DROP_ITEMS_PER_SLOT - 1; ++i) {
			this.additionalDrops.remove(firstAdditionalSlot + i);
		}
	}

	private int getDropChanceMilli(int slot) {
		Integer chance = this.dropChanceMilli.get(slot);

		if (chance != null) {
			return clampChance(chance);
		}

		Integer legacyChance = this.dropchance.get(slot);

		if (legacyChance == null) {
			return MAX_DROP_CHANCE;
		}

		return Math.max(0, Math.min(100, legacyChance)) * DROP_CHANCE_SCALE;
	}

	public NBTTagCompound writeEntityToNBT(NBTTagCompound compound) {
		compound.setInteger("MinExp", this.minExp);
		compound.setInteger("MaxExp", this.maxExp);

		compound.setTag("NpcInv", NBTTags.nbtIItemStackMap(this.drops));
		compound.setTag("NpcInvExtras", NBTTags.nbtIItemStackMap(this.additionalDrops));

		compound.setTag("Armor", NBTTags.nbtIItemStackMap(this.armor));
		compound.setTag("Weapons", NBTTags.nbtIItemStackMap(this.weapons));

		compound.setTag("DropChance", NBTTags.nbtIntegerIntegerMap(this.dropchance));
		compound.setTag("DropChanceMilli", NBTTags.nbtIntegerIntegerMap(this.dropChanceMilli));

		compound.setInteger("LootMode", this.lootMode);

		return compound;
	}

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.minExp = compound.getInteger("MinExp");
		this.maxExp = compound.getInteger("MaxExp");

		this.drops = readItemStackMap(compound.getTagList("NpcInv", 10));
		this.additionalDrops = readItemStackMap(compound.getTagList("NpcInvExtras", 10));

		this.dropchance = new HashMap<>();
		this.dropChanceMilli = new HashMap<>();

		Map<Integer, Integer> loadedLegacyChances =
				readIntegerMap(compound.getTagList("DropChance", 10));

		if (compound.hasKey("DropChanceMilli", 9)) {
			Map<Integer, Integer> loadedPreciseChances =
					readIntegerMap(compound.getTagList("DropChanceMilli", 10));

			for (Map.Entry<Integer, Integer> entry : loadedPreciseChances.entrySet()) {
				int slot = entry.getKey();

				if (slot >= 0 && slot < DROP_SLOT_COUNT) {
					int chance = clampChance(entry.getValue());
					this.dropChanceMilli.put(slot, chance);
					this.dropchance.put(slot, toLegacyChance(chance));
				}
			}
		} else {
			for (Map.Entry<Integer, Integer> entry : loadedLegacyChances.entrySet()) {
				int slot = entry.getKey();

				if (slot >= 0 && slot < DROP_SLOT_COUNT) {
					int legacyChance = Math.max(0, Math.min(100, entry.getValue()));
					this.dropchance.put(slot, legacyChance);
					this.dropChanceMilli.put(slot, legacyChance * DROP_CHANCE_SCALE);
				}
			}
		}

		this.readEquipmentFromNBT(compound);
		this.lootMode = compound.getInteger("LootMode");
	}

	public void readEquipmentFromNBT(NBTTagCompound compound) {
		this.armor = readItemStackMap(compound.getTagList("Armor", 10));
		this.weapons = readItemStackMap(compound.getTagList("Weapons", 10));
	}

	private Map<Integer, IItemStack> getInventoryMap(int inventorySlot) {
		if (inventorySlot < 4) {
			return this.armor;
		}

		if (inventorySlot < 7) {
			return this.weapons;
		}

		int dropItemSlot = (inventorySlot - DROP_INVENTORY_OFFSET) % DROP_ITEMS_PER_SLOT;
		return dropItemSlot == 0 ? this.drops : this.additionalDrops;
	}

	private int getInventoryMapSlot(int inventorySlot) {
		if (inventorySlot < 4) {
			return inventorySlot;
		}

		if (inventorySlot < 7) {
			return inventorySlot - 4;
		}

		int dropIndex = inventorySlot - DROP_INVENTORY_OFFSET;
		int group = dropIndex / DROP_ITEMS_PER_SLOT;
		int itemSlot = dropIndex % DROP_ITEMS_PER_SLOT;

		return itemSlot == 0 ? group : group * 4 + itemSlot - 1;
	}

	private IItemStack wrapItemStack(ItemStack stack) {
		NpcAPI api = NpcAPI.Instance();

		if (api == null) {
			throw new IllegalStateException("CustomNPCs API is not initialized");
		}

		return api.getIItemStack(stack);
	}

	public IItemStack getArmor(int slot) {
		return this.armor.get(slot);
	}

	public void setArmor(int slot, IItemStack item) {
		this.armor.put(slot, item);
		this.npc.updateClient = true;
	}

	public IItemStack getRightHand() {
		return this.weapons.get(0);
	}

	public void setRightHand(IItemStack item) {
		this.weapons.put(0, item);
		this.npc.updateClient = true;
	}

	public IItemStack getProjectile() {
		return this.weapons.get(1);
	}

	public void setProjectile(IItemStack item) {
		this.weapons.put(1, item);
		this.npc.updateAI = true;
	}

	public IItemStack getLeftHand() {
		return this.weapons.get(2);
	}

	public void setLeftHand(IItemStack item) {
		this.weapons.put(2, item);
		this.npc.updateClient = true;
	}

	public IItemStack getDropItem(int slot) {
		this.validateDropSlot(slot);
		return this.drops.get(slot);
	}

	public IItemStack getDropItem(int slot, int itemSlot) {
		this.validateDropSlot(slot);
		this.validateDropItemSlot(itemSlot);

		return this.getDropMap(slot, itemSlot).get(this.getDropMapSlot(slot, itemSlot));
	}

	public void setDropItem(int slot, IItemStack item, int chance) {
		this.validateDropSlot(slot);

		if (item == null || item.isEmpty()) {
			this.clearDropGroup(slot);
			this.dropchance.remove(slot);
			this.dropChanceMilli.remove(slot);
			return;
		}

		this.drops.put(slot, item);
		this.setDropChance(slot, chance);
	}

	public void setDropItem(int slot, int itemSlot, IItemStack item) {
		this.validateDropSlot(slot);
		this.validateDropItemSlot(itemSlot);

		Map<Integer, IItemStack> itemMap = this.getDropMap(slot, itemSlot);
		int mapSlot = this.getDropMapSlot(slot, itemSlot);

		if (item == null || item.isEmpty()) {
			itemMap.remove(mapSlot);
		} else {
			itemMap.put(mapSlot, item);
		}
	}

	public double getDropChance(int slot) {
		this.validateDropSlot(slot);
		return this.getDropChanceMilli(slot) / (double) DROP_CHANCE_SCALE;
	}

	public void setDropChance(int slot, double chance) {
		this.validateDropSlot(slot);

		if (Double.isNaN(chance) || Double.isInfinite(chance)) {
			throw new CustomNPCsException("Invalid drop chance: " + chance);
		}

		double clampedChance = Math.max(0.0D, Math.min(100.0D, chance));
		int chanceMilli = (int) Math.round(clampedChance * DROP_CHANCE_SCALE);

		this.dropChanceMilli.put(slot, chanceMilli);
		this.dropchance.put(slot, toLegacyChance(chanceMilli));
	}

	public IItemStack[] getItemsRNG() {
		List<IItemStack> items = new ArrayList<>();

		for (int slot = 0; slot < DROP_SLOT_COUNT; ++slot) {
			boolean hasItems = false;

			for (int itemSlot = 0; itemSlot < DROP_ITEMS_PER_SLOT; ++itemSlot) {
				IItemStack item = this.getDropItem(slot, itemSlot);

				if (item != null && !item.isEmpty()) {
					hasItems = true;
					break;
				}
			}

			if (!hasItems) {
				continue;
			}

			int chance = this.getDropChanceMilli(slot);

			if (chance <= 0) {
				continue;
			}

			if (chance < MAX_DROP_CHANCE && this.npc.world.rand.nextInt(MAX_DROP_CHANCE) >= chance) {
				continue;
			}

			for (int itemSlot = 0; itemSlot < DROP_ITEMS_PER_SLOT; ++itemSlot) {
				IItemStack item = this.getDropItem(slot, itemSlot);

				if (item != null && !item.isEmpty()) {
					items.add(item);
				}
			}
		}

		return items.toArray(new IItemStack[0]);
	}

	public void dropStuff(NpcEvent.DiedEvent event, Entity entity, DamageSource damageSource) {
		ArrayList<EntityItem> items = new ArrayList<>();

		if (event.droppedItems != null) {
			for (IItemStack item : event.droppedItems) {
				if (item == null || item.isEmpty()) {
					continue;
				}

				ItemStack stack = item.getMCItemStack();

				if (stack == null || stack.isEmpty()) {
					continue;
				}

				EntityItem entityItem = this.getEntityItem(stack.copy());

				if (entityItem != null) {
					items.add(entityItem);
				}
			}
		}

		int enchant = 0;

		if (damageSource.getTrueSource() instanceof EntityPlayer) {
			enchant = EnchantmentHelper.getLootingModifier((EntityLivingBase) damageSource.getTrueSource());
		}

		if (!ForgeHooks.onLivingDrops(this.npc, damageSource, items, enchant, true)) {
			for (EntityItem item : items) {
				if (this.lootMode == 1 && entity instanceof EntityPlayer) {
					EntityPlayer player = (EntityPlayer) entity;

					item.setPickupDelay(2);
					this.npc.world.spawnEntity(item);

					ItemStack stack = item.getItem();
					int count = stack.getCount();

					if (player.inventory.addItemStackToInventory(stack)) {
						entity.world.playSound(null, player.posX, player.posY, player.posZ,
								SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2F,
								((player.getRNG().nextFloat() - player.getRNG().nextFloat()) * 0.7F + 1.0F) * 2.0F);

						player.onItemPickup(item, count);

						if (stack.getCount() <= 0) {
							item.setDead();
						}
					}
				} else {
					this.npc.world.spawnEntity(item);
				}
			}
		}

		int exp = event.expDropped;

		while (exp > 0) {
			int split = EntityXPOrb.getXPSplit(exp);
			exp -= split;

			if (this.lootMode == 1 && entity instanceof EntityPlayer) {
				this.npc.world.spawnEntity(
						new EntityXPOrb(entity.world, entity.posX, entity.posY, entity.posZ, split));
			} else {
				this.npc.world.spawnEntity(
						new EntityXPOrb(this.npc.world, this.npc.posX, this.npc.posY, this.npc.posZ, split));
			}
		}
	}

	public EntityItem getEntityItem(ItemStack itemstack) {
		if (itemstack == null || itemstack.isEmpty()) {
			return null;
		}

		EntityItem entityitem = new EntityItem(this.npc.world, this.npc.posX,
				this.npc.posY - 0.30000001192092896D + (double) this.npc.getEyeHeight(), this.npc.posZ, itemstack);

		entityitem.setPickupDelay(40);

		float f2 = this.npc.getRNG().nextFloat() * 0.5F;
		float f4 = this.npc.getRNG().nextFloat() * 3.141593F * 2.0F;

		entityitem.motionX = -MathHelper.sin(f4) * f2;
		entityitem.motionZ = MathHelper.cos(f4) * f2;
		entityitem.motionY = 0.20000000298023224D;

		return entityitem;
	}

	public int getSizeInventory() {
		return 7 + DROP_ITEM_COUNT;
	}

	@Nonnull
	public ItemStack getStackInSlot(int slot) {
		if (slot < 0 || slot >= this.getSizeInventory()) {
			return ItemStack.EMPTY;
		}

		return ItemStackWrapper.MCItem(
				this.getInventoryMap(slot).get(this.getInventoryMapSlot(slot)));
	}

	@Nonnull
	public ItemStack decrStackSize(int slot, int count) {
		if (slot < 0 || slot >= this.getSizeInventory() || count <= 0) {
			return ItemStack.EMPTY;
		}

		Map<Integer, IItemStack> inventory = this.getInventoryMap(slot);
		int mapSlot = this.getInventoryMapSlot(slot);

		IItemStack item = inventory.get(mapSlot);

		if (item == null) {
			return ItemStack.EMPTY;
		}

		ItemStack stack = item.getMCItemStack();

		if (stack == null || stack.isEmpty()) {
			inventory.remove(mapSlot);
			return ItemStack.EMPTY;
		}

		if (stack.getCount() <= count) {
			inventory.remove(mapSlot);
			return stack;
		}

		ItemStack result = stack.splitStack(count);

		if (stack.isEmpty()) {
			inventory.remove(mapSlot);
		}

		return result;
	}

	@Nonnull
	public ItemStack removeStackFromSlot(int slot) {
		if (slot < 0 || slot >= this.getSizeInventory()) {
			return ItemStack.EMPTY;
		}

		IItemStack item = this.getInventoryMap(slot).remove(this.getInventoryMapSlot(slot));
		return ItemStackWrapper.MCItem(item);
	}

	public void setInventorySlotContents(int slot, @Nonnull ItemStack stack) {
		if (slot < 0 || slot >= this.getSizeInventory()) {
			return;
		}

		Map<Integer, IItemStack> inventory = this.getInventoryMap(slot);
		int mapSlot = this.getInventoryMapSlot(slot);

		if (stack == null || stack.isEmpty()) {
			inventory.remove(mapSlot);
		} else {
			inventory.put(mapSlot, this.wrapItemStack(stack));
		}
	}

	public int getInventoryStackLimit() {
		return 64;
	}

	public boolean isUsableByPlayer(@Nonnull EntityPlayer player) {
		return true;
	}

	public boolean isItemValidForSlot(int slot, @Nonnull ItemStack itemstack) {
		return true;
	}

	@Nonnull
	public String getName() {
		return "NPC Inventory";
	}

	public void markDirty() {
	}

	public boolean hasCustomName() {
		return true;
	}

	public ITextComponent getDisplayName() {
		return null;
	}

	public void openInventory(@Nonnull EntityPlayer player) {
	}

	public void closeInventory(@Nonnull EntityPlayer player) {
	}

	public int getField(int id) {
		return 0;
	}

	public void setField(int id, int value) {
	}

	public int getFieldCount() {
		return 0;
	}

	public void clear() {
	}

	public int getExpMin() {
		return this.npc.inventory.minExp;
	}

	public int getExpMax() {
		return this.npc.inventory.maxExp;
	}

	public int getExpRNG() {
		int exp = this.minExp;

		if (this.maxExp - this.minExp > 0) {
			exp += this.npc.world.rand.nextInt(this.maxExp - this.minExp);
		}

		return exp;
	}

	public void setExp(int min, int max) {
		min = Math.min(min, max);
		this.npc.inventory.minExp = min;
		this.npc.inventory.maxExp = max;
	}

	public boolean isEmpty() {
		for (int slot = 0; slot < this.getSizeInventory(); ++slot) {
			ItemStack item = this.getStackInSlot(slot);

			if (!NoppesUtilServer.IsItemStackNull(item) && !item.isEmpty()) {
				return false;
			}
		}

		return true;
	}
}
