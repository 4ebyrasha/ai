package noppes.npcs.client.gui.questtypes;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestLocation;

public class GuiNpcQuestTypeLocation extends SubGuiInterface
		implements ITextfieldListener {

	private final QuestLocation quest;

	public GuiNpcQuestTypeLocation(
			EntityNPCInterface npc,
			Quest q,
			GuiScreen parent) {

		this.npc = npc;
		this.parent = parent;
		this.title = "Quest Location Setup";
		this.quest = (QuestLocation) q.questInterface;

		this.setBackground("menubg.png");
		this.xSize = 256;
		this.ySize = 216;
		this.closeOnEsc = true;
	}

	public void initGui() {
		super.initGui();

		this.addLabel(new GuiNpcLabel(
				0,
				"Fill in the name of your Location Quest Block",
				this.guiLeft + 4,
				this.guiTop + 34
		));

		String[] locations = {
				this.quest.location,
				this.quest.location2,
				this.quest.location3,
				this.quest.location4,
				this.quest.location5,
				this.quest.location6
		};

		for (int i = 0; i < locations.length; ++i) {
			this.addTextField(new GuiNpcTextField(
					i,
					this,
					this.fontRenderer,
					this.guiLeft + 4,
					this.guiTop + 52 + i * 22,
					180,
					20,
					locations[i]
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
		super.actionPerformed(guibutton);

		if (guibutton.id == 0) {
			this.close();
		}
	}

	public void unFocused(GuiNpcTextField textfield) {
		String value = textfield.getText();

		switch (textfield.getId()) {
			case 0:
				this.quest.location = value;
				break;
			case 1:
				this.quest.location2 = value;
				break;
			case 2:
				this.quest.location3 = value;
				break;
			case 3:
				this.quest.location4 = value;
				break;
			case 4:
				this.quest.location5 = value;
				break;
			case 5:
				this.quest.location6 = value;
				break;
		}
	}
}
