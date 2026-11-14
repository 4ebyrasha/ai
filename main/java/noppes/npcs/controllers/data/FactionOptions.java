package noppes.npcs.controllers.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import noppes.npcs.controllers.FactionController;

public class FactionOptions {
	public static final int MAX_FACTIONS = 9;

	// legacy fields
	// keep them for compatibility with existing code and scripts
	public boolean decreaseFactionPoints = false;
	public boolean decreaseFaction2Points = false;
	public int factionId = -1;
	public int faction2Id = -1;
	public int factionPoints = 100;
	public int faction2Points = 100;

	private final int[] additionalFactionIds = new int[MAX_FACTIONS - 2];
	private final int[] additionalFactionPoints = new int[MAX_FACTIONS - 2];
	private final boolean[] additionalDecreasePoints = new boolean[MAX_FACTIONS - 2];

	public FactionOptions() {
		this.resetAdditionalFactions();
	}

	private void resetAdditionalFactions() {
		for (int i = 0; i < this.additionalFactionIds.length; ++i) {
			this.additionalFactionIds[i] = -1;
			this.additionalFactionPoints[i] = 100;
			this.additionalDecreasePoints[i] = false;
		}
	}

	public int getFactionId(int slot) {
		if (slot == 0) {
			return this.factionId;
		}

		if (slot == 1) {
			return this.faction2Id;
		}

		if (slot >= 2 && slot < MAX_FACTIONS) {
			return this.additionalFactionIds[slot - 2];
		}

		return -1;
	}

	public int getFactionPoints(int slot) {
		if (slot == 0) {
			return this.factionPoints;
		}

		if (slot == 1) {
			return this.faction2Points;
		}

		if (slot >= 2 && slot < MAX_FACTIONS) {
			return this.additionalFactionPoints[slot - 2];
		}

		return 0;
	}

	public boolean isFactionDecrease(int slot) {
		if (slot == 0) {
			return this.decreaseFactionPoints;
		}

		if (slot == 1) {
			return this.decreaseFaction2Points;
		}

		if (slot >= 2 && slot < MAX_FACTIONS) {
			return this.additionalDecreasePoints[slot - 2];
		}

		return false;
	}

	public void setFaction(int slot, int factionId, boolean decrease, int points) {
		if (slot == 0) {
			this.factionId = factionId;
			this.decreaseFactionPoints = decrease;
			this.factionPoints = points;
		} else if (slot == 1) {
			this.faction2Id = factionId;
			this.decreaseFaction2Points = decrease;
			this.faction2Points = points;
		} else if (slot >= 2 && slot < MAX_FACTIONS) {
			int index = slot - 2;
			this.additionalFactionIds[index] = factionId;
			this.additionalDecreasePoints[index] = decrease;
			this.additionalFactionPoints[index] = points;
		}
	}

	public int getFirstEmptySlot() {
		for (int i = 0; i < MAX_FACTIONS; ++i) {
			if (this.getFactionId(i) < 0) {
				return i;
			}
		}

		return -1;
	}

	public boolean hasFaction(int id) {
		for (int i = 0; i < MAX_FACTIONS; ++i) {
			if (this.getFactionId(i) == id) {
				return true;
			}
		}

		return false;
	}

	public boolean hasFactionExcept(int id, int ignoredSlot) {
		for (int i = 0; i < MAX_FACTIONS; ++i) {
			if (i != ignoredSlot && this.getFactionId(i) == id) {
				return true;
			}
		}

		return false;
	}

	public void readFromNBT(NBTTagCompound compound) {
		this.factionId = -1;
		this.faction2Id = -1;
		this.factionPoints = 100;
		this.faction2Points = 100;
		this.decreaseFactionPoints = false;
		this.decreaseFaction2Points = false;
		this.resetAdditionalFactions();

		if (compound.hasKey("FactionAssignments", 9)) {
			NBTTagList assignments = compound.getTagList("FactionAssignments", 10);

			if (assignments.tagCount() > 0) {
				for (int i = 0; i < assignments.tagCount(); ++i) {
					NBTTagCompound assignment = assignments.getCompoundTagAt(i);

					int slot = assignment.getInteger("Slot");
					if (slot < 0 || slot >= MAX_FACTIONS
							|| !assignment.hasKey("FactionId", 99)) {
						continue;
					}

					int id = assignment.getInteger("FactionId");
					int points = assignment.hasKey("Points", 99)
							? assignment.getInteger("Points") : 100;
					boolean decrease = assignment.getBoolean("Decrease");

					this.setFaction(slot, id, decrease, points);
				}

				return;
			}
		}

		// Read the original two faction slots from older quest/dialog data.
		this.factionId = compound.hasKey("OptionFactions1", 99)
				? compound.getInteger("OptionFactions1") : -1;
		this.faction2Id = compound.hasKey("OptionFactions2", 99)
				? compound.getInteger("OptionFactions2") : -1;

		this.factionPoints = compound.hasKey("OptionFaction1Points", 99)
				? compound.getInteger("OptionFaction1Points") : 100;
		this.faction2Points = compound.hasKey("OptionFaction2Points", 99)
				? compound.getInteger("OptionFaction2Points") : 100;

		this.decreaseFactionPoints = compound.getBoolean("DecreaseFaction1Points");
		this.decreaseFaction2Points = compound.getBoolean("DecreaseFaction2Points");
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		// Continue writing the legacy keys for older readers.
		compound.setInteger("OptionFactions1", this.factionId);
		compound.setInteger("OptionFactions2", this.faction2Id);
		compound.setInteger("OptionFaction1Points", this.factionPoints);
		compound.setInteger("OptionFaction2Points", this.faction2Points);
		compound.setBoolean("DecreaseFaction1Points", this.decreaseFactionPoints);
		compound.setBoolean("DecreaseFaction2Points", this.decreaseFaction2Points);

		NBTTagList assignments = new NBTTagList();

		for (int i = 0; i < MAX_FACTIONS; ++i) {
			NBTTagCompound assignment = new NBTTagCompound();
			assignment.setInteger("Slot", i);
			assignment.setInteger("FactionId", this.getFactionId(i));
			assignment.setInteger("Points", this.getFactionPoints(i));
			assignment.setBoolean("Decrease", this.isFactionDecrease(i));
			assignments.appendTag(assignment);
		}

		compound.setTag("FactionAssignments", assignments);
		return compound;
	}

	public void addPoints(EntityPlayer player) {
		boolean hasAssignments = false;

		for (int i = 0; i < MAX_FACTIONS; ++i) {
			int id = this.getFactionId(i);

			if (id < 0) {
				continue;
			}

			hasAssignments = true;

			int points = this.getFactionPoints(i);
			if (points > 0) {
				this.addPoints(
						player,
						PlayerData.get(player).factionData,
						id,
						this.isFactionDecrease(i),
						points
				);
			}
		}

		if (hasAssignments) {
			PlayerData.get(player).updateClient = true;
		}
	}

	private void addPoints(
			EntityPlayer player,
			PlayerFactionData data,
			int factionId,
			boolean decrease,
			int points
	) {
		Faction faction = FactionController.instance.getFaction(factionId);

		if (faction != null) {
			if (!faction.hideFaction) {
				String message = decrease
						? "faction.decreasepoints"
						: "faction.increasepoints";

				player.sendMessage(new TextComponentTranslation(
						message,
						new Object[] { faction.name, points }
				));
			}

			data.increasePoints(
					player,
					factionId,
					decrease ? -points : points
			);
		}
	}
}
