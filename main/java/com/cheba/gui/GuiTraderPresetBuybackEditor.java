package com.cheba.gui;

import com.cheba.trader.NpcBuybackConfig;
import com.cheba.trader.TraderPreset;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class GuiTraderPresetBuybackEditor extends GuiScreen {

    private static final int PANEL_WIDTH = 300;
    private static final int PANEL_HEIGHT = 220;

    private static final int COLUMNS = 8;
    private static final int ROWS = 5;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 18;
    private static final int MAX_OFFERS = 128;

    private final GuiTraderPresetEditor parent;
    private final TraderPreset preset;

    private int selectedIndex = -1;
    private int page;

    private GuiTextField priceField;
    private String statusMessage = "";

    public GuiTraderPresetBuybackEditor(
            GuiTraderPresetEditor parent,
            TraderPreset preset
    ) {
        if (parent == null || preset == null) {
            throw new IllegalArgumentException(
                    "Parent and preset cannot be null"
            );
        }

        this.parent = parent;
        this.preset = preset;

        if (!preset.getBuybackOffers().isEmpty()) {
            selectedIndex = 0;
        }
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private List<NpcBuybackConfig> getOffers() {
        return preset.getBuybackOffers();
    }

    private int getPageCount() {
        return Math.max(
                1,
                (getOffers().size() + PAGE_SIZE - 1) / PAGE_SIZE
        );
    }

    private NpcBuybackConfig getSelectedOffer() {
        List<NpcBuybackConfig> offers = getOffers();

        if (selectedIndex < 0 || selectedIndex >= offers.size()) {
            return null;
        }

        return offers.get(selectedIndex);
    }

    private void refreshPriceField() {
        NpcBuybackConfig offer = getSelectedOffer();

        priceField.setText(
                offer == null
                        ? "0"
                        : String.valueOf(offer.getPriceMoney())
        );
    }

    private boolean savePriceField() {
        NpcBuybackConfig offer = getSelectedOffer();

        if (offer == null) {
            return true;
        }

        try {
            long price = Long.parseLong(
                    priceField.getText().trim()
            );

            if (price < 0L) {
                throw new NumberFormatException();
            }

            offer.setPriceMoney(price);
            return true;

        } catch (NumberFormatException exception) {
            statusMessage = "Invalid buyback price.";
            return false;
        }
    }

    private void selectFirstOfferOnPage() {
        int count = getOffers().size();

        if (count == 0) {
            selectedIndex = -1;
            page = 0;
            return;
        }

        selectedIndex = Math.min(
                page * PAGE_SIZE,
                count - 1
        );
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = left();
        int y = top();

        buttonList.add(new GuiButton(
                0, x + 165, y + 150, 125, 20,
                "Add from hand"
        ));

        buttonList.add(new GuiButton(
                1, x + 165, y + 173, 125, 20,
                "Remove selected"
        ));

        buttonList.add(new GuiButton(
                2, x + 10, y + 184, 55, 20,
                "<"
        ));

        buttonList.add(new GuiButton(
                3, x + 235, y + 184, 55, 20,
                ">"
        ));

        buttonList.add(new GuiButton(
                4, x + 70, y + 184, 160, 20,
                "Done"
        ));

        priceField = new GuiTextField(
                0,
                fontRenderer,
                x + 165,
                y + 70,
                125,
                18
        );

        priceField.setMaxStringLength(20);
        refreshPriceField();
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button)
            throws IOException {

        if (!savePriceField()) {
            return;
        }

        switch (button.id) {
            case 0: {
                if (getOffers().size() >= MAX_OFFERS) {
                    statusMessage = "Buyback list is full.";
                    return;
                }

                ItemStack held = mc.player.getHeldItemMainhand();

                if (held.isEmpty()) {
                    statusMessage = "Hold an item in your main hand.";
                    return;
                }

                for (NpcBuybackConfig existing : getOffers()) {
                    if (existing.matches(held)) {
                        statusMessage = "This item is already listed.";
                        return;
                    }
                }

                NpcBuybackConfig offer =
                        new NpcBuybackConfig(held, 0L);

                if (!preset.addBuybackOffer(offer)) {
                    statusMessage = "Could not add buyback item.";
                    return;
                }

                selectedIndex = getOffers().size() - 1;
                page = selectedIndex / PAGE_SIZE;

                statusMessage = "Buyback item added.";
                refreshPriceField();
                break;
            }

            case 1: {
                NpcBuybackConfig offer = getSelectedOffer();

                if (offer == null) {
                    return;
                }

                preset.removeBuybackOffer(offer.getId());

                if (getOffers().isEmpty()) {
                    selectedIndex = -1;
                    page = 0;
                } else {
                    selectedIndex = Math.min(
                            selectedIndex,
                            getOffers().size() - 1
                    );

                    page = Math.min(
                            page,
                            getPageCount() - 1
                    );
                }

                statusMessage = "Buyback item removed.";
                refreshPriceField();
                break;
            }

            case 2: {
                page = Math.max(0, page - 1);
                selectFirstOfferOnPage();
                refreshPriceField();
                break;
            }

            case 3: {
                page = Math.min(
                        getPageCount() - 1,
                        page + 1
                );

                selectFirstOfferOnPage();
                refreshPriceField();
                break;
            }

            case 4: {
                mc.displayGuiScreen(parent);
                break;
            }
        }
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        super.mouseClicked(mouseX, mouseY, mouseButton);
        priceField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        int gridX = left() + 10;
        int gridY = top() + 42;

        int relativeX = mouseX - gridX;
        int relativeY = mouseY - gridY;

        if (relativeX < 0 || relativeY < 0) {
            return;
        }

        int column = relativeX / SLOT_SIZE;
        int row = relativeY / SLOT_SIZE;

        if (column >= COLUMNS || row >= ROWS) {
            return;
        }

        int index = page * PAGE_SIZE
                + row * COLUMNS
                + column;

        if (index < 0 || index >= getOffers().size()) {
            return;
        }

        if (!savePriceField()) {
            return;
        }

        selectedIndex = index;
        statusMessage = "";
        refreshPriceField();
    }

    @Override
    protected void keyTyped(
            char typedChar,
            int keyCode
    ) throws IOException {

        if (keyCode == 1) {
            if (savePriceField()) {
                mc.displayGuiScreen(parent);
            }
            return;
        }

        if (priceField.textboxKeyTyped(typedChar, keyCode)) {
            statusMessage = "";
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        priceField.updateCursorCounter();
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        drawDefaultBackground();

        int x = left();
        int y = top();

        drawRect(
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                0xEE202020
        );

        fontRenderer.drawString(
                "Buyback items",
                x + 10,
                y + 10,
                0xFFFFFF
        );

        fontRenderer.drawString(
                "Items: " + getOffers().size() + " / " + MAX_OFFERS,
                x + 10,
                y + 25,
                0xAAAAAA
        );

        fontRenderer.drawString(
                "Page: " + (page + 1) + " / " + getPageCount(),
                x + 165,
                y + 25,
                0xAAAAAA
        );

        List<NpcBuybackConfig> offers = getOffers();

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int column = slot % COLUMNS;
            int row = slot / COLUMNS;

            int slotX = x + 10 + column * SLOT_SIZE;
            int slotY = y + 42 + row * SLOT_SIZE;

            int index = page * PAGE_SIZE + slot;

            drawRect(
                    slotX,
                    slotY,
                    slotX + SLOT_SIZE,
                    slotY + SLOT_SIZE,
                    index == selectedIndex
                            ? 0xFF777744
                            : 0xFF555555
            );

            drawRect(
                    slotX + 1,
                    slotY + 1,
                    slotX + 17,
                    slotY + 17,
                    0xFF222222
            );

            if (index >= offers.size()) {
                continue;
            }

            ItemStack stack = offers.get(index).getItem();

            if (stack.isEmpty()) {
                continue;
            }

            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    stack,
                    slotX + 1,
                    slotY + 1
            );

            itemRender.renderItemOverlays(
                    fontRenderer,
                    stack,
                    slotX + 1,
                    slotY + 1
            );

            RenderHelper.disableStandardItemLighting();
        }

        NpcBuybackConfig selected = getSelectedOffer();

        fontRenderer.drawString(
                "Price:",
                x + 165,
                y + 55,
                0xFFFFFF
        );

        priceField.drawTextBox();

        fontRenderer.drawString(
                "Selected item:",
                x + 165,
                y + 100,
                0xFFFFFF
        );

        if (selected != null) {
            ItemStack stack = selected.getItem();

            if (!stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();

                itemRender.renderItemAndEffectIntoGUI(
                        stack,
                        x + 165,
                        y + 115
                );

                RenderHelper.disableStandardItemLighting();

                fontRenderer.drawString(
                        stack.getDisplayName(),
                        x + 185,
                        y + 120,
                        0xAAAAAA
                );
            }
        }

        if (!statusMessage.isEmpty()) {
            fontRenderer.drawString(
                    statusMessage,
                    x + 10,
                    y + 140,
                    0xFFFF55
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (mouseX >= x + 10
                && mouseX < x + 10 + COLUMNS * SLOT_SIZE
                && mouseY >= y + 42
                && mouseY < y + 42 + ROWS * SLOT_SIZE) {

            int column = (mouseX - (x + 10)) / SLOT_SIZE;
            int row = (mouseY - (y + 42)) / SLOT_SIZE;

            int index = page * PAGE_SIZE
                    + row * COLUMNS
                    + column;

            if (index >= 0 && index < offers.size()) {
                ItemStack stack = offers.get(index).getItem();

                if (!stack.isEmpty()) {
                    drawHoveringText(
                            stack.getTooltip(
                                    mc.player,
                                    ITooltipFlag.TooltipFlags.ADVANCED
                            ),
                            mouseX,
                            mouseY
                    );
                }
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
