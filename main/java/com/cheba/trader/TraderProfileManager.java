package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Хранилище торговых профилей и каталогов мира.
 * <p>
 * Данные сохраняются независимо от NPC и доступны
 * из любого измерения одного сервера.
 */
public class TraderProfileManager extends WorldSavedData {

    private static final String DATA_NAME = "ChebaTraderProfiles";

    private static final int VERSION = 1;

    private final List<TraderProfile> profiles = new ArrayList<>();
    private final List<TraderPreset> presets = new ArrayList<>();

    public TraderProfileManager() {
        super(DATA_NAME);
    }

    public TraderProfileManager(String name) {
        super(name);
    }

    /**
     * Получает общее хранилище торговой системы.
     * <p>
     * Используется сохранение основного мира, чтобы профили
     * были общими для всех измерений.
     */
    public static TraderProfileManager get(World world) {
        if (world == null || world.isRemote) {
            return null;
        }

        World storageWorld = world.getMinecraftServer() != null
                ? world.getMinecraftServer().getWorld(0)
                : world;

        if (storageWorld == null || storageWorld.isRemote) {
            return null;
        }

        MapStorage storage = storageWorld.getPerWorldStorage();

        WorldSavedData savedData = storage.getOrLoadData(
                TraderProfileManager.class,
                DATA_NAME
        );

        TraderProfileManager manager;

        if (savedData instanceof TraderProfileManager) {
            manager = (TraderProfileManager) savedData;
        } else {
            manager = new TraderProfileManager();
            storage.setData(DATA_NAME, manager);
        }

        return manager;
    }

    public List<TraderProfile> getProfiles() {
        return Collections.unmodifiableList(profiles);
    }

    public List<TraderPreset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public TraderProfile getProfileById(String id) {
        if (id == null) {
            return null;
        }

        for (TraderProfile profile : profiles) {
            if (id.equals(profile.getId())) {
                return profile;
            }
        }

        return null;
    }

    public TraderProfile getProfileByName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = normalize(name);

        for (TraderProfile profile : profiles) {
            if (normalize(profile.getName()).equals(normalized)) {
                return profile;
            }
        }

        return null;
    }

    public TraderPreset getPreset(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        String normalized = normalize(name);

        for (TraderPreset preset : presets) {
            if (normalize(preset.getName()).equals(normalized)) {
                return preset;
            }
        }

        return null;
    }

    /**
     * Создаёт пустой профиль.
     * <p>
     * Каталог можно назначить позднее.
     * Названия профилей уникальны без учёта регистра.
     */
    public TraderProfile createProfile(String name) {
        if (name == null || name.trim().isEmpty()
                || getProfileByName(name) != null) {
            return null;
        }

        TraderProfile profile = new TraderProfile(name);

        profiles.add(profile);
        markDirty();

        return profile;
    }

    public boolean addProfile(TraderProfile profile) {
        if (profile == null
                || getProfileById(profile.getId()) != null
                || getProfileByName(profile.getName()) != null) {
            return false;
        }

        profiles.add(profile);
        markDirty();

        return true;
    }

    public boolean addPreset(TraderPreset preset) {
        if (preset == null || getPreset(preset.getName()) != null) {
            return false;
        }

        presets.add(preset);
        markDirty();

        return true;
    }

    public boolean replacePreset(TraderPreset preset) {
        if (preset == null) {
            return false;
        }

        String normalized = normalize(preset.getName());

        for (int i = 0; i < presets.size(); i++) {
            if (normalize(presets.get(i).getName()).equals(normalized)) {
                presets.set(i, preset);
                markDirty();
                return true;
            }
        }

        presets.add(preset);
        markDirty();

        return true;
    }

    public void markChanged() {
        markDirty();
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        profiles.clear();
        presets.clear();

        if (tag.hasKey("Profiles", 9)) {
            NBTTagList list = tag.getTagList("Profiles", 10);

            for (int i = 0; i < list.tagCount(); i++) {
                TraderProfile profile = TraderProfile.readFromNBT(
                        list.getCompoundTagAt(i)
                );

                if (profile != null
                        && getProfileById(profile.getId()) == null
                        && getProfileByName(profile.getName()) == null) {
                    profiles.add(profile);
                }
            }
        }

        if (tag.hasKey("Presets", 9)) {
            NBTTagList list = tag.getTagList("Presets", 10);

            for (int i = 0; i < list.tagCount(); i++) {
                TraderPreset preset = TraderPreset.readFromNBT(
                        list.getCompoundTagAt(i)
                );

                if (preset != null && getPreset(preset.getName()) == null) {
                    presets.add(preset);
                }
            }
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Version", VERSION);

        NBTTagList profileTags = new NBTTagList();

        for (TraderProfile profile : profiles) {
            profileTags.appendTag(profile.writeToNBT());
        }

        tag.setTag("Profiles", profileTags);

        NBTTagList presetTags = new NBTTagList();

        for (TraderPreset preset : presets) {
            presetTags.appendTag(preset.writeToNBT());
        }

        tag.setTag("Presets", presetTags);

        return tag;
    }
}
