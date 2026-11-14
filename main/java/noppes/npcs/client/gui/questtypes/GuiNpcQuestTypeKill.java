package noppes.npcs.client.gui.questtypes;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.ITextfieldListener;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.quests.QuestKill;

public class GuiNpcQuestTypeKill extends SubGuiInterface
		implements ITextfieldListener, ICustomScrollListener {

	private static final int TARGET_COUNT = 6;
	private static final int ROW_HEIGHT = 20;

	private final QuestKill quest;

	private GuiCustomScroll scroll;
	private GuiNpcTextField lastSelected;

	public GuiNpcQuestTypeKill(
			EntityNPCInterface npc,
			Quest q,
			GuiScreen parent) {

		this.npc = npc;
		this.parent = parent;
		this.title = "Quest Kill Setup";

		this.quest = (QuestKill) q.questInterface;

		this.setBackground("menubg.png");

		this.xSize = 356;
		this.ySize = 216;
		this.closeOnEsc = true;
	}

	public void initGui() {
		super.initGui();

		this.addLabel(new GuiNpcLabel(
				0,
				"You can fill in npc or player names too",
				this.guiLeft + 4,
				this.guiTop + 50
		));

		int row = 0;

		for (Map.Entry<String, Integer> target
				: this.quest.targets.entrySet()) {

			if (row >= TARGET_COUNT) {
				break;
			}

			int amount = target.getValue() == null
					? 1
					: target.getValue();

			this.addTargetFields(row, target.getKey(), amount);
			++row;
		}

		while (row < TARGET_COUNT) {
			this.addTargetFields(row, "", 1);
			++row;
		}

		List<String> entityList = new ArrayList<>();

		for (EntityEntry entry : ForgeRegistries.ENTITIES.getValues()) {
			Class<?> entityClass = entry.getEntityClass();

			try {
				if (EntityLivingBase.class.isAssignableFrom(entityClass)
						&& !EntityNPCInterface.class.isAssignableFrom(entityClass)
						&& !Modifier.isAbstract(entityClass.getModifiers())) {

					entityClass.getConstructor(World.class);
					entityList.add(entry.getName().toString());
				}
			} catch (SecurityException exception) {
				exception.printStackTrace();
			} catch (NoSuchMethodException ignored) {
			}
		}

		if (this.scroll == null) {
			this.scroll = new GuiCustomScroll(this, 0);
		}

		this.scroll.setList(entityList);
		this.scroll.setSize(130, 198);
		this.scroll.guiLeft = this.guiLeft + 220;
		this.scroll.guiTop = this.guiTop + 14;

		this.addScroll(this.scroll);

		this.addButton(new GuiNpcButton(
				0,
				this.guiLeft + 4,
				this.guiTop + 192,
				98,
				20,
				"gui.back"
		));

		this.scroll.visible = this.lastSelected != null;
	}

	private void addTargetFields(
			int row,
			String name,
			int amount) {

		int y = this.guiTop + 70 + row * ROW_HEIGHT;

		this.addTextField(new GuiNpcTextField(
				row,
				this,
				this.fontRenderer,
				this.guiLeft + 4,
				y,
				180,
				20,
				name
		));

		int amountFieldId = row + TARGET_COUNT;

		this.addTextField(new GuiNpcTextField(
				amountFieldId,
				this,
				this.fontRenderer,
				this.guiLeft + 186,
				y,
				24,
				20,
				String.valueOf(amount)
		));

		this.getTextField(amountFieldId).numbersOnly = true;
		this.getTextField(amountFieldId).setMinMaxDefault(
				1,
				Integer.MAX_VALUE,
				1
		);
	}

	protected void actionPerformed(GuiButton guibutton) {
		super.actionPerformed(guibutton);

		if (guibutton.id == 0) {
			this.close();
		}
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		this.scroll.visible = this.lastSelected != null;
	}

	public void unFocused(GuiNpcTextField textField) {
		if (textField.getId() < TARGET_COUNT) {
			this.lastSelected = textField;
		}

		this.saveTargets();
	}

	private void saveTargets() {
		TreeMap<String, Integer> targets = new TreeMap<>();

		for (int i = 0; i < TARGET_COUNT; ++i) {
			String name = this.getTextField(i).getText();

			if (!name.isEmpty()) {
				int amount = Math.max(
						1,
						this.getTextField(i + TARGET_COUNT).getInteger()
				);

				targets.put(name, amount);
			}
		}

		this.quest.targets = targets;
	}

	public void scrollClicked(
			int mouseX,
			int mouseY,
			int mouseButton,
			GuiCustomScroll guiCustomScroll) {

		if (this.lastSelected != null) {
			this.lastSelected.setText(guiCustomScroll.getSelected());
			this.saveTargets();
		}
	}

	public void scrollDoubleClicked(
			String selection,
			GuiCustomScroll scroll) {
	}
}
