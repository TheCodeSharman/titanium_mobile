/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIAbstractTabGroup;
import ti.modules.titanium.ui.widget.TiUIActionBarTabGroup;
import ti.modules.titanium.ui.widget.TiUITabHostGroup;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_TABS_BACKGROUND_COLOR,
	TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR	
})

public class TabGroupProxy extends TiWindowProxy
{
	private static final String LCAT = "TabGroupProxy";
	private static boolean DBG = TiConfig.LOGD;

	private static final int MSG_FIRST_ID = TiWindowProxy.MSG_LAST_ID + 1;

	private static final int MSG_ADD_TAB = MSG_FIRST_ID + 100;
	private static final int MSG_REMOVE_TAB = MSG_FIRST_ID + 101;
	private static final int MSG_FINISH_OPEN = MSG_FIRST_ID + 102;
	private static final int MSG_SET_ACTIVE_TAB = MSG_FIRST_ID + 103;
	private static final int MSG_GET_ACTIVE_TAB = MSG_FIRST_ID + 104;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	private ArrayList<TabProxy> tabs = new ArrayList<TabProxy>();
	String windowId;
	TabProxy initialActiveTab;

	public TabGroupProxy()
	{
		super();
	}

	public TabGroupProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		// If the device is running Honeycomb or later use the
		// Action Bar navigation tab group implementation.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			return new TiUIActionBarTabGroup(this);

		} else {
			// TODO(josh): remove the "activity" argument.
			return new TiUITabHostGroup(this, null);
		}
	}

	public String getTabsBackgroundColor() {
		if (hasProperty(TiC.PROPERTY_TABS_BACKGROUND_COLOR)) {
			return getProperty(TiC.PROPERTY_TABS_BACKGROUND_COLOR).toString();
		} else {
			return null;
		}
	}
	
	public String getTabsBackgroundSelectedColor() {
		if (hasProperty(TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR)) {
			return getProperty(TiC.PROPERTY_TABS_BACKGROUND_SELECTED_COLOR).toString();
		} else {
			return null;
		}
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_ADD_TAB : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleAddTab((TabProxy) result.getArg());
				result.setResult(null); // signal added
				return true;
			}
			case MSG_REMOVE_TAB : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleRemoveTab((TabProxy) result.getArg());
				result.setResult(null); // signal added
				return true;
			}
			case MSG_SET_ACTIVE_TAB: {
				AsyncResult result = (AsyncResult) msg.obj;
				handleSetActiveTab((TabProxy) result.getArg());
				return true;
			}
			case MSG_GET_ACTIVE_TAB: {
				AsyncResult result = (AsyncResult) msg.obj;
				result.setResult(handleGetActiveTab());
				return true;
			}
			default : {
				return super.handleMessage(msg);
			}
		}
	}

	@Kroll.getProperty @Kroll.method
	public TabProxy[] getTabs()
	{
		TabProxy[] tps = null;

		if (tabs != null) {
			tps = tabs.toArray(new TabProxy[tabs.size()]);
		}

		return tps;
	}


	public ArrayList<TabProxy> getTabList()
	{
		return tabs;
	}


	@Kroll.method
	public void addTab(TabProxy tab)
	{
		if (TiApplication.isUIThread()) {
			handleAddTab(tab);

			return;
		}

		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ADD_TAB), tab);
	}

	private void handleAddTab(TabProxy tab)
	{
		String tag = TiConvert.toString(tab.getProperty(TiC.PROPERTY_TAG) );
		if (tag == null) {
			//since tag is used to create tabSpec, it must be unique, otherwise tabs with same tag will use same activity (Timob-7487)
			tag = tab.toString();
			tab.setProperty(TiC.PROPERTY_TAG, tag);
		}

		if (tabs.size() == 0) {
			initialActiveTab = tab;
		}
		tabs.add(tab);

		TiUIActionBarTabGroup tabGroup = (TiUIActionBarTabGroup) getOrCreateView();
		if (tabGroup != null) {
			tabGroup.addTab(tab);
		}
	}

	@Kroll.method
	public void removeTab(TabProxy tab) { }
	public void handleRemoveTab(TabProxy tab) { }

	@Kroll.setProperty @Kroll.method
	public void setActiveTab(Object tabOrIndex)
	{
		TabProxy tab;
		if (tabOrIndex instanceof Number) {
			int tabIndex = ((Number) tabOrIndex).intValue();
			tab = tabs.get(tabIndex);

		} else if (tabOrIndex instanceof TabProxy) {
			tab = (TabProxy) tabOrIndex;

		} else {
			// TODO(josh): verify this is converted to a JS exception, otherwise just log an error.
			throw new IllegalArgumentException("First argument must be a tab object or a numeric index.");
		}

		if (TiApplication.isUIThread()) {
			handleSetActiveTab(tab);

		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ACTIVE_TAB,  tab));
		}
	}

	protected void handleSetActiveTab(TabProxy tab)
	{
		TiUIAbstractTabGroup tabGroup = (TiUIAbstractTabGroup) view;
		if (tabGroup != null) {
			tabGroup.selectTab(tab);

		} else {
			// handles the case where the setActiveTab is called before the TabGroup has finished opening
			// and thus would prevent the initial tab from being set
			initialActiveTab = tab;
		}
	}

	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);

		// Support setting orientation modes at creation.
		Object orientationModes = options.get(TiC.PROPERTY_ORIENTATION_MODES);
		if (orientationModes != null && orientationModes instanceof Object[]) {
			try {
				int[] modes = TiConvert.toIntArray((Object[]) orientationModes);
				setOrientationModes(modes);

			} catch (ClassCastException e) {
				Log.e(LCAT, "Invalid orientationMode array. Must only contain orientation mode constants.");
			}
		}

		if (options.containsKey(TiC.PROPERTY_ACTIVE_TAB)) {
			setActiveTab(options.get(TiC.PROPERTY_ACTIVE_TAB));
		}
	}

	@Kroll.getProperty @Kroll.method
	public TabProxy getActiveTab() {
		if (TiApplication.isUIThread()) {
			return handleGetActiveTab();

		} else {
			return (TabProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_ACTIVE_TAB,  tab));
		}
	}

	private TabProxy handleGetActiveTab() {
		TiUIAbstractTabGroup tabGroup = (TiUIAbstractTabGroup) view;
		if (tabGroup != null) {
			return tabGroup.getSelectedTab();

		} else {
			return initialActiveTab;
		}
	}

	@Override
	protected void handleOpen(KrollDict options)
	{
		if (DBG) {
			Log.d(LCAT, "handleOpen: " + options);
		}

		Activity activity = getActivity();
		Intent intent = new Intent(activity, TiTabActivity.class);
		fillIntent(activity, intent);
		activity.startActivity(intent);
	}

	public void handlePostOpen(Activity activity)
	{
		((TiTabActivity)activity).setTabGroupProxy(this);

		// TODO(josh): move this logic into TiUITabHostGroup
		//this.weakActivity = new WeakReference<TiTabActivity>( (TiTabActivity) activity );

		TiUITabHostGroup tg = (TiUITabHostGroup) view;
		if (tabs != null) {
			for(TabProxy tab : tabs) {
				tg.addTab(tab);
			}
		}

		// Setup the new tab activity like setting orientation modes.
		onWindowActivityCreated();
		
		tg.changeActiveTab(initialActiveTab);
		// Make sure the tab indicator is selected. We need to force it to be selected due to TIMOB-7832.
		tg.setTabIndicatorSelected(initialActiveTab);

		opened = true;
		super.handlePostOpen();
		fireEvent(TiC.EVENT_OPEN, null);

		// open event's for a tab window should be fired as part of the windowCreated callback that 
		// is set as part of the fillIntentForTab call
	}

	@Override
	protected void handleClose(KrollDict options)
	{
		if (DBG) {
			Log.d(LCAT, "handleClose: " + options);
		}
		
		modelListener = null;
		releaseViews();
		windowId = null;
		view = null;

		opened = false;
	}


	public KrollDict buildFocusEvent(int toIndex, int fromIndex)
	{
		KrollDict e = new KrollDict();

		e.put(TiC.EVENT_PROPERTY_INDEX, toIndex);
		e.put(TiC.EVENT_PROPERTY_PREVIOUS_INDEX, fromIndex);

		if (fromIndex != -1) {
			e.put(TiC.EVENT_PROPERTY_PREVIOUS_TAB, tabs.get(fromIndex));
		} else {
			KrollDict fakeTab = new KrollDict();
			fakeTab.put(TiC.PROPERTY_TITLE, "no tab");
			e.put(TiC.EVENT_PROPERTY_PREVIOUS_TAB, fakeTab);
		}

		if (toIndex != -1) {
			e.put(TiC.EVENT_PROPERTY_TAB, tabs.get(toIndex));
		}

		return e;
	}

	private void fillIntent(Activity activity, Intent intent)
	{
		if (hasProperty(TiC.PROPERTY_FULLSCREEN)) {
			intent.putExtra(TiC.PROPERTY_FULLSCREEN, TiConvert.toBoolean(getProperty(TiC.PROPERTY_FULLSCREEN)));
		}
		if (hasProperty(TiC.PROPERTY_NAV_BAR_HIDDEN)) {
			intent.putExtra(TiC.PROPERTY_NAV_BAR_HIDDEN, TiConvert.toBoolean(getProperty(TiC.PROPERTY_NAV_BAR_HIDDEN)));
		}
		if (hasProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE)) {
			intent.putExtra(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE, TiConvert.toInt(getProperty(TiC.PROPERTY_WINDOW_SOFT_INPUT_MODE)));
		}

		if (hasProperty(TiC.PROPERTY_EXIT_ON_CLOSE)) {
			intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT, TiConvert.toBoolean(getProperty(TiC.PROPERTY_EXIT_ON_CLOSE)));
		} else {
			intent.putExtra(TiC.INTENT_PROPERTY_FINISH_ROOT, activity.isTaskRoot());
		}

		Handler handler = new Handler(TiMessenger.getMainMessenger().getLooper(), new MessageHandler(this));
		Messenger messenger = new Messenger(handler);
		//Messenger messenger = new Messenger(getUIHandler());
		intent.putExtra(TiC.INTENT_PROPERTY_MESSENGER, messenger);
		intent.putExtra(TiC.INTENT_PROPERTY_MSG_ID, MSG_FINISH_OPEN);
	}

	private static class MessageHandler implements Handler.Callback
	{

		private WeakReference<TabGroupProxy> tabGroupProxy;
		
		public MessageHandler(TabGroupProxy proxy) 
		{
			this.tabGroupProxy = new WeakReference<TabGroupProxy>(proxy);
		}
		
		@Override
		public boolean handleMessage(Message msg) 
		{
			TabGroupProxy tabGroupProxy = this.tabGroupProxy.get();
			if (tabGroupProxy == null) {
				return false;
			}
			
			switch(msg.what) {
				case MSG_FINISH_OPEN:
					TiTabActivity activity = (TiTabActivity) msg.obj;
					tabGroupProxy.view = new TiUITabHostGroup(tabGroupProxy, activity);
					tabGroupProxy.modelListener = tabGroupProxy.view;
					tabGroupProxy.handlePostOpen(activity);
					return true;
			}
			return false;
		}
		
	}
	@Override
	public KrollDict handleToImage()
	{
		// TODO we need to expose properties again as a KrollDict?
		return TiUIHelper.viewToImage(new KrollDict(), getActivity().getWindow().getDecorView());
	}

	@Override
	public void releaseViews()
	{
		super.releaseViews();
		if (tabs != null) {
			synchronized (tabs) {
				for (TabProxy t : tabs) {
					t.setTabGroup(null);
					t.releaseViews();
				}
			}
		}
		// Don't clear the tabs collection -- it contains proxies, not views
		//tabs.clear();
	}

	@Override
	protected Activity getWindowActivity()
	{
		// TODO(josh): do we need to return the tab group activity here?
		return null;
	}

	@Kroll.method @Kroll.setProperty
	@Override
	public void setOrientationModes(int[] modes) {
		// Unlike Windows this setter is not defined in JavaScript.
		// We need to expose it here with an annotation.
		super.setOrientationModes(modes);
	}
}
