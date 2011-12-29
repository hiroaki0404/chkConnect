// $Id: Settings.java,v 1.4 2011/10/16 11:18:30 hiroaki-mac Exp hiroaki-mac $
package jp.group.home.android.chkConnect;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		NotificationManager notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMgr.cancelAll();

        addPreferencesFromResource(R.xml.settings);
    }

    @Override
    protected void onDestroy() {
    	ChkConnectUtil util = new ChkConnectUtil();
    	if (util.getSettings(this)) {
    		final long interval = util.getInterval();
    		if (interval >= 0L) {
    			util.setNextLaunch(this, interval);
				SharedPreferences sp;
				sp = this.getSharedPreferences(this.getString(R.string.app_name), Context.MODE_PRIVATE);
				Editor ed = sp.edit();
				ed.putLong("interval", interval);
				ed.commit();

    		}
    	}
    	super.onDestroy();
    }

}