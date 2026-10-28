
package com.cheba.gui;

import java.io.IOException;
import java.util.List;

import com.cheba.trader.NpcTradeConfig;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.client.gui.roles.GuiNpcTraderSetup;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

@SuppressWarnings("deprecation")
public class GuiNpcDynamicTraderEditor extends GuiScreen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 270;
    private static final int VISIBLE_OFFERS = 6;

    private int offerPage = 0;

    private final EntityNPCInterface npc;
    private final GuiNpcTraderSetup parent;
    private final RoleTrader role;

    private int selectedOfferIndex = -1;
    private int selectedInventorySlot = -1;

    private GuiTextField buyPriceField;
    private GuiTextField sellPriceField;
    private GuiTextField maxPerDayField;
    private GuiTextField dailyStockField;

    private String statusMessage = "";

    public GuiNpcDynamicTraderEditor(
            EntityNPCInterface npc,
            GuiNpcTraderSetup parent,
            RoleTrader role
    ) {
        this.npc = npc;
        this.parent = parent;
        this.role = role;

        if (!role.getDynamicOffers().isEmpty()) {
            selectedOfferIndex = 0;
        }
    }

    private int left() {
        return (width - PANEL_WIDTH) / 2;
    }

    private int top() {
        return (height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int x = left();
        int y = top();

        buttonList.add(new GuiButton(
                0, x + 190, y + 25, 48, 20,
                I18n.translateToLocal("cheba.trader.editor.add")
        ));

        buttonList.add(new GuiButton(
                1, x + 240, y + 25, 54, 20,
                I18n.translateToLocal("cheba.trader.editor.remove")
        ));

        buttonList.add(new GuiButton(
                2, x + 296, y + 25, 54, 20,
                I18n.translateToLocal("cheba.trader.editor.save")
        ));

        buttonList.add(new GuiButton(
                3, x + 352, y + 25, 56, 20,
                I18n.translateToLocal("cheba.trader.editor.back")
        ));

        buyPriceField = createField(0, x + 292, y + 57);
        sellPriceField = createField(1, x + 292, y + 79);
        maxPerDayField = createField(2, x + 292, y + 101);
        dailyStockField = createField(3, x + 292, y + 123);

        buttonList.add(new GuiButton(
                8, left() + 7, top() + 7, 54, 18,
                I18n.translateToLocal("cheba.trader.editor.prev")
        ));

        buttonList.add(new GuiButton(
                9, left() + 64, top() + 7, 54, 18,
                I18n.translateToLocal("cheba.trader.editor.next")
        ));

        buttonList.add(new GuiButton(
                4, x + 190, y + 147, 51, 20,
                I18n.translateToLocal("cheba.trader.editor.set_product")
        ));

        buttonList.add(new GuiButton(
                5, x + 243, y + 147, 51, 20,
                I18n.translateToLocal("cheba.trader.editor.clear_product")
        ));

        buttonList.add(new GuiButton(
                6, x + 296, y + 147, 51, 20,
                I18n.translateToLocal("cheba.trader.editor.add_payment")
        ));

        buttonList.add(new GuiButton(
                7, x + 349, y + 147, 59, 20,
                I18n.translateToLocal("cheba.trader.editor.clear_payment")
        ));

        refreshFields();
    }

    private GuiTextField createField(int id, int x, int y) {
        GuiTextField field = new GuiTextField(
                id, fontRenderer, x, y, 116, 18
        );
        field.setMaxStringLength(20);
        return field;
    }

    private NpcTradeConfig getSelectedOffer() {
        List<NpcTradeConfig> offers = role.getDynamicOffers();

        if (selectedOfferIndex < 0
                || selectedOfferIndex >= offers.size()) {
            return null;
        }

        return offers.get(selectedOfferIndex);
    }

    private void refreshFields() {
        NpcTradeConfig offer = getSelectedOffer();

        if (offer == null) {
            buyPriceField.setText("0");
            sellPriceField.setText("0");
            maxPerDayField.setText("-1");
            dailyStockField.setText("-1");
            return;
        }

        buyPriceField.setText(String.valueOf(offer.getPriceMoney()));
        sellPriceField.setText(String.valueOf(offer.getSellPriceMoney()));
        maxPerDayField.setText(String.valueOf(offer.getMaxPerDay()));
        dailyStockField.setText(String.valueOf(offer.getDailyStock()));
    }

    private void saveFields() {
        NpcTradeConfig offer = getSelectedOffer();

        if (offer == null) {
            return;
        }

        offer.setPriceMoney(parseLong(
                buyPriceField.getText(),
                offer.getPriceMoney(),
                0L
        ));

        offer.setSellPriceMoney(parseLong(
                sellPriceField.getText(),
                offer.getSellPriceMoney(),
                0L
        ));

        offer.setMaxPerDay(parseInt(
                maxPerDayField.getText(),
                offer.getMaxPerDay(),
                -1
        ));

        offer.setDailyStock(parseInt(
                dailyStockField.getText(),
                offer.getDailyStock(),
                -1
        ));
    }

    private long parseLong(String text, long fallback, long minimum) {
        try {
            return Math.max(minimum, Long.parseLong(text.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int parseInt(String text, int fallback, int minimum) {
        try {
            return Math.max(minimum, Integer.parseInt(text.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private ItemStack getSelectedInventoryStack() {
        if (selectedInventorySlot < 0
                || selectedInventorySlot >= mc.player.inventory.getSizeInventory()) {
            return ItemStack.EMPTY;
        }

        return mc.player.inventory.getStackInSlot(selectedInventorySlot);
    }

    private void saveRole() {
        saveFields();

        Client.sendData(
                EnumPacketServer.RoleSave,
                role.writeToNBT(new NBTTagCompound())
        );
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        saveFields();

        switch (button.id) {
            case 0: {
                // Добавить предложение и перейти на его страницу
                role.addDynamicOffer(new NpcTradeConfig());

                selectedOfferIndex = role.getDynamicOffers().size() - 1;
                offerPage = selectedOfferIndex / VISIBLE_OFFERS;

                statusMessage = I18n.translateToLocal(
                        "cheba.trader.editor.created"
                );

                refreshFields();
                break;
            }

            case 1: {
                // Удалить выбранное предложение
                NpcTradeConfig offer = getSelectedOffer();

                if (offer != null) {
                    role.removeDynamicOffer(offer.getId());

                    int size = role.getDynamicOffers().size();

                    if (size == 0) {
                        selectedOfferIndex = -1;
                        offerPage = 0;
                    } else {
                        selectedOfferIndex = Math.min(
                                selectedOfferIndex,
                                size - 1
                        );

                        offerPage = selectedOfferIndex / VISIBLE_OFFERS;
                    }

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.removed"
                    );
                }

                refreshFields();
                break;
            }

            case 2:
                saveRole();

                statusMessage = I18n.translateToLocal(
                        "cheba.trader.editor.saved"
                );
                break;

            case 3:
                saveRole();
                mc.displayGuiScreen(parent);
                return;

            case 4: {
                // Установить товар из выбранного слота инвентаря
                NpcTradeConfig offer = getSelectedOffer();
                ItemStack stack = getSelectedInventoryStack();

                if (offer != null && !stack.isEmpty()) {
                    offer.setResult(stack.copy());

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.product_set"
                    );
                } else {
                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.select_item"
                    );
                }

                break;
            }

            case 5: {
                NpcTradeConfig offer = getSelectedOffer();

                if (offer != null) {
                    offer.setResult(ItemStack.EMPTY);

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.product_cleared"
                    );
                }

                break;
            }

            case 6: {
                // Добавить предмет из инвентаря к оплате
                NpcTradeConfig offer = getSelectedOffer();
                ItemStack stack = getSelectedInventoryStack();

                if (offer != null && !stack.isEmpty()) {
                    offer.addPriceItem(stack.copy());

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.payment_added"
                    );
                } else {
                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.select_item"
                    );
                }

                break;
            }

            case 7: {
                NpcTradeConfig offer = getSelectedOffer();

                if (offer != null) {
                    offer.clearPriceItems();

                    statusMessage = I18n.translateToLocal(
                            "cheba.trader.editor.payment_cleared"
                    );
                }

                break;
            }

            case 8: {
                // Предыдущая страница
                offerPage = Math.max(0, offerPage - 1);

                int size = role.getDynamicOffers().size();

                selectedOfferIndex = size == 0
                        ? -1
                        : Math.min(
                        offerPage * VISIBLE_OFFERS,
                        size - 1
                );

                refreshFields();
                break;
            }

            case 9: {
                // Следующая страница
                int size = role.getDynamicOffers().size();

                int maxPage = size == 0
                        ? 0
                        : (size - 1) / VISIBLE_OFFERS;

                offerPage = Math.min(maxPage, offerPage + 1);

                selectedOfferIndex = size == 0
                        ? -1
                        : Math.min(
                        offerPage * VISIBLE_OFFERS,
                        size - 1
                );

                refreshFields();
                break;
            }

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
        buyPriceField.mouseClicked(mouseX, mouseY, mouseButton);
        sellPriceField.mouseClicked(mouseX, mouseY, mouseButton);
        maxPerDayField.mouseClicked(mouseX, mouseY, mouseButton);
        dailyStockField.mouseClicked(mouseX, mouseY, mouseButton);

        int x = left();
        int y = top();

        // Выбор предложения в списке
        if (mouseButton == 0
                && mouseX >= x + 7
                && mouseX < x + 180
                && mouseY >= y + 31
                && mouseY < y + 31 + VISIBLE_OFFERS * 18) {

            int row = (mouseY - (y + 31)) / 18;
            int index = offerPage * VISIBLE_OFFERS + row;

            if (index < role.getDynamicOffers().size()) {
                saveFields();
                selectedOfferIndex = index;
                refreshFields();
                statusMessage = "";
            }
        }

        // Выбор предмета в основном инвентаре
        int inventoryX = x + (PANEL_WIDTH - 9 * 18) / 2;
        int mainY = y + 183;
        int hotbarY = y + 237;

        if (mouseButton == 0
                && mouseX >= inventoryX
                && mouseX < inventoryX + 9 * 18) {

            int col = (mouseX - inventoryX) / 18;

            if (mouseY >= mainY && mouseY < mainY + 3 * 18) {
                int row = (mouseY - mainY) / 18;
                selectedInventorySlot = 9 + row * 9 + col;
            } else if (mouseY >= hotbarY && mouseY < hotbarY + 18) {
                selectedInventorySlot = col;
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            saveRole();
            mc.displayGuiScreen(parent);
            return;
        }

        buyPriceField.textboxKeyTyped(typedChar, keyCode);
        sellPriceField.textboxKeyTyped(typedChar, keyCode);
        maxPerDayField.textboxKeyTyped(typedChar, keyCode);
        dailyStockField.textboxKeyTyped(typedChar, keyCode);

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
                x,
                y,
                x + PANEL_WIDTH,
                y + PANEL_HEIGHT,
                0xEE202020
        );

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.title"),
                width / 2,
                y + 7,
                0xFFFFFF
        );

        // Список предложений с учётом текущей страницы.
        List<NpcTradeConfig> offers = role.getDynamicOffers();

        for (int i = 0; i < VISIBLE_OFFERS; i++) {
            int offerIndex = offerPage * VISIBLE_OFFERS + i;
            int rowY = y + 31 + i * 18;

            boolean selected = offerIndex == selectedOfferIndex;

            drawRect(
                    x + 7,
                    rowY,
                    x + 180,
                    rowY + 17,
                    selected ? 0xFF626262 : 0xFF303030
            );

            if (offerIndex < offers.size()) {
                NpcTradeConfig offer = offers.get(offerIndex);
                ItemStack product = offer.getResult();

                String name = product.isEmpty()
                        ? I18n.translateToLocal(
                        "cheba.trader.editor.empty_offer"
                )
                        : product.getDisplayName();

                name = fontRenderer.trimStringToWidth(name, 160);

                fontRenderer.drawString(
                        (offerIndex + 1) + ". " + name,
                        x + 11,
                        rowY + 5,
                        0xFFFFFF
                );
            }
        }

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.buy_price"),
                x + 190,
                y + 61,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.sell_price"),
                x + 190,
                y + 83,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.max_day"),
                x + 190,
                y + 105,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.stock"),
                x + 190,
                y + 127,
                0xFFFFFF
        );

        buyPriceField.drawTextBox();
        sellPriceField.drawTextBox();
        maxPerDayField.drawTextBox();
        dailyStockField.drawTextBox();

        NpcTradeConfig selectedOffer = getSelectedOffer();

        if (selectedOffer != null) {
            String payment = I18n.translateToLocalFormatted(
                    "cheba.trader.editor.payment_count",
                    selectedOffer.getPriceItemCount()
            );

            drawString(
                    fontRenderer,
                    fontRenderer.trimStringToWidth(payment, 170),
                    x + 8,
                    y + 145,
                    0xAAAAAA
            );
        }

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.inventory"),
                x + 8,
                y + 168,
                0xFFFFFF
        );

        // Основной инвентарь игрока.
        int inventoryX = x + (PANEL_WIDTH - 9 * 18) / 2;
        int mainY = y + 183;
        int hotbarY = y + 237;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = 9 + row * 9 + col;

                drawInventorySlot(
                        inventoryX + col * 18,
                        mainY + row * 18,
                        slot
                );
            }
        }

        // Панель быстрого доступа.
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
                    y + 255,
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
                    y + PANEL_HEIGHT - 10,
                    0xFFFF55
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawInventorySlot(int x, int y, int slot) {
        boolean selected = slot == selectedInventorySlot;

        drawRect(
                x, y, x + 18, y + 18,
                selected ? 0xFFFFFF55 : 0xFF777777
        );

        drawRect(
                x + 1, y + 1, x + 17, y + 17,
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

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}

