package com.cheba.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cheba.trader.NpcBuybackConfig;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

@SuppressWarnings("deprecation")
public class GuiNpcTraderBuybackEditor extends GuiScreen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 270;

    private static final int COLUMNS = 10;
    private static final int ROWS = 8;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 16;

    private final EntityNPCInterface npc;
    private final GuiScreen parent;
    private final RoleTrader role;

    private int page;
    private int selectedOfferIndex = -1;
    private int selectedInventorySlot = -1;

    private GuiTextField priceField;
    private String statusMessage = "";

    public GuiNpcTraderBuybackEditor(
            EntityNPCInterface npc,
            GuiScreen parent,
            RoleTrader role
    ) {
        this.npc = npc;
        this.parent = parent;
        this.role = role;

        if (!role.getBuybackOffers().isEmpty()) {
            selectedOfferIndex = 0;
        }
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    private int getPageCount() {
        int count = role.getBuybackOffers().size();

        return Math.max(
                1,
                (count + PAGE_SIZE - 1) / PAGE_SIZE
        );
    }

    private NpcBuybackConfig getSelectedOffer() {
        List<NpcBuybackConfig> offers = role.getBuybackOffers();

        if (selectedOfferIndex < 0
                || selectedOfferIndex >= offers.size()) {
            return null;
        }

        return offers.get(selectedOfferIndex);
    }

    private ItemStack getSelectedInventoryStack() {
        if (selectedInventorySlot < 0
                || selectedInventorySlot
                >= mc.player.inventory.getSizeInventory()) {
            return ItemStack.EMPTY;
        }

        return mc.player.inventory.getStackInSlot(
                selectedInventorySlot
        );
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = left();
        int y = top();

        buttonList.add(new GuiButton(
                0, x + 190, y + 25, 60, 20,
                I18n.translateToLocal("cheba.trader.buyback.add")
        ));

        buttonList.add(new GuiButton(
                1, x + 252, y + 25, 48, 20,
                I18n.translateToLocal("cheba.trader.editor.remove")
        ));

        buttonList.add(new GuiButton(
                2, x + 302, y + 25, 48, 20,
                I18n.translateToLocal("cheba.trader.editor.save")
        ));

        buttonList.add(new GuiButton(
                3, x + 352, y + 25, 56, 20,
                I18n.translateToLocal("cheba.trader.editor.back")
        ));

        GuiButton previous = new GuiButton(
                4, x + 7, y + 7, 54, 18,
                I18n.translateToLocal("cheba.trader.editor.prev")
        );

        GuiButton next = new GuiButton(
                5, x + 64, y + 7, 54, 18,
                I18n.translateToLocal("cheba.trader.editor.next")
        );

        previous.enabled = page > 0;
        next.enabled = page < getPageCount() - 1;

        buttonList.add(previous);
        buttonList.add(next);

        priceField = new GuiTextField(
                0,
                fontRenderer,
                x + 292,
                y + 57,
                116,
                18
        );

        priceField.setMaxStringLength(20);
        refreshPriceField();
    }

    private void refreshPriceField() {
        NpcBuybackConfig offer = getSelectedOffer();

        priceField.setText(
                offer == null
                        ? "0"
                        : String.valueOf(offer.getPriceMoney())
        );
    }

    private void saveFields() {
        NpcBuybackConfig offer = getSelectedOffer();

        if (offer == null || priceField == null) {
            return;
        }

        try {
            offer.setPriceMoney(
                    Math.max(
                            0L,
                            Long.parseLong(priceField.getText().trim())
                    )
            );
        } catch (NumberFormatException ignored) {
            // Некорректный ввод не заменяет предыдущую цену.
        }
    }

    private void saveRole() {
        saveFields();

        role.markTradeConfigChanged();

        Client.sendData(
                EnumPacketServer.RoleSave,
                role.writeToNBT(new NBTTagCompound())
        );
    }

    private void selectFirstOfferOnPage() {
        int count = role.getBuybackOffers().size();

        if (count == 0) {
            selectedOfferIndex = -1;
            page = 0;
            return;
        }

        selectedOfferIndex = Math.min(
                page * PAGE_SIZE,
                count - 1
        );
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {
        saveFields();

        switch (button.id) {
            case 0: {
                ItemStack selected = getSelectedInventoryStack();

                if (selected.isEmpty()) {
                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.buyback.select_item"
                    );
                    break;
                }

                NpcBuybackConfig offer =
                        new NpcBuybackConfig(selected, 0L);

                if (!role.addBuybackOffer(offer)) {
                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.buyback.duplicate"
                    );
                    break;
                }

                selectedOfferIndex =
                        role.getBuybackOffers().size() - 1;

                page = selectedOfferIndex / PAGE_SIZE;

                statusMessage = I18n.translateToLocal(
                        "cheba.trader.buyback.added"
                );

                initGui();
                break;
            }

            case 1: {
                NpcBuybackConfig offer = getSelectedOffer();

                if (offer != null) {
                    role.removeBuybackOffer(offer.getId());

                    int count = role.getBuybackOffers().size();

                    if (count == 0) {
                        selectedOfferIndex = -1;
                        page = 0;
                    } else {
                        selectedOfferIndex = Math.min(
                                selectedOfferIndex,
                                count - 1
                        );

                        page = Math.min(
                                page,
                                getPageCount() - 1
                        );
                    }

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.buyback.removed"
                    );
                }

                initGui();
                break;
            }

            case 2:
                saveRole();

                statusMessage = I18n.translateToLocal(
                        "cheba.trader.buyback.saved"
                );
                break;

            case 3:
                saveRole();
                mc.displayGuiScreen(parent);
                return;

            case 4:
                page = Math.max(0, page - 1);
                selectFirstOfferOnPage();
                initGui();
                break;

            case 5:
                page = Math.min(
                        getPageCount() - 1,
                        page + 1
                );

                selectFirstOfferOnPage();
                initGui();
                break;

            default:
                break;
        }
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {
        priceField.mouseClicked(mouseX, mouseY, mouseButton);

        int x = left();
        int y = top();

        int gridX = x + 7;
        int gridY = y + 31;

        boolean insideGrid = mouseX >= gridX
                && mouseX < gridX + COLUMNS * SLOT_SIZE
                && mouseY >= gridY
                && mouseY < gridY + ROWS * SLOT_SIZE;

        if (mouseButton == 0 && insideGrid) {
            int col = (mouseX - gridX) / SLOT_SIZE;
            int row = (mouseY - gridY) / SLOT_SIZE;

            int index = page * PAGE_SIZE + row * COLUMNS + col;

            if (index < role.getBuybackOffers().size()) {
                saveFields();
                selectedOfferIndex = index;
                refreshPriceField();
                statusMessage = "";
            }
        }

        /*
         * Выбор предмета из обычного инвентаря игрока
         */
        int inventoryX = x + (PANEL_WIDTH - 9 * 18) / 2;
        int mainY = y + 198;
        int hotbarY = y + 252;

        if (mouseButton == 0
                && mouseX >= inventoryX
                && mouseX < inventoryX + 9 * 18) {

            int col = (mouseX - inventoryX) / 18;

            if (mouseY >= mainY && mouseY < mainY + 3 * 18) {
                selectedInventorySlot =
                        9 + ((mouseY - mainY) / 18) * 9 + col;

            } else if (mouseY >= hotbarY && mouseY < hotbarY + 18) {
                selectedInventorySlot = col;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(
            char typedChar,
            int keyCode
    ) throws IOException {
        if (keyCode == 1) {
            saveRole();
            mc.displayGuiScreen(parent);
            return;
        }

        priceField.textboxKeyTyped(typedChar, keyCode);

        super.keyTyped(typedChar, keyCode);
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
                x, y,
                x + PANEL_WIDTH, y + PANEL_HEIGHT,
                0xEE202020
        );

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.buyback.title"),
                x + 300,
                y + 7,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocalFormatted(
                        "cheba.trader.page",
                        page + 1,
                        getPageCount()
                ),
                x + 122,
                y + 12,
                0xFFFFFF
        );

        List<NpcBuybackConfig> offers = role.getBuybackOffers();

        // Компактная сетка 10 × 8.
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int index = page * PAGE_SIZE + row * COLUMNS + col;

                int slotX = x + 7 + col * SLOT_SIZE;
                int slotY = y + 31 + row * SLOT_SIZE;

                boolean selected = index == selectedOfferIndex;

                drawRect(
                        slotX,
                        slotY,
                        slotX + SLOT_SIZE,
                        slotY + SLOT_SIZE,
                        selected ? 0xFFFFFF55 : 0xFF777777
                );

                drawRect(
                        slotX + 1,
                        slotY + 1,
                        slotX + SLOT_SIZE - 1,
                        slotY + SLOT_SIZE - 1,
                        selected ? 0xFF555522 : 0xFF303030
                );

                if (index >= offers.size()) {
                    continue;
                }

                ItemStack stack = offers.get(index).getItem();

                if (!stack.isEmpty()) {
                    RenderHelper.enableGUIStandardItemLighting();

                    itemRender.renderItemAndEffectIntoGUI(
                            stack,
                            slotX,
                            slotY
                    );

                    itemRender.renderItemOverlays(
                            fontRenderer,
                            stack,
                            slotX,
                            slotY
                    );

                    RenderHelper.disableStandardItemLighting();
                }
            }
        }

        // Цена выбранного предложения — на прежнем месте.
        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.buyback.price"),
                x + 190,
                y + 61,
                0xFFFFFF
        );

        priceField.drawTextBox();

        NpcBuybackConfig selectedOffer = getSelectedOffer();

        if (selectedOffer != null) {
            drawString(
                    fontRenderer,
                    I18n.translateToLocalFormatted(
                            "cheba.trader.buyback.price_tooltip",
                            selectedOffer.getPriceMoney()
                    ),
                    x + 190,
                    y + 84,
                    0xAAAAAA
            );
        }

        // Инвентарь игрока не перемещаем.
        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.inventory"),
                x + 8,
                y + 168,
                0xFFFFFF
        );

        int inventoryX = x + (PANEL_WIDTH - 9 * 18) / 2;
        int mainY = y + 183;
        int hotbarY = y + 237;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawInventorySlot(
                        inventoryX + col * 18,
                        mainY + row * 18,
                        9 + row * 9 + col
                );
            }
        }

        for (int col = 0; col < 9; col++) {
            drawInventorySlot(
                    inventoryX + col * 18,
                    hotbarY,
                    col
            );
        }

        ItemStack selectedStack = getSelectedInventoryStack();

        if (!selectedStack.isEmpty()) {
            String selectedText =
                    I18n.translateToLocal("cheba.trader.editor.selected")
                            + selectedStack.getDisplayName();

            drawString(
                    fontRenderer,
                    fontRenderer.trimStringToWidth(
                            selectedText,
                            PANEL_WIDTH - 16
                    ),
                    x + 8,
                    y + 250,
                    0xAAAAAA
            );
        }

        if (!statusMessage.isEmpty()) {
            drawString(
                    fontRenderer,
                    fontRenderer.trimStringToWidth(
                            statusMessage,
                            PANEL_WIDTH - 16
                    ),
                    x + 8,
                    y + 260,
                    0xFFFF55
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawEditorTooltip(mouseX, mouseY);
    }

    private void drawInventorySlot(int x, int y, int slot) {
        boolean selected = slot == selectedInventorySlot;

        drawRect(
                x,
                y,
                x + 18,
                y + 18,
                selected ? 0xFFFFFF55 : 0xFF777777
        );

        drawRect(
                x + 1,
                y + 1,
                x + 17,
                y + 17,
                0xFF303030
        );

        ItemStack stack = mc.player.inventory.getStackInSlot(slot);

        if (!stack.isEmpty()) {
            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    stack,
                    x + 1,
                    y + 1
            );

            itemRender.renderItemOverlays(
                    fontRenderer,
                    stack,
                    x + 1,
                    y + 1
            );

            RenderHelper.disableStandardItemLighting();
        }
    }

    private void drawEditorTooltip(int mouseX, int mouseY) {
        int gridX = left() + 7;
        int gridY = top() + 31;

        if (mouseX >= gridX
                && mouseX < gridX + COLUMNS * SLOT_SIZE
                && mouseY >= gridY
                && mouseY < gridY + ROWS * SLOT_SIZE) {

            int col = (mouseX - gridX) / SLOT_SIZE;
            int row = (mouseY - gridY) / SLOT_SIZE;
            int index = page * PAGE_SIZE + row * COLUMNS + col;

            if (index < role.getBuybackOffers().size()) {
                NpcBuybackConfig offer =
                        role.getBuybackOffers().get(index);

                ItemStack stack = offer.getItem();

                if (!stack.isEmpty()) {
                    List<String> lines = new ArrayList<>(
                            stack.getTooltip(
                                    mc.player,
                                    ITooltipFlag.TooltipFlags.ADVANCED
                            )
                    );

                    lines.add(TextFormatting.GREEN
                            + I18n.translateToLocalFormatted(
                            "cheba.trader.buyback.price_tooltip",
                            offer.getPriceMoney()
                    ));

                    drawHoveringText(lines, mouseX, mouseY);
                    return;
                }
            }
        }

        int inventorySlot = getHoveredInventorySlot(mouseX, mouseY);

        if (inventorySlot >= 0) {
            ItemStack stack =
                    mc.player.inventory.getStackInSlot(inventorySlot);

            if (!stack.isEmpty()) {
                drawHoveringText(
                        stack.getTooltip(
                                mc.player,
                                ITooltipFlag.TooltipFlags.ADVANCED
                        ),
                        mouseX,
                        mouseY
                );
                return;
            }
        }

        for (GuiButton button : buttonList) {
            if (!button.mousePressed(mc, mouseX, mouseY)) {
                continue;
            }

            String key = getButtonTooltipKey(button.id);

            if (key != null) {
                drawHoveringText(
                        Collections.singletonList(
                                I18n.translateToLocal(key)
                        ),
                        mouseX,
                        mouseY
                );
                return;
            }
        }
    }

    private int getHoveredInventorySlot(int mouseX, int mouseY) {
        int x = left() + (PANEL_WIDTH - 9 * 18) / 2;
        int mainY = top() + 198;
        int hotbarY = top() + 252;

        if (mouseX < x || mouseX >= x + 9 * 18) {
            return -1;
        }

        int col = (mouseX - x) / 18;

        if (mouseY >= mainY && mouseY < mainY + 3 * 18) {
            return 9 + ((mouseY - mainY) / 18) * 9 + col;
        }

        if (mouseY >= hotbarY && mouseY < hotbarY + 18) {
            return col;
        }

        return -1;
    }

    private String getButtonTooltipKey(int id) {
        switch (id) {
            case 0:
                return "cheba.trader.tooltip.buyback.add";
            case 1:
                return "cheba.trader.tooltip.editor.remove_offer";
            case 2:
                return "cheba.trader.tooltip.editor.save";
            case 3:
                return "cheba.trader.tooltip.editor.back";
            case 4:
                return "cheba.trader.tooltip.editor.previous_page";
            case 5:
                return "cheba.trader.tooltip.editor.next_page";
            default:
                return null;
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
