
package com.cheba.gui;

import com.cheba.container.TraderSellContainer;
import com.cheba.trader.TraderSellService;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiContainerNPCInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

@SuppressWarnings("deprecation")
public class GuiDynamicTraderSell extends GuiContainerNPCInterface {

    private static final int MAX_VISIBLE_ITEMS = 7;

    private final TraderSellContainer container;

    public GuiDynamicTraderSell(
            EntityNPCInterface npc,
            TraderSellContainer container
    ) {
        super(npc, container);

        this.container = container;
        this.xSize = 230;
        this.ySize = 225;
        this.closeOnEsc = true;
        this.drawDefaultBackground = true;
    }

    @Override
    public void initGui() {
        super.initGui();

        buttonList.add(new GuiButton(
                0,
                guiLeft + 12,
                guiTop + ySize - 23,
                96,
                20,
                I18n.translateToLocal("cheba.trader.menu")
        ));

        buttonList.add(new GuiButton(
                1,
                guiLeft + 122,
                guiTop + ySize - 23,
                96,
                20,
                I18n.translateToLocal("cheba.trader.sell.execute")
        ));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button.id == 0 && npc != null) {
            int npcEntityId = npc.getEntityId();

            /*
             * При закрытии серверный контейнер вернёт предметы,
             * которые остались в буфере.
             */
            player.closeScreen();

            mc.displayGuiScreen(
                    new GuiDynamicTraderMenu(npcEntityId)
            );

            return;
        }

        if (button.id == 1 && npc != null) {
            Client.sendData(
                    EnumPacketServer.TraderSell,
                    npc.getEntityId()
            );

            return;
        }

        super.actionPerformed(button);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(
            float partialTicks,
            int mouseX,
            int mouseY
    ) {
        drawDefaultBackground();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        drawRect(
                guiLeft,
                guiTop,
                guiLeft + xSize,
                guiTop + ySize,
                0xEE202020
        );

        drawCenteredString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.sell.title"),
                guiLeft + xSize / 2,
                guiTop - 12,
                0xFFFFFF
        );

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.sell.buffer"),
                guiLeft + 23,
                guiTop - 1,
                0xFFFFFF
        );

        for (int i = 0; i < 25; i++) {
            int x = guiLeft + 23 + (i % 5) * 18;
            int y = guiTop + 10 + (i / 5) * 18;

            drawSlotFrame(x, y);
        }

        /*
         * Основной инвентарь игрока.
         */
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiLeft + 13 + col * 18,
                        guiTop + 122 + row * 18
                );
            }
        }

        /*
         * Панель быстрого доступа.
         */
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiLeft + 13 + col * 18,
                    guiTop + 180
            );
        }

        drawBufferSummary();
    }

    private void drawBufferSummary() {
        int textX = guiLeft + 118;
        int textWidth = 103;

        drawString(
                fontRenderer,
                I18n.translateToLocal("cheba.trader.sell.contents"),
                textX,
                guiTop + 1,
                0xFFFFFF
        );

        RoleTrader role = npc != null
                && npc.roleInterface instanceof RoleTrader
                ? (RoleTrader) npc.roleInterface
                : null;

        IInventory escrow = container.getEscrowInventory();

        int visible = 0;
        int totalItems = 0;
        int unsellableItems = 0;
        long total = 0L;

        for (int slot = 0; slot < escrow.getSizeInventory(); slot++) {
            ItemStack stack = escrow.getStackInSlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            long unitPrice = role == null
                    ? 0L
                    : TraderSellService.getSellPrice(role, stack);

            boolean sellable = unitPrice > 0L;

            if (sellable) {
                long count = stack.getCount();

                if (count > (Long.MAX_VALUE - total) / unitPrice) {
                    total = Long.MAX_VALUE;
                } else {
                    total += unitPrice * count;
                }
            } else {
                unsellableItems++;
            }

            if (visible >= MAX_VISIBLE_ITEMS) {
                continue;
            }

            String line = stack.getDisplayName()
                    + " x"
                    + stack.getCount();

            line = fontRenderer.trimStringToWidth(
                    line,
                    textWidth
            );

            drawString(
                    fontRenderer,
                    line,
                    textX,
                    guiTop + 12 + visible * 10,
                    sellable ? 0x55FF55 : 0xFF5555
            );

            visible++;
        }

        if (totalItems == 0) {
            drawString(
                    fontRenderer,
                    I18n.translateToLocal(
                            "cheba.trader.sell.empty_buffer"
                    ),
                    textX,
                    guiTop + 12,
                    0xAAAAAA
            );

        } else if (totalItems > MAX_VISIBLE_ITEMS) {
            drawString(
                    fontRenderer,
                    I18n.translateToLocalFormatted(
                            "cheba.trader.sell.more",
                            totalItems - MAX_VISIBLE_ITEMS
                    ),
                    textX,
                    guiTop + 12 + MAX_VISIBLE_ITEMS * 10,
                    0xAAAAAA
            );
        }

        String totalText = unsellableItems == 0
                ? I18n.translateToLocalFormatted(
                "cheba.trader.sell.total",
                total
        )
                : I18n.translateToLocalFormatted(
                "cheba.trader.sell.total_partial",
                total
        );

        drawString(
                fontRenderer,
                fontRenderer.trimStringToWidth(
                        totalText,
                        textWidth
                ),
                textX,
                guiTop + 99,
                unsellableItems == 0 ? 0x55FF55 : 0xFFFF55
        );

        if (unsellableItems > 0) {
            drawString(
                    fontRenderer,
                    fontRenderer.trimStringToWidth(
                            I18n.translateToLocalFormatted(
                                    "cheba.trader.sell.unsellable_count",
                                    unsellableItems
                            ),
                            textWidth
                    ),
                    textX,
                    guiTop + 109,
                    0xFF5555
            );
        }
    }

    private void drawSlotFrame(int x, int y) {
        drawRect(
                x - 1,
                y - 1,
                x + 17,
                y + 17,
                0xFF777777
        );

        drawRect(
                x,
                y,
                x + 16,
                y + 16,
                0xFF303030
        );
    }

    @Override
    public void save() {
        // Предметы возвращает серверный контейнер.
    }
}
