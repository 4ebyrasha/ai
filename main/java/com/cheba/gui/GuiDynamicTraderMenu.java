package com.cheba.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.translation.I18n;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumGuiType;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.entity.EntityNPCInterface;

import com.cheba.trader.TraderPreset;

import javax.annotation.Nonnull;

/**
 * Главное меню торговли.
 * <p>
 * Поддерживает два режима:
 * 1. Независимая торговля через TraderProfile.
 * 2. Старая торговля через NPC и RoleTrader.
 */
@SuppressWarnings("deprecation")
public class GuiDynamicTraderMenu extends GuiScreen {

    private final TraderClientSession session;
    private final String profileName;
    private final TraderPreset preset;
    private final int npcEntityId;

    /**
     * Создаёт меню независимого торгового профиля.
     */
    public GuiDynamicTraderMenu(
            TraderClientSession session,
            String profileName,
            TraderPreset preset
    ) {
        this.session = session;
        this.profileName = profileName;
        this.preset = preset;
        this.npcEntityId = -1;
    }

    /**
     * Создаёт старое меню торговли NPC.
     * <p>
     * Этот конструктор используется существующими GUI
     * и ClientProxy, поэтому сохраняется для совместимости.
     */
    public GuiDynamicTraderMenu(int npcEntityId) {
        this.session = null;
        this.profileName = null;
        this.preset = null;
        this.npcEntityId = npcEntityId;
    }

    @Override
    public void initGui() {
        if (session != null) {
            session.completeTransition();
        }

        buttonList.clear();

        int x = width / 2 - 80;
        int y = height / 2 - 10;

        if (session != null) {
            buttonList.add(new GuiButton(
                    0,
                    x,
                    y,
                    160,
                    20,
                    I18n.translateToLocal("cheba.trader.buy")
            ));
            return;
        }

        buttonList.add(new GuiButton(
                0,
                x,
                y - 12,
                160,
                20,
                I18n.translateToLocal("cheba.trader.buy")
        ));

        buttonList.add(new GuiButton(
                1,
                x,
                y + 14,
                160,
                20,
                I18n.translateToLocal("cheba.trader.sell")
        ));
    }

    @Override
    protected void actionPerformed(@Nonnull GuiButton button) {
        if (session != null) {
            if (button.id == 0) {
                session.beginTransition();

                mc.displayGuiScreen(
                        new GuiDynamicTraderBuy(
                                session,
                                profileName,
                                preset,
                                0
                        )
                );
            }

            return;
        }

        if (button.id == 0) {
            Client.sendData(
                    EnumPacketServer.Gui,
                    EnumGuiType.PlayerTrader.ordinal(),
                    0,
                    0,
                    0
            );

        } else if (button.id == 1) {
            Client.sendData(
                    EnumPacketServer.TraderOpenSell,
                    npcEntityId
            );
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
            throws java.io.IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        if (session != null) {
            session.onScreenClosed();
        }
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {
        drawDefaultBackground();

        String title = profileName;

        if (session == null) {
            Entity entity = mc.world == null
                    ? null
                    : mc.world.getEntityByID(npcEntityId);

            title = entity instanceof EntityNPCInterface
                    ? entity.getName()
                    : I18n.translateToLocal("cheba.trader.title");
        }

        drawCenteredString(
                fontRenderer,
                title == null ? "" : title,
                width / 2,
                height / 2 - 55,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
