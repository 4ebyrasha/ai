package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;
import com.cheba.trader.TraderPreset;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

import java.io.IOException;
import java.util.List;

/**
 * Экран подтверждения покупки товара из независимого каталога.
 */
@SuppressWarnings("deprecation")
public class GuiDynamicTraderConfirm extends GuiScreen {

    private static final int MAX_MULTIPLIER = 64;

    private final TraderClientSession session;
    private final String profileName;
    private final TraderPreset preset;
    private final int page;
    private final NpcTradeConfig offer;

    private GuiTextField quantityField;

    public GuiDynamicTraderConfirm(
            TraderClientSession session,
            String profileName,
            TraderPreset preset,
            int page,
            NpcTradeConfig offer
    ) {
        this.session = session;
        this.profileName = profileName;
        this.preset = preset;
        this.page = page;
        this.offer = offer;
    }

    private int getMultiplier() {
        if (quantityField == null) {
            return 1;
        }

        try {
            int value = Integer.parseInt(
                    quantityField.getText().trim()
            );

            return Math.max(
                    1,
                    Math.min(MAX_MULTIPLIER, value)
            );

        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    /**
     * Возвращает игрока к каталогу, сохраняя текущую сессию.
     */
    private void returnToCatalog() {
        session.beginTransition();

        mc.displayGuiScreen(
                new GuiDynamicTraderBuy(
                        session,
                        profileName,
                        preset,
                        page
                )
        );
    }

    @Override
    public void initGui() {
        session.completeTransition();
        buttonList.clear();

        quantityField = new GuiTextField(
                0,
                fontRenderer,
                width / 2 - 20,
                height / 2 + 25,
                60,
                18
        );

        quantityField.setMaxStringLength(2);
        quantityField.setValidator(
                value -> value.matches("\\d{0,2}")
        );
        quantityField.setText("1");
        quantityField.setFocused(true);

        int buttonY = height / 2 + 55;

        buttonList.add(new GuiButton(
                0,
                width / 2 - 105,
                buttonY,
                100,
                20,
                I18n.translateToLocal("cheba.trader.buy")
        ));

        buttonList.add(new GuiButton(
                1,
                width / 2 + 5,
                buttonY,
                100,
                20,
                I18n.translateToLocal("cheba.trader.cancel")
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {

        if (button.id == 0) {
            Client.sendData(
                    EnumPacketServer.TraderSessionBuy,
                    session.getToken(),
                    offer.getId(),
                    getMultiplier()
            );
        }

        returnToCatalog();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
            throws IOException {

        if (keyCode == 1) {
            returnToCatalog();
            return;
        }

        if (quantityField != null) {
            quantityField.textboxKeyTyped(
                    typedChar,
                    keyCode
            );
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        if (quantityField != null) {
            quantityField.mouseClicked(
                    mouseX,
                    mouseY,
                    mouseButton
            );
        }

        super.mouseClicked(
                mouseX,
                mouseY,
                mouseButton
        );
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        drawDefaultBackground();

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.confirm.title"),
                width / 2,
                height / 2 - 85,
                0xFFFFFF
        );

        ItemStack result = offer.getResult();

        if (!result.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    result,
                    width / 2 - 8,
                    height / 2 - 58
            );

            itemRender.renderItemOverlays(
                    fontRenderer,
                    result,
                    width / 2 - 8,
                    height / 2 - 58
            );

            RenderHelper.disableStandardItemLighting();

            drawCenteredString(
                    fontRenderer,
                    result.getDisplayName(),
                    width / 2,
                    height / 2 - 36,
                    0xFFFFFF
            );
        }

        int multiplier = getMultiplier();

        if (offer.getPriceMoney() > 0L) {
            long totalPrice;

            try {
                totalPrice = Math.multiplyExact(
                        offer.getPriceMoney(),
                        (long) multiplier
                );
            } catch (ArithmeticException ignored) {
                totalPrice = Long.MAX_VALUE;
            }

            drawCenteredString(
                    fontRenderer,
                    I18n.translateToLocalFormatted(
                            "cheba.trader.price.total",
                            totalPrice
                    ),
                    width / 2,
                    height / 2 - 15,
                    0xFFD700
            );
        }

        List<ItemStack> priceItems = offer.getPriceItems();

        int y = height / 2 + 2;
        int visible = Math.min(4, priceItems.size());

        for (int i = 0; i < visible; i++) {
            ItemStack item = priceItems.get(i);

            if (item == null || item.isEmpty()) {
                continue;
            }

            long requiredCount = (long) item.getCount()
                    * multiplier;

            drawCenteredString(
                    fontRenderer,
                    item.getDisplayName() + " x" + requiredCount,
                    width / 2,
                    y,
                    0xFFFFFF
            );

            y += 12;
        }

        if (priceItems.size() > visible) {
            drawCenteredString(
                    fontRenderer,
                    I18n.translateToLocalFormatted(
                            "cheba.trader.confirm.more_payment_items",
                            priceItems.size() - visible
                    ),
                    width / 2,
                    y,
                    0xAAAAAA
            );
        }

        drawString(
                fontRenderer,
                I18n.translateToLocal(
                        "cheba.trader.quantity.multiplier"
                ),
                width / 2 - 100,
                height / 2 + 30,
                0xFFFFFF
        );

        quantityField.drawTextBox();

        long totalItems = result.isEmpty()
                ? 0L
                : (long) result.getCount() * multiplier;

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocalFormatted(
                        "cheba.trader.quantity.total",
                        totalItems
                ),
                width / 2,
                height / 2 + 45,
                0xAAAAAA
        );

        super.drawScreen(
                mouseX,
                mouseY,
                partialTicks
        );
    }

    @Override
    public void onGuiClosed() {
        session.onScreenClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
