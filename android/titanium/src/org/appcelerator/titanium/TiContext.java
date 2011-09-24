/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.lang.ref.WeakReference;

import org.appcelerator.kroll.runtime.v8.V8Object;
import org.appcelerator.kroll.runtime.v8.V8Runtime;
import org.appcelerator.kroll.runtime.v8.V8Script;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.kroll.KrollContext;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUrl;
import org.appcelerator.titanium.util.TiWeakList;

import android.app.Activity;
import android.app.Service;
import android.content.ContextWrapper;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class TiContext
{
	private static final String LCAT = "TiContext";
	private static final boolean DBG = TiConfig.LOGD;
	@SuppressWarnings("unused")
	private static final boolean TRACE = TiConfig.LOGV;

	public static final int LIFECYCLE_ON_START = 0;
	public static final int LIFECYCLE_ON_RESUME = 1;
	public static final int LIFECYCLE_ON_PAUSE = 2;
	public static final int LIFECYCLE_ON_STOP = 3;
	public static final int LIFECYCLE_ON_DESTROY = 4;

	private long mainThreadId;

	private TiUrl baseUrl;
	private String currentUrl;
	private boolean launchContext;
	private boolean serviceContext; // Contexts created for Ti services won't have associated activities.

	private WeakReference<Activity> weakActivity;
	private TiApplication tiApp;
	private V8Object scope;

	private TiWeakList<OnLifecycleEvent> lifecycleListeners;
	private TiWeakList<OnServiceLifecycleEvent> serviceLifecycleListeners;

	public static interface OnLifecycleEvent {
		void onStart(Activity activity);
		void onResume(Activity activity);
		void onPause(Activity activity);
		void onStop(Activity activity);
		void onDestroy(Activity activity);
	}

	public static interface OnServiceLifecycleEvent {
		void onDestroy(Service service);
	}

	public TiContext(Activity activity, String baseUrl)
	{
		scope = KrollContext.getKrollContext().createScope();

		this.mainThreadId = Looper.getMainLooper().getThread().getId();
		if (activity != null) {
			this.tiApp = (TiApplication) activity.getApplication();
		} else {
			this.tiApp = TiApplication.getInstance();
		}
		this.weakActivity = new WeakReference<Activity>(activity);
		lifecycleListeners = new TiWeakList<OnLifecycleEvent>(true);
		if (baseUrl == null) {
			baseUrl = TiC.URL_APP_PREFIX;
		} else if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}
		this.baseUrl = new TiUrl(baseUrl, null);

		if (activity instanceof TiActivity) {
			((TiActivity)activity).addTiContext(this);
		}

		if (DBG) {
			Log.e(LCAT, "BaseURL for context is " + baseUrl);
		}
	}

	public boolean isUIThread()
	{
		return Thread.currentThread().getId() == mainThreadId;
	}

	public Activity getActivity()
	{
		if (weakActivity == null) return null;
		Activity activity = weakActivity.get();
		return activity;
	}

	public void setActivity(Activity activity)
	{
		if (activity instanceof TiActivity) {
			((TiActivity)activity).addTiContext(this);
		}
		weakActivity = new WeakReference<Activity>(activity);
	}

	public TiApplication getTiApp() 
	{
		return tiApp;
	}

	public TiRootActivity getRootActivity()
	{
		return getTiApp().getRootActivity();
	}

	public TiFileHelper getTiFileHelper()
	{
		return new TiFileHelper(getTiApp());
	}

	public String resolveUrl(String path)
	{
		return resolveUrl(null, path);
	}

	public String resolveUrl(String scheme, String path)
	{
		return TiUrl.resolve(baseUrl.baseUrl, path, scheme);
	}

	public String resolveUrl(String scheme, String path, String relativeTo)
	{
		return TiUrl.resolve(relativeTo, path, scheme);
	}

	public String getBaseUrl()
	{
		return baseUrl.baseUrl;
	}

	public String getCurrentUrl()
	{
		return currentUrl;
	}

	// Javascript Support

	public Object evalFile(String filename, Messenger messenger, int messageId)
	//	throws IOException
	{
		Object result = null;
		String setUrlBackTo = null;
		if (this.currentUrl != null && this.currentUrl.length() > 0 && !this.currentUrl.equals(filename)) {
			// A new file is being eval'd.  Must be from an include() statement.  Remember to set back
			// the original url, else things like JSS which depend on context's filename will break.
			setUrlBackTo = this.currentUrl;
		}
		this.currentUrl = filename;
		/*if (krollBridge == null) {
			if (DBG) {
				Log.w(LCAT, "Cannot eval file '" + filename + "'. Context has been released already.");
			}
			if (setUrlBackTo != null) { this.currentUrl = setUrlBackTo; }
			return null;
		}*/

		if (filename.startsWith("app://")) {
			KrollContext.getKrollContext().evalFile(scope, filename.replaceAll("app:/", "Resources"));
		}

		if (messenger != null) {
			try {
				Message msg = Message.obtain();
				msg.what = messageId;
				messenger.send(msg);
				if (DBG) {
					Log.d(LCAT, "Notifying caller that evalFile has completed");
				}
			} catch(RemoteException e) {
				Log.w(LCAT, "Failed to notify caller that eval completed");
			}
		}
		if (setUrlBackTo != null) { this.currentUrl = setUrlBackTo; }
		return result;
	}

	public Object evalFile(String filename)
	//	throws IOException
	{
		return evalFile(filename, null, -1);
	}

	public Object evalJS(String src)
	{
		return V8Script.runInContext(src, scope, "<eval>");
	}

	public V8Object getScope()
	{
		return scope;
	}

	public void addOnLifecycleEventListener(OnLifecycleEvent listener)
	{
		lifecycleListeners.add(new WeakReference<OnLifecycleEvent>(listener));
	}

	public void addOnServiceLifecycleEventListener(OnServiceLifecycleEvent listener)
	{
		serviceLifecycleListeners.add(new WeakReference<OnServiceLifecycleEvent>(listener));
	}

	public void removeOnLifecycleEventListener(OnLifecycleEvent listener)
	{
		lifecycleListeners.remove(listener);
	}

	public void removeOnServiceLifecycleEventListener(OnServiceLifecycleEvent listener)
	{
		serviceLifecycleListeners.remove(listener);
	}

	public void fireLifecycleEvent(Activity activity, int which)
	{
		synchronized (lifecycleListeners.synchronizedList()) {
			for (OnLifecycleEvent listener : lifecycleListeners.nonNull()) {
				try {
					fireLifecycleEvent(activity, listener, which);
				} catch (Throwable t) {
					Log.e(LCAT, "Error dispatching lifecycle event: " + t.getMessage(), t);
				}
			}
		}
	}

	protected void fireLifecycleEvent(Activity activity, OnLifecycleEvent listener, int which)
	{
		switch (which) {
			case LIFECYCLE_ON_START: listener.onStart(activity); break;
			case LIFECYCLE_ON_RESUME: listener.onResume(activity); break;
			case LIFECYCLE_ON_PAUSE: listener.onPause(activity); break;
			case LIFECYCLE_ON_STOP: listener.onStop(activity); break;
			case LIFECYCLE_ON_DESTROY: listener.onDestroy(activity); break;
		}
	}

	public void dispatchOnServiceDestroy(Service service)
	{
		synchronized (serviceLifecycleListeners) {
			for (OnServiceLifecycleEvent listener : serviceLifecycleListeners.nonNull()) {
				try {
					listener.onDestroy(service);
				} catch (Throwable t) {
					Log.e(LCAT, "Error dispatching service onDestroy  event: " + t.getMessage(), t);
				}
			}
		}
	}

	public static TiContext createTiContext(Activity activity, String baseUrl)
	{
		return createTiContext(activity, baseUrl, null);
	}

	public static TiContext createTiContext(Activity activity, String baseUrl, String loadFile)
	{
		TiContext ctx = new TiContext(activity, baseUrl);
		if (loadFile != null) {
			ctx.evalFile(loadFile);
		}
		return ctx;
	}

	/*public static TiContext getCurrentTiContext()
	{
		KrollContext currentCtx = KrollContext.getCurrentKrollContext();
		if (currentCtx == null) {
			return null;
		}
		return currentCtx.getTiContext();
	}*/

	public void release()
	{
		if (lifecycleListeners != null) {
			lifecycleListeners.clear();
		}
		if (serviceLifecycleListeners != null) {
			serviceLifecycleListeners.clear();
		}
	}

	public boolean isServiceContext() 
	{
		return serviceContext;
	}

	public void setServiceContext(boolean value)
	{
		serviceContext = true;
		if (value && serviceLifecycleListeners == null ) {
			serviceLifecycleListeners = new TiWeakList<OnServiceLifecycleEvent>(true);
		}
	}

	public boolean isLaunchContext()
	{
		return launchContext;
	}

	public void setLaunchContext(boolean launchContext)
	{
		this.launchContext = launchContext;
	}

	public ContextWrapper getAndroidContext()
	{
		if (weakActivity == null || weakActivity.get() == null) {
			return tiApp;
		}
		return weakActivity.get();
	}

	public void setBaseUrl(String baseUrl)
	{
		this.baseUrl.baseUrl = baseUrl;
		if (this.baseUrl.baseUrl == null) {
			this.baseUrl.baseUrl = TiC.URL_APP_PREFIX;
		}
	}
}
