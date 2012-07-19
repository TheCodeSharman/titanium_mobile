/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.Ti2DMatrix;
import org.appcelerator.titanium.view.TiAnimation;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.view.TiUIView;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;
import android.util.FloatMath;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;


public class TiAnimationBuilder
{
	private static final String LCAT = "TiAnimationBuilder";
	private static final boolean DBG = TiConfig.LOGD;
	
	protected float anchorX;
	protected float anchorY;
	protected Ti2DMatrix tdm = null;
	protected Double delay = null;
	protected Double duration = null;
	protected Double toOpacity = null;
	protected Double fromOpacity = null;
	protected Double repeat = null;
	protected Boolean autoreverse = null;
	protected String top = null, bottom = null, left = null, right = null;
	protected String centerX = null, centerY = null;
	protected String width = null, height = null;
	protected Integer backgroundColor = null;
	
	protected TiAnimation animationProxy;
	protected KrollFunction callback;
	protected boolean relayoutChild = false, applyOpacity = false;
	@SuppressWarnings("rawtypes")
	protected HashMap options;
	protected View view;
	protected TiViewProxy viewProxy;


	public TiAnimationBuilder() 
	{
		anchorX = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
		anchorY = Ti2DMatrix.DEFAULT_ANCHOR_VALUE;
	}

	@SuppressWarnings({"unchecked","rawtypes"})
	public void applyOptions(HashMap options)
	{
		if (options == null) {
			return;
		}

		if (options.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			Object anchorPoint = options.get(TiC.PROPERTY_ANCHOR_POINT);
			if (anchorPoint instanceof HashMap) {
				HashMap point = (HashMap)anchorPoint;
				anchorX = TiConvert.toFloat(point, TiC.PROPERTY_X);
				anchorY = TiConvert.toFloat(point, TiC.PROPERTY_Y);
			} else {
				Log.e(LCAT, "invalid argument type for anchorPoint property. Ignoring");
			}
		}

		if (options.containsKey(TiC.PROPERTY_TRANSFORM)) {
			tdm = (Ti2DMatrix) options.get(TiC.PROPERTY_TRANSFORM);
		}
		if (options.containsKey(TiC.PROPERTY_DELAY)) {
			delay = TiConvert.toDouble(options, TiC.PROPERTY_DELAY);
		}
		if (options.containsKey(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toDouble(options, TiC.PROPERTY_DURATION);
		}
		if (options.containsKey(TiC.PROPERTY_OPACITY)) {
			toOpacity = TiConvert.toDouble(options, TiC.PROPERTY_OPACITY);
		}
		if (options.containsKey(TiC.PROPERTY_REPEAT)) {
			repeat = TiConvert.toDouble(options, TiC.PROPERTY_REPEAT);
			if (repeat == 0d) {
				// A repeat of 0 is probably non-sensical. Titanium iOS
				// treats it as 1 and so should we.
				repeat = 1d;
			}
		} else {
			repeat = 1d; // Default as indicated in our documentation.
		}
		if (options.containsKey(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert.toBoolean(options, TiC.PROPERTY_AUTOREVERSE);
		}
		if (options.containsKey(TiC.PROPERTY_TOP)) {
			top = TiConvert.toString(options, TiC.PROPERTY_TOP);
		}
		if (options.containsKey(TiC.PROPERTY_BOTTOM)) {
			bottom = TiConvert.toString(options, TiC.PROPERTY_BOTTOM);
		}
		if (options.containsKey(TiC.PROPERTY_LEFT)) {
			left = TiConvert.toString(options, TiC.PROPERTY_LEFT);
		}
		if (options.containsKey(TiC.PROPERTY_RIGHT)) {
			right = TiConvert.toString(options, TiC.PROPERTY_RIGHT);
		}
		if (options.containsKey(TiC.PROPERTY_CENTER)) {
			Object centerPoint = options.get(TiC.PROPERTY_CENTER);
			if (centerPoint instanceof HashMap) {
				HashMap center = (HashMap) centerPoint;
				centerX = TiConvert.toString(center, TiC.PROPERTY_X);
				centerY = TiConvert.toString(center, TiC.PROPERTY_Y);
				
			} else {
				Log.e(LCAT, "Invalid argument type for center property. Ignoring");
			}
		}
		if (options.containsKey(TiC.PROPERTY_WIDTH)) {
			width = TiConvert.toString(options, TiC.PROPERTY_WIDTH);
		}
		if (options.containsKey(TiC.PROPERTY_HEIGHT)) {
			height = TiConvert.toString(options, TiC.PROPERTY_HEIGHT);
		}
		if (options.containsKey(TiC.PROPERTY_BACKGROUND_COLOR)) {
			backgroundColor = TiConvert.toColor(options, TiC.PROPERTY_BACKGROUND_COLOR);
		}
		
		this.options = options;
	}

	public void applyAnimation(TiAnimation anim)
	{
		this.animationProxy = anim;
		applyOptions(anim.getProperties());
	}

	public void setCallback(KrollFunction callback)
	{
		this.callback = callback;
	}

	public AnimationSet render(TiViewProxy viewProxy, View view)
	{
		ViewParent parent = view.getParent();
		int parentWidth = 0;
		int parentHeight = 0;

		if (parent instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) parent;
			parentHeight = group.getMeasuredHeight();
			parentWidth = group.getMeasuredWidth();
		}

		return render(viewProxy, view, view.getLeft(), view.getTop(), view.getMeasuredWidth(), view.getMeasuredHeight(), parentWidth, parentHeight);
	}

	private void addAnimation(AnimationSet animationSet, Animation animation)
	{
		boolean reverse = (autoreverse != null && autoreverse.booleanValue());
		animation.setRepeatMode(reverse ? Animation.REVERSE : Animation.RESTART);

		// We need to reduce the repeat count by 1, since for native Android
		// 1 would mean repeating it once.
		int repeatCount = (repeat == null ? 0 : repeat.intValue() - 1);

		// In Android (native), the repeat count includes reverses. So we
		// need to double-up and add one to the repeat count if we're reversing.
		if (reverse) {
			repeatCount = repeatCount * 2 + 1;
		}

		animation.setRepeatCount(repeatCount);

		animationSet.addAnimation(animation);
	}

	public TiMatrixAnimation createMatrixAnimation(Ti2DMatrix matrix)
	{
		return new TiMatrixAnimation(matrix, anchorX, anchorY);
	}

	public AnimationSet render(TiViewProxy viewProxy, View view, int x, int y, int w, int h, int parentWidth, int parentHeight)
	{
		this.view = view;
		this.viewProxy = viewProxy;
		TiUIView tiView = viewProxy.peekView();

		AnimationSet as = new AnimationSet(false);
		AnimationListener animationListener = new AnimationListener();

		if (toOpacity != null) {
			if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY)) {
				fromOpacity = TiConvert.toDouble(viewProxy.getProperty(TiC.PROPERTY_OPACITY));

			} else {
				fromOpacity = 1.0 - toOpacity;
			}

			Animation animation = new AlphaAnimation(fromOpacity.floatValue(), toOpacity.floatValue());
			applyOpacity = true;
			addAnimation(as, animation);
			animation.setAnimationListener(animationListener);

			if (viewProxy.hasProperty(TiC.PROPERTY_OPACITY) && fromOpacity != null && toOpacity != null
				&& tiView != null) {
				// Initialize the opacity to 1 when we are going to change it in
				// the animation. If the opacity of the view was initialized to
				// 0, the animation doesn't work
				tiView.setOpacity(1);
			}
		}

		if (backgroundColor != null) {
			int fromBackgroundColor = 0;

			if (viewProxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
				fromBackgroundColor = TiConvert.toColor(TiConvert.toString(viewProxy.getProperty(TiC.PROPERTY_BACKGROUND_COLOR)));
			} else {
				Log.w(LCAT, "Cannot animate view without a backgroundColor. View doesn't have that property. Using #00000000");
				fromBackgroundColor = Color.argb(0,0,0,0);
			}

			Animation a = new TiColorAnimation(view, fromBackgroundColor, backgroundColor);
			addAnimation(as, a);
		}

		if (tdm != null) {
			as.setFillAfter(true);
			as.setFillEnabled(true);

			Animation anim;
			if (tdm.hasScaleOperation() && tiView != null) {
				tiView.setAnimatedScaleValues(tdm.verifyScaleValues(tiView, (autoreverse != null && autoreverse.booleanValue())));
			}

			if (tdm.hasRotateOperation() && tiView != null) {
				tiView.setAnimatedRotationDegrees(tdm.verifyRotationValues(tiView, (autoreverse != null && autoreverse.booleanValue())));
			}

			anim = new TiMatrixAnimation(tdm, anchorX, anchorY);

			anim.setFillAfter(true);

			if (duration != null) {
				anim.setDuration(duration.longValue());
			}

			addAnimation(as, anim);

		}

		// Set duration after adding children.
		if (duration != null) {
			as.setDuration(duration.longValue());
		}
		if (delay != null) {
			as.setStartOffset(delay.longValue());
		}

		// ignore translate/resize if we have a matrix.. we need to eventually collect to/from properly
		if (top != null || bottom != null || left != null || right != null || centerX != null || centerY != null) {
			TiDimension optionTop = null, optionBottom = null;
			TiDimension optionLeft = null, optionRight = null;
			TiDimension optionCenterX = null, optionCenterY = null;

			// Note that we're stringifying the values to make sure we
			// use the correct TiDimension constructor, except when
			// we know the values are expressed for certain in pixels.
			if (top != null) {
				optionTop = new TiDimension(top, TiDimension.TYPE_TOP);
			} else {
				optionTop = new TiDimension(view.getTop(), TiDimension.TYPE_TOP);
				optionTop.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (bottom != null) {
				optionBottom = new TiDimension(bottom, TiDimension.TYPE_BOTTOM);
			} else {
				optionBottom = new TiDimension(view.getBottom(), TiDimension.TYPE_BOTTOM);
				optionBottom.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (left != null) {
				optionLeft = new TiDimension(left, TiDimension.TYPE_LEFT);
			} else {
				optionLeft = new TiDimension(view.getLeft(), TiDimension.TYPE_LEFT);
				optionLeft.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (right != null) {
				optionRight = new TiDimension(right, TiDimension.TYPE_RIGHT);
			} else {
				optionRight = new TiDimension(view.getRight(), TiDimension.TYPE_RIGHT);
				optionRight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (centerX != null) {
				optionCenterX = new TiDimension(centerX, TiDimension.TYPE_CENTER_X);
			}

			if (centerY != null) {
				optionCenterY = new TiDimension(centerY, TiDimension.TYPE_CENTER_Y);
			}

			int horizontal[] = new int[2];
			int vertical[] = new int[2];
			ViewParent parent = view.getParent();
			View parentView = null;

			if (parent instanceof View) {
				parentView = (View) parent;
			}

			//TODO: center
			TiCompositeLayout.computePosition(parentView, optionLeft, optionCenterX, optionRight, w, 0, parentWidth, horizontal);
			TiCompositeLayout.computePosition(parentView, optionTop, optionCenterY, optionBottom, h, 0, parentHeight, vertical);

			// Determine where the view has been animated to already (if at all), because it's from that
			// point where we'll want to animate now, rather than from the "true", non-animated position.
			// Android has no built-in way of telling us the location to where the view has already been animated,
			// so we have to look it up from a cache we keep maintain ourselves directly on the tiView.
			int previousXDelta = 0, previousYDelta = 0;
			int newXDelta = horizontal[0] - x;
			int newYDelta = vertical[0] - y;
			if (tiView != null) {
				Point currentTranslation = tiView.getAnimatedXYTranslationValues();
				if (currentTranslation != null) {
					previousXDelta = currentTranslation.x;
					previousYDelta = currentTranslation.y;
				}
			}

			Animation animation = new TranslateAnimation(Animation.ABSOLUTE, previousXDelta, Animation.ABSOLUTE, newXDelta,
				Animation.ABSOLUTE, previousYDelta, Animation.ABSOLUTE, newYDelta);
			animation.setFillEnabled(true);
			animation.setFillAfter(true);

			// Remember where we're going to, since there is no native way to look it up later.
			// We don't need to remember it if we're autoreversing, however.
			if (tiView != null && (autoreverse == null || !autoreverse.booleanValue())) {
				tiView.setAnimatedXYTranslationValues(new Point(newXDelta, newYDelta));
			}

			if (duration != null) {
				animation.setDuration(duration.longValue());
			}

			as.setFillEnabled(true);
			as.setFillAfter(true);
			animation.setAnimationListener(animationListener);
			addAnimation(as, animation);

			if (DBG) {
				Log.d(LCAT, "animate " + viewProxy + " relative to self: " + (horizontal[0]-x) + ", " + (vertical[0]-y));
			}

		}

		if (tdm == null && (width != null || height != null)) {
			TiDimension optionWidth, optionHeight;
			// we need to setup a custom animation for this, is there a better way?
			if (width != null) {
				optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
			} else {
				optionWidth = new TiDimension(w, TiDimension.TYPE_WIDTH);
				optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			if (height != null) {
				optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
			} else {
				optionHeight = new TiDimension(w, TiDimension.TYPE_HEIGHT);
				optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			int toWidth = optionWidth.getAsPixels(view);
			int toHeight = optionHeight.getAsPixels(view);
			SizeAnimation sizeAnimation = new SizeAnimation(view, w, h, toWidth, toHeight);

			if (duration != null) {
				sizeAnimation.setDuration(duration.longValue());
			}

			// Also wipe out any saved x,y deltas from Translate animation above,
			// since this size animation is going to reset the layout completely.
			if (tiView != null) {
				tiView.setAnimatedXYTranslationValues(new Point(0, 0));
			}

			sizeAnimation.setInterpolator(new LinearInterpolator());
			sizeAnimation.setAnimationListener(animationListener);
			as.addAnimation(sizeAnimation);

			relayoutChild = true;
		}

		if (callback != null || animationProxy != null) {
			as.setAnimationListener(animationListener);
		}

		return as;
	}

	protected class SizeAnimation extends Animation
	{
		protected View view;
		protected float fromWidth, fromHeight, toWidth, toHeight;
		protected static final String LCAT = "TiSizeAnimation";


		public SizeAnimation(View view, float fromWidth, float fromHeight, float toWidth, float toHeight)
		{
			this.view = view;
			this.fromWidth = fromWidth;
			this.fromHeight = fromHeight;
			this.toWidth = toWidth;
			this.toHeight = toHeight;

			if (DBG) {
				Log.d(LCAT, "animate view from (" + fromWidth + "x" + fromHeight + ") to (" + toWidth + "x" + toHeight + ")");
			}
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);
			
			int width = 0;
			if (fromWidth == toWidth) {
				width = (int) fromWidth;

			} else {
				width = (int) FloatMath.floor(fromWidth + ((toWidth - fromWidth) * interpolatedTime));
			}

			int height = 0;
			if (fromHeight == toHeight) {
				height = (int) fromHeight;

			} else {
				height = (int) FloatMath.floor(fromHeight + ((toHeight - fromHeight) * interpolatedTime));
			}
			
			ViewGroup.LayoutParams params = view.getLayoutParams();
			params.width = width;
			params.height = height;

			if (params instanceof TiCompositeLayout.LayoutParams) {
				TiCompositeLayout.LayoutParams tiParams = (TiCompositeLayout.LayoutParams)params;
				tiParams.optionHeight = new TiDimension(height, TiDimension.TYPE_HEIGHT);
				tiParams.optionHeight.setUnits(TypedValue.COMPLEX_UNIT_PX);
				tiParams.optionWidth = new TiDimension(width, TiDimension.TYPE_WIDTH);
				tiParams.optionWidth.setUnits(TypedValue.COMPLEX_UNIT_PX);
			}

			view.setLayoutParams(params);
		}
	}

	public static class TiMatrixAnimation extends Animation
	{
		protected Ti2DMatrix matrix;
		protected int childWidth, childHeight;
		protected float anchorX = -1, anchorY = -1;

		public boolean interpolate = true;


		public TiMatrixAnimation(Ti2DMatrix matrix, float anchorX, float anchorY)
		{
			this.matrix = matrix;
			this.anchorX = anchorX;
			this.anchorY = anchorY;
		}

		@Override
		public void initialize(int width, int height, int parentWidth, int parentHeight)
		{
			super.initialize(width, height, parentWidth, parentHeight);
			this.childWidth = width;
			this.childHeight = height;
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation transformation)
		{
			super.applyTransformation(interpolatedTime, transformation);
			if (interpolate) {
				Matrix m = matrix.interpolate(interpolatedTime, childWidth, childHeight, anchorX, anchorY);
				transformation.getMatrix().set(m);

			} else {
				transformation.getMatrix().set(getFinalMatrix(childWidth, childHeight));
			}
		}

		public Matrix getFinalMatrix(int childWidth, int childHeight)
		{
			return matrix.interpolate(1.0f, childWidth, childHeight, anchorX, anchorY);
		}

		public void invalidateWithMatrix(View view)
		{
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = getFinalMatrix(width, height);
			RectF rectF = new RectF(0, 0, width, height);
			m.mapRect(rectF);
			rectF.inset(-1.0f, -1.0f);
			Rect rect = new Rect();
			rectF.round(rect);

			if (view.getParent() instanceof ViewGroup) {
				int left = view.getLeft();
				int top = view.getTop();

				((ViewGroup)view.getParent()).invalidate(left + rect.left, top + rect.top, left + rect.width(), top + rect.height());
			}
		}
	}
	
	public static class TiColorAnimation extends Animation
	{
		protected View view;
		TransitionDrawable transitionDrawable;
		boolean started = false;
		
		public TiColorAnimation(View view, int fromColor, int toColor) 
		{
			this.view = view;

			ColorDrawable fromColorDrawable = new ColorDrawable(fromColor);
			ColorDrawable toColorDrawable = new ColorDrawable(toColor);
			transitionDrawable = new TransitionDrawable(new Drawable[]{fromColorDrawable, toColorDrawable});
		}

		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) 
		{
			super.applyTransformation(interpolatedTime, t);
			if (!started) {
				// Kick off a TransitionDrawable to do this for us. All
				// subsequent calls to applyTransformation will just be ignored.
				started = true;
				view.setBackgroundDrawable(transitionDrawable);
				long duration = this.getDuration();
				int durationInt = Long.valueOf(duration).intValue();
				transitionDrawable.startTransition(durationInt);
			}
		}
	}

	protected class AnimationListener implements Animation.AnimationListener
	{
		public void onAnimationEnd(Animation a)
		{
			if (relayoutChild) {
				LayoutParams params = (LayoutParams) view.getLayoutParams();
				TiConvert.fillLayout(options, params);
				view.setLayoutParams(params);
				view.clearAnimation();
				relayoutChild = false;
			}

			if (applyOpacity) {
				// There is an android bug where animations still occur after this method. We clear it from the view to
				// correct this.
				view.clearAnimation();
				if (toOpacity.floatValue() == 0) {
					view.setVisibility(View.INVISIBLE);
					
				} else {
					if (view.getVisibility() == View.INVISIBLE) {
						view.setVisibility(View.VISIBLE);
					}
					// this is apparently the only way to apply an opacity to the entire view and have it stick
					AlphaAnimation aa = new AlphaAnimation(toOpacity.floatValue(), toOpacity.floatValue());
					aa.setDuration(1);
					aa.setFillAfter(true);
					aa.setFillEnabled(true);
					view.setLayoutParams(view.getLayoutParams());
					view.startAnimation(aa);
				}

				applyOpacity = false;
			}

			if (a instanceof AnimationSet) {
				if (callback != null) {
					callback.callAsync(viewProxy.getKrollObject(), new Object[] { new KrollDict() });
				}

				if (animationProxy != null) {
					// In versions prior to Honeycomb, don't fire the event until the message queue
					// is empty.  There appears to be a bug in versions before Honeycomb where this
					// onAnimationEnd listener can be called even before the animation is really complete.
					if (Build.VERSION.SDK_INT >= TiC.API_LEVEL_HONEYCOMB) {
						animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
					} else {
						Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
							public boolean queueIdle()
							{
								animationProxy.fireEvent(TiC.EVENT_COMPLETE, null);
								return false;
							}
						});
					}
				}
			}
		}

		public void onAnimationRepeat(Animation a) 
		{
		}

		public void onAnimationStart(Animation a)
		{
			if (animationProxy != null) {
				animationProxy.fireEvent(TiC.EVENT_START, null);
			}
		}
	}
}
