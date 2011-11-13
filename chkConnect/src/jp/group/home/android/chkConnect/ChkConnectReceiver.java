// $Id: ChkConnectReceiver.java,v 1.4 2011/10/26 12:32:33 hiroaki-mac Exp hiroaki-mac $
/**
 * 
 */
package jp.group.home.android.chkConnect;

import java.net.URI;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

/**
 * @author hiroaki0404@gmail.com
 *
 */
public class ChkConnectReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(final Context context, Intent intent) {
		Log.d("chkConnect", "ChkConnectReceiver::onReceive");
		final String intentAction = intent.getAction();
		if (intentAction != null) {
			Log.d("chkConnect", intentAction);
		}
		if (intentAction != null) {
    		ChkConnectUtil util = new ChkConnectUtil();
			if (intentAction.equals(Intent.ACTION_USER_PRESENT)||util.isWifiConnected(intent)) {
				// タイマー以外で呼ばれた場合、タイマー起動をリセット。
	            Intent cancelIntent = new Intent(context.getApplicationContext(), ChkConnectReceiver.class);
	            PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
	        	AlarmManager aMgr = (AlarmManager)(context.getSystemService(Context.ALARM_SERVICE));
	    		aMgr.cancel(pendingIntent);
	    		Log.d("chkConnect", "Reset next launch");
	    		if (!util.getSettings(context)) {
	    			return;
	    		}
	    		long interval = util.getInterval();
	    		if (intentAction.equals("android.net.wifi.STATE_CHANGE")) {
	    			interval = util.getWifiDelay();
	    		}
	    		if (util.setNextLaunch(context.getApplicationContext(), interval)) {
	    			return;
	    		}
    		} else {
    			// タイマーで呼ばれた
    			Bundle bundle = intent.getExtras();
    			if (bundle != null) {
	    			final long prevTime = bundle.getLong("time");
	    			final long now = (new Date()).getTime();
	    			if (now - prevTime < util.getInterval()) {
	    				return;
	    			}
    			}
			}
		} else {
			Log.d("chkConnect", "intentAction is NULL");
		}
		(new Thread(new Runnable() {
			public void run() {
				final ChkConnectUtil util = new ChkConnectUtil();
				util.notify(context, R.drawable.icon, context.getString(R.string.ntfy_conn)); // Will remove soon
				if (util.getSettings(context)) {
					// Screen on/offで間隔を変える
					PowerManager pwMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					final URI uri = util.getChkURL();
					final long interval = pwMgr.isScreenOn()?util.getInterval():util.getScreenOffInterval();
					Thread chkThread = new Thread(new Runnable() {
						public void run() {
							util.chkConnect(context, uri, interval);
						}
					});
					chkThread.start();
					// waitする
					try {
						Thread.sleep(util.getInterval()*1000);
					} catch (InterruptedException e) {
						util.notify(context, R.drawable.icon, context.getString(R.string.ntfy_error));
						chkThread.interrupt();
						// TODO 自動生成された catch ブロック
						Log.d("chiConnect", e.getMessage());
						e.printStackTrace();
					}
					// まだthreadが生きているということは、接続できていないこと。
					if (chkThread.isAlive()) {
						chkThread.interrupt();
						util.disconnectWifi(context);
						util.notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_timeout));
						util.setNextLaunch(context, interval);
					}
				} else {
		        	// 設定がおかしくなったので、終了する
					util.notify(context, R.drawable.icon, context.getString(R.string.ntfy_seterror));
				}
			}
		})).start();
	}
}
