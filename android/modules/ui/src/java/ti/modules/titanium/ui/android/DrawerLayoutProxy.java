/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2017 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.android;

import ti.modules.titanium.ui.widget.TiUIDrawerLayout;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.os.Message;
import android.support.v4.widget.DrawerLayout;

@Kroll.proxy(creatableInModule = AndroidModule.class)
public class DrawerLayoutProxy extends TiViewProxy
{
    @Kroll.constant public static final int LOCK_MODE_LOCKED_CLOSED = DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
    @Kroll.constant public static final int LOCK_MODE_LOCKED_OPEN = DrawerLayout.LOCK_MODE_LOCKED_OPEN;
    @Kroll.constant public static final int LOCK_MODE_UNLOCKED = DrawerLayout.LOCK_MODE_UNLOCKED;
    @Kroll.constant public static final int LOCK_MODE_UNDEFINED = DrawerLayout.LOCK_MODE_UNDEFINED;

    private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

    private static final int MSG_OPEN_LEFT = MSG_FIRST_ID + 200;
    private static final int MSG_CLOSE_LEFT = MSG_FIRST_ID + 201;
    private static final int MSG_TOGGLE_LEFT = MSG_FIRST_ID + 202;
    private static final int MSG_OPEN_RIGHT = MSG_FIRST_ID + 203;
    private static final int MSG_CLOSE_RIGHT = MSG_FIRST_ID + 204;
    private static final int MSG_TOGGLE_RIGHT = MSG_FIRST_ID + 205;

    private static final String TAG = "DrawerLayoutProxy";

    private TiUIDrawerLayout drawer;

    public DrawerLayoutProxy() {
        super();
    }

    @Override
    public TiUIView createView(Activity activity) {
        drawer = new TiUIDrawerLayout(this);
        drawer.getLayoutParams().autoFillsHeight = true;
        drawer.getLayoutParams().autoFillsWidth = true;
        return drawer;
    }

    @Override
    public boolean handleMessage(Message msg) 
    {
        AsyncResult result = null;
        
        switch(msg.what) {
            case MSG_OPEN_LEFT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.openLeft();
                }
                result.setResult(null);
                return true;
            }
            case MSG_CLOSE_LEFT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.closeLeft();
                }
                result.setResult(null);
                return true;
            }
            case MSG_TOGGLE_LEFT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.toggleLeft();
                }
                result.setResult(null);
                return true;
            }
            case MSG_OPEN_RIGHT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.openRight();
                }
                result.setResult(null);
                return true;
            }
            case MSG_CLOSE_RIGHT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.closeRight();
                }
                result.setResult(null);
                return true;
            }
            case MSG_TOGGLE_RIGHT: {
                result = (AsyncResult) msg.obj;
                if (drawer != null) {
                    drawer.toggleRight();
                }
                result.setResult(null);
                return true;
            }
            
            default : {
                return super.handleMessage(msg);
            }
        }
    }

    @Kroll.method
    public void toggleLeft() {
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.toggleLeft();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_TOGGLE_LEFT));
        }
    }

    @Kroll.method
    public void openLeft() {        
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.openLeft();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_OPEN_LEFT));
        }
    }

    @Kroll.method
    public void closeLeft() {
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.closeLeft();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_CLOSE_LEFT));
        }
    }

    @Kroll.method
    public void toggleRight() {
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.toggleRight();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_TOGGLE_RIGHT));
        }
    }

    @Kroll.method
    public void openRight() {
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.openRight();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_OPEN_RIGHT));
        }
    }

    @Kroll.method
    public void closeRight() {
        if (TiApplication.isUIThread()) {
            if (drawer != null) {
                drawer.closeRight();
            }
        } else {
            TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(
                    MSG_CLOSE_RIGHT));
        }
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getIsLeftOpen() {
        return drawer != null && drawer.isLeftOpen();
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getIsRightOpen() {
        return drawer != null && drawer.isRightOpen();
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getIsLeftVisible() {
        return drawer != null && drawer.isLeftVisible();
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getIsRightVisible() {
        return drawer != null && drawer.isRightVisible();
    }

    @Kroll.method
    @Kroll.setProperty
    public void setLeftWidth(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_LEFT_WIDTH, arg);
    }

    @Kroll.method
    @Kroll.setProperty
    public void setLeftView(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_LEFT_VIEW, arg);
    }

    @Kroll.method
    @Kroll.setProperty
    public void setRightWidth(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_RIGHT_WIDTH, arg);
    }

    @Kroll.method
    @Kroll.setProperty
    public void setRightView(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_RIGHT_VIEW, arg);
    }

    @Kroll.method
    @Kroll.setProperty
    public void setCenterView(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_CENTER_VIEW, arg);
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getDrawerIndicatorEnabled() {
        if (hasProperty(TiC.PROPERTY_DRAWER_INDICATOR_ENABLED)) {
            return (Boolean) getProperty(TiC.PROPERTY_DRAWER_INDICATOR_ENABLED);
        }
        return true;
    }

    @Kroll.method
    @Kroll.setProperty
    public void setDrawerIndicatorEnabled(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_DRAWER_INDICATOR_ENABLED, arg);
    }

    @Kroll.method
    @Kroll.getProperty
    public int getDrawerLockMode() {
        if (hasProperty(TiC.PROPERTY_DRAWER_LOCK_MODE)) {
            return (Integer) getProperty(TiC.PROPERTY_DRAWER_LOCK_MODE);
        }
        return LOCK_MODE_UNDEFINED;
    }

    @Kroll.method
    @Kroll.setProperty
    public void setDrawerLockMode(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_DRAWER_LOCK_MODE, arg);
    }

    @Kroll.method
    public void interceptTouchEvent (TiViewProxy view, Boolean disallowIntercept){
        view.getOrCreateView().getOuterView().getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    @Kroll.method
    @Kroll.getProperty
    public boolean getToolbarEnabled() {
        if (hasProperty(TiC.PROPERTY_TOOLBAR_ENABLED)) {
            return (Boolean) getProperty(TiC.PROPERTY_TOOLBAR_ENABLED);
        }
        return true;
    }

    @Kroll.method
    @Kroll.setProperty
    public void setToolbarEnabled(Object arg) {
        setPropertyAndFire(TiC.PROPERTY_TOOLBAR_ENABLED, arg);
    }
}
