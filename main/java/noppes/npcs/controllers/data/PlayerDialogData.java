package noppes.npcs.controllers.data;

import java.util.HashSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class PlayerDialogData {

	public HashSet<Integer> dialogsRead = new HashSet<>();

	public void loadNBTData(NBTTagCompound compound) {
		HashSet<Integer> loadedDialogs = new HashSet<>();

		if (compound != null) {
			NBTTagList list = compound.getTagList("DialogData", 10);

			for (int i = 0; i < list.tagCount(); ++i) {
				NBTTagCompound entry = list.getCompoundTagAt(i);
				loadedDialogs.add(entry.getInteger("Dialog"));
			}

			this.dialogsRead = loadedDialogs;
		}
	}

	public void saveNBTData(NBTTagCompound compound) {
		NBTTagList list = new NBTTagList();

		for (Integer dialogId : this.dialogsRead) {
			NBTTagCompound entry = new NBTTagCompound();
			entry.setInteger("Dialog", dialogId);
			list.appendTag(entry);
		}

		compound.setTag("DialogData", list);
	}
}
