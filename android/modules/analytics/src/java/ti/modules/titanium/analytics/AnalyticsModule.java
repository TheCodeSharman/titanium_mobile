/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.analytics;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.util.TiConvert;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.appcelerator.analytics.APSAnalytics;

@Kroll.module
public class AnalyticsModule extends KrollModule
{
	private static final String TAG = "AnalyticsModule";
	
	protected static final String PROPERTY_APP_NAV = "app.nav";
	protected static final String PROPERTY_APP_TIMED = "app.timed";
	protected static final String PROPERTY_APP_FEATURE = "app.feature";
	protected static final String PROPERTY_APP_SETTINGS = "app.settings";
	protected static final String PROPERTY_APP_USER = "app.user";

	public AnalyticsModule()
	{
		super();
	}

	public AnalyticsModule(TiContext tiContext)
	{
		this();
	}

	@Kroll.method
	public void navEvent(String from, String to,
		@Kroll.argument(optional=true) String event,
		@Kroll.argument(optional=true) KrollDict data)
	{
		if (TiApplication.getInstance().isAnalyticsEnabled()) {
			if (data instanceof HashMap) {
				APSAnalytics.sendAppNavEvent(from, to, event, TiConvert.toJSON(data));

			} else {
				try {
					APSAnalytics.sendAppNavEvent(from, to, event, new JSONObject(data.toString()));
				} catch (JSONException e) {
					Log.e(TAG, "Cannot convert data into JSON");
				}
			}
		}
	}

	@Kroll.method
	public void featureEvent(String event, @Kroll.argument(optional = true) KrollDict data)
	{
		if (TiApplication.getInstance().isAnalyticsEnabled()) {
			if (data instanceof HashMap) {
				APSAnalytics.sendFeatureEvent(event, TiConvert.toJSON(data));
			} else {
				try {
					APSAnalytics.sendFeatureEvent(event, new JSONObject(data.toString()));
				} catch (JSONException e) {
					Log.e(TAG, "Cannot convert data into JSON");
				}
			}
		}
	}

	@Kroll.getProperty @Kroll.method
	public String getLastEvent()
	{
		if (TiApplication.getInstance().isAnalyticsEnabled()) {
			return APSAnalytics.getLastEvent();
		}
		return null;
	}

	@Override
	public String getApiName()
	{
		return "Ti.Analytics";
	}
}
