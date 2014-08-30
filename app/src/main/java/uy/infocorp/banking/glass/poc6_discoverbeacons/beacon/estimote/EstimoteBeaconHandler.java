package uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.estimote;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.BeaconHandler;
import uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.IBeacon;

public class EstimoteBeaconHandler extends BeaconHandler {

    private static final String TAG = EstimoteBeaconHandler.class.getSimpleName();
    private static final Region ALL_ESTIMOTE_BEACONS_REGION = new Region("rid", null, null, null);

    private BeaconManager beaconManager;
    private List<IBeacon> iBeacons;

    public EstimoteBeaconHandler(Context context) {
        super(context);
        this.beaconManager = new BeaconManager(context);

        if (!this.beaconManager.hasBluetooth()) {
            throw new RuntimeException("Bluetooth not available on this device");
        }

        if (!this.beaconManager.isBluetoothEnabled()) {
            throw new RuntimeException("Bluetooth not enabled");
        }

        //beaconManager.setBackgroundScanPeriod(TimeUnit.SECONDS.toMillis(1), 0);
        beaconManager.setRangingListener(new BeaconManager.RangingListener() {
            @Override
            public void onBeaconsDiscovered(Region region, final List<Beacon> beacons) {
                mapEstimoteBeacons(beacons);
            }
        });
        /*
        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {

            }

            @Override
            public void onExitedRegion(Region region) {

            }
        });*/
    }

    private void mapEstimoteBeacons(List<Beacon> beacons) {
        List<IBeacon> mapped = new ArrayList<IBeacon>(beacons.size());
        for (Beacon beacon : beacons) {
            mapped.add(mapEstimoteBeacon(beacon));
        }
        this.iBeacons = mapped;
    }

    private IBeacon mapEstimoteBeacon(Beacon beacon) {
        double rssi = beacon.getRssi();
        Utils.Proximity proximity = Utils.computeProximity(beacon);
        double accuracy = Utils.computeAccuracy(beacon);

        Log.i(TAG, "RSSI:\t" + rssi);
        Log.i(TAG, "Proximity:\t" + proximity.name());
        Log.i(TAG, "Accuracy:\t" + accuracy);

        IBeacon iBeacon = new IBeacon();
        return iBeacon;
    }

    @Override
    public List<IBeacon> getAllBeacons() {
        return null;
    }

    @Override
    public List<IBeacon> getAllBeaconsInRange(int metersRadius) {
        return null;
    }

    @Override
    public void startListening() {
        this.beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                try {
                    beaconManager.startRanging(ALL_ESTIMOTE_BEACONS_REGION);
                    //beaconManager.startMonitoring(ALL_ESTIMOTE_BEACONS_REGION);
                } catch (RemoteException e) {
                    throw new RuntimeException("Unable to start ranging beacon manager");
                }
            }
        });
    }

    @Override
    public void stopListening() {
        try {
            this.beaconManager.stopRanging(ALL_ESTIMOTE_BEACONS_REGION);
            //this.beaconManager.stopMonitoring(ALL_ESTIMOTE_BEACONS_REGION);
        }
        catch (RemoteException e) {
            throw new RuntimeException("Unable to stop ranging beacon manager");
        }
    }

    @Override
    public void destroy() {
        this.beaconManager.disconnect();
    }
}
