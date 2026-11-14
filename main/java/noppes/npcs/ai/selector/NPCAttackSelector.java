package noppes.npcs.ai.selector;

import com.google.common.base.Predicate;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import noppes.npcs.LogWriter;
import noppes.npcs.entity.EntityNPCInterface;

public class NPCAttackSelector implements Predicate {

	private final EntityNPCInterface npc;

	private int lastLogTick = Integer.MIN_VALUE;
	private int lastLoggedPlayerId = -1;

	public NPCAttackSelector(EntityNPCInterface npc) {
		this.npc = npc;
	}

	public boolean isEntityApplicable(EntityLivingBase entity) {
		if (!(entity instanceof EntityPlayerMP)) {
			return isOtherEntityApplicable(entity);
		}

		EntityPlayerMP player = (EntityPlayerMP) entity;

		String reason = null;
		boolean result = false;

		if (!entity.isEntityAlive()) {
			reason = "player is not alive";
		} else if (entity == this.npc) {
			reason = "player is NPC itself";
		} else if (entity.getHealth() < 1.0F) {
			reason = "player health below 1";
		} else if (!this.npc.isInRange(entity, (double) this.npc.stats.aggroRange)) {
			reason = "outside aggro range";
		} else if (this.npc.ais.directLOS
				&& !this.npc.getEntitySenses().canSee(entity)) {
			reason = "direct LOS failed";
		} else if (!this.npc.ais.attackInvisible
				&& entity.isPotionActive(MobEffects.INVISIBILITY)
				&& !this.npc.isInRange(entity, 3.0D)) {
			reason = "player is invisible";
		} else if (!this.isInsideReturnRange(entity)) {
			reason = "outside return-to-start range";
		} else if (!this.npc.faction.isAggressiveToPlayer(player)) {
			reason = "faction does not consider player hostile";
		} else if (player.capabilities.disableDamage) {
			reason = "player has disableDamage";
		} else {
			result = true;
			reason = "TARGET ACCEPTED";
		}

		this.logPlayerCheck(player, result, reason);

		return result;
	}

	private boolean isOtherEntityApplicable(EntityLivingBase entity) {
		if (entity.isEntityAlive()
				&& entity != this.npc
				&& this.npc.isInRange(entity, (double) this.npc.stats.aggroRange)
				&& entity.getHealth() >= 1.0F) {

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

				if (target.isKilled()) {
					return false;
				}

				return this.npc.advanced.attackOtherFactions
						&& this.npc.faction.isAggressiveToNpc(target);
			}
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

	private void logPlayerCheck(
			EntityPlayerMP player,
			boolean accepted,
			String reason
	) {
		long worldTick = this.npc.world.getTotalWorldTime();

		if (player.getEntityId() == this.lastLoggedPlayerId
				&& worldTick - this.lastLogTick < 20L) {
			return;
		}

		this.lastLoggedPlayerId = player.getEntityId();
		this.lastLogTick = (int) worldTick;

		double distance = this.npc.getDistance(player);

		LogWriter.info(
				"[NPCAttackSelector.isEntityApplicable] "
						+ "NPC=" + this.npc.getName()
						+ " npcId=" + this.npc.getEntityId()
						+ " player=" + player.getName()
						+ " playerId=" + player.getEntityId()
						+ " distance=" + String.format("%.2f", distance)
						+ " aggroRange=" + this.npc.stats.aggroRange
						+ " factionId=" + this.npc.faction.id
						+ " reputation="
						+ noppes.npcs.controllers.data.PlayerData
						.get(player).factionData
						.getFactionPoints(player, this.npc.faction.id)
						+ " directLOS=" + this.npc.ais.directLOS
						+ " accepted=" + accepted
						+ " reason=" + reason
		);
	}

	@Override
	public boolean apply(Object object) {
		return object instanceof EntityLivingBase
				&& this.isEntityApplicable((EntityLivingBase) object);
	}
}
