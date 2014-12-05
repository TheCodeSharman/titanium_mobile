/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.android;

import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.TiUIProgressIndicator;
import ti.modules.titanium.ui.widget.webview.TiUIWebView;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.WebSettings;

@Kroll.module(parentModule=UIModule.class)
@Kroll.dynamicApis(properties = {
	"currentActivity"
})
public class AndroidModule extends KrollModule
{
	private static final String TAG = "UIAndroidModule";
	
	@Kroll.constant public static final int PIXEL_FORMAT_A_8 = PixelFormat.A_8;
	@Kroll.constant public static final int PIXEL_FORMAT_LA_88 = PixelFormat.LA_88;
	@Kroll.constant public static final int PIXEL_FORMAT_L_8 = PixelFormat.L_8;
	@Kroll.constant public static final int PIXEL_FORMAT_OPAQUE = PixelFormat.OPAQUE;
	@Kroll.constant public static final int PIXEL_FORMAT_RGBA_4444 = PixelFormat.RGBA_4444;
	@Kroll.constant public static final int PIXEL_FORMAT_RGBA_5551 = PixelFormat.RGBA_5551;
	@Kroll.constant public static final int PIXEL_FORMAT_RGBA_8888 = PixelFormat.RGBA_8888;
	@Kroll.constant public static final int PIXEL_FORMAT_RGBX_8888 = PixelFormat.RGBX_8888;
	@Kroll.constant public static final int PIXEL_FORMAT_RGB_332 = PixelFormat.RGB_332;
	@Kroll.constant public static final int PIXEL_FORMAT_RGB_565 = PixelFormat.RGB_565;
	@Kroll.constant public static final int PIXEL_FORMAT_RGB_888 = PixelFormat.RGB_888;
	@Kroll.constant public static final int PIXEL_FORMAT_TRANSLUCENT = PixelFormat.TRANSLUCENT;
	@Kroll.constant public static final int PIXEL_FORMAT_TRANSPARENT = PixelFormat.TRANSPARENT;
	@Kroll.constant public static final int PIXEL_FORMAT_UNKNOWN = PixelFormat.UNKNOWN;
	
	@Kroll.constant public static final int SOFT_INPUT_ADJUST_PAN = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN;
	@Kroll.constant public static final int SOFT_INPUT_ADJUST_RESIZE = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
	@Kroll.constant public static final int SOFT_INPUT_ADJUST_UNSPECIFIED = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED;
	
	@Kroll.constant public static final int SOFT_INPUT_STATE_ALWAYS_HIDDEN = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN;
	@Kroll.constant public static final int SOFT_INPUT_STATE_ALWAYS_VISIBLE = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE;
	@Kroll.constant public static final int SOFT_INPUT_STATE_HIDDEN = WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN;
	@Kroll.constant public static final int SOFT_INPUT_STATE_UNSPECIFIED = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED;
	@Kroll.constant public static final int SOFT_INPUT_STATE_VISIBLE = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
	
	@Kroll.constant public static final int SOFT_KEYBOARD_DEFAULT_ON_FOCUS = TiUIView.SOFT_KEYBOARD_DEFAULT_ON_FOCUS;
	@Kroll.constant public static final int SOFT_KEYBOARD_HIDE_ON_FOCUS = TiUIView.SOFT_KEYBOARD_HIDE_ON_FOCUS;
	@Kroll.constant public static final int SOFT_KEYBOARD_SHOW_ON_FOCUS = TiUIView.SOFT_KEYBOARD_SHOW_ON_FOCUS;
	
	@Kroll.constant public static final int LINKIFY_ALL = Linkify.ALL;
	@Kroll.constant public static final int LINKIFY_EMAIL_ADDRESSES = Linkify.EMAIL_ADDRESSES;
	@Kroll.constant public static final int LINKIFY_MAP_ADDRESSES = Linkify.MAP_ADDRESSES;
	@Kroll.constant public static final int LINKIFY_PHONE_NUMBERS = Linkify.PHONE_NUMBERS;
	@Kroll.constant public static final int LINKIFY_WEB_URLS = Linkify.WEB_URLS;
	
	@Kroll.constant public static final int SWITCH_STYLE_CHECKBOX     = 0;
	@Kroll.constant public static final int SWITCH_STYLE_TOGGLEBUTTON = 1;
	
	@Kroll.constant public static final int WEBVIEW_PLUGINS_OFF = TiUIWebView.PLUGIN_STATE_OFF;
	@Kroll.constant public static final int WEBVIEW_PLUGINS_ON = TiUIWebView.PLUGIN_STATE_ON;
	@Kroll.constant public static final int WEBVIEW_PLUGINS_ON_DEMAND = TiUIWebView.PLUGIN_STATE_ON_DEMAND;
	
	@Kroll.constant public static final int WEBVIEW_LOAD_DEFAULT = WebSettings.LOAD_DEFAULT;
	@Kroll.constant public static final int WEBVIEW_LOAD_NO_CACHE = WebSettings.LOAD_NO_CACHE;
	@Kroll.constant public static final int WEBVIEW_LOAD_CACHE_ONLY = WebSettings.LOAD_CACHE_ONLY;
	@Kroll.constant public static final int WEBVIEW_LOAD_CACHE_ELSE_NETWORK = WebSettings.LOAD_CACHE_ELSE_NETWORK;

	@Kroll.constant public static final int GRAVITY_AXIS_CLIP = Gravity.AXIS_CLIP;
	@Kroll.constant public static final int GRAVITY_AXIS_PULL_AFTER = Gravity.AXIS_PULL_AFTER;
	@Kroll.constant public static final int GRAVITY_AXIS_PULL_BEFORE = Gravity.AXIS_PULL_BEFORE;
	@Kroll.constant public static final int GRAVITY_AXIS_SPECIFIED = Gravity.AXIS_SPECIFIED;
	@Kroll.constant public static final int GRAVITY_AXIS_X_SHIFT = Gravity.AXIS_X_SHIFT;
	@Kroll.constant public static final int GRAVITY_AXIS_Y_SHIFT = Gravity.AXIS_Y_SHIFT;
	@Kroll.constant public static final int GRAVITY_BOTTOM = Gravity.BOTTOM;
	@Kroll.constant public static final int GRAVITY_CENTER = Gravity.CENTER;
	@Kroll.constant public static final int GRAVITY_CENTER_HORIZONTAL = Gravity.CENTER_HORIZONTAL;
	@Kroll.constant public static final int GRAVITY_CENTER_VERTICAL = Gravity.CENTER_VERTICAL;
	@Kroll.constant public static final int GRAVITY_CLIP_HORIZONTAL = Gravity.CLIP_HORIZONTAL;
	@Kroll.constant public static final int GRAVITY_CLIP_VERTICAL = Gravity.CLIP_VERTICAL;
	@Kroll.constant public static final int GRAVITY_DISPLAY_CLIP_HORIZONTAL = Gravity.DISPLAY_CLIP_HORIZONTAL;
	@Kroll.constant public static final int GRAVITY_DISPLAY_CLIP_VERTICAL = Gravity.DISPLAY_CLIP_VERTICAL;
	@Kroll.constant public static final int GRAVITY_END = Gravity.END;
	@Kroll.constant public static final int GRAVITY_FILL = Gravity.FILL;
	@Kroll.constant public static final int GRAVITY_FILL_HORIZONTAL = Gravity.FILL_HORIZONTAL;
	@Kroll.constant public static final int GRAVITY_FILL_VERTICAL = Gravity.FILL_VERTICAL;
	@Kroll.constant public static final int GRAVITY_HORIZONTAL_GRAVITY_MASK = Gravity.HORIZONTAL_GRAVITY_MASK;
	@Kroll.constant public static final int GRAVITY_LEFT = Gravity.LEFT;
	@Kroll.constant public static final int GRAVITY_NO_GRAVITY = Gravity.NO_GRAVITY;
	@Kroll.constant public static final int GRAVITY_RELATIVE_HORIZONTAL_GRAVITY_MASK = Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK;
	@Kroll.constant public static final int GRAVITY_RELATIVE_LAYOUT_DIRECTION = Gravity.RELATIVE_LAYOUT_DIRECTION;
	@Kroll.constant public static final int GRAVITY_RIGHT = Gravity.RIGHT;
	@Kroll.constant public static final int GRAVITY_START = Gravity.START;
	@Kroll.constant public static final int GRAVITY_TOP = Gravity.TOP;
	@Kroll.constant public static final int GRAVITY_VERTICAL_GRAVITY_MASK = Gravity.VERTICAL_GRAVITY_MASK;



	@Kroll.constant public static final int PROGRESS_INDICATOR_STATUS_BAR = TiUIProgressIndicator.STATUS_BAR;
	@Kroll.constant public static final int PROGRESS_INDICATOR_DIALOG = TiUIProgressIndicator.DIALOG;
	@Kroll.constant public static final int PROGRESS_INDICATOR_INDETERMINANT = TiUIProgressIndicator.INDETERMINANT;
	@Kroll.constant public static final int PROGRESS_INDICATOR_DETERMINANT = TiUIProgressIndicator.DETERMINANT;
	
	@Kroll.constant public static final int OVER_SCROLL_ALWAYS = 0 ;               //android.view.View.OVER_SCROLL_ALWAYS;
	@Kroll.constant public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;    //android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS;
	@Kroll.constant public static final int OVER_SCROLL_NEVER = 2;                 //android.view.View.OVER_SCROLL_NEVER;

	public AndroidModule()
	{
		super();
	}

	public AndroidModule(TiContext tiContext) 
	{
		this();
	}

	// TODO - grab the activity off the invocation?
	@Kroll.method
	public void openPreferences(@Kroll.argument(optional=true) String prefsName)
	{
		Activity activity = TiApplication.getAppRootOrCurrentActivity();
		if (activity != null) {
			
			Intent i = new Intent(activity, TiPreferencesActivity.class);
			if (prefsName != null) {
				i.putExtra("prefsName", prefsName);
			}
			activity.startActivity(i);
		} else {
			Log.w(TAG, "Unable to open preferences. Activity is null", Log.DEBUG_MODE);
		}
	}

	// TODO Need to be revisited to hide keyboard based on specific view
	@Kroll.method
	public void hideSoftKeyboard()
	{
		getMainHandler().post(new Runnable() {

			@Override
			public void run() {
				Activity currentActivity = TiApplication.getAppCurrentActivity();
				if (currentActivity != null) {
					TiUIHelper.showSoftKeyboard(currentActivity.getWindow().getDecorView(), false);
				} else if (activity != null) {
					TiUIHelper.showSoftKeyboard(getActivity().getWindow().getDecorView(), false);
				} else {
					Log.w(TAG, "Unable to hide soft keyboard. Activity is null", Log.DEBUG_MODE);
				}
				
			}
			
		});
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.Android";
	}
}
