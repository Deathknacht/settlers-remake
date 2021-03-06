/*******************************************************************************
 * Copyright (c) 2016
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.ai.army;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.action.SetMaterialProductionAction.EMaterialProductionType;
import jsettlers.input.tasks.MoveToGuiTask;
import jsettlers.input.tasks.SetMaterialProductionGuiTask;
import jsettlers.input.tasks.UpgradeSoldiersGuiTask;
import jsettlers.logic.map.grid.movable.MovableGrid;
import jsettlers.logic.player.Player;
import jsettlers.network.client.interfaces.ITaskScheduler;

/**
 * This general is named winner because his attacks and defence should be very hard for human enemies. This should be realized by creating locally
 * superiority. (You can kill 200 bowmen with just 100 bowmen if you fight 100 vs 20 in loops. This general should lay the focus on some swordsmen to
 * occupy own towers, 20 spearmen to defeat rushes and the rest only bowmen because in mass this is the strongest military unit. It upgrades bowmen
 * first because this is the main unit and the 20 defeating spearmen defeats with lv1 as well. This general should store bows until level3 is reached
 * to get as many level3 bowmen as posibble. TODO: store bows until level3 is reached TODO: group soldiers in direction of enemy groups to defeat them
 * TODO: group soldiers in direction of enemy groups to attack them
 *
 * @author codingberlin
 */
public class ConfigurableGeneral implements ArmyGeneral {
	private static final byte MIN_ATTACKER_COUNT = 20;
	private static final byte MIN_SWORDSMEN_COUNT = 10;
	private static final byte MIN_PIKEMEN_COUNT = 20;
	private static final int BOWMEN_COUNT_OF_KILLING_INFANTRY = 300;
	private static final EBuildingType[] MIN_BUILDING_REQUIREMENTS_FOR_ATTACK =
			{EBuildingType.COALMINE, EBuildingType.IRONMINE, EBuildingType.IRONMELT, EBuildingType.WEAPONSMITH, EBuildingType.BARRACK};

	private final AiStatistics aiStatistics;
	private final Player player;
	private final ITaskScheduler taskScheduler;
	private final MovableGrid movableGrid;
	private float attackerCountFactor;

	public ConfigurableGeneral(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler, float
			attackerCountFactor) {
		this.aiStatistics = aiStatistics;
		this.player = player;
		this.taskScheduler = taskScheduler;
		this.movableGrid = movableGrid;
		this.attackerCountFactor = attackerCountFactor;
	}

	@Override
	public void commandTroops() {
		Situation situation = calculateSituation(player.playerId);
		if (aiStatistics.getEnemiesInTownOf(player.playerId).size() > 0) {
			defend(situation);
		} else if (enemiesAreAlive()) {
			byte weakestEnemyId = getWeakestEnemy();
			Situation enemySituation = calculateSituation(weakestEnemyId);
			boolean infantryWouldDie = wouldInfantryDie(enemySituation);
			if (attackIsPossible(situation, enemySituation, infantryWouldDie)) {
				attack(situation, infantryWouldDie);
			}
		}
	}

	private boolean attackIsPossible(Situation situation, Situation enemySituation, boolean infantryWouldDie) {
		for (EBuildingType requiredType : MIN_BUILDING_REQUIREMENTS_FOR_ATTACK) {
			if (aiStatistics.getNumberOfBuildingTypeForPlayer(requiredType, player.playerId) < 1) {
				return false;
			}
		}

		float combatStrength = 1F; //TODO: use when storages for gold are working: player.getCombatStrengthInformation().getCombatStrength(false);
		float effectiveAttackerCount;
		if (infantryWouldDie) {
			effectiveAttackerCount = situation.bowmenPositions.size() * combatStrength;
		} else {
			effectiveAttackerCount = situation.soldiersCount() * combatStrength;
		}
		return effectiveAttackerCount >= MIN_ATTACKER_COUNT && effectiveAttackerCount * attackerCountFactor > enemySituation.soldiersCount();

	}

	private boolean wouldInfantryDie(Situation enemySituation) {
		return enemySituation.bowmenPositions.size() > BOWMEN_COUNT_OF_KILLING_INFANTRY;
	}

	private boolean enemiesAreAlive() {
		for (byte enemyId : aiStatistics.getEnemiesOf(player.playerId)) {
			if (aiStatistics.isAlive(enemyId)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void levyUnits() {
		if (!upgradeSoldiers(ESoldierType.BOWMAN))
			if (!upgradeSoldiers(ESoldierType.PIKEMAN))
				upgradeSoldiers(ESoldierType.SWORDSMAN);

		int missingSwordsmenCount = Math.max(0, MIN_SWORDSMEN_COUNT
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L1, player.playerId).size()
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L2, player.playerId).size()
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L3, player.playerId).size());
		int missingSpearmenCount = Math.max(0, MIN_PIKEMEN_COUNT
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L1, player.playerId).size()
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L2, player.playerId).size()
				- aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L3, player.playerId).size());
		int bowmenCount = aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L1, player.playerId).size()
				+ aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L2, player.playerId).size()
				+ aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L3, player.playerId).size();

		if (missingSwordsmenCount > 0) {
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SWORD, missingSwordsmenCount);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SPEAR, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.BOW, 0);
			setRatioOfMaterial(player.playerId, EMaterialType.SWORD, 0F);
			setRatioOfMaterial(player.playerId, EMaterialType.SPEAR, 1F);
			setRatioOfMaterial(player.playerId, EMaterialType.BOW, 0F);
		} else if (missingSpearmenCount > 0) {
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SWORD, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SPEAR, missingSpearmenCount);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.BOW, 0);
			setRatioOfMaterial(player.playerId, EMaterialType.SWORD, 0F);
			setRatioOfMaterial(player.playerId, EMaterialType.SPEAR, 0.3F);
			setRatioOfMaterial(player.playerId, EMaterialType.BOW, 1F);
		} else if (bowmenCount * player.getCombatStrengthInformation().getCombatStrength(false) < BOWMEN_COUNT_OF_KILLING_INFANTRY){
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SWORD, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SPEAR, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.BOW, 0);
			setRatioOfMaterial(player.playerId, EMaterialType.SWORD, 0F);
			setRatioOfMaterial(player.playerId, EMaterialType.SPEAR, 0.3F);
			setRatioOfMaterial(player.playerId, EMaterialType.BOW, 1F);
		} else {
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SWORD, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.SPEAR, 0);
			setNumberOfFutureProducedMaterial(player.playerId, EMaterialType.BOW, 0);
			setRatioOfMaterial(player.playerId, EMaterialType.SWORD, 0F);
			setRatioOfMaterial(player.playerId, EMaterialType.SPEAR, 0F);
			setRatioOfMaterial(player.playerId, EMaterialType.BOW, 1F);
		}
	}

	private void setNumberOfFutureProducedMaterial(byte playerId, EMaterialType materialType, int numberToProduce) {
		if (aiStatistics.getMaterialProduction(playerId).numberOfFutureProducedMaterial(materialType) != numberToProduce) {
			taskScheduler.scheduleTask(new SetMaterialProductionGuiTask(playerId, aiStatistics.getPositionOfPartition(playerId), materialType,
					EMaterialProductionType.SET_PRODUCTION, numberToProduce));
		}
	}

	private void setRatioOfMaterial(byte playerId, EMaterialType materialType, float ratio) {
		if (aiStatistics.getMaterialProduction(playerId).configuredRatioOfMaterial(materialType) != ratio) {
			taskScheduler.scheduleTask(new SetMaterialProductionGuiTask(playerId, aiStatistics.getPositionOfPartition(playerId), materialType,
					EMaterialProductionType.SET_RATIO, ratio));
		}
	}

	private boolean upgradeSoldiers(ESoldierType type) {
		if (player.getManaInformation().isUpgradePossible(type)) {
			taskScheduler.scheduleTask(new UpgradeSoldiersGuiTask(player.playerId, type));
			return true;
		}
		return false;
	}

	private void defend(Situation situation) {
		List<ShortPoint2D> allMyTroops = new Vector<ShortPoint2D>();
		allMyTroops.addAll(situation.bowmenPositions);
		allMyTroops.addAll(situation.pikemenPositions);
		allMyTroops.addAll(situation.swordsmenPositions);
		sendTroopsTo(allMyTroops, aiStatistics.getEnemiesInTownOf(player.playerId).iterator().next());
	}

	private void attack(Situation situation, boolean infantryWouldDie) {
		byte enemyId = getWeakestEnemy();
		ShortPoint2D targetDoor = getTargetEnemyDoorToAttack(enemyId);
		if (infantryWouldDie) {
			sendTroopsTo(situation.bowmenPositions, targetDoor);
		} else {
			List<ShortPoint2D> soldiers = new ArrayList<>(situation.bowmenPositions.size() + situation.pikemenPositions.size() + situation
					.swordsmenPositions.size());
			soldiers.addAll(situation.bowmenPositions);
			soldiers.addAll(situation.pikemenPositions);
			soldiers.addAll(situation.swordsmenPositions);
			sendTroopsTo(soldiers, targetDoor);
		}
	}

	private byte getWeakestEnemy() {
		byte weakestEnemyId = 0;
		int minAmountOfEnemyId = Integer.MAX_VALUE;
		for (byte enemyId : aiStatistics.getEnemiesOf(player.playerId)) {
			if (aiStatistics.isAlive(enemyId)) {
				int amountOfEnemyTroops = aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L1, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L2, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L3, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L1, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L2, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L3, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L1, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L2, enemyId).size();
				amountOfEnemyTroops += aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L3, enemyId).size();
				if (amountOfEnemyTroops < minAmountOfEnemyId) {
					minAmountOfEnemyId = amountOfEnemyTroops;
					weakestEnemyId = enemyId;
				}
			}
		}
		return weakestEnemyId;
	}

	private void sendTroopsTo(List<ShortPoint2D> attackerPositions, ShortPoint2D target) {
		List<Integer> attackerIds = new Vector<Integer>();
		for (ShortPoint2D attackerPosition : attackerPositions) {
			attackerIds.add(movableGrid.getMovableAt(attackerPosition.x, attackerPosition.y).getID());
		}

		taskScheduler.scheduleTask(new MoveToGuiTask(player.playerId, target, attackerIds));
	}

	private ShortPoint2D getTargetEnemyDoorToAttack(byte enemyToAttackId) {
		List<ShortPoint2D> myMilitaryBuildings = aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.getMilitaryBuildings(),
				player.playerId);
		ShortPoint2D myBaseAveragePoint = aiStatistics.calculateAveragePointFromList(myMilitaryBuildings);

		List<ShortPoint2D> enemyMilitaryBuildings = aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.getMilitaryBuildings(),
				enemyToAttackId);

		return aiStatistics.getBuildingAt(AiStatistics.detectNearestPointFromList(myBaseAveragePoint, enemyMilitaryBuildings)).getDoor();
	}

	private Situation calculateSituation(byte playerId) {
		Situation situation = new Situation();
		situation.swordsmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L1, playerId));
		situation.swordsmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L2, playerId));
		situation.swordsmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.SWORDSMAN_L3, playerId));
		situation.bowmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L1, playerId));
		situation.bowmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L2, playerId));
		situation.bowmenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.BOWMAN_L3, playerId));
		situation.pikemenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L1, playerId));
		situation.pikemenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L2, playerId));
		situation.pikemenPositions.addAll(aiStatistics.getMovablePositionsByTypeForPlayer(EMovableType.PIKEMAN_L3, playerId));

		return situation;
	}

	private static class Situation {
		private final List<ShortPoint2D> swordsmenPositions = new Vector<ShortPoint2D>();
		private final List<ShortPoint2D> bowmenPositions = new Vector<ShortPoint2D>();
		private final List<ShortPoint2D> pikemenPositions = new Vector<ShortPoint2D>();
		public int soldiersCount() {
			return swordsmenPositions.size() + bowmenPositions.size() + pikemenPositions.size();
		}
	}

	@Override
	public String toString() {
		return this.getClass().getName();
	}

}
