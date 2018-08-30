package com.developers.trackme.myutils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by adnroid on 25/1/17.
 */

public class Utils {

    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
                int count=is.read(bytes, 0, buffer_size);
                if(count==-1)
                    break;
                os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    public static boolean checkInternetConenction(Activity a) {
        a.getBaseContext();
        // get Connectivity Manager object to check connection
        ConnectivityManager connec = (ConnectivityManager) a.getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check for network connections
        if (connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED
                || connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING
                || connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING
                || connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED) {
            // Toast.makeText(this, " Connected ", Toast.LENGTH_SHORT).show();
            return true;
        } else if (connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED
                || connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED) {
            // Toast.makeText(this, " Not Connected ",
            // Toast.LENGTH_SHORT).show();
            return false;
        }
        return false;
    }
}
