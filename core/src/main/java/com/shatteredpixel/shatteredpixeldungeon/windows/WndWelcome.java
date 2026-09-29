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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

//mod: one-time welcome popup on fresh installs, purely informational;
//new installs already start with a soul shard reserve baked into meta.dat
public class WndWelcome extends Window {

	private static final int WIDTH_MAX = 140;

	public WndWelcome(){
		super();

		int width = Math.min( WIDTH_MAX, (int)PixelScene.uiCamera.width - 8 );

		IconTitle title = new IconTitle( Icons.get( Icons.TALENT ), Messages.get( this, "title" ) );
		title.setRect( 0, 0, width, 0 );
		add( title );

		RenderedTextBlock info = PixelScene.renderTextBlock( Messages.get( this, "desc" ), 6 );
		info.maxWidth( width );
		info.setPos( 0, title.bottom() + 2 );
		add( info );

		RedButton btnOkay = new RedButton( Messages.get( this, "okay" ) ){
			@Override
			protected void onClick() {
				super.onClick();
				MetaProgress.markWelcomed();
				hide();
			}
		};
		btnOkay.setRect( 0, info.bottom() + 4, width, 18 );
		add( btnOkay );

		resize( width, (int) btnOkay.bottom() );
	}

}
