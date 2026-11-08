
package noppes.npcs.entity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import noppes.npcs.util.ValueUtil;

import javax.annotation.Nonnull;

public class DataInventory implements IInventory, INPCInventory {
	public static final int DROP_SLOT_COUNT = 18;

	public Map<Integer, IItemStack> drops = new HashMap<>();
	public Map<Integer, Integer> dropchance = new HashMap<>();
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

	public NBTTagCompound writeEntityToNBT(NBTTagCompound compound) {
		compound.setInteger("MinExp", this.minExp);
		compound.setInteger("MaxExp", this.maxExp);
		compound.setTag("NpcInv", NBTTags.nbtIItemStackMap(this.drops));
		compound.setTag("Armor", NBTTags.nbtIItemStackMap(this.armor));
		compound.setTag("Weapons", NBTTags.nbtIItemStackMap(this.weapons));
		compound.setTag("DropChance", NBTTags.nbtIntegerIntegerMap(this.dropchance));
		compound.setInteger("LootMode", this.lootMode);
		return compound;
	}

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.minExp = compound.getInteger("MinExp");
		this.maxExp = compound.getInteger("MaxExp");

		this.drops = readItemStackMap(compound.getTagList("NpcInv", 10));
		this.dropchance = readIntegerMap(compound.getTagList("DropChance", 10));

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

		return this.drops;
	}

	private int getInventoryMapSlot(int inventorySlot) {
		if (inventorySlot < 4) {
			return inventorySlot;
		}

		if (inventorySlot < 7) {
			return inventorySlot - 4;
		}

		return inventorySlot - 7;
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
		if (slot >= 0 && slot < DROP_SLOT_COUNT) {
			return this.drops.get(slot);
		}

		throw new CustomNPCsException("Bad slot number: " + slot);
	}

	public void setDropItem(int slot, IItemStack item, int chance) {
		if (slot < 0 || slot >= DROP_SLOT_COUNT) {
			throw new CustomNPCsException("Bad slot number: " + slot);
		}

		chance = ValueUtil.CorrectInt(chance, 1, 100);

		if (item != null && !item.isEmpty()) {
			this.dropchance.put(slot, chance);
			this.drops.put(slot, item);
		} else {
			this.dropchance.remove(slot);
			this.drops.remove(slot);
		}
	}

	public IItemStack[] getItemsRNG() {
		List<IItemStack> items = new ArrayList<>();

		for (Map.Entry<Integer, IItemStack> entry : this.drops.entrySet()) {
			Integer slot = entry.getKey();
			IItemStack item = entry.getValue();

			if (slot == null || item == null || item.isEmpty()) {
				continue;
			}

			Integer configuredChance = this.dropchance.get(slot);
			int dropChance = configuredChance == null ? 100 : configuredChance;

			int chance = this.npc.world.rand.nextInt(100) + dropChance;

			if (chance >= 100) {
				items.add(item);
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
		return 7 + DROP_SLOT_COUNT;
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

	public void setInventorySlotContents(int slot,@Nonnull ItemStack stack) {
		if (slot < 0 || slot >= this.getSizeInventory()) {
			return;
		}

		this.getInventoryMap(slot).put(
				this.getInventoryMapSlot(slot),
				this.wrapItemStack(stack));
	}

	public int getInventoryStackLimit() {
		return 64;
	}

	public boolean isUsableByPlayer(@Nonnull EntityPlayer player) {
		return true;
	}

	public boolean isItemValidForSlot(int slot,@Nonnull ItemStack itemstack) {
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
