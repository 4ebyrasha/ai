package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;

import java.util.UUID;

/**
 * Самостоятельный торговый профиль.
 * <p>
 * Профиль ссылается на каталог, но имеет собственную идентичность
 * и собственные настройки лимитов.
 */
public class TraderProfile {

    private static final int VERSION = 1;

    private final String id;

    private String name;
    private String presetName;
    private String limitGroupName;
    private boolean sharedLimits;

    public TraderProfile(String name) {
        this(
                UUID.randomUUID().toString(),
                name,
                "",
                "",
                false
        );
    }

    public TraderProfile(
            String id,
            String name,
            String presetName,
            String limitGroupName,
            boolean sharedLimits
    ) {
        this.id = id == null || id.trim().isEmpty()
                ? UUID.randomUUID().toString()
                : id;

        setName(name);
        setPresetName(presetName);
        setLimitGroupName(limitGroupName);
        this.sharedLimits = sharedLimits;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Profile name cannot be empty");
        }

        this.name = name.trim();
    }

    public String getPresetName() {
        return presetName;
    }

    public void setPresetName(String presetName) {
        this.presetName = presetName == null
                ? ""
                : presetName.trim();
    }

    public String getLimitGroupName() {
        return limitGroupName;
    }

    public void setLimitGroupName(String limitGroupName) {
        this.limitGroupName = limitGroupName == null
                ? ""
                : limitGroupName.trim();
    }

    public boolean hasSharedLimits() {
        return sharedLimits;
    }

    public void setSharedLimits(boolean sharedLimits) {
        this.sharedLimits = sharedLimits;
    }

    /**
     * Возвращает область действия лимитов.
     * <p>
     * Общие лимиты используют limitGroupName.
     * Индивидуальные лимиты используют постоянный Trader ID.
     */
    public String getLimitScope() {
        if (sharedLimits && !limitGroupName.isEmpty()) {
            return "group:" + limitGroupName.toLowerCase(java.util.Locale.ROOT);
        }

        return "trader:" + id;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        tag.setInteger("Version", VERSION);
        tag.setString("Id", id);
        tag.setString("Name", name);
        tag.setString("PresetName", presetName);
        tag.setString("LimitGroupName", limitGroupName);
        tag.setBoolean("SharedLimits", sharedLimits);

        return tag;
    }

    public static TraderProfile readFromNBT(NBTTagCompound tag) {
        String name = tag.getString("Name");

        if (name.trim().isEmpty()) {
            return null;
        }

        return new TraderProfile(
                tag.getString("Id"),
                name,
                tag.getString("PresetName"),
                tag.getString("LimitGroupName"),
                tag.getBoolean("SharedLimits")
        );
    }
}
