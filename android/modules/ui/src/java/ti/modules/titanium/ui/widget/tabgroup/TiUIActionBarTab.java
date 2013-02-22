/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tabgroup;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiUIHelper;

import ti.modules.titanium.ui.TabProxy;
import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class TiUIActionBarTab extends TiUIAbstractTab {

	private static final String TAG = "TiUIActionBarTab";
	public static class TabFragment extends Fragment {
		private View contentView;

		public void setContentView(View view) {
			contentView = view;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			return contentView;
		}
	}

	ActionBar.Tab tab;

	/**
	 * The fragment that will provide the content view of the tab.
	 * This fragment will be attached when the tab is selected and
	 * detached when it is later unselected. This reference will be
	 * initialized when the tab is first selected.
	 */
	TabFragment fragment;

	public TiUIActionBarTab(TabProxy proxy, ActionBar.Tab tab) {
		super(proxy);
		this.tab = tab;

		proxy.setModelListener(this);

		// Provide a reference to this instance by placing
		// a reference inside the "tag" slot that ActionBar.Tab provides.
		tab.setTag(this);

		Object title = proxy.getProperty(TiC.PROPERTY_TITLE);
		if (title != null) {
			tab.setText(title.toString());
		}
		Object url = proxy.getProperty(TiC.PROPERTY_ICON);
		if (url != null) {
			Drawable icon = getDrawableFromUrl(url);
			tab.setIcon(icon);
		}
		
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_TITLE)) {
			tab.setText(newValue.toString());
		}
		if (key.equals(TiC.PROPERTY_ICON)) {
			Drawable icon = null;
			if (newValue != null){
				icon = getDrawableFromUrl(newValue);
			}
			tab.setIcon(icon);
		}
	}

	private Drawable getDrawableFromUrl(Object url)
	{
		try {
			return TiUIHelper.getResourceDrawable(url);
		} catch (Exception e) {
			Log.w(TAG, "Could not load drawable "+e.getMessage(), Log.DEBUG_MODE);
			return null;
		}
	}
	
	/**
	 * Initialize this tab's fragment. Called by the tab group
	 * when the tab is first selected to create the fragment which
	 * will display the tab's content view.
	 */
	void initializeFragment() {
		fragment = new TabFragment();
		fragment.setContentView(getContentView());
	}

}
