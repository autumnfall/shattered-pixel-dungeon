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
import com.shatteredpixel.shatteredpixeldungeon.ModHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoItem;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndSadGhost;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class WndReward extends Window {

	public enum LootType {
		GOO, TENGU, DM300, KING
	}

	private static final int WIDTH		= 120;
	private static final int BTN_SIZE	= 32;
	private static final int BTN_GAP	= 5;
	private static final int GAP		= 2;

	private LootType type;

	public WndReward( LootType type ){
		super();

		this.type = type;

		IconTitle title = new IconTitle( Icons.get(Icons.SKULL), Messages.get(this, "title") );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		//mod: fate rewrite imprint, one reroll per charge
		if (ModHooks.rerollAvailable()){
			IconButton btnReroll = new IconButton( Icons.get(Icons.SHUFFLE) ){
				@Override
				protected void onClick() {
					super.onClick();
					if (ModHooks.useReroll()){
						WndReward.this.hide();
						GameScene.show( new WndReward( type ) );
					}
				}
			};
			btnReroll.setRect( WIDTH - 16, 0, 16, 16 );
			add( btnReroll );
		}

		RenderedTextBlock prompt = PixelScene.renderTextBlock( 6 );
		prompt.text( Messages.get(this, "prompt"), WIDTH );
		prompt.setPos( 0, title.bottom() + GAP );
		add( prompt );

		ArrayList<Item> rewards = generateRewards( type );

		float left = (WIDTH - (BTN_SIZE*rewards.size() + BTN_GAP*(rewards.size()-1)))/2f;
		float top = prompt.bottom() + BTN_GAP + 1;
		for (final Item reward : rewards){
			ItemButton btn = new ItemButton(){
				@Override
				protected void onClick() {
					super.onClick();
					if (item() != null){
						GameScene.show( new RewardWindow( item() ) );
					}
				}
			};
			btn.item( reward );
			btn.setRect( left, top, BTN_SIZE, BTN_SIZE );
			add( btn );
			left += BTN_SIZE + BTN_GAP;
		}

		resize( WIDTH, (int)(top + BTN_SIZE + BTN_GAP) );
	}

	@Override
	public void onBackPressed() {
		//this window holds a one-time reward, it must not be dismissable by accident
	}

	private void selectReward( Item reward ){
		hide();

		if (reward == null){
			return;
		}

		if (reward.doPickUp( Dungeon.hero )) {
			GLog.i( Messages.capitalize(Messages.get(Dungeon.hero, "you_now_have", reward.name())) );
		} else {
			Dungeon.level.drop( reward, Dungeon.hero.pos ).sprite.drop();
		}
	}

	private class RewardWindow extends WndInfoItem {

		public RewardWindow( Item item ) {
			super(item);

			RedButton btnConfirm = new RedButton(Messages.get(WndSadGhost.class, "confirm")){
				@Override
				protected void onClick() {
					super.onClick();
					RewardWindow.this.hide();

					selectReward( item );
				}
			};
			btnConfirm.setRect(0, height+2, width/2-1, 16);
			add(btnConfirm);

			RedButton btnCancel = new RedButton(Messages.get(WndSadGhost.class, "cancel")){
				@Override
				protected void onClick() {
					super.onClick();
					hide();
				}
			};
			btnCancel.setRect(btnConfirm.right()+2, height+2, btnConfirm.width(), 16);
			add(btnCancel);

			resize(width, (int)btnCancel.bottom());
		}
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
				rewards.add( prep( Generator.random( Generator.wepTiers[2] ), 1 ) );
				rewards.add( prep( randomArmorInTier( 3, 3 ), 1 ) );
				rewards.add( prep( Generator.random( Generator.misTiers[2] ), 1 ) );
				break;
			case KING:
				Weapon wep = (Weapon) prep( Generator.random( Generator.wepTiers[3] ), 1 + Random.Int( 2 ) );
				wep.enchant( Weapon.Enchantment.random() );
				rewards.add( wep );
				Armor arm = (Armor) prep( randomArmorInTier( 4, 4 ), 1 + Random.Int( 2 ) );
				arm.inscribe( Armor.Glyph.random() );
				rewards.add( arm );
				rewards.add( prep( Generator.random( Generator.misTiers[3] ), 1 + Random.Int( 2 ) ) );
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
