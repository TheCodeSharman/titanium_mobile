/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDict;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar;

import android.app.Activity;

public class SearchBarProxy extends TiViewProxy
{

	public SearchBarProxy(TiContext tiContext, Object[] args)
	{
		super(tiContext, args);
	}
	
	@Override
	protected TiDict getLangConversionTable() {
		TiDict table = new TiDict();
		table.put("prompt","promptid");
		table.put("hintText","hinttextid");
		return table;
	}
	

	@Override
	public TiUIView createView(Activity activity) {
		return new TiUISearchBar(this);
	}
}
