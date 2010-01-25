package com.flat20.fingerplay.midicontrollers;


import java.util.LinkedHashMap;

import java.util.Set;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.util.Log;
import android.os.SystemClock;

import com.flat20.fingerplay.network.ConnectionManager;
import com.flat20.fingerplay.socket.commands.midi.MidiControlChange;
import com.flat20.fingerplay.socket.commands.midi.MidiNoteOff;
import com.flat20.fingerplay.socket.commands.midi.MidiNoteOn;
import com.flat20.fingerplay.socket.commands.midi.MidiSocketCommand;
import com.flat20.fingerplay.socket.commands.SocketCommand;
import com.flat20.gui.widgets.MidiWidget;
import com.flat20.gui.widgets.Widget;
import com.flat20.gui.widgets.WidgetContainer;
import com.flat20.gui.widgets.XYPad;

public class MidiControllerManager {

    private LinkedHashMap<IMidiController, Integer> mMidiControllers = new LinkedHashMap<IMidiController, Integer>();
	private int mControllerIndex = 0;

	private ConnectionManager mConnectionManager = ConnectionManager.getInstance();

	//Stop sensor update flooding
	private static long mlast_send_time = SystemClock.uptimeMillis();
	private final long SEND_DELAY = 50;	//milliseconds	

	// Singleton
	private static MidiControllerManager mInstance = null;
	public static MidiControllerManager getInstance() {
		if (mInstance == null)
			mInstance = new MidiControllerManager();
		return mInstance;
	}

	private MidiControllerManager() {
		mConnectionManager.addConnectionListener(mConnectionListener);
	}

    public void addMidiController(IMidiController midiController) {
		midiController.setOnControlChangeListener( onControlChangeListener );
    	mMidiControllers.put(midiController, Integer.valueOf(mControllerIndex));
    	mControllerIndex += midiController.getParameters().length;
    }

    public Set<IMidiController> getMidiControllers() {
    	Set<IMidiController> mcs = (Set<IMidiController>) mMidiControllers.keySet();
    	return mcs;//(IMidiController[]) mMidiControllers.keySet().toArray();
    }

    public IMidiController getMidiControllerByName(String name) {
    	Set<IMidiController> mcs = (Set<IMidiController>) mMidiControllers.keySet();
    	for (IMidiController mc : mcs) {
    		if (mc.getName().equals(name)) {
    			return mc;
    		}
    	}
    	return null;
    }

	public int getIndex(IMidiController midiController) {
		return (int) mMidiControllers.get(midiController);
	}

	// Add all midi controllers inside widgetContainer
	public void addMidiControllersIn(WidgetContainer widgetContainer) {
		Widget[] widgets = widgetContainer.getWidgets();
        for (int i=0; i<widgets.length; i++) {
        	Widget w = widgets[i];
        	if (w instanceof MidiWidget) {
				MidiWidget midiWidget = (MidiWidget) w;
				midiWidget.setOnControlChangeListener( onControlChangeListener );
	        	addMidiController( midiWidget );
        	} else if (w instanceof WidgetContainer) {
				WidgetContainer wc = (WidgetContainer) w;
				addMidiControllersIn(wc);
			}
        }
	}

	public void releaseAllHeld() {
		Set<IMidiController> controllers = getMidiControllers();
    	for (IMidiController mc : controllers) {
    		if (mc.isHolding())
    			mc.setHold(false);
    	}
	}

	private IOnControlChangeListener onControlChangeListener = new IOnControlChangeListener() {

		// Cached to limit garbage collects. 
		final private MidiControlChange mControlChange = new MidiControlChange();
		final private MidiNoteOn mNoteOn = new MidiNoteOn();
		final private MidiNoteOff mNoteOff = new MidiNoteOff();

		@Override
    	public void onControlChange(IMidiController midiController, int index, int value) {
			//if (mConnectionManager.isConnected()) {
				int ccIndex = (int) getIndex(midiController);
				mControlChange.set(0xB0, 0, ccIndex+index, value);
				//SocketCommand socketCommand = new MidiControlChange(0, 0, ccIndex + index, value);
				mConnectionManager.send( mControlChange );
			//}
    	}

    	@Override
    	public void onNoteOn(IMidiController midiController, int key, int velocity) {
    		//if (mConnectionManager.isConnected()) {
				int controllerIndex = (int) getIndex(midiController);
				// midi channel, key, velocity
				mNoteOn.set(0, controllerIndex, velocity);
				//socketCommand = new MidiNoteOn(0, controllerIndex, velocity);
				mConnectionManager.send( mNoteOn );
    		//}
    	}

    	@Override
    	public void onNoteOff(IMidiController midiController, int key, int velocity) {
    		//if (mConnectionManager.isConnected()) {
				int controllerIndex = (int) getIndex(midiController);
				//socketCommand = new MidiNoteOff(0, controllerIndex, velocity);
				mNoteOff.set(0, controllerIndex, velocity);
				mConnectionManager.send(mNoteOff);
    		//}
    	}

    };

    // Handler receiving MIDI commands from the server.
    // TODO Implement 
    private ConnectionManager.IConnectionListener mConnectionListener = new ConnectionManager.IConnectionListener() {

    	public void onConnect() {
    	}

    	public void onDisconnect() {
    	}

    	public void onError(String errorMessage) {
    	}

    	public void onSocketCommand(SocketCommand sm) {
			if (sm.command == SocketCommand.COMMAND_MIDI_SHORT_MESSAGE) {
				Log.i("mcm", "server sent cc message");
				MidiSocketCommand msc = (MidiSocketCommand) sm;
				Log.i("mcm", " msc = " + msc);
			}
    	}
    };
    
    public void onSensorChanged(Sensor sensor, float[] sensorValues)
	{
    	//Stop sensor update flooding
    	if (SystemClock.uptimeMillis() < mlast_send_time + SEND_DELAY)
    		return;
    	MidiWidget mw;
    	float[] val;
		//Scale values depending on sensor type:
    	mlast_send_time = SystemClock.uptimeMillis();
		switch (sensor.getType())
		{
		case Sensor.TYPE_ACCELEROMETER:	//A constant describing an accelerometer sensor type.
/*
			values[0]: Acceleration minus Gx on the x-axis 
			values[1]: Acceleration minus Gy on the y-axis 
			values[2]: Acceleration minus Gz on the z-axis
 */
			mw = (MidiWidget)getMidiControllerByName("Sensor accelerometer");
			if (mw == null)
			{
				Log.e("SENSOR", "No sensor \"accelerometer\" (type " + sensor.getType() + ") found!");
				return;
			}
			val = new float[3];
			float max = sensor.getMaximumRange() * SensorManager.STANDARD_GRAVITY;	//max is reported in g's
			//scale to 0..1 range (from - max..+max):
			val[0] = (sensorValues[0] + max) / (2 * max);
			val[1] = (sensorValues[1] + max) / (2 * max);
			val[2] = (sensorValues[2] + max - SensorManager.STANDARD_GRAVITY) / (2 * max);	//TODO Futureproof me ;)
			if (mw instanceof XYPad) {
				((XYPad)mw).setMeterx((int)(val[0]*128.0f));
				((XYPad)mw).setMetery((int)(val[1]*128.0f));
			}
			mw.sendControlChange(0, (int)(val[0]*128.0f));
			mw.sendControlChange(1, (int)(val[1]*128.0f));
			mw.sendControlChange(2, (int)(val[2]*128.0f));
		break;
		case Sensor.TYPE_GYROSCOPE:	//A constant describing a gyroscope sensor type
		
		break;
		case Sensor.TYPE_LIGHT:	//A constant describing an light sensor type.
			
		break;
		case Sensor.TYPE_MAGNETIC_FIELD:	//A constant describing a magnetic field sensor type.
			
		break;
		case Sensor.TYPE_ORIENTATION:	//A constant describing an orientation sensor type.
/*			values[0]: Azimuth, angle between the magnetic north direction and the Y axis, around the Z axis (0 to 359). 0=North, 90=East, 180=South, 270=West 
			values[1]: Pitch, rotation around X axis (-180 to 180), with positive values when the z-axis moves toward the y-axis. 
			values[2]: Roll, rotation around Y axis (-90 to 90), with positive values when the x-axis moves away from the z-axis.
*/			
			mw = (MidiWidget)getMidiControllerByName("Sensor orientation");
			if (mw == null)
			{
				Log.e("SENSOR", "No sensor \"orientation\" (type " + sensor.getType() + ") found!");
				return;
			}
			val = new float[3];
			//scale to 0..1 range:
			val[0] = sensorValues[0]/359.0f;
			val[1] = (sensorValues[1]+180.0f)/359.0f;
			val[2] = (sensorValues[2]+90.0f)/180.0f;
			if (mw instanceof XYPad) {
				((XYPad)mw).setMeterx((int)(val[2]*128.0f));
				((XYPad)mw).setMetery((int)(val[1]*128.0f));
			}
			mw.sendControlChange(0, (int)(val[0]*128.0f));
			mw.sendControlChange(1, (int)(val[1]*128.0f));
			mw.sendControlChange(2, (int)(val[2]*128.0f));
		break;
		case Sensor.TYPE_PRESSURE:	//A constant describing a pressure sensor type
		
		break;
		case Sensor.TYPE_PROXIMITY:	//A constant describing an proximity sensor type.
		
		break;
		case Sensor.TYPE_TEMPERATURE:	//A constant describing a temperature sensor type
		
		break;
		}
	}
}