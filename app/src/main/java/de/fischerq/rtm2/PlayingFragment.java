package de.fischerq.rtm2;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import de.fischerq.rtm2.sound.SamplePlayer;
import de.fischerq.rtm2.sound.SoundFile;


public class PlayingFragment extends Fragment implements SensorEventListener {
    public static final String TAG = "playing";
    private static final int PICKFILE_RESULT_CODE = 1;

    View myView;

    private SensorManager sensorManager;
    private boolean activityRunning;
    private double currentSpeed;

    private boolean mIsPlaying;

    private Uri mFileURI;
    private SoundFile mSoundFile;
    private SoundFile currentSong;
    private double songSpeed;
    private String mTitle;
    private String mArtist;
    private Thread mLoadSoundFileThread;
    File mFile;
    private boolean loaded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.playing_fragment, container, false);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        ImageView btn = (ImageView) myView.findViewById(R.id.float_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnButtonClick(v);
            }
        });

        File f = new File(Environment.getExternalStorageDirectory(), "read.me");

        mIsPlaying = false;
        loaded = false;

        return myView;
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor != null) {
            sensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            Toast.makeText(getActivity(), "Count sensor not available!", Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
        // if you unregister the last listener, the hardware will stop detecting step events
//        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (activityRunning) {
            TextView speedDisplay = (TextView) myView.findViewById(R.id.BPMdisplay);
            if(speedDisplay != null)
                speedDisplay.setText("Steps: "+String.valueOf(event.values[0]));
            else
                Log.d("np", "couldnt find speed display");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void OnButtonClick(View v){
        try{
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(intent,PICKFILE_RESULT_CODE);
        }
        catch(ActivityNotFoundException exp){
            Toast.makeText(getActivity().getBaseContext(), "No File (Manager / Explorer)etc Found In Your Device",Toast.LENGTH_LONG).show();
        }
    }

   @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode== Activity.RESULT_OK){

                    mFileURI = data.getData();
                    TextView currentSong = (TextView) myView.findViewById(R.id.currentSong);
                    if(currentSong != null)
                        currentSong.setText("Song: "+String.valueOf(mFileURI));
                    loadFromFile();
                }
                break;
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };

        //This method was deprecated in API level 11
        //Cursor cursor = managedQuery(contentUri, proj, null, null, null);

        CursorLoader cursorLoader = new CursorLoader(
                getActivity(),
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    public void setTitle(String title)
    {
        TextView songname = (TextView) myView.findViewById(R.id.currentSong);
        songname.setText(title);
    }

    // from https://github.com/google/ringdroid/blob/master/src/com/ringdroid/RingdroidEditActivity.java
    private void loadFromFile()
    {
        // Load the sound file in a background thread
        mLoadSoundFileThread = new AudioThread(getActivity(), myView, mFileURI);
        mLoadSoundFileThread.start();
    }
}