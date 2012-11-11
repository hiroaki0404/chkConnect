// $Id: Settings.java,v 1.4 2011/10/16 11:18:30 hiroaki-mac Exp hiroaki-mac $
package jp.group.home.android.chkConnect;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		NotificationManager notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifyMgr.cancelAll();

        addPreferencesFromResource(R.xml.settings);
        setCurrentSettings();
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

    /**
     * 指定された設定項目の現在値をSummaryにセットする
     * @param preferenceName	設定項目の名称(key)
     */
    private void setCurrentPreference(final String preferenceName) {
    	EditTextPreference edittext_preference = (EditTextPreference)getPreferenceScreen().findPreference(preferenceName);
    	final String currentValue = edittext_preference.getText();
    	if (currentValue == null) {return;}
    	StringBuilder txt = new StringBuilder(currentValue);
    	if (!preferenceName.equals("checkURL")) {
    		txt.append(" ").append(this.getString(R.string.unit_name));
    	}
    	edittext_preference.setSummary(txt.toString());
    }
    
    /**
     * すべての設定項目の現在値をSummaryにセットする
     */
    private void setCurrentSettings() {
    	for (final String preferenceName : new String[] {"checkInterval", "screenOffCheckInterval", "wifiOnDelay", "checkURL"}) {
    		setCurrentPreference(preferenceName);
    	}
    }
    
    /**
     * 設定項目の値を変更したとき、Summaryに反映させる
     * @param sharedPreferences
     * @param key
     */
    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
	    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,  String key) {
	    	setCurrentPreference(key);
	    }
    };
    
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
    }
     
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(listener);
    }
    
}