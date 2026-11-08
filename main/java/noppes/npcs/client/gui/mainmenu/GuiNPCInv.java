
package noppes.npcs.client.gui.mainmenu;

import java.util.HashMap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcSlider;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.ISliderListener;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.containers.ContainerNPCInv;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;

@SuppressWarnings("deprecation")
public class GuiNPCInv extends GuiContainerNPCInterface2 implements ISliderListener, IGuiData {
	private static final int SLIDER_ID_OFFSET = 100;

	private final HashMap<Integer, Integer> chances = new HashMap<>();
	private final ContainerNPCInv container;
	private final ResourceLocation slot;

	public GuiNPCInv(EntityNPCInterface npc, ContainerNPCInv container) {
		super(npc, container, 3);
		this.setBackground("npcinv.png");
		this.container = container;
		this.xSize = 470;
		this.ySize = 200;
		this.slot = this.getResource("slot.png");
		Client.sendData(EnumPacketServer.MainmenuInvGet);
	}

	public void initGui() {
		super.initGui();

		this.addLabel(new GuiNpcLabel(0, "inv.minExp", this.guiLeft + 118, this.guiTop + 18));
		this.addTextField(new GuiNpcTextField(0, this, this.fontRenderer, this.guiLeft + 108, this.guiTop + 29, 60, 20,
				this.npc.inventory.getExpMin() + ""));
		this.getTextField(0).numbersOnly = true;
		this.getTextField(0).setMinMaxDefault(0, 32767, 0);

		this.addLabel(new GuiNpcLabel(1, "inv.maxExp", this.guiLeft + 118, this.guiTop + 52));
		this.addTextField(new GuiNpcTextField(1, this, this.fontRenderer, this.guiLeft + 108, this.guiTop + 63, 60, 20,
				this.npc.inventory.getExpMax() + ""));
		this.getTextField(1).numbersOnly = true;
		this.getTextField(1).setMinMaxDefault(0, 32767, 0);

		this.addButton(new GuiNpcButton(10, this.guiLeft + 88, this.guiTop + 88, 80, 20,
				new String[] { "stats.normal", "inv.auto" }, this.npc.inventory.lootMode));

		this.addLabel(new GuiNpcLabel(2, "inv.npcInventory", this.guiLeft + 191, this.guiTop + 5));
		this.addLabel(new GuiNpcLabel(3, "inv.inventory", this.guiLeft + 8, this.guiTop + 101));

		this.chances.clear();

		int rowsPerColumn = DataInventory.DROP_SLOT_COUNT / 2;

		for (int i = 0; i < DataInventory.DROP_SLOT_COUNT; ++i) {
			int column = i / rowsPerColumn;
			int row = i % rowsPerColumn;

			int chance = 100;

			if (this.npc.inventory.dropchance.containsKey(i)) {
				chance = (Integer) this.npc.inventory.dropchance.get(i);
			}

			if (chance <= 0 || chance > 100) {
				chance = 100;
			}

			this.chances.put(i, chance);

			int sliderX = this.guiLeft + (column == 0 ? 211 : 351);

			GuiNpcSlider slider = new GuiNpcSlider(this, SLIDER_ID_OFFSET + i, sliderX,
					this.guiTop + 14 + row * 21, 110, 20, (float) chance / 100.0F);

			this.addSlider(slider);
		}
	}

	protected void actionPerformed(GuiButton guibutton) {
		if (guibutton.id == 10) {
			this.npc.inventory.lootMode = ((GuiNpcButton) guibutton).getValue();
		}
	}

	protected void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		super.drawGuiContainerBackgroundLayer(f, i, j);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(this.slot);

		for (int id = 4; id <= 6; ++id) {
			Slot slot = this.container.getSlot(id);

			if (slot.getHasStack()) {
				this.drawTexturedModalRect(this.guiLeft + slot.xPos - 1, this.guiTop + slot.yPos - 1, 0, 0, 18, 18);
			}
		}
	}

	public void drawScreen(int i, int j, float f) {
		int showname = this.npc.display.getShowName();
		this.npc.display.setShowName(1);
		this.drawNpc(50, 84);
		this.npc.display.setShowName(showname);
		super.drawScreen(i, j, f);
	}

	public void save() {
		this.npc.inventory.dropchance = new HashMap<>(this.chances);
		this.npc.inventory.setExp(this.getTextField(0).getInteger(), this.getTextField(1).getInteger());

		Client.sendData(EnumPacketServer.MainmenuInvSave,
				this.npc.inventory.writeEntityToNBT(new NBTTagCompound()));
	}

	public void setGuiData(NBTTagCompound compound) {
		this.npc.inventory.readEntityFromNBT(compound);
		this.initGui();
	}

	public void mouseDragged(GuiNpcSlider guiNpcSlider) {
		guiNpcSlider.displayString = I18n.translateToLocal("inv.dropChance") + ": "
				+ (int) (guiNpcSlider.sliderValue * 100.0F) + "%";
	}

	public void mousePressed(GuiNpcSlider guiNpcSlider) {
	}

	public void mouseReleased(GuiNpcSlider guiNpcSlider) {
		int dropSlot = guiNpcSlider.id - SLIDER_ID_OFFSET;

		if (dropSlot >= 0 && dropSlot < DataInventory.DROP_SLOT_COUNT) {
			this.chances.put(dropSlot, (int) (guiNpcSlider.sliderValue * 100.0F));
		}
	}
}
