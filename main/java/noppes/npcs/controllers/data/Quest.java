package noppes.npcs.controllers.data;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import noppes.npcs.ICompatibilty;
import noppes.npcs.NpcMiscInventory;
import noppes.npcs.Server;
import noppes.npcs.VersionCompatibility;
import noppes.npcs.api.CustomNPCsException;
import noppes.npcs.api.IContainer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.api.handler.data.IQuestCategory;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.constants.EnumPacketClient;
import noppes.npcs.constants.EnumQuestCompletion;
import noppes.npcs.constants.EnumQuestRepeat;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.quests.QuestDialog;
import noppes.npcs.quests.QuestInterface;
import noppes.npcs.quests.QuestItem;
import noppes.npcs.quests.QuestKill;
import noppes.npcs.quests.QuestLocation;
import noppes.npcs.quests.QuestManual;

public class Quest implements ICompatibilty, IQuest {
	public int version;
	public int id;
	public int type;
	public EnumQuestRepeat repeat;
	public EnumQuestCompletion completion;
	public String title;
	public final QuestCategory category;
	public String logText;
	public String completeText;
	public String completerNpc;

	// legacy fields retained for compatibility
	public int nextQuestid;
	public String nextQuestTitle;

	// additional questss
	public int nextQuestid2;
	public String nextQuestTitle2;
	public int nextQuestid3;
	public String nextQuestTitle3;

	// legacy field retained for compatibility
	public PlayerMail mail;

	// additional mails
	public PlayerMail mail2;
	public PlayerMail mail3;

	public String command;
	public QuestInterface questInterface;
	public int rewardExp;
	public NpcMiscInventory rewardItems;
	public boolean randomReward;
	public FactionOptions factionOptions;

	public Quest(QuestCategory category) {
		this.version = VersionCompatibility.ModRev;
		this.id = -1;
		this.type = 0;
		this.repeat = EnumQuestRepeat.NONE;
		this.completion = EnumQuestCompletion.Npc;
		this.title = "default";
		this.logText = "";
		this.completeText = "";
		this.completerNpc = "";

		this.nextQuestid = -1;
		this.nextQuestTitle = "";
		this.nextQuestid2 = -1;
		this.nextQuestTitle2 = "";
		this.nextQuestid3 = -1;
		this.nextQuestTitle3 = "";

		this.mail = new PlayerMail();
		this.mail2 = new PlayerMail();
		this.mail3 = new PlayerMail();

		this.command = "";
		this.questInterface = new QuestItem();
		this.rewardExp = 0;
		this.rewardItems = new NpcMiscInventory(9);
		this.randomReward = false;
		this.factionOptions = new FactionOptions();
		this.category = category;
	}

	public void readNBT(NBTTagCompound compound) {
		this.id = compound.getInteger("Id");
		this.readNBTPartial(compound);
	}

	public void readNBTPartial(NBTTagCompound compound) {
		this.version = compound.getInteger("ModRev");
		VersionCompatibility.CheckAvailabilityCompatibility(this, compound);

		this.setType(compound.getInteger("Type"));

		this.title = compound.getString("Title");
		this.logText = compound.getString("Text");
		this.completeText = compound.getString("CompleteText");
		this.completerNpc = compound.getString("CompleterNpc");
		this.command = compound.getString("QuestCommand");

		// first next quest uses the original NBT keys
		this.nextQuestid = compound.getInteger("NextQuestId");
		this.nextQuestTitle = compound.getString("NextQuestTitle");

		// additional next quests use new keys
		this.nextQuestid2 = compound.hasKey("NextQuestId2", 99)
				? compound.getInteger("NextQuestId2") : -1;
		this.nextQuestTitle2 = compound.getString("NextQuestTitle2");

		this.nextQuestid3 = compound.hasKey("NextQuestId3", 99)
				? compound.getInteger("NextQuestId3") : -1;
		this.nextQuestTitle3 = compound.getString("NextQuestTitle3");

		this.updateNextQuestTitles();

		this.randomReward = compound.getBoolean("RandomReward");
		this.rewardExp = compound.getInteger("RewardExp");
		this.rewardItems.setFromNBT(compound.getCompoundTag("Rewards"));

		this.completion = EnumQuestCompletion.values()[compound.getInteger("QuestCompletion")];
		this.repeat = EnumQuestRepeat.values()[compound.getInteger("QuestRepeat")];

		this.questInterface.readEntityFromNBT(compound);
		this.factionOptions.readFromNBT(compound.getCompoundTag("QuestFactionPoints"));

		this.mail = new PlayerMail();
		this.mail.readNBT(compound.getCompoundTag("QuestMail"));

		this.mail2 = new PlayerMail();
		if (compound.hasKey("QuestMail2", 10)) {
			this.mail2.readNBT(compound.getCompoundTag("QuestMail2"));
		}

		this.mail3 = new PlayerMail();
		if (compound.hasKey("QuestMail3", 10)) {
			this.mail3.readNBT(compound.getCompoundTag("QuestMail3"));
		}
	}

	private void updateNextQuestTitles() {
		for (int slot = 0; slot < 3; ++slot) {
			Quest nextQuest = this.getNextQuest(slot);
			String title = nextQuest == null ? "" : nextQuest.title;

			this.setNextQuestTitle(slot, title);
		}
	}

	private void setNextQuestTitle(int slot, String title) {
		String value = title == null ? "" : title;

		if (slot == 0) {
			this.nextQuestTitle = value;
		} else if (slot == 1) {
			this.nextQuestTitle2 = value;
		} else if (slot == 2) {
			this.nextQuestTitle3 = value;
		}
	}

	public void setType(int questType) {
		this.type = questType;

		if (this.type == 0) {
			this.questInterface = new QuestItem();
		} else if (this.type == 1) {
			this.questInterface = new QuestDialog();
		} else if (this.type != 2 && this.type != 4) {
			if (this.type == 3) {
				this.questInterface = new QuestLocation();
			} else if (this.type == 5) {
				this.questInterface = new QuestManual();
			}
		} else {
			this.questInterface = new QuestKill();
		}

		if (this.questInterface != null) {
			this.questInterface.questId = this.id;
		}
	}

	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setInteger("Id", this.id);
		return this.writeToNBTPartial(compound);
	}

	public NBTTagCompound writeToNBTPartial(NBTTagCompound compound) {
		compound.setInteger("ModRev", this.version);
		compound.setInteger("Type", this.type);
		compound.setString("Title", this.title);
		compound.setString("Text", this.logText);
		compound.setString("CompleteText", this.completeText);
		compound.setString("CompleterNpc", this.completerNpc);

		compound.setInteger("NextQuestId", this.nextQuestid);
		compound.setString("NextQuestTitle", this.nextQuestTitle);

		compound.setInteger("NextQuestId2", this.nextQuestid2);
		compound.setString("NextQuestTitle2", this.nextQuestTitle2);
		compound.setInteger("NextQuestId3", this.nextQuestid3);
		compound.setString("NextQuestTitle3", this.nextQuestTitle3);

		compound.setInteger("RewardExp", this.rewardExp);
		compound.setTag("Rewards", this.rewardItems.getToNBT());
		compound.setString("QuestCommand", this.command);
		compound.setBoolean("RandomReward", this.randomReward);
		compound.setInteger("QuestCompletion", this.completion.ordinal());
		compound.setInteger("QuestRepeat", this.repeat.ordinal());

		this.questInterface.writeEntityToNBT(compound);

		compound.setTag(
				"QuestFactionPoints",
				this.factionOptions.writeToNBT(new NBTTagCompound())
		);

		compound.setTag("QuestMail", this.getMail(0).writeNBT());

		compound.setTag("QuestMail2", this.getMail(1).writeNBT());
		compound.setTag("QuestMail3", this.getMail(2).writeNBT());

		return compound;
	}

	public PlayerMail getMail(int slot) {
		if (slot == 0) {
			if (this.mail == null) {
				this.mail = new PlayerMail();
			}

			return this.mail;
		}

		if (slot == 1) {
			if (this.mail2 == null) {
				this.mail2 = new PlayerMail();
			}

			return this.mail2;
		}

		if (slot == 2) {
			if (this.mail3 == null) {
				this.mail3 = new PlayerMail();
			}

			return this.mail3;
		}

		return null;
	}

	public void clearMail(int slot) {
		if (slot == 0) {
			this.mail = new PlayerMail();
		} else if (slot == 1) {
			this.mail2 = new PlayerMail();
		} else if (slot == 2) {
			this.mail3 = new PlayerMail();
		}
	}

	public int getNextQuestId(int slot) {
		if (slot == 0) {
			return this.nextQuestid;
		}

		if (slot == 1) {
			return this.nextQuestid2;
		}

		if (slot == 2) {
			return this.nextQuestid3;
		}

		return -1;
	}

	public String getNextQuestTitle(int slot) {
		if (slot == 0) {
			return this.nextQuestTitle;
		}

		if (slot == 1) {
			return this.nextQuestTitle2;
		}

		if (slot == 2) {
			return this.nextQuestTitle3;
		}

		return "";
	}

	public Quest getNextQuest(int slot) {
		if (slot < 0 || slot >= 3 || QuestController.instance == null) {
			return null;
		}

		return (Quest) QuestController.instance.quests.get(this.getNextQuestId(slot));
	}

	public void setNextQuest(int slot, int questId, String title) {
		if (slot == 0) {
			this.nextQuestid = questId;
			this.nextQuestTitle = title == null ? "" : title;
		} else if (slot == 1) {
			this.nextQuestid2 = questId;
			this.nextQuestTitle2 = title == null ? "" : title;
		} else if (slot == 2) {
			this.nextQuestid3 = questId;
			this.nextQuestTitle3 = title == null ? "" : title;
		}
	}

	public boolean hasNewQuest() {
		return this.getNextQuest() != null;
	}

	public Quest getNextQuest() {
		return this.getNextQuest(0);
	}

	public boolean complete(EntityPlayer player, QuestData data) {
		if (this.completion == EnumQuestCompletion.Instant) {
			Server.sendData((EntityPlayerMP) player, EnumPacketClient.QUEST_COMPLETION, data.quest.id);
			return true;
		}

		return false;
	}

	public Quest copy() {
		Quest quest = new Quest(this.category);
		quest.readNBT(this.writeToNBT(new NBTTagCompound()));
		return quest;
	}

	public int getVersion() {
		return this.version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public int getId() {
		return this.id;
	}

	public String getName() {
		return this.title;
	}

	public int getType() {
		return this.type;
	}

	public IQuestCategory getCategory() {
		return this.category;
	}

	public void save() {
		QuestController.instance.saveQuest(this.category, this);
	}

	public void setName(String name) {
		this.title = name;
	}

	public String getLogText() {
		return this.logText;
	}

	public void setLogText(String text) {
		this.logText = text;
	}

	public String getCompleteText() {
		return this.completeText;
	}

	public void setCompleteText(String text) {
		this.completeText = text;
	}

	@Override
	public void setNextQuest(IQuest quest) {
		if (quest == null) {
			this.setNextQuest(0, -1, "");
		} else {
			if (quest.getId() < 0) {
				throw new CustomNPCsException("Quest id is lower than 0", new Object[0]);
			}

			this.setNextQuest(0, quest.getId(), quest.getName());
		}
	}

	public String getNpcName() {
		return this.completerNpc;
	}

	public void setNpcName(String name) {
		this.completerNpc = name;
	}

	public IQuestObjective[] getObjectives(IPlayer player) {
		if (!player.hasActiveQuest(this.id)) {
			throw new CustomNPCsException("Player doesnt have this quest active.", new Object[0]);
		}

		return this.questInterface.getObjectives(player.getMCEntity());
	}

	public boolean getIsRepeatable() {
		return this.repeat != EnumQuestRepeat.NONE;
	}

	public IContainer getRewards() {
		return NpcAPI.Instance().getIContainer((IInventory) this.rewardItems);
	}
}
