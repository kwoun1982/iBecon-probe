package org.appsterdam.ibeaconprobe;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.appsterdam.ibeaconprobe.IBeaconMonitor.IBeaconsListeners;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import com.radiusnetworks.ibeacon.IBeacon;
import com.radiusnetworks.ibeacon.IBeaconConsumer;
import com.radiusnetworks.ibeacon.IBeaconManager;
import com.radiusnetworks.ibeacon.Region;

public class IBeaconProbe extends Activity implements IBeaconConsumer {
	static private final String TAG = "ibprobe";
	
	private IBeaconManager iBeaconManager = IBeaconManager.getInstanceForApplication(this);
	private IBeaconMonitor monitor;
	
	private TextView textView;
	private SeekBar  seekBar;

	//private String proximityUuid = "B9407F30-F5F8-466E-AFF9-25556B57FE6D".toLowerCase (); //
	private String proximityUuid = "74278bda-b644-4520-8f0c-720eaf059935".toLowerCase (); // glimworm iBeacons
	private int[] minors = {4, 6};
	private Map<String, Double> iBeacons = new HashMap<String, Double> ();

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
		setContentView (R.layout.activity_ibeacon_probe);
		this.textView = (TextView) findViewById (android.R.id.text1);
		this.seekBar = (SeekBar) findViewById (android.R.id.progress);

		this.iBeaconManager.bind (this);
		this.monitor = new IBeaconMonitor (this, IBeaconManager.getInstanceForApplication(this));

		this.textView.setText ("Starting the iBeaconProbe!");

		this.monitor.setListener (new IBeaconsListeners() {
			@Override
			public void didExitRegion (Region region) {
				Log.i (TAG, "Beacons exited region");
				IBeaconProbe.this.updateView ();
			}
			
			@Override
			public void didEnterRegion (Region region) {
				Log.i (TAG, "Beacons entered region");
				IBeaconProbe.this.updateView ();
			}
			
			@Override
			public void didDetermineStateForRegion (int arg0, Region region) {
				Log.i (TAG, "Beacons switch state range");
			}

			@Override
			public void didRangeBeaconsInRegion (Collection<IBeacon> iBeacons, Region region) {
				Log.i (TAG, "Beacons in range: " + iBeacons.size ());
				for (IBeacon ib : iBeacons) {
					if (valueInArray (ib.getMinor (), IBeaconProbe.this.minors)) {
						IBeaconProbe.this.iBeacons.put (ibToString (ib, region), Double.valueOf (ib.getAccuracy ()));
					}
				}
				IBeaconProbe.this.updateView ();
			}
		});
	}

	@Override
	protected void onStop () {
		super.onStop ();
		this.monitor.stopMonotoring ();
		this.monitor.stopRanging ();
	}

	@Override
	protected void onDestroy () {
		super.onDestroy ();
		this.iBeaconManager.unBind (this);
	}

	@Override
	public void onIBeaconServiceConnect () {
		Log.i (TAG, "Connecting to the IBeacon service.");
		this.iBeaconManager.setMonitorNotifier (this.monitor);
		this.iBeaconManager.setRangeNotifier (this.monitor);

		this.monitor.startMonotoring (this.proximityUuid);
		this.monitor.startRanging (this.proximityUuid);
	}

	private void updateView () {
		StringBuilder builder = new StringBuilder ();
		double totalDistance = 0.0;
		double distance = Double.NaN;
		if (this.iBeacons.size () == 0) {
			this.textView.setText ("No beacons found");
			return;
		}
		for (String id : this.iBeacons.keySet ()) {
			Double ibDistance = this.iBeacons.get (id);
			if (Double.isNaN (distance))
				distance = ibDistance.doubleValue ();
			builder.append (id);
			builder.append (" - ");
			builder.append (String.format ("%.4f", ibDistance));
			builder.append ("m \n");
			totalDistance += ibDistance.doubleValue (); 
		}
		this.textView.setText (builder.toString ());
		this.seekBar.setProgress ((int) (distance / totalDistance * 100.0));
	}

	static private String ibToString (IBeacon ib, Region region) {
		String string = String.format ("%s:%d:%d",
				ib.getProximityUuid (),
				ib.getMajor (),
				ib.getMinor ());
		return string;
	}

	static private boolean valueInArray (int value, int[] array) {
		if (array == null)
			return false;
		if (array.length == 0)
			return true;
		for (int i=0; i<array.length; i++)
			if (value == array[i])
				return true;
		return false;
	}
}
