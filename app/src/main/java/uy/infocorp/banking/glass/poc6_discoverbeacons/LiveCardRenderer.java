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

    private static final float TEXT_SIZE = 30f;

    private final Paint paint;
    private String foundBeacons;

    private SurfaceHolder holder;
    private boolean renderingPaused;

    private RenderThread renderThread;

    private BeaconHandler beaconHandler;
    private ScheduledExecutorService task;

    public LiveCardRenderer(Context context) {
        this.beaconHandler = new EstimoteBeaconHandler(context);
        createAndStartScheduledTask();

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setTextSize(TEXT_SIZE);
        paint.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));

        this.foundBeacons = "Looking for beacons ...";
    }

    private void createAndStartScheduledTask() {
        this.task = Executors.newSingleThreadScheduledExecutor();

        this.task.scheduleAtFixedRate(new Runnable() {
            public void run() {
                updateBeaconsString();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void updateBeaconsString() {
        List<IBeacon> beacons = this.beaconHandler.getAllBeacons();

        if (!beacons.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (IBeacon beacon : beacons) {
                sb.append("Id: " + beacon.getId() + "\n");
                sb.append("Name: " + beacon.getName() + "\n");
                sb.append("Distance: " + String.format("%.2f", beacon.getDistance()) + "\n");
            }
            this.foundBeacons = sb.toString();
        }
        else {
            this.foundBeacons = "No beacons found";
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder = holder;
        renderingPaused = false;
        updateRenderingState();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.holder = null;
        updateRenderingState();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        renderingPaused = paused;
        updateRenderingState();
    }

    private void updateRenderingState() {
        boolean shouldRender = (holder != null) && !renderingPaused;
        boolean isRendering = (renderThread != null);

        if (shouldRender != isRendering) {
            if (shouldRender) {
                renderThread = new RenderThread();
                renderThread.start();

                beaconHandler.startListening();
                createAndStartScheduledTask();
            } else {
                renderThread.quit();
                renderThread = null;

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
            canvas = holder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
            // Clear the canvas.
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            // draw the text on the canvas.
            drawMultilineText(foundBeacons, 50, 50, paint, canvas);

            // Unlock the canvas and post the updates.
            holder.unlockCanvasAndPost(canvas);
        }
    }

    public void destroyBeaconHandler() {
        this.beaconHandler.destroy();
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

    private void drawMultilineText(String text, int x, int y, Paint paint, Canvas canvas) {
        int offset = 0;

        for (String line : text.split("\n")) {
            canvas.drawText(line, 50, y + offset, paint);
            offset += 40;
        }
    }

}
