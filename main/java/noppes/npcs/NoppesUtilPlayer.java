package noppes.npcs;

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.oredict.OreDictionary;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.event.QuestEvent;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumPlayerPacket;
import noppes.npcs.controllers.DialogController;
import noppes.npcs.controllers.PlayerDataController;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.Dialog;
import noppes.npcs.controllers.data.DialogCategory;
import noppes.npcs.controllers.data.DialogOption;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.PlayerQuestData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestData;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.roles.RoleDialog;
import noppes.npcs.controllers.data.PlayerMail;

public class NoppesUtilPlayer {
	public static void teleportPlayer(EntityPlayerMP player, double x, double y, double z, int dimension) {
		if (player.dimension != dimension) {
			int dim = player.dimension;
			MinecraftServer server = player.getServer();
			WorldServer wor = server.getWorld(dimension);
			if (wor == null) {
				player.sendMessage(new TextComponentString("Broken transporter. Dimenion does not exist"));
				return;
			}

			player.setLocationAndAngles(x, y, z, player.rotationYaw, player.rotationPitch);
			server.getPlayerList().transferPlayerToDimension(player, dimension, new CustomTeleporter(wor));
			player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
			if (!wor.playerEntities.contains(player)) {
				wor.spawnEntity(player);
			}
		} else {
			player.connection.setPlayerLocation(x, y, z, player.rotationYaw, player.rotationPitch);
		}

		player.world.updateEntityWithOptionalForce(player, false);
	}
	public static void sendData(EnumPlayerPacket enu, Object... obs) {
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());

		try {
			if (!Server.fillBuffer(buffer, enu, obs)) {
				return;
			}

			CustomNpcs.ChannelPlayer.sendToServer(new FMLProxyPacket(buffer, "CustomNPCsPlayer"));
		} catch (IOException var4) {
			var4.printStackTrace();
		}

	}

	public static void dialogSelected(int diaId, int optionId, EntityPlayerMP player, EntityNPCInterface npc) {
		PlayerData data = PlayerData.get(player);
		if (data.dialogId == diaId) {
			if (data.dialogId < 0 && npc.advanced.role == 7) {
				String text = (String) ((RoleDialog) npc.roleInterface).optionsTexts.get(optionId);
				if (text != null && !text.isEmpty()) {
					Dialog d = new Dialog((DialogCategory) null);
					d.text = text;
					NoppesUtilServer.openDialog(player, npc, d);
				}

			} else {
				Dialog dialog = (Dialog) DialogController.instance.dialogs.get(data.dialogId);
				if (dialog != null) {
					if (!dialog.hasDialogs(player) && !dialog.hasOtherOptions()) {
						closeDialog(player, npc, true);
					} else {
						DialogOption option = (DialogOption) dialog.options.get(optionId);
						if (option != null && !EventHooks.onNPCDialogOption(npc, player, dialog, option)
								&& (option.optionType != 1 || option.isAvailable(player) && option.hasDialog())
								&& option.optionType != 2 && option.optionType != 0) {
							if (option.optionType == 3) {
								closeDialog(player, npc, true);
								if (npc.roleInterface != null) {
									npc.roleInterface.interact(player);
								}
							} else if (option.optionType == 1) {
								closeDialog(player, npc, false);
								NoppesUtilServer.openDialog(player, npc, option.getDialog());
							} else if (option.optionType == 4) {
								closeDialog(player, npc, true);
								NoppesUtilServer.runCommand(npc, npc.getName(), option.command, player);
							} else {
								closeDialog(player, npc, true);
							}

						} else {
							closeDialog(player, npc, true);
						}
					}
				}
			}
		}
	}

	public static void closeDialog(EntityPlayerMP player, EntityNPCInterface npc, boolean notifyClient) {
		PlayerData data = PlayerData.get(player);
		Dialog dialog = (Dialog) DialogController.instance.dialogs.get(data.dialogId);
		EventHooks.onNPCDialogClose(npc, player, dialog);
		if (notifyClient) {
			Server.sendData(player, EnumPacketClient.GUI_CLOSE, -1, new NBTTagCompound());
		}

		data.dialogId = -1;
	}


	public static void questCompletion(EntityPlayerMP player, int questId) {
		PlayerData data = PlayerData.get(player);
		PlayerQuestData playerdata = data.questData;
		QuestData questdata = (QuestData) playerdata.activeQuests.get(questId);

		if (questdata == null) {
			return;
		}

		Quest quest = questdata.quest;

		if (!quest.questInterface.isCompleted(player)) {
			return;
		}

		QuestEvent.QuestTurnedInEvent event = new QuestEvent.QuestTurnedInEvent(
				data.scriptData.getPlayer(), quest);

		event.expReward = quest.rewardExp;

		NpcAPI npcApi = NpcAPI.Instance();

		if (npcApi == null) {
			throw new IllegalStateException(
					"CustomNPCs API is unavailable during quest completion");
		}

		List<IItemStack> itemRewards = new ArrayList<>();

		for (Object value : quest.rewardItems.items) {
			if (!(value instanceof ItemStack)) {
				continue;
			}

			ItemStack item = (ItemStack) value;

			if (!item.isEmpty()) {
				IItemStack wrappedItem = npcApi.getIItemStack(item);

				if (wrappedItem != null) {
					itemRewards.add(wrappedItem);
				}
			}
		}

		if (!quest.randomReward) {
			event.itemRewards = itemRewards.toArray(new IItemStack[0]);
		} else if (!itemRewards.isEmpty()) {
			event.itemRewards = new IItemStack[] {
					itemRewards.get(player.getRNG().nextInt(itemRewards.size()))
			};
		}

		EventHooks.onQuestTurnedIn(data.scriptData, event);

		IItemStack[] rewards = event.itemRewards;

		if (rewards != null) {
			for (IItemStack item : rewards) {
				if (item != null) {
					NoppesUtilServer.GivePlayerItem(
							player, player, item.getMCItemStack());
				}
			}
		}

		quest.questInterface.handleComplete(player);

		if (event.expReward > 0) {
			NoppesUtilServer.playSound(
					player,
					SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
					0.1F,
					0.5F * ((player.world.rand.nextFloat()
							- player.world.rand.nextFloat()) * 0.7F + 1.8F));

			player.addExperience(event.expReward);
		}

		quest.factionOptions.addPoints(player);

		for (int slot = 0; slot < 3; ++slot) {
			PlayerMail mail = quest.getMail(slot);

			if (mail != null && mail.isValid()) {
				PlayerDataController.instance.addPlayerMessage(
						player.getServer(),
						player.getName(),
						mail
				);
			}
		}

		if (quest.command != null && !quest.command.trim().isEmpty()) {
			FakePlayer cplayer = EntityNPCInterface.CommandPlayer;

			cplayer.setWorld(player.world);
			cplayer.setPosition(player.posX, player.posY, player.posZ);

			String[] commands = quest.command.replace("\r", "").split("\n");

			for (String command : commands) {
				String currentCommand = command.trim();

				if (!currentCommand.isEmpty()) {
					NoppesUtilServer.runCommand(
							cplayer,
							"QuestCompletion",
							currentCommand,
							player);
				}
			}
		}

		PlayerQuestController.setQuestFinished(quest, player);

		for (int slot = 0; slot < 3; ++slot) {
			int nextQuestId = quest.getNextQuestId(slot);

			if (nextQuestId < 0) {
				continue;
			}

			boolean duplicate = false;

			for (int previousSlot = 0; previousSlot < slot; ++previousSlot) {
				if (quest.getNextQuestId(previousSlot) == nextQuestId) {
					duplicate = true;
					break;
				}
			}

			if (!duplicate) {
				Quest nextQuest = quest.getNextQuest(slot);

				if (nextQuest != null) {
					PlayerQuestController.addActiveQuest(nextQuest, player);
				}
			}
		}
	}

	public static boolean compareItems(ItemStack item, ItemStack item2, boolean ignoreDamage, boolean ignoreNBT) {
		if (!NoppesUtilServer.IsItemStackNull(item) && !NoppesUtilServer.IsItemStackNull(item2)) {
			boolean oreMatched = false;
			OreDictionary.itemMatches(item, item2, false);
			int[] ids = OreDictionary.getOreIDs(item);
			if (ids.length > 0) {
				int[] var6 = ids;
				int var7 = ids.length;

				for (int var8 = 0; var8 < var7; ++var8) {
					int id = var6[var8];
					boolean match1 = false;
					boolean match2 = false;
					Iterator var12 = OreDictionary.getOres(OreDictionary.getOreName(id)).iterator();

					while (var12.hasNext()) {
						ItemStack is = (ItemStack) var12.next();
						if (compareItemDetails(item, is, ignoreDamage, ignoreNBT)) {
							match1 = true;
						}

						if (compareItemDetails(item2, is, ignoreDamage, ignoreNBT)) {
							match2 = true;
						}
					}

					if (match1 && match2) {
						return true;
					}
				}
			}

			return compareItemDetails(item, item2, ignoreDamage, ignoreNBT);
		} else {
			return false;
		}
	}

	private static boolean compareItemDetails(ItemStack item, ItemStack item2, boolean ignoreDamage,
			boolean ignoreNBT) {
		if (item.getItem() != item2.getItem()) {
			return false;
		} else if (!ignoreDamage && item.getItemDamage() != -1 && item.getItemDamage() != item2.getItemDamage()) {
			return false;
		} else if (!ignoreNBT && item.getTagCompound() != null
				&& (item2.getTagCompound() == null || !item.getTagCompound().equals(item2.getTagCompound()))) {
			return false;
		} else {
			return ignoreNBT || item2.getTagCompound() == null || item.getTagCompound() != null;
		}
	}

	public static boolean compareItems(EntityPlayer player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
		int size = 0;

		for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
			ItemStack is = player.inventory.getStackInSlot(i);
			if (!NoppesUtilServer.IsItemStackNull(is) && compareItems(item, is, ignoreDamage, ignoreNBT)) {
				size += is.getCount();
			}
		}

		return size >= item.getCount();
	}

	public static void consumeItem(EntityPlayer player, ItemStack item, boolean ignoreDamage, boolean ignoreNBT) {
		if (!NoppesUtilServer.IsItemStackNull(item)) {
			int size = item.getCount();

			for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
				ItemStack is = player.inventory.getStackInSlot(i);
				if (!NoppesUtilServer.IsItemStackNull(is) && compareItems(item, is, ignoreDamage, ignoreNBT)) {
					if (size < is.getCount()) {
						is.splitStack(size);
						break;
					}

					size -= is.getCount();
					player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
				}
			}

		}
	}

	public static List countStacks(IInventory inv, boolean ignoreDamage, boolean ignoreNBT) {
		List list = new ArrayList();

		for (int i = 0; i < inv.getSizeInventory(); ++i) {
			ItemStack item = inv.getStackInSlot(i);
			if (!NoppesUtilServer.IsItemStackNull(item)) {
				boolean found = false;
				Iterator var7 = list.iterator();

				while (var7.hasNext()) {
					ItemStack is = (ItemStack) var7.next();
					if (compareItems(item, is, ignoreDamage, ignoreNBT)) {
						is.setCount(is.getCount() + item.getCount());
						found = true;
						break;
					}
				}

				if (!found) {
					list.add(item.copy());
				}
			}
		}

		return list;
	}
}
