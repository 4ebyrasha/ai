package com.cheba.gui;

import com.cheba.trader.NpcBuybackConfig;
import com.cheba.trader.NpcTradeConfig;
import com.cheba.trader.TraderPreset;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.client.Client;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.roles.RoleTrader;

import java.util.List;

/**
 * Shared data adapter for the legacy full-featured trader editor screens.
 * It keeps the old RoleTrader workflow intact while allowing the same
 * screens to edit an independent TraderPreset.
 */
public final class TraderEditorModel {
    private static TraderEditorModel activeModel;

    private final RoleTrader legacyRole;
    private final TraderPreset preset;
    private boolean saving;
    private boolean stale;
    private String status = "";

    public TraderEditorModel(RoleTrader role) {
        if (role == null) throw new IllegalArgumentException("RoleTrader cannot be null");
        legacyRole = role;
        preset = null;
        activeModel = this;
    }

    public TraderEditorModel(TraderPreset preset) {
        if (preset == null) throw new IllegalArgumentException("TraderPreset cannot be null");
        legacyRole = null;
        this.preset = TraderPreset.readFromNBT(preset.writeToNBT());
        activeModel = this;
    }

    public boolean isPresetMode() { return preset != null; }
    public boolean isSaving() { return saving; }
    public boolean isStale() { return stale; }
    public String getStatus() { return status; }
    public TraderPreset getPreset() { return preset; }

    public List<NpcTradeConfig> getDynamicOffers() {
        return isPresetMode() ? preset.getOffers() : legacyRole.getDynamicOffers();
    }

    public void addDynamicOffer(NpcTradeConfig offer) {
        if (isPresetMode()) preset.addOffer(offer);
        else legacyRole.addDynamicOffer(offer);
    }

    public boolean removeDynamicOffer(String id) {
        return isPresetMode() ? preset.removeOffer(id) : legacyRole.removeDynamicOffer(id);
    }

    public List<NpcBuybackConfig> getBuybackOffers() {
        return isPresetMode() ? preset.getBuybackOffers() : legacyRole.getBuybackOffers();
    }

    public boolean addBuybackOffer(NpcBuybackConfig offer) {
        return isPresetMode() ? preset.addBuybackOffer(offer) : legacyRole.addBuybackOffer(offer);
    }

    public boolean removeBuybackOffer(String id) {
        return isPresetMode() ? preset.removeBuybackOffer(id) : legacyRole.removeBuybackOffer(id);
    }

    public void markTradeConfigChanged() {
        if (!isPresetMode()) legacyRole.markTradeConfigChanged();
    }

    public void save() {
        if (saving || stale) return;
        if (!isPresetMode()) {
            legacyRole.markTradeConfigChanged();
            NBTTagCompound data = legacyRole.writeToNBT(new NBTTagCompound());
            data.setBoolean("ChebaSyncTraderCatalog", true);
            Client.sendData(EnumPacketServer.RoleSave, data);
            return;
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setLong("ExpectedRevision", preset.getRevision());
        data.setTag("Preset", preset.writeToNBT());
        Client.sendData(EnumPacketServer.TraderPresetEditorSave, data);
        saving = true;
        status = "Saving...";
    }

    public static void handleSaveResult(boolean success, String message, TraderPreset saved) {
        TraderEditorModel model = activeModel;
        if (model == null || !model.isPresetMode()) return;
        model.saving = false;
        model.status = message == null ? "" : message;
        if (success && saved != null) {
            model.preset.replaceContents(saved, saved.getRevision());
        }
    }

    public static void handleStale(String presetName, long revision) {
        TraderEditorModel model = activeModel;
        if (model == null || !model.isPresetMode()
                || presetName == null
                || !model.preset.getName().equalsIgnoreCase(presetName)
                || revision <= model.preset.getRevision()) return;
        model.stale = true;
        model.saving = false;
        model.status = "Catalog changed in another editor. Reopen to fetch the latest version.";
    }
}
