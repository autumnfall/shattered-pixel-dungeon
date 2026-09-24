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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.MetaProgress;
import com.shatteredpixel.shatteredpixeldungeon.MetaProgress.Imprint;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndInfoImprint;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.utils.Callback;

import java.util.ArrayList;
import java.util.Locale;

public class WndMeta extends Window {

	private IconTitle title;
	private RenderedTextBlock shards;
	private RenderedTextBlock note;

	private final RenderedTextBlock[] tierTitles	= new RenderedTextBlock[3];
	private final ColorBlock[] seps			= new ColorBlock[3];
	private final ArrayList<ImprintButton> buttons	= new ArrayList<>();

	private StyledButton btnReset;

	private int shownShards = -1;
	private int shownTotal = -1;

	public WndMeta(){
		super();

		width = Math.min( 200, (int)(PixelScene.uiCamera.width - 8) );

		title = new IconTitle( Icons.get(Icons.TALENT), Messages.get(this, "title") );
		title.setRect( 0, 0, width, 0 );
		add( title );

		shards = PixelScene.renderTextBlock( 6 );
		shards.hardlight( 0xCCCCCC );
		add( shards );

		note = PixelScene.renderTextBlock( Messages.get(this, "note"), 5 );
		note.hardlight( 0x888888 );
		add( note );

		for (int i = 0; i < 3; i++){
			tierTitles[i] = PixelScene.renderTextBlock( 6 );
			tierTitles[i].hardlight( TITLE_COLOR );
			add( tierTitles[i] );

			seps[i] = new ColorBlock( 0, 1, 0xFF000000 );
			add( seps[i] );
		}

		for (Imprint im : Imprint.values()){
			ImprintButton btn = new ImprintButton( im );
			buttons.add( btn );
			add( btn );
		}

		btnReset = new StyledButton( Chrome.Type.GREY_BUTTON_TR, Messages.get(this, "reset") ){
			@Override
			protected void onClick() {
				super.onClick();
				ShatteredPixelDungeon.scene().add( new WndOptions(
						Messages.get(WndMeta.class, "reset_title"),
						Messages.get(WndMeta.class, "reset_message"),
						Messages.get(WndMeta.class, "reset_yes"),
						Messages.get(WndMeta.class, "reset_no") ){
					@Override
					protected void onSelect( int index ) {
						if (index == 0){
							MetaProgress.refundAll();
							refresh();
						}
					}
				} );
			}
		};
		btnReset.setSize( width, 16 );
		add( btnReset );

		refresh();
	}

	private void refresh(){
		shownShards = MetaProgress.soulShards();
		shownTotal = MetaProgress.totalLevels();

		shards.text( Messages.get(this, "shards", shownShards) );

		for (int i = 0; i < 3; i++){
			if (MetaProgress.isTierUnlocked(i + 1)){
				tierTitles[i].text( Messages.get(this, "tier", i + 1) );
			} else {
				tierTitles[i].text( Messages.get(this, "tier", i + 1)
						+ "  " + Messages.get(this, "locked", 3 - MetaProgress.totalLevelsInTier(i)) );
			}
		}

		for (ImprintButton btn : buttons){
			btn.refresh();
		}

		layoutNodes();
		resize( width, (int) btnReset.bottom() + 2 );
	}

	private void layoutNodes(){
		float pos = title.bottom() + 2;

		shards.setPos( 0, pos );
		note.setPos( 0, shards.bottom() + 1 );
		pos = note.bottom() + 3;

		for (int tier = 1; tier <= 3; tier++){
			tierTitles[tier-1].maxWidth( width );
			tierTitles[tier-1].setPos( (width - tierTitles[tier-1].width())/2f, pos );
			pos = tierTitles[tier-1].bottom() + 2;

			ArrayList<ImprintButton> tierButtons = new ArrayList<>();
			for (ImprintButton btn : buttons){
				if (btn.imprint.tier() == tier){
					tierButtons.add( btn );
				}
			}

			float left = 0;
			float rowTop = pos;
			float rowLabelBottom = pos;
			for (ImprintButton btn : tierButtons){
				if (left > 0 && left + ImprintButton.WIDTH > width){
					left = 0;
					rowTop = rowLabelBottom + 1;
				}
				btn.setPos( left, rowTop );
				PixelScene.align( btn );
				rowLabelBottom = Math.max( rowLabelBottom, btn.labelBottom() );
				left += ImprintButton.WIDTH + 6;
			}
			pos = rowLabelBottom;

			seps[tier-1].size( width, 1 );
			seps[tier-1].x = 0;
			seps[tier-1].y = pos + 1;
			pos += 4;
		}

		btnReset.setRect( 0, pos, width, 16 );
	}

	@Override
	public void update() {
		super.update();

		if (shownShards != MetaProgress.soulShards() || shownTotal != MetaProgress.totalLevels()){
			refresh();
		}
	}

	private class ImprintButton extends Button {

		static final int WIDTH	= 20;
		static final int HEIGHT	= 26;

		Imprint imprint;

		Image bg;
		Image icon;
		ColorBlock fill;
		RenderedTextBlock label;

		ImprintButton( Imprint im ){
			super();
			hotArea.blockLevel = PointerArea.NEVER_BLOCK;

			imprint = im;
			width = WIDTH;
			height = HEIGHT;

			bg = new Image( Assets.Interfaces.TALENT_BUTTON );
			bg.frame( 20*(im.maxLevel()-1), 0, WIDTH, HEIGHT );
			add( bg );

			icon = WndInfoImprint.iconFor( im );
			add( icon );

			fill = new ColorBlock( 0, 4, 0xFFFFFF44 );
			add( fill );

			label = PixelScene.renderTextBlock( 5 );
			add( label );
		}

		void refresh(){
			String key = imprint.name().toLowerCase( Locale.ENGLISH );
			label.text( Messages.titleCase(Messages.get(WndMeta.this, key)) );
			label.maxWidth( 26 );

			enable( MetaProgress.isTierUnlocked(imprint.tier()) );

			layout();
		}

		float labelBottom(){
			return y + HEIGHT + 1 + label.height();
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;

			icon.x = x + (WIDTH - icon.width())/2f;
			icon.y = y + 2;
			PixelScene.align( icon );

			fill.x = x + 2;
			fill.y = y + WIDTH - 1;
			fill.size( imprint.level()/(float)imprint.maxLevel() * (WIDTH-4), 5 );

			label.setPos( x + WIDTH/2f - label.width()/2f, y + HEIGHT + 1 );
			PixelScene.align( label );
		}

		@Override
		protected void onClick() {
			super.onClick();
			ShatteredPixelDungeon.scene().addToFront( new WndInfoImprint( imprint, new Callback() {
				@Override
				public void call() {
					WndMeta.this.refresh();
				}
			} ) );
		}

		@Override
		protected String hoverText() {
			return Messages.titleCase(Messages.get(WndMeta.this, imprint.name().toLowerCase( Locale.ENGLISH )));
		}

		void enable( boolean value ){
			active = value;
			icon.alpha( value ? 1.0f : 0.3f );
			bg.alpha( value ? 1.0f : 0.3f );
		}
	}
}
