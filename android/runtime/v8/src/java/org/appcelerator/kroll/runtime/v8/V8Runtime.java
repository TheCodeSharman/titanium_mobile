/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll.runtime.v8;

import org.appcelerator.kroll.KrollProxySupport;
import org.appcelerator.kroll.KrollRuntime;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


public final class V8Runtime extends KrollRuntime implements Handler.Callback
{
	private static final String TAG = "KrollV8Runtime";
	private static final String NAME = "v8";
	private static final int MSG_PROCESS_DEBUG_MESSAGES = KrollRuntime.MSG_LAST_ID + 100;

	private boolean libLoaded = false;

	@Override
	public void initRuntime()
	{
		boolean useGlobalRefs = true;

		if (Build.PRODUCT.equals("sdk") || Build.PRODUCT.equals("google_sdk") || Build.FINGERPRINT.startsWith("generic")) {
			Log.i(TAG, "Emulator detected, storing global references in a global Map");
			useGlobalRefs = false;
		}

		boolean debuggerEnabled = getKrollApplication().isDebuggerEnabled();

		if (!libLoaded) {
			System.loadLibrary("kroll-v8");
			libLoaded = true;
		}

		nativeInit(useGlobalRefs, debuggerEnabled);
	}

	@Override
	public void doDispose()
	{
		nativeDispose();
	}

	@Override
	public void doRunModule(String source, String filename, KrollProxySupport activityProxy)
	{
		nativeRunModule(source, filename, activityProxy);
	}

	@Override
	public void initObject(KrollProxySupport proxy)
	{
		V8Object.nativeInitObject(proxy.getClass(), proxy);
	}

	@Override
	public boolean handleMessage(Message message)
	{
		switch (message.what) {
			case MSG_PROCESS_DEBUG_MESSAGES:
				nativeProcessDebugMessages();

				return true;
		}

		return super.handleMessage(message);
	}

	@Override
	public String getRuntimeName()
	{
		return NAME;
	}

	protected void dispatchDebugMessages()
	{
		handler.sendEmptyMessage(MSG_PROCESS_DEBUG_MESSAGES);
	}


	// JNI method prototypes
	private native void nativeInit(boolean useGlobalRefs, boolean debuggerActive);
	private native void nativeRunModule(String source, String filename, KrollProxySupport activityProxy);
	private native void nativeProcessDebugMessages();
	private native void nativeDispose();
}

