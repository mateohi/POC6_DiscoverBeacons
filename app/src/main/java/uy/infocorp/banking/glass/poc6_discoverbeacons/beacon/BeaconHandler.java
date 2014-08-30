package uy.infocorp.banking.glass.poc6_discoverbeacons.beacon;

import android.content.Context;

import java.util.List;

public abstract class BeaconHandler {

    protected Context context;

    protected BeaconHandler(Context context) {
        this.context = context;
    }

    public abstract List<IBeacon> getAllBeacons();
    public abstract List<IBeacon> getAllBeaconsInRange(int metersRadius);

    public abstract void startListening();
    public abstract void stopListening();
    public abstract void destroy();
}
