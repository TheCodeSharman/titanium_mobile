/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.runtime.v8.EventEmitter;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiMessageQueue;
import org.appcelerator.titanium.util.AsyncResult;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUrl;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

@Kroll.proxy
public class KrollProxy extends EventEmitter
	implements Handler.Callback
{
	private static final String TAG = "KrollProxy";
	private static final boolean DBG = TiConfig.LOGD;

	protected static final int MSG_MODEL_PROPERTY_CHANGE = EventEmitter.MSG_LAST_ID + 100;
	protected static final int MSG_LISTENER_ADDED = EventEmitter.MSG_LAST_ID + 101;
	protected static final int MSG_LISTENER_REMOVED = EventEmitter.MSG_LAST_ID + 102;
	protected static final int MSG_MODEL_PROPERTIES_CHANGED = EventEmitter.MSG_LAST_ID + 103;
	protected static final int MSG_LAST_ID = EventEmitter.MSG_LAST_ID + 999;
	protected static AtomicInteger proxyCounter = new AtomicInteger();

	public static final String PROXY_ID_PREFIX = "proxy$";


	protected Activity activity;
	protected String proxyId;
	protected TiUrl creationUrl;
	protected KrollProxyListener modelListener;
	protected KrollModule createdInModule;
	protected boolean coverageEnabled;
	protected KrollDict creationDict = null;

	// entry point for generator code
	public static KrollProxy create(Class<? extends KrollProxy> objClass, Object[] creationArguments, long v8ObjectPointer, String creationUrl)
	{
		KrollProxy proxyInstance = null;

		try {
			proxyInstance = objClass.newInstance();

			/* store reference to the native object that represents this proxy so we can drive changes to the JS 
			 * object
			 */
			proxyInstance.setPointer(v8ObjectPointer); // TODO - rename to KrollObject pointer?  should be runtime agnostic
			proxyInstance.creationUrl = new TiUrl(creationUrl);

			/* associate the activity with the proxy.  if the proxy needs activity association delayed until a 
			 * later point then initActivity should be overridden to be a no-op and then call setActivity directly
			 * at the appropriate time
			 */
			proxyInstance.initActivity(TiApplication.getInstance().getCurrentActivity());

			// setup the proxy according to the creation arguments
			proxyInstance.handleCreationArgs(null, creationArguments);

			return proxyInstance;

		} catch (IllegalAccessException e) {
			e.printStackTrace();

		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		return null;
	}

	public KrollProxy()
	{
		super(0);
	}

	protected void initActivity(Activity activity)
	{
		this.activity = activity;
	}

	/**
	 * Handle the arguments passed into the "create" method for this proxy.
	 * If your proxy simply needs to handle a KrollDict, see {@link KrollProxy#handleCreationDict(KrollDict)}
	 * @param args
	 */
	public void handleCreationArgs(KrollModule createdInModule, Object[] args)
	{
		this.createdInModule = createdInModule;

		if (args.length >= 1 && args[0] instanceof HashMap) {
			if (args[0] instanceof KrollDict) {
				handleCreationDict((KrollDict)args[0]);
			} else {
				handleCreationDict(new KrollDict((HashMap)args[0]));
			}
		}
	}

	/**
	 * Handle the creation {@link KrollDict} passed into the create method for this proxy.
	 * This is usually the first (and sometimes only) argument to the proxy's create method.
	 * @param dict
	 */
	public void handleCreationDict(KrollDict dict)
	{
		if (dict != null) {
			// TODO we need to set properties inside the proxy from the creation dict on the V8 side
			/*for (String key : dict.keySet()) {
				setProperty(key, dict.get(key), true);
			}*/
			creationDict = (KrollDict)dict.clone();
			if (modelListener != null) {
				modelListener.processProperties(creationDict);
			}
		}
	}

	public void setActivity(Activity activity)
	{
		this.activity = activity;
	}

	public Activity getActivity()
	{
		return activity;
	}

	public TiUrl getCreationUrl()
	{
		return creationUrl;
	}

	@Deprecated
	public boolean hasProperty(String name)
	{
		return has(name);
	}

	@Deprecated
	public Object getProperty(String name)
	{
		return get(name);
	}

	@Deprecated
	public void setProperty(String name, Object value)
	{
		set(name, value);
	}

	@Deprecated
	public void setProperty(String name, Object value, boolean fireChange)
	{
		if (!fireChange) {
			set(name, value);
		} else {
			setAndFire(name, value);
		}
	}

	protected void firePropertyChanged(String name, Object oldValue, Object newValue)
	{
		if (modelListener != null) {
			if (TiApplication.isUIThread()) {
				modelListener.propertyChanged(name, oldValue, newValue, this);
			} else {
				KrollPropertyChange pch = new KrollPropertyChange(name, oldValue, newValue);
				getUIHandler().obtainMessage(MSG_MODEL_PROPERTY_CHANGE, pch).sendToTarget();
			}
		}
	}

	protected boolean shouldFireChange(Object oldValue, Object newValue)
	{
		if (!(oldValue == null && newValue == null)) {
			if ((oldValue == null && newValue != null)
					|| (newValue == null && oldValue != null)
					|| (!oldValue.equals(newValue))) {
				return true;
			}
		}
		return false;
	}

	public void setAndFire(String name, Object value)
	{
		Object current = get(name);
		set(name, value);

		if (shouldFireChange(current, value)) {
			firePropertyChanged(name, current, value);
		}
	}

	protected void firePropertiesChanged(List<KrollPropertyChange> changes)
	{
		if (modelListener != null) {
			modelListener.propertiesChanged(changes, this);
		}
	}

	public KrollDict getCreationDict()
	{
		return creationDict;
	}

	public KrollModule getCreatedInModule()
	{
		return createdInModule;
	}

	@Override @SuppressWarnings("unchecked")
	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_MODEL_PROPERTY_CHANGE: {
				KrollPropertyChange pch = (KrollPropertyChange) msg.obj;
				pch.fireEvent(this, modelListener);
				return true;
			}
			case MSG_LISTENER_ADDED:
			case MSG_LISTENER_REMOVED: {
				if (modelListener == null) return true;

				String event = msg.getData().getString(EventEmitter.EVENT_NAME);
				HashMap<String, Object> map = (HashMap<String, Object>) msg.obj;
				int count = TiConvert.toInt(map.get(TiC.PROPERTY_COUNT));
				if (msg.what == MSG_LISTENER_ADDED) {
					eventListenerAdded(event, count, this);
				} else {
					eventListenerRemoved(event, count, this);
				}
				return true;
			}
			case MSG_MODEL_PROPERTIES_CHANGED: {
				firePropertiesChanged((List<KrollPropertyChange>)msg.obj);
				return true;
			}
		}
		return super.handleMessage(msg);
	}

	protected void eventListenerAdded(String event, int count, KrollProxy proxy)
	{
		modelListener.listenerAdded(event, count, this);
	}

	protected void eventListenerRemoved(String event, int count, KrollProxy proxy)
	{
		modelListener.listenerRemoved(event, count, this);
	}

	public void setModelListener(KrollProxyListener modelListener)
	{
		// Double-setting the same modelListener can potentially have weird side-effects.
		if (this.modelListener != null && this.modelListener.equals(modelListener)) { return; }

		this.modelListener = modelListener;
		if (modelListener != null) {
			modelListener.processProperties(creationDict);
			creationDict = null;
		}
	}

	public String resolveUrl(String scheme, String path)
	{
		return TiUrl.resolve(creationUrl.baseUrl, path, scheme);
	}

	public Object sendBlockingUiMessage(int what, Object asyncArg)
	{
		AsyncResult result = new AsyncResult(asyncArg);
		return sendBlockingUiMessage(
			getUIHandler().obtainMessage(what, result), result);
	}
	
	public Object sendBlockingUiMessage(int what, int arg1)
	{
		AsyncResult result = new AsyncResult(null);
		return sendBlockingUiMessage(
			getUIHandler().obtainMessage(what, arg1, -1), result);
	}

	public Object sendBlockingUiMessage(int what, Object asyncArg, int arg1, int arg2)
	{
		AsyncResult result = new AsyncResult(asyncArg);
		return sendBlockingUiMessage(
			getUIHandler().obtainMessage(what, arg1, arg2, result), result);
	}

	public Object sendBlockingUiMessage(Message msg, AsyncResult result)
	{
		return TiMessageQueue.getMessageQueue().sendBlockingMessage(
			msg, TiMessageQueue.getMainMessageQueue(), result);
	}

	public String getProxyId()
	{
		return proxyId;
	}

	protected KrollDict createErrorResponse(int code, String message)
	{
		KrollDict error = new KrollDict();
		error.put(TiC.ERROR_PROPERTY_CODE, code);
		error.put(TiC.ERROR_PROPERTY_MESSAGE, message);

		return error;
	}

	public Object getDefaultValue(Class<?> typeHint)
	{
		return toString();
	}
}
