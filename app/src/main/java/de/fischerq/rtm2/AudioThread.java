package de.fischerq.rtm2;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.fischerq.rtm2.sound.SamplePlayer;
import de.fischerq.rtm2.sound.SoundFile;

public class AudioThread extends Thread {
    private View myView;
    private Activity myActivity;
    private Uri mFileURI;
    private SamplePlayer mPlayer;
    private boolean loaded;

    public AudioThread(Activity a, View v, Uri u)
    {
        myView = v;
        myActivity = a;
        mFileURI = u;
    }
    private Handler handler = new Handler() {

        public void handleMessage(Message msg) {

            double progress = msg.getData().getDouble("progress");

            TextView currentSong = (TextView) myView.findViewById(R.id.currentSong);
            if(currentSong != null)
                currentSong.setText("Song: "+progress);

        }
    };

    final SoundFile.ProgressListener listener =
            new SoundFile.ProgressListener() {
                public boolean reportProgress(double fractionComplete) {
                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putDouble("progress", fractionComplete);
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                    return true;
                }
            };

    public void run() {
        try {
            SoundFile mSoundFile = SoundFile.create(myActivity.getApplicationContext(), mFileURI, listener);

            mPlayer = new SamplePlayer(mSoundFile);
            mPlayer.start();
            loaded = true;
            Toast.makeText(myActivity, "Loaded", Toast.LENGTH_SHORT).show();
        } catch (final Exception e) {
            e.printStackTrace();
        }

    }
}
