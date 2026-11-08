package noppes.npcs.client.gui;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.data.DataDisplay;

public class SubGuiNpcName extends SubGuiInterface implements ITextfieldListener {
	private DataDisplay display;

	public SubGuiNpcName(DataDisplay display) {
		this.display = display;
		this.setBackground("menubg.png");
		this.xSize = 256;
		this.ySize = 100;
		this.closeOnEsc = true;
	}

	public void initGui() {
		super.initGui();

		int y = this.guiTop + 4;

		this.addButton(new GuiNpcButton(66, this.guiLeft + this.xSize - 24, y, 20, 20, "X"));

		y += 50;

		this.addTextField(new GuiNpcTextField(0, this, this.fontRenderer,
				this.guiLeft + 4, y, 226, 20, this.display.getName()));
	}

	public void unFocused(GuiNpcTextField textfield) {
		if (textfield.getId() == 0) {
			if (!textfield.isEmpty()) {
				this.display.setName(textfield.getText());
			} else {
				textfield.setText(this.display.getName());
			}
		}
	}

	protected void actionPerformed(GuiButton guibutton) {
		if (guibutton.id == 66) {
			this.close();
		}
	}

	public void save() {
		GuiNpcTextField textfield = this.getTextField(0);

		if (textfield != null && !textfield.isEmpty()) {
			this.display.setName(textfield.getText());
		}
	}
}
