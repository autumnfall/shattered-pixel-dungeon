/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.WndReward;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

public class ModHooks {

	//mod: imprints never apply in daily runs; in regular challenge runs they apply
	//only when enabled via the toggle in the meta window
	public static boolean imprintsActive(){
		if (Dungeon.daily){
			return false;
		}
		return Dungeon.challenges == 0 || MetaProgress.challengesEnabled();
	}

	public static void onHeroInit( Hero hero ){
		if (!imprintsActive()){
			return;
		}
		//mod: freeze a per-run copy of imprint levels before applying start-of-run effects,
		//so meta edits made mid-run cannot alter this run
		MetaProgress.takeRunSnapshot();
		hero.belongings.backpack.bonusCapacity = MetaProgress.Imprint.BAG.level();
		Dungeon.gold += MetaProgress.Imprint.GOLD.level()*100;
		//mod: starting supplies, lv1 ration / lv2 identify scroll / lv3 darts
		if (MetaProgress.Imprint.SUPPLIES.level() >= 1){
			new SmallRation().collect();
		}
		if (MetaProgress.Imprint.SUPPLIES.level() >= 2){
			new ScrollOfIdentify().identify().collect();
		}
		if (MetaProgress.Imprint.SUPPLIES.level() >= 3){
			new Dart().quantity(2).collect();
		}
		for (int i = 0; i < MetaProgress.Imprint.SEEDS.level(); i++){
			Generator.random( Generator.Category.SEED ).collect();
		}
		Statistics.rerollCharges = MetaProgress.Imprint.REROLL.level();
	}

	//mod: fate rewrite imprint, consumes one reroll charge if available
	public static boolean rerollAvailable(){
		return Statistics.rerollCharges > 0;
	}

	public static boolean useReroll(){
		if (Statistics.rerollCharges > 0){
			Statistics.rerollCharges--;
			return true;
		}
		return false;
	}

	public static void onReallyDie(){
		final int earned = Settlement.calculate(false);
		MetaProgress.earnShards(earned);
		if (earned > 0){
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Dungeon.hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get(MetaProgress.class, "earned", earned) );
				}
			});
		}
	}

	public static void onVictory(){
		MetaProgress.earnShards( Settlement.calculate(true) );
	}

	public static int bonusTalentPoints( Hero hero, int tier ){
		//each imprint level grants +1 talent point to one more tier, awarded as tiers unlock,
		//mirroring the potion of divine inspiration
		if (!imprintsActive()){
			return 0;
		}
		if (tier >= 1 && tier <= MetaProgress.Imprint.BORN_TALENT.runLevel()){
			return 1;
		}
		return 0;
	}

	public static int modifyGoldPickup( int quantity ){
		if (!imprintsActive()){
			return quantity;
		}
		return Math.round( quantity * (1 + 0.1f*MetaProgress.Imprint.FORTUNE.runLevel()) );
	}

	public static void onBossKilled( Mob boss ){
		if (!imprintsActive()){
			return;
		}
		int lootLvl = MetaProgress.Imprint.BOSS_LOOT.runLevel();
		if (boss instanceof Goo && lootLvl >= 1){
			showReward( WndReward.LootType.GOO );
		} else if (boss instanceof Tengu && lootLvl >= 2){
			showReward( WndReward.LootType.TENGU );
		} else if (boss instanceof DM300 && lootLvl >= 3){
			showReward( WndReward.LootType.DM300 );
		} else if (boss instanceof DwarfKing && lootLvl >= 4){
			showReward( WndReward.LootType.KING );
		}
	}

	private static void showReward( final WndReward.LootType type ){
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndReward( type ) );
			}
		});
	}

	//mod: divine ward imprint, per-run counter of wraith-free tomb/remains openings
	public static class DivineWardTracker extends CounterBuff {}

	public static boolean suppressWraiths( Hero hero ){
		if (!imprintsActive()) return false;
		int level = MetaProgress.Imprint.DIVINE_WARD.runLevel();
		if (level <= 0) return false;
		DivineWardTracker tracker = Buff.affect( hero, DivineWardTracker.class );
		if (tracker.count() < level){
			tracker.countUp( 1 );
			return true;
		}
		return false;
	}

	//mod: self-reliance imprint, empty ring (and artifact, at lv1) slots act as +1 ring of wealth
	public static int selfRelianceBonus( Char target ){
		if (!imprintsActive() || !(target instanceof Hero)) return 0;
		int level = MetaProgress.Imprint.SELF_RELIANCE.runLevel();
		if (level <= 0) return 0;
		Hero hero = (Hero) target;
		if (hero.belongings.ring() != null) return 0;
		if (level < 2 && hero.belongings.artifact() != null) return 0;
		return 1;
	}

	//mod: travel light imprint, bonuses while equipping base-strength-10 (tier-1) gear
	public static boolean travelLightWeaponMinBonus( KindOfWeapon weapon ){
		if (!imprintsActive() || MetaProgress.Imprint.TRAVEL_LIGHT.runLevel() < 1) return false;
		return Dungeon.hero != null && Dungeon.hero.belongings.weapon() == weapon
				&& weapon instanceof Weapon && ((Weapon)weapon).STRReq( 0 ) == 10;
	}

	public static int travelLightWeaponMin( int min, KindOfWeapon weapon ){
		if (travelLightWeaponMinBonus( weapon )) return min + 1;
		return min;
	}

	public static int travelLightArmorMinDr( int minDr ){
		if (!imprintsActive() || MetaProgress.Imprint.TRAVEL_LIGHT.runLevel() < 2) return minDr;
		if (Dungeon.hero != null && Dungeon.hero.belongings.armor() != null
				&& Dungeon.hero.belongings.armor().STRReq( 0 ) == 10){
			return minDr + 1;
		}
		return minDr;
	}

	public static float travelLightSpeedMult( Hero hero ){
		if (!imprintsActive() || MetaProgress.Imprint.TRAVEL_LIGHT.runLevel() < 3) return 1f;
		int lightGear = 0;
		if (hero.belongings.weapon() instanceof Weapon && ((Weapon)hero.belongings.weapon()).STRReq( 0 ) == 10) lightGear++;
		if (hero.belongings.armor() != null && hero.belongings.armor().STRReq( 0 ) == 10) lightGear++;
		return 1f + 0.05f*lightGear;
	}

}
