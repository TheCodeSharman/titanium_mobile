/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.appcelerator.titanium.util.Log;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;

public class TiCamera
{
	private static final String LCAT = "TiCamera";
	private static Camera camera;

	public TiCamera()
	{
		if (camera == null) {
			Log.i(LCAT, "camera created");
			camera = Camera.open();
		}
	}

	public Camera getCamera()
	{
		return camera;
	}

	// add some fancy click noise here in the future
	ShutterCallback shutterCallback = new ShutterCallback()
	{
		public void onShutter()
		{
			Log.i(LCAT, "Pretend you heard a 'click' sound");
		}
	};

	// need the callback but we don't want to do anything with this currently
	PictureCallback rawCallback = new PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			Log.i(LCAT, "raw picture available");
		}
	};

	PictureCallback jpegCallback = new PictureCallback()
	{
		public void onPictureTaken(byte[] data, Camera camera)
		{
			FileOutputStream outputStream = null;
			try {
				String photosPath = "/sdcard/CameraTest/photos/";

				// create storage location for photos if it does not exist
				File photosDirectory = new File(photosPath);
				if(!(photosDirectory.exists())) {
					photosDirectory.mkdirs();
				}

				// write photo to storage
				outputStream = new FileOutputStream(String.format(photosPath + "%d.jpg", System.currentTimeMillis()));
				outputStream.write(data);
				outputStream.close();

				// camera preview stops when a photo is actually taken, restart it
				camera.startPreview();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	};
}

