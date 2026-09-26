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

import com.watabou.utils.Bundle;
import com.watabou.utils.FileUtils;

import java.io.IOException;

public class MetaProgress {

	public static final String META_FILE	= "meta.dat";

	private static final String SOUL_SHARDS	= "soul_shards";

	private static int soulShards = 0;

	private static int[] imprintLevels = new int[Imprint.values().length];

	private static boolean loaded = false;

	public static final int DEBUG_STARTING_SHARDS = 500;

	public static final int BORN_TALENT_MAX_LEVEL = 4;

	public enum Imprint {
		RATIONS    ("imprint_rations",	1, new int[]{15}),
		IDENTIFY   ("imprint_identify",	1, new int[]{25}),
		GOLD       ("imprint_gold",	1, new int[]{10, 25, 45, 70}),
		BAG        ("imprint_bag",	2, new int[]{30, 70, 150, 350}),
		FORTUNE    ("imprint_fortune",	2, new int[]{50, 110, 200}),
		BORN_TALENT("born_talent",	3, new int[]{20, 50, 120, 300}),
		BOSS_LOOT  ("imprint_boss_loot",	3, new int[]{100, 200, 350, 450}),
		SEEDS      ("imprint_seeds",	1, new int[]{12, 25, 40}),
		SHOP       ("imprint_shop",	2, new int[]{45, 90, 150}),
		REROLL     ("imprint_reroll",	3, new int[]{90, 170, 260});

		private String key;
		private int tier;
		private int[] costs;

		Imprint( String key, int tier, int[] costs ){
			this.key = key;
			this.tier = tier;
			this.costs = costs;
		}

		public int tier(){
			return tier;
		}

		public int maxLevel(){
			return costs.length;
		}

		public int level(){
			loadGlobal();
			return imprintLevels[ordinal()];
		}

		public int upgradeCost(){
			int lvl = level();
			if (lvl < maxLevel()){
				return costs[lvl];
			}
			return -1;
		}

		public boolean upgrade(){
			loadGlobal();
			int lvl = imprintLevels[ordinal()];
			if (MetaProgress.isTierUnlocked(tier) && lvl < maxLevel() && soulShards >= costs[lvl]){
				soulShards -= costs[lvl];
				imprintLevels[ordinal()] = lvl + 1;
				saveGlobal(true);
				return true;
			}
			return false;
		}
	}

	public static void loadGlobal() {
		if (!loaded) {
			try {
				Bundle bundle = FileUtils.bundleFromFile( META_FILE );
				soulShards = bundle.getInt( SOUL_SHARDS );
				for (Imprint im : Imprint.values()){
					imprintLevels[im.ordinal()] = bundle.getInt( im.key );
				}
				if (!bundle.contains( Imprint.BOSS_LOOT.key )){
					imprintLevels[Imprint.BOSS_LOOT.ordinal()] =
							  bundle.getInt( "imprint_goo" )
							+ bundle.getInt( "imprint_tengu" )
							+ bundle.getInt( "imprint_dm300" )
							+ bundle.getInt( "imprint_king" );
					saveGlobal(true);
				}
			} catch (IOException e) {
				soulShards = DEBUG_STARTING_SHARDS;
				imprintLevels = new int[Imprint.values().length];
			}
			loaded = true;
		}
	}

	public static void saveGlobal(){
		saveGlobal(false);
	}

	public static void saveGlobal(boolean force) {
		if (force) {

			Bundle bundle = new Bundle();
			bundle.put( SOUL_SHARDS, soulShards );
			for (Imprint im : Imprint.values()){
				bundle.put( im.key, imprintLevels[im.ordinal()] );
			}

			try {
				FileUtils.bundleToFile(META_FILE, bundle);
			} catch (IOException e) {
				ShatteredPixelDungeon.reportException(e);
			}
		}
	}

	public static int soulShards(){
		loadGlobal();
		return soulShards;
	}

	public static void earnShards( int amount ){
		loadGlobal();
		if (amount > 0){
			soulShards += amount;
			saveGlobal(true);
		}
	}

	public static boolean spendShards( int amount ){
		loadGlobal();
		if (amount > 0 && soulShards >= amount){
			soulShards -= amount;
			saveGlobal(true);
			return true;
		}
		return false;
	}

	public static int bornTalent(){
		return Imprint.BORN_TALENT.level();
	}

	public static boolean upgradeBornTalent(){
		return Imprint.BORN_TALENT.upgrade();
	}

	public static int bornTalentUpgradeCost(){
		return Imprint.BORN_TALENT.upgradeCost();
	}

	public static int totalLevelsInTier( int tier ){
		loadGlobal();
		int total = 0;
		for (Imprint im : Imprint.values()){
			if (im.tier == tier){
				total += imprintLevels[im.ordinal()];
			}
		}
		return total;
	}

	public static int totalLevels(){
		loadGlobal();
		int total = 0;
		for (int lvl : imprintLevels){
			total += lvl;
		}
		return total;
	}

	public static boolean isTierUnlocked( int tier ){
		if (tier <= 1){
			return true;
		}
		return totalLevelsInTier(tier-1) >= 3;
	}

	public static int refundAll(){
		loadGlobal();
		int refund = 0;
		for (Imprint im : Imprint.values()){
			int lvl = imprintLevels[im.ordinal()];
			for (int i = 0; i < lvl; i++){
				refund += im.costs[i];
			}
			imprintLevels[im.ordinal()] = 0;
		}
		soulShards += refund;
		saveGlobal(true);
		return refund;
	}

}
