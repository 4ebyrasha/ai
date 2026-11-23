package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;
import com.cheba.trader.TraderPreset;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

public class GuiTraderPresetEditor extends GuiScreen {

    private static final int PANEL_WIDTH = 390;
    private static final int PANEL_HEIGHT = 270;

    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 6;
    private static final int SLOT_SIZE = 18;

    private final TraderPreset preset;

    private String selectedOfferId;
    private String statusMessage = "";

    private GuiTextField priceField;
    private GuiTextField sellPriceField;
    private GuiTextField maxPerDayField;
    private GuiTextField dailyStockField;

    public GuiTraderPresetEditor(TraderPreset preset) {
        if (preset == null) {
            throw new IllegalArgumentException("Preset cannot be null");
        }

        this.preset = TraderPreset.readFromNBT(
                preset.writeToNBT()
        );

        normalizeDisplaySlots();

        if (!this.preset.getOffers().isEmpty()) {
            selectedOfferId = this.preset.getOffers().get(0).getId();
        }
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private void normalizeDisplaySlots() {
        boolean[] occupied = new boolean[GRID_COLUMNS * GRID_ROWS];

        for (NpcTradeConfig offer : preset.getOffers()) {
            int slot = offer.getDisplaySlot();

            if (slot >= 0 && slot < occupied.length
                    && !occupied[slot]) {
                occupied[slot] = true;
            } else {
                offer.setDisplaySlot(-1);
            }
        }

        for (NpcTradeConfig offer : preset.getOffers()) {
            if (offer.getDisplaySlot() >= 0) {
                continue;
            }

            int freeSlot = findFreeSlot(occupied);

            if (freeSlot >= 0) {
                offer.setDisplaySlot(freeSlot);
                occupied[freeSlot] = true;
            }
        }
    }

    private int findFreeSlot(boolean[] occupied) {
        for (int i = 0; i < occupied.length; i++) {
            if (!occupied[i]) {
                return i;
            }
        }

        return -1;
    }

    private NpcTradeConfig getSelectedOffer() {
        if (selectedOfferId == null) {
            return null;
        }

        return preset.getOffer(selectedOfferId);
    }

    private NpcTradeConfig getOfferAtSlot(int slot) {
        for (NpcTradeConfig offer : preset.getOffers()) {
            if (offer.getDisplaySlot() == slot) {
                return offer;
            }
        }

        return null;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = left();
        int y = top();

        buttonList.add(new GuiButton(
                0, x + 190, y + 226, 58, 20, "Add"
        ));

        buttonList.add(new GuiButton(
                1, x + 250, y + 226, 58, 20, "Remove"
        ));

        buttonList.add(new GuiButton(
                2, x + 310, y + 226, 70, 20, "Save"
        ));

        buttonList.add(new GuiButton(
                3, x + 310, y + 248, 70, 20, "Cancel"
        ));

        buttonList.add(new GuiButton(
                4, x + 190, y + 174, 92, 20, "Set from hand"
        ));

        buttonList.add(new GuiButton(
                5, x + 284, y + 174, 96, 20, "Clear product"
        ));

        priceField = createField(0, x + 275, y + 55);
        sellPriceField = createField(1, x + 275, y + 78);
        maxPerDayField = createField(2, x + 275, y + 101);
        dailyStockField = createField(3, x + 275, y + 124);

        refreshFields();
    }

    private GuiTextField createField(int id, int x, int y) {
        GuiTextField field = new GuiTextField(
                id,
                fontRenderer,
                x,
                y,
                105,
                18
        );

        field.setMaxStringLength(20);
        return field;
    }

    private void refreshFields() {
        NpcTradeConfig offer = getSelectedOffer();

        if (offer == null) {
            priceField.setText("0");
            sellPriceField.setText("0");
            maxPerDayField.setText("-1");
            dailyStockField.setText("-1");
            return;
        }

        priceField.setText(
                String.valueOf(offer.getPriceMoney())
        );

        sellPriceField.setText(
                String.valueOf(offer.getSellPriceMoney())
        );

        maxPerDayField.setText(
                String.valueOf(offer.getMaxPerDay())
        );

        dailyStockField.setText(
                String.valueOf(offer.getDailyStock())
        );
    }

    private boolean saveFields() {
        NpcTradeConfig offer = getSelectedOffer();

        if (offer == null) {
            return true;
        }

        try {
            long price = Long.parseLong(priceField.getText().trim());
            long sellPrice = Long.parseLong(
                    sellPriceField.getText().trim()
            );

            int maxPerDay = Integer.parseInt(
                    maxPerDayField.getText().trim()
            );

            int dailyStock = Integer.parseInt(
                    dailyStockField.getText().trim()
            );

            if (price < 0L || sellPrice < 0L
                    || maxPerDay < -1 || dailyStock < -1) {
                throw new NumberFormatException();
            }

            offer.setPriceMoney(price);
            offer.setSellPriceMoney(sellPrice);
            offer.setMaxPerDay(maxPerDay);
            offer.setDailyStock(dailyStock);

            return true;

        } catch (NumberFormatException exception) {
            statusMessage = "Invalid price or limit value.";
            return false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {

        if (button.id != 3 && !saveFields()) {
            return;
        }

        switch (button.id) {
            case 0: {
                if (preset.getOffers().size() >= 54) {
                    statusMessage = "Catalog is full.";
                    return;
                }

                boolean[] occupied = new boolean[54];

                for (NpcTradeConfig offer : preset.getOffers()) {
                    int slot = offer.getDisplaySlot();

                    if (slot >= 0 && slot < occupied.length) {
                        occupied[slot] = true;
                    }
                }

                int freeSlot = findFreeSlot(occupied);

                if (freeSlot < 0) {
                    statusMessage = "No free catalog slot.";
                    return;
                }

                NpcTradeConfig offer = new NpcTradeConfig();
                offer.setDisplaySlot(freeSlot);

                preset.addOffer(offer);
                selectedOfferId = offer.getId();

                statusMessage = "Offer created.";
                refreshFields();
                break;
            }

            case 1: {
                NpcTradeConfig offer = getSelectedOffer();

                if (offer == null) {
                    return;
                }

                preset.removeOffer(offer.getId());

                selectedOfferId = preset.getOffers().isEmpty()
                        ? null
                        : preset.getOffers().get(0).getId();

                refreshFields();
                statusMessage = "Offer removed.";
                break;
            }

            case 2: {
                savePreset();
                break;
            }

            case 3: {
                mc.displayGuiScreen(null);
                break;
            }

            case 4: {
                NpcTradeConfig offer = getSelectedOffer();

                if (offer == null) {
                    return;
                }

                ItemStack held = mc.player.getHeldItemMainhand();

                if (held.isEmpty()) {
                    statusMessage = "Hold an item in your main hand.";
                    return;
                }

                offer.setResult(held);
                statusMessage = "Product updated.";
                break;
            }

            case 5: {
                NpcTradeConfig offer = getSelectedOffer();

                if (offer != null) {
                    offer.setResult(ItemStack.EMPTY);
                    statusMessage = "Product cleared.";
                }
                break;
            }
        }
    }

    private void savePreset() {
        for (NpcTradeConfig offer : preset.getOffers()) {
            if (offer.getResult().isEmpty()
                    || (offer.getPriceMoney() <= 0L
                    && offer.getPriceItemCount() == 0)) {
                statusMessage =
                        "Every offer needs a product and a payment.";
                return;
            }
        }

        NBTTagCompound data = new NBTTagCompound();

        data.setLong(
                "ExpectedRevision",
                preset.getRevision()
        );

        data.setTag(
                "Preset",
                preset.writeToNBT()
        );

        Client.sendData(
                EnumPacketServer.TraderPresetEditorSave,
                data
        );

        statusMessage = "Save request sent.";
        mc.displayGuiScreen(null);
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        super.mouseClicked(mouseX, mouseY, mouseButton);

        priceField.mouseClicked(mouseX, mouseY, mouseButton);
        sellPriceField.mouseClicked(mouseX, mouseY, mouseButton);
        maxPerDayField.mouseClicked(mouseX, mouseY, mouseButton);
        dailyStockField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        int gridX = left() + 12;
        int gridY = top() + 43;

        int column = (mouseX - gridX) / SLOT_SIZE;
        int row = (mouseY - gridY) / SLOT_SIZE;

        if (mouseX < gridX || mouseY < gridY
                || column < 0 || column >= GRID_COLUMNS
                || row < 0 || row >= GRID_ROWS) {
            return;
        }

        int slot = row * GRID_COLUMNS + column;
        NpcTradeConfig offer = getOfferAtSlot(slot);

        if (offer != null) {
            if (!saveFields()) {
                return;
            }

            selectedOfferId = offer.getId();
            refreshFields();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
            throws IOException {

        if (priceField.textboxKeyTyped(typedChar, keyCode)
                || sellPriceField.textboxKeyTyped(typedChar, keyCode)
                || maxPerDayField.textboxKeyTyped(typedChar, keyCode)
                || dailyStockField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        priceField.updateCursorCounter();
        sellPriceField.updateCursorCounter();
        maxPerDayField.updateCursorCounter();
        dailyStockField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int x = left();
        int y = top();

        drawRect(
                x, y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                0xDD202020
        );

        fontRenderer.drawString(
                "Trader catalog: " + preset.getName(),
                x + 10,
                y + 10,
                0xFFFFFF
        );

        fontRenderer.drawString(
                "Revision: " + preset.getRevision(),
                x + 10,
                y + 25,
                0xAAAAAA
        );

        drawGrid(x + 12, y + 43, mouseX, mouseY);

        fontRenderer.drawString("Buy price:", x + 190, y + 59, 0xFFFFFF);
        fontRenderer.drawString("Sell price:", x + 190, y + 82, 0xFFFFFF);
        fontRenderer.drawString("Max per day:", x + 190, y + 105, 0xFFFFFF);
        fontRenderer.drawString("Daily stock:", x + 190, y + 128, 0xFFFFFF);

        NpcTradeConfig selected = getSelectedOffer();

        fontRenderer.drawString(
                selected == null
                        ? "No offer selected"
                        : "Offer ID: " + selected.getId(),
                x + 190,
                y + 34,
                0xAAAAAA
        );

        if (selected != null) {
            ItemStack result = selected.getResult();

            if (!result.isEmpty()) {
                fontRenderer.drawString(
                        "Product:",
                        x + 190,
                        y + 151,
                        0xFFFFFF
                );

                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(
                        result,
                        x + 250,
                        y + 145
                );
                RenderHelper.disableStandardItemLighting();
            }
        }

        priceField.drawTextBox();
        sellPriceField.drawTextBox();
        maxPerDayField.drawTextBox();
        dailyStockField.drawTextBox();

        if (!statusMessage.isEmpty()) {
            fontRenderer.drawString(
                    statusMessage,
                    x + 12,
                    y + 250,
                    0xFFFF55
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawGrid(
            int gridX,
            int gridY,
            int mouseX,
            int mouseY
    ) {
        for (int slot = 0; slot < 54; slot++) {
            int column = slot % GRID_COLUMNS;
            int row = slot / GRID_COLUMNS;

            int slotX = gridX + column * SLOT_SIZE;
            int slotY = gridY + row * SLOT_SIZE;

            NpcTradeConfig offer = getOfferAtSlot(slot);

            int color = offer != null
                    && offer.getId().equals(selectedOfferId)
                    ? 0xFF777744
                    : 0xFF555555;

            drawRect(
                    slotX,
                    slotY,
                    slotX + 18,
                    slotY + 18,
                    color
            );

            drawRect(
                    slotX + 1,
                    slotY + 1,
                    slotX + 17,
                    slotY + 17,
                    0xFF222222
            );

            if (offer == null) {
                continue;
            }

            ItemStack stack = offer.getResult();

            if (!stack.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();

                itemRender.renderItemAndEffectIntoGUI(
                        stack,
                        slotX + 1,
                        slotY + 1
                );

                RenderHelper.disableStandardItemLighting();
            }

            if (mouseX >= slotX && mouseX < slotX + 18
                    && mouseY >= slotY && mouseY < slotY + 18) {

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

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
