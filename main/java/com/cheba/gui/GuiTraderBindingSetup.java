package com.cheba.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

import java.io.IOException;

/** NPC-side settings: catalog binding, limit group and GUI visibility. */
public class GuiTraderBindingSetup extends GuiScreen {
    private final GuiScreen parent;
    private GuiTextField shopNameField;
    private GuiTextField limitGroupField;
    private boolean opensGui = true;
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
        int left = (width - 360) / 2, top = (height - 210) / 2;
        shopNameField = new GuiTextField(0, fontRenderer, left + 50, top + 43, 260, 20);
        shopNameField.setMaxStringLength(128);
        limitGroupField = new GuiTextField(1, fontRenderer, left + 50, top + 91, 260, 20);
        limitGroupField.setMaxStringLength(128);
        buttonList.add(new GuiButton(0, left + 35, top + 150, 120, 20, "Сохранить"));
        buttonList.add(new GuiButton(1, left + 205, top + 150, 120, 20, "Отмена"));
        buttonList.add(new GuiButton(2, left + 35, top + 122, 120, 20,
                opensGui ? "Открывает GUI: Да" : "Открывает GUI: Нет"));
        buttonList.add(new GuiButton(3, left + 205, top + 122, 120, 20, "Копировать ID"));
        if (!loadRequested) {
            loadRequested = true;
            NBTTagCompound request = new NBTTagCompound();
            request.setString("Action", "load");
            Client.sendData(EnumPacketServer.TraderBindingSetup, request);
        }
    }

    private void updateToggle() {
        for (GuiButton button : buttonList) if (button.id == 2) {
            button.displayString = opensGui ? "Открывает GUI: Да" : "Открывает GUI: Нет";
            return;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0) {
            if (saving) return;
            NBTTagCompound request = new NBTTagCompound();
            request.setString("Action", "save");
            request.setString("ShopName", shopNameField.getText().trim());
            request.setString("LimitGroupName", limitGroupField.getText().trim());
            request.setBoolean("OpensGui", opensGui);
            saving = true;
            statusMessage = "Сохранение...";
            Client.sendData(EnumPacketServer.TraderBindingSetup, request);
        } else if (button.id == 1) {
            mc.displayGuiScreen(parent);
        } else if (button.id == 2) {
            opensGui = !opensGui;
            updateToggle();
        } else if (button.id == 3 && !profileId.isEmpty()) {
            GuiScreen.setClipboardString(profileId);
            statusMessage = "ID скопирован.";
        }
    }

    public void onSetupResult(NBTTagCompound data) {
        saving = false;
        statusMessage = data.getString("Message");
        if (!data.getBoolean("Success")) return;
        profileId = data.getString("ProfileId");
        if ("load".equals(data.getString("Action"))) {
            shopNameField.setText(data.getString("ShopName"));
            limitGroupField.setText(data.getString("LimitGroupName"));
            opensGui = !data.hasKey("OpensGui", 1) || data.getBoolean("OpensGui");
            updateToggle();
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
        if (!shopNameField.textboxKeyTyped(typedChar, keyCode)
                && !limitGroupField.textboxKeyTyped(typedChar, keyCode)) super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        shopNameField.mouseClicked(mouseX, mouseY, mouseButton);
        limitGroupField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        shopNameField.updateCursorCounter();
        limitGroupField.updateCursorCounter();
        for (GuiButton button : buttonList) button.enabled = !saving && (button.id != 3 || !profileId.isEmpty());
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int left = (width - 360) / 2, top = (height - 210) / 2;
        drawCenteredString(fontRenderer, "Профиль торговца NPC", width / 2, top + 10, 0xFFFFFF);
        drawString(fontRenderer, "Магазин (имя из списка «Магазины»)", left + 50, top + 30, 0xFFFFFF);
        shopNameField.drawTextBox();
        drawString(fontRenderer, "Группа лимитов (пусто — общая для магазина)",
                left + 10, top + 77, 0xFFFFFF);
        limitGroupField.drawTextBox();
        drawString(fontRenderer, "ID профиля: " + (profileId.isEmpty() ? "не создан" : profileId),
                left + 10, top + 165, 0xAAAAAA);
        if (!statusMessage.isEmpty()) drawCenteredString(fontRenderer, statusMessage, width / 2, top + 194, 0xE0E0E0);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
