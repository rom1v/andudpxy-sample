package com.rom1v.andudpxysample;

import java.io.IOException;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.rom1v.andudpxy.UdpxyService;

public class MainActivity extends Activity implements OnPreparedListener, SurfaceHolder.Callback,
        OnVideoSizeChangedListener {

    private static final String TAG = "AndUdpxySample";

    /** Hardcoded address for the sample. */
    private static final String ADDR = "239.0.0.1:1234";

    private View container;
    private SurfaceView surfaceView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // the container of the surface view
        container = findViewById(R.id.container);

        // surface view takes the whole screen, ignoring the aspect-ratio
        surfaceView = (SurfaceView) findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(this);

        // start the udpxy daemon
        UdpxyService.startUdpxy(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // stop the udpxy daemon
        UdpxyService.stopUdpxy(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // use the proxified address to target udpxy
        String uriString = UdpxyService.proxify(ADDR);
        Uri uri = Uri.parse(uriString);

        try {
            // init the mediaplayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDisplay(holder);
            mediaPlayer.setScreenOnWhilePlaying(true);
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnVideoSizeChangedListener(this);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, "Cannot init media player", e);
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "surface changed " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = null;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // hope that udpxy is started
        mediaPlayer.start();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
        if (w == 0 || h == 0) {
            // avoid division by zero
            return;
        }
        // resize the surfaceView to keep the aspect-ratio
        int cw = container.getWidth();
        int ch = container.getHeight();
        LayoutParams surfaceLp = surfaceView.getLayoutParams();
        if (w * ch > h * cw) {
            // the video has a greater aspect-ratio than the container
            surfaceLp.width = cw;
            surfaceLp.height = cw * h / w;
        } else {
            // the container has a greater aspect-ratio than the video
            surfaceLp.height = ch;
            surfaceLp.width = ch * w / h;
        }
        surfaceView.requestLayout();
    }

}
