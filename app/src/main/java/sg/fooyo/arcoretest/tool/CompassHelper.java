package sg.fooyo.arcoretest.tool;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.support.v4.math.MathUtils;
import android.util.Log;
import android.view.Display;

import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;

import java.util.Arrays;

import static android.hardware.SensorManager.SENSOR_DELAY_FASTEST;
import static sg.fooyo.arcoretest.CommonKt.axisRotation;

/**
 * Created by inio on 2/28/18.
 */

public class CompassHelper implements SensorEventListener {
    private float[] accumulated = new float[3];
    private Pose deviceToWorld;
    private final DisplayRotationHelper rotationHelper;
    private SensorManager sensorManager;


    private final float DECAY_RATE = 0.9f;
    private final float SQRT_HALF = (float) Math.sqrt(0.5f);
    private final float VALID_TRESHOLD = 0.1f;

    public CompassHelper(Context context) {
        this.rotationHelper = new DisplayRotationHelper(context);
        sensorManager = context.getSystemService(SensorManager.class);
    }

    public void onResume() {
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor
                .TYPE_MAGNETIC_FIELD), SENSOR_DELAY_FASTEST /* 5Hz */);
    }

    public void onUpdate(Frame frame) {
        deviceToWorld = frame.getAndroidSensorPose();
        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            for (int i = 0; i < 3; ++i) {
                accumulated[i] = 0;
            }
        }
    }

    public void onPause() {
        sensorManager.unregisterListener(this);
    }

    public void getFieldDirection(float[] out) {
        System.arraycopy(accumulated, 0, out, 0, 3);
    }

    public boolean rotationValid() {
        return (accumulated[0] * accumulated[0] + accumulated[2] * accumulated[2]) > VALID_TRESHOLD;
    }

    /**
     * Returns the rotation about the Y axis (in radians) that results in the local X axis
     * pointing east.
     */
    public float rotateXToEastAngle() {
        if (!rotationValid()) {
            return 0;
        }
        float eastX = accumulated[0];
        float eastZ = accumulated[2];
        // negative because positive rotation about Y rotates X away from Z
        return (float) (-(float) Math.atan2(eastZ, eastX) + Math.PI / 2);
    }

    public Pose rotateXToEastPose() {

        return axisRotation(1, rotateXToEastAngle());
    }

    private Pose getDevicePose(Frame frame) {
        // Cheat: Pose.makeInterpolated for rotation multiplication
        return frame.getCamera().getDisplayOrientedPose().compose(
                Pose.makeInterpolated(
                        Pose.IDENTITY,
                        Pose.makeRotation(0, 0, SQRT_HALF, SQRT_HALF),
                        rotationHelper.getRotation()));
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) return;
        float[] rotated = new float[3];
        if (deviceToWorld != null) {
            deviceToWorld.rotateVector(sensorEvent.values, 0, rotated, 0);
            for (int i = 0; i < 3; ++i) {
                accumulated[i] =rotated[i];
            }
            if (callBack != null)
                callBack.callBack(sensorEvent.values[0] + "," + accumulated[0]);
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Should probably do something here...
    }


    private CallBack callBack;

    public void setCallBack(CallBack callBack) {
        this.callBack = callBack;
    }

    public interface CallBack {
        void callBack(String value);
    }
}
