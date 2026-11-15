package com.cheba.client.gui;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcButtonYesNo;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.entity.data.DataRanged;

public class SubGuiNpcRangeAdvanced extends SubGuiInterface implements ITextfieldListener {
    private final DataRanged ranged;

    public SubGuiNpcRangeAdvanced(DataRanged ranged) {
        this.ranged = ranged;
        this.setBackground("menubg.png");
        this.xSize = 256;
        this.ySize = 246;
        this.closeOnEsc = true;
    }

    public void initGui() {
        super.initGui();

        this.addLabel(new GuiNpcLabel(1, "npc.fireRateRPM",
                this.guiLeft + 5, this.guiTop + 15));
        this.addTextField(new GuiNpcTextField(1, this, this.fontRenderer,
                this.guiLeft + 165, this.guiTop + 10, 60, 18,
                this.ranged.getFireRateRPM() + ""));
        this.getTextField(1).numbersOnly = true;
        this.getTextField(1).setMinMaxDefault(1, 1200, 40);

        this.addLabel(new GuiNpcLabel(12, "npc.magazineSize",
                this.guiLeft + 5, this.guiTop + 45));
        this.addTextField(new GuiNpcTextField(12, this, this.fontRenderer,
                this.guiLeft + 165, this.guiTop + 40, 60, 18,
                this.ranged.getMagazineSize() + ""));
        this.getTextField(12).numbersOnly = true;
        this.getTextField(12).setMinMaxDefault(1, 999, 30);

        this.addLabel(new GuiNpcLabel(16, "npc.reloadDuration",
                this.guiLeft + 5, this.guiTop + 75));
        this.addTextField(new GuiNpcTextField(16, this, this.fontRenderer,
                this.guiLeft + 165, this.guiTop + 70, 60, 18,
                this.ranged.getReloadDurationSeconds() + ""));
        this.getTextField(16).decimalNumbers = true;

        this.addLabel(new GuiNpcLabel(15, "npc.reloadAfterDeAggro",
                this.guiLeft + 5, this.guiTop + 105));
        this.addTextField(new GuiNpcTextField(15, this, this.fontRenderer,
                this.guiLeft + 165, this.guiTop + 100, 60, 18,
                this.ranged.getReloadAfterDeAggroSeconds() + ""));
        this.getTextField(15).decimalNumbers = true;

        this.addLabel(new GuiNpcLabel(18, "npc.reaction",
                this.guiLeft + 5, this.guiTop + 135));
        this.addTextField(new GuiNpcTextField(18, this, this.fontRenderer,
                this.guiLeft + 165, this.guiTop + 130, 60, 18,
                this.ranged.getReactionSeconds() + ""));
        this.getTextField(18).decimalNumbers = true;

        this.addLabel(new GuiNpcLabel(14, "npc.ignorePlayerIFrames.ranged",
                this.guiLeft + 5, this.guiTop + 165));
        this.addButton(new GuiNpcButtonYesNo(14,
                this.guiLeft + 165, this.guiTop + 160,
                this.ranged.getIgnorePlayerIFrames()));

        this.addLabel(new GuiNpcLabel(17, "npc.hideEffectParticles.ranged",
                this.guiLeft + 5, this.guiTop + 195));
        this.addButton(new GuiNpcButtonYesNo(17,
                this.guiLeft + 165, this.guiTop + 190,
                this.ranged.getHideEffectParticles()));

        this.addButton(new GuiNpcButton(66,
                this.guiLeft + 190, this.guiTop + 220, 60, 20, "gui.done"));
    }

    private double parseSeconds(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public void unFocused(GuiNpcTextField textfield) {
        if (textfield.getId() == 1) {
            this.ranged.setFireRateRPM(textfield.getInteger());
        } else if (textfield.getId() == 12) {
            this.ranged.setMagazineSize(textfield.getInteger());
        } else if (textfield.getId() == 16) {
            this.ranged.setReloadDurationSeconds(
                    this.parseSeconds(textfield.getText(), this.ranged.getReloadDurationSeconds()));
            textfield.setText(this.ranged.getReloadDurationSeconds() + "");
        } else if (textfield.getId() == 15) {
            this.ranged.setReloadAfterDeAggroSeconds(
                    this.parseSeconds(textfield.getText(), this.ranged.getReloadAfterDeAggroSeconds()));
            textfield.setText(this.ranged.getReloadAfterDeAggroSeconds() + "");
        } else if (textfield.getId() == 18) {
            this.ranged.setReactionSeconds(
                    this.parseSeconds(textfield.getText(), this.ranged.getReactionSeconds()));
            textfield.setText(this.ranged.getReactionSeconds() + "");
        }
    }

    protected void actionPerformed(GuiButton guibutton) {
        int id = guibutton.id;

        if (id == 14) {
            this.ranged.setIgnorePlayerIFrames(
                    ((GuiNpcButtonYesNo) guibutton).getBoolean());
        } else if (id == 17) {
            this.ranged.setHideEffectParticles(
                    ((GuiNpcButtonYesNo) guibutton).getBoolean());
        } else if (id == 66) {
            this.close();
        }
    }
}
