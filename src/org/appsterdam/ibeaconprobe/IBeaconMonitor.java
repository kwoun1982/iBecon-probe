package org.appsterdam.ibeaconprobe;

import java.util.Collection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.MonitorNotifier;
import com.radiusnetworks.ibeacon.RangeNotifier;
import com.radiusnetworks.ibeacon.Region;

public class IBeaconMonitor implements MonitorNotifier, RangeNotifier, IBeaconConsumer {
	static final private String TAG = "ibmonitor";


	static public interface IBeaconsListeners extends MonitorNotifier, RangeNotifier {
		void onIBeaconServiceConnect ();
	}

	final private IBeaconManager ibeaconManager;
	final private Activity activity;
	private Region monitorRegion = null;
	private Region rangingRegion = null;
	private IBeaconsListeners listener;

	public IBeaconMonitor (Activity activity) {
		this.activity = activity;
		this.ibeaconManager = IBeaconManager.getInstanceForApplication(activity);
		this.ibeaconManager.bind (this);
	}

	public void setListener (IBeaconsListeners listener) {
		this.listener = listener;
	}

	public void startMonotoring (String proximityUuid) {
		if (this.monitorRegion != null) {
			Log.i (TAG, "iBeacon monitor already started");
			return;
		}
		this.monitorRegion = new Region ("uniqueRegion", proximityUuid, null, null);
		try {
			//this.ibeaconManager.startMonitoringBeaconsInRegion(this.activeRegion);
			this.ibeaconManager.startRangingBeaconsInRegion (this.monitorRegion);
		} catch (RemoteException e) {
			Log.e (TAG, "Unable to start monitoring", e);
		}

	}

	public void stopMonotoring () {
		try {
			//this.ibeaconManager.stopMonitoringBeaconsInRegion (this.activeRegion);
			this.ibeaconManager.stopRangingBeaconsInRegion (this.monitorRegion);
			this.monitorRegion = null;
		} catch (RemoteException e) {
			Log.e (TAG, "Unable to stop monitoring", e);
		}
	}

	public void startRanging (String proximityUUID) {
		if (this.rangingRegion != null) {
			return;
		}
		this.rangingRegion = new Region ("rangingRegion",proximityUUID, null, null);
		try {
			this.ibeaconManager.startRangingBeaconsInRegion (this.rangingRegion);
		} catch (RemoteException e) {
			Log.e (TAG, "Unable to start ranging", e);
		}
	}

	public void stopRanging () {
		if (this.rangingRegion == null) {
			return;
		}
		try {
			this.ibeaconManager.stopRangingBeaconsInRegion (this.rangingRegion);
			this.rangingRegion = null;
		} catch (RemoteException e) {
			Log.e (TAG, "Unable to stop ranging", e);
		}
	}

	public void unbind () {
		this.ibeaconManager.unBind (this);
	}

	@Override
	public void didExitRegion (final Region region) {
		Log.i (TAG, "A iBeacon left!");
		if (this.listener != null) {
			System.out.println ("Exit " + region.getUniqueId ());
			this.activity.runOnUiThread (new Runnable() {
				@Override
				public void run () {
					IBeaconMonitor.this.listener.didExitRegion (region);
				}
			});
			
		}
	}
	
	@Override
	public void didEnterRegion (final Region region) {
		Log.i (TAG, "I see a iBeacon!");
		if (this.listener != null) {
			System.out.println ("Enter " + region.getUniqueId ());
			this.activity.runOnUiThread (new Runnable() {
				@Override
				public void run () {
					IBeaconMonitor.this.listener.didEnterRegion (region);
				}
			});
		}
	}
	
	@Override
	public void didDetermineStateForRegion (final int arg0, final Region arg1) {
		Log.i (TAG, "Switching seeing / not seeing iBeacons!");
		if (this.listener != null) {
			this.activity.runOnUiThread (new Runnable() {

				@Override
				public void run () {
					IBeaconMonitor.this.listener.didDetermineStateForRegion (arg0, arg1);
				}
			});
			
		}
	}

	@Override
	public void didRangeBeaconsInRegion (final Collection<IBeacon> iBeacons, final Region region) {
		Log.i (TAG, String.format ("Found %d ibeconds in range", iBeacons.size ()));
		if (this.listener != null) {
			this.activity.runOnUiThread (new Runnable () {
				@Override
				public void run () {
					IBeaconMonitor.this.listener.didRangeBeaconsInRegion (iBeacons, region);
				}});
		}
	}

	@Override
	public Context getApplicationContext () {
		return this.activity.getApplicationContext ();
	}

	@Override
	public boolean bindService (Intent service, ServiceConnection conn, int flags) {
		return this.activity.bindService (service, conn, flags);
	}

	@Override
	public void unbindService (ServiceConnection conn) {
		this.activity.unbindService (conn);
	}

	@Override
	public void onIBeaconServiceConnect () {
		Log.i (TAG, "Connecting to the IBeacon service.");
		this.ibeaconManager.setMonitorNotifier (this);
		this.ibeaconManager.setRangeNotifier (this);

//		this.monitor.startMonotoring (this.proximityUuid);
//		this.monitor.startRanging (this.proximityUuid);
	}

}
