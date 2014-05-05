package com.ggstudios.widget;

import com.ggstudios.divisionbyzero.LevelMap.LevelNode;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;

public class LevelMapBackground extends View {
	//private static final String TAG = "LevelMapBackground";

	private static final int MAX_LINES = 50;
	private static final int INTS_PER_LINE = 4;

	private float[] pts = new float[MAX_LINES * INTS_PER_LINE];
	private Paint paint = new Paint();

	private SparseArray<LevelNode> map;

	public LevelMapBackground(Context context) {
		this(context, null);
	}

	public LevelMapBackground(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public LevelMapBackground(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);
	}

	public void setData(SparseArray<LevelNode> arr) {
		map = arr;
		layout();
	}

	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
	}
	
	private void layout() {
		int idx = 0;

		final int len = map.size();
		for(int i = 0; i < len; i++) {
			LevelNode node = map.valueAt(i);

			if(node.childCount != 0) {
				ImageView v = (ImageView) node.view;
				final int x0 = v.getLeft() + (v.getWidth() / 2);
				final int y0 = v.getTop() + (v.getHeight() / 2);

				for(int j = 0; j < node.childCount; j++) {
					ImageView v2 = (ImageView) node.children.get(j).view;

					final int x1 = v2.getLeft() + (v2.getWidth() / 2);
					final int y1 = v2.getTop() + (v2.getHeight() / 2);

					pts[idx++] = x0;
					pts[idx++] = y0;
					pts[idx++] = x1;
					pts[idx++] = y1;
				}
			}
		}
	}

	@Override
	protected void onDraw(Canvas c) {
		super.onDraw(c);

		c.drawLines(pts, paint);
	}
}
