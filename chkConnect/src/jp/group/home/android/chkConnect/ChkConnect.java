// $Id: ChkConnect.java,v 1.5 2011/10/26 12:30:49 hiroaki-mac Exp hiroaki-mac $
package jp.group.home.android.chkConnect;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ChkConnect extends Activity {
	public long lastTime;
//	private BroadcastReceiver connectedReceiver = new ChkConnectReceiver();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);

        // 設定がされているか確認する
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
    	ChkConnectUtil util = new ChkConnectUtil();
        final boolean isSettings = (pref == null)? false:util.getSettings(getApplicationContext());
        if (false) {
        	// 画面のon/offが変化した時にキックする
//        	IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
//        	filter.addAction("android.net.wifi.STATE_CHANGE");
//        	this.getApplicationContext().registerReceiver(connectedReceiver, filter);
        	
        	// 接続をチェックする
//        	util.chkConnect(this.getApplicationContext(), util.getChkURL(), util.getInterval());
        	util.setNextLaunch(this.getApplicationContext(), util.getInterval());
        	finish();
        }else{
        	// 設定画面を呼び出す
			Intent intent = new Intent( ChkConnect.this, Settings.class );
			startActivity( intent );
			finish();
        }
    }

    @Override
    protected void onDestroy() {
//    	if (null != connectedReceiver) {
//    		this.getApplicationContext().unregisterReceiver(connectedReceiver); // これがエラーの原因
 //   		connectedReceiver = null;
            Intent intent = new Intent(this, ChkConnectReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        	AlarmManager aMgr = (AlarmManager)(getSystemService(Context.ALARM_SERVICE));
    		aMgr.cancel(pendingIntent);
  //  	}
    	super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu( menu );

        MenuInflater inflater = getMenuInflater();

        inflater.inflate( R.menu.menu, menu );
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
    	switch ( item.getItemId() )
    	{
    		case R.id.item1:
    		{
    			Intent intent = new Intent( this, Settings.class );
    			startActivity( intent );
    			return true;
    		}
    	}
    	return super.onOptionsItemSelected(item);
    }

    private boolean tryConnect_old(final int timeout, final URI chkURL) {
    	ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = cm.getActiveNetworkInfo();
    	if (netInfo == null) {
    		return false;
    	}
    	if (!netInfo.isConnected()) {
    		return false;
    	}
    	// つながっているけど、通信できるか？
    	HttpClient client = new DefaultHttpClient();
    	HttpHead chkMethod = new HttpHead();
    	chkMethod.setURI(chkURL);
    	HttpParams params = new BasicHttpParams();
    	params.setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, timeout*1000);
    	params.setIntParameter(AllClientPNames.SO_TIMEOUT, timeout*1000);
    	chkMethod.setParams(params);
    	try {
			HttpResponse res = client.execute(chkMethod);
			StatusLine statusLine = res.getStatusLine();
			if ((statusLine != null)&&(statusLine.getStatusCode() == HttpStatus.SC_OK)) {
				return true;
			}
		} catch (ClientProtocolException e) {
//			disconnectNetwork(netInfo);
		} catch (IOException e) {
//			disconnectNetwork(netInfo);
		}
    	return false;
    }
    
/*
    private void disconnectNetwork(NetworkInfo netInfo) {
    	String extInfo = netInfo.getExtraInfo();
    	Intent intent = new Intent();
    	intent.setAction("android.intent.action.DATA_CONNECTION_FAILED");
    	if ((extInfo != null) && !extInfo.equals("")) {
    		intent.putExtra("apn", extInfo);
    	}
    	sendBroadcast(intent);
    } */

}
