package noppes.npcs.command;

import com.cheba.trader.TraderProfile;
import com.cheba.trader.TraderProfileManager;
import com.cheba.trader.TraderPreset;
import com.cheba.trader.TraderSessionManager;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import noppes.npcs.Server;
import noppes.npcs.api.CommandNoppesBase;
import noppes.npcs.constants.EnumPacketClient;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

/**
 * Команды управления пользовательскими торговыми профилями.
 */
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

    /**
     * Открывает торговый профиль указанному игроку.
     * <p>
     * Использование:
     * /customnpcs opentrader <player> <profileName>
     *
     * @param server сервер Minecraft
     * @param sender отправитель команды
     * @param args аргументы команды
     * @throws CommandException если игрок или профиль не найдены
     */
    @SubCommand(
            name = "opentrader",
            desc = "Open trader profile",
            usage = "<player> <profileName>",
            permission = 2
    )
    public void openTrader(
            MinecraftServer server,
            ICommandSender sender,
            String[] args
    ) throws CommandException {

        if (args.length < 2) {
            throw new CommandException(
                    "Usage: /customnpcs opentrader <player> <profileName>"
            );
        }

        EntityPlayer target = getPlayer(server, sender, args[0]);

        if (!(target instanceof EntityPlayerMP)) {
            throw new CommandException("Target player is not available");
        }

        EntityPlayerMP player = (EntityPlayerMP) target;
        String profileName = args[1];

        if (profileName.length() > 128) {
            throw new CommandException("Profile name is too long");
        }

        TraderProfileManager manager =
                TraderProfileManager.get(player.world);

        if (manager == null) {
            throw new CommandException("Trader profile storage is unavailable");
        }

        TraderProfile profile = manager.getProfileByName(profileName);

        if (profile == null) {
            throw new CommandException(
                    "Trader profile '%s' was not found",
                    profileName
            );
        }

        TraderPreset preset = manager.getPreset(profile.getPresetName());

        if (preset == null) {
            throw new CommandException(
                    "Catalog for trader profile '%s' was not found",
                    profileName
            );
        }

        TraderSessionManager.Session session =
                TraderSessionManager.open(player, profileName);

        if (session == null) {
            throw new CommandException(
                    "Could not create trader session"
            );
        }

        NBTTagCompound data = new NBTTagCompound();
        data.setString("Token", session.getToken());
        data.setString("ProfileName", profile.getName());
        data.setTag("Preset", preset.writeToNBT());

        Server.sendData(
                player,
                EnumPacketClient.TraderSessionOpen,
                data
        );
    }
}
