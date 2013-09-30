/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.searchbar;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;

import ti.modules.titanium.ui.widget.TiUIText;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TiUISearchBar extends TiUIText
{
	protected ImageButton cancelBtn;
	private TiEditText tv;
	private TextView promptText;
	
	public interface OnSearchChangeListener {
		public void filterBy(String text);
	}
	
	protected OnSearchChangeListener searchChangeListener;
	
	public TiUISearchBar(final TiViewProxy proxy)
	{
		super(proxy, true);

		tv = (TiEditText) getNativeView();
		tv.setImeOptions(EditorInfo.IME_ACTION_DONE);
		promptText = new TextView(proxy.getActivity());
		promptText.setEllipsize(TruncateAt.END);
		promptText.setSingleLine(true);

		// TODO Add Filter support

		// Steal the Text's nativeView. We're going to replace it with our layout.
		cancelBtn = new ImageButton(proxy.getActivity());
		cancelBtn.isFocusable();
		cancelBtn.setId(101);
		cancelBtn.setImageResource(android.R.drawable.ic_input_delete);
		// set some minimum dimensions for the cancel button, in a density-independent way.
		final float scale = cancelBtn.getContext().getResources().getDisplayMetrics().density;
		cancelBtn.setMinimumWidth((int) (48 * scale));
		cancelBtn.setMinimumHeight((int) (20 * scale));
		cancelBtn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				/* TODO try {
					proxy.set(getProxy().getTiContext().getScope(), "value", "");
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				tv.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				fireEvent("cancel", null);
			}
		});

		RelativeLayout layout = new RelativeLayout(proxy.getActivity())
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(proxy);
			}
		};

		layout.setGravity(Gravity.NO_GRAVITY);
		layout.setPadding(0,0,0,0);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		promptText.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(promptText, params);

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		params.addRule(RelativeLayout.LEFT_OF, 101);
//		params.setMargins(4, 4, 4, 4);
		layout.addView(tv, params);

		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
//		params.setMargins(0, 4, 4, 4);
		layout.addView(cancelBtn, params);

		setNativeView(layout);
	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (this.searchChangeListener != null) {
			this.searchChangeListener.filterBy(s.toString());
		}
		super.onTextChanged(s, start, before, count);
	}

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);
		if (d.containsKey(TiC.PROPERTY_SHOW_CANCEL)) {
			applyProperties(TiC.PROPERTY_SHOW_CANCEL, null, TiConvert.toString(proxy.getProperty(TiC.PROPERTY_SHOW_CANCEL)),
				null);
		}
		if (d.containsKey(TiC.PROPERTY_BAR_COLOR)) {
			applyProperties(TiC.PROPERTY_BAR_COLOR, null, TiConvert.toString(proxy.getProperty(TiC.PROPERTY_BAR_COLOR)), null);
		}
		if (d.containsKey(TiC.PROPERTY_PROMPT)) {
			applyProperties(TiC.PROPERTY_PROMPT, null, TiConvert.toString(proxy.getProperty(TiC.PROPERTY_PROMPT)), null);
		}
		if (d.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			applyProperties(TiC.PROPERTY_BACKGROUND_IMAGE, null,
				TiConvert.toString(proxy.getProperty(TiC.PROPERTY_BACKGROUND_IMAGE)), proxy);
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		super.propertyChanged(key, oldValue, newValue, proxy);
		applyProperties(key, oldValue, newValue, proxy);
	}

	private void applyProperties(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (key.equals(TiC.PROPERTY_SHOW_CANCEL)) {
			boolean showCancel = TiConvert.toBoolean(newValue);
			cancelBtn.setVisibility(showCancel ? View.VISIBLE : View.GONE);
		} else if (key.equals(TiC.PROPERTY_BAR_COLOR)) {
			nativeView.setBackgroundColor(TiConvert.toColor(TiConvert.toString(newValue)));
		} else if (key.equals(TiC.PROPERTY_PROMPT)) {
			String strPrompt = TiConvert.toString(newValue);
			promptText.setText(strPrompt);
		} else if (key.equals(TiC.PROPERTY_BACKGROUND_IMAGE)) {
			String bkgdImage = TiConvert.toString(newValue);
			TiFileHelper tfh = new TiFileHelper(tv.getContext());
			String url = proxy.resolveUrl(null, bkgdImage);
			Drawable background = tfh.loadDrawable(url, false);
			nativeView.setBackgroundDrawable(background);
		}
	}
	
	public void setOnSearchChangeListener(OnSearchChangeListener listener) {
		this.searchChangeListener = listener;
	}
}
