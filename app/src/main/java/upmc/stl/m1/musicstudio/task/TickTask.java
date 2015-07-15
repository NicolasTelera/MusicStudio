package upmc.stl.m1.musicstudio.task;

import android.app.Activity;
import android.media.SoundPool;
import android.os.AsyncTask;

/**
 * Created by nicolas on 13/05/2015.
 */
public class TickTask extends AsyncTask<Void, Void, Void> {

    private SoundPool soundPool;
    private int soundID;
    private float volume;

    public TickTask(SoundPool soundPool, int soundID, float volume) {
        this.soundPool = soundPool;
        this.soundID = soundID;
        this.volume = volume;
    }

    @Override
    protected Void doInBackground(Void... params) {
        for (int i=0 ; i<4 ; i++) {
            soundPool.play(this.soundID, this.volume, this.volume, 1, 0, 1f);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
