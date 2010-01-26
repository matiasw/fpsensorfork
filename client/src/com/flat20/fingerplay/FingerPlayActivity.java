package com.flat20.fingerplay;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.widget.Toast;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.util.Log;

import com.flat20.fingerplay.midicontrollers.MidiControllerManager;
import com.flat20.fingerplay.network.ConnectionManager;
import com.flat20.fingerplay.settings.SettingsModel;
import com.flat20.fingerplay.settings.SettingsView;
import com.flat20.gui.InteractiveActivity;
import com.flat20.gui.NavigationOverlay;
import com.flat20.gui.animations.AnimationManager;
import com.flat20.gui.animations.Splash;
import com.flat20.gui.sprites.Logo;
import com.flat20.gui.widgets.MidiWidgetContainer;
import com.flat20.gui.LayoutManager;

public class FingerPlayActivity extends InteractiveActivity implements SensorEventListener {

	private SettingsModel mSettingsModel;

    private MidiControllerManager mMidiControllerManager;

    private MidiWidgetContainer mMidiWidgetsContainer;

    private Logo mLogo;

    private NavigationOverlay mNavigationOverlay; 
 
    public SensorManager sensorManager;

    private List<Sensor> sensors;

    @Override
    public void onCreate(Bundle savedInstanceState) {

    	// Init needs to be done first!
		mSettingsModel = SettingsModel.getInstance();
		mSettingsModel.init(this);

		mMidiControllerManager = MidiControllerManager.getInstance();        

		super.onCreate(savedInstanceState);

        Runtime r = Runtime.getRuntime();
        r.gc();

        Toast info = Toast.makeText(this, "Go to http://thesundancekid.net/ for help.", Toast.LENGTH_LONG);
        info.show();

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        sensors = new ArrayList<Sensor>(sensorManager.getSensorList(Sensor.TYPE_ALL));
     
        // Simple splash animation

        Splash navSplash = new Splash(mNavigationOverlay, 64, 190, mWidth, mNavigationOverlay.x);
        mNavigationOverlay.x = mWidth;
        AnimationManager.getInstance().add(navSplash);

        Splash mwcSplash = new Splash(mMidiWidgetsContainer, 64, 200, -mWidth, mMidiWidgetsContainer.x);
        mMidiWidgetsContainer.x = -mWidth;
        AnimationManager.getInstance().add(mwcSplash);
    }

    /**
     * Test of the new TestRenderer which groups Textures together
     * so OpenGL only needs to render one texture at a time.
     */
    /*
	@Override
    protected void onCreateView() {
		mWidgetRenderer = new TestRenderer(mWidth, mHeight);
		mRenderer = mWidgetRenderer;
        mView = new GLSurfaceView(this);
        mView.setRenderer(mRenderer);
    }*/


	@Override
	protected void onCreateGraphics() {

		// Draw the FingerPlay logo as our background.
		// Logo uses screenWidth and height and tries to fill it
		mLogo = new Logo(mWidth, mHeight);
		mRenderer.addSprite(mLogo);

        // We're drawing all controller screens in their own container so we can move them
        // separately from the navigation and the background.
        // MidiWidgetContainer calculates its height depending on the content added.
        mMidiWidgetsContainer = new MidiWidgetContainer(mWidth, mHeight);
        mMidiWidgetsContainer.z = 1.0f;

        // TODO Make LayoutManager part of GUI lib
        File xmlFile = new File(Environment.getExternalStorageDirectory() + "/FingerPlayMIDI/" + mSettingsModel.layoutFile);
        //Log.i("FPA", "mSettingsModel.layoutFile = " + mSettingsModel.layoutFile);
        if (xmlFile != null && xmlFile.canRead())
        	LayoutManager.loadXML(mMidiWidgetsContainer, xmlFile, mWidth, mHeight);
        else
        	LayoutManager.loadXML(mMidiWidgetsContainer, getApplicationContext().getResources().openRawResource(R.raw.layout_default), mWidth, mHeight);
 
        // Add all midi controllers to the manager
        mMidiControllerManager.addMidiControllersIn(mMidiWidgetsContainer);

        mRenderer.addSprite( mMidiWidgetsContainer );

		// Navigation
        mNavigationOverlay = new NavigationOverlay(64, mHeight-16, mNavigationListener, mMidiWidgetsContainer, mHeight);
        mNavigationOverlay.x = mWidth - mNavigationOverlay.width+2;
        mNavigationOverlay.y = 8;//dm.heightPixels/2 - navigationScreen.height/2;
        mNavigationOverlay.z = 2.0f;
        //mNavigationButtons.setScreenHeight( 320 );
        // Navigation goes on top.
        mRenderer.addSprite( mNavigationOverlay );

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		mMidiWidgetsContainer.onKeyDown(keyCode, event);
		return super.onKeyDown(keyCode, event);
	}


	NavigationOverlay.IListener mNavigationListener = new NavigationOverlay.IListener() {

		@Override
		public void onReleaseAllSelected() {
			mMidiControllerManager.releaseAllHeld();
		}

		@Override
		public void onSettingsSelected() {
			Intent settingsIntent = new Intent(getApplicationContext(), SettingsView.class);
			settingsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			startActivity( settingsIntent );
		}
/*
		@Override
		public void onScroll(float pos) {
			Log.i("FPA", "onScroll " + pos + " = " + (pos*mMidiWidgetsContainer.height));
			mMidiWidgetsContainer.scrollTo((int) -(pos*mMidiWidgetsContainer.height));
		}
*/
	};

	@Override
	protected void onResume() {
		super.onResume();
		for (int i = 0; i < sensors.size(); i++)
			sensorManager.registerListener(this, sensors.get(i), SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected void onPause() {
		super.onPause();
		for (int i = 0; i < sensors.size(); i++)
			sensorManager.unregisterListener(this, sensors.get(i));
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    		//Log.d("ACCU", String.format("onAccuracyChanged  sensor: %d   accuraccy: %d", sensor, accuracy));
  	}

	public void onSensorChanged(SensorEvent e) {
		float[] values = e.values;
/*
  		int sensorReporting = e.sensor.getType();
		String str = "Sensor " + sensorReporting + " changed: ";
		for (int i = 0; i < values.length; i++)
			str += " " + values[i] + " ";
		Log.i("SENSOR", str);
*/
		mMidiControllerManager.onSensorChanged(e.sensor, values);
	}

	@Override
	protected void onDestroy() {
    	ConnectionManager.getInstance().cleanup();
		super.onDestroy();

		System.runFinalizersOnExit(true);
		System.exit(0);
	}

}
