
package noppes.npcs.client.gui.roles;

import com.cheba.gui.GuiTraderPresetEditor;
import com.cheba.gui.GuiTraderBindingSetup;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.containers.ContainerNPCTraderSetup;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

public class GuiNpcTraderSetup extends GuiContainerNPCInterface2 {

	private final ResourceLocation slot =
			new ResourceLocation("customnpcs", "textures/gui/slot.png");

	private final EntityNPCInterface traderNpc;

	private RoleTrader role;

	public GuiNpcTraderSetup(
			EntityNPCInterface npc,
			ContainerNPCTraderSetup container
	) {
		super(npc, container);

		this.traderNpc = npc;
		this.ySize = 220;
		this.menuYOffset = 10;
		this.role = container.role;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.buttonList.clear();
		this.setBackground("tradersetup.png");

		this.addLabel(new GuiNpcLabel(
				1,
				"gui.ignoreDamage",
				this.guiLeft + 260,
				this.guiTop + 29
		));

		this.buttonList.add(new GuiButton(1, this.guiLeft + 340, this.guiTop + 24, 55, 20, this.role.ignoreDamage ? "Да" : "Нет"));

		this.addLabel(new GuiNpcLabel(
				2,
				"gui.ignoreNBT",
				this.guiLeft + 260,
				this.guiTop + 51
		));

		this.buttonList.add(new GuiButton(2, this.guiLeft + 340, this.guiTop + 46, 55, 20, this.role.ignoreNBT ? "Да" : "Нет"));

		this.buttonList.add(new GuiButton(
				3,
				this.guiLeft + 260,
				this.guiTop + 75,
				135,
				20,
				I18n.translateToLocal("cheba.trader.editor.open")
		));

		this.buttonList.add(new GuiButton(
				5,
				this.guiLeft + 260,
				this.guiTop + 99,
				135,
				20,
				"Имя магазина"
		));
	}

	@Override
	public void drawScreen(int i, int j, float f) {
		this.guiTop += 10;
		super.drawScreen(i, j, f);
		this.guiTop -= 10;
	}

	@Override
	public void actionPerformed(GuiButton button) {
		if (button.id == 1) {
			this.role.ignoreDamage = !this.role.ignoreDamage;
			button.displayString = this.role.ignoreDamage ? "Да" : "Нет";
		}

		if (button.id == 2) {
			this.role.ignoreNBT = !this.role.ignoreNBT;
			button.displayString = this.role.ignoreNBT ? "Да" : "Нет";
		}

		if (button.id == 3) {
			Client.sendData(EnumPacketServer.TraderPresetEditorFetch);
		}
		if (button.id == 5) {
			this.mc.displayGuiScreen(new GuiTraderBindingSetup(this));
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(
			float f,
			int i,
			int j
	) {
		super.drawGuiContainerBackgroundLayer(f, i, j);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		for (int slot = 0; slot < 18; ++slot) {
			int x = this.guiLeft + slot % 3 * 94 + 7;
			int y = this.guiTop + slot / 3 * 22 + 4;

			this.mc.renderEngine.bindTexture(this.slot);

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			this.drawTexturedModalRect(x - 1, y, 0, 0, 18, 18);
			this.drawTexturedModalRect(x + 17, y, 0, 0, 18, 18);

			this.fontRenderer.drawString(
					"=",
					x + 36,
					y + 5,
					CustomNpcResourceListener.DefaultTextColor
			);

			this.mc.renderEngine.bindTexture(this.slot);

			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

			this.drawTexturedModalRect(x + 42, y, 0, 0, 18, 18);
		}
	}

	@Override
	public void save() {
		Client.sendData(
				EnumPacketServer.RoleSave,
				this.role.writeToNBT(new NBTTagCompound())
		);
	}
}
