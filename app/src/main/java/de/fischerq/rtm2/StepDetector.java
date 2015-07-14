/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.fischerq.rtm2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;


public class StepDetector implements SensorEventListener
{
    private final static String TAG = "StepDetector";
    private class Sample
    {
        public double r = 0;
        public long t = 0;
    }

    private float[] smoothed = null;
    private static final float smooth_factor = 7;
    private LinkedList<float[]> lastValues = new LinkedList<float[]>();
    private static final int filter_window = 3;

    private static final double avg_factor = 0.5;
    private double average = 10;
    private static final double threshold = 18;
    private static final double t_threshold = 150;

    private boolean gotCrossing = false;
    private Sample next_max = null;
    Sample last_sample = null;
    private LinkedList<Sample> last_maxes = new LinkedList<Sample>();

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    private Context c;
    public StepDetector(Context b) {
        c= b;

        c.deleteFile("logDetector.csv");
    }
    
    public void setSensitivity(float sensitivity) {
    }
    
    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }
    
    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event) {
        try {
            FileOutputStream fos  = c.openFileOutput("logDetector.csv", Context.MODE_APPEND);


        Sensor sensor = event.sensor;
        if(!(sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            Log.d(TAG, "Bad sensor");
            return;
        }
        synchronized (this) {

            if(smoothed == null)
                smoothed = event.values;
            else
            {
                if(last_sample != null)
                {
                    float delta_t = Math.min(1, smooth_factor * (float)((event.timestamp - last_sample.t) / 1000000) / 1000);
                    for (int j=0 ; j<3 ; j++) {
                        smoothed[j] = event.values[j] *delta_t + (1-delta_t)*smoothed[j];
                    }
                }
            }

            /*lastValues.offer(event.values);
            if(lastValues.size() > filter_window)
                lastValues.poll();


            float[] avg = new float[3];
            for(int i = 0; i < lastValues.size(); ++i)
            {
                float[] val = lastValues.get(i);
                for (int j=0 ; j<3 ; j++) {
                    avg[j] += val[j]/lastValues.size();
                }
            }*/

            double r = 0;
            for (int i=0 ; i<3 ; i++) {
               r+= smoothed[i]*smoothed[i];
            }
            r = Math.sqrt(r);

            Sample s = new Sample();
            s.r = r;
            s.t = event.timestamp;

            if(last_sample != null)
            {
                double delta_t = Math.min(1, avg_factor* (double)((s.t - last_sample.t) / 1000000) / 1000);
                average = s.r *delta_t + (1-delta_t)*average;
            }
            double ch = 0;
            double ch_t = 0;
            if(last_maxes.size() == 3) {
                ch = last_maxes.get(2).r - last_maxes.get(1).r + -(next_max.r - last_maxes.get(2).r);
                ch_t = (next_max.t - last_maxes.get(1).t)/1000000 /10;
            }
                fos.write(((s.t / 1000000) + ", " + s.r + ", 0, " + ch_t + ", " + ch + "\n").getBytes());


            if(last_sample == null) {
                last_sample = s;
                next_max = s;
            }
            else
            {
                //Log.d(TAG, String.valueOf(s.r));
                //Log.d(TAG, (s.r < R_NULL)+" "+(last_sample.r < R_NULL));
                if((s.r < average) != (last_sample.r < average))
                {
                    if(gotCrossing)
                    {
                        if(last_maxes.size() < 3)
                            last_maxes.offer(next_max);
                        else if((last_maxes.getLast().r > average) == (next_max.r > average))
                        {
                            last_maxes.pollLast();
                            last_maxes.offer(next_max);
                        }
                        else {
                            last_maxes.pollFirst();
                            last_maxes.offer(next_max);
                        }
                        if(last_maxes.size() == 3)
                        {
                            double change = last_maxes.get(1).r-last_maxes.get(0).r + -(last_maxes.get(2).r - last_maxes.get(1).r);
                            double change_t = (last_maxes.get(2).t-last_maxes.get(0).t) / 1000000;

                            if(change > threshold && change_t > t_threshold)
                            {
                                fos.write(((s.t / 1000000) + ", " + s.r + ", 1, 0, 0\n").getBytes());

                                for(int i = 0; i< mStepListeners.size(); ++i)
                                    mStepListeners.get(i).onStep(last_maxes.get(1).t);
                            }
                        }
                    }
                    else
                        gotCrossing = true;

                    next_max = new Sample();
                    next_max.r = average;
                }

                if(s.r > average)
                {
                    if(s.r > next_max.r)
                    {
                        next_max = s;
                        //Log.d(TAG, "updating max "+next_max.r);
                    }
                }
                else
                {
                    if(s.r < next_max.r)
                    {
                        next_max = s;
                        //Log.d(TAG, "updating min "+next_max.r);
                    }
                }
                last_sample = s;
            }
        }

            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}