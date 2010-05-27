/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;


import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.titanium.TiDict;
import org.appcelerator.titanium.TiProxy;
import org.appcelerator.titanium.TiProxyListener;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiAnimationBuilder;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.animation.AnimationSet;
import android.view.inputmethod.InputMethodManager;

public abstract class TiUIView
	implements TiProxyListener, OnFocusChangeListener
{
	private static final String LCAT = "TiUIView";
	private static final boolean DBG = TiConfig.LOGD;

	private static AtomicInteger idGenerator;

	protected View nativeView; // Native View object

	protected TiViewProxy proxy;
	protected TiViewProxy parent;

	protected LayoutParams layoutParams;
	protected int zIndex;
	protected TiAnimationBuilder animBuilder;
	protected TiBackgroundDrawable background;
	protected boolean usingCustomBackground = false;

	public TiUIView(TiViewProxy proxy)
	{
		if (idGenerator == null) {
			idGenerator = new AtomicInteger(0);
		}

		this.proxy = proxy;
		this.layoutParams = new TiCompositeLayout.LayoutParams();
		this.background = new TiBackgroundDrawable();
	}

	public void add(TiUIView child)
	{
		if (child != null) {
			View cv = child.getNativeView();
			if (cv != null) {
				View nv = getNativeView();
				if (nv instanceof ViewGroup) {
					if (cv.getParent() == null) {
						((ViewGroup) nv).addView(cv, child.getLayoutParams());
					}
				}
			}
		}
	}

	public void remove(TiUIView child)
	{
		if (child != null) {
			View cv = child.getNativeView();
			if (cv != null) {
				View nv = getNativeView();
				if (nv instanceof ViewGroup) {
					((ViewGroup) nv).removeView(cv);
				}
			}
		}
	}

	public TiViewProxy getProxy() {
		return proxy;
	}
	public void setProxy(TiViewProxy proxy) {
		this.proxy = proxy;
	}
	public TiViewProxy getParent() {
		return parent;
	}
	public void setParent(TiViewProxy parent) {
		this.parent = parent;
	}
	public LayoutParams getLayoutParams() {
		return layoutParams;
	}
	public int getZIndex() {
		return zIndex;
	}
	public View getNativeView() {
		return nativeView;
	}
	protected void setNativeView(View view) {
		if (view.getId() == View.NO_ID) {
			view.setId(idGenerator.incrementAndGet());
		}
		this.nativeView = view;
		nativeView.setOnFocusChangeListener(this);
	}
	protected void setLayoutParams(LayoutParams layoutParams) {
		this.layoutParams = layoutParams;
	}
	protected void setZIndex(int index) {
		zIndex = index;
	}

	public void animate()
	{
		TiAnimationBuilder builder = proxy.getPendingAnimation();
		if (builder != null && nativeView != null) {

			// Capture dimension
			int w = nativeView.getMeasuredWidth();
			int h = nativeView.getMeasuredHeight();

			AnimationSet as = builder.render(w, h);
			Log.d(LCAT, "starting animation: "+as);
			nativeView.startAnimation(as);

			// Clean up proxy
			proxy.clearAnimation();
		}
	}

	public void listenerAdded(String type, int count, TiProxy proxy) {
	}

	public void listenerRemoved(String type, int count, TiProxy proxy) {
	}

	public void propertyChanged(String key, Object oldValue, Object newValue, TiProxy proxy)
	{
		if (key.equals("left")) {
			if (newValue != null) {
				layoutParams.optionLeft = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
			} else {
				layoutParams.optionLeft = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("top")) {
			if (newValue != null) {
				layoutParams.optionTop = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
			} else {
				layoutParams.optionTop = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("right")) {
			if (newValue != null) {
				layoutParams.optionRight = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
			} else {
				layoutParams.optionRight = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("bottom")) {
			if (newValue != null) {
				layoutParams.optionBottom = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
			} else {
				layoutParams.optionBottom = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("height")) {
			if (newValue != null) {
				if (!newValue.equals("auto")) {
					layoutParams.optionHeight = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
					layoutParams.autoHeight = false;
				} else {
					layoutParams.optionHeight = TiCompositeLayout.NOT_SET;
					layoutParams.autoHeight = true;
				}
			} else {
				layoutParams.optionHeight = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("width")) {
			if (newValue != null) {
				if (!newValue.equals("auto")) {
					layoutParams.optionWidth = TiConvert.toTiDimension(TiConvert.toString(newValue)).getIntValue();
					layoutParams.autoWidth = false;
				} else {
					layoutParams.optionWidth = TiCompositeLayout.NOT_SET;
					layoutParams.autoWidth = true;
				}
			} else {
				layoutParams.optionWidth = TiCompositeLayout.NOT_SET;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("zIndex")) {
			if (newValue != null) {
				layoutParams.optionZIndex = TiConvert.toInt(TiConvert.toString(newValue));
			} else {
				layoutParams.optionZIndex = 0;
			}
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		} else if (key.equals("visible")) {
			nativeView.setVisibility(TiConvert.toBoolean(newValue) ? View.VISIBLE : View.INVISIBLE);
		} else if (key.equals("opacity") || key.equals("backgroundColor")) {
			TiDict d = proxy.getDynamicProperties();
			if (proxy.getDynamicValue("backgroundColor") != null) {
				Integer bgColor = TiConvert.toColor(d, "backgroundColor", "opacity");
				background.setBackgroundColor(bgColor);
				//nativeView.setBackgroundDrawable(new ColorDrawable(bgColor));
				if (nativeView != null){					
					nativeView.postInvalidate();
				}				
			} else {
				Log.w(LCAT, "Unable to set opacity w/o background color");
			}
		} else if (key.equals("backgroundImage")) {
			handleBackgroundImage(proxy.getDynamicProperties());
			if (nativeView != null) {
				nativeView.postInvalidate();
			}
		} else if (key.startsWith("border")) {
			handleBorderProperty(key, newValue);
			if (nativeView != null) {
				nativeView.postInvalidate();
			}
		} else {
			if (DBG) {
				Log.i(LCAT, "Unhandled property key: " + key);
			}
		}
	}

	public void processProperties(TiDict d)
	{
		if (d.containsKey("layout")) {
			String layout = TiConvert.toString(d, "layout");
			if (layout.equals("vertical")) {
				if (nativeView instanceof TiCompositeLayout) {
					((TiCompositeLayout)nativeView).setVerticalLayout(true);
				}
			}
		}
		if (TiConvert.fillLayout(d, layoutParams)) {
			if (nativeView != null) {
				nativeView.requestLayout();
			}
		}

		Integer bgColor = null;

		// Default background processing.
		// Prefer image to color.
		if (d.containsKey("backgroundImage")) {
			handleBackgroundImage(d);
		} else if (d.containsKey("backgroundColor")) {
			bgColor = TiConvert.toColor(d, "backgroundColor", "opacity");
			nativeView.setBackgroundColor(bgColor);
		}
		if (d.containsKey("visible")) {
			nativeView.setVisibility(TiConvert.toBoolean(d, "visible") ? View.VISIBLE : View.INVISIBLE);
		}

		initializeBorder(d, bgColor);

		if (d.containsKey("transform")) {
			animBuilder = new TiAnimationBuilder();
			animBuilder.applyOptions(d);
			AnimationSet as = animBuilder.render(nativeView);
			nativeView.startAnimation(as);
		}
	}

	private void applyCustomBackground() {
		applyCustomBackground(true);
	}

	private void applyCustomBackground(boolean reuseCurrentDrawable) {
		if (nativeView != null && !usingCustomBackground) {
			nativeView.setClickable(true);

			Drawable currentDrawable = nativeView.getBackground();
			if (currentDrawable != null && reuseCurrentDrawable) {
				background.setBackgroundDrawable(currentDrawable);
			}
			nativeView.setBackgroundDrawable(background);
			usingCustomBackground = true;
		}
	}

	public void onFocusChange(View v, boolean hasFocus)
	{
		if (hasFocus) {
			proxy.fireEvent("focus", getFocusEventObject(hasFocus));
		} else {
			proxy.fireEvent("blur", getFocusEventObject(hasFocus));
		}
	}

	protected TiDict getFocusEventObject(boolean hasFocus) {
		return null;
	}

	protected InputMethodManager getIMM() {
		InputMethodManager imm = null;
		imm = (InputMethodManager) proxy.getTiContext().getTiApp().getSystemService(Context.INPUT_METHOD_SERVICE);
		return imm;
	}

	public void focus() {
		if (nativeView != null) {
			nativeView.requestFocus();
		}
	}

	public void blur() {
		if (nativeView != null) {
	        InputMethodManager imm = getIMM();
	        if (imm != null) {
	        	imm.hideSoftInputFromWindow(nativeView.getWindowToken(), 0);
	        }
			nativeView.clearFocus();
		}
	}

	public void release()
	{
		if (DBG) {
			Log.i(LCAT, "Release: " + getClass().getSimpleName());
		}
		View nv = getNativeView();
		if (nv != null) {
			if (nv instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) nv;
				if (DBG) {
					Log.d(LCAT, "Group has: " + vg.getChildCount());
				}
				vg.removeAllViews();
			}
		}

		Drawable d = nv.getBackground();
		if (d != null) {
			nv.setBackgroundDrawable(null);
			d.setCallback(null);
			if (d instanceof TiBackgroundDrawable) {
				((TiBackgroundDrawable)d).releaseDelegate();
			}
			d = null;
		}
	}

	public void show()
	{
		if (nativeView != null) {
			nativeView.setVisibility(View.VISIBLE);
		} else {
			if (DBG) {
				Log.w(LCAT, "Attempt to show null native control");
			}
		}
	}

	public void hide()
	{
		if (nativeView != null) {
			nativeView.setVisibility(View.INVISIBLE);
		} else {
			if (DBG) {
				Log.w(LCAT, "Attempt to hide null native control");
			}
		}
	}

	// Initial implementation.
	// TODO implement other background states.
	private void handleBackgroundImage(TiDict d)
	{
		String path = TiConvert.toString(d, "backgroundImage");
		if (path.endsWith(".9.png")) {
			TiFileHelper helper = new TiFileHelper(getProxy().getTiContext().getActivity());
			Drawable drawable = helper.loadDrawable(path, false, true);
			if (drawable !=  null) {
				background.setBackgroundDrawable(drawable);
				applyCustomBackground(false);
			}
		} else {
			String url = getProxy().getTiContext().resolveUrl(null, path);
			TiBaseFile file = TiFileFactory.createTitaniumFile(getProxy().getTiContext(), new String[] { url }, false);
			InputStream is = null;
			try {
				is = file.getInputStream();
				Bitmap b = TiUIHelper.createBitmap(is);
				if (b != null) {
					background.setBackgroundImage(b);
					applyCustomBackground(false);
				}
			} catch (IOException e) {
				Log.e(LCAT, "Error creating background image from path: " + path.toString(), e);
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException ig) {
						// Ignore
					}
				}
			}
		}
	}

	private void initializeBorder(TiDict d, Integer bgColor)
	{
		if (d.containsKey("borderRadius") || d.containsKey("borderColor") || d.containsKey("borderWidth")) {

			if (background.getBorder() == null) {
				background.setBorder(new TiBackgroundDrawable.Border());
			}

			TiBackgroundDrawable.Border border = background.getBorder();

			if (d.containsKey("borderRadius")) {

				if (d.containsKey("borderRadius")) {
					border.setRadius(TiConvert.toFloat(d, "borderRadius"));
				}
			}
			if (d.containsKey("borderColor") || d.containsKey("borderWidth")) {
				if (d.containsKey("borderColor")) {
					border.setColor(TiConvert.toColor(d, "borderColor", "opacity"));
				} else {
					if (bgColor != null) {
						border.setColor(bgColor);
					}
				}
				if (d.containsKey("borderWidth")) {
					border.setWidth(TiConvert.toFloat(d, "borderWidth"));
				}
			}
			applyCustomBackground();
		}
	}

	private void handleBorderProperty(String property, Object value)
	{
		if (background.getBorder() == null) {
			background.setBorder(new TiBackgroundDrawable.Border());
		}
		TiBackgroundDrawable.Border border = background.getBorder();

		if (property.equals("borderColor")) {
			border.setColor(TiConvert.toColor(value.toString()));
		} else if (property.equals("borderRadius")) {
			border.setRadius(TiConvert.toFloat(value));
		} else if (property.equals("borderWidth")) {
			border.setWidth(TiConvert.toFloat(value));
		}
		applyCustomBackground();
	}

	private static HashMap<Integer, String> motionEvents = new HashMap<Integer,String>();
	static {
		motionEvents.put(MotionEvent.ACTION_DOWN, "touchstart");
		motionEvents.put(MotionEvent.ACTION_UP, "touchend");
		motionEvents.put(MotionEvent.ACTION_MOVE, "touchmove");
		motionEvents.put(MotionEvent.ACTION_CANCEL, "touchcancel");
	}

	private TiDict dictFromEvent(MotionEvent e) {
		TiDict data = new TiDict();
		data.put("x", (double)e.getX());
		data.put("y", (double)e.getY());
		data.put("source", proxy);
		return data;
	}

	protected boolean allowRegisterForTouch() {
		return true;
	}

	public void registerForTouch() {
		if (allowRegisterForTouch()) {
			registerForTouch(getNativeView());
		}
	}

	protected void registerForTouch(View touchable) {
		if (touchable == null) {
			return;
		}
		final GestureDetector detector = new GestureDetector(proxy.getTiContext().getActivity(),
			new SimpleOnGestureListener() {
				@Override
				public boolean onDoubleTap(MotionEvent e) {
					boolean handledTap = proxy.fireEvent("doubletap", dictFromEvent(e));
					boolean handledClick = proxy.fireEvent("dblclick", dictFromEvent(e));
					return handledTap || handledClick;
				}

				@Override
				public boolean onSingleTapConfirmed(MotionEvent e) {
					Log.e(LCAT, "TAP, TAP, TAP");
					boolean handledTap = proxy.fireEvent("singletap", dictFromEvent(e));
					boolean handledClick = proxy.fireEvent("click", dictFromEvent(e));
					return handledTap || handledClick;
				}
			});

		touchable.setOnTouchListener(new OnTouchListener() {
			public boolean onTouch(View view, MotionEvent event) {
				boolean handled = detector.onTouchEvent(event);
				if (!handled && motionEvents.containsKey(event.getAction())) {
					handled = proxy.fireEvent(motionEvents.get(event.getAction()), dictFromEvent(event));
				}
				return handled;
			}
		});

	}

	public TiDict toImage() {
		return TiUIHelper.viewToImage(proxy.getTiContext(), getNativeView());
	}
}
