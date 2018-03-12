package ti.modules.titanium.ui.android;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class TiPreferencesFragment extends PreferenceFragment
{

	private static final String TAG = "TiPreferencesFragment";
	private CharSequence title = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		String prefsName = getArguments().getString(TiPreferencesActivity.PREFS_KEY);

		// Find the layout file, do nothing if not found
		try {
			getPreferenceManager().setSharedPreferencesName(TiApplication.APPLICATION_PREFERENCES_NAME);
			int resid = TiRHelper.getResource("xml." + prefsName);
			if (resid != 0) {
				addPreferencesFromResource(resid);
				if (getPreferenceScreen() != null) {
					title = getPreferenceScreen().getTitle();
				}
			} else {
				Log.e(TAG, "xml." + prefsName + " preferences not found.");
				return;
			}
		} catch (TiRHelper.ResourceNotFoundException e) {
			Log.e(TAG, "Error loading preferences: " + e.getMessage());
			return;
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (title != null && getActivity() != null) {
			getActivity().setTitle(title);
		}
	}
}
