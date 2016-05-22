package com.coogeetech.morningstar;


import android.inputmethodservice.Keyboard;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.ViewDebug.IntToString;

public class SwipeTouchPoint_old {
	
	public final float DISTANCE_SWIPE =  6.0f;
	public final float VELOCITY_SWIPE = 100f;//200.0f;
	public final float VELOCITY_TRACK = 1.0f;// 20.0f;
	public final float VELOCITY_FEEDBACK = 1.0f;
	public final float ANGLE_TOLERANCE = 80.0f;

	public float fastDeltaX = 0;
	public float fastDeltaY = 0;
	public long  fastDeltaTime = 0;
	
	public float lastX = 0;
	public float lastY = 0;
	public long  lastTime = 0;
	
	public float startX = 0;
	public float startY = 0;
	public long  startTime = 0;
    public boolean isStartSet = false;
	
	public float endX = 0;
	public float endY = 0;
	public long  endTime = 0;
    public boolean isEndSet = false;

	public boolean isWatching = false;
	public boolean isCancel = false;
	public boolean isValid = false;
	public boolean isSwipe = false;
	public boolean isAligned = false;
	public boolean isResting = false;
	
	public float pressureAvg = 0;
	public float pressureMax = 0;
	float currentPressure = 0;
	int totHistory = 0;
	int historySize=0;
	
	
	public int pid = 0;  // pointer id to identify the motion
	
	public Keyboard.Key key;

	int keyCodeAngleListIndex = -1;
	
	int[]    angleList   = null;  // swipe angles in degrees key responds to 
	String[] keyCodeList = null;  // keycode for each swipe angle
	
	public SwipeTouchPoint_old(int pointerID, Keyboard.Key key) {
		this.pid = pointerID;
		this.key = key;
	}
	
	public void reset() {
		// clear state except key field pid
		key = null;
		angleList = null; 
		keyCodeList = null;
		keyCodeAngleListIndex = -1;
		fastDeltaX = fastDeltaY = lastX = lastY = 0;
		fastDeltaTime = lastTime = 0;
		startX = startY = endX = endY = 0;
		startTime = endTime = 0;
		isStartSet = isEndSet = false;
		isWatching = isCancel = isValid = isSwipe = false;
	}
	
	
    public int getPointerIndex( MotionEvent ev ) {
      // RRS: get index of pointer in MotionEvent object this SwipeTouchPoint object is tracking
      for (int i = 0; i < ev.getPointerCount(); i++) {
        if( ev.getPointerId(i) == this.pid ) {
  		  return i;
  		  }
  	    }
      return -1;
    }
	
	public void process_action_down( MotionEvent mv ) {	
		int pointerIndex = getPointerIndex(mv);
		isCancel = false;   // TODO are these necissary if a new object of Swipe Touch is created ever time?
		isSwipe = false;
		isStartSet = true;
		isEndSet = false;
		isWatching = true;
		fastDeltaX = 0;
		fastDeltaY = 0;
		fastDeltaTime = 0;
		startX    = lastX    = mv.getX(pointerIndex);
		startY    = lastY    = mv.getY(pointerIndex);
		startTime = lastTime = mv.getEventTime();
		keyCodeAngleListIndex = -1;
		// validity check: is pointer within allowable distance of key boundary?
		isValid = isValidXY( startX, startY ); 
	}
	
	public void process_action_move( MotionEvent mv ) { //sometimes touchpoint hasn't changed!! calls aren't made at regular intervals!! TODO
		if( isWatching ) {
		  int pointerIndex = getPointerIndex(mv);
		  historySize = mv.getHistorySize();
		  
		  totHistory += historySize+1;
          float x,y;
          long t;
          for (int h = 0; h < historySize+1; h++) {
        	if( h < historySize ) {
        		x = mv.getHistoricalX(pointerIndex, h); //why
        		y = mv.getHistoricalY(pointerIndex, h);
        		t = mv.getHistoricalEventTime(h);

        		if (mv.getHistoricalPressure(pointerIndex, h)>pressureMax)
        			pressureMax = mv.getHistoricalPressure(pointerIndex, h);
        	} else {
        		x = mv.getX(pointerIndex);   
        		y = mv.getY(pointerIndex);
        		t = mv.getEventTime();
        		pressureAvg = pressureAvg*(historySize/(historySize+1)) + (mv.getPressure(pointerIndex)/(historySize+1));
        		if (mv.getPressure(pointerIndex)>pressureMax)
        			pressureMax = mv.getPressure(pointerIndex);
        	}
            float deltaD_sqr = (x-lastX)*(x-lastX) + (y-lastY)*(y-lastY);
            float deltaT     = (1/(float)1000.0) * (float)(t-lastTime);
            if( deltaT>0 && deltaD_sqr/(deltaT*deltaT) >= VELOCITY_TRACK*VELOCITY_TRACK ) {
            	fastDeltaX    = fastDeltaX    + (x-lastX);
            	fastDeltaY    = fastDeltaY    + (y-lastY);
            	fastDeltaTime = fastDeltaTime + (t-lastTime);   // TODO this is inefficient, we don't need to recalculate this every time, just update it
            } //for
    		lastX    = x;
    		lastY    = y;
    		lastTime = t;		
    		currentPressure = mv.getPressure(pointerIndex);
          }
		  endX = lastX;
		  endY = lastY;
		  endTime = lastTime;		
		  isEndSet = true;
		  
		  
		  // update candidate KeyCode and Angle index if any
		  updateKeyCodeAngleListIndex();
		  // validity check: none for now...
		}
	}
	
	public void process_action_up( MotionEvent mv ) {	
		if( isWatching ) {
		  // make sure any final motion is processed
		  process_action_move(mv);
		  isWatching = false;
		  // validity check: none for now...
		  // does recent motion qualify as a swipe?
		  if( isValidSwipe() ) isSwipe=true;
		}
	}
	
	public boolean isValidXY( float x, float y ) {
		// validity check: is pointer within allowable distance of key boundary?
		if( key != null ) {
	      int touchX = (int) x - MorningStar.mPaddingLeft;
	      int touchY = (int) y + MorningStar.mVerticalCorrection - MorningStar.mPaddingTop;
	      // test if still inside key boudary as a placeholder; need something more sophisticated 
	      return key.isInside(touchX,touchY);
		}
	    return false;
	}
	
	//This tests to see if the user has made a swipe which is in between keys     TODO non robust
	public boolean isDriftingSwipe(){
		if(distance_squared() > DISTANCE_SWIPE*DISTANCE_SWIPE && velocity() >= VELOCITY_SWIPE && (!isValid))
			return true;
		return false;
	} //isDriftingSwipe
	
	public boolean isValidSwipe() {
		// validity check: is pointer within allowable distance of key boundary?
		// placeholder rules need to be replaced with more sophisticated logic
		updateKeyCodeAngleListIndex();
		if( key!=null && isValid && !isCancel &&
				keyCodeList!=null && keyCodeAngleListIndex >= 0 &&
				distance_squared() > DISTANCE_SWIPE*DISTANCE_SWIPE && 
				velocity() >= VELOCITY_SWIPE ) {
			return true;
		}
	    return false;
	}
	
	public double time() {
		// get distance^2 between start and most recent end pointer positions, if both set
		return ((double)fastDeltaTime)/1000.0; 
	}
	
	public float distance_squared() {
		// get distance^2 between start and most recent end pointer positions, if both set
		if( isStartSet && isEndSet ) {
			return distance_squared (fastDeltaX, fastDeltaY); 
		} else {
			return 0;
		}
	}
	
	public float distance_squared( float fastDeltaX, float fastDeltaY ) {
		// get distance^2 between arbitrary pairs of x and y coordinates
		return fastDeltaX*fastDeltaX + fastDeltaY*fastDeltaY;	
	}
	
	public double velocity() {
		// return velocity in pixels/second or 0 if pair of endpoints not available
		if( isStartSet && isEndSet ) {
			float dsqr = distance_squared( lastX-startX, lastY-startY );
			//float dsqr = distance_squared( fastDeltaX, fastDeltaY );
			long milliseconds = endTime - startTime;
			//long milliseconds = fastDeltaTime;
			if( dsqr > 0 && milliseconds > 0 ) {
				return Math.sqrt( (double) dsqr ) / ( (double) milliseconds / 1000.0 );
			}
		}
		return Double.NaN;		
	}

	public double theta() {
		// return theta in degrees or 0 if pair of endpoints not available
		if( isStartSet && isEndSet ) {
			float dx = endX - startX;
			float dy = endY - startY;
			if( dx != 0 || dy != 0 ) {
				return 90.0 - (180.0/3.141592) * Math.atan2( (double) dy, (double) dx );
			}
		}
		return Double.NaN;
	}
	
	public void updateKeyCodeAngleListIndex() {
		this.keyCodeAngleListIndex = getKeyCodeAngleListIndex();
	}
	
	public int getKeyCodeAngleListIndex() {
		int keyIndex = -1;
		double theta = this.theta();
		if( angleList != null ) {
			for( int i=0; i<angleList.length; i++ ) {
				//int[] angleList = null; // swipe angles in degrees key responds to 
				//String[] keyCodeList = null;
				double angle_diff = theta - (double)angleList[i];
				while( angle_diff<-180.0 ) angle_diff=angle_diff+360.0;
				while( angle_diff>+180.0 ) angle_diff=angle_diff-360.0;
				if( Math.abs(angle_diff) <= ANGLE_TOLERANCE ) {
					keyIndex = i;
				}
			}
		}
		return keyIndex;
	}

}
