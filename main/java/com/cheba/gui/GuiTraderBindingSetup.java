package com.cheba.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

import java.io.IOException;

/** User-facing settings for an NPC shop; internal profile IDs are automatic. */
public class GuiTraderBindingSetup extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField shopNameField;
    private boolean sharedLimits;
    private boolean loadRequested;
    private boolean saving;
    private String profileId = "";
    private String statusMessage = "";

    public GuiTraderBindingSetup(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int left = (width - 360) / 2;
        int top = (height - 165) / 2;

        shopNameField = new GuiTextField(
                0, fontRenderer, left + 10, top + 42, 290, 20
        );
        shopNameField.setMaxStringLength(128);

        buttonList.add(new GuiButton(0, left + 10, top + 92, 110, 20, "Сохранить"));
        buttonList.add(new GuiButton(1, left + 190, top + 92, 110, 20, "Отмена"));
        buttonList.add(new GuiButton(3, left + 245, top + 119, 105, 20, "Копировать ID"));
        updateSharedButton();

        if (!loadRequested) {
            loadRequested = true;
            NBTTagCompound request = new NBTTagCompound();
            request.setString("Action", "load");
            Client.sendData(EnumPacketServer.TraderBindingSetup, request);
        }
    }

    private void updateSharedButton() {
        int left = (width - 310) / 2;
        int top = (height - 130) / 2;
        GuiButton replacement = new GuiButton(
                2, left + 10, top + 66, 170, 20,
                "Общие лимиты: " + (sharedLimits ? "ВКЛ" : "ВЫКЛ")
        );
        for (int i = 0; i < buttonList.size(); i++) {
            if (buttonList.get(i).id == 2) {
                buttonList.set(i, replacement);
                return;
            }
        }
        buttonList.add(replacement);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            save();
        } else if (button.id == 1) {
            mc.displayGuiScreen(parent);
        } else if (button.id == 2) {
            sharedLimits = !sharedLimits;
            updateSharedButton();
        } else if (button.id == 3 && !profileId.isEmpty()) {
            GuiScreen.setClipboardString(profileId);
            statusMessage = "ID скопирован.";
        }
    }

    private void save() {
        if (saving) return;
        NBTTagCompound request = new NBTTagCompound();
        request.setString("Action", "save");
        request.setString("ShopName", shopNameField.getText().trim());
        request.setBoolean("SharedLimits", sharedLimits);
        saving = true;
        statusMessage = "Сохранение...";
        Client.sendData(EnumPacketServer.TraderBindingSetup, request);
    }

    public void onSetupResult(NBTTagCompound data) {
        saving = false;
        statusMessage = data.getString("Message");
        if (!data.getBoolean("Success")) return;

        if (data.hasKey("ProfileId", 8)) {
            profileId = data.getString("ProfileId");
        }
        if ("load".equals(data.getString("Action"))) {
            shopNameField.setText(data.getString("ShopName"));
            sharedLimits = data.getBoolean("SharedLimits");
            updateSharedButton();
            return;
        }
        mc.displayGuiScreen(parent);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (!shopNameField.textboxKeyTyped(typedChar, keyCode)) {
            super.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
            throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        shopNameField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        shopNameField.updateCursorCounter();
        for (GuiButton button : buttonList) {
            button.enabled = !saving && (button.id != 3 || !profileId.isEmpty());
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = (width - 310) / 2;
        int top = (height - 130) / 2;

        drawCenteredString(fontRenderer, "Настройки магазина NPC",
                width / 2, top + 10, 0xFFFFFF);
        fontRenderer.drawString("Главное имя магазина (необязательно)",
                left + 10, top + 30, 0xFFFFFF);
        shopNameField.drawTextBox();
        fontRenderer.drawString("Пустое имя — отдельный магазин этого NPC.",
                left + 10, top + 55, 0xAAAAAA);
        fontRenderer.drawString("Одинаковое имя — общий каталог предложений.",
                left + 10, top + 78, 0xAAAAAA);
        fontRenderer.drawString("Trader ID: " + (profileId.isEmpty() ? "не создан" : profileId),
                left + 10, top + 124, 0xAAAAAA);

        if (!statusMessage.isEmpty()) {
            drawCenteredString(fontRenderer, statusMessage,
                    width / 2, top + 151, 0xE0E0E0);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
