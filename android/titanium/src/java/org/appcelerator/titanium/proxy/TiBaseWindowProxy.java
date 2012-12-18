package org.appcelerator.titanium.proxy;

import java.lang.ref.WeakReference;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.util.Pair;

/**
 * This class exists to allow JS wrapping of the abstract methods
 */
@Kroll.proxy
public class TiBaseWindowProxy extends TiWindowProxy
{
	private static final String TAG = "TiBaseWindow";
	
	private WeakReference<TiBaseActivity> hostActivity;
	
	private TiViewProxy mViewProxy;

	/**
	 * Called to associate a view with a JS window wrapper 
	 * 
	 * @param viewProxy			real view that the JS wrapper represents
	 */
	@Kroll.method
	public void setWindowView(TiViewProxy viewProxy) {
		TiUIView view = viewProxy.peekView();
		setView(view);
		setModelListener(view);
		mViewProxy = viewProxy;
	}
	
	@Kroll.method
	public void addSelfToStack() 
	{
		// adding window to stack
		Activity topActivity = TiApplication.getAppCurrentActivity();
		if (topActivity instanceof TiBaseActivity) {
			TiBaseActivity baseActivity = (TiBaseActivity)topActivity;
			hostActivity = new WeakReference<TiBaseActivity>(baseActivity);
			baseActivity.addWindowToStack(this);
		}
	}
	
	@Kroll.method
	public void removeSelfFromStack() 
	{
		// removing window from stack
		TiBaseActivity activity = (hostActivity != null) ? hostActivity.get() : null;
		if (activity != null) {
			activity.removeWindowFromStack(this);
		}
	}

	/**
	 * Returns the view that is wrapped by this object.  The caller is
	 * expected to check the return value for null
	 * 
	 * @return		view proxy that is wrapped by this object
	 * 
	 */
	public TiViewProxy getWrappedView() {
		return view != null ? view.getProxy() : null;
	}

	@Override
	public TiUIView getOrCreateView() {
		throw new IllegalStateException("Cannot create view on TiBaseWindowProxy");
	}

	@Override
	protected void handleOpen(KrollDict options)
	{
	}

	@Override
	protected void handleClose(KrollDict options)
	{
	}

	@Override
	protected Activity getWindowActivity()
	{
		return null;
	}
	
	@Override
	public void onPropertyChanged(String name, Object value) {
		//
		// Set on both proxies.  We must set on the viewProxy
		// first for things to work correctly.
		//
		if (mViewProxy != null) {
			
			String propertyName = name;
			Object newValue = value;

			if (isLocaleProperty(name)) {
				Log.i(TAG, "Updating locale: " + name, Log.DEBUG_MODE);
				Pair<String, String> update = updateLocaleProperty(name, value.toString());
				if (update != null) {
					propertyName = update.first;
					newValue = update.second;
				}
			}

			mViewProxy.setProperty(propertyName, newValue);
		}
		super.onPropertyChanged(name, value);
	}
	
	@Override 
	public void setProperty(String name, Object value) {
		//
		// Set on both proxies.  We must set on the viewProxy
		// first for things to work correctly.
		//
		if (mViewProxy != null) {
			mViewProxy.setProperty(name,  value);
		}
		super.setProperty(name, value);	
	}

}
