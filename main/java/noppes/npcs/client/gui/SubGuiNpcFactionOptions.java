package noppes.npcs.client.gui;

import java.util.HashMap;
import java.util.Vector;

import net.minecraft.client.gui.GuiButton;
import noppes.npcs.client.Client;
import noppes.npcs.client.gui.util.GuiCustomScroll;
import noppes.npcs.client.gui.util.GuiNpcButton;
import noppes.npcs.client.gui.util.GuiNpcLabel;
import noppes.npcs.client.gui.util.GuiNpcTextField;
import noppes.npcs.client.gui.util.ICustomScrollListener;
import noppes.npcs.client.gui.util.IScrollData;
import noppes.npcs.client.gui.util.SubGuiInterface;
import noppes.npcs.constants.EnumPacketServer;
import noppes.npcs.controllers.data.FactionOptions;

public class SubGuiNpcFactionOptions extends SubGuiInterface
		implements IScrollData, ICustomScrollListener {

	private static final int ASSIGNMENT_BUTTON_BASE = 100;
	private static final int REMOVE_BUTTON_BASE = 200;

	private final FactionOptions options;
	private final HashMap<String, Integer> data = new HashMap<>();

	private final GuiCustomScroll scrollFactions;

	private int selectedFaction = -1;
	private int selectedAssignment = -1;

	public SubGuiNpcFactionOptions(FactionOptions options) {
		this.options = options == null ? new FactionOptions() : options;

		this.scrollFactions = new GuiCustomScroll(this, 0);
		this.scrollFactions.setSize(158, 228);

		this.setBackground("menubg.png");
		this.xSize = 420;
		this.ySize = 236;
		this.closeOnEsc = true;
	}

	public void initPacket() {
		Client.sendData(EnumPacketServer.FactionsGet);
	}

	@Override
	public void initGui() {
		super.initGui();

		this.scrollFactions.guiLeft = this.guiLeft + 258;
		this.scrollFactions.guiTop = this.guiTop + 4;
		this.scrollFactions.setSize(158, 228);
		this.addScroll(this.scrollFactions);

		int row = 0;

		for (int slot = 0; slot < FactionOptions.MAX_FACTIONS; ++slot) {
			int factionId = this.options.getFactionId(slot);

			if (factionId < 0) {
				continue;
			}

			String marker = this.selectedAssignment == slot ? "> " : "";
			String direction = this.options.isFactionDecrease(slot) ? "-" : "+";

			String label = marker + (slot + 1) + ". "
					+ this.getFactionName(factionId)
					+ " (" + direction + this.options.getFactionPoints(slot) + ")";

			int y = this.guiTop + 4 + row * 18;

			this.addButton(new GuiNpcButton(
					ASSIGNMENT_BUTTON_BASE + slot,
					this.guiLeft + 4,
					y,
					222,
					18,
					label
			));

			this.addButton(new GuiNpcButton(
					REMOVE_BUTTON_BASE + slot,
					this.guiLeft + 228,
					y,
					20,
					18,
					"X"
			));

			++row;
		}

		this.addButton(new GuiNpcButton(
				2,
				this.guiLeft + 4,
				this.guiTop + 172,
				76,
				20,
				new String[] { "gui.increase", "gui.decrease" },
				this.selectedAssignment >= 0
						&& this.options.isFactionDecrease(this.selectedAssignment) ? 1 : 0
		));

		this.addLabel(new GuiNpcLabel(
				0,
				"faction.points",
				this.guiLeft + 84,
				this.guiTop + 180
		));

		int points = this.selectedAssignment >= 0
				? this.options.getFactionPoints(this.selectedAssignment)
				: 10;

		this.addTextField(new GuiNpcTextField(
				1,
				this,
				this.fontRenderer,
				this.guiLeft + 140,
				this.guiTop + 172,
				55,
				20,
				Integer.toString(points)
		));

		this.getTextField(1).numbersOnly = true;
		this.getTextField(1).setMinMaxDefault(1, 100000, 10);

		this.addButton(new GuiNpcButton(
				3,
				this.guiLeft + 199,
				this.guiTop + 172,
				58,
				20,
				"gui.add",
				this.canAddSelectedFaction()
		));

		this.addButton(new GuiNpcButton(
				66,
				this.guiLeft + 82,
				this.guiTop + 208,
				92,
				20,
				"gui.done"
		));
	}

	private boolean canAddSelectedFaction() {
		if (this.selectedFaction < 0) {
			return false;
		}

		int targetSlot = this.selectedAssignment >= 0
				? this.selectedAssignment
				: this.options.getFirstEmptySlot();

		return targetSlot >= 0
				&& !this.options.hasFactionExcept(this.selectedFaction, targetSlot);
	}

	private void addOrUpdateFaction() {
		boolean editing = this.selectedAssignment >= 0;

		int targetSlot = editing
				? this.selectedAssignment
				: this.options.getFirstEmptySlot();

		if (targetSlot < 0 || this.selectedFaction < 0
				|| this.options.hasFactionExcept(this.selectedFaction, targetSlot)) {
			return;
		}

		int points = 10;
		GuiNpcTextField pointsField = this.getTextField(1);

		if (pointsField != null && pointsField.isInteger()) {
			points = pointsField.getInteger();
		}

		points = Math.max(1, Math.min(100000, points));

		GuiNpcButton directionButton = this.getButton(2);
		boolean decrease = directionButton != null
				&& directionButton.getValue() == 1;

		this.options.setFaction(
				targetSlot,
				this.selectedFaction,
				decrease,
				points
		);

		// Keep edit mode only when an existing assignment was selected.
		if (editing) {
			this.selectedAssignment = targetSlot;
		}
	}

	private String getFactionName(int factionId) {
		for (String name : this.data.keySet()) {
			Integer id = this.data.get(name);

			if (id != null && id.intValue() == factionId) {
				return name;
			}
		}

		return "Unknown faction #" + factionId;
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		int id = button.id;

		if (id == 66) {
			this.close();
			return;
		}

		if (id == 3) {
			this.addOrUpdateFaction();
			this.initGui();
			return;
		}

		if (id >= ASSIGNMENT_BUTTON_BASE
				&& id < ASSIGNMENT_BUTTON_BASE + FactionOptions.MAX_FACTIONS) {

			int slot = id - ASSIGNMENT_BUTTON_BASE;

			if (this.selectedAssignment == slot) {
				this.selectedAssignment = -1;
				this.selectedFaction = -1;
				this.scrollFactions.setSelected(null);
			} else {
				this.selectedAssignment = slot;
				this.selectedFaction = this.options.getFactionId(slot);

				this.scrollFactions.setSelected(
						this.getFactionName(this.selectedFaction)
				);
			}

			this.initGui();
			return;
		}

		if (id >= REMOVE_BUTTON_BASE
				&& id < REMOVE_BUTTON_BASE + FactionOptions.MAX_FACTIONS) {

			int slot = id - REMOVE_BUTTON_BASE;
			int removedFaction = this.options.getFactionId(slot);

			this.options.setFaction(slot, -1, false, 100);

			if (this.selectedAssignment == slot) {
				this.selectedAssignment = -1;
			}

			if (this.selectedFaction == removedFaction) {
				this.selectedFaction = -1;
				this.scrollFactions.setSelected(null);
			}

			this.initGui();
		}
	}

	@Override
	public void scrollClicked(int x, int y, int button, GuiCustomScroll scroll) {
		if (scroll.id != 0) {
			return;
		}

		Integer factionId = this.data.get(scroll.getSelected());
		this.selectedFaction = factionId == null ? -1 : factionId;

		GuiNpcButton addButton = this.getButton(3);

		if (addButton != null) {
			addButton.setEnabled(this.canAddSelectedFaction());
		}
	}

	@Override
	public void setData(Vector<String> list, HashMap<String, Integer> data) {
		String selectedName = this.scrollFactions.getSelected();

		this.data.clear();

		if (data != null) {
			this.data.putAll(data);
		}

		Vector<String> factionList = list == null
				? new Vector<>()
				: new Vector<>(list);

		this.scrollFactions.setList(factionList);

		if (selectedName != null) {
			this.scrollFactions.setSelected(selectedName);
		}

		this.initGui();
	}

	@Override
	public void setSelected(String selected) {
	}

	@Override
	public void scrollDoubleClicked(String selection, GuiCustomScroll scroll) {
	}

	@Override
	public void save() {
	}
}
