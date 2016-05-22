package com.coogeetech.morningstar;


import android.inputmethodservice.Keyboard;
import android.util.FloatMath;
import android.view.MotionEvent;
import android.view.ViewDebug.IntToString;

public class SwipeTouchPoint {
	
	//=================================================================================
	//         Declarations
	//=================================================================================	

	private final float ANGLE_TOLERANCE = 22.0f;
	private final float TIME_FOR_TAP = 250f; //in ms //and 500
	private final float TIME_FOR_SWIPE = 250f; //in ms
	private final float DISTANCE_SWIPE =  MorningStar.DENSITY*5.0f; //in dip	
	public final float  TIME_FOR_CONCURRENCY = 20f;//in ms
	
	private int fingerAssignment = -1;
	public final static int RIGHT_INDEX = 4;
	public final static int LEFT_INDEX = 7;
	public final static int UNKNOWN_FINGER = -1;
		
	private float startX = 0;
	private float startY = 0;
	private long  startTime = 0;
	private float startSize = 0; //starting finger size
	private int numDataPoints = 0;
	private double uniqueCode;
	
    private float endX = 0;
	float endY = 0;
	private long  endTime = 0;

	private boolean hasPotential = true; //does this swipe still have potential to become a valid entry?
	private boolean isSwipe = false; //does this touchpoint meet the swipe requirements?
	private boolean isAligned = false; //has this touchpoint been used to aligned the keyboard?
	private boolean isTap = false; //does this touchpoint meet the tap requirements?
	//private boolean wantsAlignment = false; //should this touchpoint be aligned?
	private boolean isFinished = false; //has this touchpoint been lifted?
		
	public int pid;  // pointer id to identify the motion
	
	private Keyboard.Key _key;
	
	int keyCodeAngleListIndex = -1;
	
	int[]    angleList   = null;  // swipe angles in degrees key responds to 
	String[] keyCodeList = null;  // keycode for each swipe angle
	
	//=================================================================================
	//         End Declarations
	//=================================================================================
	
	//=================================================================================
	//         get and set functions
	//=================================================================================
	
	boolean 	getIsAligned(){return isAligned;}
	void 		setIsAligned(boolean passIsAligned){isAligned = passIsAligned;}
	void 		cancelSwipe(){	hasPotential = false;
						isSwipe = false;
						isTap = false;}
	long 		getStartTime(){return startTime;}
	long		getEndTime(){return endTime;}
	double 		getDistance(){return Math.sqrt((endX-startX)*(endX-startX)+(endY-startY)*(endY-startY));}
	float 		getTimeDiff(){return endTime - startTime;}
	//void 		setWantsAlignment (Boolean passWantsAlignment){wantsAlignment = passWantsAlignment;}
	//Boolean 	getWantsAlignment(){return wantsAlignment;}
	Boolean 	getIsSwipe(){return isSwipe;}
	Boolean 	getIsTap(){return isTap;}
	Boolean		getIsFinished(){return isFinished;}
	Boolean		getHasPotential(){return hasPotential;}
	float		getStartX(){return startX;}
	float		getStartY(){return startY;}
	float		getStartSize(){return startSize;}
	Keyboard.Key getKey(){	if (hasPotential) return (Keyboard.Key)_key;
	else return null;}
	void		setFinger(int passAssignemnt){fingerAssignment = passAssignemnt;}
	int			getFinger(){return fingerAssignment;}
	double		getUniqueCode(){return uniqueCode;}
	
	
	//=================================================================================
	//         end get and set functions
	//=================================================================================
	
	public SwipeTouchPoint(int pointerID, Keyboard.Key passKey) {
		this.pid = pointerID;
		this._key = passKey;
		uniqueCode = Math.random();
		
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
		startX = mv.getX(pointerIndex);
		startY = mv.getY(pointerIndex);
		startTime = mv.getEventTime();
		startSize=mv.getSize();
		keyCodeAngleListIndex = -1;
		// validity check: is pointer within allowable distance of key boundary?
		hasPotential = true;//isValidXY( startX, startY ); 
		numDataPoints++;
	}
	
	public void process_action_move(MotionEvent mv){
		numDataPoints += mv.getHistorySize() + 1;
		
		int pointerIndex = getPointerIndex(mv);
		endX = mv.getX(pointerIndex);
		endY = mv.getY(pointerIndex);
		endTime = mv.getEventTime();
		if (hasPotential){
		if (getDistance() > DISTANCE_SWIPE && getTimeDiff() < TIME_FOR_SWIPE)
			isSwipe = true;
		//else if (endTime-startTime < TIME_FOR_TAP)
			//isTap = true;
		//else hasPotential = false;
		}
		updateKeyCodeAngleListIndex();
	}
	/*
//	public void process_action_move( MotionEvent mv ) { //sometimes touchpoint hasn't changed!! calls aren't made at regular intervals!! TODO
//		if( isWatching ) {                           //unnecessary if just using start and stop points
//		  int pointerIndex = getPointerIndex(mv);
//		  historySize = mv.getHistorySize();
//		  
//		  totHistory += historySize+1;
//          float x,y;
//          long t;
//          for (int h = 0; h < historySize+1; h++) {
//        	if( h < historySize ) {
//        		x = mv.getHistoricalX(pointerIndex, h); //why
//        		y = mv.getHistoricalY(pointerIndex, h);
//        		t = mv.getHistoricalEventTime(h);
//
//        		if (mv.getHistoricalPressure(pointerIndex, h)>pressureMax)
//        			pressureMax = mv.getHistoricalPressure(pointerIndex, h);
//        	} else {
//        		x = mv.getX(pointerIndex);   
//        		y = mv.getY(pointerIndex);
//        		t = mv.getEventTime();
//        		pressureAvg = pressureAvg*(historySize/(historySize+1)) + (mv.getPressure(pointerIndex)/(historySize+1));
//        		if (mv.getPressure(pointerIndex)>pressureMax)
//        			pressureMax = mv.getPressure(pointerIndex);
//        	}
//            float deltaD_sqr = (x-lastX)*(x-lastX) + (y-lastY)*(y-lastY);
//            float deltaT     = (1/(float)1000.0) * (float)(t-lastTime);
//            if( deltaT>0 && deltaD_sqr/(deltaT*deltaT) >= VELOCITY_TRACK*VELOCITY_TRACK ) {
//            	fastDeltaX    = fastDeltaX    + (x-lastX);
//            	fastDeltaY    = fastDeltaY    + (y-lastY);
//            	fastDeltaTime = fastDeltaTime + (t-lastTime);   // TODO this is inefficient, we don't need to recalculate this every time, just update it
//            } //for
//    		lastX    = x;
//    		lastY    = y;
//    		lastTime = t;		
//    		currentPressure = mv.getPressure(pointerIndex);
//          }
//		  endX = lastX;
//		  endY = lastY;
//		  endTime = lastTime;		
//		  isEndSet = true;
//		  
//		  
//		  // update candidate KeyCode and Angle index if any
//		  updateKeyCodeAngleListIndex();
//		  // validity check: none for now...
//		}
//	}
	*/
	
	public void process_action_up( MotionEvent mv ) {	//only used on completed swipes!!!
		//if( isWatching ) {
			int pointerIndex = getPointerIndex(mv);
			endX = mv.getX(pointerIndex);
			endY = mv.getY(pointerIndex);
			endTime = mv.getEventTime();
			numDataPoints++;
			if (hasPotential){
			if (getDistance() > DISTANCE_SWIPE && getTimeDiff() < TIME_FOR_SWIPE)
				isSwipe = true;
			else if (getTimeDiff() < TIME_FOR_TAP)
				isTap = true;
			else hasPotential = false;
			}
			updateKeyCodeAngleListIndex();
			
			isFinished = true;
			
		  // make sure any final motion is processed
		  //process_action_move(mv);
		  //isWatching = false;
		  // validity check: none for now...
		  // does recent motion qualify as a swipe?
		  //if( isValidSwipe() ) isSwipe=true;
		//}
	}
	
	
	
	public boolean isValidXY( float x, float y ) {
		// validity check: is pointer within allowable distance of key boundary?
		if( _key != null ) {
	      int touchX = (int) x - MorningStar.mPaddingLeft;
	      int touchY = (int) y + MorningStar.mVerticalCorrection - MorningStar.mPaddingTop;
	      // test if still inside key boudary as a placeholder; need something more sophisticated 
	      return _key.isInside(touchX,touchY);
		}
	    return false;
	}
	
//	//This tests to see if the user has made a swipe which is in between keys     
//	public boolean isDriftingSwipe(){
//		if((isSwipe || isTap) && _key == null)
//			return true;
//		return false;
//	} //isDriftingSwipe
	
//	public boolean isValidSwipe() {
//		// validity check: is pointer within allowable distance of key boundary?
//		// placeholder rules need to be replaced with more sophisticated logic
//		updateKeyCodeAngleListIndex();
//		if( key!=null && isValid && !isCancel &&
//				keyCodeList!=null && keyCodeAngleListIndex >= 0 &&
//				distance_squared() > DISTANCE_SWIPE*DISTANCE_SWIPE && 
//				velocity() >= VELOCITY_SWIPE ) {
//			return true;
//		}
//	    return false;
//	}


	public double theta() {
			float dx = endX - startX;
			float dy = endY - startY;
			if( dx != 0 || dy != 0 ) {
				return 90.0 - (180.0/3.141592) * Math.atan2( (double) dy, (double) dx );
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
			if (isSwipe){
				for( int i=0; i<angleList.length; i++ ) {
					if (angleList[i]!=-1){//tap is given the specific angle value of -1	so ignore this angle value	
						double angle_diff = theta - (double)angleList[i];
						while( angle_diff<-180.0 ) angle_diff=angle_diff+360.0;
						while( angle_diff>+180.0 ) angle_diff=angle_diff-360.0;
						if( Math.abs(angle_diff) <= ANGLE_TOLERANCE) {
							keyIndex = i;
						}//if within tolorance
					}//if not tap angle
				}//for
			}// if(isSwipe
			if (keyIndex == -1 && isTap){  //if it did not match any swipe angles or was not a swipe, we check to see if it is a tap
				for( int i=0; i<angleList.length; i++ ) {
					if (angleList[i]==-1){	//tap is given the specific angle value of -1			
						keyIndex = i;						
					}
				}//for
			}//if (keyIndex == -1 && isTap)
		}
		return keyIndex;
	}

}
