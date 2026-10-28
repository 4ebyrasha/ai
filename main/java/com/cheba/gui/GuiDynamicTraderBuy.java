package com.cheba.gui;

import com.cheba.trader.NpcTradeConfig;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;
import net.minecraft.util.text.translation.I18n;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiDynamicTraderBuy extends GuiScreen {

    private static final int COLUMNS = 9;
    private static final int ROWS = 6;
    private static final int PAGE_SIZE = COLUMNS * ROWS;
    private static final int CELL_SIZE = 22;
    private static final int MAX_PAGES = 20;

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
                0, width / 2 - 105, controlsY, 65, 20, I18n.translateToLocal("cheba.trader.back")
        ));

        buttonList.add(new GuiButton(
                1, width / 2 - 32, controlsY, 64, 20, I18n.translateToLocal("cheba.trader.menu")
        ));

        buttonList.add(new GuiButton(
                2, width / 2 + 40, controlsY, 65, 20, I18n.translateToLocal("cheba.trader.next")
        ));

        int pages = getPageCount();
        page = Math.min(page, pages - 1);
    }

    private int getPageCount() {
        int count = role == null ? 0 : role.getDynamicOffers().size();
        return Math.max(1, Math.min(MAX_PAGES,
                (count + PAGE_SIZE - 1) / PAGE_SIZE));
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
            mc.displayGuiScreen(new GuiDynamicTraderMenu(npcEntityId));
        } else if (button.id == 2) {
            page = Math.min(getPageCount() - 1, page + 1);
            initGui();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {

        if (mouseButton == 0 && role != null) {
            int col = (mouseX - getGridX()) / CELL_SIZE;
            int row = (mouseY - getGridY()) / CELL_SIZE;

            boolean insideGrid =
                    mouseX >= getGridX()
                            && mouseX < getGridX() + COLUMNS * CELL_SIZE
                            && mouseY >= getGridY()
                            && mouseY < getGridY() + ROWS * CELL_SIZE;

            if (insideGrid && col >= 0 && col < COLUMNS
                    && row >= 0 && row < ROWS) {

                int offerIndex = page * PAGE_SIZE + row * COLUMNS + col;

                if (offerIndex < role.getDynamicOffers().size()) {
                    NpcTradeConfig offer =
                            role.getDynamicOffers().get(offerIndex);

                    if (offer != null && offer.isValid()) {
                        mc.displayGuiScreen(
                                new GuiDynamicTraderConfirm(
                                        npcEntityId, page, offer
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
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        drawCenteredString(
                fontRenderer, I18n.translateToLocal("cheba.trader.buy.title"),
                width / 2, 12, 0xFFFFFF
        );

        if (role == null) {
            drawCenteredString(
                    fontRenderer,
                    I18n.translateToLocal("cheba.trader.buy.load_error"),
                    width / 2, height / 2, 0xFF5555
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

            if (result != null && !result.isEmpty()) {
                RenderHelper.enableGUIStandardItemLighting();
                itemRender.renderItemAndEffectIntoGUI(result, x + 2, y + 2);
                itemRender.renderItemOverlays(fontRenderer, result, x + 2, y + 2);
                RenderHelper.disableStandardItemLighting();

                if (mouseX >= x && mouseX < x + 20
                        && mouseY >= y && mouseY < y + 20) {
                    drawOfferTooltip(offer, result, mouseX, mouseY);
                }
            }
        }

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocalFormatted(
                        "cheba.trader.page",
                        page + 1,
                        getPageCount()
                ),
                width / 2, getGridY() + ROWS * CELL_SIZE + 31,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawOfferTooltip(
            NpcTradeConfig offer,
            ItemStack result,
            int mouseX,
            int mouseY
    ) {
        List<String> lines = new ArrayList<>();

        lines.add(result.getDisplayName());

        if (offer.getPriceMoney() > 0) {
            lines.add(
                    TextFormatting.GOLD
                            + I18n.translateToLocalFormatted(
                            "cheba.trader.price",
                            offer.getPriceMoney()
                    )
            );
        }

        for (ItemStack priceItem : offer.getPriceItems()) {
            if (priceItem != null && !priceItem.isEmpty()) {
                lines.add(priceItem.getDisplayName()
                        + " x" + priceItem.getCount());
            }
        }

        drawHoveringText(lines, mouseX, mouseY);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
