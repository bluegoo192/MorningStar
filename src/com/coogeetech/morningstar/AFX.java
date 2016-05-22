package com.coogeetech.morningstar;




import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.IBinder;
import android.os.Vibrator;

public class AFX {
	private MediaPlayer mp;
	Vibrator v;
	static final int KEYSTRIKE = 1;
	static final int ERROR = 0;
	static final int STARTUP = 2;
	static final int SWIPE = 3;
	static final int KEYSTRIKE2 = 4;
	Thread continuousSound;
	Context largerContext;
	Vibrator myV; 
	
	AFX(Context passContext){
//		setVolumeControlStream(AudioManager.STREAM_MUSIC);
//		onAudioFocusChange (AudioManager.AUDIOFOCUS_GAIN);
		largerContext = passContext;
		myV = (Vibrator) passContext.getSystemService(Service.VIBRATOR_SERVICE);
	}//AFX constructor
	
	public void play(Context passContext, int passSound){
		if (mp!=null)
			mp.release();
		switch(passSound){
		case KEYSTRIKE:
			mp = MediaPlayer.create(passContext, R.raw.switch1);
			mp.start();
			break;
		case ERROR:
			mp = MediaPlayer.create(passContext, R.raw.agogo);
//			mp.setOnErrorListener(new OnErrorListener() {
//				public boolean onError(MediaPlayer mp, int what, int extra) {
//			        mp.reset();
//			        return true;
//			    }
//			});
			mp.start();
			v = (Vibrator) passContext.getSystemService(Service.VIBRATOR_SERVICE);
			v.vibrate(500);
			break;
		case SWIPE:
				mp = MediaPlayer.create(passContext, R.raw.swing);
				mp.setOnErrorListener(new OnErrorListener() {
					public boolean onError(MediaPlayer mp, int what, int extra) {
				        mp.reset();
				        return true;
				    }
				});
				mp.start();

				break;
		case STARTUP:
			if (mp!=null)
				mp.release();
			mp = MediaPlayer.create(passContext, R.raw.startup_beep);
			mp.setOnErrorListener(new OnErrorListener() {
				public boolean onError(MediaPlayer mp, int what, int extra) {
			        mp.reset();
			        return true;
			    }
			});
			mp.start();
			break;
		case KEYSTRIKE2:
			mp = MediaPlayer.create(passContext, R.raw.button_19);
			mp.start();
			break;
		}
	}
	
}
