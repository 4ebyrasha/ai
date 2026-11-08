package noppes.npcs.client.gui.questtypes;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.select.GuiDialogSelection;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiSelectionListener;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestDialog;

public class GuiNpcQuestTypeDialog extends SubGuiInterface
		implements GuiSelectionListener, IGuiData {

	private final QuestDialog quest;
	private final Map<Integer, String> data = new HashMap<>();
	private int selectedSlot;

	public GuiNpcQuestTypeDialog(
			EntityNPCInterface npc,
			Quest q,
			GuiScreen parent) {

		this.npc = npc;
		this.parent = parent;
		this.title = "Quest Dialog Setup";
		this.quest = (QuestDialog) q.questInterface;

		this.setBackground("menubg.png");

		this.xSize = 256;
		this.ySize = 216;
		this.closeOnEsc = true;

		Client.sendData(
				EnumPacketServer.QuestDialogGetTitle,
				this.quest.dialogs.getOrDefault(0, -1),
				this.quest.dialogs.getOrDefault(1, -1),
				this.quest.dialogs.getOrDefault(2, -1),
				this.quest.dialogs.getOrDefault(3, -1),
				this.quest.dialogs.getOrDefault(4, -1),
				this.quest.dialogs.getOrDefault(5, -1)
		);
	}

	public void initGui() {
		super.initGui();

		for (int i = 0; i < 6; ++i) {
			String title = this.data.getOrDefault(
					i,
					"dialog.selectoption"
			);

			this.addButton(new GuiNpcButton(
					i + 9,
					this.guiLeft + 10,
					55 + i * 22,
					20,
					20,
					"X"
			));

			this.addButton(new GuiNpcButton(
					i + 3,
					this.guiLeft + 34,
					55 + i * 22,
					210,
					20,
					title
			));
		}

		this.addButton(new GuiNpcButton(
				0,
				this.guiLeft + 150,
				this.guiTop + 190,
				98,
				20,
				"gui.back"
		));
	}

	protected void actionPerformed(GuiButton guibutton) {
		GuiNpcButton button = (GuiNpcButton) guibutton;

		if (button.id == 0) {
			this.close();
			return;
		}

		if (button.id >= 3 && button.id < 9) {
			this.selectedSlot = button.id - 3;

			int dialogId = this.quest.dialogs.getOrDefault(
					this.selectedSlot,
					-1
			);

			this.setSubGui(new GuiDialogSelection(dialogId));
			return;
		}

		if (button.id >= 9 && button.id < 15) {
			int slot = button.id - 9;

			this.quest.dialogs.remove(slot);
			this.data.remove(slot);

			this.initGui();
		}
	}

	public void selected(int id, String name) {
		this.quest.dialogs.put(this.selectedSlot, id);
		this.data.put(this.selectedSlot, name);
	}

	public void setGuiData(NBTTagCompound compound) {
		this.data.clear();

		for (int i = 0; i < 6; ++i) {
			String key = String.valueOf(i + 1);

			if (compound.hasKey(key)) {
				this.data.put(i, compound.getString(key));
			}
		}

		this.initGui();
	}
}
