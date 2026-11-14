package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;

public class QuestLocation extends QuestInterface {

	public String location = "";
	public String location2 = "";
	public String location3 = "";
	public String location4 = "";
	public String location5 = "";
	public String location6 = "";

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.location = compound.getString("QuestLocation");
		this.location2 = compound.getString("QuestLocation2");
		this.location3 = compound.getString("QuestLocation3");
		this.location4 = compound.getString("QuestLocation4");
		this.location5 = compound.getString("QuestLocation5");
		this.location6 = compound.getString("QuestLocation6");
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setString("QuestLocation", this.location);
		compound.setString("QuestLocation2", this.location2);
		compound.setString("QuestLocation3", this.location3);
		compound.setString("QuestLocation4", this.location4);
		compound.setString("QuestLocation5", this.location5);
		compound.setString("QuestLocation6", this.location6);
	}

	public boolean isCompleted(EntityPlayer player) {
		PlayerQuestData playerData = PlayerData.get(player).questData;
		QuestData data = (QuestData) playerData.activeQuests.get(this.questId);

		return data != null && this.getFound(data, 0);
	}

	public void handleComplete(EntityPlayer player) {
	}

	private String getLocation(int slot) {
		switch (slot) {
			case 1:
				return this.location;
			case 2:
				return this.location2;
			case 3:
				return this.location3;
			case 4:
				return this.location4;
			case 5:
				return this.location5;
			case 6:
				return this.location6;
			default:
				return "";
		}
	}

	private String getFoundKey(int slot) {
		return slot == 1 ? "LocationFound" : "Location" + slot + "Found";
	}

	public boolean getFound(QuestData data, int slot) {
		if (slot >= 1 && slot <= 6) {
			String key = this.getFoundKey(slot);

			if (data.extraData.getBoolean(key)) {
				return true;
			}


			if (slot == 2 && data.extraData.getBoolean("LocationFound2")) {
				return true;
			}

			if (slot == 3 && data.extraData.getBoolean("LocationFound3")) {
				return true;
			}

			return false;
		}

		for (int i = 1; i <= 6; ++i) {
			if (!this.getLocation(i).isEmpty() && !this.getFound(data, i)) {
				return false;
			}
		}

		return true;
	}

	public boolean setFound(QuestData data, String location) {
		for (int i = 1; i <= 6; ++i) {
			String target = this.getLocation(i);

			if (!target.isEmpty()
					&& target.equalsIgnoreCase(location)
					&& !this.getFound(data, i)) {

				data.extraData.setBoolean(this.getFoundKey(i), true);
				return true;
			}
		}

		return false;
	}

	public IQuestObjective[] getObjectives(EntityPlayer player) {
		List<IQuestObjective> objectives = new ArrayList<>();

		for (int i = 1; i <= 6; ++i) {
			String target = this.getLocation(i);

			if (!target.isEmpty()) {
				objectives.add(new QuestLocationObjective(
						player,
						target,
						this.getFoundKey(i)
				));
			}
		}

		return objectives.toArray(new IQuestObjective[0]);
	}

	private class QuestLocationObjective implements IQuestObjective {

		private final EntityPlayer player;
		private final String location;
		private final String nbtName;

		public QuestLocationObjective(
				EntityPlayer player,
				String location,
				String nbtName) {

			this.player = player;
			this.location = location;
			this.nbtName = nbtName;
		}

		public int getProgress() {
			return this.isCompleted() ? 1 : 0;
		}

		public void setProgress(int progress) {
			if (progress < 0 || progress > 1) {
				throw new CustomNPCsException(
						"Progress has to be 0 or 1"
				);
			}

			PlayerData data = PlayerData.get(this.player);
			QuestData questData = (QuestData) data.questData.activeQuests.get(
					QuestLocation.this.questId
			);

			if (questData == null) {
				return;
			}

			boolean completed = questData.extraData.getBoolean(this.nbtName);

			if (completed != (progress == 1)) {
				questData.extraData.setBoolean(this.nbtName, progress == 1);
				data.questData.checkQuestCompletion(this.player, 3);
				data.updateClient = true;
			}
		}

		public int getMaxProgress() {
			return 1;
		}

		public boolean isCompleted() {
			PlayerData data = PlayerData.get(this.player);
			QuestData questData = (QuestData) data.questData.activeQuests.get(
					QuestLocation.this.questId
			);

			return questData != null
					&& questData.extraData.getBoolean(this.nbtName);
		}

		@SuppressWarnings("deprecation")
		public String getText() {
			String found = I18n.translateToLocal("quest.found");
			String notFound = I18n.translateToLocal("quest.notfound");

			return this.location + ": "
					+ (this.isCompleted() ? found : notFound);
		}
	}
}
