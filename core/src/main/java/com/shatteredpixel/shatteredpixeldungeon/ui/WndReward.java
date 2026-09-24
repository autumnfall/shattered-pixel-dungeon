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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WndReward extends Window {

	public enum LootType {
		GOO, TENGU, DM300, KING
	}

	private static final int WIDTH		= 160;
	private static final int GAP		= 3;
	private static final int BTN_HEIGHT	= 18;

	public WndReward( LootType type ){
		super();

		IconTitle title = new IconTitle( Icons.get(Icons.SKULL), Messages.get(this, "title") );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		RenderedTextBlock prompt = PixelScene.renderTextBlock( 6 );
		prompt.text( Messages.get(this, "prompt"), WIDTH );
		prompt.setPos( 0, title.bottom() + GAP );
		add( prompt );

		float pos = prompt.bottom() + GAP + 1;
		for (Item reward : generateRewards( type )){
			addRewardButton( reward, pos );
			pos += BTN_HEIGHT + GAP;
		}

		resize( WIDTH, (int)(pos - GAP) );
	}

	private void addRewardButton( final Item reward, float pos ){
		RedButton btn = new RedButton( Messages.titleCase( reward.title() ) ){
			@Override
			protected void onClick() {
				super.onClick();
				if (!reward.collect()){
					Dungeon.level.drop( reward, Dungeon.hero.pos ).sprite.drop();
				}
				hide();
			}
		};
		btn.icon( new ItemSprite( reward ) );
		btn.setRect( 0, pos, WIDTH, BTN_HEIGHT );
		add( btn );
	}

	private ArrayList<Item> generateRewards( LootType type ){
		ArrayList<Item> rewards = new ArrayList<>();
		switch (type){
			case GOO: default:
				rewards.add( prep( Generator.random( Generator.Category.RING ), 0 ) );
				rewards.add( prep( Generator.random( Generator.Category.WAND ), 0 ) );
				rewards.add( prep( Generator.random( Generator.Category.ARTIFACT ), 0 ) );
				break;
			case TENGU:
				rewards.add( prep( Generator.random( Generator.Category.RING ), Random.Int( 2 ) ) );
				rewards.add( prep( Generator.random( Generator.Category.WAND ), Random.Int( 2 ) ) );
				rewards.add( prep( Generator.random( Generator.Category.ARTIFACT ), Random.Int( 2 ) ) );
				break;
			case DM300:
				rewards.add( prep( Generator.random( Generator.wepTiers[Random.IntRange( 1, 3 )] ), 1 + Random.Int( 2 ) ) );
				rewards.add( prep( randomArmorInTier( 2, 4 ), 1 + Random.Int( 2 ) ) );
				rewards.add( prep( Generator.random( Generator.misTiers[Random.IntRange( 1, 3 )] ), 1 + Random.Int( 2 ) ) );
				break;
			case KING:
				Weapon wep = (Weapon) prep( Generator.random( Generator.wepTiers[Random.IntRange( 2, 4 )] ), 3 + Random.Int( 2 ) );
				wep.enchant( Weapon.Enchantment.random() );
				rewards.add( wep );
				Armor arm = (Armor) prep( randomArmorInTier( 3, 5 ), 3 + Random.Int( 2 ) );
				arm.inscribe( Armor.Glyph.random() );
				rewards.add( arm );
				rewards.add( prep( Generator.random( Generator.misTiers[Random.IntRange( 2, 4 )] ), 3 + Random.Int( 2 ) ) );
				break;
		}
		return rewards;
	}

	private Item prep( Item item, int level ){
		if (level > 0){
			item.upgrade( level );
		}
		item.cursed = false;
		item.identify();
		return item;
	}

	private Armor randomArmorInTier( int minTier, int maxTier ){
		Armor armor = Generator.randomArmor();
		int tries = 0;
		while ((armor.tier < minTier || armor.tier > maxTier) && tries < 50){
			armor = Generator.randomArmor();
			tries++;
		}
		return armor;
	}

}
