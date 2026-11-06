
package noppes.npcs.client.gui.mainmenu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.gui.advanced.GuiNPCAdvancedLinkedNpc;
import noppes.npcs.client.gui.advanced.GuiNPCDialogNpcOptions;
import noppes.npcs.client.gui.advanced.GuiNPCFactionSetup;
import noppes.npcs.client.gui.advanced.GuiNPCLinesMenu;
import noppes.npcs.client.gui.advanced.GuiNPCMarks;
import noppes.npcs.client.gui.advanced.GuiNPCNightSetup;
import noppes.npcs.client.gui.advanced.GuiNPCScenes;
import noppes.npcs.client.gui.advanced.GuiNPCSoundsMenu;
import noppes.npcs.client.gui.roles.GuiNpcBard;
import noppes.npcs.client.gui.roles.GuiNpcConversation;
import noppes.npcs.client.gui.roles.GuiNpcPuppet;
import noppes.npcs.client.gui.roles.GuiRoleDialog;
import noppes.npcs.client.gui.util.GuiButtonBiDirectional;
import noppes.npcs.client.gui.util.GuiNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiNpcAdvanced extends GuiNPCInterface2 implements IGuiData {
	private static final int[] ROLE_IDS = { 0, 1, 5, 7 };
	private static final String[] ROLE_NAMES = {
			"role.none", "role.trader", "role.mailman", "dialog.dialog"
	};

	private static final int[] JOB_IDS = { 0, 1, 7, 8, 9 };
	private static final String[] JOB_NAMES = {
			"job.none", "job.bard", "job.conversation", "job.chunkloader", "job.puppet"
	};

	private boolean hasChanges = false;

	public GuiNpcAdvanced(EntityNPCInterface npc) {
		super(npc, 4);
		Client.sendData(EnumPacketServer.MainmenuAdvancedGet);
	}

	private int getOptionIndex(int[] ids, int value) {
		for (int i = 0; i < ids.length; ++i) {
			if (ids[i] == value) {
				return i;
			}
		}

		return 0;
	}

	public void initGui() {
		super.initGui();

		int y = this.guiTop + 8;

		this.addButton(new GuiNpcButton(3, this.guiLeft + 85 + 160, y, 52, 20, "selectServer.edit"));
		this.addButton(new GuiButtonBiDirectional(8, this.guiLeft + 85, y, 155, 20,
				ROLE_NAMES, this.getOptionIndex(ROLE_IDS, this.npc.advanced.role)));
		this.getButton(3).setEnabled(this.npc.advanced.role != 0 && this.npc.advanced.role != 5);

		int var10004 = this.guiLeft + 85 + 160;
		y += 22;

		this.addButton(new GuiNpcButton(4, var10004, y, 52, 20, "selectServer.edit"));
		this.addButton(new GuiButtonBiDirectional(5, this.guiLeft + 85, y, 155, 20,
				JOB_NAMES, this.getOptionIndex(JOB_IDS, this.npc.advanced.job)));
		this.getButton(4).setEnabled(this.npc.advanced.job != 0 && this.npc.advanced.job != 8);

		var10004 = this.guiLeft + 15;
		y += 22;

		this.addButton(new GuiNpcButton(7, var10004, y, 190, 20, "advanced.lines"));
		this.addButton(new GuiNpcButton(9, this.guiLeft + 208, y, 190, 20, "menu.factions"));

		var10004 = this.guiLeft + 15;
		y += 22;

		this.addButton(new GuiNpcButton(10, var10004, y, 190, 20, "dialog.dialogs"));
		this.addButton(new GuiNpcButton(11, this.guiLeft + 208, y, 190, 20, "advanced.sounds"));

		var10004 = this.guiLeft + 15;
		y += 22;

		this.addButton(new GuiNpcButton(12, var10004, y, 190, 20, "advanced.night"));
		this.addButton(new GuiNpcButton(13, this.guiLeft + 208, y, 190, 20, "global.linked"));

		var10004 = this.guiLeft + 15;
		y += 22;

		this.addButton(new GuiNpcButton(14, var10004, y, 190, 20, "advanced.scenes"));
		this.addButton(new GuiNpcButton(15, this.guiLeft + 208, y, 190, 20, "advanced.marks"));
	}

	protected void actionPerformed(GuiButton guibutton) {
		GuiNpcButton button = (GuiNpcButton) guibutton;

		if (button.id == 3) {
			this.save();
			Client.sendData(EnumPacketServer.RoleGet);
		}

		if (button.id == 8) {
			this.hasChanges = true;
			this.npc.advanced.setRole(ROLE_IDS[button.getValue()]);
			this.getButton(3).setEnabled(this.npc.advanced.role != 0 && this.npc.advanced.role != 5);
		}

		if (button.id == 4) {
			this.save();
			Client.sendData(EnumPacketServer.JobGet);
		}

		if (button.id == 5) {
			this.hasChanges = true;
			this.npc.advanced.setJob(JOB_IDS[button.getValue()]);
			this.getButton(4).setEnabled(this.npc.advanced.job != 0 && this.npc.advanced.job != 8);
		}

		if (button.id == 9) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCFactionSetup(this.npc));
		}

		if (button.id == 10) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCDialogNpcOptions(this.npc, this));
		}

		if (button.id == 11) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCSoundsMenu(this.npc));
		}

		if (button.id == 7) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCLinesMenu(this.npc));
		}

		if (button.id == 12) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCNightSetup(this.npc));
		}

		if (button.id == 13) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCAdvancedLinkedNpc(this.npc));
		}

		if (button.id == 14) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCScenes(this.npc));
		}

		if (button.id == 15) {
			this.save();
			NoppesUtil.openGUI(this.player, new GuiNPCMarks(this.npc));
		}
	}

	public void setGuiData(NBTTagCompound compound) {
		if (compound.hasKey("RoleData")) {
			if (this.npc.roleInterface != null) {
				this.npc.roleInterface.readFromNBT(compound);
			}

			if (this.npc.advanced.role == 1) {
				NoppesUtil.requestOpenGUI(EnumGuiType.SetupTrader);
			} else if (this.npc.advanced.role == 7) {
				NoppesUtil.openGUI(this.player, new GuiRoleDialog(this.npc));
			}
		} else if (compound.hasKey("JobData")) {
			if (this.npc.jobInterface != null) {
				this.npc.jobInterface.readFromNBT(compound);
			}

			if (this.npc.advanced.job == 1) {
				NoppesUtil.openGUI(this.player, new GuiNpcBard(this.npc));
			} else if (this.npc.advanced.job == 7) {
				NoppesUtil.openGUI(this.player, new GuiNpcConversation(this.npc));
			} else if (this.npc.advanced.job == 9) {
				NoppesUtil.openGUI(this.player, new GuiNpcPuppet(this, (EntityCustomNpc) this.npc));
			}
		} else {
			this.npc.advanced.readToNBT(compound);
			this.initGui();
		}
	}

	public void save() {
		if (this.hasChanges) {
			Client.sendData(EnumPacketServer.MainmenuAdvancedSave,
					this.npc.advanced.writeToNBT(new NBTTagCompound()));
			this.hasChanges = false;
		}
	}
}
