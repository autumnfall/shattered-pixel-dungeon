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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.WndReward;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

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
		//mod: divine ward tracker attaches at run start so remaining uses are visible immediately
		if (MetaProgress.Imprint.DIVINE_WARD.runLevel() > 0){
			Buff.affect( hero, DivineWardTracker.class );
		}
		initFaction( hero );
	}

	//mod: faction chosen on the hero select screen is locked in for this run
	private static void initFaction( Hero hero ){
		if (Dungeon.daily) return;
		Factions.Faction faction = Factions.Faction.values()[SPDSettings.faction()];
		Factions.init( hero, faction );
		if (faction == Factions.Faction.ORDER){
			//starting knowledge counts toward 学识
			for (Item item : hero.belongings){
				if (item.isIdentified()
						&& (item instanceof Potion || item instanceof Scroll || item instanceof Ring
						|| item instanceof Weapon || item instanceof Armor)){
					Factions.earn( 1 );
				}
			}
		}
	}

	//mod: faction contribution + on-kill effects, called for every enemy death
	public static void onEnemyKilled( Mob mob, Object cause ){
		boolean heroKill = cause == Dungeon.hero || cause instanceof Weapon || cause instanceof Weapon.Enchantment;
		if (!heroKill) return;
		Factions.Faction faction = Factions.active();
		if (faction == Factions.Faction.WARBAND){
			boolean boss = mob.properties().contains( Char.Property.BOSS );
			Factions.earn( boss ? 10 : 1 );
			//战意: heal 1 HP per kill at tier 2+
			if (Factions.tier() >= 2 && Dungeon.hero.HP < Dungeon.hero.HT){
				Dungeon.hero.HP++;
			}
		} else if (faction == Factions.Faction.ORDER && mob.properties().contains( Char.Property.BOSS )){
			//战后总结: identify 2 unidentified items per boss, max 8
			Factions.FactionTracker t = Factions.tracker();
			if (Factions.tier() >= 1 && t.bossIdentified < 8){
				ArrayList<Item> candidates = new ArrayList<>();
				for (Item item : Dungeon.hero.belongings){
					if (!item.isIdentified() && (item instanceof Potion || item instanceof Scroll || item instanceof Ring)){
						candidates.add( item );
					}
				}
				Random.shuffle( candidates );
				for (int i = 0; i < 2 && i < candidates.size() && t.bossIdentified < 8; i++){
					candidates.get( i ).identify();
					t.bossIdentified++;
				}
			}
		}
	}

	//mod: 学识 contribution, called whenever the hero identifies an item
	public static void onItemIdentified( Item item ){
		if (Factions.active() != Factions.Faction.ORDER) return;
		if (item instanceof Potion || item instanceof Scroll || item instanceof Ring
				|| item instanceof Weapon || item instanceof Armor || item instanceof Wand){
			Factions.earn( 1 );
		}
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
		if (Factions.active() == Factions.Faction.CARAVAN){
			Factions.earn( quantity );
		}
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
	public static class DivineWardTracker extends CounterBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			//ENDURE is unused by vanilla buffs; blue tint distinguishes it further
			return BuffIndicator.ENDURE;
		}

		@Override
		public void tintIcon( Image icon ) {
			//blue tint, no vanilla collision for this icon
			icon.hardlight( 0.6f, 0.8f, 1f );
		}

		@Override
		public String name() {
			//class-based lookup would produce "modhooks$divinewardtracker" keys; use explicit ones
			return Messages.get( "modhooks.divinewardtracker.name" );
		}

		@Override
		public String desc() {
			return Messages.get( "modhooks.divinewardtracker.desc",
					MetaProgress.Imprint.DIVINE_WARD.runLevel() - (int)count() );
		}
	}

	public static boolean suppressWraiths( Hero hero ){
		if (!imprintsActive()) return false;
		int level = MetaProgress.Imprint.DIVINE_WARD.runLevel();
		if (level <= 0) return false;
		DivineWardTracker tracker = Buff.affect( hero, DivineWardTracker.class );
		if (tracker.count() < level){
			tracker.countUp( 1 );
			return true;
		}
		tracker.detach();
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

	private static boolean isLightOrEmpty( KindOfWeapon wep ){
		return wep == null || (wep instanceof Weapon && ((Weapon)wep).STRReq( 0 ) == 10);
	}

	//+3: +5% movement speed per equipment slot that is empty or holds base-strength-10 gear, max +25%
	public static float travelLightSpeedMult( Hero hero ){
		if (!imprintsActive() || MetaProgress.Imprint.TRAVEL_LIGHT.runLevel() < 3) return 1f;
		int lightGear = 0;
		if (isLightOrEmpty( hero.belongings.weapon() )) lightGear++;
		if (isLightOrEmpty( hero.belongings.secondWep() )) lightGear++;
		Armor armor = hero.belongings.armor();
		if (armor == null || armor.STRReq( 0 ) == 10) lightGear++;
		//artifact/misc/ring slots have no strength-10 gear; only empty slots count
		if (hero.belongings.artifact() == null) lightGear++;
		if (hero.belongings.misc() == null) lightGear++;
		if (hero.belongings.ring() == null) lightGear++;
		return 1f + 0.05f*Math.min( 5, lightGear );
	}


// === faction effect helpers ===

	//true if the damage source belongs to the hero: melee, thrown weapons, weapon
	//enchantments, and wands all report themselves as the damage source
	public static boolean isHeroDamageSource( Object src ){
		return src == Dungeon.hero
				|| src instanceof KindOfWeapon
				|| src instanceof Weapon.Enchantment
				|| src instanceof Wand;
	}

	//central damage hook for faction damage bonuses: 处决/屠龙术/商业帝国
	public static int modifyDealtDamage( Char target, Object src, int dmg ){
		if (!(target instanceof Mob) || Dungeon.hero == null) return dmg;
		if (!isHeroDamageSource( src )) return dmg;
		Factions.Faction faction = Factions.active();
		int tier = Factions.tier();
		if (faction == Factions.Faction.WARBAND){
			//屠龙术: +5% vs bosses, +1% per 10 战功 beyond 100, capped at +20%
			if (tier >= 5 && target.properties().contains( Char.Property.BOSS )){
				dmg = Math.round( dmg * (1f + Math.min( 0.20f, 0.05f + 0.01f * ((Factions.contribution() - 100) / 10f) )) );
			}
			//弱点打击: 20% chance to make the target vulnerable for 2 turns
			if (tier >= 3 && ((Mob) target).buff( Vulnerable.class ) == null && Random.Float() < 0.2f){
				Buff.affect( target, Vulnerable.class, 2f );
			}
		} else if (faction == Factions.Faction.CARAVAN && tier >= 5){
			//商业帝国: +1% damage per 400 gold held, cap grows with contribution
			float cap = Math.min( 0.25f, 0.10f + 0.03f * ((Factions.contribution() - 8000) / 2000f) );
			dmg = Math.round( dmg * (1f + Math.min( cap, 0.01f * (Dungeon.gold / 400) )) );
		}
		return dmg;
	}

	//狂怒: warband tier 4 emulates a +0 ring of furor (+9.05% attack speed)
	public static float warbandAttackSpeedMult( Char owner ){
		if (Dungeon.hero != null && owner == Dungeon.hero
				&& Factions.active() == Factions.Faction.WARBAND && Factions.tier() >= 4){
			return 1.09051f;
		}
		return 1f;
	}

	//商会底牌: revive by spending half of current gold, once per run
	public static boolean caravanRevive( Hero hero ){
		if (Factions.active() != Factions.Faction.CARAVAN || Factions.tier() < 3) return false;
		Factions.FactionTracker t = Factions.tracker();
		if (t.revivesUsed >= 1 || Dungeon.gold <= 0) return false;
		t.revivesUsed++;
		Dungeon.gold /= 2;
		hero.HP = hero.HT / 4;
		PotionOfHealing.cure( hero );
		Buff.prolong( hero, Invulnerability.class, Invulnerability.DURATION );
		GLog.w( Messages.get( ModHooks.class, "caravan_revive" ) );
		return true;
	}

	//幸运眷顾: +20% drop luck, equivalent to one level of wealth bonus
	public static float caravanLuckBonus(){
		if (Factions.active() == Factions.Faction.CARAVAN && Factions.tier() >= 4) return 0.2f;
		return 0f;
	}

	//砍价: shop buy price multiplier
	public static float caravanBuyPriceMult(){
		if (Factions.active() == Factions.Faction.CARAVAN && Factions.tier() >= 1) return 0.88f;
		return 1f;
	}

	//贵宾货架: extra rare equipment in every shop
	public static boolean caravanRareItem(){
		return Factions.active() == Factions.Faction.CARAVAN && Factions.tier() >= 2;
	}

	//悉心保管: scrolls immune to burning, potions immune to freezing
	public static boolean orderProtectsScrolls(){
		return Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 2;
	}

	public static boolean orderProtectsPotions(){
		return Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 2;
	}

	//精神食粮: hunger restored per scroll read
	//精神食粮: satiety restored when reading scrolls, 1/3 of a small ration
	public static float orderScrollFoodEnergy(){
		if (Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 3) return Hunger.HUNGRY / 6f;
		return 0f;
	}

	//奥术共鸣: charge speed of one energy ring level (x1.175)
	public static float orderChargeMult(){
		if (Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 4) return 1.175f;
		return 1f;
	}

	//明察秋毫: growth beyond the unlock threshold, each step adds +1 search
	//level and +1 vision turn, capped at +4 (search bonus is 1+growth levels)
	public static int orderSearchGrowth(){
		if (Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 5){
			return (int)Math.min( 4, (Factions.contribution() - 32) / 8 );
		}
		return 0;
	}

	//search range bonus levels (0-8), each odd level extends orthogonally first,
	//the following even level completes the ring diagonally (4 base levels = 2 rings)
	public static int orderSearchBonus(){
		if (Factions.active() == Factions.Faction.ORDER && Factions.tier() >= 5){
			return 4 + orderSearchGrowth();
		}
		return 0;
	}

	//武器掌握: warband tier 1, melee/thrown weapon max damage +1 per weapon tier
	public static int warbandWeaponMaxBonus( Weapon w ){
		if (Factions.active() != Factions.Faction.WARBAND || Factions.tier() < 1) return 0;
		if (w instanceof MeleeWeapon) return Math.min( 5, ((MeleeWeapon) w).tier );
		if (w instanceof MissileWeapon) return Math.min( 5, ((MissileWeapon) w).tier );
		return 0;
	}
}
