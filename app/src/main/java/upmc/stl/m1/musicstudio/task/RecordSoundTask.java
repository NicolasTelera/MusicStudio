package upmc.stl.m1.musicstudio.task;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import upmc.stl.m1.musicstudio.R;

/**
 * Tâche asynchrone d'enregistrement d'un son.
 */
public class RecordSoundTask extends AsyncTask<Void, Void, Void> {

    /* CONSTANTES */
    private final int FREQUENCY = 44100;
    private final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private final int BUFFER_SIZE = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL_IN_CONFIG, AUDIO_FORMAT);

    /* VARIABLES */
    private Fragment context;
    private boolean isRecording;
    private String path;

    public RecordSoundTask(Fragment context, String path) {
        this.context = context;
        this.path = path;
        this.isRecording = true;
    }

    public void setIsRecording(boolean recording) {
        this.isRecording = recording;
    }

    @Override
    protected void onPreExecute() {
        /*
        Button recordButton = (Button) this.context.getView().findViewById(R.id.track_play_button);
        recordButton.setClickable(false);
        */
        super.onPreExecute();
    }

    @Override
    protected Void doInBackground(Void... params) {

        //android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        byte audioData[] = new byte[BUFFER_SIZE];
        AudioRecord recorder = new AudioRecord(AUDIO_SOURCE, FREQUENCY, CHANNEL_IN_CONFIG, AUDIO_FORMAT, BUFFER_SIZE);
        recorder.startRecording();

        BufferedOutputStream os = null;
        try { os = new BufferedOutputStream(new FileOutputStream(this.path + ".raw")); }
        catch (FileNotFoundException e) { e.printStackTrace(); }

        while (this.isRecording) {

            int status = recorder.read(audioData, 0, audioData.length);
            if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) return null;
            try { os.write(audioData, 0, audioData.length); }
            catch (IOException e) { return null; }
        }

        try {
            os.close();
            recorder.stop();
            recorder.release();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}

