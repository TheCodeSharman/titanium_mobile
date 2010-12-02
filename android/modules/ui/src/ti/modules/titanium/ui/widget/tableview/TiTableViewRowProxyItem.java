/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tableview;

import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.LabelProxy;
import ti.modules.titanium.ui.TableViewProxy;
import ti.modules.titanium.ui.TableViewRowProxy;
import ti.modules.titanium.ui.widget.TiUILabel;
import ti.modules.titanium.ui.widget.tableview.TableViewModel.Item;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class TiTableViewRowProxyItem extends TiBaseTableViewItem
{
	private static final String LCAT = "TitaniumTableViewItem";
	private static final boolean DBG = TiConfig.LOGD;

	private static final int LEFT_MARGIN = 5;
	private static final int RIGHT_MARGIN = 7;

	private BitmapDrawable hasChildDrawable, hasCheckDrawable;
	private ImageView leftImage;
	private ImageView rightImage;
	private TiCompositeLayout content;
	private ArrayList<TiUIView> views;
	private boolean hasControls;
	private int height = -1;
	private Item item;

	public TiTableViewRowProxyItem(TiContext tiContext)
	{
		super(tiContext);

		this.handler = new Handler(this);
		this.leftImage = new ImageView(tiContext.getActivity());
		leftImage.setVisibility(GONE);
		addView(leftImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		this.content = new TiCompositeLayout(tiContext.getActivity(), false);
		content.setMinimumHeight(48);
		addView(content, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));

		this.rightImage = new ImageView(tiContext.getActivity());
		rightImage.setVisibility(GONE);
		addView(rightImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	protected TableViewRowProxy getRowProxy() {
		return (TableViewRowProxy)item.proxy;
	}

	public void setRowData(Item item) {
		this.item = item;
		TableViewRowProxy rp = getRowProxy();
		rp.setTableViewItem(this);
		setRowData(rp);
	}

	public Item getRowData() {
		return this.item;
	}

	protected TiViewProxy addViewToOldRow(int index, TiUIView titleView, TiViewProxy newViewProxy) {
		if (DBG) {
			Log.w(LCAT, newViewProxy + " was added an old style row, reusing the title TiUILabel");
		}
		LabelProxy label = new LabelProxy(tiContext);
		label.handleCreationDict(titleView.getProxy().getProperties());
		label.setView(titleView);
		label.setModelListener(titleView);
		titleView.setProxy(label);

		getRowProxy().getControls().add(index, label);
		views.add(newViewProxy.getView(tiContext.getActivity()));
		return label;
	}

	protected void refreshControls() {
		ArrayList<TiViewProxy> proxies = getRowProxy().getControls();
		int len = proxies.size();

		if (views == null) {
			views = new ArrayList<TiUIView>(len);
		} else if (views.size() != len) {
			for (TiUIView view : views) {
				View v = view.getNativeView();
				if (v != null && v.getParent().equals(content)) {
					content.removeView(v);
				}
			}
			views = new ArrayList<TiUIView>(len);
		}

		for (int i = 0; i < len; i++) {
			TiUIView view = views.size() > i ? views.get(i) : null;
			TiViewProxy proxy = proxies.get(i);
			if (view != null && view.getProxy() instanceof TableViewRowProxy) {
				proxy = addViewToOldRow(i, view, proxy);
				len++;
			}
			if (view == null) {
				// In some cases the TiUIView for this proxy has been reassigned to another proxy
				// We don't want to actually release it though, just reassign by creating a new view
				view = proxy.forceCreateView(tiContext.getActivity());
				if (i >= views.size()) {
					views.add(view);
				} else {
					views.set(i, view);
				}
			}

			View v = view.getNativeView();
			view.setProxy(proxy);
			view.processProperties(proxy.getProperties());
			if (v.getParent() == null) {
				content.addView(v, view.getLayoutParams());
			}
		}
	}

	protected void refreshOldStyleRow() {
		TableViewRowProxy rp = getRowProxy();
		String title = "Missing title";
		if (rp.getProperty("title") != null) {
			title = TiConvert.toString(rp.getProperty("title"));
		}
		if (!rp.hasProperty("touchEnabled")) {
			rp.setProperty("touchEnabled", false);
		}
		if (views == null) {
			views = new ArrayList<TiUIView>();
			views.add(new TiUILabel(rp));
		}
		TiUILabel t = (TiUILabel) views.get(0);
		t.setProxy(rp);
		t.processProperties(filterProperties(rp.getProperties()));
		View v = t.getNativeView();
		if (v.getParent() == null) {
			TextView tv = (TextView) v;
			//tv.setTextColor(Color.WHITE);
			TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) t.getLayoutParams();
			params.optionLeft = 5;
			params.optionRight = 5;
			params.autoFillsWidth = true;
			content.addView(v, params);
		}
	}

	public void setRowData(TableViewRowProxy rp) {
		KrollDict props = rp.getProperties();
		hasControls = rp.hasControls();

		setBackgroundFromProperties(props);

		// Handle right image
		boolean clearRightImage = true;
		if (props.containsKey("hasChild")) {
			if (TiConvert.toBoolean(props, "hasChild")) {
				if (hasChildDrawable == null) {
					hasChildDrawable = createHasChildDrawable();
				}
				rightImage.setImageDrawable(hasChildDrawable);
				rightImage.setVisibility(VISIBLE);
				clearRightImage = false;
			}
		}
		else if (props.containsKey("hasCheck")) {
			if (TiConvert.toBoolean(props, "hasCheck")) {
				if (hasCheckDrawable == null) {
					hasCheckDrawable = createHasCheckDrawable();
				}
				rightImage.setImageDrawable(hasCheckDrawable);
				rightImage.setVisibility(VISIBLE);
				clearRightImage = false;
			}
		}

		if (props.containsKey("rightImage")) {
			String path = TiConvert.toString(props, "rightImage");
			String url = tiContext.resolveUrl(null, path);

			Drawable d = loadDrawable(url);
			if (d != null) {
				rightImage.setImageDrawable(d);
				rightImage.setVisibility(VISIBLE);
				clearRightImage = false;
			}
		}

		if (clearRightImage) {
			rightImage.setImageDrawable(null);
			rightImage.setVisibility(GONE);
		}

		// Handle left image
		if (props.containsKey("leftImage")) {
			String path = TiConvert.toString(props, "leftImage");
			String url = tiContext.resolveUrl(null, path);

			Drawable d = loadDrawable(url);
			if (d != null) {
				leftImage.setImageDrawable(d);
				leftImage.setVisibility(VISIBLE);
			}
		} else {
			leftImage.setImageDrawable(null);
			leftImage.setVisibility(GONE);
		}

		if (props.containsKey("height")) {
			if (!props.get("height").equals("auto")) {
				height = TiConvert.toInt(props, "height");
			}
		}
		
		if (rp.hasControls()) {
			refreshControls();
		} else {
			refreshOldStyleRow();
		}
	}

	protected boolean hasView(TiUIView view) {
		if (views == null) return false;
		for (TiUIView v : views) {
			if (v == view) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		int imageHMargin = 0;

		int leftImageWidth = 0;
		int leftImageHeight = 0;
		if (leftImage != null && leftImage.getVisibility() != View.GONE) {
			measureChild(leftImage, widthMeasureSpec, heightMeasureSpec);
			leftImageWidth = leftImage.getMeasuredWidth();
			leftImageHeight = leftImage.getMeasuredHeight();
			imageHMargin += LEFT_MARGIN;
		}

		int rightImageWidth = 0;
		int rightImageHeight = 0;
		if (rightImage != null && rightImage.getVisibility() != View.GONE) {
			measureChild(rightImage, widthMeasureSpec, heightMeasureSpec);
			rightImageWidth = rightImage.getMeasuredWidth();
			rightImageHeight = rightImage.getMeasuredHeight();
			imageHMargin += RIGHT_MARGIN;
		}

		int adjustedWidth = w - leftImageWidth - rightImageWidth - imageHMargin;
		//int adjustedWidth = w;

		if (content != null) {
			measureChild(content, MeasureSpec.makeMeasureSpec(adjustedWidth, wMode), heightMeasureSpec);
			if(hMode == MeasureSpec.UNSPECIFIED) {
				TableViewProxy table = ((TableViewRowProxy)item.proxy).getTable();
				int minRowHeight = 0;
				if (table != null && table.hasProperty("minRowHeight")) {
					minRowHeight = TiConvert.toInt(table.getProperty("minRowHeight"));
				}

				if (height == -1) {
					h = Math.max(h, Math.max(content.getMeasuredHeight(), Math.max(leftImageHeight, rightImageHeight)));
					h = Math.max(h, minRowHeight);
				} else {
					h = Math.max(minRowHeight, height);
				}
				if (DBG) {
					Log.d(LCAT, "Row content measure (" + adjustedWidth + "x" + h + ")");
				}
				measureChild(content, MeasureSpec.makeMeasureSpec(adjustedWidth, wMode), MeasureSpec.makeMeasureSpec(h, hMode));
			}
		}
		
		setMeasuredDimension(w, Math.max(h, Math.max(leftImageHeight, rightImageHeight)));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		int contentLeft = left;
		int contentRight = right;
		bottom = bottom - top;
		top = 0;

		int height = bottom - top;

		if (leftImage != null && leftImage.getVisibility() != GONE) {
			int w = leftImage.getMeasuredWidth();
			int h = leftImage.getMeasuredHeight();
			int leftMargin = LEFT_MARGIN;

			contentLeft += w + leftMargin;
			int offset = (height - h) / 2;
			leftImage.layout(left+leftMargin, top+offset, left+leftMargin+w, top+offset+h);
		}

		if (rightImage != null && rightImage.getVisibility() != GONE) {
			int w = rightImage.getMeasuredWidth();
			int h = rightImage.getMeasuredHeight();
			int rightMargin = RIGHT_MARGIN;

			contentRight -= w + rightMargin;
			int offset = (height - h) / 2;
			rightImage.layout(right-w-rightMargin, top+offset, right-rightMargin, top+offset+h);
		}

		if (hasControls) {
			contentLeft = left + LEFT_MARGIN;
			contentRight = right - RIGHT_MARGIN;
		}

		if (content != null) {
			content.layout(contentLeft, top, contentRight, bottom);
		}
	}

	private static String[] filteredProperties = new String[]{
		"backgroundImage", "backgroundColor"
	};
	private KrollDict filterProperties(KrollDict d)
	{
		if (d == null) return new KrollDict();
		
		KrollDict filtered = new KrollDict(d);
		for (int i = 0;i < filteredProperties.length; i++) {
			if (filtered.containsKey(filteredProperties[i])) {
				filtered.remove(filteredProperties[i]);
			}
		}
		return filtered;
	}

	@Override
	public boolean providesOwnSelector() {
		return true;
	}
	
	@Override
	public void release() {
		super.release();
		if (views != null) {
			for (TiUIView view : views) {
				view.release();
			}
			views = null;
		}
		if (content != null) {
			content.removeAllViews();
			content = null;
		}
		if (hasCheckDrawable != null) {
			hasCheckDrawable.setCallback(null);
			hasCheckDrawable = null;
		}
		if (hasChildDrawable != null) {
			hasChildDrawable.setCallback(null);
			hasChildDrawable = null;
		}
		
	}
}
