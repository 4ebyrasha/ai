package com.cheba.trader;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import javax.annotation.Nonnull;

import java.util.*;

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

    public boolean removeProfile(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        TraderProfile profile = getProfileById(id);
        if (profile == null || !profiles.remove(profile)) {
            return false;
        }
        markDirty();
        return true;
    }

    public boolean removeProfileIfUnused(
            World world,
            String id,
            EntityNPCInterface ignoredNpc
    ) {
        if (world == null || world.isRemote || id == null || id.trim().isEmpty()) {
            return false;
        }

        for (net.minecraft.world.WorldServer loadedWorld
                : net.minecraftforge.common.DimensionManager.getWorlds()) {
            if (loadedWorld == null) {
                continue;
            }
            for (net.minecraft.entity.Entity entity : loadedWorld.loadedEntityList) {
                if (entity instanceof EntityNPCInterface && entity != ignoredNpc) {
                    EntityNPCInterface npc = (EntityNPCInterface) entity;
                    if (npc.hasTraderBinding()
                            && id.equals(npc.getTraderBinding().getTraderId())) {
                        return false;
                    }
                }
            }
        }
        return removeProfile(id);
    }

    public boolean addPreset(TraderPreset preset) {
        if (preset == null || getPreset(preset.getName()) != null) {
            return false;
        }

        presets.add(preset);
        markDirty();

        return true;
    }

    /**
     * Сохраняет изменения каталога, только если его ревизия
     * совпадает с ревизией, на основе которой работал редактор.
     *
     * Обновляет существующий объект, не заменяя его в списке.
     */
    public boolean saveEditedPreset(
            String presetName,
            long expectedRevision,
            TraderPreset editedPreset
    ) {
        if (presetName == null
                || presetName.trim().isEmpty()
                || editedPreset == null
                || expectedRevision < 0L
                || expectedRevision == Long.MAX_VALUE
                || !presetName.equalsIgnoreCase(editedPreset.getName())) {
            return false;
        }

        TraderPreset current = getPreset(presetName);

        if (current == null
                || current.getRevision() != expectedRevision) {
            return false;
        }

        if (!current.replaceContents(
                editedPreset,
                expectedRevision + 1L
        )) {
            return false;
        }

        markDirty();
        return true;
    }

    public boolean renamePreset(String oldName, String newName) {
        return renamePreset(oldName, newName, null);
    }

    public boolean renamePreset(String oldName, String newName, World world) {
        if (oldName == null || newName == null || newName.trim().isEmpty()
                || newName.trim().length() > 128) return false;
        TraderPreset current = getPreset(oldName);
        TraderPreset collision = getPreset(newName);
        if (current == null || (collision != null && collision != current)) return false;

        Map<TraderProfile, String> oldScopes = new HashMap<>();
        for (TraderProfile profile : profiles) {
            if (profile.getPresetName().equalsIgnoreCase(oldName)) {
                oldScopes.put(profile, profile.getLimitScope());
            }
        }

        TraderPreset replacement = current.copyAs(newName.trim());
        int index = presets.indexOf(current);
        if (index < 0) return false;
        presets.set(index, replacement);
        for (TraderProfile profile : oldScopes.keySet()) {
            profile.setPresetName(replacement.getName());
            if (world != null) {
                TraderRuntimeState.migrateScope(
                        world, oldScopes.get(profile), profile.getLimitScope());
            }
        }
        markDirty();
        return true;
    }

    public List<String> getPresetReferences(String name) {
        List<String> references = new ArrayList<>();
        if (name == null) return references;
        for (TraderProfile profile : profiles) {
            if (profile.getPresetName().equalsIgnoreCase(name)) references.add(profile.getName());
        }
        return references;
    }

    public boolean deletePreset(String name) {
        TraderPreset preset = getPreset(name);
        if (preset == null || !getPresetReferences(name).isEmpty()) return false;
        if (!presets.remove(preset)) return false;
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

    /**
     * Переносит старого NPC с RoleTrader в новую систему профилей.
     * <p>
     * Каждый NPC получает собственный профиль и Trader ID.
     * NPC с одинаковым старым marketName используют один каталог.
     * При включённых sharedLimits они также используют общую группу
     * лимитов с прежним названием рынка.
     * <p>
     * Метод безопасен для повторного вызова: NPC с существующей
     * TraderBinding повторно не мигрирует.
     *
     * @param npc старый NPC
     * @param role его старая торговая роль
     * @return созданный или найденный профиль
     */
    public TraderProfile migrateLegacyNpc(
            EntityNPCInterface npc,
            RoleTrader role
    ) {
        if (npc == null
                || role == null
                || npc.world == null
                || npc.world.isRemote) {
            return null;
        }

        if (npc.hasTraderBinding()) {
            return getProfileById(
                    npc.getTraderBinding().getTraderId()
            );
        }

        String npcUuid = npc.getUniqueID().toString();

        String oldMarketName = role.getMarket() == null
                ? ""
                : role.getMarket().trim();

        String presetName = oldMarketName.isEmpty()
                ? "legacy-" + npcUuid
                : oldMarketName;

        /*
         * Сначала переносим каталог.
         *
         * Если другой NPC уже перенёс этот рынок,
         * повторно каталог не создаём.
         */
        TraderPreset preset = getPreset(presetName);

        if (preset == null) {
            preset = role.createTraderPreset(presetName);

            if (!addPreset(preset)) {
                preset = getPreset(presetName);
            }
        }

        if (preset == null) {
            return null;
        }

        /*
         * Профиль всегда индивидуальный.
         * Общность лимитов задаётся отдельно через limitGroupName.
         */
        String profileName = npc.getName() + " [" + npcUuid.substring(0, 8) + "]";

        String limitGroupName = role.sharedLimits
                && !oldMarketName.isEmpty()
                ? oldMarketName
                : "legacy-" + npcUuid;

        TraderProfile profile = new TraderProfile(
                UUID.randomUUID().toString(),
                profileName,
                preset.getName(),
                limitGroupName,
                role.sharedLimits
        );

        if (!addProfile(profile)) {
            return null;
        }

        /*
         * Переносим старые счётчики.
         *
         * Старые ключи:
         * market:<name>|offer:<id>
         * npc:<uuid>|offer:<id>
         *
         * Новые ключи:
         * group:<name>|offer:<id>
         * trader:<traderId>|offer:<id>
         */
        String oldScope;

        if (role.sharedLimits && !oldMarketName.isEmpty()) {
            oldScope = "market:"
                    + oldMarketName.toLowerCase(Locale.ROOT);
        } else {
            oldScope = "npc:" + npcUuid;
        }

        TraderRuntimeState.migrateScope(
                npc.world,
                oldScope,
                profile.getLimitScope()
        );

        npc.setTraderBinding(new TraderBinding(profile.getId()));

        return profile;
    }

    /**
     * Synchronizes edits made through the familiar NPC trader editor.
     * A missing binding is migrated once; subsequent saves update its catalog.
     */
    /**
     * Legacy RoleSave packets must not overwrite independent catalogs.
     * Old NPCs are migrated once; catalog edits use revision-checked saves.
     */
    public TraderProfile syncLegacyRole(EntityNPCInterface npc, RoleTrader role) {
        if (npc == null || role == null || npc.world == null || npc.world.isRemote) {
            return null;
        }
        if (npc.hasTraderBinding()) {
            return getProfileById(npc.getTraderBinding().getTraderId());
        }
        return migrateLegacyNpc(npc, role);
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
