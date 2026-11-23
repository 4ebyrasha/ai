package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Каталог товаров торговой системы.
 * <p>
 * Один каталог может использоваться несколькими торговыми профилями.
 * TraderPreset не зависит от NPC и не содержит настроек лимитов профиля.
 */
public class TraderPreset {

    private static final int VERSION = 1;

    private final String name;

    private final List<NpcTradeConfig> offers = new ArrayList<>();
    private final List<NpcBuybackConfig> buybackOffers = new ArrayList<>();

    /**
     * Старые настройки CustomNPCs:
     * TraderCurrency, TraderSold, TraderIgnoreDamage, TraderIgnoreNBT.
     * <p>
     * Храним их отдельно, чтобы не потерять совместимость
     * при переносе старых market JSON.
     */
    private NBTTagCompound legacyData = new NBTTagCompound();

    public TraderPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Preset name cannot be empty");
        }

        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public List<NpcTradeConfig> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    public List<NpcBuybackConfig> getBuybackOffers() {
        return Collections.unmodifiableList(buybackOffers);
    }

    public NpcTradeConfig getOffer(String id) {
        if (id == null) {
            return null;
        }

        for (NpcTradeConfig offer : offers) {
            if (id.equals(offer.getId())) {
                return offer;
            }
        }

        return null;
    }

    public void addOffer(NpcTradeConfig offer) {
        if (offer == null || getOffer(offer.getId()) != null) {
            return;
        }

        offers.add(offer);
    }

    public boolean removeOffer(String id) {
        if (id == null) {
            return false;
        }

        for (int i = 0; i < offers.size(); i++) {
            if (id.equals(offers.get(i).getId())) {
                offers.remove(i);
                return true;
            }
        }

        return false;
    }

    public void clearOffers() {
        offers.clear();
    }

    public boolean addBuybackOffer(NpcBuybackConfig offer) {
        if (offer == null || offer.getItem().isEmpty()) {
            return false;
        }

        for (NpcBuybackConfig existing : buybackOffers) {
            if (existing.getId().equals(offer.getId())) {
                return false;
            }
        }

        buybackOffers.add(offer);
        return true;
    }

    public boolean removeBuybackOffer(String id) {
        if (id == null) {
            return false;
        }

        for (int i = 0; i < buybackOffers.size(); i++) {
            if (id.equals(buybackOffers.get(i).getId())) {
                buybackOffers.remove(i);
                return true;
            }
        }

        return false;
    }

    public void clearBuybackOffers() {
        buybackOffers.clear();
    }

    public NBTTagCompound getLegacyData() {
        return legacyData.copy();
    }

    public void setLegacyData(NBTTagCompound data) {
        legacyData = data == null
                ? new NBTTagCompound()
                : data.copy();
    }

    /**
     * Сериализует каталог вместе с предложениями и старыми настройками.
     */
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("Version", VERSION);
        tag.setString("Name", name);

        NBTTagList offerTags = new NBTTagList();

        for (NpcTradeConfig offer : offers) {
            if (offer != null) {
                offerTags.appendTag(offer.writeToNBT());
            }
        }

        tag.setTag("Offers", offerTags);

        NBTTagList buybackTags = new NBTTagList();

        for (NpcBuybackConfig offer : buybackOffers) {
            if (offer != null) {
                buybackTags.appendTag(offer.writeToNBT());
            }
        }

        tag.setTag("BuybackOffers", buybackTags);
        tag.setTag("LegacyData", legacyData.copy());

        return tag;
    }

    /**
     * Загружает каталог.
     * <p>
     * Для старых предложений без DisplaySlot сохраняется
     * прежний порядок отображения.
     */
    public static TraderPreset readFromNBT(NBTTagCompound tag) {
        String name = tag.getString("Name");

        if (name.trim().isEmpty()) {
            return null;
        }

        TraderPreset preset = new TraderPreset(name);

        if (tag.hasKey("LegacyData", 10)) {
            preset.setLegacyData(tag.getCompoundTag("LegacyData"));
        }

        if (tag.hasKey("Offers", 9)) {
            NBTTagList offerTags = tag.getTagList("Offers", 10);

            for (int i = 0; i < offerTags.tagCount(); i++) {
                NBTTagCompound offerTag = offerTags.getCompoundTagAt(i);

                NpcTradeConfig offer =
                        NpcTradeConfig.readFromNBT(offerTag);

                if (!offerTag.hasKey("DisplaySlot", 3)) {
                    offer.setDisplaySlot(i);
                }

                preset.addOffer(offer);
            }
        }

        if (tag.hasKey("BuybackOffers", 9)) {
            NBTTagList buybackTags =
                    tag.getTagList("BuybackOffers", 10);

            for (int i = 0; i < buybackTags.tagCount(); i++) {
                preset.addBuybackOffer(
                        NpcBuybackConfig.readFromNBT(
                                buybackTags.getCompoundTagAt(i)
                        )
                );
            }
        }

        return preset;
    }
}
