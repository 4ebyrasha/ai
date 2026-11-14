package noppes.npcs.ai.target;

import com.google.common.base.Predicate;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIClosestTarget extends EntityAITarget {
	private final Class targetClass;
	private final int targetChance;
	private final Sorter theNearestAttackableTargetSorter;
	private final Predicate targetEntitySelector;
	private EntityLivingBase targetEntity;
	private EntityNPCInterface npc;

	public EntityAIClosestTarget(
			EntityNPCInterface npc,
			Class par2Class,
			int par3,
			boolean par4,
			boolean par5,
			Predicate par6IEntitySelector
	) {
		super(npc, par4, par5);
		this.targetClass = par2Class;
		this.targetChance = par3;
		this.theNearestAttackableTargetSorter = new Sorter(npc);
		this.setMutexBits(1);
		this.targetEntitySelector = par6IEntitySelector;
		this.npc = npc;
	}

	public boolean shouldExecute() {
		if (this.targetChance > 0
				&& this.taskOwner.getRNG().nextInt(this.targetChance) != 0) {
			return false;
		}

		double distance = this.getTargetDistance();

		List list = this.taskOwner.world.getEntitiesWithinAABB(
				this.targetClass,
				this.taskOwner.getEntityBoundingBox().grow(
						distance,
						(double) MathHelper.ceil(distance / 2.0D),
						distance
				),
				this.targetEntitySelector
		);

		Collections.sort(list, this.theNearestAttackableTargetSorter);

		if (list.isEmpty()) {
			return false;
		}

		this.targetEntity = (EntityLivingBase) list.get(0);
		return true;
	}

	public void startExecuting() {
		EntityLivingBase selectedTarget = this.targetEntity;

		this.taskOwner.setAttackTarget(selectedTarget);

		boolean assigned = this.taskOwner.getAttackTarget() == selectedTarget;

		LogWriter.info(
				"[EntityAIClosestTarget.startExecuting] "
						+ "NPC=" + this.npc.getName()
						+ " npcId=" + this.npc.getEntityId()
						+ " selectedTarget=" + selectedTarget.getName()
						+ " targetId=" + selectedTarget.getEntityId()
						+ " assigned=" + assigned
						+ " currentTarget="
						+ (this.taskOwner.getAttackTarget() == null
						? "null"
						: this.taskOwner.getAttackTarget().getName())
		);

		if (selectedTarget instanceof EntityMob
				&& ((EntityMob) selectedTarget).getAttackTarget() == null) {
			((EntityMob) selectedTarget).setAttackTarget(this.taskOwner);
		}

		super.startExecuting();
	}
}
