package noppes.npcs.ai.target;

import com.google.common.base.Predicate;
import java.util.Collections;
import java.util.List;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget.Sorter;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.math.MathHelper;
import noppes.npcs.entity.EntityNPCInterface;

public class EntityAIClosestTarget extends EntityAITarget {

	private final Class<? extends EntityLivingBase> targetClass;
	private final int targetChance;
	private final Sorter theNearestAttackableTargetSorter;
	private final Predicate<? super EntityLivingBase> targetEntitySelector;
	private EntityLivingBase targetEntity;
	private final EntityNPCInterface npc;

	public EntityAIClosestTarget(
			EntityNPCInterface npc,
			Class<? extends EntityLivingBase> targetClass,
			int targetChance,
			boolean checkSight,
			boolean nearbyOnly,
			Predicate<? super EntityLivingBase> targetEntitySelector
	) {
		super(npc, checkSight, nearbyOnly);
		this.targetClass = targetClass;
		this.targetChance = targetChance;
		this.theNearestAttackableTargetSorter = new Sorter(npc);
		this.setMutexBits(1);
		this.targetEntitySelector = targetEntitySelector;
		this.npc = npc;
	}

	@Override
	public boolean shouldExecute() {
		if (this.targetChance > 0
				&& this.taskOwner.getRNG().nextInt(this.targetChance) != 0) {
			return false;
		}

		double distance = this.getTargetDistance();

		List<? extends EntityLivingBase> entities =
				this.taskOwner.world.getEntitiesWithinAABB(
						this.targetClass,
						this.taskOwner.getEntityBoundingBox().grow(
								distance,
								(double) MathHelper.ceil(distance / 2.0D),
								distance
						),
						this.targetEntitySelector
				);

		Collections.sort(entities, this.theNearestAttackableTargetSorter);

		if (entities.isEmpty()) {
			return false;
		}

		this.targetEntity = entities.get(0);
		return true;
	}

	@Override
	public void startExecuting() {
		EntityLivingBase selectedTarget = this.targetEntity;

		this.taskOwner.setAttackTarget(selectedTarget);

		if (selectedTarget instanceof EntityMob
				&& ((EntityMob) selectedTarget).getAttackTarget() == null) {
			((EntityMob) selectedTarget).setAttackTarget(this.taskOwner);
		}

		super.startExecuting();
	}
}
