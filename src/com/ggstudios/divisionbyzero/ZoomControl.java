package com.ggstudios.divisionbyzero;

import com.ggstudios.divisionbyzero.Button.OnClickListener;

public class ZoomControl extends Drawable implements Clickable {

	private static final float ZOOM_INCREMENT = 0.25f;

	private Button btnZoomIn, btnZoomOut;

	public ZoomControl() {}

	static class ZoomAnimation implements Updatable{

		private static final float DURATION = 0.3f;
		private float delta;
		private float time;
		private float base;

		private boolean finished = true;

		public void setDeltaZoom(float delta) {
			if(finished) {
				this.delta = delta;
				base = Core.zoom;
				time = 0;
				finished = false;
			} else {
				this.delta += delta;
				base = Core.zoom;
				time = 0;
			}
		}

		@Override
		public boolean update(float dt) {
			boolean result;
			
			float oldZoom = Core.zoom;
			
			time += dt;
			if(time < DURATION) {
				float newZoom;
				float t =  time / (DURATION /2f);
				if (t < 1) {
					newZoom = delta/2*t*t + base;
				} else {
					t--;
					newZoom = -delta/2 * (t*(t-2) - 1) + base;
				}
				
				result = Core.game.setZoom(newZoom);
			} else {
				Core.game.setZoom(base + delta);
				result = false;
			}
			
			if(!result) {
				finished = true;
			} else {
				float a_x = (0.5f) * ((Core.canvasWidth  / Core.zoom) - (Core.canvasWidth  / oldZoom));
				float a_y = (0.5f) * ((Core.canvasHeight  / Core.zoom) - (Core.canvasHeight / oldZoom));
				Core.offX += a_x;
				Core.offY += a_y;
				
				Core.game.onScrolled();
			}
			
			return result;
		}

	}

	ZoomAnimation zoomAnimation = new ZoomAnimation();

	public void build() {
		btnZoomIn = new Button(Core.canvasWidth - Core.SDP * 1.5f, Core.SDP_H, Core.SDP, Core.SDP, R.drawable.zoom_control_in);
		btnZoomOut = new Button(Core.canvasWidth - Core.SDP * 1.5f, Core.SDP * 1.5f, Core.SDP, Core.SDP, R.drawable.zoom_control_out);

		btnZoomIn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				zoomAnimation.setDeltaZoom(ZOOM_INCREMENT);
				if(!Core.gu.hasUiUpdatable(zoomAnimation)) {
					Core.gu.addUiUpdatable(zoomAnimation);
				}
			}

		});

		btnZoomOut.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(Button sender) {
				zoomAnimation.setDeltaZoom(-ZOOM_INCREMENT);
				if(!Core.gu.hasUiUpdatable(zoomAnimation)) {
					Core.gu.addUiUpdatable(zoomAnimation);
				}
			}

		});
	}

	@Override
	public void draw(float offX, float offY) {
		btnZoomIn.draw(0, 0);
		btnZoomOut.draw(0, 0);
	}

	@Override
	public void refresh() {
		btnZoomIn.refresh();
		btnZoomOut.refresh();
	}

	@Override
	public boolean onTouchEvent(int action, int x, int y) {
		return btnZoomIn.onTouchEvent(action, x, y) || btnZoomOut.onTouchEvent(action, x, y);	
	}

}
