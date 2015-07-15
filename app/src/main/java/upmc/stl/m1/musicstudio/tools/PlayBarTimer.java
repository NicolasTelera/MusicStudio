package upmc.stl.m1.musicstudio.tools;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.ArrayList;

import upmc.stl.m1.musicstudio.R;
import upmc.stl.m1.musicstudio.fragment.TrackFragment;

/**
 * Created by nicolas on 30/04/2015.
 */
public class PlayBarTimer extends java.util.TimerTask {

    private ArrayList<TrackFragment> tracks;

    public PlayBarTimer(ArrayList<TrackFragment> tracks) {
        this.tracks = tracks;
    }

    @Override
    public void run() {
        for (TrackFragment f : this.tracks) {
            f.drawTimeAndPlay();
        }
    }

}
