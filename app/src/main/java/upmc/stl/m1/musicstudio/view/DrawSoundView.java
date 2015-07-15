package upmc.stl.m1.musicstudio.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by nicolas on 04/03/2015.
 * Cette classe permet de dessiner les sons dans les blocs.
 */
public class DrawSoundView extends View {

    private Paint paint;
    private int center;
    private short[] data;

    public DrawSoundView(Context context, int height, short[] data) {
        super(context);
        this.center = height/2;
        this.data = data;
        this.paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
    }

    // on dessine deux pixel par sample pour plus de lisibilité
    @Override
    public void onDraw(Canvas canvas) {

        // ligne centrale
        canvas.drawLine(0, center, this.getWidth(), center, this.paint);

        int x = 0;
        int temp = 0;
        // on commence à 5 pour enlever les bruits parasites du début de prise
        for(int i=5 ; i<data.length ; i++) {
            temp = Math.abs(data[i] - 274);
            canvas.drawLine(x, center-temp, x, center+temp, paint);
            x++;
            canvas.drawLine(x, center-temp, x, center+temp, paint);
            x++;
        }

    }

}
