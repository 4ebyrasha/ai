package com.cheba.gui;

import com.cheba.container.TraderSellContainer;
import net.minecraft.client.renderer.GlStateManager;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.entity.EntityNPCInterface;

public class GuiDynamicTraderSell extends GuiContainerNPCInterface {

    private final TraderSellContainer container;

    public GuiDynamicTraderSell(
            EntityNPCInterface npc,
            TraderSellContainer container
    ) {
        super(npc, container);

        this.container = container;
        this.xSize = 230;
        this.ySize = 225;
        this.closeOnEsc = true;
        this.drawDefaultBackground = true;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
            float partialTicks,
            int mouseX,
            int mouseY
    ) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        // Основная панель.
        drawRect(
                guiLeft, guiTop,
                guiLeft + xSize, guiTop + ySize,
                0xEE202020
        );

        // Подписи панелей.
        drawCenteredString(
                fontRenderer, "Предметы для продажи",
                guiLeft + xSize / 2, guiTop - 12,
                0xFFFFFF
        );

        drawString(
                fontRenderer, "Буфер продажи",
                guiLeft + 23, guiTop - 1, 0xFFFFFF
        );

        // Фон 25 ячеек буфера.
        for (int i = 0; i < 25; i++) {
            int x = guiLeft + 23 + (i % 5) * 18;
            int y = guiTop + 10 + (i / 5) * 18;

            drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
            drawRect(x, y, x + 16, y + 16, 0xFF303030);
        }

        // Фон инвентаря игрока.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiLeft + 13 + col * 18,
                        guiTop + 122 + row * 18
                );
            }
        }

        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiLeft + 13 + col * 18,
                    guiTop + 180
            );
        }
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
        drawRect(x, y, x + 16, y + 16, 0xFF303030);
    }

    @Override
    public void save() {
        // Возврат предметов выполняет серверный TraderSellContainer.
    }
}
