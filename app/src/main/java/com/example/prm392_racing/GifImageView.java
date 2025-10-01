package com.example.prm392_racing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import java.io.InputStream;

public class GifImageView extends View {

    private Movie movie;
    private long movieStart = 0;

    public GifImageView(Context context) {
        super(context);
    }

    public GifImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GifImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /** Load a GIF from raw resources */
    public void setGifResource(int resId) {
        InputStream is = getContext().getResources().openRawResource(resId);
        movie = Movie.decodeStream(is);
        requestLayout();
    }

    /** Load a GIF from URI (optional) */
    public void setGifUri(Uri uri) {
        try {
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            movie = Movie.decodeStream(is);
            requestLayout();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (movie == null) return;

        long now = android.os.SystemClock.uptimeMillis();

        if (movieStart == 0) {
            movieStart = now;
        }

        int duration = movie.duration();
        if (duration == 0) duration = 1000; // fallback

        int relTime = (int) ((now - movieStart) % duration);

        movie.setTime(relTime);
        float scaleX = (float) getWidth() / movie.width();
        float scaleY = (float) getHeight() / movie.height();
        canvas.scale(scaleX, scaleY);
        movie.draw(canvas, 0, 0);

        invalidate(); // loop forever
    }
}
