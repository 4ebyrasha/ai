package com.cheba.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;
import net.minecraft.util.text.translation.I18n;

import java.io.IOException;

public class GuiDynamicTraderMenu extends GuiScreen {

    private final int npcEntityId;

    public GuiDynamicTraderMenu(int npcEntityId) {
        this.npcEntityId = npcEntityId;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int buttonWidth = 160;
        int buttonHeight = 20;
        int x = (width - buttonWidth) / 2;
        int y = height / 2 - 12;

        buttonList.add(new GuiButton(
                0, x, y, buttonWidth, buttonHeight, I18n.translateToLocal("cheba.trader.buy")
        ));

        buttonList.add(new GuiButton(
                1, x, y + 26, buttonWidth, buttonHeight, I18n.translateToLocal("cheba.trader.sell")
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            mc.displayGuiScreen(new GuiDynamicTraderBuy(npcEntityId, 0));
        } else if (button.id == 1) {
            // Просим сервер открыть контейнер продажи.
            mc.displayGuiScreen(null);
            Client.sendData(
                    EnumPacketServer.TraderOpenSell,
                    npcEntityId
            );
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.title"),
                width / 2,
                height / 2 - 55,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
