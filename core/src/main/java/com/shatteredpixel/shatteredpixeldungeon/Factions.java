/*
 * SoulShards Dungeon mod
 * Copyright (C) 2026
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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

//mod: per-run faction system. One faction is chosen at hero creation, contribution
//is earned during the run, and 5 effects unlock (the 5th also grows) as tiers are reached.
public class Factions {

	public enum Faction {
		NONE,
		WARBAND,
		CARAVAN,
		ORDER;

		public int[] thresholds(){
			switch (this){
				case WARBAND:	return new int[]{10, 25, 45, 70, 100};
				case CARAVAN:	return new int[]{500, 1500, 3000, 5000, 8000};
				case ORDER:		return new int[]{8, 14, 20, 26, 32};
				default:		return new int[]{};
			}
		}

		public String title(){
			return Messages.get( Factions.class, this.name().toLowerCase() );
		}

		public String desc(){
			return Messages.get( Factions.class, this.name().toLowerCase() + "_desc" );
		}
	}

	public static class FactionTracker extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public Faction faction = Faction.NONE;
		public float contribution = 0;

		//per-run counters for effects with usage caps
		public int bossIdentified = 0;		//ORDER: 战后总结, max 8 items
		public int revivesUsed = 0;			//CARAVAN: 商会底牌

		private static final String FACTION		= "faction";
		private static final String CONTRIBUTION	= "contribution";
		private static final String BOSS_IDENTS	= "boss_idents";
		private static final String REVIVES		= "revives";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( FACTION, faction.ordinal() );
			bundle.put( CONTRIBUTION, contribution );
			bundle.put( BOSS_IDENTS, bossIdentified );
			bundle.put( REVIVES, revivesUsed );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			faction = Faction.values()[bundle.getInt( FACTION )];
			contribution = bundle.getFloat( CONTRIBUTION );
			bossIdentified = bundle.getInt( BOSS_IDENTS );
			revivesUsed = bundle.getInt( REVIVES );
		}

		public int tier(){
			int tier = 0;
			for (int t : faction.thresholds()){
				if (contribution >= t) tier++;
			}
			return tier;
		}

		public void earn( float amount ){
			contribution += amount;
		}

		@Override
		public int icon() {
			switch (faction){
				case WARBAND:	return BuffIndicator.SACRIFICE;
				case CARAVAN:	return BuffIndicator.AMULET;
				case ORDER:		return BuffIndicator.LIGHT;
				default:		return BuffIndicator.NONE;
			}
		}

		@Override
		public void tintIcon( Image icon ) {
			switch (faction){
				case WARBAND:	icon.hardlight( 1f, 0.6f, 0.5f );	break;
				case CARAVAN:	icon.hardlight( 1f, 0.9f, 0.4f );	break;
				case ORDER:		icon.hardlight( 0.6f, 0.9f, 1f );	break;
			}
		}

		@Override
		public String name() {
			return faction.title();
		}

		@Override
		public String desc() {
			if (faction == Faction.NONE) return "";
			int[] thresholds = faction.thresholds();
			int tier = tier();
			StringBuilder sb = new StringBuilder();
			sb.append( Messages.get( Factions.class, "tracker_desc",
					faction.title(), (int)contribution, tier, thresholds.length ) );
			for (int i = 0; i < thresholds.length; i++){
				sb.append( "\n\n" );
				sb.append( tier > i
						? Messages.get( Factions.class, "effect_unlocked", i+1 )
						: Messages.get( Factions.class, "effect_locked", i+1, (int)thresholds[i] - (int)contribution ) );
				sb.append( " " ).append( Messages.get( Factions.class, faction.name().toLowerCase() + "_effect_" + (i+1) ) );
			}
			return sb.toString();
		}
	}

	public static FactionTracker tracker(){
		if (Dungeon.hero == null) return null;
		return Dungeon.hero.buff( FactionTracker.class );
	}

	public static Faction active(){
		FactionTracker t = tracker();
		return t == null ? Faction.NONE : t.faction;
	}

	public static int tier(){
		FactionTracker t = tracker();
		return t == null ? 0 : t.tier();
	}

	public static float contribution(){
		FactionTracker t = tracker();
		return t == null ? 0 : t.contribution;
	}

	public static void earn( float amount ){
		FactionTracker t = tracker();
		if (t != null && t.faction != Faction.NONE) t.earn( amount );
	}

	public static void init( Hero hero, Faction faction ){
		if (faction == Faction.NONE) return;
		FactionTracker t = Buff.affect( hero, FactionTracker.class );
		t.faction = faction;
	}
}
