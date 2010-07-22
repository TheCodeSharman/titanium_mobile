/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package ti.modules.titanium.___PROJECTNAME___;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDict;
import org.appcelerator.titanium.TiModule;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.TiProxy;
import org.appcelerator.titanium.util.TiConfig;

import org.appcelerator.titanium.annotations.manifest.Ti;
import org.appcelerator.titanium.annotations.manifest.Ti.version;
import org.appcelerator.titanium.annotations.manifest.Ti.manifest.activity.configChangesTypes;
import org.appcelerator.titanium.annotations.manifest.Ti.manifest.activity.launchModeTypes;

@Ti.module(
		name = "__PROJECT_SHORT_NAME__", 
		version = @version(buildVersion=0, minorVersion=0, majorVersion=1),
		dependsUponTitanium = @version(minorVersion=3, majorVersion=1)
)

public class __PROJECT_SHORT_NAME__Module extends TiModule
{

	// Standard Debugging variables
	private static final String LCAT = "__PROJECT_SHORT_NAME__Module";
	private static final boolean DBG = TiConfig.LOGD;

	// Standard constants exposed to external developers
	private static TiDict constants;

	public __PROJECT_SHORT_NAME__Module(TiContext tiContext) {
		super(tiContext);
	}

	@Override
	public TiDict getConstants()
	{
		if (constants == null) {
			constants = new TiDict();

			/**
			 * Place your own constants here in the form:
			 *    constants.put("EXTERNAL_NAME", value);
			 */
		}

		return constants;
	}

	// Methods
	
	// Standard helpers 
	// To log a message to logcat
	//    Log.e(LCAT, "Unable to create tmp file: " + e.getMessage(), e);

}
