// $Id: ChkConnect.java,v 1.5 2011/10/26 12:30:49 hiroaki-mac Exp hiroaki-mac $
package jp.group.home.android.chkConnect;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class ChkConnect extends Activity {
	public long lastTime;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    	// 設定画面を呼び出す
		Intent intent = new Intent( ChkConnect.this, Settings.class );
		startActivity( intent );
		finish();
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, ChkConnectReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    	AlarmManager aMgr = (AlarmManager)(getSystemService(Context.ALARM_SERVICE));
		aMgr.cancel(pendingIntent);
    	Log.d("chkConnect", "onDestroy");
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
 }
