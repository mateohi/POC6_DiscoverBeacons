package uy.infocorp.banking.glass.poc6_discoverbeacons;

import com.google.android.glass.timeline.DirectRenderingCallback;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.SurfaceHolder;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.BeaconHandler;
import uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.IBeacon;
import uy.infocorp.banking.glass.poc6_discoverbeacons.beacon.estimote.EstimoteBeaconHandler;

public class LiveCardRenderer implements DirectRenderingCallback {

    /** The duration, in millisconds, of one frame. */
    private static final long FRAME_TIME_MILLIS = 40;

    /** "Hello world" text size. */
    private static final float TEXT_SIZE = 70f;

    /** Alpha variation per frame. */
    private static final int ALPHA_INCREMENT = 5;

    /** Max alpha value. */
    private static final int MAX_ALPHA = 256;

    private final Paint mPaint;
    private String foundBeacons;

    private int mCenterX;
    private int mCenterY;

    private SurfaceHolder mHolder;
    private boolean mRenderingPaused;

    private RenderThread mRenderThread;

    private BeaconHandler beaconHandler;
    private ScheduledExecutorService task;

    public LiveCardRenderer(Context context) {
        this.beaconHandler = new EstimoteBeaconHandler(context);
        createAndStartScheduledTask();

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(Color.WHITE);
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(TEXT_SIZE);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        mPaint.setAlpha(0);

        foundBeacons = "Looking for beacons ...";
    }

    private void createAndStartScheduledTask() {
        task = Executors.newSingleThreadScheduledExecutor();

        task.scheduleAtFixedRate(new Runnable() {
            public void run() {
                updateBeaconsString();
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    private void updateBeaconsString() {
        List<IBeacon> beacons = this.beaconHandler.getAllBeacons();
        StringBuilder sb = new StringBuilder();
        for (IBeacon beacon : beacons) {
            sb.append("Name: ");
        }
        this.foundBeacons = sb.toString();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCenterX = width / 2;
        mCenterY = height / 2;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        mRenderingPaused = false;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        mRenderingPaused = paused;
        updateRenderingState();
    }

    private void updateRenderingState() {
        boolean shouldRender = (mHolder != null) && !mRenderingPaused;
        boolean isRendering = (mRenderThread != null);

        if (shouldRender != isRendering) {
            if (shouldRender) {
                mRenderThread = new RenderThread();
                mRenderThread.start();

                beaconHandler.startListening();
                createAndStartScheduledTask();
            } else {
                mRenderThread.quit();
                mRenderThread = null;

                beaconHandler.stopListening();
                task.shutdown();
                task = null;
            }
        }
    }

    /**
     * Draws the view in the SurfaceHolder's canvas.
     */
    private void draw() {
        Canvas canvas;
        try {
            canvas = mHolder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
            // Clear the canvas.
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // Update the text alpha and draw the text on the canvas.
            mPaint.setAlpha((mPaint.getAlpha() + ALPHA_INCREMENT) % MAX_ALPHA);
            canvas.drawText(foundBeacons, mCenterX, mCenterY, mPaint);

            // Unlock the canvas and post the updates.
            mHolder.unlockCanvasAndPost(canvas);
        }
    }

    private class RenderThread extends Thread {
        private boolean shouldRun;

        public RenderThread() {
            shouldRun = true;
        }

        private synchronized boolean shouldRun() {
            return shouldRun;
        }

        public synchronized void quit() {
            shouldRun = false;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                long frameStart = SystemClock.elapsedRealtime();
                draw();
                long frameLength = SystemClock.elapsedRealtime() - frameStart;

                long sleepTime = FRAME_TIME_MILLIS - frameLength;
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
            }
        }
    }

}
