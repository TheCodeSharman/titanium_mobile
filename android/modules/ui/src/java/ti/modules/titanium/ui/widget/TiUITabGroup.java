/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiBaseWindowProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.TabGroupProxy;
import ti.modules.titanium.ui.TabProxy;
import ti.modules.titanium.ui.TiTabActivity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;

public class TiUITabGroup extends TiUIView
	implements OnTabChangeListener
{
	private static final String LCAT = "TiUITabGroup";
	private static final boolean DBG = TiConfig.LOGD;
	private static final String TAB = "tab";
	private static final String TAB_GROUP = "tabGroup";

	private TabHost tabHost;

	private int previousTabID = -1;
	private int currentTabID = 0;
	
	private HashMap<Integer, ArrayList<String>> tabBackgroundColors;
	private HashMap<Integer, ArrayList<String>> tabBackgroundSelectedColors;
	private Drawable defaultDrawable;
	private Drawable defaultSelectedDrawable;
	private boolean cacheDefaults = true;


	public TiUITabGroup(TiViewProxy proxy, TiTabActivity activity)
	{
		super(proxy);
		tabHost = activity.getTabHost();
		// Set to GONE to overcome a NullPointerException
		// deep in Android code in pre api 8.  See Android issue
		// 2772.
		tabHost.setVisibility(View.GONE);
		tabHost.clearAllTabs();
		tabHost.setOnTabChangedListener(this);
		tabHost.setup(activity.getLocalActivityManager());

		Object bgColor = proxy.getProperty(TiC.PROPERTY_BACKGROUND_COLOR);
		if (bgColor != null) {
			tabHost.setBackgroundColor(TiConvert.toColor(bgColor.toString()));
		} else {
			tabHost.setBackgroundDrawable(new ColorDrawable(TiConvert.toColor("#ff1a1a1a")));
		}

		setNativeView(tabHost);
		
		tabBackgroundColors = new HashMap<Integer, ArrayList<String>>();
		tabBackgroundSelectedColors = new HashMap<Integer, ArrayList<String>>();
	}

	public TabSpec newTab(String id)
	{
		return tabHost.newTabSpec(id);
	}

	protected void registerTouchForTabGroup(final View touchable, final TabProxy tabProxy)
	{
		if (touchable == null) {
			return;
		}
		
		registerTouchEvents(touchable);

		final int tabCount = tabHost.getTabWidget().getTabCount();
		
		boolean clickable = touchable.isClickable();
		if (!clickable) {
			touchable.setOnClickListener(null); // This will set clickable to true in the view, so make sure it stays here so the next line turns it off.
			touchable.setClickable(false);
			touchable.setOnLongClickListener(null);
			touchable.setLongClickable(false);
		} else if ( ! (touchable instanceof AdapterView) ) {
			// n.b.: AdapterView throws if click listener set.
			// n.b.: setting onclicklistener automatically sets clickable to true.
			touchable.setOnClickListener(new OnClickListener()
			{
				public void onClick(View v)
				{
					// We have to set the current tab here to restore the widget's default behavior since
					// setOnClickListener seems to overwrite it
					tabHost.setCurrentTab(tabCount - 1);
					tabProxy.fireEvent(TiC.EVENT_CLICK, null);
				}
			});
			setOnLongClickListener(touchable);
		}
	}
	
	public void addTab(TabSpec tab, final TabProxy tabProxy)
	{
		tabHost.addTab(tab);
		if (tabHost.getVisibility() == View.GONE) {
			boolean visibilityPerProxy = true; // default
			if (proxy.hasProperty(TiC.PROPERTY_VISIBLE)) {
				visibilityPerProxy = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_VISIBLE));
			}
			if (visibilityPerProxy) {
				tabHost.setVisibility(View.VISIBLE);
			} else {
				tabHost.setVisibility(View.INVISIBLE);
			}
		}
		final int tabCount = tabHost.getTabWidget().getTabCount();
		if (tabCount > 0) {
			int currentTabIndex = tabCount - 1;

			tabHost.getTabWidget().getChildTabViewAt(currentTabIndex).setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// We have to set the current tab here to restore the widget's default behavior since
					// setOnClickListener seems to overwrite it
					tabHost.setCurrentTab(tabCount - 1);
					tabProxy.fireEvent(TiC.EVENT_CLICK, null);
				}
			});

			// Store off the background/selectedBackground color when the tab is created.
			if (tabProxy.hasProperty(TiC.PROPERTY_BACKGROUND_COLOR)) {
				addTabsBackgroundColor(currentTabIndex, TAB, tabProxy.getProperty(TiC.PROPERTY_BACKGROUND_COLOR).toString());
			} else if (proxy.hasProperty(TiC.PROPERTY_TABS_BACKGROUND_COLOR)) {
				addTabsBackgroundColor(currentTabIndex, TAB_GROUP, proxy.getProperty(TiC.PROPERTY_TABS_BACKGROUND_COLOR)
					.toString());
			}

			if (tabProxy.hasProperty(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR)) {
				addTabsBackgroundSelectedColor(currentTabIndex, TAB,
					tabProxy.getProperty(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR).toString());
			} else if (proxy.hasProperty(TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR)) {
				addTabsBackgroundSelectedColor(currentTabIndex, TAB_GROUP,
					proxy.getProperty(TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR).toString());
			}

			// Don't overwrite the background color if it's the first tab. If it's the first tab being added,
			// onTabChanged will be fired and we set the background color there
			if (tabBackgroundColors.containsKey(currentTabIndex) && tabCount != 1) {
				tabHost.getTabWidget().getChildAt(currentTabIndex)
					.setBackgroundColor(TiConvert.toColor(tabBackgroundColors.get(currentTabIndex).get(1)));
			} else if (tabBackgroundSelectedColors.containsKey(currentTabIndex) && tabCount == 1) {
				tabHost.getTabWidget().getChildAt(currentTabIndex)
					.setBackgroundColor(TiConvert.toColor(tabBackgroundSelectedColors.get(currentTabIndex).get(1)));
			}

			
			registerTouchForTabGroup(tabHost.getTabWidget().getChildTabViewAt(tabCount - 1), tabProxy);
		}
	}

	public void setActiveTab(int index)
	{
		if (tabHost != null) {
			tabHost.setCurrentTab(index);
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus)
	{
		// ignore focus change for tab group.
		// we can simply fire focus/blur from onTabChanged (to avoid chicken/egg event problems)
	}

	@SuppressWarnings("unused")
	private TiViewProxy getTabWindow(TabProxy tab)
	{
		TiViewProxy viewProxy = tab.getWindow();
		if (viewProxy instanceof TiBaseWindowProxy) {
			TiViewProxy wrappedViewProxy = ((TiBaseWindowProxy) viewProxy).getWrappedView();
			if (wrappedViewProxy != null) {
				viewProxy = wrappedViewProxy;
			}
		}

		return viewProxy;
	}

	@Override
	public void onTabChanged(String id)
	{
		TabGroupProxy tabGroupProxy = ((TabGroupProxy) proxy);

		currentTabID = tabHost.getCurrentTab();
		
		// This is the first place we can cache the background info before it gets changed by some of our logic. The
		// first addTab() call from android triggers onTabChanged(), so this is the best place to cache the default
		// drawables.
		if (cacheDefaults) {
			defaultSelectedDrawable = tabHost.getTabWidget().getChildAt(currentTabID).getBackground();
			defaultDrawable = tabHost.getBackground();
			cacheDefaults = false;
		}

		if (DBG) {
			Log.d(LCAT,"Tab change from " + previousTabID + " to " + currentTabID);
		}

		ArrayList<TabProxy> tabs = tabGroupProxy.getTabList();
		TabProxy prevTab = (previousTabID >= 0 ? tabs.get(previousTabID) : null);
		TabProxy currentTab = tabs.get(currentTabID);

		proxy.setProperty(TiC.PROPERTY_ACTIVE_TAB, currentTab);

		// Apply the appropriate background color on all tabs
		for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
			if (tabBackgroundColors.containsKey(i)) {
				tabHost.getTabWidget().getChildAt(i)
				.setBackgroundColor(TiConvert.toColor(tabBackgroundColors.get(i).get(1)));
			} else {
				tabHost.getTabWidget().getChildAt(i).setBackgroundDrawable(defaultDrawable);
			}
		}

		// If we have tabsBackgroundSelectedColor set, apply that color to the current tab
		if (tabBackgroundSelectedColors.containsKey(currentTabID)) {
			tabHost.getTabWidget().getChildAt(currentTabID)
			.setBackgroundColor(TiConvert.toColor(tabBackgroundSelectedColors.get(currentTabID).get(1)));
		} else {
			tabHost.getTabWidget().getChildAt(currentTabID).setBackgroundDrawable(defaultSelectedDrawable);
		}
		
		KrollDict tabChangeEventData = tabGroupProxy.buildFocusEvent(currentTabID, previousTabID);
		if (prevTab != null) {
			// Create a clone of the event data since the 'source' needs to be
			// correctly set for the proxy firing the event.
			prevTab.fireEvent(TiC.EVENT_BLUR, tabChangeEventData.clone(), true);
		}
		currentTab.fireEvent(TiC.EVENT_FOCUS, tabChangeEventData, true);

		previousTabID = currentTabID;
	}
	
	public void setTabIndicatorSelected(Object t)
	{
		ArrayList<TabProxy> tabList = ((TabGroupProxy)proxy).getTabList();
		
		if (t != null && tabList != null) {	
			int index = -1;
			int len = tabList.size();
			
			if (t instanceof Number) {
				index = TiConvert.toInt(t);
				if (index >= len) {
					return;
				}
			} else if (t instanceof TabProxy) {
				TabProxy tab = (TabProxy) t;
				for (int i=0; i<len; i++) {
					if (tabList.get(i) == tab) {
						index = i;
						break;
					}
				}
			} else {
				Log.w(LCAT, "Attempt to set tab indicator using a non-supported argument. Ignoring");
				return;
			}
			
			if (index >= 0) {
				View tabIndicator = tabHost.getTabWidget().getChildTabViewAt(index);
				if (!tabIndicator.isSelected()) {
					tabIndicator.setSelected(true);
				}
			}
		}
	}
	
	private void addTabsBackgroundColor(int index, String type, String value)
	{
		ArrayList<String> arrayList = new ArrayList<String>();
		arrayList.add(type);
		arrayList.add(value);
		tabBackgroundColors.put(index,arrayList);
	}
	
	private void addTabsBackgroundSelectedColor(int index, String type, String value)
	{
		ArrayList<String> arrayList = new ArrayList<String>();
		arrayList.add(type);
		arrayList.add(value);
		tabBackgroundSelectedColors.put(index,arrayList);
	}
	
	private void updateBackgroundValues(String color, HashMap<Integer, ArrayList<String>> backgroundValues)
	{
		int currentTabIndex = tabHost.getCurrentTab();

		// loop through and check if it's in hashmap. If it isn't (signifying that it has a default
		// color), then update it with the new color. If the value is in hashmap, then we want to see if
		// it's set to the correct color
		for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
			if (backgroundValues.containsKey(i)) {
				// if the old value in tabBackgroundColors is derived from the old tab group value, we overwrite it with
				// the new value. Otherwise, it is derived from the tab itself, so we leave it alone
				if (backgroundValues.get(i).get(0).equals(TAB_GROUP)) {
					backgroundValues.get(i).set(1, color);
				}
			} else {
				// We have the default color, so we update it with the new color
				addTabsBackgroundColor(i, TAB_GROUP, color);
			}
			tabHost.getTabWidget().getChildAt(i).setBackgroundColor(TiConvert.toColor(tabBackgroundColors.get(i).get(1)));
		}

		// Set the backgroundSelectedColor for the current tab
		if (tabBackgroundSelectedColors.containsKey(currentTabIndex)) {
			tabHost.getTabWidget().getChildAt(currentTabIndex)
				.setBackgroundColor(TiConvert.toColor(tabBackgroundSelectedColors.get(currentTabIndex).get(1)));
		} else {
			tabHost.getTabWidget().getChildAt(currentTabIndex).setBackgroundDrawable(defaultSelectedDrawable);
		}
	}

	public void setTabBackgroundColor(Object colorValue){
		updateBackgroundValues((String) colorValue, tabBackgroundColors);
	}

	public void setTabBackgroundSelectedColor(Object colorValue){
		updateBackgroundValues((String) colorValue, tabBackgroundSelectedColors);
	}
	
	public void changeActiveTab(Object t)
	{
		if (t != null) {
			Integer index = null;
			if (t instanceof Number) {
				index = TiConvert.toInt(t);

				int len = tabHost.getTabWidget().getTabCount();
				if (index >= len) {
					// TODO consider throwing an exception to JS.
					Log.w(LCAT, "Index out of bounds. Attempt to set active tab to " + index + ". There are " + len + " tabs.");
					index = null;
				} else {
					tabHost.setCurrentTab(index);
				}
			} else if (t instanceof TabProxy) {
				TabProxy tab = (TabProxy) t;
				String tag = TiConvert.toString(tab.getProperty("tag"));
				if (tag != null) {
					tabHost.setCurrentTabByTag(tag);
				}
			} else {
				Log.w(LCAT, "Attempt to set active tab using a non-supported argument. Ignoring");
			}
		}
	}

	public int getActiveTab()
	{
		if(tabHost != null) {
			return tabHost.getCurrentTab();
		} else {
			return -1;
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if ("activeTab".equals(key)) {
			changeActiveTab(newValue);
		} else if (TiC.PROPERTY_TABS_BACKGROUND_COLOR.equals(key)) {
			setTabBackgroundColor(newValue);
		} else if (TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR.equals(key)) {
			setTabBackgroundSelectedColor(newValue);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}
}
