package noppes.npcs.client.gui.questtypes;

import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestManual;

public class GuiNpcQuestTypeManual extends SubGuiInterface
		implements ITextfieldListener {

	private static final int TARGET_COUNT = 6;
	private static final int ROW_HEIGHT = 22;

	private final QuestManual quest;

	public GuiNpcQuestTypeManual(
			EntityNPCInterface npc,
			Quest q,
			GuiScreen parent) {

		this.npc = npc;
		this.parent = parent;
		this.title = "Quest Manual Setup";
		this.quest = (QuestManual) q.questInterface;

		this.setBackground("menubg.png");
		this.xSize = 356;
		this.ySize = 216;
		this.closeOnEsc = true;
	}

	public void initGui() {
		super.initGui();

		this.addLabel(new GuiNpcLabel(
				0,
				"You can fill in npc or player names too",
				this.guiLeft + 4,
				this.guiTop + 34
		));

		int row = 0;

		for (Map.Entry<String, Integer> entry
				: this.quest.manuals.entrySet()) {

			if (row >= TARGET_COUNT) {
				break;
			}

			this.addTargetFields(
					row,
					entry.getKey(),
					entry.getValue()
			);

			++row;
		}

		while (row < TARGET_COUNT) {
			this.addTargetFields(row, "", 1);
			++row;
		}

		this.addButton(new GuiNpcButton(
				0,
				this.guiLeft + 4,
				this.guiTop + 190,
				98,
				20,
				"gui.back"
		));
	}

	private void addTargetFields(
			int row,
			String name,
			int amount) {

		int y = this.guiTop + 52 + row * ROW_HEIGHT;

		this.addTextField(new GuiNpcTextField(
				row,
				this,
				this.fontRenderer,
				this.guiLeft + 4,
				y,
				180,
				20,
				name
		));

		this.addTextField(new GuiNpcTextField(
				row + TARGET_COUNT,
				this,
				this.fontRenderer,
				this.guiLeft + 186,
				y,
				24,
				20,
				String.valueOf(amount)
		));

		this.getTextField(row + TARGET_COUNT).numbersOnly = true;
		this.getTextField(row + TARGET_COUNT).setMinMaxDefault(
				1,
				Integer.MAX_VALUE,
				1
		);
	}

	protected void actionPerformed(GuiButton guibutton) {
		super.actionPerformed(guibutton);

		if (guibutton.id == 0) {
			this.close();
		}
	}

	public void unFocused(GuiNpcTextField textField) {
		this.saveTargets();
	}

	private void saveTargets() {
		TreeMap<String, Integer> map = new TreeMap<>();

		for (int i = 0; i < TARGET_COUNT; ++i) {
			String name = this.getTextField(i).getText().trim();

			if (!name.isEmpty()) {
				int amount = Math.max(
						1,
						this.getTextField(i + TARGET_COUNT).getInteger()
				);

				map.put(name, amount);
			}
		}

		this.quest.manuals = map;
	}
}
