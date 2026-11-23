package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

public class GuiTraderPresetPaymentEditor extends GuiScreen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 190;

    private static final int SLOT_SIZE = 18;
    private static final int COLUMNS = 6;
    private static final int ROWS = 2;
    private static final int PAGE_SIZE = COLUMNS * ROWS;

    private final GuiTraderPresetEditor parent;
    private final NpcTradeConfig offer;

    private int selectedIndex = -1;
    private int page;

    public GuiTraderPresetPaymentEditor(
            GuiTraderPresetEditor parent,
            NpcTradeConfig offer
    ) {
        if (parent == null || offer == null) {
            throw new IllegalArgumentException(
                    "Parent and offer cannot be null"
            );
        }

        this.parent = parent;
        this.offer = offer;
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private int getPageCount() {
        return Math.max(
                1,
                (offer.getPriceItemCount() + PAGE_SIZE - 1) / PAGE_SIZE
        );
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = left();
        int y = top();

        buttonList.add(new GuiButton(
                0, x + 10, y + 145, 100, 20, "Add from hand"
        ));

        buttonList.add(new GuiButton(
                1, x + 115, y + 145, 115, 20, "Remove selected"
        ));

        buttonList.add(new GuiButton(
                2, x + 10, y + 168, 55, 20, "<"
        ));

        buttonList.add(new GuiButton(
                3, x + 175, y + 168, 55, 20, ">"
        ));

        buttonList.add(new GuiButton(
                4, x + 70, y + 168, 100, 20, "Done"
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {

        switch (button.id) {
            case 0: {
                ItemStack held = mc.player.getHeldItemMainhand();

                if (held.isEmpty()) {
                    return;
                }

                if (offer.getPriceItemCount() >= 54) {
                    return;
                }

                offer.addPriceItem(held);

                page = Math.min(
                        getPageCount() - 1,
                        page
                );

                selectedIndex = offer.getPriceItemCount() - 1;
                break;
            }

            case 1: {
                if (selectedIndex < 0
                        || selectedIndex >= offer.getPriceItemCount()) {
                    return;
                }

                offer.removePriceItem(selectedIndex);

                if (selectedIndex >= offer.getPriceItemCount()) {
                    selectedIndex = offer.getPriceItemCount() - 1;
                }

                page = Math.min(
                        page,
                        getPageCount() - 1
                );
                break;
            }

            case 2: {
                page = Math.max(0, page - 1);
                break;
            }

            case 3: {
                page = Math.min(getPageCount() - 1, page + 1);
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

        if (mouseButton != 0) {
            return;
        }

        int gridX = left() + 66;
        int gridY = top() + 48;

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

        int index = page * PAGE_SIZE + row * COLUMNS + column;

        if (index >= 0 && index < offer.getPriceItemCount()) {
            selectedIndex = index;
        }
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
                0xDD202020
        );

        fontRenderer.drawString(
                "Offer payment items",
                x + 10,
                y + 10,
                0xFFFFFF
        );

        fontRenderer.drawString(
                "Items: " + offer.getPriceItemCount() + " / 54",
                x + 10,
                y + 25,
                0xAAAAAA
        );

        fontRenderer.drawString(
                "Page: " + (page + 1) + " / " + getPageCount(),
                x + 10,
                y + 35,
                0xAAAAAA
        );

        List<ItemStack> items = offer.getPriceItems();

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int column = slot % COLUMNS;
            int row = slot / COLUMNS;

            int slotX = x + 66 + column * SLOT_SIZE;
            int slotY = y + 48 + row * SLOT_SIZE;

            int index = page * PAGE_SIZE + slot;

            boolean selected = index == selectedIndex;

            drawRect(
                    slotX,
                    slotY,
                    slotX + SLOT_SIZE,
                    slotY + SLOT_SIZE,
                    selected ? 0xFF777744 : 0xFF555555
            );

            drawRect(
                    slotX + 1,
                    slotY + 1,
                    slotX + 17,
                    slotY + 17,
                    0xFF222222
            );

            if (index >= items.size()) {
                continue;
            }

            ItemStack stack = items.get(index);

            if (stack.isEmpty()) {
                continue;
            }

            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    stack,
                    slotX + 1,
                    slotY + 1
            );

            itemRender.renderItemOverlayIntoGUI(
                    fontRenderer,
                    stack,
                    slotX + 1,
                    slotY + 1,
                    null
            );

            RenderHelper.disableStandardItemLighting();

            if (mouseX >= slotX && mouseX < slotX + SLOT_SIZE
                    && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
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

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
