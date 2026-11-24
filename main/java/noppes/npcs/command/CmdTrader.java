package noppes.npcs.command;

import com.cheba.trader.TraderBinding;
import com.cheba.trader.TraderProfile;
import com.cheba.trader.TraderProfileManager;
import com.cheba.trader.TraderPreset;
import com.cheba.trader.TraderSessionManager;
import com.cheba.trader.TraderPresetEditorService;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import noppes.npcs.Server;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.entity.EntityNPCInterface;

import javax.annotation.Nonnull;

import java.util.List;

public class CmdTrader extends CommandNoppesBase {

    @Nonnull
    @Override
    public String getName() {
        return "trader";
    }

    @Override
    public String getDescription() {
        return "Trader profile operations";
    }

    private String decodeName(String value) {
        return value == null
                ? ""
                : value.replace('%', ' ').trim();
    }

    private String joinName(String[] args) {
        StringBuilder builder = new StringBuilder();

        for (String arg : args) {
            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(arg);
        }

        return builder.toString().trim();
    }

    private TraderProfileManager getManager(
            ICommandSender sender
    ) throws CommandException {

        TraderProfileManager manager =
                TraderProfileManager.get(sender.getEntityWorld());

        if (manager == null) {
            throw new CommandException(
                    "Trader profile storage is unavailable"
            );
        }

        return manager;
    }

    private EntityNPCInterface findNpc(
            ICommandSender sender,
            String name
    ) throws CommandException {

        if (!(sender instanceof EntityPlayer)) {
            throw new CommandException(
                    "This command can only be used by a player"
            );
        }

        String expectedName = decodeName(name);

        if (expectedName.isEmpty()) {
            throw new CommandException("Invalid NPC name");
        }

        World world = sender.getEntityWorld();

        AxisAlignedBB area = new AxisAlignedBB(
                sender.getPosition(),
                sender.getPosition().add(1, 1, 1)
        ).grow(80.0D);

        List<EntityNPCInterface> npcs =
                world.getEntitiesWithinAABB(
                        EntityNPCInterface.class,
                        area
                );

        EntityNPCInterface nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (EntityNPCInterface npc : npcs) {
            if (!npc.display.getName().equalsIgnoreCase(expectedName)) {
                continue;
            }

            double distance = npc.getDistanceSq(
                    sender.getPosition()
            );

            if (distance < nearestDistance) {
                nearest = npc;
                nearestDistance = distance;
            }
        }

        if (nearest == null) {
            throw new CommandException(
                    "NPC '%s' was not found within 80 blocks",
                    expectedName
            );
        }

        return nearest;
    }

    @SubCommand(
            name = "list",
            desc = "List trader profiles and catalogs",
            permission = 2
    )
    public void list(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        TraderProfileManager manager = getManager(sender);

        sender.sendMessage(new TextComponentString(
                "§6Trader profiles:"
        ));

        if (manager.getProfiles().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                    "§7No profiles."
            ));
        }

        for (TraderProfile profile : manager.getProfiles()) {
            sender.sendMessage(new TextComponentString(
                    "§e" + profile.getName()
                            + " §7| ID: " + profile.getId()
                            + " | Catalog: "
                            + (profile.getPresetName().isEmpty()
                            ? "none"
                            : profile.getPresetName())
                            + " | Shared limits: "
                            + profile.hasSharedLimits()
            ));
        }

        sender.sendMessage(new TextComponentString(
                "§6Trader catalogs:"
        ));

        if (manager.getPresets().isEmpty()) {
            sender.sendMessage(new TextComponentString(
                    "§7No catalogs."
            ));
        }

        for (TraderPreset preset : manager.getPresets()) {
            sender.sendMessage(new TextComponentString(
                    "§e" + preset.getName()
                            + " §7| Offers: "
                            + preset.getOffers().size()
                            + " | Buyback: "
                            + preset.getBuybackOffers().size()
                            + " | Revision: "
                            + preset.getRevision()
            ));
        }
    }

    @SubCommand(
            name = "createprofile",
            desc = "Create trader profile",
            usage = "<profileName>",
            permission = 2
    )
    public void createProfile(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        String name = joinName(args);

        if (name.isEmpty() || name.length() > 128) {
            throw new CommandException("Invalid profile name");
        }

        TraderProfileManager manager = getManager(sender);

        TraderProfile profile = manager.createProfile(name);

        if (profile == null) {
            throw new CommandException(
                    "Profile '%s' already exists",
                    name
            );
        }

        sender.sendMessage(new TextComponentString(
                "Created trader profile: " + profile.getName()
        ));
    }

    @SubCommand(
            name = "createpreset",
            desc = "Create trader catalog",
            usage = "<presetName>",
            permission = 2
    )
    public void createPreset(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        String name = joinName(args);

        if (name.isEmpty() || name.length() > 128) {
            throw new CommandException("Invalid preset name");
        }

        TraderProfileManager manager = getManager(sender);

        TraderPreset preset;

        try {
            preset = new TraderPreset(name);
        } catch (IllegalArgumentException exception) {
            throw new CommandException("Invalid preset name");
        }

        if (!manager.addPreset(preset)) {
            throw new CommandException(
                    "Catalog '%s' already exists",
                    name
            );
        }

        sender.sendMessage(new TextComponentString(
                "Created trader catalog: " + preset.getName()
        ));
    }

    @SubCommand(
            name = "setpreset",
            desc = "Assign catalog to profile",
            usage = "<profileName> <presetName>",
            permission = 2
    )
    public void setPreset(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 2) {
            throw new CommandException(
                    "Usage: /customnpcs trader setpreset <profileName> <presetName>"
            );
        }

        String profileName = decodeName(args[0]);
        String presetName = decodeName(args[1]);

        TraderProfileManager manager = getManager(sender);

        TraderProfile profile =
                manager.getProfileByName(profileName);

        if (profile == null) {
            throw new CommandException(
                    "Profile '%s' was not found",
                    profileName
            );
        }

        TraderPreset preset = manager.getPreset(presetName);

        if (preset == null) {
            throw new CommandException(
                    "Catalog '%s' was not found",
                    presetName
            );
        }

        profile.setPresetName(preset.getName());
        manager.markChanged();

        sender.sendMessage(new TextComponentString(
                "Assigned catalog '" + preset.getName()
                        + "' to profile '" + profile.getName() + "'."
        ));
    }

    @SubCommand(
            name = "setlimits",
            desc = "Configure shared trader limits",
            usage = "<profileName> <true|false> [groupName]",
            permission = 2
    )
    public void setLimits(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 2) {
            throw new CommandException(
                    "Usage: /customnpcs trader setlimits <profileName> <true|false> [groupName]"
            );
        }

        String profileName = decodeName(args[0]);
        String sharedValue = args[1];

        if (!"true".equalsIgnoreCase(sharedValue)
                && !"false".equalsIgnoreCase(sharedValue)) {
            throw new CommandException(
                    "Shared limits must be true or false"
            );
        }

        boolean shared = Boolean.parseBoolean(sharedValue);

        String groupName = args.length >= 3
                ? decodeName(args[2])
                : "";

        if (groupName.length() > 128) {
            throw new CommandException("Limit group name is too long");
        }

        TraderProfileManager manager = getManager(sender);

        TraderProfile profile =
                manager.getProfileByName(profileName);

        if (profile == null) {
            throw new CommandException(
                    "Profile '%s' was not found",
                    profileName
            );
        }

        profile.setSharedLimits(shared);
        profile.setLimitGroupName(groupName);

        manager.markChanged();

        sender.sendMessage(new TextComponentString(
                "Updated limits for profile '" + profile.getName()
                        + "'. Shared: " + shared
                        + ", group: "
                        + (groupName.isEmpty() ? "none" : groupName)
        ));
    }

    @SubCommand(
            name = "bind",
            desc = "Bind nearby NPC to trader profile",
            usage = "<npcName> <profileName>",
            permission = 2
    )
    public void bind(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 2) {
            throw new CommandException(
                    "Usage: /customnpcs trader bind <npcName> <profileName>"
            );
        }

        String npcName = decodeName(args[0]);
        String profileName = decodeName(args[1]);

        TraderProfileManager manager = getManager(sender);

        TraderProfile profile =
                manager.getProfileByName(profileName);

        if (profile == null) {
            throw new CommandException(
                    "Profile '%s' was not found",
                    profileName
            );
        }

        EntityNPCInterface npc = findNpc(sender, npcName);

        npc.setTraderBinding(
                new TraderBinding(profile.getId())
        );

        npc.updateClient = true;

        sender.sendMessage(new TextComponentString(
                "Bound NPC '" + npc.display.getName()
                        + "' to profile '" + profile.getName() + "'."
        ));
    }

    @SubCommand(
            name = "unbind",
            desc = "Remove trader profile binding from nearby NPC",
            usage = "<npcName>",
            permission = 2
    )
    public void unbind(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 1) {
            throw new CommandException(
                    "Usage: /customnpcs trader unbind <npcName>"
            );
        }

        String npcName = decodeName(args[0]);

        if (npcName.isEmpty()) {
            throw new CommandException("Invalid NPC name");
        }

        EntityNPCInterface npc = findNpc(sender, npcName);

        if (!npc.hasTraderBinding()) {
            throw new CommandException(
                    "NPC '%s' has no trader profile binding",
                    npc.display.getName()
            );
        }

        npc.setTraderBinding(null);
        npc.updateClient = true;

        sender.sendMessage(new TextComponentString(
                "Removed trader binding from NPC '"
                        + npc.display.getName() + "'."
        ));
    }

    @SubCommand(
            name = "opentrader",
            desc = "Open trader profile",
            usage = "<player> <profileName|profileId>",
            permission = 2
    )
    public void openTrader(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 2) {
            throw new CommandException(
                    "Usage: /customnpcs trader opentrader <player> <profileName|profileId>"
            );
        }

        EntityPlayer target = getPlayer(server, sender, args[0]);

        if (!(target instanceof EntityPlayerMP)) {
            throw new CommandException("Target player is not available");
        }

        EntityPlayerMP player = (EntityPlayerMP) target;
        String profileName = decodeName(args[1]);

        TraderProfileManager manager =
                TraderProfileManager.get(player.world);

        if (manager == null) {
            throw new CommandException(
                    "Trader profile storage is unavailable"
            );
        }

        TraderProfile profile = manager.getProfileById(profileName);
        if (profile == null) {
            profile = manager.getProfileByName(profileName);
        }

        if (profile == null) {
            throw new CommandException(
                    "Trader profile '%s' was not found",
                    profileName
            );
        }

        TraderPreset preset =
                manager.getPreset(profile.getPresetName());

        if (preset == null) {
            throw new CommandException(
                    "Catalog for trader profile '%s' was not found",
                    profileName
            );
        }

        TraderSessionManager.Session session =
                TraderSessionManager.open(player, profile.getId());

        if (session == null) {
            throw new CommandException(
                    "Could not create trader session"
            );
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setString("Token", session.getToken());
        data.setString("ProfileName", preset.getName());
        data.setTag("Preset", preset.writeToNBT());

        Server.sendData(
                player,
                EnumPacketClient.TraderSessionOpen,
                data
        );
    }

    @SubCommand(
            name = "editpreset",
            desc = "Edit trader catalog",
            usage = "<presetName>",
            permission = 2
    )
    public void editPreset(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (!(sender instanceof EntityPlayerMP)) {
            throw new CommandException(
                    "This command can only be used by a player"
            );
        }

        String presetName = joinName(args);

        if (presetName.isEmpty() || presetName.length() > 128) {
            throw new CommandException("Invalid preset name");
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;

        TraderProfileManager manager =
                TraderProfileManager.get(player.world);

        if (manager == null) {
            throw new CommandException(
                    "Trader profile storage is unavailable"
            );
        }

        TraderPreset preset = manager.getPreset(presetName);

        if (preset == null) {
            throw new CommandException(
                    "Trader preset '%s' was not found",
                    presetName
            );
        }

        TraderPresetEditorService.registerEditor(player, preset.getName());

        NBTTagCompound data = new NBTTagCompound();
        data.setTag("Preset", preset.writeToNBT());

        Server.sendData(
                player,
                EnumPacketClient.TraderPresetEditorOpen,
                data
        );
    }
}
