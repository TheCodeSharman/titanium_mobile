/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.database;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DatabaseModule extends TiModule
{
	private static final String LCAT = "TiDatabase";
	private static final boolean DBG = TiConfig.LOGD;

	public DatabaseModule(TiContext tiContext) {
		super(tiContext);
	}

	public TiDatabaseProxy open(String name) {
		TiDatabaseProxy dbp = null;

		try {
			SQLiteDatabase db = getTiContext().getTiApp().openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
			if (DBG) {
				Log.d(LCAT, "Opened database: " + name);
			}

			dbp = new TiDatabaseProxy(getTiContext(), name, db);
		} catch (SQLException e) {
			String msg = "Error opening database: " + name + " msg=" + e.getMessage();
			Log.e(LCAT, msg, e);
			//TODO throw exception
		}

		return dbp;
	}

	public TiDatabaseProxy install(String url, String name)
	{
		try {
			Context ctx = getTiContext().getTiApp();
			for (String dbname : ctx.databaseList())
			{
				if (dbname.equals(name))
				{
					return open(name);
				}
			}
			// open an empty one to get the full path and then close and delete it
			File dbPath = ctx.getDatabasePath(name);

			if (DBG) {
				Log.d(LCAT,"db path is = "+dbPath);
				Log.d(LCAT,"db url is = "+url);
			}

			String path = getTiContext().resolveUrl(null, url);
			TiBaseFile srcDb = TiFileFactory.createTitaniumFile(getTiContext(), path, false);

			if (DBG) {
				Log.d(LCAT,"new url is = "+url);
			}

			InputStream is = null;
			OutputStream os = null;

			byte[] buf = new byte[8096];
			int count = 0;
			try
			{
				is = new BufferedInputStream(srcDb.getInputStream());
				os = new BufferedOutputStream(new FileOutputStream(dbPath));

				while((count = is.read(buf)) != -1) {
					os.write(buf, 0, count);
				}
			}
			finally
			{
				try { is.close(); } catch (Exception ig) { }
				try { os.close(); } catch (Exception ig) { }
			}

			return open(name);

		} catch (SQLException e) {
			String msg = "Error installing database: " + name + " msg=" + e.getMessage();
			Log.e(LCAT, msg, e);
			//TODO throw exception
		}
		catch (IOException e) {
			String msg = "Error installing database: " + name + " msg=" + e.getMessage();
			Log.e(LCAT, msg, e);
			//TODO throw exception
		}

		return null;
	}
}
