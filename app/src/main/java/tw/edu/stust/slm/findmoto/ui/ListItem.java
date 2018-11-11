package tw.edu.stust.slm.findmoto.ui;

/** ============================================================== */
public class ListItem
{
	public String UUID = "";
	public String major = "";
	public String minor = "";
	public String rssi = "";
	public String tV_batteryPower = "";
	String tV_mac = null;

	public ListItem()
	{
	}

	public ListItem(String UUID, String major, String minor, String rssi, String tV_batteryPower, String tV_mac)
	{
		this.UUID = UUID;
		this.major = major;
		this.minor = minor;
		this.rssi = rssi;
		this.tV_batteryPower = tV_batteryPower;
		this.tV_mac = tV_mac;
	}
}
