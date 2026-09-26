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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.WndReward;
import com.watabou.noosa.Game;
import com.watabou.utils.Callback;

public class ModHooks {

	public static void onHeroInit( Hero hero ){
		if (Dungeon.daily){
			return;
		}
		hero.belongings.backpack.bonusCapacity = MetaProgress.Imprint.BAG.level();
		Dungeon.gold += MetaProgress.Imprint.GOLD.level()*100;
		for (int i = 0; i < MetaProgress.Imprint.RATIONS.level(); i++){
			new SmallRation().collect();
		}
		for (int i = 0; i < MetaProgress.Imprint.IDENTIFY.level(); i++){
			new ScrollOfIdentify().collect();
		}
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
		if (tier >= 1 && tier <= MetaProgress.Imprint.BORN_TALENT.level()){
			return 1;
		}
		return 0;
	}

	public static int modifyGoldPickup( int quantity ){
		return Math.round( quantity * (1 + 0.1f*MetaProgress.Imprint.FORTUNE.level()) );
	}

	public static void onBossKilled( Mob boss ){
		if (Dungeon.daily){
			return;
		}
		int lootLvl = MetaProgress.Imprint.BOSS_LOOT.level();
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

}
