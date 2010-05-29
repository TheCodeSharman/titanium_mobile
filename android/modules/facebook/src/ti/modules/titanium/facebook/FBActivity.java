/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.facebook;

import java.util.HashMap;
import java.util.Map;

import org.appcelerator.titanium.util.TiConfig;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

public class FBActivity extends Activity
{
	private static final String LOG = FBActivity.class.getSimpleName();
	private static final boolean DBG = TiConfig.LOGD;

    private static final Map<String,FBActivityDelegate> activities = new HashMap<String,FBActivityDelegate>();

	 private FBDialog dialog;

    public static void registerActivity(String identifier, FBActivityDelegate activity)
    {
        activities.put(identifier, activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        String action = getIntent().getAction();
        FBActivityDelegate delegate = activities.get(action);
        dialog = delegate.onCreate(action,this,savedInstanceState);
    }

	@Override
	protected void onStart()
	{
		super.onStart();
		dialog.onStart();
	}

 	 @Override
	 protected void onRestart()
 	 {
			super.onRestart();
			dialog.onRestart();
 	 }

 	 @Override
	 protected void onResume()
 	 {
		super.onResume();
		dialog.onResume();
 	 }

	@Override
	protected void onPause()
 	 {
		super.onPause();
		dialog.onPause();
 	 }

	 @Override
    protected void onStop()
 	 {
		super.onStop();
		dialog.onStop();
 	 }

	 @Override
    protected void onDestroy()
 	 {
		super.onDestroy();
		dialog.onDestroy();
		dialog = null;
 	 }

}
