/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;

public class TiUIScrollView extends TiUIView {

	// TODO: right now Android only has a ScrollView (vertical) or HorizontalScrollView
	// we prefer the vertical scroll view by default, but there is no easy way to combine them
	public static final int TYPE_VERTICAL = 0;
	public static final int TYPE_HORIZONTAL = 1;

	private static final String SHOW_VERTICAL_SCROLL_INDICATOR = "showVerticalScrollIndicator";
	private static final String SHOW_HORIZONTAL_SCROLL_INDICATOR = "showHorizontalScrollIndicator";
	private static final String LCAT = "TiUIScrollView";

	private class TiScrollViewLayout extends TiCompositeLayout
	{
		private static final int AUTO = Integer.MAX_VALUE;
		protected int measuredWidth = 0, measuredHeight = 0;

		public TiScrollViewLayout(Context context, LayoutArrangement arrangement) {
			super(context, arrangement);
		}

		private LayoutParams getParams(View child) {
			return (LayoutParams)child.getLayoutParams();
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			super.onLayout(changed, l, t, r, b);
			measuredHeight = measuredWidth = 0;
		}
		
		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
		{
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(Math.max(measuredWidth, getMeasuredWidth()), Math.max(measuredHeight, getMeasuredHeight()));
		}

		private int getContentProperty(String property) {
			Object value = getProxy().getProperty(property);
			if (value != null) {
				if (value.equals(TiC.SIZE_AUTO)) {
					return AUTO;
				} else if (value instanceof Number) {
					return ((Number)value).intValue();
				}
			}
			return AUTO;
		}

		private int calculateAbsoluteRight(View child)
		{
			LayoutParams p = getParams(child);
			int contentWidth = getContentProperty("contentWidth");
			if (contentWidth == AUTO) {
				int childMeasuredWidth = child.getMeasuredWidth();
				if (!p.autoWidth) {
					childMeasuredWidth = p.optionWidth.getAsPixels(this);
				}
				if (p.optionLeft != null) {
					childMeasuredWidth += p.optionLeft.getAsPixels(this);
				}
				if (p.optionRight != null) {
					childMeasuredWidth += p.optionRight.getAsPixels(this);
				}

				measuredWidth = Math.max(childMeasuredWidth, measuredWidth);
			} else {
				measuredWidth = contentWidth;
			}
			
			return measuredWidth;
		}

		private int calculateAbsoluteBottom(View child)
		{
			LayoutParams p = (LayoutParams)child.getLayoutParams();
			int contentHeight = getContentProperty("contentHeight");
			
			if (contentHeight == AUTO) {
				int childMeasuredHeight = child.getMeasuredHeight();
				if (!p.autoHeight) {
					childMeasuredHeight = p.optionHeight.getAsPixels(this);
				}
				if (p.optionTop != null) {
					childMeasuredHeight += p.optionTop.getAsPixels(this);
				}
				if (p.optionBottom != null) {
					childMeasuredHeight += p.optionBottom.getAsPixels(this);
				}

				measuredHeight = Math.max(childMeasuredHeight, measuredHeight);
			} else {
				measuredHeight = contentHeight;
			}
			return measuredHeight;
		}

		@Override
		protected void constrainChild(View child, int width, int wMode,
				int height, int hMode) {

			super.constrainChild(child, width, wMode, height, hMode);
			
			// We need to support an automatically growing contentArea, so this code is
			// updates the measured dimensions as needed. absWidth, absHeight are
			// left in for debugging purposes. ATM
			
			int absWidth = calculateAbsoluteRight(child);
			int absHeight = calculateAbsoluteBottom(child);
		}


		@Override
		protected int getWidthMeasureSpec(View child) {
			int contentWidth = getContentProperty("contentWidth");
			if (contentWidth == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else return super.getWidthMeasureSpec(child);
		}

		@Override
		protected int getHeightMeasureSpec(View child) {
			int contentHeight = getContentProperty("contentHeight");
			if (contentHeight == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else return super.getHeightMeasureSpec(child);
		}

		@Override
		protected int getMeasuredWidth(int maxWidth, int widthSpec) {
			int contentWidth = getContentProperty("contentWidth");
			if (contentWidth == AUTO) {
				return maxWidth; //measuredWidth;
			} else return contentWidth;
		}

		@Override
		protected int getMeasuredHeight(int maxHeight, int heightSpec) {
			int contentHeight = getContentProperty("contentHeight");
			if (contentHeight == AUTO) {
				return maxHeight; //measuredHeight;
			}
			else return contentHeight;
		}
	}

	// same code, different super-classes
	private class TiVerticalScrollView extends ScrollView
	{
		private TiScrollViewLayout layout;

		public TiVerticalScrollView(Context context, LayoutArrangement arrangement)
		{
			super(context);
			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
			//setFillViewport(true);
			//setScrollContainer(true);

			layout = new TiScrollViewLayout(context, arrangement);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);
			layout.setLayoutParams(params);
			super.addView(layout, params);
		}

		@Override
		public void addView(View child,
				android.view.ViewGroup.LayoutParams params) {
			layout.addView(child, params);
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);

			KrollDict data = new KrollDict();
			data.put("x", l);
			data.put("y", t);
			getProxy().fireEvent("scroll", data);
		}
	}

	private class TiHorizontalScrollView extends HorizontalScrollView
	{
		private TiScrollViewLayout layout;

		public TiHorizontalScrollView(Context context, LayoutArrangement arrangement)
		{
			super(context);
			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
			setFillViewport(true);
			setScrollContainer(true);

			layout = new TiScrollViewLayout(context, arrangement);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.FILL_PARENT);
			layout.setLayoutParams(params);
			super.addView(layout, params);
		}

		@Override
		public void addView(View child,
				android.view.ViewGroup.LayoutParams params) {
			layout.addView(child, params);
		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt) {
			super.onScrollChanged(l, t, oldl, oldt);

			KrollDict data = new KrollDict();
			data.put("x", l);
			data.put("y", t);
			getProxy().fireEvent("scroll", data);
		}
	}

	public TiUIScrollView(TiViewProxy proxy)
	{
		// we create the view after the properties are procesed
		super(proxy);
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;
	}

	@Override
	public void processProperties(KrollDict d)
	{
		boolean showHorizontalScrollBar = false;
		boolean showVerticalScrollBar = false;

		if (d.containsKey(SHOW_HORIZONTAL_SCROLL_INDICATOR)) {
			showHorizontalScrollBar = TiConvert.toBoolean(d, SHOW_HORIZONTAL_SCROLL_INDICATOR);
		}
		if (d.containsKey(SHOW_VERTICAL_SCROLL_INDICATOR)) {
			showVerticalScrollBar = TiConvert.toBoolean(d, SHOW_VERTICAL_SCROLL_INDICATOR);
		}

		if (showHorizontalScrollBar && showVerticalScrollBar) {
			Log.w(LCAT, "Both scroll bars cannot be shown. Defaulting to vertical shown");
			showHorizontalScrollBar = false;
		}

		int type = TYPE_VERTICAL;

		if (d.containsKey("width") && d.containsKey("contentWidth")) {
			Object width = d.get("width");
			Object contentWidth = d.get("contentWidth");
			if (width.equals(contentWidth) || showVerticalScrollBar) {
				type = TYPE_VERTICAL;
			}
		}

		if (d.containsKey("height") && d.containsKey("contentHeight")) {
			Object height = d.get("height");
			Object contentHeight = d.get("contentHeight");
			if (height.equals(contentHeight) || showHorizontalScrollBar) {
				type = TYPE_HORIZONTAL;
			}
		}

		// android only property
		if (d.containsKey("scrollType")) {
			Object scrollType = d.get("scrollType");
			if (scrollType.equals("vertical")) {
				type = TYPE_VERTICAL;
			} else if (scrollType.equals("horizontal")) {
				type = TYPE_HORIZONTAL;
			}
		}
		
		// we create the view here since we now know the potential widget type
		View view = null;
		LayoutArrangement arrangement = LayoutArrangement.DEFAULT;
		if (d.containsKey(TiC.PROPERTY_LAYOUT) && d.getString(TiC.PROPERTY_LAYOUT).equals(TiC.LAYOUT_VERTICAL)) {
			arrangement = LayoutArrangement.VERTICAL;
		} else if (d.containsKey(TiC.PROPERTY_LAYOUT) && d.getString(TiC.PROPERTY_LAYOUT).equals(TiC.LAYOUT_HORIZONTAL)) {
			arrangement = LayoutArrangement.HORIZONTAL;
		}
		switch (type) {
			case TYPE_HORIZONTAL:
				Log.d(LCAT, "creating horizontal scroll view");
				view = new TiHorizontalScrollView(getProxy().getContext(), arrangement);
				break;
			case TYPE_VERTICAL:
			default:
				Log.d(LCAT, "creating vertical scroll view");
				view = new TiVerticalScrollView(getProxy().getContext(), arrangement);
		}
		setNativeView(view);

		nativeView.setHorizontalScrollBarEnabled(showHorizontalScrollBar);
		nativeView.setVerticalScrollBarEnabled(showVerticalScrollBar);

		super.processProperties(d);
	}

	public TiScrollViewLayout getLayout() {
		View nativeView = getNativeView();
		if (nativeView instanceof TiVerticalScrollView) {
			return ((TiVerticalScrollView)nativeView).layout;
		} else {
			return ((TiHorizontalScrollView)nativeView).layout;
		}
	}

	public void scrollTo(int x, int y)
	{
		getNativeView().scrollTo(x, y);
		getNativeView().computeScroll();
		//getLayout().scrollTo(x, y);
	}
	
	public void scrollToBottom() {
		View view = getNativeView();
		if (view instanceof TiHorizontalScrollView) {
			TiHorizontalScrollView scrollView = (TiHorizontalScrollView)view;
			scrollView.fullScroll(View.FOCUS_RIGHT);
		} else if (view instanceof TiVerticalScrollView) {
			TiVerticalScrollView scrollView = (TiVerticalScrollView)view;
			scrollView.fullScroll(View.FOCUS_DOWN);
		}
	}
	
	@Override
	public void add(TiUIView child) {
		super.add(child);
		
		if (getNativeView() != null) {
			getLayout().requestLayout();
			if (child.getNativeView() != null) {
				child.getNativeView().requestLayout();
			}
		}
	}

}
