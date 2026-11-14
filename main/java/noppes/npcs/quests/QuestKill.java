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

public class QuestKill extends QuestInterface {

	public TreeMap<String, Integer> targets = new TreeMap<>();

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.targets = new TreeMap<>(
				readStringIntegerMap(
						compound.getTagList("QuestDialogs", 10)
				)
		);
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setTag(
				"QuestDialogs",
				writeStringIntegerMap(this.targets)
		);
	}

	public boolean isCompleted(EntityPlayer player) {
		PlayerQuestData playerData = PlayerData.get(player).questData;
		QuestData data = (QuestData) playerData.activeQuests.get(this.questId);

		if (data == null) {
			return false;
		}

		HashMap<String, Integer> killed = this.getKilled(data);

		if (killed.size() != this.targets.size()) {
			return false;
		}

		for (Map.Entry<String, Integer> target : this.targets.entrySet()) {
			Integer killedAmount = killed.get(target.getKey());

			if (killedAmount == null || target.getValue() > killedAmount) {
				return false;
			}
		}

		return true;
	}

	public void handleComplete(EntityPlayer player) {
	}

	public HashMap<String, Integer> getKilled(QuestData data) {
		return readStringIntegerMap(
				data.extraData.getTagList("Killed", 10)
		);
	}

	public void setKilled(
			QuestData data,
			Map<String, Integer> killed) {

		data.extraData.setTag(
				"Killed",
				writeStringIntegerMap(killed)
		);
	}

	public IQuestObjective[] getObjectives(EntityPlayer player) {
		List<IQuestObjective> objectives = new ArrayList<>();

		for (Map.Entry<String, Integer> target : this.targets.entrySet()) {
			objectives.add(new QuestKillObjective(
					player,
					target.getKey(),
					target.getValue()
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

	private class QuestKillObjective implements IQuestObjective {

		private final EntityPlayer player;
		private final String entity;
		private final int amount;

		public QuestKillObjective(
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
					QuestKill.this.questId
			);

			HashMap<String, Integer> killed =
					QuestKill.this.getKilled(questData);

			Integer progress = killed.get(this.entity);

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
					QuestKill.this.questId
			);

			HashMap<String, Integer> killed =
					QuestKill.this.getKilled(questData);

			Integer currentProgress = killed.get(this.entity);

			if (currentProgress == null || currentProgress != progress) {
				killed.put(this.entity, progress);

				QuestKill.this.setKilled(questData, killed);

				data.questData.checkQuestCompletion(this.player, 2);
				data.questData.checkQuestCompletion(this.player, 4);

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
			String name = "entity." + this.entity + ".name";
			String transName = I18n.translateToLocal(name);

			if (name.equals(transName)) {
				transName = this.entity;
			}

			return transName + ": "
					+ this.getProgress()
					+ "/" + this.getMaxProgress();
		}
	}
}
