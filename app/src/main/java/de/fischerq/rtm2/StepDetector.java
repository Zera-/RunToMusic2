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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 * @author Levente Bagi
 * @todo REFACTOR: SensorListener is deprecated
 */


public class StepDetector implements SensorEventListener
{
    private final static String TAG = "StepDetector";
    private class Sample
    {
        public double r = 0;
        public long t = 0;
    }
    private LinkedList<float[]> lastValues = new LinkedList<float[]>();
    private static final int filter_window = 3;

    boolean rising = true;
    private static final double R_NULL = 10;
    private static final double threshold = 17;

    private boolean gotCrossing = false;
    private Sample next_max = null;
    Sample last_sample = null;
    private LinkedList<Sample> last_maxes = new LinkedList<Sample>();

    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();
    
    public StepDetector() {
    }
    
    public void setSensitivity(float sensitivity) {
    }
    
    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }
    
    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if(!(sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {
            Log.d(TAG, "Bad sensor");
            return;
        }
        synchronized (this) {
            lastValues.offer(event.values);
            if(lastValues.size() > filter_window)
                lastValues.poll();

            float[] avg = new float[3];
            for(int i = 0; i < lastValues.size(); ++i)
            {
                float[] val = lastValues.get(i);
                for (int j=0 ; j<3 ; j++) {
                    avg[j] += val[j]/lastValues.size();
                }
            }

            double r = 0;
            for (int i=0 ; i<3 ; i++) {
               r+= avg[i]*avg[i];
            }
            r = Math.sqrt(r);

            Sample s = new Sample();
            s.r = r;
            s.t = event.timestamp;
            if(last_sample == null) {
                last_sample = s;
                if(s.r < R_NULL)
                    rising = false;
                else
                    rising = true;
                next_max = s;
            }
            else
            {
                //Log.d(TAG, String.valueOf(s.r));
                //Log.d(TAG, (s.r < R_NULL)+" "+(last_sample.r < R_NULL));
                if((s.r < R_NULL) != (last_sample.r < R_NULL))
                {
                    if(gotCrossing)
                    {
                        Log.d(TAG, "crossing, Adding a extr "+next_max.r);
                        last_maxes.offer(next_max);
                        if(last_maxes.size() > 3) {
                            last_maxes.poll();
                        }
                        if(last_maxes.size() == 3)
                        {
                            double change = last_maxes.get(1).r-last_maxes.get(0).r + -(last_maxes.get(2).r-last_maxes.get(1).r);
                            if(change > 2*threshold)
                            {
                                Log.d(TAG, "sending step "+last_maxes.get(1).r);
                                for(int i = 0; i< mStepListeners.size(); ++i)
                                    mStepListeners.get(i).onStep(last_maxes.get(1).t);
                            }
                        }
                    }
                    else
                        gotCrossing = true;

                    next_max = new Sample();
                    next_max.r = R_NULL;
                    rising = !rising;
                }

                if(rising)
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
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }

}