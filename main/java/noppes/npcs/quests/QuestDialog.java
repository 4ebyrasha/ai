package noppes.npcs.quests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NBTTags;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.PlayerData;

public class QuestDialog extends QuestInterface {

	public HashMap<Integer, Integer> dialogs = new HashMap<>();

	public void readEntityFromNBT(NBTTagCompound compound) {
		this.dialogs = NBTTags.getIntegerIntegerMap(
				compound.getTagList("QuestDialogs", 10)
		);
	}

	public void writeEntityToNBT(NBTTagCompound compound) {
		compound.setTag(
				"QuestDialogs",
				NBTTags.nbtIntegerIntegerMap(this.dialogs)
		);
	}

	public boolean isCompleted(EntityPlayer player) {
		PlayerData playerData = PlayerData.get(player);

		for (Integer dialogId : this.dialogs.values()) {
			if (!playerData.dialogData.dialogsRead.contains(dialogId)) {
				return false;
			}
		}

		return true;
	}

	public void handleComplete(EntityPlayer player) {
	}

	public IQuestObjective[] getObjectives(EntityPlayer player) {
		List<IQuestObjective> objectives = new ArrayList<>();

		for (int i = 0; i < 6; ++i) {
			Integer dialogId = this.dialogs.get(i);

			if (dialogId == null) {
				continue;
			}

			Dialog dialog = (Dialog) DialogController.instance.dialogs.get(
					dialogId
			);

			if (dialog != null) {
				objectives.add(new QuestDialogObjective(player, dialog));
			}
		}

		return objectives.toArray(new IQuestObjective[0]);
	}

	private static class QuestDialogObjective implements IQuestObjective {

		private final EntityPlayer player;
		private final Dialog dialog;

		public QuestDialogObjective(EntityPlayer player, Dialog dialog) {
			this.player = player;
			this.dialog = dialog;
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
			boolean completed = data.dialogData.dialogsRead.contains(
					this.dialog.id
			);

			if (progress == 0 && completed) {
				data.dialogData.dialogsRead.remove(this.dialog.id);
				data.questData.checkQuestCompletion(this.player, 1);
				data.updateClient = true;
			}

			if (progress == 1 && !completed) {
				data.dialogData.dialogsRead.add(this.dialog.id);
				data.questData.checkQuestCompletion(this.player, 1);
				data.updateClient = true;
			}
		}

		public int getMaxProgress() {
			return 1;
		}

		public boolean isCompleted() {
			PlayerData data = PlayerData.get(this.player);

			return data.dialogData.dialogsRead.contains(this.dialog.id);
		}

		public String getText() {
			return this.dialog.title
					+ (this.isCompleted() ? " (read)" : " (unread)");
		}
	}
}
