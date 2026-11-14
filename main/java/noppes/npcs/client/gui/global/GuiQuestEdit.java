package noppes.npcs.client.gui.global;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.NoppesUtilServer;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.SubGuiMailmanSendSetup;
import noppes.npcs.client.gui.SubGuiNpcCommand;
import noppes.npcs.client.gui.SubGuiNpcFactionOptions;
import noppes.npcs.client.gui.SubGuiNpcTextArea;
import noppes.npcs.client.gui.questtypes.GuiNpcQuestTypeDialog;
import noppes.npcs.client.gui.questtypes.GuiNpcQuestTypeKill;
import noppes.npcs.client.gui.questtypes.GuiNpcQuestTypeLocation;
import noppes.npcs.client.gui.questtypes.GuiNpcQuestTypeManual;
import noppes.npcs.client.gui.select.GuiQuestSelection;
import noppes.npcs.client.gui.util.GuiButtonBiDirectional;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.GuiSelectionListener;
import noppes.npcs.client.gui.util.ISubGuiListener;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerMail;
import noppes.npcs.controllers.data.Quest;

public class GuiQuestEdit extends SubGuiInterface
		implements ISubGuiListener, GuiSelectionListener, ITextfieldListener {

	private Quest quest;
	private boolean questlogTA = false;
	private int selectedNextQuestSlot = 0;

	public GuiQuestEdit(Quest quest) {
		this.quest = quest;
		this.setBackground("menubg.png");
		this.xSize = 386;
		this.ySize = 226;
		NoppesUtilServer.setEditingQuest(this.player, quest);
		this.closeOnEsc = true;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.addLabel(new GuiNpcLabel(
				1, "gui.title",
				this.guiLeft + 4, this.guiTop + 8
		));

		this.addTextField(new GuiNpcTextField(
				1, this, this.fontRenderer,
				this.guiLeft + 46, this.guiTop + 3,
				220, 20, this.quest.title
		));

		this.addLabel(new GuiNpcLabel(
				0, "ID",
				this.guiLeft + 268, this.guiTop + 4
		));

		this.addLabel(new GuiNpcLabel(
				2, this.quest.id + "",
				this.guiLeft + 268, this.guiTop + 14
		));

		this.addLabel(new GuiNpcLabel(
				3, "quest.completedtext",
				this.guiLeft + 4, this.guiTop + 30
		));

		this.addButton(new GuiNpcButton(
				3, this.guiLeft + 120, this.guiTop + 25,
				50, 20, "selectServer.edit"
		));

		this.addLabel(new GuiNpcLabel(
				4, "quest.questlogtext",
				this.guiLeft + 4, this.guiTop + 51
		));

		this.addButton(new GuiNpcButton(
				4, this.guiLeft + 120, this.guiTop + 46,
				50, 20, "selectServer.edit"
		));

		this.addLabel(new GuiNpcLabel(
				5, "quest.reward",
				this.guiLeft + 4, this.guiTop + 72
		));

		this.addButton(new GuiNpcButton(
				5, this.guiLeft + 120, this.guiTop + 67,
				50, 20, "selectServer.edit"
		));

		this.addLabel(new GuiNpcLabel(
				6, "gui.type",
				this.guiLeft + 4, this.guiTop + 93
		));

		this.addButton(new GuiButtonBiDirectional(
				6,
				this.guiLeft + 70,
				this.guiTop + 88,
				90,
				20,
				new String[] {
						"quest.item",
						"quest.dialog",
						"quest.kill",
						"quest.location",
						"quest.areakill",
						"quest.manual"
				},
				this.quest.type
		));

		this.addButton(new GuiNpcButton(
				7, this.guiLeft + 162, this.guiTop + 88,
				50, 20, "selectServer.edit"
		));

		this.addLabel(new GuiNpcLabel(
				8, "quest.repeatable",
				this.guiLeft + 4, this.guiTop + 114
		));

		this.addButton(new GuiButtonBiDirectional(
				8,
				this.guiLeft + 70,
				this.guiTop + 109,
				140,
				20,
				new String[] {
						"gui.no",
						"gui.yes",
						"quest.mcdaily",
						"quest.mcweekly",
						"quest.rldaily",
						"quest.rlweekly"
				},
				this.quest.repeat.ordinal()
		));

		this.addButton(new GuiNpcButton(
				9,
				this.guiLeft + 4,
				this.guiTop + 131,
				90,
				20,
				new String[] { "quest.npc", "quest.instant" },
				this.quest.completion.ordinal()
		));

		if (this.quest.completerNpc.isEmpty()) {
			this.quest.completerNpc = this.npc.display.getName();
		}

		this.addTextField(new GuiNpcTextField(
				2, this, this.fontRenderer,
				this.guiLeft + 96, this.guiTop + 131,
				114, 20, this.quest.completerNpc
		));

		this.getTextField(2).enabled =
				this.quest.completion == EnumQuestCompletion.Npc;

		this.addLabel(new GuiNpcLabel(
				10, "faction.options",
				this.guiLeft + 214, this.guiTop + 30
		));

		this.addButton(new GuiNpcButton(
				10, this.guiLeft + 330, this.guiTop + 25,
				50, 20, "selectServer.edit"
		));

		this.addLabel(new GuiNpcLabel(
				15, "advMode.command",
				this.guiLeft + 214, this.guiTop + 52
		));

		this.addButton(new GuiNpcButton(
				15, this.guiLeft + 330, this.guiTop + 47,
				50, 20, "selectServer.edit"
		));

		// Three mail slots on the left.
		this.addMailButton(0, 13, 14, 153);
		this.addMailButton(1, 16, 17, 175);
		this.addMailButton(2, 18, 19, 197);

		// Three next-quest slots on the right.
		this.addNextQuestButton(0, 11, 12, 153);
		this.addNextQuestButton(1, 20, 21, 175);
		this.addNextQuestButton(2, 22, 23, 197);

		this.addButton(new GuiNpcButton(
				66, this.guiLeft + 362, this.guiTop + 4,
				20, 20, "X"
		));
	}

	private void addMailButton(int slot, int buttonId, int clearId, int y) {
		PlayerMail mail = this.quest.getMail(slot);

		GuiNpcButton mailButton = new GuiNpcButton(
				buttonId,
				this.guiLeft + 4,
				this.guiTop + y,
				164,
				20,
				"mailbox.setup"
		);

		if (!mail.subject.isEmpty()) {
			mailButton.setDisplayText(mail.subject);
		}

		this.addButton(mailButton);

		this.addButton(new GuiNpcButton(
				clearId,
				this.guiLeft + 170,
				this.guiTop + y,
				20,
				20,
				"X"
		));
	}

	private void addNextQuestButton(int slot, int buttonId, int clearId, int y) {
		GuiNpcButton questButton = new GuiNpcButton(
				buttonId,
				this.guiLeft + 195,
				this.guiTop + y,
				144,
				20,
				"quest.next"
		);

		String title = this.quest.getNextQuestTitle(slot);

		if (!title.isEmpty()) {
			questButton.setDisplayText(title);
		}

		this.addButton(questButton);

		this.addButton(new GuiNpcButton(
				clearId,
				this.guiLeft + 341,
				this.guiTop + y,
				20,
				20,
				"X"
		));
	}

	public void buttonEvent(GuiButton guibutton) {
		int id = guibutton.id;
		GuiNpcButton button = (GuiNpcButton) guibutton;

		if (id == 3) {
			this.questlogTA = false;
			this.setSubGui(new SubGuiNpcTextArea(this.quest.completeText));
		} else if (id == 4) {
			this.questlogTA = true;
			this.setSubGui(new SubGuiNpcTextArea(this.quest.logText));
		} else if (id == 5) {
			Client.sendData(
					EnumPacketServer.QuestOpenGui,
					EnumGuiType.QuestReward,
					this.quest.writeToNBT(new NBTTagCompound())
			);
		} else if (id == 6) {
			this.quest.setType(button.getValue());
		} else if (id == 7) {
			if (this.quest.type == 0) {
				Client.sendData(
						EnumPacketServer.QuestOpenGui,
						EnumGuiType.QuestItem,
						this.quest.writeToNBT(new NBTTagCompound())
				);
			} else if (this.quest.type == 1) {
				this.setSubGui(new GuiNpcQuestTypeDialog(this.npc, this.quest, this.parent));
			} else if (this.quest.type == 2 || this.quest.type == 4) {
				this.setSubGui(new GuiNpcQuestTypeKill(this.npc, this.quest, this.parent));
			} else if (this.quest.type == 3) {
				this.setSubGui(new GuiNpcQuestTypeLocation(this.npc, this.quest, this.parent));
			} else if (this.quest.type == 5) {
				this.setSubGui(new GuiNpcQuestTypeManual(this.npc, this.quest, this.parent));
			}
		} else if (id == 8) {
			this.quest.repeat = EnumQuestRepeat.values()[button.getValue()];
		} else if (id == 9) {
			this.quest.completion = EnumQuestCompletion.values()[button.getValue()];
			this.getTextField(2).enabled =
					this.quest.completion == EnumQuestCompletion.Npc;
		} else if (id == 10) {
			this.setSubGui(new SubGuiNpcFactionOptions(this.quest.factionOptions));
		} else if (id == 15) {
			this.setSubGui(new SubGuiNpcCommand(this.quest.command));
		} else if (id == 13 || id == 16 || id == 18) {
			int slot = id == 13 ? 0 : id == 16 ? 1 : 2;
			this.setSubGui(new SubGuiMailmanSendSetup(this.quest.getMail(slot)));
		} else if (id == 14 || id == 17 || id == 19) {
			int slot = id == 14 ? 0 : id == 17 ? 1 : 2;
			this.quest.clearMail(slot);
			this.initGui();
		} else if (id == 11 || id == 20 || id == 22) {
			this.selectedNextQuestSlot = id == 11 ? 0 : id == 20 ? 1 : 2;
			this.setSubGui(new GuiQuestSelection(
					this.quest.getNextQuestId(this.selectedNextQuestSlot)
			));
		} else if (id == 12 || id == 21 || id == 23) {
			int slot = id == 12 ? 0 : id == 21 ? 1 : 2;
			this.quest.setNextQuest(slot, -1, "");
			this.initGui();
		} else if (id == 66) {
			this.close();
		}
	}

	@Override
	public void unFocused(GuiNpcTextField textField) {
		if (textField.getId() == 1) {
			for (this.quest.title = textField.getText();
				 QuestController.instance.containsQuestName(this.quest.category, this.quest);
				 this.quest.title += "_") {
			}
		}

		if (textField.getId() == 2) {
			this.quest.completerNpc = textField.getText();
		}
	}

	@Override
	public void subGuiClosed(SubGuiInterface subgui) {
		if (subgui instanceof SubGuiNpcTextArea) {
			SubGuiNpcTextArea gui = (SubGuiNpcTextArea) subgui;

			if (this.questlogTA) {
				this.quest.logText = gui.text;
			} else {
				this.quest.completeText = gui.text;
			}
		} else if (subgui instanceof SubGuiNpcCommand) {
			this.quest.command = ((SubGuiNpcCommand) subgui).command;
		} else {
			this.initGui();
		}
	}

	@Override
	public void selected(int id, String name) {
		this.quest.setNextQuest(this.selectedNextQuestSlot, id, name);
	}

	@Override
	public void close() {
		super.close();
	}

	@Override
	public void save() {
		GuiNpcTextField.unfocus();

		Client.sendData(
				EnumPacketServer.QuestSave,
				this.quest.category.id,
				this.quest.writeToNBT(new NBTTagCompound())
		);
	}
}
