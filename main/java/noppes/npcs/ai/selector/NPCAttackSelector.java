package noppes.npcs.ai.selector;

import com.google.common.base.Predicate;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import noppes.npcs.entity.EntityNPCInterface;

public class NPCAttackSelector implements Predicate<EntityLivingBase> {

	private final EntityNPCInterface npc;

	public NPCAttackSelector(EntityNPCInterface npc) {
		this.npc = npc;
	}

	public boolean isEntityApplicable(EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayer)) {
			return isOtherEntityApplicable(entity);
		}

		EntityPlayer player = (EntityPlayer) entity;

		if (!entity.isEntityAlive()) {
			return false;
		}

		if (entity == this.npc) {
			return false;
		}

		if (!this.npc.canBePerceivedBy(player)) {
			return false;
		}

		if (!this.npc.isInRange(entity, (double) this.npc.stats.aggroRange)) {
			return false;
		}

		if (this.npc.ais.directLOS
				&& !this.npc.getEntitySenses().canSee(entity)) {
			return false;
		}

		if (!this.npc.ais.attackInvisible
				&& entity.isPotionActive(MobEffects.INVISIBILITY)
				&& !this.npc.isInRange(entity, 3.0D)) {
			return false;
		}

		if (!this.isInsideReturnRange(entity)) {
			return false;
		}

		if (!this.npc.faction.isAggressiveToPlayer(player)) {
			return false;
		}

		return !player.capabilities.disableDamage;
	}

	private boolean isOtherEntityApplicable(EntityLivingBase entity) {
		if (!entity.isEntityAlive()
				|| entity == this.npc
				|| !this.npc.isInRange(entity, (double) this.npc.stats.aggroRange)
				|| entity.getHealth() < 1.0F) {
			return false;
		}

		if (this.npc.ais.directLOS
				&& !this.npc.getEntitySenses().canSee(entity)) {
			return false;
		}

		if (!this.npc.ais.attackInvisible
				&& entity.isPotionActive(MobEffects.INVISIBILITY)
				&& !this.npc.isInRange(entity, 3.0D)) {
			return false;
		}

		if (!this.isInsideReturnRange(entity)) {
			return false;
		}

		if (entity instanceof EntityNPCInterface) {
			EntityNPCInterface target = (EntityNPCInterface) entity;

			return !target.isKilled()
					&& this.npc.advanced.attackOtherFactions
					&& this.npc.faction.isAggressiveToNpc(target);
		}

		return false;
	}

	private boolean isInsideReturnRange(EntityLivingBase entity) {
		if (this.npc.isFollower() || !this.npc.ais.shouldReturnHome()) {
			return true;
		}

		int allowedDistance = this.npc.stats.aggroRange * 2;

		if (this.npc.ais.getMovingType() == 1) {
			allowedDistance += this.npc.ais.walkingRange;
		}

		double distance = entity.getDistanceSq(
				(double) this.npc.getStartXPos(),
				this.npc.getStartYPos(),
				(double) this.npc.getStartZPos()
		);

		if (this.npc.ais.getMovingType() == 2) {
			int[] path = this.npc.ais.getCurrentMovingPath();

			if (path == null || path.length < 3) {
				return false;
			}

			distance = entity.getDistanceSq(
					(double) path[0],
					(double) path[1],
					(double) path[2]
			);
		}

		return distance <= (double) allowedDistance * allowedDistance;
	}

	@Override
	public boolean apply(EntityLivingBase entity) {
		return this.isEntityApplicable(entity);
	}
}
