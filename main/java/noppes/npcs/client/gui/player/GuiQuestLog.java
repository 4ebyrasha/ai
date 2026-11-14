
package noppes.npcs.client.gui.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.translation.I18n;
import noppes.npcs.api.handler.data.IQuestObjective;
import noppes.npcs.client.CustomNpcResourceListener;
import noppes.npcs.client.NoppesUtil;
import noppes.npcs.client.TextBlockClient;
import noppes.npcs.client.gui.util.GuiButtonNextPage;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiMenuSideButton;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.ITopButtonListener;
import noppes.npcs.controllers.PlayerQuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.util.NaturalOrderComparator;

@SuppressWarnings("deprecation")
public class GuiQuestLog extends GuiNPCInterface implements ITopButtonListener, ICustomScrollListener {
	private static final int MAX_LINES = 10;

	private final ResourceLocation resource = new ResourceLocation("customnpcs", "textures/gui/standardbg.png");

	public HashMap<String, List<Quest>> activeQuests = new HashMap<>();
	private HashMap<String, Quest> categoryQuests = new HashMap<>();
	public Quest selectedQuest = null;
	public String selectedCategory = "";

	private final EntityPlayer player;
	private GuiCustomScroll scroll;
	private final HashMap<Integer, GuiMenuSideButton> sideButtons = new HashMap<>();

	private boolean noQuests = false;
	private int currentPage = 0;
	private int maxPages = 1;

	private TextBlockClient textblock = null;
	private final Minecraft mc = Minecraft.getMinecraft();

	public GuiQuestLog(EntityPlayer player) {
		this.player = player;
		this.xSize = 280;
		this.ySize = 180;
		this.drawDefaultBackground = false;
	}

	public void initGui() {
		super.initGui();

		this.activeQuests.clear();

		for (Object entry : PlayerQuestController.getActiveQuests(this.player)) {
			Quest quest = (Quest) entry;
			String category = quest.category.title;

			this.activeQuests.computeIfAbsent(category, key -> new ArrayList<>()).add(quest);
		}

		this.sideButtons.clear();
		this.guiTop += 10;
		this.noQuests = this.activeQuests.isEmpty();

		if (!this.noQuests) {
			List<String> categories = new ArrayList<>(this.activeQuests.keySet());

			Comparator<String> comparator = new NaturalOrderComparator()::compare;
			categories.sort(comparator);

			if (!this.activeQuests.containsKey(this.selectedCategory)) {
				this.selectedCategory = categories.get(0);
			}

			for (int i = 0; i < categories.size(); ++i) {
				String category = categories.get(i);

				this.sideButtons.put(i,
						new GuiMenuSideButton(i, this.guiLeft - 69, this.guiTop + 2 + i * 21, 70, 22, category));
			}

			int selectedCategoryIndex = categories.indexOf(this.selectedCategory);
			GuiMenuSideButton selectedButton = this.sideButtons.get(selectedCategoryIndex);

			if (selectedButton != null) {
				selectedButton.active = true;
			}

			if (this.scroll == null) {
				this.scroll = new GuiCustomScroll(this, 0);
			}

			HashMap<String, Quest> questsByTitle = new HashMap<>();
			List<Quest> selectedCategoryQuests = this.activeQuests.get(this.selectedCategory);

			for (Quest quest : selectedCategoryQuests) {
				questsByTitle.put(quest.title, quest);
			}

			this.categoryQuests = questsByTitle;
			this.scroll.setList(new ArrayList<>(questsByTitle.keySet()));
			this.scroll.setSize(134, 174);
			this.scroll.guiLeft = this.guiLeft + 5;
			this.scroll.guiTop = this.guiTop + 15;
			this.addScroll(this.scroll);

			this.addButton(new GuiButtonNextPage(1, this.guiLeft + 286, this.guiTop + 114, true));
			this.addButton(new GuiButtonNextPage(2, this.guiLeft + 144, this.guiTop + 114, false));

			this.getButton(1).visible = this.selectedQuest != null && this.currentPage < this.maxPages - 1;
			this.getButton(2).visible = this.selectedQuest != null && this.currentPage > 0;
		}
	}

	protected void actionPerformed(GuiButton guibutton) {
		if (guibutton instanceof GuiButtonNextPage) {
			if (guibutton.id == 1) {
				++this.currentPage;
				this.initGui();
			}

			if (guibutton.id == 2) {
				--this.currentPage;
				this.initGui();
			}
		}
	}

	public void drawScreen(int i, int j, float f) {
		if (this.scroll != null) {
			this.scroll.visible = !this.noQuests;
		}

		this.drawDefaultBackground();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.renderEngine.bindTexture(this.resource);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, 252, 195);
		this.drawTexturedModalRect(this.guiLeft + 252, this.guiTop, 188, 0, 67, 195);
		super.drawScreen(i, j, f);

		if (this.noQuests) {
			this.mc.fontRenderer.drawString(I18n.translateToLocal("quest.noquests"), this.guiLeft + 84,
					this.guiTop + 80, CustomNpcResourceListener.DefaultTextColor);
		} else {
			GuiMenuSideButton[] buttons = this.sideButtons.values()
					.toArray(new GuiMenuSideButton[0]);

			for (GuiMenuSideButton button : buttons) {
				button.drawButton(this.mc, i, j, f);
			}

			this.mc.fontRenderer.drawString(this.selectedCategory, this.guiLeft + 5, this.guiTop + 5,
					CustomNpcResourceListener.DefaultTextColor);

			if (this.selectedQuest != null) {
				this.drawProgress();
				this.drawQuestText();

				GlStateManager.pushMatrix();
				GlStateManager.translate((float) (this.guiLeft + 148), (float) this.guiTop, 0.0F);
				GlStateManager.scale(1.24F, 1.24F, 1.24F);

				String title = I18n.translateToLocal(this.selectedQuest.title);
				this.fontRenderer.drawString(title, (130 - this.fontRenderer.getStringWidth(title)) / 2, 4,
						CustomNpcResourceListener.DefaultTextColor);

				GlStateManager.popMatrix();

				this.drawHorizontalLine(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 17,
						-16777216 + CustomNpcResourceListener.DefaultTextColor);
			}
		}
	}

	private void drawQuestText() {
		if (this.textblock != null) {
			for (int i = 0; i < MAX_LINES; ++i) {
				int index = i + this.currentPage * MAX_LINES;

				if (index < this.textblock.lines.size()) {
					String text = ((ITextComponent) this.textblock.lines.get(index)).getFormattedText();

					this.fontRenderer.drawString(text, this.guiLeft + 142,
							this.guiTop + 20 + i * this.fontRenderer.FONT_HEIGHT,
							CustomNpcResourceListener.DefaultTextColor);
				}
			}
		}
	}

	private void drawProgress() {
		String title = I18n.translateToLocal("quest.objectives") + ":";

		this.mc.fontRenderer.drawString(title, this.guiLeft + 142, this.guiTop + 130,
				CustomNpcResourceListener.DefaultTextColor);

		this.drawHorizontalLine(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 140,
				-16777216 + CustomNpcResourceListener.DefaultTextColor);

		int yoffset = this.guiTop + 144;
		IQuestObjective[] objectives = this.selectedQuest.questInterface.getObjectives(this.player);

		for (IQuestObjective objective : objectives) {
			this.mc.fontRenderer.drawString("- " + objective.getText(), this.guiLeft + 142, yoffset,
					CustomNpcResourceListener.DefaultTextColor);

			yoffset += 10;
		}

		this.drawHorizontalLine(this.guiLeft + 142, this.guiLeft + 312, this.guiTop + 178,
				-16777216 + CustomNpcResourceListener.DefaultTextColor);
	}

	public void mouseClicked(int i, int j, int k) {
		super.mouseClicked(i, j, k);

		if (k == 0) {
			if (this.scroll != null) {
				this.scroll.mouseClicked(i, j, k);
			}

			for (GuiMenuSideButton button : new ArrayList<>(this.sideButtons.values())) {
				if (button.mousePressed(this.mc, i, j)) {
					this.sideButtonPressed(button);
				}
			}
		}
	}

	private void sideButtonPressed(GuiMenuSideButton button) {
		if (!button.active) {
			NoppesUtil.clickSound();
			this.selectedCategory = button.displayString;
			this.selectedQuest = null;
			this.currentPage = 0;
			this.initGui();
		}
	}

	public void scrollClicked(int i, int j, int k, GuiCustomScroll scroll) {
		if (scroll.hasSelected()) {
			this.selectedQuest = this.categoryQuests.get(scroll.getSelected());
			this.textblock = new TextBlockClient(this.selectedQuest.getLogText(), 172, true, this.player);

			this.maxPages = Math.max(1, MathHelper.ceil(
					(float) this.textblock.lines.size() / MAX_LINES));

			this.currentPage = 0;
			this.initGui();
		}
	}

	public void keyTyped(char c, int i) {
		if (i == 1 || i == this.mc.gameSettings.keyBindInventory.getKeyCode()) {
			this.mc.displayGuiScreen(null);
			this.mc.setIngameFocus();
		}
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void save() {
	}

	public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
	}
}
