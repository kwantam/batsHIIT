package org.jfet.batsHIIT;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundManager {
	private final SoundPool sndPool;
	private final Activity pContext;
	private int soundsLoaded;
	private final float maxVolume;
	private final AudioManager audioMan;
	
	public SoundManager(Activity appContext) {
		sndPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		sndPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool pool, int sndId, int status) {
				SoundManager.this.soundsLoaded += sndId;
			}
		});
		pContext = appContext;
		pContext.setVolumeControlStream(AudioManager.STREAM_MUSIC);
		audioMan = (AudioManager) pContext.getSystemService(Context.AUDIO_SERVICE);
		maxVolume = (float) audioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}
	
	public void pauseUntilLoaded(int soundsExpected) {
		while (this.soundsLoaded != soundsExpected) {
			try { Thread.sleep(100); }
			catch (InterruptedException ex) { break; }
		}
	}
	
	public int loadSound(int sndId) {
		return sndPool.load(pContext, sndId, 1);
	} // load a sound described by the resources sndId from context pContext

    public void playSound(int sndId) {
    	float pVol = audioMan.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume;
    	sndPool.play(sndId, pVol, pVol, 1, 0, 1.0f);
    } // play a sound that's been loaded, using present volume
}
