// $Id: ChkConnectUtil.java,v 1.5 2011/10/26 12:34:11 hiroaki-mac Exp hiroaki-mac $
/**
 * 
 */
package jp.group.home.android.chkConnect;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

/**
 * @author hiroaki0404@gmail.com
 *
 */
public class ChkConnectUtil {
	static final String FILENAME = "connect.log";
	 volatile boolean isForceEnd = false;
	/**
	 * 指定されたuriにアクセスし、正常終了するかどうかを返す
	 * @param retry	試行回数
	 * @param url	アクセス先
	 * @return		true: 正常終了(status 200) / false: 異常終了
	 */
	private int tryConnect(final int retry, final URI uri) {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpParams httpParams = httpclient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 2000); /* OnReceive()でのsleep()より短くする */
		HttpConnectionParams.setSoTimeout(httpParams, 2000); /* OnReceive()でのsleep()より短くする */
		HttpClientParams.setRedirecting(httpParams, false);
		
//		boolean retCode = false;
		HttpHead request = new HttpHead(uri);
		int statusCode = -1;
		isForceEnd = false;
		
		for (int i = 0; i < retry; i++) {
			if (isForceEnd) {
				break;
			}
			try {
				statusCode = httpclient.execute(request, new ResponseHandler<Integer>() {
					public Integer handleResponse(HttpResponse response) {
						return response.getStatusLine().getStatusCode();
					}
				});
			}catch(IOException e){
				final String errMsg = e.getMessage();
				Log.d("chkConnect", (errMsg == null)?"error": errMsg);
				statusCode = -2; // debug
				// Do nothing. Try again.
			}catch(Exception e){
				// Unexpected exception. Return as fail.
				final String errMsg = e.getMessage();
				Log.d("chkConnect", (errMsg == null)?"error": errMsg);
				statusCode = -3; // debug
				break;
			}
		}
		httpclient.getConnectionManager().shutdown();
		return statusCode;
	}
	
	/**
	 * Wifi接続を切断する
	 * @param context	コンテキスト
	 * @return	切断結果
	 */
	public boolean disconnectWifi(Context context) {
		WifiManager mgr = (WifiManager)(context.getSystemService(Context.WIFI_SERVICE));
		final long now = (new Date()).getTime();
		SharedPreferences sp;
		sp = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
		final long interval = sp.getLong("interval", 0L);
		final long disconnect = sp.getLong("disconnect", 0L);
		if (now - disconnect > interval) {
			Editor ed = sp.edit();
			ed.putLong("disconnect", now);
			ed.commit();
			return mgr.disconnect();
		}
		return false;
	}

	/**
	 * 接続をチェックする
	 * @param context
	 * @param chkURL	チェック時にアクセスしてみるURL
	 * @param interval	次にチェックするまでの間隔
	 * @param timeLimit	最大処理時間
	 * @return
	 */
	public boolean chkConnect(Context context, final URI chkURL, final long interval, final long timeLimit) {
		if (interval <= 0L) {
			notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_error));
			return false;
		}
		Log.d("chkConnect", "chkConnect");
//		notify(context, "chkConnect");
		// Wifi接続中？
		WifiManager wMgr = (WifiManager)(context.getSystemService(Context.WIFI_SERVICE));
		WifiInfo info = wMgr.getConnectionInfo();
		if ((info != null)&&(info.getSSID() != null)) {
			// Wifi接続している?
			if (info.getSupplicantState() != SupplicantState.COMPLETED) {
				setNextLaunch(context, getInterval());
	    		notify(context, R.drawable.icon, context.getString(R.string.ntfy_conn));
				return true;
			}
			// サイトに接続できるかチェックする
			Log.d("chkConnect", "Start checking");
			final int statusCode = tryConnect(3, chkURL); // 時間がかかる可能性がある
			final boolean connectStatus = statusCode == HttpStatus.SC_OK;
			Log.d("chkConnect", "Check result:" + Integer.valueOf(statusCode));
			SharedPreferences sp;
			sp = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
			
			long delay = sp.getLong("interval", interval);
			if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
				delay = delay + interval/5; // 302だった場合、どんどん遅くしていく
				if (delay > interval * 3) {
					delay = interval;
				}
			} else if ((statusCode > 0)&&(statusCode < 299)) {
				delay = interval; // XXX 遅くしていった値を戻す。-1の時、もどす？
			}
			setNextLaunch(context, delay);
	    	if (!connectStatus) {
	    		notify(context, R.drawable.icon, context.getString(R.string.ntfy_discn) + " " + Integer.toString(statusCode));
	    		try {
					Thread.sleep(timeLimit); // 待たないで切断すると、接続イベントに対する処理が溜まってしまう
				} catch (InterruptedException e) {
				}
	    		notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_nowifi));
	    		disconnectWifi(context);
	    	}else{
	    		notify(context, R.drawable.okwifi, context.getString(R.string.ntfy_alive) + " " + Integer.toString(statusCode));
		    	Log.d("chkConnect", "Connection alive");
	    	}
			Editor ed = sp.edit();
			ed.putLong("interval", delay);
			ed.commit();

	    	return true;
		} else {
			// 接続していない
			Log.d("chkConnect", "No wifi connection");
    		notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_nowifi));
			return false;
		}
	}
	
	/**
	 * 次の呼び出しをセットする
	 * @param context	コンテキスト
	 * @param interval	次に呼び出すまでの時間
	 * @return true: セットした / false: 次の呼び出しが0だった。セットしなかった。
	 */
	public boolean setNextLaunch(Context context, final long interval) {
		Log.d("chkConnect", "Set Next " + Long.toString(interval)+ "sec later");
		if (interval <= 0L) {
			notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_error));
			logging(context, "Invalid interval value");
			return false;
		}
		cancelNext(context);
		// 一定時間後に起動するようタイマーセット
    	AlarmManager aMgr = (AlarmManager)(context.getSystemService(Context.ALARM_SERVICE));
    	long nextTime = SystemClock.elapsedRealtime() + interval * 1000;
    	Intent intent = new Intent(context, ChkConnectReceiver.class);
    	intent.putExtra("time", (new Date()).getTime());
    	PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	aMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextTime, pendingIntent);
    	return true;
	}

	/**
	 * セットされている呼び出しを取り消す
	 * @param context	コンテキスト
	 */
	public void cancelNext(final Context context) {
		Intent cancelIntent = new Intent(context.getApplicationContext(), ChkConnectReceiver.class);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		AlarmManager aMgr = (AlarmManager)(context.getSystemService(Context.ALARM_SERVICE));
		aMgr.cancel(pendingIntent);
	}

	/**
	 * 設定状態確認
	 * @param context	コンテキスト
	 * @return true:設定あり false:設定なし
	 */
	public boolean getSettings(Context context) {
		// 設定がされているか確認する
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isSettings = false;
        if (pref != null) {
        	String strInterval = "", strOffInterval = "", strWifiDelay = "", strURL = "";
        	try {
        		strInterval = pref.getString("checkInterval", "");
        		strOffInterval = pref.getString("screenOffCheckInterval", "");
        		strWifiDelay = pref.getString("wifiOnDelay", "");
        		strURL = pref.getString("checkURL", "");
        	}catch(ClassCastException e){
        		// 設定されていないことにする
        	}
        	if (strInterval.equals("") || strURL.equals("")) {
        		isSettings = false;
        	}else{
        		try {
        			interval = Long.parseLong(strInterval);
            		isSettings = true;
        		}catch(NumberFormatException e){
        			// 設定されていないことにする。
        			isSettings = false;
        		}
        		try {
        			screenOffInterval = Long.parseLong(strOffInterval);
        		}catch(NumberFormatException e){
        			screenOffInterval = 3600L;
        		}
        		try {
        			wifiDelay = Long.parseLong(strWifiDelay);
        		}catch(NumberFormatException e){
        			if (isSettings) {
        				wifiDelay = interval;
        			}
        		}
        		try {
					chkURL = new URI(strURL);
				} catch (URISyntaxException e) {
        			// 設定されていないことにする。
        			isSettings = false;
				}
        	}
        }
        return isSettings;
	}
	
	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public long getScreenOffInterval() {
		return screenOffInterval;
	}

	public void setScreenOffInterval(long screenOffInterval) {
		this.screenOffInterval = screenOffInterval;
	}

	public URI getChkURL() {
		return chkURL;
	}

	public void setChkURL(URI chkURL) {
		this.chkURL = chkURL;
	}

	public long getWifiDelay() {
		return wifiDelay;
	}

	public void setWifiDelay(long wifiDelay) {
		this.wifiDelay = wifiDelay;
	}

	private long interval;
	private long screenOffInterval;
	private long wifiDelay;
	private URI chkURL;

	/**
	 * Wifi接続完了したか？
	 * @param intent
	 * @return true:接続完了 / false:完了していない、違うIntentなど
	 */
	public boolean isWifiConnected(Intent intent) {
		if (intent == null) {return false;}
		final String intentAction = intent.getAction();
		if (intentAction.equals("android.net.wifi.STATE_CHANGE")) {
			// 接続中か接続完了か、判断する
			Bundle extras = intent.getExtras();
			if (extras != null) {
				NetworkInfo info = extras.getParcelable(WifiManager.EXTRA_NETWORK_INFO);
		        if ((info != null)&& info.isConnected()) {
		        	// 繋がっている
		    		Log.d("chkConnect", "Connected");
		    		return true;
		        } else {
		    		Log.d("chkConnect", "Connecting");
		        	return false;
		        }
			} else {
				return false;
			}
		}
		return false;
	}
	/**
	 * 通知領域に通知する
	 * @param context	コンテキスト
	 * @param iconID	表示するアイコンのID
	 * @param msg		通知文字列
	 */
	public void notify(Context context, int iconID, final String msg) {
		NotificationManager notifyMgr = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification(iconID, msg, System.currentTimeMillis());
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		Intent intent = new Intent(context, Settings.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
		notification.setLatestEventInfo(context, "ChkConnect", msg, contentIntent);
		notifyMgr.notify(R.string.app_name, notification);
	}

	public boolean isForceEnd() {
		return isForceEnd;
	}

	public void setForceEnd(boolean isForceEnd) {
		this.isForceEnd = isForceEnd;
	}
	
	/**
	 * debug用ログ出力(File)
	 * @param context
	 * @param str 出力内容
	 */
	public void logging(Context context, final String str) {
		Log.d("chkConnect", str);
		Time time = new Time("Asia/Tokyo");
		time.setToNow();
		final String logMsg = time.toString() + " " + str;
		
	    // ストリームを開く
	    FileOutputStream output;
		try {
			output = context.openFileOutput(FILENAME, Context.MODE_WORLD_READABLE);
			output.write(logMsg.getBytes());
			output.close();
		} catch (FileNotFoundException e) {
			notify(context, R.drawable.nowifi, "Log file not found.");
			Log.d("chkConnect", "Log file not found.");
		} catch (IOException e) {
			notify(context, R.drawable.nowifi, "Log file access error.");
			Log.d("chkConnect", "Log file access error.");
		}
	}
}
