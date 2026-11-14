package noppes.npcs.client.gui.questtypes;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.global.GuiNPCManageQuest;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcButtonYesNo;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.containers.ContainerNpcQuestTypeItem;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestItem;

public class GuiNpcQuestTypeItem extends GuiContainerNPCInterface
		implements ITextfieldListener {

	private final Quest quest;
	private final ResourceLocation resource;

	public GuiNpcQuestTypeItem(
			EntityNPCInterface npc,
			ContainerNpcQuestTypeItem container) {

		super(npc, container);

		this.quest = NoppesUtilServer.getEditingQuest(this.player);
		this.resource = this.getResource("questreward.png");

		this.title = "";
		this.closeOnEsc = false;
	}

	public void initGui() {
		super.initGui();

		QuestItem questItem = (QuestItem) this.quest.questInterface;

		this.addLabel(new GuiNpcLabel(
				0,
				"quest.takeitems",
				this.guiLeft + 4,
				this.guiTop + 1
		));

		this.addButton(new GuiNpcButton(
				0,
				this.guiLeft + 4,
				this.guiTop + 10,
				50,
				18,
				new String[] { "gui.yes", "gui.no" },
				questItem.leaveItems ? 1 : 0
		));

		this.addLabel(new GuiNpcLabel(
				1,
				"gui.ignoreDamage",
				this.guiLeft + 4,
				this.guiTop + 28
		));

		this.addButton(new GuiNpcButtonYesNo(
				1,
				this.guiLeft + 4,
				this.guiTop + 37,
				50,
				18,
				questItem.ignoreDamage
		));

		this.addLabel(new GuiNpcLabel(
				2,
				"gui.ignoreNBT",
				this.guiLeft + 4,
				this.guiTop + 55
		));

		this.addButton(new GuiNpcButtonYesNo(
				2,
				this.guiLeft + 4,
				this.guiTop + 64,
				50,
				18,
				questItem.ignoreNBT
		));

		this.addButton(new GuiNpcButton(
				5,
				this.guiLeft,
				this.guiTop + this.ySize,
				98,
				20,
				"gui.back"
		));
	}

	public void actionPerformed(GuiButton button) {
		QuestItem questItem = (QuestItem) this.quest.questInterface;

		if (button.id == 0) {
			questItem.leaveItems =
					((GuiNpcButton) button).getValue() == 1;
		}

		if (button.id == 1) {
			questItem.ignoreDamage =
					((GuiNpcButtonYesNo) button).getBoolean();
		}

		if (button.id == 2) {
			questItem.ignoreNBT =
					((GuiNpcButtonYesNo) button).getBoolean();
		}

		if (button.id == 5) {
			NoppesUtil.openGUI(
					this.player,
					GuiNPCManageQuest.Instance
			);
		}
	}

	protected void drawGuiContainerBackgroundLayer(
			float partialTicks,
			int mouseX,
			int mouseY) {

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		this.mc.renderEngine.bindTexture(this.resource);

		int x = (this.width - this.xSize) / 2;
		int y = (this.height - this.ySize) / 2;

		this.drawTexturedModalRect(
				x,
				y,
				0,
				0,
				this.xSize,
				this.ySize
		);

		super.drawGuiContainerBackgroundLayer(
				partialTicks,
				mouseX,
				mouseY
		);
	}

	public void save() {
	}

	public void unFocused(GuiNpcTextField textfield) {
		this.quest.rewardExp = textfield.getInteger();
	}
}
