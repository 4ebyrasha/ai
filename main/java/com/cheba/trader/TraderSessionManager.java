package com.cheba.trader;

import net.minecraft.entity.player.EntityPlayerMP;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleTrader;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Управляет независимыми серверными торговыми сессиями.
 * <p>
 * Сессия принадлежит конкретному игроку и ссылается на профиль
 * по его постоянному UUID. NPC не является частью идентичности сессии.
 * <p>
 * Для каждого игрока разрешена одна активная торговая сессия.
 * Повторное открытие другого профиля заменяет предыдущую сессию.
 * <p>
 * Все методы должны вызываться из серверного потока Minecraft.
 */
public final class TraderSessionManager {

    private static final Map<String, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, String> PLAYER_SESSIONS = new HashMap<>();

    private TraderSessionManager() {
    }

    /**
     * Возвращает токен текущей сессии игрока.
     * <p>
     * Используется при открытии серверного контейнера торговли.
     */
    public static String getActiveToken(EntityPlayerMP player) {
        if (player == null) {
            return null;
        }

        String token = PLAYER_SESSIONS.get(player.getUniqueID());
        if (token == null) {
            return null;
        }

        Session session = SESSIONS.get(token);

        if (session == null || !session.owner.equals(player.getUniqueID())) {
            PLAYER_SESSIONS.remove(player.getUniqueID());
            return null;
        }

        return token;
    }

    /**
     * Создаёт сессию для указанного профиля.
     * <p>
     * Профиль и его каталог проверяются до создания сессии.
     * Предыдущая сессия игрока при этом закрывается.
     *
     * @param player игрок, открывающий торговлю
     * @param profileName имя профиля
     * @return созданная сессия или null, если профиль недоступен
     */
    public static Session open(EntityPlayerMP player, String profileName) {
        if (player == null || player.world == null || player.world.isRemote
                || profileName == null || profileName.trim().isEmpty()) {
            return null;
        }

        TraderProfileManager manager = TraderProfileManager.get(player.world);
        if (manager == null) {
            return null;
        }

        TraderProfile profile = manager.getProfileByName(profileName);
        if (profile == null || profile.getPresetName().isEmpty()) {
            return null;
        }

        return openProfile(player, manager, profile);
    }

    /**
     * Opens a shop using the persistent binding stored on the NPC.
     * Every NPC keeps its own profile, while several profiles may point
     * to the same shared catalog.
     */
    public static Session openForNpc(
            EntityPlayerMP player,
            EntityNPCInterface npc,
            RoleTrader role
    ) {
        if (player == null || npc == null || role == null
                || player.world == null || player.world.isRemote
                || npc.world != player.world || npc.isDead
                || player.getDistanceSq(npc) > 64.0D) {
            return null;
        }

        TraderProfileManager manager = TraderProfileManager.get(player.world);
        if (manager == null) {
            return null;
        }

        TraderProfile profile;
        if (npc.hasTraderBinding()) {
            profile = manager.getProfileById(
                    npc.getTraderBinding().getTraderId()
            );
        } else {
            profile = manager.migrateLegacyNpc(npc, role);
        }

        if (profile == null || profile.getPresetName().isEmpty()) {
            return null;
        }

        return openProfile(player, manager, profile);
    }

    private static Session openProfile(
            EntityPlayerMP player,
            TraderProfileManager manager,
            TraderProfile profile
    ) {
        if (profile == null || profile.getPresetName().isEmpty()
                || manager.getPreset(profile.getPresetName()) == null) {
            return null;
        }

        closePlayer(player);

        String token = UUID.randomUUID().toString();
        Session session = new Session(
                token,
                player.getUniqueID(),
                player.dimension,
                profile.getId()
        );

        SESSIONS.put(token, session);
        PLAYER_SESSIONS.put(player.getUniqueID(), token);
        return session;
    }

    /**
     * Возвращает актуальные данные профиля и каталога для сессии.
     * <p>
     * Проверяет владельца, измерение, профиль и каталог.
     * При смене измерения сессия владельца закрывается.
     *
     * @param player игрок, отправивший запрос
     * @param token токен торговой сессии
     * @return проверенный контекст или null
     */
    public static Context resolve(EntityPlayerMP player, String token) {
        if (player == null || player.world == null || player.world.isRemote
                || token == null || token.isEmpty()) {
            return null;
        }

        Session session = SESSIONS.get(token);
        if (session == null) {
            return null;
        }

        if (!session.owner.equals(player.getUniqueID())) {
            return null;
        }

        if (session.dimension != player.dimension) {
            close(token);
            return null;
        }

        TraderProfileManager manager = TraderProfileManager.get(player.world);
        if (manager == null) {
            close(token);
            return null;
        }

        TraderProfile profile = manager.getProfileById(session.profileId);
        if (profile == null || profile.getPresetName().isEmpty()) {
            close(token);
            return null;
        }

        TraderPreset preset = manager.getPreset(profile.getPresetName());
        if (preset == null) {
            close(token);
            return null;
        }

        return new Context(session, profile, preset);
    }

    /**
     * Закрывает сессию по токену.
     *
     * @param token токен сессии
     */
    public static void close(String token) {
        if (token == null) {
            return;
        }

        Session session = SESSIONS.remove(token);
        if (session == null) {
            return;
        }

        String playerToken = PLAYER_SESSIONS.get(session.owner);
        if (token.equals(playerToken)) {
            PLAYER_SESSIONS.remove(session.owner);
        }
    }

    /**
     * Закрывает активную сессию игрока.
     *
     * @param player игрок
     */
    public static void closePlayer(EntityPlayerMP player) {
        if (player == null) {
            return;
        }

        String token = PLAYER_SESSIONS.remove(player.getUniqueID());
        if (token != null) {
            SESSIONS.remove(token);
        }
    }

    /**
     * Неизменяемая запись серверной сессии.
     */
    public static final class Session {

        private final String token;
        private final UUID owner;
        private final int dimension;
        private final String profileId;

        private Session(
                String token,
                UUID owner,
                int dimension,
                String profileId
        ) {
            this.token = token;
            this.owner = owner;
            this.dimension = dimension;
            this.profileId = profileId;
        }

        public String getToken() {
            return token;
        }

        public String getProfileId() {
            return profileId;
        }
    }

    /**
     * Актуальные объекты профиля и каталога после проверки сессии.
     */
    public static final class Context {

        private final Session session;
        private final TraderProfile profile;
        private final TraderPreset preset;

        private Context(
                Session session,
                TraderProfile profile,
                TraderPreset preset
        ) {
            this.session = session;
            this.profile = profile;
            this.preset = preset;
        }

        public Session getSession() {
            return session;
        }

        public TraderProfile getProfile() {
            return profile;
        }

        public TraderPreset getPreset() {
            return preset;
        }
    }
    /**
     * Закрывает сессию только в том случае, если токен принадлежит игроку.
     *
     * @param player владелец запроса
     * @param token токен сессии
     */
    public static void closeOwned(EntityPlayerMP player, String token) {
        if (player == null || token == null) {
            return;
        }

        Session session = SESSIONS.get(token);

        if (session != null && session.owner.equals(player.getUniqueID())) {
            close(token);
        }
    }
}
