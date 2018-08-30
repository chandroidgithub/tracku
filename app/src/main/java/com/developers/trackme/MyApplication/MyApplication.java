package com.developers.trackme.MyApplication;

import android.app.Application;

import com.developers.trackme.constants.Constants;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

/**
 * Created by android on 19/4/18.
 */

public class MyApplication extends Application {

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket(Constants.SOCKETURL);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }

}
