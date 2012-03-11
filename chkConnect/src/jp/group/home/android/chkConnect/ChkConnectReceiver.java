// $Id$
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
import android.content.SharedPreferences;
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
		ChkConnectUtil util = new ChkConnectUtil();
		util.cancelNext(context);
		Log.d("chkConnect", "Reset next launch");
		final String intentAction = intent.getAction();
		final long lastSentTime = intent.getLongExtra("time", 0);
		if (intentAction != null) {
			Log.d("chkConnect", intentAction);
			if (intentAction.equals(Intent.ACTION_USER_PRESENT)||util.isWifiConnected(intent)) {
	    		if (!util.getSettings(context)) {
					util.notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_conn));
	    			return;
	    		}
	    		long interval = util.getInterval();
	    		if (intentAction.equals("android.net.wifi.STATE_CHANGE")) {
	    			util.notify(context, R.drawable.icon, "Set check timer for the Wifi on");
	    			interval = util.getWifiDelay();
	    		}
	    		util.setNextLaunch(context.getApplicationContext(), interval);
	    		return;
/*    		} else {
    			// タイマーで呼ばれた
    			Bundle bundle = intent.getExtras();
    			if (bundle != null) {
	    			final long prevTime = bundle.getLong("time");
	    			final long now = (new Date()).getTime();
	    			if (now - prevTime < util.getInterval()) {
	    				return;
	    			}
    			} */
			}
		} else {
			util.notify(context, R.drawable.nowifi, (0L == lastSentTime)? "intent is null": "timer");
			Log.d("chkConnect", "intentAction is NULL");
		}
		// intentの送信時間がセットされていなければ、timer起動ではないから接続チェックはしない
		if (0L == lastSentTime) {
			return;
		}
		// 接続確認のthreadをキックする。イベント処理ルーチンで余計な時間をかけない。
		(new Thread(new Runnable() {
			public void run() {
				final ChkConnectUtil util = new ChkConnectUtil();
				util.notify(context, R.drawable.icon, context.getString(R.string.ntfy_conn)); // Will remove soon
				if (util.getSettings(context)) {
					// Screen on/offで間隔を変える
					PowerManager pwMgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					final URI uri = util.getChkURL();
					final long interval = pwMgr.isScreenOn()?util.getInterval():util.getScreenOffInterval();
					final long now = (new Date()).getTime();
					if (now - lastSentTime < interval) {
						return;
					}
					SharedPreferences sp;
					sp = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
					final long delay = sp.getLong("interval", interval);

					Thread chkThread = new Thread(new Runnable() {
						public void run() {
							util.chkConnect(context, uri, delay);
						}
					});
					chkThread.start();
					// waitする
					try {
						Thread.sleep(delay*1000-200);
					} catch (InterruptedException e) {
						util.notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_error));
						chkThread.interrupt();
						// TODO 自動生成された catch ブロック
						Log.d("chiConnect", e.getMessage());
						e.printStackTrace();
					}
					// まだthreadが生きているということは、接続できていないこと。
					if (chkThread.isAlive()) {
						util.setForceEnd(true);
						chkThread.interrupt();
						util.disconnectWifi(context);
						util.notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_timeout));
						util.setNextLaunch(context, interval);
					}
				} else {
		        	// 設定がおかしくなったので、終了する
					util.notify(context, R.drawable.nowifi, context.getString(R.string.ntfy_seterror));
				}
			}
		})).start();
	}
}
