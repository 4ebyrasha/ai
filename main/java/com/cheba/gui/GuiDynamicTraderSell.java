package com.cheba.gui;

import com.cheba.container.TraderSellContainer;
import com.cheba.trader.TraderItemProtection;
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

    private static final int ESCROW_SIZE = 25;

    private final ItemStack[] cachedEscrowStacks =
            new ItemStack[ESCROW_SIZE];

    private final long[] cachedUnitPrices =
            new long[ESCROW_SIZE];

    private RoleTrader cachedPriceRole;

    private long cachedPriceRevision = Long.MIN_VALUE;

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

    private void refreshSellPriceCache(
            RoleTrader role,
            IInventory escrow
    ) {
        long revision = role == null
                ? Long.MIN_VALUE
                : role.getTradeConfigRevision();

        boolean catalogChanged = role != cachedPriceRole
                || revision != cachedPriceRevision;

        int slots = Math.min(
                ESCROW_SIZE,
                escrow.getSizeInventory()
        );

        for (int slot = 0; slot < slots; slot++) {
            ItemStack current = escrow.getStackInSlot(slot);
            ItemStack cached = cachedEscrowStacks[slot];

            if (!catalogChanged
                    && cached != null
                    && sameStackForCache(current, cached)) {
                continue;
            }

            cachedEscrowStacks[slot] = current.isEmpty()
                    ? ItemStack.EMPTY
                    : current.copy();

            long unitPrice = 0L;

            if (role != null
                    && !current.isEmpty()
                    && !TraderItemProtection.isProtected(player, current)) {
                unitPrice = TraderSellService.getSellPrice(role, current);
            }

            cachedUnitPrices[slot] = unitPrice;
        }

        // На случай если размер буфера когда либо изменится
        for (int slot = slots; slot < ESCROW_SIZE; slot++) {
            cachedEscrowStacks[slot] = ItemStack.EMPTY;
            cachedUnitPrices[slot] = 0L;
        }

        cachedPriceRole = role;
        cachedPriceRevision = revision;
    }

    private boolean sameStackForCache(
            ItemStack first,
            ItemStack second
    ) {
        if (first == null || second == null) {
            return first == second;
        }

        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }

        return first.getCount() == second.getCount()
                && ItemStack.areItemsEqual(first, second)
                && ItemStack.areItemStackTagsEqual(first, second);
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

        // Основной инвентарь игрока.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiLeft + 13 + col * 18,
                        guiTop + 122 + row * 18
                );
            }
        }

        // Панель быстрого доступа.
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

        refreshSellPriceCache(role, escrow);

        int visible = 0;
        int totalItems = 0;
        int unsellableItems = 0;
        long total = 0L;

        int slots = Math.min(
                ESCROW_SIZE,
                escrow.getSizeInventory()
        );

        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = escrow.getStackInSlot(slot);

            if (stack.isEmpty()) {
                continue;
            }

            totalItems++;

            long unitPrice = cachedUnitPrices[slot];
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
                    I18n.translateToLocal("cheba.trader.sell.empty_buffer"),
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

        String totalText = I18n.translateToLocalFormatted(
                "cheba.trader.sell.total",
                total
        );

        drawString(
                fontRenderer,
                fontRenderer.trimStringToWidth(totalText, textWidth),
                textX,
                guiTop + 99,
                total > 0L ? 0x55FF55 : 0xFFFFFF
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
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF777777);
        drawRect(x, y, x + 16, y + 16, 0xFF303030);
    }

    @Override
    public void save() {
        // Буфером продажи управляет серверный контейнер
    }
}
