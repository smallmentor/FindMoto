/** ============================================================== */
package tw.edu.stust.slm.findmoto;
/** ============================================================== */
import android.app.Application;
import android.content.Context;


/** ============================================================== */
//public class THLApp extends Application
public class THLApp
{
	public static THLApp App		= null;
	public static THLConfig Config	= null;

	public THLApp(Context context){
		App		= this;
		Config	= new THLConfig(context);
		Config.loadSettings();
	}
	/** ================================================ */
	public static THLApp getApp()
	{
		return App;
	}
	
	/** ================================================ */
//	@Override
//	public void onCreate()
//	{
//		super.onCreate();
//		System.out.println("THLApp-onCreate-1");
//		App		= this;
//		Config	= new THLConfig(this);
//		System.out.println("THLApp-onCreate-2");
//		Config.loadSettings();
//	}

	/** ================================================ */
//	@Override
//	public void onTerminate()
//	{
//		Config.saveSettings();
//
//		super.onTerminate();
//	}

	public void onTerminate(){
		Config.saveSettings();
	}
}

/** ============================================================== */

