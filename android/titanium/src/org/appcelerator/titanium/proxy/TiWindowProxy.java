/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.AsyncResult;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiAnimation;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.os.Message;

@Kroll.proxy(propertyAccessors={"title"})
public abstract class TiWindowProxy extends TiViewProxy
{
	private static final String LCAT = "TiWindowProxy";
	private static final boolean DBG = TiConfig.LOGD;
	
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_OPEN = MSG_FIRST_ID + 100;
	private static final int MSG_CLOSE = MSG_FIRST_ID + 101;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	protected boolean opened;
	protected boolean focused;
	protected boolean fullscreen;
	protected boolean modal;
	protected boolean restoreFullscreen;
	protected int[] orientationModes = new int[0];
	
	protected TiViewProxy tabGroup;
	protected TiViewProxy tab;
	protected boolean inTab;

	public TiWindowProxy(TiContext tiContext)
	{
		super(tiContext);
		inTab = false;
	}

	@Override
	public TiUIView createView(Activity activity) {
		throw new IllegalStateException("Windows are created during open");
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_OPEN : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleOpen((KrollDict) result.getArg());
				result.setResult(null); // signal opened
				return true;
			}
			case MSG_CLOSE : {
				AsyncResult result = (AsyncResult) msg.obj;
				handleClose((KrollDict) result.getArg());
				result.setResult(null); // signal closed
				return true;
			}
			default : {
				return super.handleMessage(msg);
			}
		}
	}

	@Kroll.method
	public void open(@Kroll.argument(optional=true) Object arg)
	{
		if (opened) {
			return;
		}

		KrollDict options = null;
		TiAnimation animation = null;

		if (arg != null) {
			if (arg instanceof KrollDict) {
				options = (KrollDict) arg;
			} else if (arg instanceof TiAnimation) {
				options = new KrollDict();
				options.put("_anim", animation);
			}
		}

		if (getTiContext().isUIThread()) {
			handleOpen(options);
			return;
		}

		AsyncResult result = new AsyncResult(options);
		Message msg = getUIHandler().obtainMessage(MSG_OPEN, result);
		msg.sendToTarget();
		result.getResult(); // Don't care about result, just synchronizing.
	}

	@Kroll.method
	public void close(@Kroll.argument(optional=true) Object arg)
	{
		if (!opened) {
			return;
		}

		KrollDict options = null;
		TiAnimation animation = null;

		if (arg != null) {
			if (arg instanceof KrollDict) {
				options = (KrollDict) arg;
			} else if (arg instanceof TiAnimation) {
				options = new KrollDict();
				options.put("_anim", animation);
			}
		}

		if (getTiContext().isUIThread()) {
			handleClose(options);
			return;
		}

		AsyncResult result = new AsyncResult(options);
		Message msg = getUIHandler().obtainMessage(MSG_CLOSE, result);
		msg.sendToTarget();
		result.getResult(); // Don't care about result, just synchronizing.
	}

	public void closeFromActivity() {
		if (!opened) {
			return;
		}
		releaseViews();
		opened = false;
		TiContext context = getTiContext();
		if (creatingContext != null && context != null && !creatingContext.equals(context)) {
			switchToCreatingContext();
		}

	}
	
	public void setTabProxy(TiViewProxy tabProxy) {
		this.tab = tabProxy;
	}

	public TiViewProxy getTabProxy() {
		return this.tab;
	}

	public void setTabGroupProxy(TiViewProxy tabGroupProxy) {
		this.tabGroup = tabGroupProxy;
	}
	public TiViewProxy getTabGroupProxy() {
		return this.tabGroup;
	}

	@Kroll.method
	public void hideTabBar() {
		// iPhone only right now.
	}

	public KrollDict handleToImage() {
		return TiUIHelper.viewToImage(getTiContext(), getTiContext().getActivity().getWindow().getDecorView());
	}
	
	@Kroll.method @Kroll.setProperty
	public void setOrientationModes(int[] modes) {
		orientationModes = modes;
		
		Activity activity = handleGetActivity();
		if (activity instanceof TiBaseActivity) {
			TiBaseActivity tiActivity = (TiBaseActivity) activity;
			tiActivity.updateOrientation();
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public int[] getOrientationModes() {
		return orientationModes;
	}
	
	public boolean isOrientationMode(int orientation) {
		if (orientationModes.length == 0) return true;
		
		boolean allowOrientationChange = false;
		int currentMode = TiUIHelper.convertToTiOrientation(orientation);
		for (int mode : orientationModes) {
			if (mode == currentMode) {
				allowOrientationChange = true;
				break;
			}
		}
		return allowOrientationChange;
	}
	
	public ActivityProxy getActivity() {
		return (ActivityProxy) getProperty("activity");
	}
	
	public void setActivity(ActivityProxy activity) {
		setProperty("activity", activity);
	}
	
	protected abstract void handleOpen(KrollDict options);
	//public abstract void handlePostOpen(Activity activity);
	protected abstract void handleClose(KrollDict options);
	protected abstract Activity handleGetActivity();
}
