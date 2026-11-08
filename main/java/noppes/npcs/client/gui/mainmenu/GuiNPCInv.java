
package noppes.npcs.client.gui.mainmenu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import noppes.npcs.client.Client;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.IGuiData;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.containers.ContainerNPCInv;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;

public class GuiNPCInv extends GuiContainerNPCInterface2 implements IGuiData, ITextfieldListener {
	private static final int CHANCE_FIELD_ID_OFFSET = 100;

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
		this.addTextField(new GuiNpcTextField(0, this, this.fontRenderer,
				this.guiLeft + 108, this.guiTop + 29, 60, 20,
				this.npc.inventory.getExpMin() + ""));

		this.getTextField(0).numbersOnly = true;
		this.getTextField(0).setMinMaxDefault(0, 32767, 0);

		this.addLabel(new GuiNpcLabel(1, "inv.maxExp", this.guiLeft + 118, this.guiTop + 52));
		this.addTextField(new GuiNpcTextField(1, this, this.fontRenderer,
				this.guiLeft + 108, this.guiTop + 63, 60, 20,
				this.npc.inventory.getExpMax() + ""));

		this.getTextField(1).numbersOnly = true;
		this.getTextField(1).setMinMaxDefault(0, 32767, 0);

		this.addButton(new GuiNpcButton(10, this.guiLeft + 88, this.guiTop + 88, 80, 20,
				new String[] { "stats.normal", "inv.auto" }, this.npc.inventory.lootMode));

		this.addLabel(new GuiNpcLabel(2, "inv.npcInventory", this.guiLeft + 191, this.guiTop + 5));
		this.addLabel(new GuiNpcLabel(3, "inv.inventory", this.guiLeft + 8, this.guiTop + 101));

		this.chances.clear();

		int rowsPerColumn = DataInventory.DROP_SLOT_COUNT / 2;

		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			int column = group / rowsPerColumn;
			int row = group % rowsPerColumn;

			int chance = (int) Math.round(
					this.npc.inventory.getDropChance(group) * DataInventory.DROP_CHANCE_SCALE);

			chance = Math.max(0, Math.min(DataInventory.MAX_DROP_CHANCE, chance));
			this.chances.put(group, chance);

			int fieldX = this.guiLeft + (column == 0 ? 282 : 422);
			int fieldY = this.guiTop + 14 + row * 21;

			GuiNpcTextField chanceField = new GuiNpcTextField(
					CHANCE_FIELD_ID_OFFSET + group,
					this,
					this.fontRenderer,
					fieldX,
					fieldY,
					40,
					20,
					this.formatChance(chance));

			chanceField.decimalNumbers = true;
			chanceField.setMaxStringLength(7);

			this.addTextField(chanceField);
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
			Slot equipmentSlot = this.container.getSlot(id);

			if (equipmentSlot.getHasStack()) {
				this.drawTexturedModalRect(
						this.guiLeft + equipmentSlot.xPos - 1,
						this.guiTop + equipmentSlot.yPos - 1,
						0, 0, 18, 18);
			}
		}

		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			for (int itemSlot = 0; itemSlot < DataInventory.DROP_ITEMS_PER_SLOT; ++itemSlot) {
				int containerSlot = 7 + group * DataInventory.DROP_ITEMS_PER_SLOT + itemSlot;
				Slot dropSlot = this.container.getSlot(containerSlot);

				this.drawTexturedModalRect(
						this.guiLeft + dropSlot.xPos - 1,
						this.guiTop + dropSlot.yPos - 1,
						0, 0, 18, 18);
			}
		}

		for (int row = 0; row < DataInventory.DROP_SLOT_COUNT / 2; ++row) {
			int y = this.guiTop + 19 + row * 21;

			this.fontRenderer.drawString("%", this.guiLeft + 324, y,
					CustomNpcResourceListener.DefaultTextColor);

			this.fontRenderer.drawString("%", this.guiLeft + 464, y,
					CustomNpcResourceListener.DefaultTextColor);
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
		this.saveChanceFields();

		this.npc.inventory.setExp(
				this.getTextField(0).getInteger(),
				this.getTextField(1).getInteger());

		Client.sendData(EnumPacketServer.MainmenuInvSave,
				this.npc.inventory.writeEntityToNBT(new NBTTagCompound()));
	}

	public void setGuiData(NBTTagCompound compound) {
		this.npc.inventory.readEntityFromNBT(compound);
		this.initGui();
	}

	public void unFocused(GuiNpcTextField textField) {
		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			if (this.getTextField(CHANCE_FIELD_ID_OFFSET + group) == textField) {
				this.updateChanceField(group, textField);
				return;
			}
		}
	}

	private void saveChanceFields() {
		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			GuiNpcTextField field = this.getTextField(CHANCE_FIELD_ID_OFFSET + group);

			if (field != null) {
				this.updateChanceField(group, field);
			}
		}

		for (int group = 0; group < DataInventory.DROP_SLOT_COUNT; ++group) {
			int chance = this.chances.getOrDefault(group, DataInventory.MAX_DROP_CHANCE);

			this.npc.inventory.setDropChance(
					group,
					chance / (double) DataInventory.DROP_CHANCE_SCALE);
		}
	}

	private void updateChanceField(int group, GuiNpcTextField field) {
		int previousChance = this.chances.getOrDefault(
				group, DataInventory.MAX_DROP_CHANCE);

		String text = field.getText().trim().replace(',', '.');
		int chance = previousChance;

		try {
			BigDecimal percentage = new BigDecimal(text);

			if (percentage.compareTo(BigDecimal.ZERO) < 0
					|| percentage.compareTo(BigDecimal.valueOf(100)) > 0) {
				throw new NumberFormatException("Chance must be between 0 and 100");
			}

			chance = percentage
					.setScale(3, RoundingMode.HALF_UP)
					.movePointRight(3)
					.intValueExact();
		} catch (NumberFormatException | ArithmeticException exception) {
			chance = previousChance;
		}

		chance = Math.max(0, Math.min(DataInventory.MAX_DROP_CHANCE, chance));

		this.chances.put(group, chance);
		field.setText(this.formatChance(chance));
	}

	private String formatChance(int chance) {
		return BigDecimal.valueOf(chance, 3)
				.stripTrailingZeros()
				.toPlainString();
	}
}
