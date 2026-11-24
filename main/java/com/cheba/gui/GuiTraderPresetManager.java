package com.cheba.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.constants.EnumPacketServer;

/** Shop management with the standard CustomNPCs scroll list. */
public class GuiTraderPresetManager extends GuiNPCInterface
        implements ICustomScrollListener, GuiYesNoCallback {

    private final GuiScreen parent;
    private final Map<String, String> shopLabels = new HashMap<>();
    private final Map<String, NBTTagCompound> references = new HashMap<>();
    private GuiCustomScroll scroll;
    private GuiTextField nameField;
    private String selectedShop;
    private String selectedReference;
    private String mode = "shops";
    private String status = "";
    private boolean waiting;

    public GuiTraderPresetManager(NBTTagCompound data) {
        this(data, null);
    }

    public GuiTraderPresetManager(NBTTagCompound data, GuiScreen parent) {
        super();
        this.parent = parent;
        this.xSize = 256;
        this.ySize = 222;
        this.title = "Магазины";
        setBackground("menubg.png");
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
        String responseMode = data.getString("Mode");
        mode = responseMode.isEmpty() ? "shops" : responseMode;

        if ("references".equals(mode)) {
            references.clear();
            if (data.hasKey("References", 9)) {
                NBTTagList list = data.getTagList("References", 10);
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound entry = list.getCompoundTagAt(i);
                    references.put(entry.getString("Label"), entry.copy());
                }
            }
            if (selectedReference == null || !references.containsKey(selectedReference)) {
                selectedReference = references.isEmpty()
                        ? null : references.keySet().iterator().next();
            }
        } else {
            shopLabels.clear();
            if (data.hasKey("Presets", 9)) {
                NBTTagList list = data.getTagList("Presets", 10);
                for (int i = 0; i < list.tagCount(); i++) {
                    NBTTagCompound entry = list.getCompoundTagAt(i);
                    String name = entry.getString("Name");
                    shopLabels.put(name + " (" + entry.getInteger("Offers") + ")", name);
                }
            }
            if (selectedShop == null || !shopLabels.containsValue(selectedShop)) {
                selectedShop = shopLabels.isEmpty()
                        ? null : shopLabels.values().iterator().next();
            }
        }

        refreshModeControls();
    }

    @Override
    public void initGui() {
        super.initGui();
        if (scroll == null) {
            scroll = new GuiCustomScroll(this, 0);
            scroll.setSize(165, 208);
        }
        scroll.guiLeft = guiLeft + 4;
        scroll.guiTop = guiTop + 4;
        addScroll(scroll);

        nameField = new GuiTextField(0, fontRenderer, guiLeft + 170, guiTop + 34, 82, 20);
        nameField.setMaxStringLength(128);

        buttonList.add(new GuiButton(0, guiLeft + 170, guiTop + 62, 82, 20, "Создать"));
        buttonList.add(new GuiButton(1, guiLeft + 170, guiTop + 84, 82, 20, "Переименовать"));
        buttonList.add(new GuiButton(2, guiLeft + 170, guiTop + 106, 82, 20, "Удалить"));
        buttonList.add(new GuiButton(3, guiLeft + 170, guiTop + 128, 82, 20, "Открыть"));
        buttonList.add(new GuiButton(5, guiLeft + 170, guiTop + 150, 82, 20, "Используют NPC"));
        buttonList.add(new GuiButton(6, guiLeft + 170, guiTop + 172, 82, 20, "Отвязать"));
        buttonList.add(new GuiButton(4, guiLeft + 170, guiTop + 196, 82, 20, "Назад"));

        refreshModeControls();
    }

    private void refreshModeControls() {
        boolean referencesMode = "references".equals(mode);

        if (nameField != null) nameField.setVisible(!referencesMode);
        setButtonVisible(0, !referencesMode);
        setButtonVisible(1, !referencesMode);
        setButtonVisible(2, !referencesMode);
        setButtonVisible(3, !referencesMode);
        setButtonVisible(5, !referencesMode);
        setButtonVisible(6, referencesMode);

        GuiButton backButton = findButton(4);
        if (backButton != null) {
            backButton.displayString = referencesMode ? "К магазинам" : "Назад";
        }

        if (scroll != null) {
            if (referencesMode) {
                scroll.setList(new ArrayList<>(references.keySet()));
                if (selectedReference != null) scroll.setSelected(selectedReference);
            } else {
                List<String> labels = new ArrayList<>(shopLabels.keySet());
                java.util.Collections.sort(labels, new noppes.npcs.util.NaturalOrderComparator());
                scroll.setList(labels);
                selectShopLabel();
            }
        }

        updateButtons();
    }

    private void setButtonVisible(int id, boolean visible) {
        GuiButton button = findButton(id);
        if (button != null) button.visible = visible;
    }

    private GuiButton findButton(int id) {
        for (GuiButton button : buttonList) if (button.id == id) return button;
        return null;
    }

    private void selectShopLabel() {
        if (selectedShop == null) return;
        for (Map.Entry<String, String> entry : shopLabels.entrySet()) {
            if (selectedShop.equals(entry.getValue())) {
                scroll.setSelected(entry.getKey());
                return;
            }
        }
    }

    private void updateButtons() {
        boolean referencesMode = "references".equals(mode);
        boolean selected = referencesMode
                ? selectedReference != null : selectedShop != null;

        for (GuiButton button : buttonList) {
            button.enabled = button.id == 4 || !waiting;
        }

        if (referencesMode) {
            NBTTagCompound selectedEntry = selectedReference == null
                    ? null : references.get(selectedReference);
            boolean loadedNpc = selectedEntry != null
                    && selectedEntry.getInteger("EntityId") >= 0;
            setButtonEnabled(6, !waiting && selected && loadedNpc);
        } else {
            setButtonEnabled(1, !waiting && selected);
            setButtonEnabled(2, !waiting && selected);
            setButtonEnabled(3, !waiting && selected);
            setButtonEnabled(5, !waiting && selected);
        }
    }

    private void setButtonEnabled(int id, boolean enabled) {
        GuiButton button = findButton(id);
        if (button != null) button.enabled = enabled;
    }

    private void send(String action) {
        if (waiting) return;
        NBTTagCompound request = new NBTTagCompound();
        request.setString("Action", action);
        request.setString("Name", selectedShop == null ? "" : selectedShop);
        request.setString("NewName", nameField == null ? "" : nameField.getText().trim());
        if ("detach".equals(action) && selectedReference != null) {
            NBTTagCompound entry = references.get(selectedReference);
            if (entry != null) {
                request.setString("ProfileId", entry.getString("ProfileId"));
                request.setInteger("EntityId", entry.getInteger("EntityId"));
                request.setInteger("Dimension", entry.getInteger("Dimension"));
            }
        }
        waiting = true;
        updateButtons();
        Client.sendData(EnumPacketServer.TraderPresetManager, request);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 4) {
            if ("references".equals(mode)) send("list");
            else mc.displayGuiScreen(parent);
        } else if (button.id == 0) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                status = "Введите имя магазина.";
                return;
            }
            selectedShop = name;
            send("create");
        } else if (button.id == 1 && selectedShop != null) {
            send("rename");
        } else if (button.id == 2 && selectedShop != null) {
            mc.displayGuiScreen(new GuiYesNo(this, "Удалить магазин " + selectedShop + "?",
                    "Сначала отвяжите всех NPC от этого магазина.", 2));
        } else if (button.id == 3 && selectedShop != null) {
            send("edit");
        } else if (button.id == 5 && selectedShop != null) {
            send("references");
        } else if (button.id == 6 && selectedReference != null) {
            NBTTagCompound entry = references.get(selectedReference);
            String warning = entry != null && entry.getInteger("EntityId") < 0
                    ? "NPC не загружен. Если он есть в выгруженном чанке, привязка там останется."
                    : "NPC потеряет привязку к этому торговому профилю.";
            mc.displayGuiScreen(new GuiYesNo(this, "Отвязать " + selectedReference + "?", warning, 6));
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        // Restore this manager before sending: the server response must update
        // this instance, not create a new manager with GuiYesNo as its parent.
        mc.displayGuiScreen(this);

        if (result && id == 2) {
            send("delete");
        } else if (result && id == 6) {
            send("detach");
        }
    }

    @Override
    public void scrollClicked(int mouseX, int mouseY, int mouseButton, GuiCustomScroll source) {
        String label = source.getSelected();
        if (label == null) return;
        if ("references".equals(mode)) selectedReference = label;
        else {
            selectedShop = shopLabels.get(label);
            if (selectedShop != null && nameField != null) nameField.setText(selectedShop);
        }
        updateButtons();
    }

    @Override
    public void scrollDoubleClicked(String value, GuiCustomScroll source) {
        if (!"references".equals(mode) && selectedShop != null) send("edit");
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (nameField != null && nameField.getVisible()) {
            nameField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1) {
            if ("references".equals(mode)) send("list");
            else mc.displayGuiScreen(parent);
            return;
        }
        if (nameField != null && nameField.getVisible()) {
            nameField.textboxKeyTyped(typedChar, keyCode);
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (nameField != null) nameField.updateCursorCounter();
        updateButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (nameField != null && nameField.getVisible()) {
            drawString(fontRenderer, "Имя магазина", guiLeft + 170, guiTop + 20, 0xFFFFFF);
            nameField.drawTextBox();
        }
        if (!status.isEmpty()) {
            drawString(fontRenderer, status, guiLeft + 4, guiTop + ySize - 10, 0xFFAA55);
        }
    }

    @Override
    public void save() {
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
