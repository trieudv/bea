package com.unarin.cordova.beacon;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import android.R;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.altbeacon.beacon.*;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import systena.co.jp.airtimer.*;

/**
 * Created by Tom on 01/06/2015.
 */
@SuppressLint("NewApi")
public class BackgroundBeaconService extends Service implements BootstrapNotifier {

	public BackgroundBeaconService() {
		super();
	}

	private final double START_DISTANCE = 2;
	private BeaconManager iBeaconManager;
	private RegionBootstrap regionBootstrap;
	private double distance;
	private boolean notifyFlag = true;
	
	public void onCreate() {
		super.onCreate();
		
		try {
			FileInputStream fileIn = openFileInput("ibeacon.txt");
			InputStreamReader InputRead = new InputStreamReader(fileIn);
			
			char[] inputBuffer = new char[100];
			String ibeaconInfo = "";
			int charRead;
			
			while ((charRead = InputRead.read(inputBuffer)) > 0) {
				String readstring = String.copyValueOf(inputBuffer, 0, charRead);
				ibeaconInfo += readstring;
			}
			String identifier = ibeaconInfo.split(",")[0];
			String uuid = ibeaconInfo.split(",")[1];
			Region region = new Region(identifier, Identifier.parse(uuid), null, null);
			regionBootstrap = new RegionBootstrap(this, region);
			
			startRangingBeacons(region);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onDestroy(){
		regionBootstrap.disable();
	}

	@Override
	public void didEnterRegion(Region region) {
		Log.d("com.unarin.cordova.beacon", "BackgroundBeaconService.didEnterRegion called!");
		startRangingBeacons(region);
	}

	@Override
	public void didExitRegion(Region region) {
		Log.d("com.unarin.cordova.beacon", "BackgroundBeaconService.didExitRegion called!");
		try {
			iBeaconManager.stopRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void didDetermineStateForRegion(int i, Region region) {
		Log.d("com.unarin.cordova.beacon", "BackgroundBeaconService.didDetermineStateForRegion called!");
	}

	@Override
	public Context getApplicationContext() {
		return this.getApplication().getApplicationContext();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public void startRangingBeacons(Region region) {
		try {
			distance = START_DISTANCE;
			notifyFlag = true;
			
			iBeaconManager = BeaconManager.getInstanceForApplication(this);
			iBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
			iBeaconManager.setBackgroundBetweenScanPeriod(1000);
			iBeaconManager.setBackgroundScanPeriod(1000);
			iBeaconManager.setRangeNotifier(new RangeNotifier() {
				@Override
				public void didRangeBeaconsInRegion(final Collection<Beacon> iBeacons, Region region) {
					for (Beacon beacon : iBeacons) {
						distance = beacon.getDistance();
						System.out.println("distance: " + String.valueOf(distance));
					}
					if (distance < START_DISTANCE) {
						if (notifyFlag) {
							Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
							
							NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext())
								    .setSmallIcon(0x7f020000)
								    .setContentTitle("AirTimer")
								    .setContentText("勤怠登録")
								    .setSound(alarmSound)
								    .setAutoCancel(true);
							Intent resultIntent = new Intent(getApplicationContext(), CordovaApp.class);
							PendingIntent resultPendingIntent = PendingIntent.getActivity(
							    	getApplicationContext(),
								    0,
								    resultIntent,
								    PendingIntent.FLAG_UPDATE_CURRENT
								);
							mBuilder.setContentIntent(resultPendingIntent);
							
							NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
							mNotifyMgr.notify(1, mBuilder.build());
							notifyFlag = false;
						}
					} else {
						notifyFlag = true;
					}
				}
			});
			iBeaconManager.startRangingBeaconsInRegion(region);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}