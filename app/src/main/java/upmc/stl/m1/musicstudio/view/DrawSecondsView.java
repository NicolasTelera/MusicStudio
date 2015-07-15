package upmc.stl.m1.musicstudio.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by nicolas on 30/04/2015.
 */
public class DrawSecondsView extends View {

    private Paint paint;
    private int center;
    private int width;

    public DrawSecondsView(Context context, int height, int width) {
        super(context);
        this.center = height / 2;
        this.width = width;
        this.paint = new Paint();
        this.paint.setColor(Color.GRAY);
    }

    public void onDraw(Canvas canvas) {

        // on dessine les barres
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        for (int i=85 ; i<width ; i++) {
            canvas.drawLine(i, center + 10, i, center+center, paint);
            i += 84;
        }

        // on dessine les secondes
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(40);
        int sec = 1;
        for (int i=85 ; i<width ; i++) {
            if (sec < 10) canvas.drawText(Integer.toString(sec++), i-11, center-6, paint);
            else if (sec < 100) canvas.drawText(Integer.toString(sec++), i-23, center-6, paint);
            else canvas.drawText(Integer.toString(sec++), i-35, center-6, paint);
            i += 84;
        }

    }

}
