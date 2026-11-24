package com.cheba.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;

/** Centralized management of independent trader catalogs. */
public class GuiTraderPresetManager extends GuiScreen implements GuiYesNoCallback {
    private final GuiScreen parent;
    private final List<String> names = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private GuiTextField nameField;
    private String selected;
    private String status = "";
    private int scroll;
    private boolean waiting;

    public GuiTraderPresetManager(NBTTagCompound data) {
        this(data, null);
    }

    public GuiTraderPresetManager(NBTTagCompound data, GuiScreen parent) {
        this.parent = parent;
        applyData(data);
    }

    public void onEditorClosed() {
        waiting = false;
        send("list");
    }

    public void applyData(NBTTagCompound data) {
        if (data == null) return;
        waiting = false;
        status = data.getString("Message");
        names.clear();
        labels.clear();
        if (data.hasKey("Presets", 9)) {
            NBTTagList list = data.getTagList("Presets", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entry = list.getCompoundTagAt(i);
                String name = entry.getString("Name");
                names.add(name);
                labels.add(name + "  (" + entry.getInteger("Offers") + ")");
            }
        }
        if (selected != null && !names.contains(selected)) selected = null;
        if (selected == null && !names.isEmpty()) selected = names.get(0);
        scroll = Math.min(scroll, Math.max(0, names.size() - 9));
    }

    @Override
    public void initGui() {
        int x = width / 2 - 190, y = height / 2 - 125;
        nameField = new GuiTextField(0, fontRenderer, x + 205, y + 35, 175, 20);
        nameField.setMaxStringLength(128);
        buttonList.clear();
        buttonList.add(new GuiButton(0, x + 205, y + 70, 175, 20, "Создать"));
        buttonList.add(new GuiButton(1, x + 205, y + 94, 175, 20, "Переименовать"));
        buttonList.add(new GuiButton(2, x + 205, y + 118, 175, 20, "Удалить"));
        buttonList.add(new GuiButton(3, x + 205, y + 154, 175, 20, "Открыть редактор"));
        buttonList.add(new GuiButton(4, x + 205, y + 218, 175, 20, "Назад"));
    }

    private void send(String action) {
        if (waiting) return;
        NBTTagCompound request = new NBTTagCompound();
        request.setString("Action", action);
        request.setString("Name", selected == null ? "" : selected);
        request.setString("NewName", nameField.getText().trim());
        waiting = true;
        Client.sendData(EnumPacketServer.TraderPresetManager, request);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 4) {
            mc.displayGuiScreen(parent);
        } else if (button.id == 0) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                status = "Введите имя магазина.";
                return;
            }
            selected = name;
            send("create");
        } else if (button.id == 1 && selected != null) {
            send("rename");
        } else if (button.id == 2 && selected != null) {
            mc.displayGuiScreen(new GuiYesNo(this, "Удалить магазин " + selected + "?",
                    "Удаление невозможно, пока магазин назначен NPC.", 2));
        } else if (button.id == 3 && selected != null) {
            send("edit");
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result && id == 2) send("delete");
        else mc.displayGuiScreen(this);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        int x = width / 2 - 190, y = height / 2 - 125;
        if (mouseX >= x + 8 && mouseX <= x + 192
                && mouseY >= y + 30 && mouseY < y + 210) {
            int index = (mouseY - (y + 30)) / 20 + scroll;
            if (index >= 0 && index < names.size()) {
                selected = names.get(index);
                nameField.setText(selected);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = org.lwjgl.input.Mouse.getEventDWheel();
        if (wheel != 0) {
            scroll = Math.max(0, Math.min(Math.max(0, names.size() - 9),
                    scroll + (wheel > 0 ? -1 : 1)));
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        nameField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        nameField.updateCursorCounter();
        for (GuiButton button : buttonList) button.enabled = !waiting;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int x = width / 2 - 190, y = height / 2 - 125;
        drawCenteredString(fontRenderer, "Магазины", width / 2, y + 8, 0xFFFFFF);
        for (int i = 0; i < 9 && i + scroll < labels.size(); i++) {
            String name = names.get(i + scroll);
            drawString(fontRenderer, labels.get(i + scroll), x + 10, y + 34 + i * 20,
                    name.equals(selected) ? 0xFFFF55 : 0xFFFFFF);
        }
        drawString(fontRenderer, "Имя магазина", x + 205, y + 20, 0xFFFFFF);
        nameField.drawTextBox();
        if (!status.isEmpty()) drawString(fontRenderer, status, x + 10, y + 225, 0xFFAA55);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
