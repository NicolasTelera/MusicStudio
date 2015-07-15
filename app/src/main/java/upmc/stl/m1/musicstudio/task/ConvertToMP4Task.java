package upmc.stl.m1.musicstudio.task;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

import upmc.stl.m1.musicstudio.R;
import upmc.stl.m1.musicstudio.fragment.TrackFragment;

/**
 * Tâche asynchrone de conversion d'un fichier .pcm en .mp4
 */
public class ConvertToMP4Task extends AsyncTask<Void, Integer, Void> {

    /* CONSTANTES */
    private final String COMPRESSED_AUDIO_FILE_MIME_TYPE = "audio/mp4a-latm";
    private final int COMPRESSED_AUDIO_FILE_BIT_RATE = 128000; // 128kbps (Kbits par seconde soit 128 000 bit / seconde)
    private final int FREQUENCY = 44100;
    private final int CODEC_TIMEOUT_IN_MS = 5000;
    private final int BUFFER_SIZE = 88200;

    /* VARIABLES */
    private Fragment context;
    private String path;
    private ProgressBar progressBar;
    private TrackFragment.ConvertCallback delegate;

    public ConvertToMP4Task(Fragment context, String path, TrackFragment.ConvertCallback delegate) {
        this.context = context;
        this.path = path;
        this.delegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        this.progressBar = (ProgressBar) this.context.getView().findViewById(R.id.track_progress_bar);
        this.progressBar.setVisibility(View.VISIBLE);
        super.onPreExecute();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2) // annotation pour enlever les messages d'erreurs, à ne pas forcément garder (manifest normalement suffisant)
    @Override
    protected Void doInBackground(Void... params) {

        try {
            File inputFile = new File(this.path + ".raw");
            FileInputStream fis = new FileInputStream(inputFile);
            File outputFile = new File(this.path + ".mp4");
            if (outputFile.exists()) outputFile.delete();

            MediaMuxer mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE, FREQUENCY, 1);
            outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);

            MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
            codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            codec.start();

            ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: Array of buffers
            ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

            MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();

            byte[] tempBuffer = new byte[BUFFER_SIZE];
            boolean hasMoreData = true;
            double presentationTimeUs = 0;
            int audioTrackIdx = 0;
            int totalBytesRead = 0;
            int percentComplete;

            do {

                int inputBufIndex = 0;
                while (inputBufIndex != -1 && hasMoreData) {
                    inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        dstBuf.clear();

                        int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
                        if (bytesRead == -1) { // -1 implies EOS
                            hasMoreData = false;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            totalBytesRead += bytesRead;
                            dstBuf.put(tempBuffer, 0, bytesRead);
                            codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
                            presentationTimeUs = 1000000l * (totalBytesRead / 2) / FREQUENCY;
                        }
                    }
                }

                // Drain audio
                int outputBufIndex = 0;
                while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {

                    outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
                    if (outputBufIndex >= 0) {
                        ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
                        encodedData.position(outBuffInfo.offset);
                        encodedData.limit(outBuffInfo.offset + outBuffInfo.size);

                        if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        } else {
                            mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
                            codec.releaseOutputBuffer(outputBufIndex, false);
                        }
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        outputFormat = codec.getOutputFormat();
                        //Log.v("CONVERT", "Output format changed - " + outputFormat);
                        audioTrackIdx = mux.addTrack(outputFormat);
                        mux.start();
                    } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        //Log.e("CONVERT", "Output buffers changed during encode!");
                    } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // NO OP
                    } else {
                        //Log.e("CONVERT", "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
                    }
                }
                percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
                //Log.v("CONVERT", "Conversion % - " + percentComplete);
                publishProgress(percentComplete);
            } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);

            fis.close();
            mux.stop();
            mux.release();
            //Log.v("CONVERT", "Compression done ...");
        } catch (FileNotFoundException e) {
            //Log.e("CONVERT", "File not found!", e);
            e.printStackTrace();
        } catch (IOException e) {
            //Log.e("CONVERT", "IO exception!", e);
            e.printStackTrace();
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        this.progressBar.setProgress(values[0]);
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        this.progressBar.setVisibility(View.INVISIBLE);
        /*
        Button recordButton = (Button) this.context.getView().findViewById(R.id.track_play_button);
        recordButton.setClickable(true);
        */
        this.delegate.onTaskDone();
        super.onPostExecute(aVoid);
    }
}
