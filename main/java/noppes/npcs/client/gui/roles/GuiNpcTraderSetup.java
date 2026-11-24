package noppes.npcs.client.gui.roles;

import com.cheba.gui.GuiTraderBindingSetup;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface2;
import noppes.npcs.containers.ContainerNPCTraderSetup;
import noppes.npcs.entity.EntityNPCInterface;

/** The NPC trader role only owns a profile binding, never shop contents. */
public class GuiNpcTraderSetup extends GuiContainerNPCInterface2 {
    public GuiNpcTraderSetup(EntityNPCInterface npc, ContainerNPCTraderSetup container) {
        super(npc, container);
        this.ySize = 200;
        this.menuYOffset = 0;
    }

    @Override
    public void initGui() {
        super.initGui();
        setBackground("menubg.png");
        buttonList.add(new GuiButton(
                5, guiLeft + 137, guiTop + 65, 146, 20,
                "Настройки профиля торговца"
        ));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 5) {
            mc.displayGuiScreen(new GuiTraderBindingSetup(this));
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        drawCenteredString(fontRenderer, "Торговый профиль NPC",
                guiLeft + xSize / 2, guiTop + 35, 0xFFFFFF);
    }

    @Override
    public void save() {
        // Profile settings are saved by GuiTraderBindingSetup.
    }
}
