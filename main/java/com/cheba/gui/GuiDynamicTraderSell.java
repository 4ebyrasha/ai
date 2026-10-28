package com.cheba.gui;

import com.cheba.container.TraderSellContainer;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumPacketServer;
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
    public void initGui() {
        super.initGui();

        buttonList.add(new GuiButton(
                0,
                guiLeft + 12,
                guiTop + ySize - 23,
                96,
                20,
                I18n.translateToLocal("cheba.trader.menu")
        ));

        buttonList.add(new GuiButton(
                1,
                guiLeft + 122,
                guiTop + ySize - 23,
                96,
                20,
                I18n.translateToLocal("cheba.trader.sell.execute")
        ));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 0 && npc != null) {
            int npcEntityId = npc.getEntityId();

            // При закрытии контейнера сервер вернет предметы
            // из буфера продажи в инвентарь игрока
            player.closeScreen();

            mc.displayGuiScreen(
                    new GuiDynamicTraderMenu(npcEntityId)
            );
            return;
        }

        if (button.id == 1 && npc != null) {
            Client.sendData(
                    EnumPacketServer.TraderSell,
                    npc.getEntityId()
            );
            return;
        }

        super.actionPerformed(button);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
            float partialTicks,
            int mouseX,
            int mouseY
    ) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawRect(
                guiLeft, guiTop,
                guiLeft + xSize, guiTop + ySize,
                0xEE202020
        );

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.sell.title"),
                guiLeft + xSize / 2,
                guiTop - 12,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.sell.buffer"),
                guiLeft + 23,
                guiTop - 1,
                0xFFFFFF
        );

        for (int i = 0; i < 25; i++) {
            int x = guiLeft + 23 + (i % 5) * 18;
            int y = guiTop + 10 + (i / 5) * 18;

            drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
            drawRect(x, y, x + 16, y + 16, 0xFF303030);
        }

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
        // Предметы возвращает серверный контейнер
    }
}
