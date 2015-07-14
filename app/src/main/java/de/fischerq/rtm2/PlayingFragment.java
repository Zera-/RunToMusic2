package de.fischerq.rtm2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.smp.soundtouchandroid.OnProgressChangedListener;
import com.smp.soundtouchandroid.SoundStreamAudioPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


public class PlayingFragment extends Fragment implements StepListener, OnProgressChangedListener, SensorEventListener
{
    public static final String TAG = "playing";
    private static final int PICKFILE_RESULT_CODE = 1;
    private static long max_delay = 1000;
    View myView;

    private PowerManager.WakeLock wakeLock;
    private SensorManager sensorManager;
    private StepDetector stepDetector;
    private double defaultMusicSpeed;
    private double playbackSpeed;
    private double runningSpeed;

    private LinkedList<Long> last_steps = new LinkedList<Long>();
    private long last_time = 0;
    private static long timeout = 5000;
    private static int n_buffered_steps = 8;
    private static double adjustment_rate = 100;//bpm change/sec
    private boolean mIsPlaying;
    private Handler speedUpdater;
    private static long updateDelay = 200;

    private Uri mFileURI;

    private LinkedList<PlaylistEntry> queued_songs = new LinkedList<PlaylistEntry>();
    private double songSpeed;
    private String mTitle;
    private String mArtist;

    boolean adapt;

    SoundStreamAudioPlayer stream;

    private Runnable speedUpdaterRunnable = new Runnable() {
        @Override
        public void run(){
            TextView speedDisplay = (TextView) myView.findViewById(R.id.BPMdisplay);
            /*double avg_time = updateDelay;
            int diff_steps = stepcounter - last_counter;
            if(current_intervals_used < total_intervals)
            {
                current_intervals_used++;
                step_updates.offer(diff_steps);
            }
            else
            {
                step_updates.offer(diff_steps);
                int to_remove = step_updates.poll();
                stepcounter -= to_remove;
            }

            if(adapt)
            {
                if(stepcounter > 0) {

                    avg_time = updateDelay*current_intervals_used / stepcounter;
                    runningSpeed = (1000*60)/avg_time;
                    if(runningSpeed > 0.666*defaultMusicSpeed && runningSpeed < defaultMusicSpeed*1.75 )
                        playbackSpeed = runningSpeed;
                    else
                        playbackSpeed = defaultMusicSpeed;
                }
                else
                    playbackSpeed = defaultMusicSpeed;
                last_counter = stepcounter;
            }
*/
            double target_speed = defaultMusicSpeed;
            if(adapt) {
                double avg_time = 0;
                for (int i = 1; i < last_steps.size(); ++i)
                    avg_time = (avg_time * (i - 1) + (last_steps.get(i) - last_steps.get(i - 1)) / 1000000) / i;
                if (avg_time != 0)
                    runningSpeed = (1000 * 60) / avg_time;
                else
                    runningSpeed = 0;
                if (runningSpeed > 0.666 * defaultMusicSpeed && runningSpeed < defaultMusicSpeed * 1.75)
                    target_speed = runningSpeed;
                else if (runningSpeed < 0.666 * defaultMusicSpeed)
                    target_speed = 0.666 * defaultMusicSpeed;
                else if (runningSpeed > 1.75 * defaultMusicSpeed)
                    target_speed = 1.75 * defaultMusicSpeed;
            }

            long now = System.currentTimeMillis();
            if(now-last_time > timeout)
                target_speed = defaultMusicSpeed;

            if(Math.abs(target_speed - playbackSpeed) < adjustment_rate*updateDelay/1000)
                playbackSpeed = target_speed;
            else
                playbackSpeed +=  Math.signum(target_speed - playbackSpeed)*adjustment_rate*updateDelay/1000;

            float speedup = (float)(playbackSpeed / defaultMusicSpeed);

            if(speedDisplay != null)
                speedDisplay.setText("BPM: "+(int)(playbackSpeed));
            if(stream != null)
                stream.setTempo(speedup);

            speedUpdater.postDelayed(this, updateDelay);
        }
    };

    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        int wakeFlags;
            wakeFlags = PowerManager.SCREEN_DIM_WAKE_LOCK;

        wakeLock = pm.newWakeLock(wakeFlags, TAG);
        wakeLock.acquire();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            myView = inflater.inflate(R.layout.playing_fragment, container, false);

            sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

            Button play = (Button) myView.findViewById(R.id.play);
            play.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playNext();
                }
            });

            Button pause = (Button) myView.findViewById(R.id.pause);
            pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(stream != null)
                        stream.pause();
                }
            });

            Button resume = (Button) myView.findViewById(R.id.resume);
            resume.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(stream != null)
                        stream.start();
                }
            });

            Button next = (Button) myView.findViewById(R.id.next);
            next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playNext();
                }
            });

            mIsPlaying = false;



            Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            if (countSensor != null) {
                sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                Toast.makeText(getActivity(), "Count sensor not available!", Toast.LENGTH_LONG).show();
            }




            acquireWakeLock();
            stepDetector = new StepDetector(this.getActivity().getBaseContext());
            stepDetector.addStepListener(this);
            Sensor accSensor = sensorManager.getDefaultSensor(
                    Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(stepDetector,
                    accSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);


            TextView playlist_info = (TextView)myView.findViewById(R.id.playlist_info);
            playlist_info.setText("Queued songs: "+queued_songs.size());

            speedUpdater = new Handler();
            speedUpdater.postDelayed(speedUpdaterRunnable, 100);

            adapt = true;
            if(getArguments() != null && getArguments().containsKey("mode"))
            {
                int mode = getArguments().getInt("mode");
                if(mode == 1) {
                    adapt = true;
                    TextView md = (TextView) myView.findViewById(R.id.mode);
                    md.setText("Adapt mode");
                }
                else if(mode== 2)
                {
                    adapt = false;
                    playbackSpeed = getArguments().getDouble("speed");
                    TextView md = (TextView) myView.findViewById(R.id.mode);
                    md.setText("Challenge mode: "+playbackSpeed);
                }

            }
        return myView;
    }

    public void setPlaylist(LinkedList<PlaylistEntry> list)
    {
        queued_songs = list;
    }

    @Override
    public void onResume() {
        super.onResume();
//        activityRunning = true;


    }

    @Override
    public void onPause() {
        super.onPause();
//        activityRunning = false;
        // if you unregister the last listener, the hardware will stop detecting step events
//        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        FileOutputStream fos;
        try {
            fos  = getActivity().getBaseContext().openFileOutput("logDetector.csv", Context.MODE_APPEND);
            Log.d(TAG, "FOS: "+getActivity().getFilesDir().getAbsolutePath());
            fos.write(((event.timestamp / 1000000) +", "+ 3 +", 2, "+ 0+", 0\n").getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /*
        long nextTime = event.timestamp;
        last_steps.offer(nextTime);
        if(last_steps.size() > n_buffered_steps)
            last_steps.poll();
        last_time = System.currentTimeMillis();
        */

        //TextView counter = (TextView) myView.findViewById(R.id.counter);
        //counter.setText("steps:"+stepcounter);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }


    public void onStep(long timestamp)
    {
        last_steps.offer(timestamp);
        if(last_steps.size() > n_buffered_steps)
            last_steps.poll();
        last_time = System.currentTimeMillis();
        Log.d(TAG, "Got Step");
    }


    public void onProgressChanged(int track, double currentTime,
                           long position)
    {
        TextView currentSong = (TextView) myView.findViewById(R.id.progress);
        int seconds = (int)currentTime;
        if(currentSong != null)
            currentSong.setText( "Current time: "+seconds/60+":"+seconds%60);
    }

    public void onTrackEnd(int track)
    {
        Toast.makeText(getActivity().getBaseContext(), "Next Song",Toast.LENGTH_LONG).show();
        playNext();
    }

    public void onExceptionThrown(String string)
    {
        Toast.makeText(getActivity().getBaseContext(), "Exception: "+string,Toast.LENGTH_LONG).show();
    }

    public void setTitle(String title)
    {
        TextView songname = (TextView) myView.findViewById(R.id.currentSong);
        songname.setText(title);
    }

    // from https://github.com/google/ringdroid/blob/master/src/com/ringdroid/RingdroidEditActivity.java
    private void playNext()
    {
        // Load the sound file in a background thread
        //mLoadSoundFileThread = new AudioThread(getActivity(), myView, mFileURI, this);
        //mLoadSoundFileThread.start();
        try{
            if(stream != null)
                stream.stop();

            PlaylistEntry next = queued_songs.poll();
            if(next == null)
                return;
            stream = new SoundStreamAudioPlayer(0, getActivity().getApplicationContext(), next.uri, 1, 1);
            defaultMusicSpeed = next.speed;
            playbackSpeed = defaultMusicSpeed;
            TextView currentSong = (TextView) myView.findViewById(R.id.currentSong);
            if(currentSong != null)
                currentSong.setText("Song: "+String.valueOf(next.name));

            TextView playlist_info = (TextView)myView.findViewById(R.id.playlist_info);
            playlist_info.setText("Playlist size: "+queued_songs.size());

            stream.setOnProgressChangedListener(this);
            new Thread(stream).start();
            stream.start();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}