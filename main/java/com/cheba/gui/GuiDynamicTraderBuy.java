package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;
import com.cheba.trader.TraderPreset;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Экран просмотра каталога торгового профиля.
 * <p>
 * Каталог отображается сеткой 9x6.
 * Позиции товаров определяются displaySlot, а не порядком в списке.
 */
@SuppressWarnings("deprecation")
public class GuiDynamicTraderBuy extends GuiScreen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 22;

    private final TraderClientSession session;
    private final TraderPreset preset;
    private final String profileName;

    private int page;

    public GuiDynamicTraderBuy(
            TraderClientSession session,
            String profileName,
            TraderPreset preset,
            int page
    ) {
        this.session = session;
        this.profileName = profileName;
        this.preset = preset;
        this.page = Math.max(0, page);
    }

    @Override
    public void initGui() {
        session.completeTransition();
        buttonList.clear();

        int controlsY = getGridY() + ROWS * CELL_SIZE + 8;

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

    private int getGridX() {
        return (width - COLUMNS * CELL_SIZE) / 2;
    }

    private int getGridY() {
        return Math.max(32, height / 2 - 78);
    }

    /**
     * Возвращает фактический номер слота товара.
     * Для старых товаров без displaySlot используется индекс списка.
     */
    private int getDisplaySlot(NpcTradeConfig offer, int index) {
        int slot = offer.getDisplaySlot();

        return slot >= 0 && slot < PAGE_SIZE
                ? slot
                : index;
    }

    private int getPageCount() {
        int maxSlot = -1;

        List<NpcTradeConfig> offers = preset.getOffers();

        for (int i = 0; i < offers.size(); i++) {
            NpcTradeConfig offer = offers.get(i);

            if (offer == null) {
                continue;
            }

            maxSlot = Math.max(
                    maxSlot,
                    getDisplaySlot(offer, i)
            );
        }

        return Math.max(
                1,
                maxSlot / PAGE_SIZE + 1
        );
    }

    /**
     * Находит товар по абсолютному номеру ячейки каталога.
     */
    private NpcTradeConfig getOfferAtSlot(int absoluteSlot) {
        List<NpcTradeConfig> offers = preset.getOffers();

        for (int i = 0; i < offers.size(); i++) {
            NpcTradeConfig offer = offers.get(i);

            if (offer != null
                    && getDisplaySlot(offer, i) == absoluteSlot) {
                return offer;
            }
        }

        return null;
    }

    @Override
    protected void actionPerformed(GuiButton button)
            throws IOException {

        if (button.id == 0) {
            page = Math.max(0, page - 1);
            initGui();

        } else if (button.id == 1) {
            session.beginTransition();

            mc.displayGuiScreen(
                    new GuiDynamicTraderMenu(
                            session,
                            preset.getName(),
                            preset
                    )
            );

        } else if (button.id == 2) {
            page = Math.min(
                    getPageCount() - 1,
                    page + 1
            );

            initGui();
        }
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        if (mouseButton == 0) {
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
                    int absoluteSlot = page * PAGE_SIZE
                            + row * COLUMNS
                            + col;

                    NpcTradeConfig offer = getOfferAtSlot(
                            absoluteSlot
                    );

                    if (offer != null && offer.isValid()) {
                        session.beginTransition();

                        mc.displayGuiScreen(
                                new GuiDynamicTraderConfirm(
                                        session,
                                        preset,
                                        page,
                                        offer
                                )
                        );

                        return;
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

        int gridX = getGridX();
        int gridY = getGridY();

        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            int x = gridX + (slot % COLUMNS) * CELL_SIZE;
            int y = gridY + (slot / COLUMNS) * CELL_SIZE;

            drawRect(x, y, x + 20, y + 20, 0xFF555555);
            drawRect(x + 1, y + 1, x + 19, y + 19, 0xFF202020);

            NpcTradeConfig offer = getOfferAtSlot(
                    page * PAGE_SIZE + slot
            );

            if (offer == null || !offer.isValid()) {
                continue;
            }

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
                gridY + ROWS * CELL_SIZE + 31,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);

        NpcTradeConfig hovered = getHoveredOffer(
                mouseX,
                mouseY
        );

        if (hovered != null) {
            ItemStack result = hovered.getResult();

            if (!result.isEmpty()) {
                drawOfferTooltip(
                        hovered,
                        result,
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private NpcTradeConfig getHoveredOffer(
            int mouseX,
            int mouseY
    ) {
        int gridX = getGridX();
        int gridY = getGridY();

        if (mouseX < gridX
                || mouseX >= gridX + COLUMNS * CELL_SIZE
                || mouseY < gridY
                || mouseY >= gridY + ROWS * CELL_SIZE) {
            return null;
        }

        int relativeX = mouseX - gridX;
        int relativeY = mouseY - gridY;

        if (relativeX % CELL_SIZE >= 20
                || relativeY % CELL_SIZE >= 20) {
            return null;
        }

        int col = relativeX / CELL_SIZE;
        int row = relativeY / CELL_SIZE;

        return getOfferAtSlot(
                page * PAGE_SIZE + row * COLUMNS + col
        );
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

        for (ItemStack priceItem : offer.getPriceItems()) {
            if (priceItem != null && !priceItem.isEmpty()) {
                lines.add(
                        TextFormatting.GRAY
                                + priceItem.getDisplayName()
                                + " x"
                                + priceItem.getCount()
                );
            }
        }

        drawHoveringText(lines, mouseX, mouseY);
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
