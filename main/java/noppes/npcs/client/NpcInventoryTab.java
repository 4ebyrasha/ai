package noppes.npcs.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class NpcInventoryTab extends GuiButton {

    public static final int FACTIONS_BUTTON_ID = -31001;
    public static final int QUESTS_BUTTON_ID = -31002;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation("textures/gui/container/creative_inventory/tabs.png");

    private final ItemStack icon;

    public NpcInventoryTab(int id, int x, int y, ItemStack icon) {
        super(id, x, y, 28, 32, "");
        this.icon = icon;
    }

    public static NpcInventoryTab createFactions(int x, int y) {
        return new NpcInventoryTab(
                FACTIONS_BUTTON_ID,
                x,
                y,
                new ItemStack(Items.BANNER, 1, 1)
        );
    }

    public static NpcInventoryTab createQuests(int x, int y) {
        return new NpcInventoryTab(
                QUESTS_BUTTON_ID,
                x,
                y,
                new ItemStack(Items.BOOK)
        );
    }

    @Override
    public void drawButton(
            Minecraft mc,
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x
                && mouseY >= this.y
                && mouseX < this.x + this.width
                && mouseY < this.y + this.height;

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int yTexPos = this.enabled ? 3 : 32;
        int ySize = this.enabled ? 25 : 32;
        int yPos = this.y + (this.enabled ? 3 : 0);

        mc.getTextureManager().bindTexture(TEXTURE);

        this.drawTexturedModalRect(
                this.x,
                yPos,
                28,
                yTexPos,
                28,
                ySize
        );

        RenderItem itemRenderer = mc.getRenderItem();

        RenderHelper.enableGUIStandardItemLighting();

        this.zLevel = 100.0F;
        itemRenderer.zLevel = 100.0F;

        GlStateManager.enableLighting();
        GlStateManager.enableRescaleNormal();

        itemRenderer.renderItemAndEffectIntoGUI(
                this.icon,
                this.x + 6,
                this.y + 8
        );

        itemRenderer.renderItemOverlayIntoGUI(
                mc.fontRenderer,
                this.icon,
                this.x + 6,
                this.y + 8,
                null
        );

        GlStateManager.disableLighting();
        GlStateManager.disableRescaleNormal();

        itemRenderer.zLevel = 0.0F;
        this.zLevel = 0.0F;

        RenderHelper.disableStandardItemLighting();
    }
}
