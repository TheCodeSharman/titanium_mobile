/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package org.appcelerator.titanium.util;

import java.util.Arrays;

import org.appcelerator.kroll.common.Log;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;

/**
 * Utility class for image manipulations.
 */
public class TiImageHelper
{
	private static final String TAG = "TiImageHelper";

	/**
	 * Add an alpha channel to the given image if it does not already have one.
	 * 
	 * @param image
	 *            the image to add an alpha channel to.
	 * @return a copy of the given image with an alpha channel.
	 */
	public static Bitmap imageWithAlpha(Bitmap image)
	{
		if (image == null) {
			return null;
		}
		if (image.hasAlpha()) {
			return image;
		}
		return image.copy(Bitmap.Config.ARGB_8888, true);
	}

	/**
	 * Create a copy of the given image with rounded corners and a transparent border around its edges.
	 * 
	 * @param image
	 *            the image to add rounded corners to.
	 * @param cornerRadius
	 *            the radius of the rounded corners.
	 * @param borderSize
	 *            the size of the border to be added.
	 * @return a copy of the given image with rounded corners and a transparent border.
	 */
	public static Bitmap imageWithRoundedCorner(Bitmap image, float cornerRadius, float borderSize)
	{
		if (image == null) {
			return null;
		}
		if (cornerRadius < 0 || borderSize < 0) {
			Log.e(TAG, "Invalid corner radius or border size for imageWithRoundedCorner");
			return image;
		}

		if (cornerRadius == 0) {
			return imageWithTransparentBorder(image, (int) borderSize);
		}

		int width = image.getWidth();
		int height = image.getHeight();
		Bitmap imageRoundedCorner = Bitmap.createBitmap(width + (int) (borderSize * 2), height + (int) (borderSize * 2),
			Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(imageRoundedCorner);

		Path clipPath = new Path();
		RectF imgRect = new RectF(borderSize, borderSize, width + borderSize, height + borderSize);

		float radii[] = new float[8];
		Arrays.fill(radii, cornerRadius);
		clipPath.addRoundRect(imgRect, radii, Direction.CW);

		// This still happens sometimes when hw accelerated so, catch and warn
		try {
			canvas.clipPath(clipPath);
		} catch (Exception e) {
			Log.e(TAG, "Unable to create the image with rounded corners. clipPath failed on canvas: " + e.getMessage());
			canvas.clipRect(imgRect);
		}

		canvas.drawBitmap(imageWithAlpha(image), borderSize, borderSize, null);
		return imageRoundedCorner;
	}

	/**
	 * Add a transparent border to the given image around its edges.
	 * 
	 * @param image
	 *            the image to add a transparent border to.
	 * @param borderSize
	 *            the size of the border to be added.
	 * @return a copy of the given image with a transparent border.
	 */
	public static Bitmap imageWithTransparentBorder(Bitmap image, int borderSize)
	{
		if (image == null) {
			return null;
		}
		if (borderSize < 0) {
			Log.e(TAG, "Invalid border size for imageWithTransparentBorder");
			return image;
		}

		if (borderSize == 0) {
			return image;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		Bitmap imageBorder = Bitmap.createBitmap(width + borderSize * 2, height + borderSize * 2, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(imageBorder);
		canvas.drawBitmap(imageWithAlpha(image), borderSize, borderSize, null);
		return imageBorder;
	}
}
