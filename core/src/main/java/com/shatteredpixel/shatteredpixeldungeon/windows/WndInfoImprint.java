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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.MetaProgress;
import com.shatteredpixel.shatteredpixeldungeon.MetaProgress.Imprint;
import com.shatteredpixel.shatteredpixeldungeon.items.food.SmallRation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.WndMeta;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.utils.Callback;

import java.util.Locale;

public class WndInfoImprint extends Window {

	private static final int WIDTH	= 120;
	private static final float GAP	= 2;

	private Imprint imprint;
	private Callback onUpgrade;

	private IconTitle titlebar;
	private RenderedTextBlock info;
	private RedButton btnUpgrade;

	public WndInfoImprint( Imprint imprint, Callback onUpgrade ){
		super();
		this.imprint = imprint;
		this.onUpgrade = onUpgrade;

		titlebar = new IconTitle();
		titlebar.icon( iconFor( imprint ) );
		add( titlebar );

		info = PixelScene.renderTextBlock( 6 );
		info.maxWidth( WIDTH );
		add( info );

		btnUpgrade = new RedButton( "" ){
			@Override
			protected void onClick() {
				super.onClick();
				if (imprint.upgrade()){
					if (onUpgrade != null){
						onUpgrade.call();
					}
					refresh();
				}
			}
		};
		btnUpgrade.icon( Icons.get( Icons.TALENT ) );
		add( btnUpgrade );

		refresh();
	}

	private void refresh(){
		String key = imprint.name().toLowerCase( Locale.ENGLISH );

		titlebar.label( Messages.titleCase(Messages.get(WndMeta.class, key))
				+ "  _" + imprint.level() + "/" + imprint.maxLevel() + "_", TITLE_COLOR );
		titlebar.setRect( 0, 0, WIDTH, 0 );

		String text = Messages.get(WndMeta.class, key + "_desc", imprint.level());

		int cost = imprint.upgradeCost();
		if (!MetaProgress.isTierUnlocked(imprint.tier())){
			text += "\n\n" + Messages.get(this, "locked");
			btnUpgrade.text( Messages.get(this, "locked") );
			btnUpgrade.enable( false );
		} else if (cost >= 0){
			Object nextArg = imprint.level() + 1;
			if (imprint == Imprint.BOSS_LOOT){
				nextArg = Messages.get(WndMeta.class, "boss_" + nextArg);
			}
			text += "\n\n" + Messages.get(WndMeta.class, key + "_next", nextArg)
					+ "\n" + Messages.get(this, "cost", cost);
			btnUpgrade.text( Messages.get(this, "upgrade", cost) );
			btnUpgrade.enable( MetaProgress.soulShards() >= cost );
		} else {
			text += "\n\n" + Messages.get(this, "maxed");
			btnUpgrade.text( Messages.get(this, "maxed") );
			btnUpgrade.enable( false );
		}
		info.text( text );
		info.setPos( 0, titlebar.bottom() + 2*GAP );

		btnUpgrade.setRect( 0, info.bottom() + 2*GAP, WIDTH, 18 );

		resize( WIDTH, (int) btnUpgrade.bottom() );
	}

	public static Image iconFor( Imprint im ){
		switch (im){
			case RATIONS:		return new ItemSprite( new SmallRation() );
			case IDENTIFY:		return new ItemSprite( new ScrollOfIdentify() );
			case GOLD:		return Icons.get( Icons.GOLD );
			case BAG:		return Icons.get( Icons.BACKPACK );
			case FORTUNE:		return Icons.get( Icons.COIN_SML );
			case BORN_TALENT:	return Icons.get( Icons.TALENT );
			case BOSS_LOOT: default:	return Icons.get( Icons.SKULL );
		}
	}

}
