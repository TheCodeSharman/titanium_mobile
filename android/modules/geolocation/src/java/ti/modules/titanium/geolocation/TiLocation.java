/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.geolocation;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiConfig;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.analytics.TiAnalyticsEvent;
import org.appcelerator.titanium.analytics.TiAnalyticsEventFactory;
import org.appcelerator.titanium.util.TiPlatformHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;


public class TiLocation implements Handler.Callback
{
	public static final int ERR_POSITION_UNAVAILABLE = 2;
	public static final int MSG_FIRST_ID = 100;
	public static final int MSG_LOOKUP = MSG_FIRST_ID + 1;
	public static final int MSG_LAST_ID = MSG_FIRST_ID + 2;

	public static LocationManager locationManager;

	private static final String TAG = "TiLocation";
	private static final boolean DBG = TiConfig.LOGD;
	private static final String BASE_GEO_URL = "http://api.appcelerator.net/p/v1/geo?";

	private static TiLocation instance;
	private static String mobileId;
	private static String appGuid;
	private static String sessionId;
	private static String countryCode;
	private static long lastAnalyticsTimestamp = 0;
	private static List<String> knownProviders;

	private Handler runtimeHandler;


	public interface GeocodeResponseHandler
	{
		public abstract void handleGeocodeResponse(HashMap<String, Object> geocodeResponse);
	}


	static
	{
		// initialize static values once
		locationManager = (LocationManager) TiApplication.getInstance().getSystemService(Context.LOCATION_SERVICE);
		knownProviders = locationManager.getAllProviders();
		mobileId = TiPlatformHelper.getMobileId();
		appGuid = TiApplication.getInstance().getAppInfo().getGUID();
		sessionId = TiPlatformHelper.getSessionId();
		countryCode = Locale.getDefault().getCountry();

		// create instance since we need a handler instance
		instance = new TiLocation();
	}

	private TiLocation()
	{
		runtimeHandler = new Handler(TiMessenger.getRuntimeMessenger().getLooper(), this);
	}

	public boolean handleMessage(Message msg)
	{
		if (msg.what == MSG_LOOKUP) {
			AsyncTask<Object, Void, Integer> task = getLookUpTask();
			task.execute(msg.getData().getString(TiC.PROPERTY_URL), msg.getData().getString(TiC.PROPERTY_DIRECTION), msg.obj);

			return true;
		}

		return false;
	}

	public static boolean isProvider(String name)
	{
		return knownProviders.contains(name);
	}

	public static boolean getLocationServicesEnabled()
	{
		List<String> providerNames = locationManager.getProviders(true);

		if (DBG) {
			Log.i(TAG, "Enabled location provider count: " + providerNames.size());

			for (String providerName : providerNames) {
				Log.i(TAG, providerName + " service available");
			}
		}

		// don't count the passive provider
		for(String name : providerNames) {
			if(name.equals(LocationManager.NETWORK_PROVIDER) || name.equals(LocationManager.GPS_PROVIDER)) {
				return true;
			}
		}

		return false;
	}

	public static Location getLastKnownLocation()
	{
		Location latestKnownLocation = null;

		for (String provider : knownProviders) {
			Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
			if ((latestKnownLocation == null) || (lastKnownLocation.getTime() > latestKnownLocation.getTime())) {
				latestKnownLocation = lastKnownLocation;
			}
		}

		return latestKnownLocation;
	}

	public static void doAnalytics(Location location)
	{
		long locationTime = location.getTime();
		if (locationTime - lastAnalyticsTimestamp > GeolocationModule.MAX_GEO_ANALYTICS_FREQUENCY) {
			TiAnalyticsEvent event = TiAnalyticsEventFactory.createAppGeoEvent(location);
			if (event != null) {
				TiApplication.getInstance().postAnalyticsEvent(event);
				lastAnalyticsTimestamp = locationTime;
			}
		}
	}

	public static void forwardGeocode(String address, GeocodeResponseHandler responseHandler)
	{
		if (address != null) {
			String geocoderUrl = buildGeocoderURL("forward", mobileId, appGuid, sessionId, address, countryCode);
			if (geocoderUrl != null) {
				Message message = instance.runtimeHandler.obtainMessage(MSG_LOOKUP);
				message.getData().putString(TiC.PROPERTY_DIRECTION, "forward");
				message.getData().putString(TiC.PROPERTY_URL, geocoderUrl);

				message.obj = responseHandler;
				message.sendToTarget();
			}

		} else {
			Log.e(TAG, "unable to forward geocode, address is null");
		}
	}

	public static void reverseGeocode(double latitude, double longitude, GeocodeResponseHandler responseHandler)
	{
		String geocoderUrl = buildGeocoderURL("reverse", mobileId, appGuid, sessionId, latitude + "," + longitude, countryCode);
		if (geocoderUrl != null) {
			Message message = instance.runtimeHandler.obtainMessage(MSG_LOOKUP);
			message.getData().putString(TiC.PROPERTY_DIRECTION, "reverse");
			message.getData().putString(TiC.PROPERTY_URL, geocoderUrl);

			message.obj = responseHandler;
			message.sendToTarget();

		} else {
			Log.e(TAG, "unable to reverse geocode, geocoder url is null");
		}
	}

	private static String buildGeocoderURL(String direction, String mid, String aguid, String sid, String query, String countryCode)
	{
		String url = null;

		try {
			StringBuilder sb = new StringBuilder();
			sb.append(BASE_GEO_URL)
				.append("d=r")
				.append("&mid=")
				.append(mid)
				.append("&aguid=")
				.append(aguid)
				.append("&sid=")
				.append(sid)
				.append("&q=")
				.append(URLEncoder.encode(query, "utf-8"));

			url = sb.toString();

		} catch (UnsupportedEncodingException e) {
			Log.e(TAG, "unable to encode query to utf-8: " + e.getMessage());
		}

		return url;
	}

	private AsyncTask<Object, Void, Integer> getLookUpTask()
	{
		AsyncTask<Object, Void, Integer> task = new AsyncTask<Object, Void, Integer>() {
			@Override
			protected Integer doInBackground(Object... args) {
				try {
					String url = (String) args[0];
					String direction = (String) args[1];
					GeocodeResponseHandler geocodeResponseHandler = (GeocodeResponseHandler) args[2];

					if (DBG) {
						Log.d(TAG, "GEO URL [" + url + "]");
					}
					HttpGet httpGet = new HttpGet(url);

					HttpParams httpParams = new BasicHttpParams();
					HttpConnectionParams.setConnectionTimeout(httpParams, 5000);

					HttpClient client = new DefaultHttpClient(httpParams);
					client.getParams().setBooleanParameter("http.protocol.expect-continue", false);
					ResponseHandler<String> responseHandler = new BasicResponseHandler();
					String response = client.execute(httpGet, responseHandler);

					if (DBG) {
						Log.i(TAG, "received Geo [" + response + "]");
					}

					HashMap<String, Object> event = null;
					if (response != null) {
						try {
							JSONObject jsonObject = new JSONObject(response);
							if (jsonObject.getBoolean(TiC.PROPERTY_SUCCESS)) {
								if (direction.equals("forward")) {
									event = buildForwardGeocodeResponse(jsonObject);

								} else {
									event = buildReverseGeocodeResponse(jsonObject);
								}

							} else {
								event = new KrollDict();
								KrollDict errorDict = new KrollDict();
								String errorCode = jsonObject.getString(TiC.ERROR_PROPERTY_ERRORCODE);
								errorDict.put(TiC.PROPERTY_MESSAGE, "Unable to resolve message: Code (" + errorCode + ")");
								errorDict.put(TiC.PROPERTY_CODE, errorCode);
								event.put(TiC.EVENT_PROPERTY_ERROR, errorDict);
							}

						} catch (JSONException e) {
							Log.e(TAG, "error converting geo response to JSONObject [" + e.getMessage() + "]", e);
						}
					}

					if (event != null) {
						geocodeResponseHandler.handleGeocodeResponse(event);
					}

				} catch (Throwable t) {
					Log.e(TAG, "error retrieving geocode information [" + t.getMessage() + "]", t);
				}

				return -1;
			}
		};

		return task;
	}

	private HashMap<String, Object> buildForwardGeocodeResponse(JSONObject jsonResponse)
		throws JSONException
	{
		HashMap<String, Object> address = new HashMap<String, Object>();

		JSONArray places = jsonResponse.getJSONArray(TiC.PROPERTY_PLACES);
		if (places.length() > 0) {
			address = buildAddress(places.getJSONObject(0));
		}

		return address;
	}

	private HashMap<String, Object> buildReverseGeocodeResponse(JSONObject jsonResponse)
		throws JSONException
	{
		JSONArray places = jsonResponse.getJSONArray(TiC.PROPERTY_PLACES);
		ArrayList<HashMap<String, Object>> addresses = new ArrayList<HashMap<String, Object>>();

		int count = places.length();
		for (int i = 0; i < count; i++) {
			addresses.add(buildAddress(places.getJSONObject(i)));
		}

		HashMap<String, Object> response = new HashMap<String, Object>();
		response.put(TiC.PROPERTY_SUCCESS, true);
		response.put(TiC.PROPERTY_PLACES, addresses.toArray());

		return response;
	}

	private HashMap<String, Object> buildAddress(JSONObject place)
	{
		HashMap<String, Object> address = new HashMap<String, Object>();
		address.put(TiC.PROPERTY_STREET1, place.optString(TiC.PROPERTY_STREET, ""));
		address.put(TiC.PROPERTY_STREET, place.optString(TiC.PROPERTY_STREET, ""));
		address.put(TiC.PROPERTY_CITY, place.optString(TiC.PROPERTY_CITY, ""));
		address.put(TiC.PROPERTY_REGION1, ""); // AdminArea
		address.put(TiC.PROPERTY_REGION2, ""); // SubAdminArea
		address.put(TiC.PROPERTY_POSTAL_CODE, place.optString("zipcode", ""));
		address.put(TiC.PROPERTY_COUNTRY, place.optString(TiC.PROPERTY_COUNTRY, ""));
		address.put("countryCode", place.optString(TiC.PROPERTY_COUNTRY_CODE, "")); // TIMOB-4478, remove this later, was old android name
		address.put(TiC.PROPERTY_COUNTRY_CODE, place.optString(TiC.PROPERTY_COUNTRY_CODE, ""));		
		address.put(TiC.PROPERTY_LONGITUDE, place.optString(TiC.PROPERTY_LONGITUDE, ""));
		address.put(TiC.PROPERTY_LATITUDE, place.optString(TiC.PROPERTY_LATITUDE, ""));
		address.put(TiC.PROPERTY_DISPLAY_ADDRESS, place.optString(TiC.PROPERTY_ADDRESS));
		address.put(TiC.PROPERTY_ADDRESS, place.optString(TiC.PROPERTY_ADDRESS));

		return address;
	}
}


