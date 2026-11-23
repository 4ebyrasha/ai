package com.cheba.gui;

import java.io.IOException;
import java.util.List;

import com.cheba.trader.NpcTradeConfig;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

/**
 * Визуальное расположение торговых предложений.
 * <p>
 * Предложения перемещаются между ячейками drag-and-drop.
 * При переносе на занятую ячейку предложения меняются местами.
 * Постоянные offerId не изменяются.
 */
@SuppressWarnings("deprecation")
public class GuiNpcTraderOfferLayout extends GuiScreen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int SLOT_COUNT = COLUMNS * ROWS;
    private static final int SLOT_SIZE = 22;

    private final EntityNPCInterface npc;
    private final GuiScreen parent;
    private final RoleTrader role;

    private int draggedSlot = -1;
    private int dragMouseX;
    private int dragMouseY;

    private String statusMessage = "";

    public GuiNpcTraderOfferLayout(
            EntityNPCInterface npc,
            GuiScreen parent,
            RoleTrader role
    ) {
        this.npc = npc;
        this.parent = parent;
        this.role = role;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        normalizeDisplaySlots();

        buttonList.add(new GuiButton(
                0,
                width / 2 - 45,
                getGridY() + ROWS * SLOT_SIZE + 22,
                90,
                20,
                I18n.translateToLocal("cheba.trader.editor.done")
        ));
    }

    private int getGridX() {
        return (width - COLUMNS * SLOT_SIZE) / 2;
    }

    private int getGridY() {
        return Math.max(
                40,
                (height - ROWS * SLOT_SIZE) / 2 - 25
        );
    }

    /**
     * Назначает свободные позиции предложениям,
     * у которых displaySlot отсутствует или повторяется.
     * <p>
     * Существующие корректные позиции сохраняются.
     */
    private void normalizeDisplaySlots() {
        boolean[] used = new boolean[SLOT_COUNT];
        boolean changed = false;

        List<NpcTradeConfig> offers = role.getDynamicOffers();

        for (NpcTradeConfig offer : offers) {
            if (offer == null) {
                continue;
            }

            int slot = offer.getDisplaySlot();

            if (slot >= 0
                    && slot < SLOT_COUNT
                    && !used[slot]) {
                used[slot] = true;
                continue;
            }

            int freeSlot = findFreeSlot(used);

            if (freeSlot < 0) {
                if (slot != -1) {
                    offer.setDisplaySlot(-1);
                    changed = true;
                }
                continue;
            }

            offer.setDisplaySlot(freeSlot);
            used[freeSlot] = true;
            changed = true;
        }

        if (changed) {
            saveRole();
        }
    }

    private int findFreeSlot(boolean[] used) {
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Находит предложение по его визуальной позиции.
     */
    private NpcTradeConfig getOfferAtSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return null;
        }

        for (NpcTradeConfig offer : role.getDynamicOffers()) {
            if (offer != null && offer.getDisplaySlot() == slot) {
                return offer;
            }
        }

        return null;
    }

    private int getHoveredSlot(int mouseX, int mouseY) {
        int x = getGridX();
        int y = getGridY();

        if (mouseX < x
                || mouseX >= x + COLUMNS * SLOT_SIZE
                || mouseY < y
                || mouseY >= y + ROWS * SLOT_SIZE) {
            return -1;
        }

        int col = (mouseX - x) / SLOT_SIZE;
        int row = (mouseY - y) / SLOT_SIZE;

        return row * COLUMNS + col;
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {
        if (mouseButton == 0) {
            int slot = getHoveredSlot(mouseX, mouseY);

            if (slot >= 0 && getOfferAtSlot(slot) != null) {
                draggedSlot = slot;
                dragMouseX = mouseX;
                dragMouseY = mouseY;
                statusMessage = "";
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(
            int mouseX,
            int mouseY,
            int clickedMouseButton,
            long timeSinceLastClick
    ) {
        if (draggedSlot >= 0 && clickedMouseButton == 0) {
            dragMouseX = mouseX;
            dragMouseY = mouseY;
        }

        super.mouseClickMove(
                mouseX,
                mouseY,
                clickedMouseButton,
                timeSinceLastClick
        );
    }

    @Override
    protected void mouseReleased(
            int mouseX,
            int mouseY,
            int state
    ) {
        if (state == 0 && draggedSlot >= 0) {
            int targetSlot = getHoveredSlot(mouseX, mouseY);

            if (targetSlot >= 0 && targetSlot != draggedSlot) {
                moveOffer(draggedSlot, targetSlot);
            }

            draggedSlot = -1;
        }

        super.mouseReleased(mouseX, mouseY, state);
    }

    /**
     * Перемещает предложение.
     * <p>
     * Если целевая ячейка занята, предложения меняются местами.
     */
    private void moveOffer(int sourceSlot, int targetSlot) {
        NpcTradeConfig source = getOfferAtSlot(sourceSlot);

        if (source == null) {
            return;
        }

        NpcTradeConfig target = getOfferAtSlot(targetSlot);

        if (target != null) {
            target.setDisplaySlot(sourceSlot);
        }

        source.setDisplaySlot(targetSlot);

        saveRole();

        statusMessage = I18n.translateToLocal(
                "cheba.trader.editor.layout_saved"
        );
    }

    /**
     * Сохраняет обновлённый список предложений на сервере.
     */
    private void saveRole() {
        role.markTradeConfigChanged();

        Client.sendData(
                EnumPacketServer.RoleSave,
                role.writeToNBT(new NBTTagCompound())
        );
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {
        if (button.id == 0) {
            saveRole();
            mc.displayGuiScreen(parent);
        }
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

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        drawDefaultBackground();

        int gridX = getGridX();
        int gridY = getGridY();

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.layout_title"),
                width / 2,
                gridY - 25,
                0xFFFFFF
        );

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.editor.layout_hint"),
                width / 2,
                gridY - 12,
                0xAAAAAA
        );

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int col = slot % COLUMNS;
            int row = slot / COLUMNS;

            int x = gridX + col * SLOT_SIZE;
            int y = gridY + row * SLOT_SIZE;

            boolean hovered = getHoveredSlot(mouseX, mouseY) == slot;
            boolean source = draggedSlot == slot;

            int background = hovered
                    ? 0xFFFFFF55
                    : 0xFF777777;

            drawRect(
                    x,
                    y,
                    x + 20,
                    y + 20,
                    background
            );

            drawRect(
                    x + 1,
                    y + 1,
                    x + 19,
                    y + 19,
                    source ? 0xFF505050 : 0xFF252525
            );

            NpcTradeConfig offer = getOfferAtSlot(slot);

            if (offer == null || source) {
                continue;
            }

            ItemStack stack = offer.getResult();

            if (stack.isEmpty()) {
                continue;
            }

            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    stack,
                    x + 2,
                    y + 2
            );

            itemRender.renderItemOverlays(
                    fontRenderer,
                    stack,
                    x + 2,
                    y + 2
            );

            RenderHelper.disableStandardItemLighting();
        }

        if (draggedSlot >= 0) {
            NpcTradeConfig dragged = getOfferAtSlot(draggedSlot);

            if (dragged != null) {
                ItemStack stack = dragged.getResult();

                if (!stack.isEmpty()) {
                    RenderHelper.enableGUIStandardItemLighting();

                    itemRender.renderItemAndEffectIntoGUI(
                            stack,
                            dragMouseX - 8,
                            dragMouseY - 8
                    );

                    itemRender.renderItemOverlays(
                            fontRenderer,
                            stack,
                            dragMouseX - 8,
                            dragMouseY - 8
                    );

                    RenderHelper.disableStandardItemLighting();
                }
            }
        }

        if (!statusMessage.isEmpty()) {
            drawCenteredString(
                    fontRenderer,
                    statusMessage,
                    width / 2,
                    gridY + ROWS * SLOT_SIZE + 6,
                    0x55FF55
            );
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        int hoveredSlot = getHoveredSlot(mouseX, mouseY);

        if (hoveredSlot >= 0) {
            NpcTradeConfig offer = getOfferAtSlot(hoveredSlot);

            if (offer != null) {
                ItemStack stack = offer.getResult();

                if (!stack.isEmpty()) {
                    drawHoveringText(
                            stack.getTooltip(
                                    mc.player,
                                    mc.gameSettings.advancedItemTooltips
                                            ? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED
                                            : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL
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
