package com.cheba.gui;

import com.cheba.trader.TraderPreset;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.translation.I18n;

/**
 * Главное меню независимого торгового профиля.
 */
@SuppressWarnings("deprecation")
public class GuiDynamicTraderMenu extends GuiScreen {

    private final TraderClientSession session;
    private final String profileName;
    private final TraderPreset preset;

    public GuiDynamicTraderMenu(
            TraderClientSession session,
            String profileName,
            TraderPreset preset
    ) {
        this.session = session;
        this.profileName = profileName;
        this.preset = preset;
    }

    @Override
    public void initGui() {
        session.completeTransition();
        buttonList.clear();

        int x = width / 2 - 80;
        int y = height / 2 - 10;

        buttonList.add(new GuiButton(
                0,
                x,
                y,
                160,
                20,
                I18n.translateToLocal("cheba.trader.buy")
        ));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
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
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode)
            throws java.io.IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        session.onScreenClosed();
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
                profileName,
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
