/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll.runtime.v8;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public final class V8Runtime implements Handler.Callback
{
	private static final String TAG = "V8Runtime";
	private static final String DEVICE_LIB = "kroll-v8-device";
	private static final String EMULATOR_LIB = "kroll-v8-emulator";

	private static final int MSG_PROCESS_DEBUG_MESSAGES = 1000;
	private static final int MSG_NATIVE_RELEASE = 1001;

	private static V8Runtime _instance;

	private long mainThreadId;
	private Handler mainHandler;

	private V8Runtime(Looper looper)
	{
		boolean useGlobalRefs = true;
		String libName = DEVICE_LIB;
		if (Build.PRODUCT.equals("sdk") || Build.PRODUCT.equals("google_sdk")) {
			Log.i(TAG, "Loading emulator version of kroll-v8");
			libName = EMULATOR_LIB;
			useGlobalRefs = false;
		}

		System.loadLibrary(libName);
		_instance = this;

		Looper mainLooper = Looper.getMainLooper();
		mainThreadId = mainLooper.getThread().getId();
		mainHandler = new Handler(mainLooper, this);
		nativeInit(useGlobalRefs);
	}

	public static void init(Looper v8Looper)
	{
		if (_instance == null) {
			new V8Runtime(v8Looper);
		}
	}

	public static V8Runtime getInstance()
	{
		return _instance;
	}

	public boolean isUiThread()
	{
		return Thread.currentThread().getId() == mainThreadId;
	}

	public void dispose()
	{
		nativeDispose();
	}

	public void release(ManagedV8Reference ref)
	{
		if (isUiThread()) {
			ref.release();
		} else {
			Message msg = mainHandler.obtainMessage(MSG_NATIVE_RELEASE, ref);
			msg.sendToTarget();
		}
	}

	public void setProperty(final V8Object object, final String property, final Object value)
	{
		if (isUiThread()) {
			object.doSetProperty(property, value);
		} else {
			mainHandler.post(new Runnable() {
				public void run()
				{
					object.doSetProperty(property, value);
				}
			});
		}
	}

	public void runModule(String source, String filename)
	{
		nativeRunModule(source, filename);
	}

	protected void dispatchDebugMessages()
	{
		Message msg = mainHandler.obtainMessage(MSG_PROCESS_DEBUG_MESSAGES);
		msg.sendToTarget();
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_PROCESS_DEBUG_MESSAGES:
				nativeProcessDebugMessages();
				return true;
			case MSG_NATIVE_RELEASE:
				ManagedV8Reference ref = (ManagedV8Reference) msg.obj;
				ref.release();
				return true;
		}
		return false;
	}

	private native void nativeInit(boolean useGlobalRefs);
	private native void nativeRunModule(String source, String filename);
	private native void nativeProcessDebugMessages();
	private native void nativeDispose();
}
