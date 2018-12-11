/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2016 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.tabgroup.TiUITab;
import android.app.Activity;
// clang-format off
@Kroll.proxy(creatableInModule = UIModule.class,
	propertyAccessors = {
		TiC.PROPERTY_TITLE,
		TiC.PROPERTY_TITLEID,
		TiC.PROPERTY_ICON
})
// clang-format on
public class TabProxy extends TiViewProxy
{
	@SuppressWarnings("unused")
	private static final String TAG = "TabProxy";

	private TabGroupProxy tabGroupProxy;
	private TiWindowProxy window;
	private boolean windowOpened = false;
	private int windowId;

	public TabProxy()
	{
		super();
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
		return null;
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
		Object window = options.get(TiC.PROPERTY_WINDOW);
		if (window instanceof TiWindowProxy) {
			setWindow((TiWindowProxy) window);
		}
	}

	// clang-format off
	@Kroll.method
	@Kroll.getProperty
	public boolean getActive()
	// clang-format on
	{
		if (tabGroupProxy != null) {
			return tabGroupProxy.getActiveTab() == this;
		}

		return false;
	}

	// clang-format off
	@Kroll.method
	@Kroll.setProperty
	public void setActive(boolean active)
	// clang-format on
	{
		if (tabGroupProxy != null) {
			tabGroupProxy.setActiveTab(this);
		}
	}

	@Kroll.method
	public void setWindow(TiWindowProxy window)
	{
		this.window = window;

		// don't call setProperty cause the property is already set on the JS
		// object and thus we don't need to cross back over the bridge, we just
		// need to set it on the internal properties map of the proxy
		properties.put(TiC.PROPERTY_WINDOW, window);

		if (window == null) {
			return;
		}

		this.window.setTabProxy(this);

		if (tabGroupProxy != null) {
			// Set window's tab group if this tab has been added to a group.
			this.window.setTabGroupProxy(tabGroupProxy);
		}

		//Send out a sync event to indicate window is added to tab
		this.window.fireSyncEvent(TiC.EVENT_ADDED_TO_TAB, null);
		// TODO: Deprecate old event
		this.window.fireSyncEvent("addedToTab", null);
	}

	public TiWindowProxy getWindow()
	{
		return this.window;
	}

	// clang-format off
	@Kroll.method
	@Kroll.getProperty
	public TabGroupProxy getTabGroup()
	// clang-format on
	{
		return this.tabGroupProxy;
	}

	public void setTabGroup(TabGroupProxy tabGroupProxy)
	{
		setParent(tabGroupProxy);
		this.tabGroupProxy = tabGroupProxy;

		if (window != null) {
			// If a window was set before the tab
			// was added to a group we need to initialize
			// the window's tab group reference.
			window.setTabGroupProxy(tabGroupProxy);
		}
	}

	public void setWindowId(int id)
	{
		windowId = id;
	}

	public int getWindowId()
	{
		return windowId;
	}

	@Override
	public void releaseViews()
	{
		super.releaseViews();
		if (window != null) {
			window.setTabProxy(null);
			window.setTabGroupProxy(null);
			window.releaseViews();
		}
	}

	public void releaseViewsForActivityForcedToDestroy()
	{
		super.releaseViews();
		if (window != null) {
			window.releaseViews();
		}
	}

	void onFocusChanged(boolean focused, KrollDict eventData)
	{
		// Windows are lazily opened when the tab is first focused.
		if (window != null && !windowOpened) {
			// Need to handle the url window in the JS side.
			window.callPropertySync(TiC.PROPERTY_LOAD_URL, null);
			windowOpened = true;
			window.fireEvent(TiC.EVENT_OPEN, null, false);
		}

		// The focus and blur events for tab changes propagate like so:
		//    window -> tab -> tab group
		//
		// The window is optional and will be skipped if it does not exist.
		String event = focused ? TiC.EVENT_FOCUS : TiC.EVENT_BLUR;

		if (window != null) {
			window.fireEvent(event, null, false);
		}
		fireEvent(event, eventData, true);
	}

	void close(boolean activityIsFinishing)
	{
		if (windowOpened && window != null) {
			windowOpened = false;
			KrollDict data = null;
			if (!activityIsFinishing) {
				data = new KrollDict();
				data.put("_closeFromActivityForcedToDestroy", true);
			}
			window.fireSyncEvent(TiC.EVENT_CLOSE, data);
		}
	}

	void onSelectionChanged(boolean selected)
	{
		if (!selected) {
			//When tab selection changes, we hide the soft keyboard.
			Activity currentActivity = TiApplication.getAppCurrentActivity();
			if (currentActivity != null) {
				TiUIHelper.showSoftKeyboard(currentActivity.getWindow().getDecorView(), false);
			}
		}

		((TiUITab) view).onSelectionChange(selected);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.Tab";
	}
}
