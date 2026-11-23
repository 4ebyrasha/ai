package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TraderPreset {

    private static final int VERSION = 2;

    private final String name;

    private final List<NpcTradeConfig> offers = new ArrayList<>();
    private final List<NpcBuybackConfig> buybackOffers = new ArrayList<>();

    private NBTTagCompound legacyData = new NBTTagCompound();

    /**
     * Версия содержимого каталога.
     * Увеличивается при каждом успешном сохранении редактором.
     */
    private long revision;

    public TraderPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Preset name cannot be empty");
        }

        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public long getRevision() {
        return revision;
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
            if (existing.getId().equals(offer.getId())
                    || existing.matches(offer.getItem())) {
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
     * Обновляет содержимое существующего объекта.
     * Имя каталога и сам объект остаются неизменными.
     */
    public boolean replaceContents(
            TraderPreset source,
            long newRevision
    ) {
        if (source == null
                || !name.equalsIgnoreCase(source.getName())
                || newRevision < 0L) {
            return false;
        }

        offers.clear();
        offers.addAll(source.offers);

        buybackOffers.clear();
        buybackOffers.addAll(source.buybackOffers);

        legacyData = source.legacyData.copy();
        revision = newRevision;

        return true;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("Version", VERSION);
        tag.setString("Name", name);
        tag.setLong("Revision", revision);

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

    public static TraderPreset readFromNBT(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }

        String name = tag.getString("Name");

        if (name.trim().isEmpty()) {
            return null;
        }

        TraderPreset preset = new TraderPreset(name);
        preset.revision = Math.max(0L, tag.getLong("Revision"));

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
