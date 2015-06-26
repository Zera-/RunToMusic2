package de.fischerq.rtm2;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ModeFragment extends Fragment {
    public static final String TAG = "mode";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_mode, container, false);

        SeekBar speedSlider = (SeekBar) v.findViewById(R.id.speedSlider);
        speedSlider.setProgress(10);
        TextView speedDisplay = (TextView) v.findViewById(R.id.speedDisplay);
        speedDisplay.setText("BPM: " + 160);

        speedSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                update(seekBar.getProgress());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            void update(int position) {
                int bpm = 160 + (int) (((double) position - 10) / 10 * 20);
                TextView speedDisplay = (TextView) getView().findViewById(R.id.speedDisplay);
                speedDisplay.setText("BPM: " + bpm);
            }
        });


        Button buttonAdapt = (Button) v.findViewById(R.id.buttonAdapt);
        buttonAdapt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Selected AdaptMode", Toast.LENGTH_SHORT).show();
                Fragment fragment = getFragmentManager().findFragmentByTag(PlayingFragment.TAG);
                if (fragment == null) {
                    fragment = new PlayingFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlayingFragment.TAG).commit();
            }
        });

        Button buttonChallenge = (Button) v.findViewById(R.id.buttonChallenge);
        buttonChallenge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(), "Selected ChallengeMode", Toast.LENGTH_SHORT).show();
                Fragment fragment = getFragmentManager().findFragmentByTag(PlayingFragment.TAG);
                if (fragment == null) {
                    fragment = new PlayingFragment();
                }
                getFragmentManager().beginTransaction().replace(R.id.container, fragment, PlayingFragment.TAG).commit();

            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}