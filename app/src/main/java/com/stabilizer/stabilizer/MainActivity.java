package com.stabilizer.stabilizer;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.android.OpenCVLoader;


public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        if (!OpenCVLoader.initDebug()) {
            Log.d("OLOLO", "no opencv");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (null == savedInstanceState)
        {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance())
                    .commit();
        }

    }
}
