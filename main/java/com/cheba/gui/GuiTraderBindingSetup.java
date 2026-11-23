package com.cheba.gui;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Configures an NPC's independent trader profile and catalog binding.
 * Existing names are selected by entering their exact name; unknown names
 * are created when the configuration is saved.
 */
public class GuiTraderBindingSetup extends GuiScreen {

    private final GuiScreen parent;

    private GuiTextField profileField;
    private GuiTextField presetField;
    private GuiTextField limitGroupField;

    private boolean sharedLimits;
    private boolean loadRequested;
    private final List<String> profileNames = new ArrayList<>();
    private final List<String> presetNames = new ArrayList<>();
    private String statusMessage = "";

    public GuiTraderBindingSetup(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();

        int left = (width - 350) / 2;
        int top = (height - 190) / 2;

        profileField = createField(0, left + 105, top + 38, 170);
        presetField = createField(1, left + 105, top + 68, 170);
        limitGroupField = createField(2, left + 105, top + 98, 225);

        buttonList.add(new GuiButton(0, left + 10, top + 145, 92, 20, "Save"));
        buttonList.add(new GuiButton(1, left + 108, top + 145, 112, 20, "Save and edit"));
        buttonList.add(new GuiButton(2, left + 226, top + 145, 104, 20, "Cancel"));
        buttonList.add(new GuiButton(4, left + 280, top + 38, 20, 20, "<"));
        buttonList.add(new GuiButton(5, left + 304, top + 38, 20, 20, ">"));
        buttonList.add(new GuiButton(6, left + 280, top + 68, 20, 20, "<"));
        buttonList.add(new GuiButton(7, left + 304, top + 68, 20, 20, ">"));
        updateSharedButton();

        if (!loadRequested) {
            loadRequested = true;
            NBTTagCompound request = new NBTTagCompound();
            request.setString("Action", "load");
            Client.sendData(EnumPacketServer.TraderBindingSetup, request);
        }
    }

    private GuiTextField createField(int id, int x, int y, int fieldWidth) {
        GuiTextField field = new GuiTextField(id, fontRenderer, x, y, fieldWidth, 20);
        field.setMaxStringLength(128);
        return field;
    }

    private void updateSharedButton() {
        int left = (width - 350) / 2;
        int top = (height - 190) / 2;
        GuiButton button = new GuiButton(
                3, left + 10, top + 118, 110, 20,
                "Shared limits: " + (sharedLimits ? "ON" : "OFF")
        );

        for (int i = 0; i < buttonList.size(); i++) {
            if (buttonList.get(i).id == 3) {
                buttonList.set(i, button);
                return;
            }
        }

        buttonList.add(button);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 0 || button.id == 1) {
            save(button.id == 1);
        } else if (button.id == 2) {
            mc.displayGuiScreen(parent);
        } else if (button.id == 3) {
            sharedLimits = !sharedLimits;
            updateSharedButton();
        } else if (button.id == 4) {
            selectName(profileNames, profileField, -1);
        } else if (button.id == 5) {
            selectName(profileNames, profileField, 1);
        } else if (button.id == 6) {
            selectName(presetNames, presetField, -1);
        } else if (button.id == 7) {
            selectName(presetNames, presetField, 1);
        }
    }

    private void selectName(List<String> names, GuiTextField field, int direction) {
        if (names.isEmpty()) {
            return;
        }

        int current = -1;
        String value = field.getText().trim();
        for (int i = 0; i < names.size(); i++) {
            if (names.get(i).equalsIgnoreCase(value)) {
                current = i;
                break;
            }
        }

        int next = (current + direction + names.size()) % names.size();
        field.setText(names.get(next));
    }

    private void save(boolean openEditor) {
        String profileName = profileField.getText().trim();
        String presetName = presetField.getText().trim();
        String groupName = limitGroupField.getText().trim();

        if (profileName.isEmpty()) {
            statusMessage = "Profile name is required.";
            return;
        }

        if (openEditor && presetName.isEmpty()) {
            statusMessage = "Enter a catalog name before opening its editor.";
            return;
        }

        if (sharedLimits && groupName.isEmpty()) {
            statusMessage = "Enter a limit group for shared limits.";
            return;
        }

        NBTTagCompound request = new NBTTagCompound();
        request.setString("Action", "save");
        request.setString("ProfileName", profileName);
        request.setString("PresetName", presetName);
        request.setString("LimitGroupName", groupName);
        request.setBoolean("SharedLimits", sharedLimits);
        request.setBoolean("OpenEditor", openEditor);

        statusMessage = "Saving...";
        Client.sendData(EnumPacketServer.TraderBindingSetup, request);
    }

    public void onSetupResult(NBTTagCompound data) {
        boolean success = data.getBoolean("Success");
        statusMessage = data.getString("Message");

        if (!success) {
            return;
        }

        if ("load".equals(data.getString("Action"))) {
            profileField.setText(data.getString("ProfileName"));
            presetField.setText(data.getString("PresetName"));
            limitGroupField.setText(data.getString("LimitGroupName"));
            sharedLimits = data.getBoolean("SharedLimits");

            profileNames.clear();
            presetNames.clear();
            readNames(data.getTagList("Profiles", 8), profileNames);
            readNames(data.getTagList("Presets", 8), presetNames);

            updateSharedButton();
            return;
        }

        if (data.getBoolean("OpenEditor") && data.hasKey("Preset", 10)) {
            mc.displayGuiScreen(new GuiTraderPresetEditor(
                    com.cheba.trader.TraderPreset.readFromNBT(
                            data.getCompoundTag("Preset")
                    )
            ));
        }
    }

    private void readNames(NBTTagList tags, List<String> destination) {
        for (int i = 0; i < tags.tagCount(); i++) {
            String name = tags.getStringTagAt(i);
            if (!name.isEmpty()) {
                destination.add(name);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }

        profileField.textboxKeyTyped(typedChar, keyCode);
        presetField.textboxKeyTyped(typedChar, keyCode);
        limitGroupField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        profileField.mouseClicked(mouseX, mouseY, mouseButton);
        presetField.mouseClicked(mouseX, mouseY, mouseButton);
        limitGroupField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        profileField.updateCursorCounter();
        presetField.updateCursorCounter();
        limitGroupField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int left = (width - 350) / 2;
        int top = (height - 190) / 2;

        drawCenteredString(fontRenderer, "NPC trader profile", width / 2, top + 8, 0xFFFFFF);
        fontRenderer.drawString("Profile", left + 10, top + 44, 0xFFFFFF);
        fontRenderer.drawString("Catalog", left + 10, top + 74, 0xFFFFFF);
        fontRenderer.drawString("Limit group", left + 10, top + 104, 0xFFFFFF);

        profileField.drawTextBox();
        presetField.drawTextBox();
        limitGroupField.drawTextBox();

        if (!statusMessage.isEmpty()) {
            drawCenteredString(fontRenderer, statusMessage, width / 2, top + 174, 0xE0E0E0);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}