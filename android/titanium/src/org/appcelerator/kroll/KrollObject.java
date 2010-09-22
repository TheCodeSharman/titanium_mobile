/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

@SuppressWarnings("serial")
public class KrollObject extends ScriptableObject {

	protected KrollProxy proxy;
	
	public KrollObject(KrollProxy proxy) {
		this.proxy = proxy;
	}
	
	@Override
	public String getClassName() {
		return "Ti."+proxy.getAPIName() + (proxy instanceof KrollModule ? "Module":"");
	}
	
	@Override
	public Object get(String name, Scriptable start) {
		try {
			Object value = proxy.get(start, name);
			if (value.equals(KrollProxy.UNDEFINED)) {
				return Scriptable.NOT_FOUND;
			}
			
			KrollInvocation invocation = KrollInvocation.createPropertyGetInvocation(start, null, name, null, proxy);
			return KrollConverter.getInstance().convertNative(invocation, value);
			
		} catch (NoSuchFieldException e) {
			return Scriptable.NOT_FOUND;
		}
	}
	
	@Override
	public Object get(int index, Scriptable start) {
		return super.get(index, start);
	}
	
	@Override
	public void put(String name, Scriptable start, Object value) {
		try {
			
			KrollInvocation invocation = KrollInvocation.createPropertySetInvocation(start, null, name, null, proxy);
			value = KrollConverter.getInstance().convertJavascript(invocation, value, Object.class);
			proxy.set(start, name, value);
		} catch (NoSuchFieldException e) {
			Context.throwAsScriptRuntimeEx(e);
		}
	}
	
	@Override
	public void put(int index, Scriptable start, Object value) {
		super.put(index, start, value);
	}
	
	public KrollProxy getProxy() {
		return proxy;
	}
}
