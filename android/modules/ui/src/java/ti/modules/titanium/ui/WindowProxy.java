/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui;

import java.lang.ref.WeakReference;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiActivity;
import org.appcelerator.titanium.TiActivityWindow;
import org.appcelerator.titanium.TiActivityWindows;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiTranslucentActivity;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.view.TiUIView;
import ti.modules.titanium.ui.widget.TiView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.transition.ChangeBounds;
import android.transition.ChangeClipBounds;
import android.transition.ChangeImageTransform;
import android.transition.ChangeTransform;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
// clang-format off
@Kroll.proxy(creatableInModule = UIModule.class,
	propertyAccessors = {
		TiC.PROPERTY_MODAL,
		TiC.PROPERTY_WINDOW_PIXEL_FORMAT,
		TiC.PROPERTY_FLAG_SECURE
})
// clang-format on
public class WindowProxy extends TiWindowProxy implements TiActivityWindow
{
	private static final String TAG = "WindowProxy";
	private static final String PROPERTY_POST_WINDOW_CREATED = "postWindowCreated";

	private static final int MSG_FIRST_ID = TiWindowProxy.MSG_LAST_ID + 1;
	private static final int MSG_SET_PIXEL_FORMAT = MSG_FIRST_ID + 100;
	private static final int MSG_SET_TITLE = MSG_FIRST_ID + 101;
	private static final int MSG_SET_WIDTH_HEIGHT = MSG_FIRST_ID + 102;
	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	private WeakReference<TiBaseActivity> windowActivity;

	public WindowProxy()
	{
		super();
		defaultValues.put(TiC.PROPERTY_WINDOW_PIXEL_FORMAT, PixelFormat.UNKNOWN);
		defaultValues.put(TiC.PROPERTY_SUSTAINED_PERFORMANCE_MODE, false);
	}

	@Override
	protected KrollDict getLangConversionTable()
	{
		KrollDict table = new KrollDict();
		table.put(TiC.PROPERTY_TITLE, TiC.PROPERTY_TITLEID);
		return table;
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		TiUIView v = new TiView(this);
		v.getLayoutParams().autoFillsHeight = true;
		v.getLayoutParams().autoFillsWidth = true;
		setView(v);
		return v;
	}

	@Override
	public void open(@Kroll.argument(optional = true) Object arg)
	{
		HashMap<String, Object> option = null;
		if (arg instanceof HashMap) {
			option = (HashMap<String, Object>) arg;
		}
		if (option != null) {
			properties.putAll(option);
		}

		if (hasProperty(TiC.PROPERTY_ORIENTATION_MODES)) {
			Object obj = getProperty(TiC.PROPERTY_ORIENTATION_MODES);
			if (obj instanceof Object[]) {
				orientationModes = TiConvert.toIntArray((Object[]) obj);
			}
		}

		// The "top", "bottom", "left" and "right" properties do not work for heavyweight windows.
		properties.remove(TiC.PROPERTY_TOP);
		properties.remove(TiC.PROPERTY_BOTTOM);
		properties.remove(TiC.PROPERTY_LEFT);
		properties.remove(TiC.PROPERTY_RIGHT);
		super.open(arg);
	}

	@Override
	public void close(@Kroll.argument(optional = true) Object arg)
	{
		if (!(opened || opening)) {
			return;
		}
		super.close(arg);
	}

	@Override
	protected void handleOpen(KrollDict options)
	{
		Activity topActivity = TiApplication.getAppCurrentActivity();
		// Don't open if app is closing or closed
		if (topActivity == null || topActivity.isFinishing()) {
			return;
		}
		Intent intent = new Intent(topActivity, TiActivity.class);
		fillIntent(topActivity, intent);

		int windowId = TiActivityWindows.addWindow(this);
		intent.putExtra(TiC.INTENT_PROPERTY_USE_ACTIVITY_WINDOW, true);
		intent.putExtra(TiC.INTENT_PROPERTY_WINDOW_ID, windowId);

		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		if (!animated) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			topActivity.startActivity(intent);
			topActivity.overridePendingTransition(0, 0);
		} else if (options.containsKey(TiC.PROPERTY_ACTIVITY_ENTER_ANIMATION)
				   || options.containsKey(TiC.PROPERTY_ACTIVITY_EXIT_ANIMATION)) {
			topActivity.startActivity(intent);
			int enterAnimation = TiConvert.toInt(options.get(TiC.PROPERTY_ACTIVITY_ENTER_ANIMATION), 0);
			int exitAnimation = TiConvert.toInt(options.get(TiC.PROPERTY_ACTIVITY_EXIT_ANIMATION), 0);
			topActivity.overridePendingTransition(enterAnimation, exitAnimation);
		} else {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
				&& Build.VERSION.SDK_INT != Build.VERSION_CODES.M) {
				topActivity.startActivity(intent, createActivityOptionsBundle(topActivity));
			} else {
				topActivity.startActivity(intent);
			}
		}

		if (options.containsKey(TiC.PROPERTY_SUSTAINED_PERFORMANCE_MODE)) {
			setSustainedPerformanceMode((Boolean) options.get(TiC.PROPERTY_SUSTAINED_PERFORMANCE_MODE));
		}
	}

	@Override
	protected void handleClose(KrollDict options)
	{
		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		TiBaseActivity activity = (windowActivity != null) ? windowActivity.get() : null;
		if (activity != null && !activity.isFinishing()) {
			if (super.hasActivityTransitions()) {
				activity.finishAfterTransition();
			} else {
				activity.finish();
			}
			if (!animated) {
				activity.overridePendingTransition(0, 0);
			} else if (options.containsKey(TiC.PROPERTY_ACTIVITY_ENTER_ANIMATION)
					   || options.containsKey(TiC.PROPERTY_ACTIVITY_EXIT_ANIMATION)) {
				int enterAnimation = TiConvert.toInt(options.get(TiC.PROPERTY_ACTIVITY_ENTER_ANIMATION), 0);
				int exitAnimation = TiConvert.toInt(options.get(TiC.PROPERTY_ACTIVITY_EXIT_ANIMATION), 0);
				activity.overridePendingTransition(enterAnimation, exitAnimation);
			}

			// Finishing an activity is not synchronous, so we remove the activity from the activity stack here
			TiApplication.removeFromActivityStack(activity);
			windowActivity = null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void windowCreated(TiBaseActivity activity, Bundle savedInstanceState)
	{
		windowActivity = new WeakReference<TiBaseActivity>(activity);
		activity.setWindowProxy(this);
		setActivity(activity);

		// Handle the "activity" property.
		ActivityProxy activityProxy = activity.getActivityProxy();
		KrollDict options = null;
		if (hasProperty(TiC.PROPERTY_ACTIVITY)) {
			Object activityObject = getProperty(TiC.PROPERTY_ACTIVITY);
			if (activityObject instanceof HashMap<?, ?>) {
				options = new KrollDict((HashMap<String, Object>) activityObject);
				activityProxy.handleCreationDict(options);
			}
		}

		Window win = activity.getWindow();
		// Handle the background of the window activity if it is a translucent activity.
		// If it is a modal window, set a translucent dimmed background to the window.
		// If the opacity is given, set a transparent background to the window. In this case, if no backgroundColor or
		// backgroundImage is given, the window will be completely transparent.
		boolean modal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_MODAL), false);
		Drawable background = null;
		if (modal) {
			background = new ColorDrawable(0x9F000000);
		} else if (hasProperty(TiC.PROPERTY_OPACITY)) {
			background = new ColorDrawable(0x00000000);
		}
		if (background != null) {
			win.setBackgroundDrawable(background);
		}

		// Handle activity transitions
		if (LOLLIPOP_OR_GREATER) {
			applyActivityTransitions(win, properties);
		}

		// Handle the width and height of the window.
		// TODO: If width / height is a percentage value, we can not get the dimension in pixel because
		// the width / height of the decor view is not measured yet at this point. So we can not use the
		// getAsPixels() method. Maybe we can use WindowManager.getDefaultDisplay.getRectSize(rect) to
		// get the application display dimension.
		if (hasProperty(TiC.PROPERTY_WIDTH) || hasProperty(TiC.PROPERTY_HEIGHT)) {
			Object width = getProperty(TiC.PROPERTY_WIDTH);
			Object height = getProperty(TiC.PROPERTY_HEIGHT);
			View decorView = win.getDecorView();
			if (decorView != null) {
				int w = LayoutParams.MATCH_PARENT;
				if (!(width == null || width.equals(TiC.LAYOUT_FILL))) {
					TiDimension wDimension = TiConvert.toTiDimension(width, TiDimension.TYPE_WIDTH);
					if (!wDimension.isUnitPercent()) {
						w = wDimension.getAsPixels(decorView);
					}
				}
				int h = LayoutParams.MATCH_PARENT;
				if (!(height == null || height.equals(TiC.LAYOUT_FILL))) {
					TiDimension hDimension = TiConvert.toTiDimension(height, TiDimension.TYPE_HEIGHT);
					if (!hDimension.isUnitPercent()) {
						h = hDimension.getAsPixels(decorView);
					}
				}
				win.setLayout(w, h);
			}
		}

		activity.getActivityProxy().getDecorView().add(this);
		activity.addWindowToStack(this);

		// Need to handle the cached activity proxy properties and url window in the JS side.
		callPropertySync(PROPERTY_POST_WINDOW_CREATED, null);
	}

	@Override
	public void onWindowActivityCreated()
	{
		// Fire the open event after setContentView() because getActionBar() need to be called
		// after setContentView(). (TIMOB-14914)
		opened = true;
		opening = false;
		fireEvent(TiC.EVENT_OPEN, null);
		handlePostOpen();

		super.onWindowActivityCreated();
	}

	@Override
	protected Activity getWindowActivity()
	{
		return (windowActivity != null) ? windowActivity.get() : null;
	}

	private void fillIntent(Activity activity, Intent intent)
	{
		int windowFlags = 0;
		if (hasProperty(TiC.PROPERTY_WINDOW_FLAGS)) {
			windowFlags = TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_FLAGS), 0);
		}

		//Set the fullscreen flag
		if (hasProperty(TiC.PROPERTY_FULLSCREEN)) {
			boolean flagVal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_FULLSCREEN), false);
			if (flagVal) {
				windowFlags = windowFlags | WindowManager.LayoutParams.FLAG_FULLSCREEN;
			}
		}

		//Set the secure flag
		if (hasProperty(TiC.PROPERTY_FLAG_SECURE)) {
			boolean flagVal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_FLAG_SECURE), false);
			if (flagVal) {
				windowFlags = windowFlags | WindowManager.LayoutParams.FLAG_SECURE;
			}
		}

		//Stuff flags in intent
		intent.putExtra(TiC.PROPERTY_WINDOW_FLAGS, windowFlags);

		if (hasProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE)) {
			intent.putExtra(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE,
							TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE), -1));
		}
		if (hasProperty(TiC.PROPERTY_EXIT_ON_CLOSE)) {
			intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT,
							TiConvert.toBoolean(getProperty(TiC.PROPERTY_EXIT_ON_CLOSE), false));
		} else {
			intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT, activity.isTaskRoot());
		}

		boolean modal = false;
		if (hasProperty(TiC.PROPERTY_MODAL)) {
			modal = TiConvert.toBoolean(getProperty(TiC.PROPERTY_MODAL), false);
			if (modal) {
				intent.setClass(activity, TiTranslucentActivity.class);
			}
			intent.putExtra(TiC.PROPERTY_MODAL, modal);
		}
		if (!modal && hasProperty(TiC.PROPERTY_OPACITY)) {
			intent.setClass(activity, TiTranslucentActivity.class);
		} else if (hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
			int bgColor = TiConvert.toColor(properties, TiC.PROPERTY_BACKGROUND_COLOR);
			if (Color.alpha(bgColor) < 0xFF) {
				intent.setClass(activity, TiTranslucentActivity.class);
			}
		}
		if (hasProperty(TiC.PROPERTY_WINDOW_PIXEL_FORMAT)) {
			intent.putExtra(TiC.PROPERTY_WINDOW_PIXEL_FORMAT,
							TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_PIXEL_FORMAT), PixelFormat.UNKNOWN));
		}
		if (hasProperty(TiC.PROPERTY_EXTEND_SAFE_AREA)) {
			boolean value = TiConvert.toBoolean(getProperty(TiC.PROPERTY_EXTEND_SAFE_AREA), false);
			intent.putExtra(TiC.PROPERTY_EXTEND_SAFE_AREA, value);
		}

		// Set the theme property
		if (hasProperty(TiC.PROPERTY_THEME)) {
			String theme = TiConvert.toString(getProperty(TiC.PROPERTY_THEME));
			if (theme != null) {
				try {
					intent.putExtra(TiC.PROPERTY_THEME,
									TiRHelper.getResource("style." + theme.replaceAll("[^A-Za-z0-9_]", "_")));
				} catch (Exception e) {
					Log.w(TAG, "Cannot find the theme: " + theme);
				}
			}
		}

		// Set the splitActionBar property
		if (hasProperty(TiC.PROPERTY_SPLIT_ACTIONBAR)) {
			boolean splitActionBar = TiConvert.toBoolean(getProperty(TiC.PROPERTY_SPLIT_ACTIONBAR), false);
			if (splitActionBar) {
				intent.putExtra(TiC.PROPERTY_SPLIT_ACTIONBAR, splitActionBar);
			}
		}
	}

	@Override
	public void onPropertyChanged(String name, Object value)
	{
		if (opening || opened) {
			if (TiC.PROPERTY_WINDOW_PIXEL_FORMAT.equals(name)) {
				getMainHandler().obtainMessage(MSG_SET_PIXEL_FORMAT, value).sendToTarget();
			} else if (TiC.PROPERTY_TITLE.equals(name)) {
				getMainHandler().obtainMessage(MSG_SET_TITLE, value).sendToTarget();
			} else if (TiC.PROPERTY_TOP.equals(name) || TiC.PROPERTY_BOTTOM.equals(name)
					   || TiC.PROPERTY_LEFT.equals(name) || TiC.PROPERTY_RIGHT.equals(name)) {
				// The "top", "bottom", "left" and "right" properties do not work for heavyweight windows.
				return;
			} else if (TiC.PROPERTY_EXIT_ON_CLOSE.equals(name)) {
				Activity activity = (windowActivity != null) ? (Activity) (windowActivity.get()) : null;
				if (activity != null) {
					Intent intent = activity.getIntent();
					intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT, TiConvert.toBoolean(value));
				}
			}
		}

		super.onPropertyChanged(name, value);
	}

	// clang-format off
	@Kroll.method
	@Kroll.setProperty
	public void setSustainedPerformanceMode(boolean mode)
	// clang-format on
	{
		setProperty(TiC.PROPERTY_SUSTAINED_PERFORMANCE_MODE, mode);
		windowActivity.get().setSustainMode(mode);
	}

	// clang-format off
	@Kroll.method
	@Kroll.getProperty
	public boolean getSustainedPerformanceMode()
	// clang-format on
	{
		return TiConvert.toBoolean(getProperty(TiC.PROPERTY_SUSTAINED_PERFORMANCE_MODE), false);
	}

	// clang-format off
	@Override
	@Kroll.setProperty(retain = false)
	@Kroll.method
	public void setWidth(Object width)
	// clang-format on
	{
		if (opening || opened) {
			Object current = getProperty(TiC.PROPERTY_WIDTH);
			if (shouldFireChange(current, width)) {
				Object height = getProperty(TiC.PROPERTY_HEIGHT);
				if (TiApplication.isUIThread()) {
					setWindowWidthHeight(width, height);
				} else {
					getMainHandler().obtainMessage(MSG_SET_WIDTH_HEIGHT, new Object[] { width, height }).sendToTarget();
				}
			}
		}
		super.setWidth(width);
	}

	// clang-format off
	@Override
	@Kroll.setProperty(retain = false)
	@Kroll.method
	public void setHeight(Object height)
	// clang-format on
	{
		if (opening || opened) {
			Object current = getProperty(TiC.PROPERTY_HEIGHT);
			if (shouldFireChange(current, height)) {
				Object width = getProperty(TiC.PROPERTY_WIDTH);
				if (TiApplication.isUIThread()) {
					setWindowWidthHeight(width, height);
				} else {
					getMainHandler().obtainMessage(MSG_SET_WIDTH_HEIGHT, new Object[] { width, height }).sendToTarget();
				}
			}
		}
		super.setHeight(height);
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_SET_PIXEL_FORMAT: {
				Activity activity = getWindowActivity();
				if (activity != null) {
					Window win = activity.getWindow();
					if (win != null) {
						win.setFormat(TiConvert.toInt((Object) (msg.obj), PixelFormat.UNKNOWN));
						win.getDecorView().invalidate();
					}
				}
				return true;
			}
			case MSG_SET_TITLE: {
				Activity activity = getWindowActivity();
				if (activity != null) {
					activity.setTitle(TiConvert.toString((Object) (msg.obj), ""));
				}
				return true;
			}
			case MSG_SET_WIDTH_HEIGHT: {
				Object[] obj = (Object[]) msg.obj;
				setWindowWidthHeight(obj[0], obj[1]);
				return true;
			}
		}
		return super.handleMessage(msg);
	}

	private void setWindowWidthHeight(Object width, Object height)
	{
		Activity activity = getWindowActivity();
		if (activity != null) {
			Window win = activity.getWindow();
			if (win != null) {
				View decorView = win.getDecorView();
				if (decorView != null) {
					int w = LayoutParams.MATCH_PARENT;
					if (!(width == null || width.equals(TiC.LAYOUT_FILL))) {
						TiDimension wDimension = TiConvert.toTiDimension(width, TiDimension.TYPE_WIDTH);
						if (!wDimension.isUnitPercent()) {
							w = wDimension.getAsPixels(decorView);
						}
					}
					int h = LayoutParams.MATCH_PARENT;
					if (!(height == null || height.equals(TiC.LAYOUT_FILL))) {
						TiDimension hDimension = TiConvert.toTiDimension(height, TiDimension.TYPE_HEIGHT);
						if (!hDimension.isUnitPercent()) {
							h = hDimension.getAsPixels(decorView);
						}
					}
					win.setLayout(w, h);
				}
			}
		}
	}

	/**
	 * Helper method to apply activity transitions.
	 * @param win The window holding the activity.
	 * @param props The property dictionary.
	 */
	private void applyActivityTransitions(Window win, KrollDict props)
	{
		if (LOLLIPOP_OR_GREATER) {
			// Return and reenter transitions defaults to enter and exit transitions respectively only if they are not set.
			// And setting a null transition makes the view unaccounted from transition.
			if (props.containsKeyAndNotNull(TiC.PROPERTY_ENTER_TRANSITION)) {
				win.setEnterTransition(createTransition(props, TiC.PROPERTY_ENTER_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_EXIT_TRANSITION)) {
				win.setExitTransition(createTransition(props, TiC.PROPERTY_EXIT_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_RETURN_TRANSITION)) {
				win.setReturnTransition(createTransition(props, TiC.PROPERTY_RETURN_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_REENTER_TRANSITION)) {
				win.setReenterTransition(createTransition(props, TiC.PROPERTY_REENTER_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_SHARED_ELEMENT_ENTER_TRANSITION)) {
				win.setSharedElementEnterTransition(
					createTransition(props, TiC.PROPERTY_SHARED_ELEMENT_ENTER_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_SHARED_ELEMENT_EXIT_TRANSITION)) {
				win.setSharedElementExitTransition(
					createTransition(props, TiC.PROPERTY_SHARED_ELEMENT_EXIT_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_SHARED_ELEMENT_REENTER_TRANSITION)) {
				win.setSharedElementReenterTransition(
					createTransition(props, TiC.PROPERTY_SHARED_ELEMENT_REENTER_TRANSITION));
			}

			if (props.containsKeyAndNotNull(TiC.PROPERTY_SHARED_ELEMENT_RETURN_TRANSITION)) {
				win.setSharedElementReturnTransition(
					createTransition(props, TiC.PROPERTY_SHARED_ELEMENT_RETURN_TRANSITION));
			}
		}
	}

	/**
	 * Creates a transition for the supplied transition type.
	 * @param props The property dictionary.
	 * @param key The transition type
	 * @return A Transition or null if UIModule.TRANSITION_NONE or unknown transition is specified.
	 */
	@SuppressLint({ "InlinedApi", "RtlHardcoded" })
	@Nullable
	private Transition createTransition(KrollDict props, String key)
	{
		if (LOLLIPOP_OR_GREATER) {
			Transition t = null;
			final int transitionType = props.getInt(key);
			switch (transitionType) {
				case TiUIView.TRANSITION_EXPLODE:
					t = new Explode();
					break;

				case TiUIView.TRANSITION_FADE_IN:
					t = new Fade(Fade.IN);
					break;

				case TiUIView.TRANSITION_FADE_OUT:
					t = new Fade(Fade.OUT);
					break;

				case TiUIView.TRANSITION_SLIDE_TOP:
					t = new Slide(Gravity.TOP);
					break;

				case TiUIView.TRANSITION_SLIDE_RIGHT:
					t = new Slide(Gravity.RIGHT);
					break;

				case TiUIView.TRANSITION_SLIDE_BOTTOM:
					t = new Slide(Gravity.BOTTOM);
					break;

				case TiUIView.TRANSITION_SLIDE_LEFT:
					t = new Slide(Gravity.LEFT);
					break;

				case TiUIView.TRANSITION_CHANGE_BOUNDS:
					t = new ChangeBounds();
					break;

				case TiUIView.TRANSITION_CHANGE_CLIP_BOUNDS:
					t = new ChangeClipBounds();
					break;

				case TiUIView.TRANSITION_CHANGE_TRANSFORM:
					t = new ChangeTransform();
					break;

				case TiUIView.TRANSITION_CHANGE_IMAGE_TRANSFORM:
					t = new ChangeImageTransform();
					break;

				default:
					break;
			}
			return t;
		} else {
			return null;
		}
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.Window";
	}
}
