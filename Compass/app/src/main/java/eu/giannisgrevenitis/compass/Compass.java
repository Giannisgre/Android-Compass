package eu.giannisgrevenitis.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

public class Compass implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometerSensor, magnetometerSensor;
    private float[] mGravity = new float[3];
    private float[] mMagnetic = new float[3];
    //Azimuth is the degrees in the angle between a point of reference (e.g. North) 
	//and a line from the observer (user or device) to a point of interest
    private float azimuth = 0.0f;
    private float currentAzimuth = 0.0f;

	//An arrow to be rotated accordingly
    public ImageView arrow = null;

    public Compass(Context context){
		//Sensor Manager, Accelerometer and Magnetometer initialization
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy){}

    @Override
    public void onSensorChanged(SensorEvent sensorEvent){
        //alpha = t/(t+dT)
        //t = low-pass filter's time constant
        //dT = event delivery rate
        final float alpha = 0.8f;

		//The processes below are inside a synchronized block, so that "this" (meaning the sensorEvent) remains unchanged during the 
		//execution of the whole block to make sure that it's value doesn't get changed by another thread.
        synchronized (this){
            //For the Magnetometer
            if(sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
                mMagnetic[0] = alpha * mMagnetic[0] + (1 - alpha) * sensorEvent.values[0];
                mMagnetic[1] = alpha * mMagnetic[1] + (1 - alpha) * sensorEvent.values[1];
                mMagnetic[2] = alpha * mMagnetic[2] + (1 - alpha) * sensorEvent.values[2];
            }
            //For the Accelerometer
            if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
            }
			//R & I variables are necessary because the getRotationMatrix function cannot be passed null parameters
            float[] R = new float[9];
            float[] I = new float[9];
            //I = tilt angle
            //R = rotation matrix
            //R*Gravity = magnitude of gravity
            //I*R*geomagnetism = magnitude of geomagnetic field
            //Returns true on success, false in any other case. Calculates R and I by converting a vector from the device's coordinates system to a real world coordinates system.
            boolean flag = SensorManager.getRotationMatrix(R, I, mGravity, mMagnetic);

            if(flag){
                float[] orientation = new float[3];
                //Get the orientaion
                SensorManager.getOrientation(R, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]);
                changeArrowPositioning();
            }
        }
    }

    public void start(){
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop(){
        sensorManager.unregisterListener(this);
    }

    public void changeArrowPositioning(){
		//If the arrow's view has not been initialized
        if(arrow == null){
            return;
        }
		//Else, i create an animation using the current azimuth(at application startup = 0.0) and the azimuth
        Animation animation = new RotateAnimation(currentAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        currentAzimuth = -azimuth;

        animation.setRepeatCount(0);    //Times to repeat the animation
        animation.setFillAfter(true);   //The change done during the animation will persist when the animation finishes
        animation.setDuration(500);     //Animation duration
        arrow.startAnimation(animation); //Call crafted animation on the compass' ImageView
    }
}