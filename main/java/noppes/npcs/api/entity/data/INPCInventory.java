
package noppes.npcs.api.entity.data;

import noppes.npcs.api.item.IItemStack;

public interface INPCInventory {

	public IItemStack getRightHand();

	public void setRightHand(IItemStack item);

	public IItemStack getLeftHand();

	public void setLeftHand(IItemStack item);

	public IItemStack getProjectile();

	public void setProjectile(IItemStack item);

	/**
	 * @param slot The armor slot to return. 0:head, 1:body, 2:legs, 3:boots
	 * @return Returns the armor item
	 */
	public IItemStack getArmor(int slot);

	/**
	 * @param slot The armor slot to return. 0:head, 1:body, 2:legs, 3:boots
	 * @param item
	 */
	public void setArmor(int slot, IItemStack item);

	/**
	 * Gets the primary item in a drop group.
	 *
	 * @param slot Drop group index, 0-17
	 */
	public IItemStack getDropItem(int slot);

	/**
	 * Gets an item from a drop group.
	 *
	 * @param slot Drop group index, 0-17
	 * @param itemSlot Item position in the group, 0-4
	 */
	public IItemStack getDropItem(int slot, int itemSlot);

	/**
	 * Sets the primary item and the group's chance.
	 *
	 * @param slot Drop group index, 0-17
	 * @param item
	 * @param chance Chance in percent, 0-100
	 */
	public void setDropItem(int slot, IItemStack item, int chance);

	/**
	 * Sets or clears an item in a drop group without changing its chance.
	 *
	 * @param slot Drop group index, 0-17
	 * @param itemSlot Item position in the group, 0-4
	 * @param item
	 */
	public void setDropItem(int slot, int itemSlot, IItemStack item);

	/**
	 * Gets the chance of a drop group in percent.
	 *
	 * @param slot Drop group index, 0-17
	 * @return Chance between 0 and 100, with precision of 0.001
	 */
	public double getDropChance(int slot);

	/**
	 * Sets the chance of a drop group in percent.
	 *
	 * @param slot Drop group index, 0-17
	 * @param chance Chance between 0 and 100, with precision of 0.001
	 */
	public void setDropChance(int slot, double chance);

	public int getExpMin();

	public int getExpMax();

	/**
	 * @return Returns a value between expMin and expMax
	 */
	public int getExpRNG();

	/**
	 * Sets the random exp dropped when the npc dies
	 */
	public void setExp(int min, int max);

	public IItemStack[] getItemsRNG();
}
