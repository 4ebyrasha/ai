package noppes.npcs.client.gui;

import java.util.Arrays;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;

public class SubGuiNpcCommand extends SubGuiInterface implements ITextfieldListener {
	private static final int COMMAND_COUNT = 4;

	public String command;

	private final String[] commands = new String[COMMAND_COUNT];

	public SubGuiNpcCommand(String command) {
		this.command = command == null ? "" : command;

		Arrays.fill(this.commands, "");

		String[] savedCommands = this.command.replace("\r", "").split("\n", -1);

		System.arraycopy(
				savedCommands,
				0,
				this.commands,
				0,
				Math.min(savedCommands.length, COMMAND_COUNT)
		);

		this.setBackground("menubg.png");
		this.xSize = 256;
		this.ySize = 216;
		this.closeOnEsc = true;
	}

	@Override
	public void initGui() {
		super.initGui();

		for (int i = 0; i < COMMAND_COUNT; ++i) {
			this.addTextField(new GuiNpcTextField(
					i,
					this,
					this.fontRenderer,
					this.guiLeft + 4,
					this.guiTop + 35 + i * 22,
					248,
					20,
					this.commands[i]));

			this.getTextField(i).setMaxStringLength(32767);
		}

		this.addLabel(new GuiNpcLabel(4, "advMode.command",
				this.guiLeft + 4, this.guiTop + 124));

		this.addLabel(new GuiNpcLabel(5, "advMode.nearestPlayer",
				this.guiLeft + 4, this.guiTop + 137));

		this.addLabel(new GuiNpcLabel(6, "advMode.randomPlayer",
				this.guiLeft + 4, this.guiTop + 150));

		this.addLabel(new GuiNpcLabel(7, "advMode.allPlayers",
				this.guiLeft + 4, this.guiTop + 163));

		this.addLabel(new GuiNpcLabel(8, "dialog.commandoptionplayer",
				this.guiLeft + 4, this.guiTop + 176));

		this.addButton(new GuiNpcButton(
				66, this.guiLeft + 82, this.guiTop + 190,
				98, 20, "gui.done"));
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		if (guibutton.id == 66) {
			this.close();
		}
	}

	@Override
	public void unFocused(GuiNpcTextField textfield) {
		int id = textfield.getId();

		if (id >= 0 && id < COMMAND_COUNT) {
			this.commands[id] = textfield.getText();
		}
	}

	@Override
	public void save() {
		int lastNonEmptyCommand = -1;

		for (int i = 0; i < COMMAND_COUNT; ++i) {
			GuiNpcTextField field = this.getTextField(i);

			if (field != null) {
				this.commands[i] = field.getText();
			}

			if (!this.commands[i].trim().isEmpty()) {
				lastNonEmptyCommand = i;
			}
		}

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i <= lastNonEmptyCommand; ++i) {
			if (i > 0) {
				builder.append('\n');
			}

			builder.append(this.commands[i]);
		}

		this.command = builder.toString();
	}
}
