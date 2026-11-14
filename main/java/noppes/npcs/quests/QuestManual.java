package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.QuestData;

public class QuestManual extends QuestInterface {

	public TreeMap<String, Integer> manuals = new TreeMap<>();

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.manuals = new TreeMap<>(
		readStringIntegerMap(compound.getTagList("QuestManual", 10))
		);
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setTag(
				"QuestManual",
				writeStringIntegerMap(this.manuals)
		);
	}

	public boolean isCompleted(EntityPlayer player) {
		PlayerQuestData playerData = PlayerData.get(player).questData;
		QuestData data = (QuestData) playerData.activeQuests.get(this.questId);

		if (data == null) {
			return false;
		}

		HashMap<String, Integer> manual = this.getManual(data);

		if (manual.size() != this.manuals.size()) {
			return false;
		}

		for (Map.Entry<String, Integer> entry : manual.entrySet()) {
			Integer required = this.manuals.get(entry.getKey());

			if (required == null || required > entry.getValue()) {
				return false;
			}
		}

		return true;
	}

	public void handleComplete(EntityPlayer player) {
	}

	public HashMap<String, Integer> getManual(QuestData data) {
		return readStringIntegerMap(
				data.extraData.getTagList("Manual", 10)
		);
	}

	public void setManual(
			QuestData data,
			HashMap<String, Integer> manual) {

		data.extraData.setTag(
				"Manual",
				writeStringIntegerMap(manual)
		);
	}

	public IQuestObjective[] getObjectives(EntityPlayer player) {
		List<IQuestObjective> objectives = new ArrayList<>();

		for (Map.Entry<String, Integer> entry : this.manuals.entrySet()) {
			objectives.add(new QuestManualObjective(
					player,
					entry.getKey(),
					entry.getValue()
			));
		}

		return objectives.toArray(new IQuestObjective[0]);
	}

	private static HashMap<String, Integer> readStringIntegerMap(
			NBTTagList tagList) {

		HashMap<String, Integer> map = new HashMap<>();

		for (int i = 0; i < tagList.tagCount(); ++i) {
			NBTTagCompound entry = tagList.getCompoundTagAt(i);

			map.put(
					entry.getString("Slot"),
					entry.getInteger("Value")
			);
		}

		return map;
	}

	private static NBTTagList writeStringIntegerMap(
			Map<String, Integer> map) {

		NBTTagList list = new NBTTagList();

		if (map == null) {
			return list;
		}

		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			NBTTagCompound compound = new NBTTagCompound();

			compound.setString("Slot", entry.getKey());
			compound.setInteger("Value", entry.getValue());

			list.appendTag(compound);
		}

		return list;
	}

	private class QuestManualObjective implements IQuestObjective {

		private final EntityPlayer player;
		private final String entity;
		private final int amount;

		public QuestManualObjective(
				EntityPlayer player,
				String entity,
				int amount) {

			this.player = player;
			this.entity = entity;
			this.amount = amount;
		}

		public int getProgress() {
			PlayerData data = PlayerData.get(this.player);
			PlayerQuestData playerData = data.questData;

			QuestData questData = (QuestData) playerData.activeQuests.get(
					QuestManual.this.questId
			);

			if (questData == null) {
				return 0;
			}

			Integer progress = QuestManual.this.getManual(questData).get(
					this.entity
			);

			return progress == null ? 0 : progress;
		}

		public void setProgress(int progress) {
			if (progress < 0 || progress > this.amount) {
				throw new CustomNPCsException(
						"Progress has to be between 0 and " + this.amount
				);
			}

			PlayerData data = PlayerData.get(this.player);
			PlayerQuestData playerData = data.questData;

			QuestData questData = (QuestData) playerData.activeQuests.get(
					QuestManual.this.questId
			);

			if (questData == null) {
				return;
			}

			HashMap<String, Integer> manual =
					QuestManual.this.getManual(questData);

			Integer currentProgress = manual.get(this.entity);

			if (currentProgress == null || currentProgress != progress) {
				manual.put(this.entity, progress);

				QuestManual.this.setManual(questData, manual);

				data.questData.checkQuestCompletion(this.player, 5);
				data.updateClient = true;
			}
		}

		public int getMaxProgress() {
			return this.amount;
		}

		public boolean isCompleted() {
			return this.getProgress() >= this.amount;
		}

		@SuppressWarnings("deprecation")
		public String getText() {
			return I18n.translateToLocal(this.entity)
					+ ": "
					+ this.getProgress()
					+ "/"
					+ this.getMaxProgress();
		}
	}
}
