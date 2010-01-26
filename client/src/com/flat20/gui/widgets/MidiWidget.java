package com.flat20.gui.widgets;

import android.os.SystemClock;

import com.flat20.fingerplay.midicontrollers.IMidiController;
import com.flat20.fingerplay.midicontrollers.IOnControlChangeListener;

public abstract class MidiWidget extends Widget implements IMidiController {
/*
	final protected static int SHADOW_PADDING = 6;

	final protected MaterialSprite mShadow;

	// We need a skinning class
	final protected MaterialSprite mBackground;
	final protected MaterialSprite mOutline;
	final protected MaterialSprite mOutlineSelected;
	final protected MaterialSprite mTvScanlines;
*/
	//Stop sensor update flooding
	private long mlast_send_time = SystemClock.uptimeMillis();
	
	protected String mName = null;

	protected boolean mHold = false;

	public MidiWidget(String name) {
		super();

		setName(name);
	}

	public long getLastSendTime()
	{
		return mlast_send_time;
	}
	
	public void setName(String name) {
		mName = name;
	}
	public String getName() {
		return mName;
	}

	public void sendControlChange(int index, int value) {
		mlast_send_time = SystemClock.uptimeMillis();
		if (listener != null) {
			listener.onControlChange(this, index, value);
		}
	}

	public void sendNoteOn(int key, int velocity) {
		if (listener != null) {
			listener.onNoteOn(this, key, velocity);
		}
	}

	public void sendNoteOff(int key, int velocity) {
		if (listener != null) {
			listener.onNoteOff(this, key, velocity);
		}
	}

	@Override
	public void setHold(boolean hold) {
		mHold = hold;
		if (hold) {
			press(1.0f);
		} else {
			release(1.0f);
		}
	}

	public boolean isHolding() {
		return mHold;
	}

	protected void press(float pressure) {
		
	}

	protected void release(float pressure) {
		
	}



	protected IOnControlChangeListener listener;

    public void setOnControlChangeListener(IOnControlChangeListener l) {
    	listener = l;
    }

}
