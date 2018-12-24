package tw.edu.stust.slm.findmoto;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

public class Beacon {
    private String          name;
    private ParcelUuid[]    uuid;
    private String          macAddress;
    private int             rssi;
    private int             txPower;
    private double          lestDist = 0;
    private int             atOneMeter = 59;

    Beacon(BluetoothDevice device){
        this.name       = device.getName();
        this.uuid       = device.getUuids();
        this.macAddress = device.getAddress();
    }

    Beacon(BluetoothDevice device, int rssi, int txPower) {
        this.name       = device.getName();
        this.uuid       = device.getUuids();
        this.macAddress = device.getAddress();
        this.rssi       = rssi;
        this.txPower    = txPower;
    }

    public double getDistance1() {
        int absRssi = Math.abs(rssi);
        double power = (absRssi - atOneMeter) / 20;
        return Math.pow(10,power);
    }


    public double getDistance2() {
        if (rssi == 0)
        {
            return -1.0;
        }
        double ratio = rssi * 1.0 / txPower;

        if (ratio < 1.0)
        {
            return Math.pow(ratio, 10);
        }
        else
        {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
            return accuracy;
        }
    }


    public int getRssi() {
        return rssi;
    }

    public int getTxPower() {
        return txPower;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ParcelUuid[] getUuid() {
        return uuid;
    }

    public void setUuid(ParcelUuid[] uuid) {
        this.uuid = uuid;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public double getLestDist() {
        return lestDist;
    }

    public void setLestDist(double lestDist) {
        this.lestDist = lestDist;
    }

    public int getAtOneMeter() {
        return atOneMeter;
    }

    public void setAtOneMeter(int atOneMeter) {
        this.atOneMeter = atOneMeter;
    }
}
