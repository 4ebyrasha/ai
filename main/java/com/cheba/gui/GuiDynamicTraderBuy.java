package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class GuiDynamicTraderBuy extends GuiScreen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 22;

    private final int npcEntityId;

    private int page;
    private EntityNPCInterface npc;
    private RoleTrader role;

    public GuiDynamicTraderBuy(int npcEntityId, int page) {
        this.npcEntityId = npcEntityId;
        this.page = Math.max(0, page);
    }

    @Override
    public void initGui() {
        buttonList.clear();

        Entity entity = mc.world == null
                ? null
                : mc.world.getEntityByID(npcEntityId);

        npc = entity instanceof EntityNPCInterface
                ? (EntityNPCInterface) entity
                : null;

        role = npc != null && npc.roleInterface instanceof RoleTrader
                ? (RoleTrader) npc.roleInterface
                : null;

        int gridY = getGridY();
        int controlsY = gridY + ROWS * CELL_SIZE + 8;

        buttonList.add(new GuiButton(
                0,
                width / 2 - 105,
                controlsY,
                65,
                20,
                I18n.translateToLocal("cheba.trader.back")
        ));

        buttonList.add(new GuiButton(
                1,
                width / 2 - 32,
                controlsY,
                64,
                20,
                I18n.translateToLocal("cheba.trader.menu")
        ));

        buttonList.add(new GuiButton(
                2,
                width / 2 + 40,
                controlsY,
                65,
                20,
                I18n.translateToLocal("cheba.trader.next")
        ));

        int pages = getPageCount();

        page = Math.min(page, pages - 1);
        buttonList.get(0).enabled = page > 0;
        buttonList.get(2).enabled = page < pages - 1;
    }

    private int getPageCount() {
        int count = role == null
                ? 0
                : role.getDynamicOffers().size();

        return Math.max(
                1,
                (count + PAGE_SIZE - 1) / PAGE_SIZE
        );
    }

    private int getGridX() {
        return (width - COLUMNS * CELL_SIZE) / 2;
    }

    private int getGridY() {
        return Math.max(32, height / 2 - 78);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            page = Math.max(0, page - 1);
            initGui();

        } else if (button.id == 1) {
            mc.displayGuiScreen(
                    new GuiDynamicTraderMenu(npcEntityId)
            );

        } else if (button.id == 2) {
            page = Math.min(getPageCount() - 1, page + 1);
            initGui();
        }
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        if (mouseButton == 0 && role != null) {
            int gridX = getGridX();
            int gridY = getGridY();

            boolean insideGrid = mouseX >= gridX
                    && mouseX < gridX + COLUMNS * CELL_SIZE
                    && mouseY >= gridY
                    && mouseY < gridY + ROWS * CELL_SIZE;

            if (insideGrid) {
                int col = (mouseX - gridX) / CELL_SIZE;
                int row = (mouseY - gridY) / CELL_SIZE;
                int insideCellX = (mouseX - gridX) % CELL_SIZE;
                int insideCellY = (mouseY - gridY) % CELL_SIZE;

                if (insideCellX < 20 && insideCellY < 20) {
                    int offerIndex =
                            page * PAGE_SIZE + row * COLUMNS + col;

                    List<NpcTradeConfig> offers =
                            role.getDynamicOffers();

                    if (offerIndex >= 0 && offerIndex < offers.size()) {
                        NpcTradeConfig offer = offers.get(offerIndex);

                        if (offer != null && offer.isValid()) {
                            mc.displayGuiScreen(
                                    new GuiDynamicTraderConfirm(
                                            npcEntityId,
                                            page,
                                            offer
                                    )
                            );

                            return;
                        }
                    }
                }
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
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
                I18n.translateToLocal("cheba.trader.buy.title"),
                width / 2,
                12,
                0xFFFFFF
        );

        if (role == null) {
            drawCenteredString(
                    fontRenderer,
                    I18n.translateToLocal("cheba.trader.buy.load_error"),
                    width / 2,
                    height / 2,
                    0xFF5555
            );

            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        List<NpcTradeConfig> offers = role.getDynamicOffers();

        int start = page * PAGE_SIZE;
        int end = Math.min(offers.size(), start + PAGE_SIZE);
        int gridX = getGridX();
        int gridY = getGridY();

        for (int i = start; i < end; i++) {
            int localIndex = i - start;
            int x = gridX + (localIndex % COLUMNS) * CELL_SIZE;
            int y = gridY + (localIndex / COLUMNS) * CELL_SIZE;

            drawRect(x, y, x + 20, y + 20, 0xFF555555);
            drawRect(x + 1, y + 1, x + 19, y + 19, 0xFF202020);

            NpcTradeConfig offer = offers.get(i);
            ItemStack result = offer.getResult();

            if (result.isEmpty()) {
                continue;
            }

            RenderHelper.enableGUIStandardItemLighting();

            itemRender.renderItemAndEffectIntoGUI(
                    result,
                    x + 2,
                    y + 2
            );

            itemRender.renderItemOverlays(
                    fontRenderer,
                    result,
                    x + 2,
                    y + 2
            );

            RenderHelper.disableStandardItemLighting();
        }

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocalFormatted(
                        "cheba.trader.page",
                        page + 1,
                        getPageCount()
                ),
                width / 2,
                getGridY() + ROWS * CELL_SIZE + 31,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);

        int hoveredIndex = getHoveredOfferIndex(mouseX, mouseY);

        if (hoveredIndex >= 0 && hoveredIndex < offers.size()) {
            NpcTradeConfig offer = offers.get(hoveredIndex);
            ItemStack result = offer.getResult();

            if (!result.isEmpty()) {
                drawOfferTooltip(
                        offer,
                        result,
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private int getHoveredOfferIndex(int mouseX, int mouseY) {
        int gridX = getGridX();
        int gridY = getGridY();

        if (mouseX < gridX
                || mouseX >= gridX + COLUMNS * CELL_SIZE
                || mouseY < gridY
                || mouseY >= gridY + ROWS * CELL_SIZE) {
            return -1;
        }

        int relativeX = mouseX - gridX;
        int relativeY = mouseY - gridY;

        int col = relativeX / CELL_SIZE;
        int row = relativeY / CELL_SIZE;

        if (relativeX % CELL_SIZE >= 20
                || relativeY % CELL_SIZE >= 20) {
            return -1;
        }

        return page * PAGE_SIZE + row * COLUMNS + col;
    }

    private void drawOfferTooltip(
            NpcTradeConfig offer,
            ItemStack result,
            int mouseX,
            int mouseY
    ) {
        List<String> lines = new ArrayList<>();

        boolean advanced = mc.gameSettings.advancedItemTooltips;

        lines.addAll(result.getTooltip(
                mc.player,
                advanced
                        ? ITooltipFlag.TooltipFlags.ADVANCED
                        : ITooltipFlag.TooltipFlags.NORMAL
        ));

        lines.add(TextFormatting.GRAY + I18n.translateToLocalFormatted(
                "cheba.trader.quantity",
                result.getCount()
        ));

        if (offer.getPriceMoney() > 0L) {
            lines.add(TextFormatting.GOLD + I18n.translateToLocalFormatted(
                    "cheba.trader.price",
                    offer.getPriceMoney()
            ));
        }

        if (offer.getSellPriceMoney() > 0L) {
            lines.add(TextFormatting.GREEN + I18n.translateToLocalFormatted(
                    "cheba.trader.editor.sell_price_tooltip",
                    offer.getSellPriceMoney()
            ));
        }

        for (ItemStack priceItem : offer.getPriceItems()) {
            if (priceItem != null && !priceItem.isEmpty()) {
                lines.add(TextFormatting.GRAY + priceItem.getDisplayName()
                        + " x" + priceItem.getCount());
            }
        }

        int tooltipY = mouseY;

        if (mouseY >= getGridY() + (ROWS - 1) * CELL_SIZE) {
            int estimatedHeight = lines.size() * 10 + 8;
            tooltipY = mouseY - estimatedHeight;
        }

        drawHoveringText(lines, mouseX, tooltipY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
