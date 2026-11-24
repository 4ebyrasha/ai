package com.cheba.trader;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentString;
import noppes.npcs.Server;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.constants.EnumPacketClient;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.WeakHashMap;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTSizeTracker;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Серверная обработка сохранения каталогов независимой торговли.
 * <p>
 * Клиент передаёт только изменённый каталог и исходную ревизию.
 * Проверка, сохранение и рассылка выполняются сервером.
 */
public final class TraderPresetEditorService {

    private static final int MAX_PRESET_NAME_LENGTH = 128;
    private static final int MAX_OFFER_ID_LENGTH = 128;
    private static final int MAX_OFFERS = 54;
    private static final int MAX_BUYBACK_OFFERS = 128;
    private static final int MAX_PRICE_ITEMS = 54;
    private static final Map<EntityPlayerMP, String> OPEN_EDITORS = new WeakHashMap<>();

    private TraderPresetEditorService() {
    }

    public static void registerEditor(EntityPlayerMP player, String presetName) {
        if (player != null && presetName != null && !presetName.trim().isEmpty()) {
            OPEN_EDITORS.put(player, presetName);
        }
    }

    public static void handleManagerRequest(EntityPlayerMP player, NBTTagCompound request) {
        if (player == null || player.world == null || player.world.isRemote || request == null) return;
        TraderProfileManager manager = TraderProfileManager.get(player.world);
        if (manager == null) {
            sendManagerResult(player, false, "Trader storage is unavailable.", null);
            return;
        }

        String action = request.getString("Action");
        String name = request.getString("Name").trim();
        String newName = request.getString("NewName").trim();

        if ("edit".equals(action)) {
            TraderPreset preset = manager.getPreset(name);
            if (preset == null) {
                sendManagerResult(player, false, "Shop not found.", manager);
                return;
            }
            registerEditor(player, preset.getName());
            NBTTagCompound open = new NBTTagCompound();
            open.setTag("Preset", preset.writeToNBT());
            Server.sendData(player, EnumPacketClient.TraderPresetEditorOpen, open);
            return;
        }

        if ("references".equals(action)) {
            sendReferenceResult(player, manager, name, "");
            return;
        }

        if ("detach".equals(action)) {
            String profileId = request.getString("ProfileId");
            int entityId = request.getInteger("EntityId");
            int dimension = request.getInteger("Dimension");
            boolean detached = false;

            if (!profileId.isEmpty() && entityId >= 0) {
                net.minecraft.world.WorldServer targetWorld =
                        net.minecraftforge.common.DimensionManager.getWorld(dimension);
                net.minecraft.entity.Entity entity = targetWorld == null
                        ? null : targetWorld.getEntityByID(entityId);
                if (entity instanceof EntityNPCInterface) {
                    EntityNPCInterface npc = (EntityNPCInterface) entity;
                    if (npc.hasTraderBinding()
                            && profileId.equals(npc.getTraderBinding().getTraderId())) {
                        npc.setTraderBinding(null);
                        npc.updateClient = true;
                        detached = true;
                    }
                }
            }

            boolean removed = detached
                    && manager.removeProfileIfUnused(player.world, profileId, null);
            String message = removed
                    ? "NPC отвязан от магазина."
                    : detached
                        ? "NPC отвязан, но профиль ещё используется другим NPC."
                        : "NPC не загружен. Перейдите к нему или загрузите его чанк, затем повторите отвязку.";
            sendReferenceResult(player, manager, name, message);
            return;
        }

        boolean success;
        String message;
        if ("create".equals(action)) {
            success = !name.isEmpty() && name.length() <= MAX_PRESET_NAME_LENGTH
                    && manager.addPreset(new TraderPreset(name));
            message = success ? "Магазин создан." : "Недопустимое имя или магазин уже существует.";
        } else if ("rename".equals(action)) {
            success = !newName.isEmpty() && newName.length() <= MAX_PRESET_NAME_LENGTH
                    && manager.renamePreset(name, newName, player.world);
            message = success ? "Магазин переименован." : "Не удалось переименовать магазин.";
        } else if ("delete".equals(action)) {
            java.util.List<String> refs = manager.getPresetReferences(name);
            success = refs.isEmpty() && manager.deletePreset(name);
            message = success ? "Магазин удалён."
                    : "Сначала отвяжите профили NPC: " + refs.toString();
        } else if ("list".equals(action)) {
            success = true;
            message = "";
        } else {
            success = false;
            message = "Неизвестная операция.";
        }
        sendManagerResult(player, success, message, manager);
    }

    private static void sendManagerResult(EntityPlayerMP player, boolean success,
            String message, TraderProfileManager manager) {
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("Success", success);
        data.setString("Mode", "shops");
        data.setString("Message", message == null ? "" : message);
        if (manager != null) {
            NBTTagList presets = new NBTTagList();
            for (TraderPreset preset : manager.getPresets()) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("Name", preset.getName());
                entry.setLong("Revision", preset.getRevision());
                entry.setInteger("Offers", preset.getOffers().size());
                presets.appendTag(entry);
            }
            data.setTag("Presets", presets);
        }
        Server.sendData(player, EnumPacketClient.TraderPresetManagerOpen, data);
    }

    private static void sendReferenceResult(
            EntityPlayerMP player,
            TraderProfileManager manager,
            String presetName,
            String message
    ) {
        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("Success", true);
        data.setString("Mode", "references");
        data.setString("Name", presetName);
        data.setString("Message", message == null ? "" : message);
        NBTTagList references = new NBTTagList();

        for (TraderProfile profile : manager.getProfiles()) {
            if (!profile.getPresetName().equalsIgnoreCase(presetName)) {
                continue;
            }

            boolean found = false;
            for (net.minecraft.world.WorldServer loadedWorld
                    : net.minecraftforge.common.DimensionManager.getWorlds()) {
                if (loadedWorld == null) continue;
                for (net.minecraft.entity.Entity entity : loadedWorld.loadedEntityList) {
                    if (!(entity instanceof EntityNPCInterface)) continue;
                    EntityNPCInterface npc = (EntityNPCInterface) entity;
                    if (!npc.hasTraderBinding()
                            || !profile.getId().equals(npc.getTraderBinding().getTraderId())) {
                        continue;
                    }

                    NBTTagCompound entry = new NBTTagCompound();
                    entry.setString("ProfileId", profile.getId());
                    entry.setInteger("EntityId", npc.getEntityId());
                    entry.setInteger("Dimension", loadedWorld.provider.getDimension());
                    entry.setString("Label", npc.getName() + " [" + loadedWorld.provider.getDimension() + "]");
                    references.appendTag(entry);
                    found = true;
                }
            }

            if (!found) {
                NBTTagCompound entry = new NBTTagCompound();
                entry.setString("ProfileId", profile.getId());
                entry.setInteger("EntityId", -1);
                entry.setInteger("Dimension", 0);
                entry.setString("Label", profile.getName() + " — NPC не загружен или запись устарела");
                references.appendTag(entry);
            }
        }

        data.setTag("References", references);
        Server.sendData(player, EnumPacketClient.TraderPresetManagerOpen, data);
    }

    public static void handleSave(
            EntityPlayerMP player,
            NBTTagCompound data
    ) {
        if (player == null || player.world == null
                || player.world.isRemote || data == null) {
            return;
        }

        if (!data.hasKey("ExpectedRevision", 4)
                || !data.hasKey("Preset", 10)) {
            sendResult(player, false, "Invalid editor data.", null);
            return;
        }

        long expectedRevision = data.getLong("ExpectedRevision");
        NBTTagCompound presetTag = data.getCompoundTag("Preset");

        if (expectedRevision < 0L || !isValidPresetTag(presetTag)) {
            sendResult(player, false, "Invalid trader preset.", null);
            return;
        }

        TraderPreset editedPreset;

        try {
            editedPreset = TraderPreset.readFromNBT(presetTag);
        } catch (RuntimeException exception) {
            sendResult(player, false, "Could not read trader preset.", null);
            return;
        }

        if (editedPreset == null) {
            sendResult(player, false, "Invalid trader preset.", null);
            return;
        }

        TraderProfileManager manager =
                TraderProfileManager.get(player.world);

        if (manager == null) {
            sendResult(player, false, "Trader storage is unavailable.", null);
            return;
        }

        TraderPreset currentPreset =
                manager.getPreset(editedPreset.getName());

        if (currentPreset == null) {
            sendResult(player, false, "Trader preset not found.", null);
            return;
        }

        if (currentPreset.getRevision() != expectedRevision) {
            sendResult(
                    player,
                    false,
                    "Preset was changed by another editor. Close and reopen the editor.",
                    null
            );
            return;
        }

        if (!manager.saveEditedPreset(
                editedPreset.getName(),
                expectedRevision,
                editedPreset
        )) {
            sendResult(player, false, "Trader preset could not be saved.", null);
            return;
        }

        TraderPreset savedPreset =
                manager.getPreset(editedPreset.getName());

        if (savedPreset == null) {
            sendResult(
                    player,
                    false,
                    "Preset was saved, but could not be reloaded.",
                    null
            );
            return;
        }

        broadcastPresetUpdate(player, savedPreset);
        broadcastEditorStale(player, savedPreset);

        sendResult(
                player,
                true,
                "Catalog saved. Revision: " + savedPreset.getRevision(),
                savedPreset
        );
    }

    private static void sendResult(
            EntityPlayerMP player,
            boolean success,
            String message,
            TraderPreset preset
    ) {
        if (player == null) {
            return;
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setBoolean("Success", success);
        data.setString("Message", message == null ? "" : message);

        if (success && preset != null) {
            data.setTag("Preset", preset.writeToNBT());
        }

        Server.sendData(
                player,
                EnumPacketClient.TraderPresetEditorResult,
                data
        );
    }

    /**
     * Проверяет NBT до создания объектов каталога.
     * Это не позволяет загрузчику незаметно пропустить
     * некорректные предложения и сохранить неполный каталог.
     */
    private static boolean isValidPresetTag(NBTTagCompound tag) {
        if (tag == null
                || !tag.hasKey("Name", 8)
                || !tag.hasKey("Offers", 9)
                || !tag.hasKey("BuybackOffers", 9)) {
            return false;
        }

        String name = tag.getString("Name");

        if (name.trim().isEmpty()
                || name.length() > MAX_PRESET_NAME_LENGTH) {
            return false;
        }

        NBTTagList offers = tag.getTagList("Offers", 10);
        NBTTagList buybackOffers =
                tag.getTagList("BuybackOffers", 10);

        if (offers.tagCount() > MAX_OFFERS
                || buybackOffers.tagCount() > MAX_BUYBACK_OFFERS) {
            return false;
        }

        Set<String> offerIds = new HashSet<>();
        Set<Integer> displaySlots = new HashSet<>();

        for (int i = 0; i < offers.tagCount(); i++) {
            NBTTagCompound offerTag = offers.getCompoundTagAt(i);

            if (!isValidOfferTag(offerTag)) {
                return false;
            }

            String id = offerTag.getString("Id");

            if (!offerIds.add(id)) {
                return false;
            }

            int displaySlot = offerTag.getInteger("DisplaySlot");

            if (displaySlot >= 0 && !displaySlots.add(displaySlot)) {
                return false;
            }
        }

        Set<String> buybackIds = new HashSet<>();

        for (int i = 0; i < buybackOffers.tagCount(); i++) {
            NBTTagCompound buybackTag =
                    buybackOffers.getCompoundTagAt(i);

            if (!isValidBuybackTag(buybackTag)) {
                return false;
            }

            if (!buybackIds.add(buybackTag.getString("Id"))) {
                return false;
            }

            ItemStack item = new ItemStack(
                    buybackTag.getCompoundTag("Item")
            );

            for (int previousIndex = 0;
                 previousIndex < i;
                 previousIndex++) {

                ItemStack previousItem = new ItemStack(
                        buybackOffers
                                .getCompoundTagAt(previousIndex)
                                .getCompoundTag("Item")
                );

                if (ItemStack.areItemsEqual(item, previousItem)
                        && ItemStack.areItemStackTagsEqual(
                        item,
                        previousItem
                )) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isValidOfferTag(NBTTagCompound tag) {
        if (tag == null
                || !tag.hasKey("Id", 8)
                || !tag.hasKey("DisplaySlot", 3)
                || !tag.hasKey("Result", 10)
                || !tag.hasKey("PriceMoney", 4)
                || !tag.hasKey("PriceItems", 9)) {
            return false;
        }

        String id = tag.getString("Id");

        if (id.trim().isEmpty() || id.length() > MAX_OFFER_ID_LENGTH) {
            return false;
        }

        int displaySlot = tag.getInteger("DisplaySlot");

        if (displaySlot < -1 || displaySlot >= 54) {
            return false;
        }

        if (tag.hasKey("SellPriceMoney", 4)
                && tag.getLong("SellPriceMoney") < 0L) {
            return false;
        }

        if (tag.getLong("PriceMoney") < 0L) {
            return false;
        }

        if (tag.hasKey("MaxPerDay", 3)
                && tag.getInteger("MaxPerDay") < -1) {
            return false;
        }

        if (tag.hasKey("DailyStock", 3)
                && tag.getInteger("DailyStock") < -1) {
            return false;
        }

        ItemStack result = new ItemStack(
                tag.getCompoundTag("Result")
        );

        if (result.isEmpty()) {
            return false;
        }

        NBTTagList priceItems =
                tag.getTagList("PriceItems", 10);

        if (priceItems.tagCount() > MAX_PRICE_ITEMS) {
            return false;
        }

        boolean hasPrice = tag.getLong("PriceMoney") > 0L;

        for (int i = 0; i < priceItems.tagCount(); i++) {
            ItemStack priceItem = new ItemStack(
                    priceItems.getCompoundTagAt(i)
            );

            if (priceItem.isEmpty()) {
                return false;
            }

            hasPrice = true;
        }

        return hasPrice;
    }

    private static boolean isValidBuybackTag(NBTTagCompound tag) {
        if (tag == null
                || !tag.hasKey("Id", 8)
                || !tag.hasKey("Item", 10)
                || !tag.hasKey("PriceMoney", 4)) {
            return false;
        }

        String id = tag.getString("Id");

        if (id.trim().isEmpty() || id.length() > MAX_OFFER_ID_LENGTH) {
            return false;
        }

        if (tag.getLong("PriceMoney") < 0L) {
            return false;
        }

        ItemStack item = new ItemStack(
                tag.getCompoundTag("Item")
        );

        return !item.isEmpty();
    }

    /**
     * Отправляет обновлённый каталог только игрокам,
     * у которых сейчас открыта сессия с этим каталогом.
     */
    private static void broadcastPresetUpdate(
            EntityPlayerMP sourcePlayer,
            TraderPreset preset
    ) {
        if (sourcePlayer == null
                || sourcePlayer.getServer() == null
                || preset == null) {
            return;
        }

        NBTTagCompound presetData = preset.writeToNBT();

        for (EntityPlayerMP target :
                sourcePlayer.getServer().getPlayerList().getPlayers()) {

            String token =
                    TraderSessionManager.getActiveToken(target);

            if (token == null) {
                continue;
            }

            TraderSessionManager.Context context =
                    TraderSessionManager.resolve(target, token);

            if (context == null
                    || !context.getPreset().getName()
                    .equalsIgnoreCase(preset.getName())) {
                continue;
            }

            NBTTagCompound update = new NBTTagCompound();
            update.setString("Token", token);
            update.setTag("Preset", presetData.copy());

            Server.sendData(
                    target,
                    EnumPacketClient.TraderPresetUpdate,
                    update
            );
        }
    }

    private static void broadcastEditorStale(
            EntityPlayerMP sourcePlayer,
            TraderPreset preset
    ) {
        if (sourcePlayer == null || sourcePlayer.getServer() == null || preset == null) {
            return;
        }

        NBTTagCompound update = new NBTTagCompound();
        update.setString("PresetName", preset.getName());
        update.setLong("Revision", preset.getRevision());

        for (Map.Entry<EntityPlayerMP, String> entry : OPEN_EDITORS.entrySet()) {
            EntityPlayerMP target = entry.getKey();
            if (target == null || target == sourcePlayer
                    || !preset.getName().equalsIgnoreCase(entry.getValue())) {
                continue;
            }

            Server.sendData(target, EnumPacketClient.TraderPresetEditorStale, update);
        }
    }

    private static void sendMessage(
            EntityPlayerMP player,
            String message
    ) {
        player.sendMessage(new TextComponentString(
                "[Trader] " + message
        ));
    }
    /**
     * Читает NBT редактора с ограничением размера.
     * <p>
     * Максимальный сжатый размер — 60 КБ.
     * Максимальный распакованный размер — 1 МБ.
     */
    public static NBTTagCompound readPacket(ByteBuf buffer)
            throws IOException {

        if (buffer == null || buffer.readableBytes() < 4) {
            throw new IOException("Missing trader editor payload");
        }

        int length = buffer.readInt();

        if (length <= 0
                || length > 60000
                || length != buffer.readableBytes()) {
            throw new IOException("Invalid trader editor payload size");
        }

        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);

        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(
                                new ByteArrayInputStream(bytes)
                        )
                )
        )) {
            return CompressedStreamTools.read(
                    input,
                    new NBTSizeTracker(1048576L)
            );
        }
    }
}
