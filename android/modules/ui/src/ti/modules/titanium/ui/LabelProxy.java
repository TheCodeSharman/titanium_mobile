/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiProxy;
import org.appcelerator.titanium.TiDict;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUILabel;
import ti.modules.titanium.ui.widget.tableview.TiTableViewRowProxyItem;
import android.app.Activity;

public class LabelProxy extends TiViewProxy
{
	private boolean clickable = false;
	public LabelProxy(TiContext tiContext, Object[] args)
	{
		super(tiContext, args);
		tiContext.addOnEventChangeListener(this);
	}

	@Override
	protected TiDict getLangConversionTable() {
		TiDict table = new TiDict();
		table.put("text","textid");
		return table;
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		TiUILabel label = new TiUILabel(this);
		clickable = hasListeners("click");
		if (clickable) {
			label.setClickable(true);
		}
		return label;
	}
	
	@Override
	public void eventListenerAdded(String eventName, int count, TiProxy proxy) {
		super.eventListenerAdded(eventName, count, proxy);
		
		if (eventName.equals("click") && proxy.equals(this)) {
			if (peekView() != null) {
				((TiUILabel)getView(getTiContext().getActivity())).setClickable(true);
			}
		}
	}
	
	@Override
	public void eventListenerRemoved(String eventName, int count, TiProxy proxy) {
		super.eventListenerRemoved(eventName, count, proxy);
		
		if (eventName.equals("click") && count == 0 && proxy.equals(this)) {
			if (peekView() != null) {
				((TiUILabel)getView(getTiContext().getActivity())).setClickable(false);
			}
		}
	}
	
	public void setClickable(boolean clickable) {
		this.clickable = clickable;
		if (peekView() != null) {
			TiUILabel label = (TiUILabel)getView(getTiContext().getActivity());
			if (label != null) {
				label.setClickable(clickable);
			}
		}
	}
}
