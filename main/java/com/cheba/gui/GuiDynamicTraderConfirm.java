package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

import java.io.IOException;
import java.util.List;

public class GuiDynamicTraderConfirm extends GuiScreen {

    private final int npcEntityId;
    private final int page;
    private final NpcTradeConfig offer;

    public GuiDynamicTraderConfirm(
            int npcEntityId,
            int page,
            NpcTradeConfig offer
    ) {
        this.npcEntityId = npcEntityId;
        this.page = page;
        this.offer = offer;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = width / 2 - 105;
        int y = height / 2 + 45;

        buttonList.add(new GuiButton(
                0, x, y, 100, 20, "Купить"
        ));

        buttonList.add(new GuiButton(
                1, x + 110, y, 100, 20, "Отмена"
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            Client.sendData(
                    EnumPacketServer.TraderBuy,
                    npcEntityId,
                    offer.getId()
            );
        }

        mc.displayGuiScreen(
                new GuiDynamicTraderBuy(npcEntityId, page)
        );
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(
                fontRenderer,
                "Подтверждение покупки",
                width / 2, height / 2 - 55, 0xFFFFFF
        );

        ItemStack result = offer.getResult();

        if (result != null && !result.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();
            itemRender.renderItemAndEffectIntoGUI(
                    result, width / 2 - 8, height / 2 - 28
            );
            itemRender.renderItemOverlays(
                    fontRenderer, result, width / 2 - 8, height / 2 - 28
            );
            RenderHelper.disableStandardItemLighting();

            drawCenteredString(
                    fontRenderer, result.getDisplayName(),
                    width / 2, height / 2 - 8, 0xFFFFFF
            );
        }

        int y = height / 2 + 3;

        if (offer.getPriceMoney() > 0) {
            drawCenteredString(
                    fontRenderer,
                    "Цена: " + offer.getPriceMoney() + " монет",
                    width / 2, y, 0xFFD700
            );
            y += 12;
        }

        List<ItemStack> priceItems = offer.getPriceItems();

        for (ItemStack item : priceItems) {
            if (item != null && !item.isEmpty()) {
                drawCenteredString(
                        fontRenderer,
                        item.getDisplayName() + " x" + item.getCount(),
                        width / 2, y, 0xFFFFFF
                );
                y += 12;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
