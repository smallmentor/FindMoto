/** ============================================================== */
package tw.edu.stust.slm.findmoto;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/** ============================================================== */
public class THLConfig
{
	final static String TAG_NET_ALLOW_3G= "NET_ALLOW_3G";
	
	Context mContext			= null;
	
	public boolean allow3G		= false;

	/** ================================================ */
	public THLConfig(Context context)
	{
		mContext= context;
	}
	
	/** ================================================ */
	public void loadSettings()
	{
		SharedPreferences sp= PreferenceManager.getDefaultSharedPreferences(mContext);
		
		/** */
		allow3G= sp.getBoolean(TAG_NET_ALLOW_3G, allow3G);
	}
	
	/** ================================================ */
	public void saveSettings()
	{
		SharedPreferences sp			= PreferenceManager.getDefaultSharedPreferences(mContext);
    	SharedPreferences.Editor edit	= sp.edit();
    	
    	edit.putBoolean(TAG_NET_ALLOW_3G, allow3G);
    	
		edit.commit();
	}
}

/** ============================================================== */

