/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.media;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiContext.OnLifecycleEvent;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;

import ti.modules.titanium.filesystem.FileProxy;
import android.app.Activity;

@Kroll.proxy(creatableInModule=MediaModule.class)
public class SoundProxy extends KrollProxy
	implements OnLifecycleEvent
{
	private static final String LCAT = "SoundProxy";
	private static final boolean DBG = TiConfig.LOGD;

	protected TiSound snd;

	public SoundProxy(TiContext tiContext)
	{
		super(tiContext);
		tiContext.addOnLifecycleEventListener(this);
		setProperty("volume", 0.5, true);
	}
	
	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
		if (options.containsKey("url")) {
			setProperty("url", getTiContext().resolveUrl(null, TiConvert.toString(options, "url")));
		} else if (options.containsKey("sound")) {
			FileProxy fp = (FileProxy) options.get("sound");
			if (fp != null) {
				String url = fp.getNativePath();
				setProperty("url", url);
			}
		}
		if (options.containsKey("allowBackground")) {
			setProperty("allowBackground", options.get("allowBackground"));
		}
		if (DBG) {
			Log.i(LCAT, "Creating sound proxy for url: " + TiConvert.toString(getProperty("url")));
		}
	}

	@Kroll.method @Kroll.getProperty
	public boolean isPlaying() {
		TiSound s = getSound();
		if (s != null) {
			return s.isPlaying();
		}
		return false;
	}

	@Kroll.method @Kroll.getProperty
	public boolean isPaused() {
		TiSound s = getSound();
		if (s != null) {
			return s.isPaused();
		}
		return false;
	}

	@Kroll.method @Kroll.getProperty
	public boolean isLooping() {
		TiSound s = getSound();
		if (s != null) {
			return s.isLooping();
		}
		return false;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setLooping(boolean looping) {
		TiSound s = getSound();
		if (s != null) {
			s.setLooping(looping);
		}
	}

	@Kroll.method
	// An alias for play so that sound can be used instead of an audioplayer
	public void start() {
		play();
	}

	@Kroll.method
	public void play() {
		TiSound s = getSound();
		if (s != null) {
			s.play();
		}
	}

	@Kroll.method
	public void pause() {
		TiSound s = getSound();
		if (s != null) {
			s.pause();
		}
	}

	@Kroll.method
	public void reset() {
		TiSound s = getSound();
		if (s != null) {
			s.reset();
		}
	}

	@Kroll.method
	public void release() {
		TiSound s = getSound();
		if (s != null) {
			s.release();
			snd = null;
		}
	}

	@Kroll.method
	public void destroy() {
		release();
	}

	@Kroll.method
	public void stop() {
		TiSound s = getSound();
		if (s != null) {
			s.stop();
		}
	}

	@Kroll.method @Kroll.getProperty
	public int getDuration() {
		TiSound s = getSound();
		if (s != null) {
			return s.getDuration();
		}

		return 0;
	}

	@Kroll.method @Kroll.getProperty
	public int getTime() {
		TiSound s = getSound();
		if (s != null) {
			return s.getTime();
		}
		return 0;
	}

	@Kroll.method @Kroll.setProperty
	public void setTime(Object pos) {
		if (pos != null) {
			TiSound s = getSound();
			if (s != null) {
				s.setTime(TiConvert.toInt(pos));
			}
		}
	}
	
	protected TiSound getSound()
	{
		if (snd == null) {
			snd = new TiSound(this);
			setModelListener(snd);
		}
		return snd;
	}

	private boolean allowBackground() {
		boolean allow = false;
		if (hasProperty("allowBackground")) {
			allow = TiConvert.toBoolean(getProperty("allowBackground"));
		}
		return allow;
	}

	public void onStart(Activity activity) {
	}

	public void onResume(Activity activity) {
		if (!allowBackground()) {
			if (snd != null) {
				snd.onResume();
			}
		}
	}

	public void onPause(Activity activity) {
		if (!allowBackground()) {
			if (snd != null) {
				snd.onPause();
			}
		}
	}

	public void onStop(Activity activity) {
	}

	public void onDestroy(Activity activity) {
		if (snd != null) {
			snd.onDestroy();
		}
		snd = null;
	}


}
