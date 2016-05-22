package com.coogeetech.morningstar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;



import android.inputmethodservice.Keyboard.Row;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Bundle;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.view.GestureDetector;
import android.view.MotionEvent;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.Layout;
import android.util.FloatMath;
import android.util.Log;

import com.softcrafted.disambiguation.OutputBuffer;


public class MorningStar extends Activity
			implements OnKeyboardActionListener, OnKeyListener {
	
	private static final String TAG = "TAG_David";
	protected static float DENSITY; //defined after class is created
	protected boolean isRightHandMode = true;
	private boolean isLeftHandMode = false;
	protected boolean isShifted = false;
	protected boolean isShiftedLock = false;
	private int concurrentTaps = 0; //number of pointers coming down at approximately the same time
	private long concurrencyTime=0; //timestamp for concurrant pointers
	//private DrawKeyboard _drawKeyboard;
	
    HashMap<String, String[]> keyCodeMap = new HashMap<String, String[]>();   
    HashMap<String, int[]> angleMap = new HashMap<String, int[] >();
    	
    //protected final int angles_DownOnly[] = {0};
    //protected final int angles_UpDown[] = {180,0};
    protected final int angles_LeftRight[] = {-90,90};
    protected final int angles_Tap_DiagsDown[] = {-1,-45,45};
    protected final int angles_Tap_DiagsDown_LeftRight[] = {-1,-45,45,-90,90};
    protected final int angles_TapOnly[] = {-1};
    protected final int angles_Tap_LeftRight[] = {-1,-90, 90};
    protected final int angles_DiagsDown[]={-45,45};
    protected final int angles_DiagsDown_LeftRight[]={-45,45,-90,90};
    protected final int angles_Tap_Left[] = {-1,-90};
    protected final int angles_Tap_Right[] = {-1,90};
    protected final int angles_Tap_Up[] = {-1, 180};
    protected final int angles_Tap_LeftRight_Up[] = {-1,-90,90,180};


    protected List<Keyboard.Key> keyboard_keys;
    KeyboardView keyboardView;
    Keyboard keyboard;
    Keyboard.Key currentKey;
    Keyboard.Key spaceKey;
    int currentKeyIndex;
    
    private AFX audio;
    ImageView feedBack;
    // properties of KeyboardView
    protected static int mPaddingLeft = 0;
    protected static int mVerticalCorrection = 0;
    protected static int mPaddingTop = 0;
    

	static final float FEEDBACK_THRESHOLD = 0.1f;
	static final float FINGER_REALIGNMENT_POSITION = .95f;	
	static final float KEY_WIDTH_TO_HEIGHT_FACTOR = 1.0f;
	static final int KEY_WIDTH=105;
	final int MID_KEY_WIDTH = KEY_WIDTH;
	
	static final int LEFT_FINGER_INDEX =13;
	static final int RIGHT_FINGER_INDEX =16;
	static final int HALF_KEYBOARD_RIGHT_INDEX=6;
	static final int SLANT=30;
	static final int EXTRA_SLANT = SLANT;
	
	OutputBuffer myOutput;

    // initial and final data for touchpoints in gesture
    private List <SwipeTouchPoint> touchPoints = new ArrayList <SwipeTouchPoint>();  
    
    private SwipeTouchPoint getTouchPoint(Double code){
    	for(int i=0; i<touchPoints.size(); i++)
    	{
    		if (touchPoints.get(i).getUniqueCode()==code)
    			return touchPoints.get(i);
    	}
    	return null;
    }
    private Boolean isLeftIndex(){
    	for(int i=0; i<touchPoints.size(); i++)
    	{
    		if (touchPoints.get(i).getFinger() == SwipeTouchPoint.LEFT_INDEX)
    			return true;
    	}
    	return false;
    }
    private Boolean isRightIndex(){
    	for(int i=0; i<touchPoints.size(); i++)
    	{
    		if (touchPoints.get(i).getFinger() == SwipeTouchPoint.RIGHT_INDEX)
    			return true;
    	}
    	return false;
    }
    
    private SwipeTouchPoint getTouchPoint( MotionEvent ev ) {
       // Get SwipeTouchPoint object used to track pointer by pointer ID
   	   // default to pointer associated with DOWN or UP events if not specified
       int pointerIndex = ev.getActionIndex();  
       return getTouchPoint( ev, pointerIndex );
    }
    
    private SwipeTouchPoint getTouchPoint( MotionEvent ev, int pointerIndex ) {   	
        // Get SwipeTouchPoint object used to track pointer by pointer ID
    	// caller-specified pointer index could be used to iterate over multiple pointers for MOVE events
     	SwipeTouchPoint touchPoint = null;
    	int pointerId = ev.getPointerId(pointerIndex);
		int action = 0;
     	// Look for touchPoint already in list belonging to pointer and return it if found
    	for (int i = 0; i < touchPoints.size(); i++) {
    	  touchPoint = touchPoints.get(i);
  		  if( touchPoint.pid == pointerId ) {
  			// SwipeTouchPoint object for pointer already exists
  		   	action = ev.getAction() & MotionEvent.ACTION_MASK;
           	if( action==MotionEvent.ACTION_DOWN ||
           		action==MotionEvent.ACTION_POINTER_DOWN ) {
           		// already used; re-use on new key and new swipe event
           		touchPoint = new SwipeTouchPoint(pointerId,getKeyboardKeyAtXY( ev.getX(pointerIndex), ev.getY(pointerIndex) ));
           		touchPoints.set(i, touchPoint);
           		if( touchPoint.getKey() != null ) {
           		  touchPoint.keyCodeList = keyCodeMap.get(String.valueOf(touchPoint.getKey().codes[0]));
           		  touchPoint.angleList = angleMap.get(String.valueOf(touchPoint.getKey().codes[0]));
          	    }
           	}
            //these three lines will immediately begin following any pointer that comes down on the F or J key
           	//if (!isRightHandMode && touchPoint.getKey()==keyboard_keys.get(LEFT_FINGER_INDEX))touchPoint.setFinger(SwipeTouchPoint.LEFT_INDEX);//TODO && !isLeftHandMode
           	//if (!isRightHandMode && touchPoint.getKey()==keyboard_keys.get(MorningStar.RIGHT_FINGER_INDEX))touchPoint.setFinger(SwipeTouchPoint.RIGHT_INDEX);
           	//if (isRightHandMode && touchPoint.getKey() == keyboard_keys.get(MorningStar.HALF_KEYBOARD_RIGHT_INDEX))touchPoint.setFinger(SwipeTouchPoint.RIGHT_INDEX);
  		    return touchPoint;
  		  }
  	    }
        // Existing touchPoint not found; create and add new touchPoint to list and return it
        Keyboard.Key key = getKeyboardKeyAtXY( ev.getX(pointerIndex), ev.getY(pointerIndex) );
  	    touchPoint = new SwipeTouchPoint(pointerId,key);
  	    if( key != null ) {
   		  touchPoint.keyCodeList = keyCodeMap.get(String.valueOf(touchPoint.getKey().codes[0]));
   		  touchPoint.angleList = angleMap.get(String.valueOf(touchPoint.getKey().codes[0]));
  	    }
  	    touchPoints.add(touchPoint);
  	    //these three lines will immediately begin following any pointer that comes down on the F or J key
  	    //if (!isRightHandMode && touchPoint.getKey()==keyboard_keys.get(LEFT_FINGER_INDEX))touchPoint.setFinger(SwipeTouchPoint.LEFT_INDEX);
  	    //if (!isRightHandMode && touchPoint.getKey()==keyboard_keys.get(MorningStar.RIGHT_FINGER_INDEX))touchPoint.setFinger(SwipeTouchPoint.RIGHT_INDEX);
       	//if (isRightHandMode && touchPoint.getKey() == keyboard_keys.get(MorningStar.HALF_KEYBOARD_RIGHT_INDEX))touchPoint.setFinger(SwipeTouchPoint.RIGHT_INDEX);
     	return touchPoint;
      }      
    
    /*
     * If difference in starting time between current pointer (which just came up) and starting time
     * of any other pointer is less than TIME_FOR_concurrenCY all pointers are canceled
     */
    private void ensureNonConcurrency (SwipeTouchPoint activeTouchPoint) {
    	
    	if((activeTouchPoint.getStartTime()-concurrencyTime)*(activeTouchPoint.getStartTime()-concurrencyTime) < activeTouchPoint.TIME_FOR_CONCURRENCY*activeTouchPoint.TIME_FOR_CONCURRENCY){
    		concurrentTaps++;
    		//updateMessage("add conc :: ");
    		if (concurrentTaps == 4){
    			playSound(AFX.KEYSTRIKE2);
    			onKeySwipe(" ");
    			concurrencyTime = 0;
    		}
    	}
    	
    	SwipeTouchPoint otherTouchPoint;
    	float timeDiff;
    	for (int i = 0; i < touchPoints.size(); i++) {
        	otherTouchPoint = touchPoints.get(i);
        	timeDiff = activeTouchPoint.getStartTime()-otherTouchPoint.getStartTime();
      		if( activeTouchPoint.pid != otherTouchPoint.pid && (timeDiff*timeDiff < activeTouchPoint.TIME_FOR_CONCURRENCY*activeTouchPoint.TIME_FOR_CONCURRENCY)) {
      			
      			if(activeTouchPoint.getIsTap()){
  					concurrencyTime = activeTouchPoint.getStartTime();
  					concurrentTaps = 2;
  				}
      			for(int j=0; j< touchPoints.size(); j++){	
    				touchPoints.get(j).cancelSwipe();
    			}//
       		  }
      	    }//for
    }//ensureNonConcurrency
      
      
    private Keyboard.Key getKeyboardKeyAtXY( float x, float y ) {
      // get Keyboard.Key object at X,Y or return null if none found
      
      // shift from event to Keyboard Key frame of reference for x,y positions 
      int touchX = (int) x - mPaddingLeft;
      int touchY = (int) y + mVerticalCorrection - mPaddingTop;
          	
      // check keys one at a time to find key with pointer inside
      for (int i = 0; i < keyboard_keys.size(); i++) {
    	if( keyboard_keys.get(i).isInside(touchX,touchY) ) {
    		currentKey = keyboard_keys.get(i);
    		currentKeyIndex = i; 		
    	  return keyboard_keys.get(i);
    	}
  	  }

      if (!isRightHandMode && !isLeftHandMode && x-keyboardView.getPaddingLeft()<keyboard_keys.get(27).x+KEY_WIDTH && x-keyboardView.getPaddingLeft() > keyboard_keys.get(22).x){
    	  return spaceKey;
      }else if (isRightHandMode){
    	  return spaceKey;
      }
      
   	  return null;
   	}//getKeyboardKey
    
    public Boolean keyIsPressed(){return currentKey.pressed;}
    
    @Override   //this creates the menu button in top right corner of display
    public boolean onCreateOptionsMenu(Menu passMenu){
    	super.onCreateOptionsMenu(passMenu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.settings_main, passMenu);
    	
    	return true;
    }//onCreateMenu
    
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) { 	
        super.onCreate(savedInstanceState);
        
        
        /*This can be uncommented to hide the top title bar*/
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
    
        setContentView(R.layout.main);
        feedBack = (ImageView) findViewById(R.id.feedbackImageView);
        /*creates the audio and haptic engine*/
        audio = new AFX(this);
        audio.play(this,AFX.STARTUP);
        
        /*sets the pixel density factor of the current screen*/
        DENSITY = getResources().getDisplayMetrics().density;
     
//      Draws and initiates keyboard ================================================
        keyboardView = (KeyboardView) findViewById(R.id.keyboardView);
        keyboardView.setEnabled(true);
        keyboardView.setPreviewEnabled(false);  // popup visibility
        keyboardView.setOnKeyListener(this);
        keyboardView.setOnKeyboardActionListener(this);
        keyboardView.setVisibility(View.VISIBLE);
        isRightHandMode = false;
        
        final EditText editText = (EditText) findViewById(R.id.editText);
        myOutput = new OutputBuffer(this, editText);
        
		MorningStar.mPaddingLeft = keyboardView.getPaddingLeft();
		MorningStar.mVerticalCorrection = 0;
		MorningStar.mPaddingTop = keyboardView.getPaddingTop();
		
		//this is a messy fix that waits for after keyboardview is initiated to assign the keyboard. A more elegant solution may be possible
		keyboardView.post(new Runnable(){ //TODO THIS IS MESSY AND FLASHES A TRASH SCREEN
			@Override
			public void run() {
				changeKeyboard(R.xml.qwerty);
				keyboardView.post(new Runnable(){
					@Override
					public void run() {
						changeKeyboard(R.xml.qwerty);		
						
						float lX = (float) (keyboardView.getWidth()/2-1.5*KEY_WIDTH-keyboardView.getPaddingLeft());
			    		float rX = (float) (keyboardView.getWidth()/2+1.5*KEY_WIDTH-keyboardView.getPaddingLeft()); 
			    		float lY = (float) (keyboardView.getHeight()-keyboardView.getPaddingTop()-1.5*KEY_WIDTH);
			    		float rY = (float) (keyboardView.getHeight()-keyboardView.getPaddingTop()-1.5*KEY_WIDTH); 
			    		redrawKeysFull(KEY_WIDTH, KEY_WIDTH,lX,rX,lY,rY,SLANT);	
			    		
					}					
				});			
			}			
		});
		
		
     touchPoints.clear();
     
     keyboardView.setOnTouchListener( 
	   new View.OnTouchListener() { 	 	 		 
		 // A gesture starts with a motion event with ACTION_DOWN 
		 // that provides the location of the first pointer down. 
		 // As each additional pointer that goes down or up, the framework
		 // will generate a motion event with ACTION_POINTER_DOWN or 
		 // ACTION_POINTER_UP accordingly. Pointer movements are described 
		 // by motion events with ACTION_MOVE. Finally, a gesture end either 
		 // when the final pointer goes up as represented by a motion event 
		 // with ACTION_UP or when gesture is canceled with ACTION_CANCEL. 
		 
		 // public final int getActionIndex () 
		 // For ACTION_POINTER_DOWN or ACTION_POINTER_UP as returned by getActionMasked(),
		 // this returns the associated pointer index. The index may be used with 
		 // getPointerId(int), getX(int), getY(int), getPressure(int), and getSize(int) 
		 // to get information about the pointer that has gone down or up.

         public boolean onTouch(View v, MotionEvent ev) {
        	 

        	SwipeTouchPoint touchPoint;        // SwipeTouchPoint used to track pointer responsible for event
        	switch (ev.getAction() & MotionEvent.ACTION_MASK) {
           	case MotionEvent.ACTION_DOWN:

                touchPoints.clear();

           	case MotionEvent.ACTION_POINTER_DOWN:

           		feedBack.setVisibility(View.INVISIBLE);
           		touchPoint = getTouchPoint(ev);
           		touchPoint.process_action_down(ev); //saves initial touchpoint information
           		backspaceWatcher(touchPoint); //listens for quick backspace gesture
           		checkFingerRealign(ev); //listens for four finger realignment gesture
        		break;
        	case MotionEvent.ACTION_MOVE:
        		for (int i=0; i < ev.getPointerCount();i++){ //cycle through all active touchpoints
        			touchPoint = getTouchPoint(ev,i);
        			touchPoint.process_action_move(ev);
        			followIndex(ev,touchPoint,i); //listens for index following
        			}

//UNCOMMENT TO VIEW TOUCHPOINT INFO ON SCREEN == DEBUG ONLY     		
//       		    for (int i = 0; i<10 ; i++) {
//       		    	if( i < touchPoints.size() ) {
//       		    		touchPoint = touchPoints.get(i);
//       		    			onShowSwipeStatus( touchPoint, i );
//       		    	} else {
//       		    		onShowSwipeStatus( null, i );
//       		    	}
//    		    } 
        		
        	    break;
        	case MotionEvent.ACTION_POINTER_UP:
        		// A pointer comes up
        	case MotionEvent.ACTION_UP:
           		// final pointer up (end of gesture)
           		touchPoint = getTouchPoint(ev);           		
           		touchPoint.process_action_up(ev);
           		
           		ensureNonConcurrency(touchPoint);
	           	if( (touchPoint.getIsSwipe() || touchPoint.getIsTap()) && touchPoint.getKey()!=null && touchPoint.keyCodeList!=null && touchPoint.keyCodeAngleListIndex>=0 ) {	           		
		    		if (touchPoint.getIsSwipe()) playSound( AFX.SWIPE);
		    		else if (touchPoint.getIsTap()) playSound(AFX.KEYSTRIKE);
	           		onKeySwipe( touchPoint.keyCodeList[touchPoint.keyCodeAngleListIndex] );
		    		if(touchPoint.keyCodeList[touchPoint.keyCodeAngleListIndex] != " " && touchPoint.keyCodeList[touchPoint.keyCodeAngleListIndex] != "\b"){
		    			realignIndividualKey(touchPoint.getStartX(),touchPoint.getStartY(),touchPoint.getKey());
		    		}
	           	}	
           		//userFeedback(touchPoint);
           		touchPoints.remove(touchPoint);
        		break;
        	case MotionEvent.ACTION_CANCEL:
        		// gesture cancelled
        	    touchPoints.clear();
        	    break;
        	default:
//        		updateMessage("UNDEFINED STATE");
        	}//switch

      	    
             return false;   
           }//onTouch
        }//new view.OnTouchListener ()
	  );  //setOnTouchListener
     
     initiateHashMap();         
     }
    
    
    //This method keeps an eye on touchpoints to see if they give the backspace gesture
    //It is still buggy in that multiple fingers can give backspace gestures at the same time,
    //and a whole hand moving around the keyboard can also give a backspace gesture
    private void backspaceWatcher (final SwipeTouchPoint passTouchPoint){
    	
    	new Thread (new Runnable() {
	      public void run() { 
	    	  //this code will be run on an independant thread so that GUI will still respond to user
	          try {
					Thread.sleep(600);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}//try catch
	          
	          if (passTouchPoint == null)return;
	          if (passTouchPoint.getKeyCodeAngleListIndex() == -1)return;
	          while(passTouchPoint.getIsSwipe() && passTouchPoint.keyCodeList[passTouchPoint.getKeyCodeAngleListIndex()]=="\b" && !passTouchPoint.getIsFinished()){
	        	  try {
					Thread.sleep(100);
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	  if(isRightIndex() && isLeftIndex())break;
	        	  runOnUiThread(new Runnable() {
	        		 public void run(){
	        			 onKeySwipe("\b");
	        			 
	        		 }
	        	  });
	        	  if (passTouchPoint.getKeyCodeAngleListIndex() == -1)break;
	          }//while
	          

	      }//run
    	}).start();


    }//backspace watcher

    /*This method is used send a message to the user at the cursor. It is used when keyboards drift outside of key boundaries
     * or too close to each other.
     * */ //TODO buggy! doesn't follow text cursor after user has performed a backspace. Sometimes doesn't wrap text
    public void feedBackMessage(String message){
    	
    	final EditText editText = (EditText) findViewById(R.id.editText);
    	int pos = editText.getSelectionStart();
    	Layout layout = editText.getLayout();
    	int line = layout.getLineForOffset(pos);
    	int baseline = layout.getLineBaseline(line);
    	int ascent = layout.getLineAscent(line);
    	float x = layout.getPrimaryHorizontal(pos);
    	float y = baseline + ascent + 35*DENSITY;
    	
    	Paint alertBackground = new Paint();
    	alertBackground.setColor(Color.BLACK);
    	Paint alertText = new Paint();
    	alertText.setColor(Color.WHITE);
    	alertText.setTextSize(25);
    	alertText.getTextSize();
    	
    	
    	
    	Bitmap bitmap;
    	bitmap = Bitmap.createBitmap((int) (alertText.getTextSize()*message.length()/2), (int) alertText.getTextSize(), Bitmap.Config.ARGB_8888);

    	
    	Canvas alertCanvas = new Canvas(bitmap);
    	alertCanvas.drawPaint(alertBackground);
    	alertCanvas.drawText(message, 0, baseline, alertText);
    	
    	
    	feedBack.setImageBitmap(bitmap);
    	feedBack.setX(x);
    	feedBack.setY(y);
    	feedBack.setVisibility(View.VISIBLE);

    	 //This makes the feedback view dissapear after half a second
    	new Thread (new Runnable() {
	      public void run() {
	          try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	          runOnUiThread(new Runnable() {
	              public void run() {
	              	//if
	                  feedBack.setVisibility(View.INVISIBLE);
	              }//run
	          });//runOnUiThread
	      }//run
	  }).start();    	 	        	            	
    	
    }//feedbackMessage
  
    //Display touchpoint feedback to developer. Used for debugging purposes
    public void onShowSwipeStatus( SwipeTouchPoint touchpoint, int status_index ) {

		TextView textview = null;
		
    	switch (status_index) {
       	case 0:  textview = (TextView) findViewById(R.id.swipe1Text);  break;
       	case 1:  textview = (TextView) findViewById(R.id.swipe2Text);  break;
       	case 2:  textview = (TextView) findViewById(R.id.swipe3Text);  break;
       	case 3:  textview = (TextView) findViewById(R.id.swipe4Text);  break;
       	case 4:  textview = (TextView) findViewById(R.id.swipe5Text);  break;
       	case 5:  textview = (TextView) findViewById(R.id.swipe6Text);  break;
       	case 6:  textview = (TextView) findViewById(R.id.swipe7Text);  break;
       	case 7:  textview = (TextView) findViewById(R.id.swipe8Text);  break;
       	case 8:  textview = (TextView) findViewById(R.id.swipe9Text);  break;
       	case 9:  textview = (TextView) findViewById(R.id.swipe10Text);  break;
       	default: return;
      	}
    	
    	textview.setText( String.format("#%d: ",status_index+1) );
    	
    	if( touchpoint == null ) {
    		textview.append("Not Used");
    		return;
    	}

    	//updateMessage(Integer.toString(touchpoint.historySize));
    	
//        textview.append( String.format("history=%d ",           touchpoint.totHistory)     );
//        textview.append( String.format("x=%f ",           touchpoint.startX)     );
        textview.append( String.format("pid=%d ",               touchpoint.pid)           );
        textview.append( String.format("dist=%04.2f ",          touchpoint.getDistance()) );
        textview.append( String.format("start=%3d ",          touchpoint.getStartTime()));
        textview.append( String.format("time=%4.2f ",           touchpoint.getTimeDiff())        );  
//        textview.append( String.format("vel=%3.1f ",            touchpoint.velocity())    );
        textview.append( String.format("ang=%3.1f ",            touchpoint.theta())       );
        textview.append( String.format("isSwipe=%b ",          touchpoint.getIsSwipe())      );
        textview.append( String.format("isTap=%b ",           touchpoint.getIsTap())       );
        textview.append( String.format("hasPotent=%b ",           touchpoint.getHasPotential())       );
        textview.append( String.format("isKey=%b ",           touchpoint.getKey() !=null)       );     
        textview.append( String.format("haveCodeList=%b ",           touchpoint.keyCodeList!=null )       );
        textview.append( String.format("angleIndex=%3d ",     touchpoint.keyCodeAngleListIndex)    );
//        textview.append( String.format("fastDelY=%3.1f \n",     touchpoint.fastDeltaY)    );
//        textview.append( String.format("fastDelTime=%d ",     touchpoint.fastDeltaTime) );
        textview.append( String.format("touchX=%3.1f ",          touchpoint.getStartX())         );
        textview.append( String.format("touchY=%3.1f ",          touchpoint.getStartY())         );
        textview.append( String.format("touchSize=%3.6f ",          touchpoint.getStartSize())         );
//        textview.append( String.format("lastTime=%d ",          touchpoint.lastTime)      );  //is correct
//        textview.append( String.format("dX=%3.1f ",             touchpoint.endX - touchpoint.startX) );
//        textview.append( String.format("dY=%3.1f ",             touchpoint.endY - touchpoint.startY) );  
//        textview.append( String.format("Pres=%f ",          touchpoint.currentPressure)      );  //TODO incorrect
//        textview.append( String.format("PresMax=%f ",           touchpoint.pressureMax)   );
//        textview.append( String.format("PresAvg=%f ",           touchpoint.pressureAvg)   );
        
        String c = null;
        if( touchpoint.keyCodeList!=null && touchpoint.keyCodeAngleListIndex>=0 ) {
          c = touchpoint.keyCodeList[touchpoint.keyCodeAngleListIndex];
        }
        if(!(c == null)) {
          textview.append("["+c+"]  ");
        } else {
          textview.append("[none]  ");
        }
  	}
    
    //This method processes all functions and letters that the keyboard generates. Every time a key is activated,
    //the associated string is sent to this method
	private void onKeySwipe(String keyString) { 
	  final EditText edittext = (EditText) findViewById(R.id.editText);
	  edittext.setCursorVisible(true);
      if( keyString != null ) {
    	if( keyString == "123" ) {
    		if(isRightHandMode){
      			changeKeyboard(R.xml.symbols_123_right);            
      		}else{
      			changeKeyboard(R.xml.symbols_123);
      		}
      	}// "123" key input
    	else if( keyString == "ABC" ) {
    		if(isRightHandMode){
      			changeKeyboard(R.xml.qwerty_right);
      		}else{
      			changeKeyboard(R.xml.qwerty);
      		}// "ABC" key input
      	}//ABC key input    	
    	else if(keyString == "Other"){
      		if(isRightHandMode){
      			isRightHandMode = false;
      			changeKeyboard(R.xml.qwerty);
        		
      		}else{
      			isRightHandMode = true;
      			changeKeyboard(R.xml.qwerty_right);        		
      		}
      	} // "other" key input
    	else if(keyString == "Special"){
    		if(isRightHandMode){
      			changeKeyboard(R.xml.special_right);            
      		}else{
      			changeKeyboard(R.xml.special);             
      		}
      	} // "special" key input 
    	else if( keyString == "Shift"){
    		if(isRightHandMode || isLeftHandMode){
    			if(!isShifted && !isShiftedLock){
    				isShifted = true;
    				changeKeyboard(R.xml.qwerty_right_shifted);
    			}else if(isShifted && !isShiftedLock){
    				isShiftedLock = true;
    			}else if(isShiftedLock){
    				isShifted = false;
    				isShiftedLock = false;
    				changeKeyboard(R.xml.qwerty_right);
    			}
    		}else{//is full keyboard mode
    			if(!isShifted && !isShiftedLock){
    				isShifted = true;
    				changeKeyboard(R.xml.qwerty_shifted);
    			}else if(isShifted && !isShiftedLock){
    				isShiftedLock = true;
    			}else if(isShiftedLock){
    				isShifted = false;
    				isShiftedLock = false;
    				changeKeyboard(R.xml.qwerty);
    			}
    		}
    	}// "Shift"
    	else if( keyString == "\b" ) {
        	  // backspace removes character from end of string
        	  String text = edittext.getText().toString();
        	  if( text.length()>0 ) {
                edittext.setText(text.substring(0, text.length() - 1));
        	  }
        	}//backspace key input 
    	else {
          myOutput.pushText(keyString); //Plug to disambiguation package
          if(isShifted && !isShiftedLock){ //resets the shift state if the capslock is not turned on
        	  isShifted = false;
        	  if (isRightHandMode) changeKeyboard(R.xml.qwerty_right);
        	  else changeKeyboard(R.xml.qwerty);
          }
          //updateMessage(Integer.toString(edittext.getSelectionEnd()));
      	}
      }//if( keyString != null )

	}// onKeySwipe
	
	/*This method can be used to have text displayed towards the top of the screen
	 * used for debugging purposes*/
	public void updateMessage(String passMessage){
		TextView statusText = (TextView) findViewById(R.id.statusText);
    	if(statusText !=null) statusText.append(passMessage );
	}//updateMessage
	
	private void changeKeyboard(int passResource){
		
		//the old left and right index finger position
		float lX=0;
		float lY=0;
		float rX=0;
		float rY=0;
		//We want to draw the new keyboard at the same place as the old one
		
		
		//Find the previous position of the keyboard
		if(keyboardView.getKeyboard() != null){
			if(isRightHandMode){
				if(keyboardView.getKeyboard().getKeys().size() > 20){
					rX = getKeyCenterX(RIGHT_FINGER_INDEX);
					rY = getKeyCenterY(RIGHT_FINGER_INDEX);
				}else{
					rX = getKeyCenterX(HALF_KEYBOARD_RIGHT_INDEX);
					rY = getKeyCenterY(HALF_KEYBOARD_RIGHT_INDEX);
				}
			}else{
				if(keyboardView.getKeyboard().getKeys().size() > 20){
					lX = getKeyCenterX(LEFT_FINGER_INDEX);
					lY = getKeyCenterY(LEFT_FINGER_INDEX);
					rX = getKeyCenterX(RIGHT_FINGER_INDEX);
					rY = getKeyCenterY(RIGHT_FINGER_INDEX);
				}else{
					rX = getKeyCenterX(HALF_KEYBOARD_RIGHT_INDEX);
					rY = getKeyCenterY(HALF_KEYBOARD_RIGHT_INDEX);
					lX = rX - 3*KEY_WIDTH;
					lY = rY;							
				}
			}
		}else{//if there is no keyboard yet, we define a starting position for the keyboard
			lX = (float) (keyboardView.getWidth()/2-1.5*KEY_WIDTH-keyboardView.getPaddingLeft());
    		rX = (float) (keyboardView.getWidth()/2+1.5*KEY_WIDTH-keyboardView.getPaddingLeft()); 
    		lY = keyboardView.getHeight()/2-keyboardView.getPaddingTop();
    		rY = keyboardView.getHeight()/2-keyboardView.getPaddingTop(); 
		}
		
		//initiate the new keyboard and set padding/keys
  	    Keyboard _keyboard = new Keyboard(this, passResource);
  	    keyboardView.setKeyboard(_keyboard);
  	    this.keyboard_keys = _keyboard.getKeys(); 	    
  	    int _screenWidth = getWindowManager().getDefaultDisplay().getWidth();
	    int _screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		int _sidePadding;
		int _topPadding;
	    if (isLeftHandMode || isRightHandMode){
			_sidePadding = (_screenWidth - 5*KEY_WIDTH)/2;			
	    }
		else {
			_sidePadding = (_screenWidth - 8*KEY_WIDTH + 2*MID_KEY_WIDTH)/2;			
		}
	    _topPadding = (int) ((_screenHeight - 3*KEY_WIDTH/KEY_WIDTH_TO_HEIGHT_FACTOR));
		keyboardView.setPadding(_sidePadding, _topPadding, _sidePadding, 0);
		MorningStar.mPaddingLeft = keyboardView.getPaddingLeft();
	    MorningStar.mVerticalCorrection = 0;
	    MorningStar.mPaddingTop = keyboardView.getPaddingTop();
  	    
	    //initiates the space key for everywhere outside of the keyboard area
  	    spaceKey = new Key(new Row(_keyboard));
		spaceKey.codes = new int[] {0};
  	      	    	    
	    redrawKeysHalf(KEY_WIDTH,rX,rY);
	    redrawKeysFull(KEY_WIDTH, KEY_WIDTH,lX,rX,lY,rY,SLANT);	     

	}//changeKeyboard
	
	//plays error sound
	public void playError(){audio.play(this,AFX.ERROR);}//playError
	//plays a defined sound
	public void playSound(int passSound){audio.play(this, passSound);}
	
	//If we have determined that this certain finger is an index finger, we align the whole keyboard to it
	private void followIndex(MotionEvent ev, SwipeTouchPoint touchPoint,int i){
		if(isRightHandMode){
			if(touchPoint.getFinger()==SwipeTouchPoint.RIGHT_INDEX) redrawKeysHalf(KEY_WIDTH,ev.getX(i)-keyboardView.getPaddingLeft(),ev.getY(i)-keyboardView.getPaddingTop());
		}else{
			if(touchPoint.getFinger()==SwipeTouchPoint.LEFT_INDEX) redrawKeysFull(KEY_WIDTH,KEY_WIDTH,
					ev.getX(i)-keyboardView.getPaddingLeft(),
					getKeyCenterX(MorningStar.RIGHT_FINGER_INDEX),
					ev.getY(i)-keyboardView.getPaddingTop(),
					getKeyCenterY(MorningStar.RIGHT_FINGER_INDEX),
					SLANT);
			if(touchPoint.getFinger()==SwipeTouchPoint.RIGHT_INDEX) redrawKeysFull(KEY_WIDTH,KEY_WIDTH,
					getKeyCenterX(MorningStar.LEFT_FINGER_INDEX),
					ev.getX(i)-keyboardView.getPaddingLeft(),
					getKeyCenterY(MorningStar.LEFT_FINGER_INDEX),
					ev.getY(i)-keyboardView.getPaddingTop(),
					SLANT);
		}//if else							
	}
	
	//This is a listener for the four finger realign gesture. It is also used to define a certain finger
	// as an index finger
	private void checkFingerRealign (MotionEvent ev){
		if(touchPoints.size()<4) return;
		
		if(isRightHandMode || isLeftHandMode) {
			realignHalfKeyboard(ev);//checkForRealign(ev);
			return;
		}
		
		SwipeTouchPoint _touchPoint;
		SwipeTouchPoint _LeftmostTouchPoint;
		
		//assume left hand is on left half of screen
		//find groups of four fingers on left
		//draw left half
		//find groups of four fingers on right
		//draw right half
		List <SwipeTouchPoint> leftList = new ArrayList <SwipeTouchPoint>(); //list touchPoints that are on the left side of screen sorted left to right
		List <SwipeTouchPoint> rightList = new ArrayList <SwipeTouchPoint>(); //list of touchPoints that are on the right side of the screen sorted left to right
		int viewWidth = keyboardView.getWidth();
		float leftMostTouch;
		for(int j=0;j<touchPoints.size();j++){
			leftMostTouch = 10000f;
			_LeftmostTouchPoint = null;
			for(int i=0; i<touchPoints.size(); i++){
				_touchPoint = touchPoints.get(i);
				if (_touchPoint.getStartX()<leftMostTouch && !_touchPoint.getIsAligned() && true/*is above the bottom row*/ && true/*is within range*/){ //TODO FINISH THIS ALGORITHM
					if (false/*touchpoint is out of reasonable range*/)
						return;
					leftMostTouch = _touchPoint.getStartX();
					_LeftmostTouchPoint = _touchPoint;
				}//if
			}//for
			if(_LeftmostTouchPoint !=null){
				if (_LeftmostTouchPoint.getStartX()<viewWidth/2)
					{leftList.add(_LeftmostTouchPoint);}
				else {rightList.add(_LeftmostTouchPoint);}
				_LeftmostTouchPoint.setIsAligned(true); 
			}//if(_LeftmostTouchPoint !=null){
		}//for
		
		
		//the positions and widths to redraw the keyboard at
		float _xLeft=0;
		float _xRight=0;
		float _yLeft = 0;
		float _yRight = 0;
		int _keyWidthLeft = KEY_WIDTH;
		int _keyWidthRight = KEY_WIDTH;

		Boolean doLeftAlign = false;
		Boolean doRightAlign = false;
		
		if (leftList.size()==4){//if there are four fingers down on either half of the screen, we realign to that index finger and recognize it.
			
			doLeftAlign = true;

			_xLeft=leftList.get(3).getStartX();
			_yLeft=leftList.get(3).getStartY();
			leftList.get(3).setFinger(SwipeTouchPoint.LEFT_INDEX);

			_xLeft -=keyboardView.getPaddingLeft();			
			_yLeft -=keyboardView.getPaddingTop();
		}//if (leftList.size()=4)
		else{
			for(int i=0; i<leftList.size();i++)
				leftList.get(i).setIsAligned(false);
		}
		
		if (rightList.size()==4){
			doRightAlign = true;
			_xRight=rightList.get(0).getStartX();
			_yRight=rightList.get(0).getStartY();
			rightList.get(0).setFinger(SwipeTouchPoint.RIGHT_INDEX);
			
			_xRight -=keyboardView.getPaddingLeft();	
			_yRight -=keyboardView.getPaddingTop();
		}//if (rightList.size()=4)
		else{
			for(int i=0; i<rightList.size();i++)
				rightList.get(i).setIsAligned(false);
		}
		
		int slant = 30; //TODO messy
		if(!doLeftAlign && !doRightAlign) return;
		else if (doLeftAlign && doRightAlign){
			redrawKeysFull(_keyWidthLeft,_keyWidthRight,_xLeft,_xRight,_yLeft,_yRight,slant);
		}else if (doLeftAlign && !doRightAlign){
			_xRight = getKeyCenterX(RIGHT_FINGER_INDEX);
			_yRight = getKeyCenterY(RIGHT_FINGER_INDEX);//13 is the key index for 'F'
			redrawKeysFull(_keyWidthLeft,_keyWidthRight,_xLeft,_xRight,_yLeft,_yRight,slant);
		}else if (!doLeftAlign && doRightAlign){
			_xLeft = getKeyCenterX(LEFT_FINGER_INDEX);
			_yLeft = getKeyCenterY(LEFT_FINGER_INDEX);//13 is the key index for 'F'
			redrawKeysFull(_keyWidthLeft,_keyWidthRight,_xLeft,_xRight,_yLeft,_yRight,slant);
		}
		
		
	}//checkHorizRealign
	
	private void realignHalfKeyboard(MotionEvent ev){
		
		SwipeTouchPoint _touchPoint;
		SwipeTouchPoint _LeftmostTouchPoint;
		
		List <SwipeTouchPoint> alignList = new ArrayList <SwipeTouchPoint>();
		//resize and shift keys in unison for half keyboard 
		float leftMostTouch;
		for(int j=0;j<touchPoints.size();j++){
			leftMostTouch = 10000f;
			_LeftmostTouchPoint = null;
			for(int i=0; i<touchPoints.size(); i++){
				_touchPoint = touchPoints.get(i);
				if (_touchPoint.getStartX()<leftMostTouch && !_touchPoint.getIsAligned() && true/*is above the bottom row*/ && true/*is within range*/){ //TODO FINISH THIS ALGORITHM
					if (false/*touchpoint is out of reasonable range*/)
						return;
					leftMostTouch = _touchPoint.getStartX();
					_LeftmostTouchPoint = _touchPoint;
				}//if
			}//for
			if(_LeftmostTouchPoint !=null){
				alignList.add(_LeftmostTouchPoint);
				_LeftmostTouchPoint.setIsAligned(true); //TODO don't set this until it is actually aligned!!
			}//if(_LeftmostTouchPoint !=null){
		}//for
		//updateMessage(Integer.toString(alignList.size()));
		if (alignList.size()!=4)
			return;
		
		SwipeTouchPoint indexFinger = alignList.get(0);
		indexFinger.setFinger(SwipeTouchPoint.RIGHT_INDEX);
		int _keyWidth = KEY_WIDTH;
		float _xPos=indexFinger.getStartX() - keyboardView.getPaddingLeft();
		float _yPos=indexFinger.getStartY() - keyboardView.getPaddingTop();
		
		redrawKeysHalf (_keyWidth, _xPos, _yPos);
		
	}
		
	/*This method takes in width and index finger positions (using coordinate system 3 defined below) and draws the full keyboard*/
	private void redrawKeysFull(int passLeftWidth, int passRightWidth, float passXLeft, float passXRight, float passYLeft, float passYRight, int slant){
		
		/*DRAWING AND REPOSITIONING NOTES
		 * 
		 * There are five separate coordinate systems to keep in mind when drawing and repositioning
		 * 
		 * 1.the real android screen cordinates
		 * 2.the entire keyboardView object (includes padding)
		 * 3.the keyboard coordinates (a subset of the keyboardView that is inside of padding)
		 * 4.the Left hand keyboard coordinates
		 * 5.the Right hand keyboard coordinates
		 * 
		 * user input as well as object position values occur in #3. Therefore, if you want something to be on the left side of the screen
		 * it must have negative values in this coordinate system.
		 * 
		 * Drawing occurs in #2. The coordinates (0,0) will always be in the top left of the view(not necessarily the visible view), regardless of padding size;
		 * 
		 * #1 is what we really care about, so I try and line it up with #2 if at all possible. They share left, bottom, and right side
		 * edges.
		 * 
		 * #4 and #5 are simplifying coordinates that let us move half keyboards as a single object, while keeping aspect ratios.
		 * 
		 * NOTE: keyboardView getWidth and getHeight functions are in system #2
		 * */ 
		
		if (keyboardView.getPaddingLeft() == 0) return;
		if (isRightHandMode || isLeftHandMode)return;
		
		float indexXLeft = passXLeft;
		float indexXRight = passXRight;
		float indexYLeft = passYLeft;
		float indexYRight = passYRight;
		
		
		passXLeft -= 3.5*passLeftWidth + slant;
		passXRight -= 1.5*passRightWidth + slant;
		
		passYLeft -= 1.5f*(float)passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR;
		passYRight -= 1.5f*(float)passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR;
		
		//float viewWidth = keyboardView.getWidth();
		
		//THE FOLLOWING VALUES ARE ALL IN COORDINATE SYSTEM #3 AS DEFINED ABOVE TO SIMPLIFY SETTING OBJECT POSITIONS
		
		float leftMaxX= (float) (indexXLeft + 0.5*passLeftWidth + MID_KEY_WIDTH + EXTRA_SLANT);
		float leftMinX= (float) (indexXLeft - 3.5*passLeftWidth - slant);
		float leftMaxY= (float) (indexYLeft +1.5*passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		float leftMinY= (float) (indexYLeft -1.5*passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		
		float rightMaxX= (float) (indexXRight + 3.5* passRightWidth + EXTRA_SLANT + keyboardView.getPaddingLeft());
		float rightMinX= (float) (indexXRight - 0.5*passRightWidth - MID_KEY_WIDTH - SLANT);
		float rightMaxY= (float) (indexYRight + 1.5*passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		float rightMinY= (float) (indexYRight - 1.5*passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		
		int screenMaxX=keyboardView.getWidth();//-keyboardView.getPaddingLeft();
		int screenMinX=-keyboardView.getPaddingLeft();
		int screenMaxY=keyboardView.getHeight()-keyboardView.getPaddingBottom()-keyboardView.getPaddingTop();
		int screenMinY=-keyboardView.getPaddingTop();
		
		
		//the position of coordinate system #4 and #5 within System #3 (coordinate transformation factors)
		float leftHandX = leftMinX;
		float leftHandY = leftMinY;
		float rightHandX = rightMinX;
		float rightHandY = rightMinY;
		
		float handWidth = 4*KEY_WIDTH + MID_KEY_WIDTH + SLANT + EXTRA_SLANT;
		
		
		/*BOUNDARY CHECK*/		
		int yPillow=(int) (KEY_WIDTH/KEY_WIDTH_TO_HEIGHT_FACTOR/2);
		int xPillow = KEY_WIDTH/2;
		
		if (leftMinX < screenMinX - xPillow){ //LEFT HAND LEFT SIDE
			leftHandX += screenMinX-leftMinX - xPillow;
			playError();
			feedBackMessage("Left hand off screen!");
		}
		if(rightMaxX  > screenMaxX + xPillow){ //RIGHT HAND RIGHT SIDE
			rightHandX -= rightMaxX -screenMaxX - xPillow;
			playError();
			feedBackMessage("Right hand off screen!");
		}
		
		if(leftMaxY > screenMaxY + yPillow){ //LEFT HAND BOTTOM SIDE
			leftHandY -= leftMaxY - screenMaxY - yPillow;
			playError();
			feedBackMessage("Left hand too low!");
		}
		if(leftMinY < screenMinY -yPillow){ //LEFT HAND TOP SIDE
			leftHandY += screenMinY-leftMinY -yPillow;
			playError();
			feedBackMessage("Left hand too high!");
		}
		
		if(rightMaxY > screenMaxY + yPillow){ //RIGHT HAND BOTTOM SIDE
			rightHandY -= rightMaxY-screenMaxY - yPillow;
			playError();
			feedBackMessage("Right hand too low!");
		}
		if(rightMinY < screenMinY - yPillow){ //RIGHT HAND TOP SIDE
			rightHandY += screenMinY-rightMinY - yPillow;
			playError();
			feedBackMessage("Right hand too high!");
		}
		
		Boolean doSqueezeKeys = false;
		int caseNum = (int)(( rightHandY-leftHandY )/(KEY_WIDTH/KEY_WIDTH_TO_HEIGHT_FACTOR)-2);
		float minDistance = SLANT * (caseNum) - 2*MID_KEY_WIDTH + KEY_WIDTH ;
		
		//updateMessage(Integer.toString(caseNum) + " : ");
		
		if(rightMinX - leftMaxX < minDistance){
			rightHandX += (minDistance - rightMinX + leftMaxX)/2;
			leftHandX -= (minDistance - rightMinX + leftMaxX)/2;
			playError();
			feedBackMessage("Hands too close together!");
		}
		if(rightMinX - leftMaxX < minDistance + 2*MID_KEY_WIDTH - KEY_WIDTH)
			doSqueezeKeys = true;
		
		
		// END BOUNDARY CHECKS
		
		
		Keyboard.Key _key = null;
		//this for loop draws each of the fingers according to the passed values, corrected for boundaries
		for(int i=0; i<keyboard_keys.size();i++){
			_key = keyboard_keys.get(i);
			
			if (i%10 < 5 && i < 30){ //left hand keys
				_key.width = passLeftWidth;
				_key.height = (int)((float)passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
				_key.x=(i%5)*passLeftWidth;
				_key.y= ((int)(i/10))*_key.height;
				
				_key.x+=leftHandX;
				_key.y+=leftHandY;
			}else if (i%10 >= 5 && i<30){ //right hand keys
				_key.width = passRightWidth;
				_key.height = (int)((float)passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
				_key.x=(i%5)*passRightWidth;
				_key.y= ((int)(i/10))*_key.height;
				
				_key.x+=rightHandX;
				_key.y+=rightHandY;
			}else{ //bottom row keys are special cases
				switch(i){
				case 30:
					_key.width = passLeftWidth;
					_key.height = (int)((float)passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
					_key.x=(int) leftHandX;
					_key.y=(int) (leftHandY + 3*_key.height);
					break;
				case 31:
					_key.width = 4*passLeftWidth;
					_key.height = (int)((float)passLeftWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
					_key.x=(int) leftHandX + passLeftWidth;
					_key.y=(int) (leftHandY + 3*_key.height);
					break;
				case 32:
					_key.width = 3*passRightWidth;
					_key.height = (int)((float)passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
					_key.x=(int) rightHandX;
					_key.y=(int) (rightHandY + 3*_key.height);
					break;
				case 33:
					_key.width = passRightWidth;
					_key.height = (int)((float)passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
					_key.x=(int) rightHandX + 3*passRightWidth;
					_key.y=(int) (rightHandY + 3*_key.height);
					break;
				case 34:
					_key.width = passRightWidth;
					_key.height = (int)((float)passRightWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
					_key.x=(int) rightHandX + 4*passRightWidth;
					_key.y=(int) (rightHandY + 3*_key.height);
					break;
				}//switch

			}//else
			/*adds the slant into the keys*/
			if ((int)(i/10) == 0)
				_key.x-=0;
			if ((int)(i/10) == 1)
				_key.x+=slant;
			if ((int)((float)i/10f) == 2)
				_key.x+=2*slant;
			if ((int)(i/10) == 3)
				_key.x+=3*slant;
			

			
			if(i%10 == 4 || i%10 == 5){ //set middle keys larger width
				_key.width=MID_KEY_WIDTH;
			}
			if(i%10 > 5){ //realign right hand keys to larger middle keys
				_key.x+=MID_KEY_WIDTH - KEY_WIDTH;
			}
		}//for loop which cycles through all keys
		
		
		//This switch compresses the center keys if the hands get close together
		float widthChange = ((minDistance + 2*MID_KEY_WIDTH - KEY_WIDTH) - (rightHandX - leftHandX-handWidth))/2;		
		if(doSqueezeKeys){
			switch(caseNum){
			case  0:
				keyboard_keys.get(5).width-=widthChange;
				keyboard_keys.get(5).x+=widthChange;
				keyboard_keys.get(24).width-=widthChange;
				
				keyboard_keys.get(14).width-=widthChange -SLANT;
				keyboard_keys.get(15).width-=widthChange -SLANT;
				keyboard_keys.get(15).x+=widthChange-SLANT;
				break;
			case -1:
				keyboard_keys.get(4).width-=widthChange -SLANT;
				keyboard_keys.get(5).width-=widthChange;
				keyboard_keys.get(5).x+=widthChange;
				keyboard_keys.get(14).width-=widthChange;
				keyboard_keys.get(15).width-=widthChange;
				keyboard_keys.get(15).x+=widthChange;
				keyboard_keys.get(24).width-=widthChange;
				keyboard_keys.get(25).width-=widthChange-SLANT;
				keyboard_keys.get(25).x+=widthChange-SLANT;
				break;
			case -2:
				keyboard_keys.get(4).width-=widthChange;
				keyboard_keys.get(5).width-=widthChange;
				keyboard_keys.get(5).x+=widthChange;
				keyboard_keys.get(14).width-=widthChange;
				keyboard_keys.get(15).width-=widthChange;
				keyboard_keys.get(15).x+=widthChange;
				keyboard_keys.get(24).width-=widthChange;
				keyboard_keys.get(25).width-=widthChange;
				keyboard_keys.get(25).x+=widthChange;
				break;
			case -3:
				keyboard_keys.get(4).width-=widthChange;
				keyboard_keys.get(14).width-=widthChange;
				keyboard_keys.get(15).width-=widthChange;
				keyboard_keys.get(15).x+=widthChange;
				keyboard_keys.get(25).width-=widthChange;
				keyboard_keys.get(25).x+=widthChange;
				break;
			case -4:
				keyboard_keys.get(4).width-=widthChange;
				keyboard_keys.get(25).width-=widthChange;
				keyboard_keys.get(25).x+=widthChange;
				break;
			case -5:
				break;
			}//case
		}
		
		keyboardView.invalidateAllKeys();
			
	}//redrawKeysFull
	
	private float getKeyCenterX(int keyIndex){return keyboard_keys.get(keyIndex).x+keyboard_keys.get(keyIndex).width/2f;}
	private float getKeyCenterY(int keyIndex){return keyboard_keys.get(keyIndex).y+keyboard_keys.get(keyIndex).height/2f;}

	/*inputs coordinates of index finger relative to the key's coordinates, not the view or screen coordinates*/
	private void redrawKeysHalf(int passKeyWidth, float indexXRight, float indexYRight){
		int viewWidth = keyboardView.getWidth();
		int viewHeight = keyboardView.getHeight();
		
		float keyboardOriginX = (float) (indexXRight - 1.5*KEY_WIDTH /* - SLANT*/);
		float keyboardOriginY = (float) (indexYRight - 1.5*KEY_WIDTH/KEY_WIDTH_TO_HEIGHT_FACTOR);
		
		float rightMaxX= (float) (indexXRight + 3.5* passKeyWidth /*+ EXTRA_SLANT */+ keyboardView.getPaddingLeft());
		float rightMinX= (float) (indexXRight - 0.5*passKeyWidth - MID_KEY_WIDTH /*- SLANT*/);
		float rightMaxY= (float) (indexYRight + 1.5*passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		float rightMinY= (float) (indexYRight - 1.5*passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
		
		int screenMaxX=keyboardView.getWidth();//-keyboardView.getPaddingLeft();
		int screenMinX=-keyboardView.getPaddingLeft();
		int screenMaxY=keyboardView.getHeight()-keyboardView.getPaddingBottom()-keyboardView.getPaddingTop();
		int screenMinY=-keyboardView.getPaddingTop();

		int yPillow=(int) (KEY_WIDTH/KEY_WIDTH_TO_HEIGHT_FACTOR/2);
		int xPillow = KEY_WIDTH/2;
		
		//BOUNDARY CHECKS
		
		if(rightMaxX  > screenMaxX + xPillow){ //RIGHT HAND RIGHT SIDE
			keyboardOriginX -= rightMaxX -screenMaxX - xPillow;
			playError();
			feedBackMessage("Right hand off screen!");
		}
		if(rightMinX  < screenMinX - xPillow){ //RIGHT HAND LEFT SIDE
			keyboardOriginX += screenMinX - rightMinX - xPillow;
			playError();
			feedBackMessage("Right hand off screen!");
		}					
		if(rightMaxY > screenMaxY + yPillow){ //RIGHT HAND BOTTOM SIDE
			keyboardOriginY -= rightMaxY-screenMaxY - yPillow;
			playError();
			feedBackMessage("Right hand too low!");
		}
		if(rightMinY < screenMinY - yPillow){ //RIGHT HAND TOP SIDE
			keyboardOriginY += screenMinY-rightMinY - yPillow;
			playError();
			feedBackMessage("Right hand too high!");
		}		
		
		//END boundary check
		
		
		Keyboard.Key _key = null;
		
		for(int i=0; i<keyboard_keys.size();i++){
			_key = keyboard_keys.get(i);
			_key.width = passKeyWidth;
			_key.height = (int)((float)passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
			
			_key.x=(i%5)*passKeyWidth;// + ((int)(i/5))*SLANT;
			_key.y= ((int)(i/5))*_key.height;
			
			if (i == 15)
				_key.width *=2;
			
			if (i > 15)
				_key.x+=_key.width;
			
			
			
			_key.x+= keyboardOriginX;
			_key.y+= keyboardOriginY;
			
			//_key.x-=_screenWidth/2;

		}
		
		keyboardView.invalidateAllKeys();

	
	}//redrawKeysHalf
	
	private void redrawKeysHalfOld (int passKeyWidth, float passX, float passY){ //passX and passY are in relation to key origin, NOT view origin
		if (isLeftHandMode || isRightHandMode)
		{
			//int _screenWidth = getWindowManager().getDefaultDisplay().getWidth();
			//int _sidePadding = (_screenWidth - 5*passKeyWidth)/2;
			//keyboardView.setPadding(_sidePadding, 30, _sidePadding, 30);
			
			/*Check to make sure requested redraw is on screen
			 * if not, sound error and return*/
			int viewWidth = keyboardView.getWidth();
			int viewHeight = keyboardView.getHeight();
			//updateMessage(Integer.toString(viewHeight));
			//updateMessage("<");
			//updateMessage(Float.toString(passY + 4*passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR+keyboardView.getPaddingTop()));
			
			if (passX < -keyboardView.getPaddingLeft() 
					|| passY < -keyboardView.getPaddingTop()
					|| passY + 4*passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR+keyboardView.getPaddingTop()>viewHeight
					|| (int)passX+5*passKeyWidth + keyboardView.getPaddingLeft() > viewWidth){
				playError();
				return;
			}
			
			//END boundary check
			
			
			Keyboard.Key _key = null;
			
			for(int i=0; i<keyboard_keys.size();i++){
				_key = keyboard_keys.get(i);
				_key.width = passKeyWidth;
				_key.height = (int)((float)passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR);
				
				_key.x=(i%5)*passKeyWidth;
				_key.y= ((int)(i/5))*_key.height;
				
				if (i == 15)
					_key.width *=2;
				
				if (i > 15)
					_key.x+=_key.width;
				
				
				_key.x+= passX;
				_key.y+= passY;
				
				//_key.x-=_screenWidth/2;
	
			}
			//keyboardView.setPadding(10, 30,5*passKeyWidth +2*REAL_PADDING_SIDES - keyboardView.getWidth(), (int) (4*passKeyWidth/KEY_WIDTH_TO_HEIGHT_FACTOR +2*REAL_PADDING_TOP- keyboardView.getHeight()));
			
			keyboardView.invalidateAllKeys();
			//_drawKeyboard.init(keyboard_keys, keyCodeMap);
		}else { //if (isLeftHandMode || isRightHandMode)
			
		}
	}//redrawKeys
	
	private void realignIndividualKey(float touchX,float touchY,Keyboard.Key currentKey){
		for(int i=0;i<keyboard_keys.size();i++){
			if (currentKey == keyboard_keys.get(i))
				{realignIndividualKey(touchX,touchY,currentKey,i);}
		}//for
	}
	//aligns the keyboard after a keystroke
	private void realignIndividualKey(float touchX,float touchY, Keyboard.Key currentKey, int index){
		if (index%10 == 0 || index%10 == 9 || index > 29)return;
		if (isRightHandMode || isLeftHandMode)return;
		
		float adjRX = 0;// touchX - keyboardView.getPaddingLeft() - getKeyCenterX(index) ;
		float adjLX = 0;//getKeyCenterY(index) - touchX;
		float adjRY = 0;
		float adjLY = 0;
		float moveRange = 0.3f;
		if (index == LEFT_FINGER_INDEX || index == RIGHT_FINGER_INDEX) moveRange = 0.05f;
		float xOffset = touchX - keyboardView.getPaddingLeft() - getKeyCenterX(index) ;
		float yOffset = touchY - keyboardView.getPaddingTop() - getKeyCenterY(index);
		
		
		if ((xOffset*xOffset) < (currentKey.width*currentKey.width*moveRange*moveRange)) xOffset = 0;
		else if (xOffset > 0) xOffset = (float) (xOffset - currentKey.width*moveRange);
		else xOffset = (float) (xOffset +currentKey.width*moveRange);
		
		
		if ((yOffset*yOffset) < (currentKey.width*currentKey.width*moveRange*moveRange)) yOffset = 0;
		else if (yOffset > 0) yOffset = (float)(yOffset - currentKey.width*moveRange);
		else yOffset = (float)(yOffset + currentKey.width*moveRange);

		if (index%10 < 5) {
			adjLX = xOffset;
			adjLY = yOffset;
		}
		else {
			adjRX = xOffset;
			adjRY = yOffset;
		}
			
		redrawKeysFull(KEY_WIDTH,KEY_WIDTH, getKeyCenterX(LEFT_FINGER_INDEX) + adjLX, getKeyCenterX(RIGHT_FINGER_INDEX) + adjRX, getKeyCenterY(LEFT_FINGER_INDEX) + adjLY, getKeyCenterY(RIGHT_FINGER_INDEX)+adjRY, SLANT);
	}//realignIndividualKey

	@Override
	public void onKey(int primaryCode, int[] keyCodes) {
	  //traditional keystroke
    }
    
	@Override
	public void onPress(int primaryCode) {
		//final EditText edittext = (EditText) findViewById(R.id.editText);
		//edittext.setCursorVisible(false);
        //String c = keyCodeMap.get(String.valueOf(primaryCode));
        //if(!(c == null)) edittext.append(" <V"+c+"> ");
	}

	@Override
	public void onRelease(int primaryCode) {
		//final EditText edittext = (EditText) findViewById(R.id.editText);
		//edittext.setCursorVisible(false);
        //String c = keyCodeMap.get(String.valueOf(primaryCode));
        //if(!(c == null)) edittext.append(" <^"+c+"> ");
	}

	@Override
	public void onText(CharSequence text) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void swipeDown() {
		//final EditText editText = (EditText) findViewById(R.id.editText);
		//editText.setCursorVisible(false);
        //editText.append(" <swipeDown> ");
	}	

	@Override
	public void swipeLeft() {
		//final EditText editText = (EditText) findViewById(R.id.editText);
		//editText.setCursorVisible(false);
        //editText.append(" <swipeLeft> ");
	}

	@Override
	public void swipeRight() {
		//final EditText edittext = (EditText) findViewById(R.id.editText);
		//edittext.setCursorVisible(false);
        //edittext.append(" <swipeRight> ");
	}
	


	@Override
	public void swipeUp() {
		//final EditText edittext = (EditText) findViewById(R.id.editText);
		//edittext.setCursorVisible(false);
        //edittext.append(" <swipeUp> ");
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void initiateHashMap(){	
		//all keys for all keyboards are functionally defined here
		
		angleMap.put("0", angles_LeftRight ); keyCodeMap.put("0",new String[]{"\b"," "});
		
		//1234 full keyboard
		 angleMap.put("49",angles_Tap_LeftRight ); keyCodeMap.put("49",new String[]{"1","\b"," "});
	     angleMap.put("50",angles_Tap_LeftRight ); keyCodeMap.put("50",new String[]{"2","\b"," "});
	     angleMap.put("51",angles_Tap_LeftRight ); keyCodeMap.put("51",new String[]{"3","\b"," "});
	     angleMap.put("52",angles_Tap_LeftRight ); keyCodeMap.put("52",new String[]{"4","\b"," "});
	     angleMap.put("53",angles_Tap_LeftRight ); keyCodeMap.put("53",new String[]{"5","\b"," "});
	     angleMap.put("54",angles_Tap_LeftRight ); keyCodeMap.put("54",new String[]{"6","\b"," "});
	     angleMap.put("55",angles_Tap_LeftRight ); keyCodeMap.put("55",new String[]{"7","\b"," "});
	     angleMap.put("56",angles_Tap_LeftRight ); keyCodeMap.put("56",new String[]{"8","\b"," "});
	     angleMap.put("57",angles_Tap_LeftRight ); keyCodeMap.put("57",new String[]{"9","\b"," "});
	     angleMap.put("48",angles_Tap_LeftRight ); keyCodeMap.put("48",new String[]{"0","\b"," "});
	     
	     angleMap.put("45",angles_Tap_LeftRight ); keyCodeMap.put("45",new String[]{"-","\b"," "});
	     angleMap.put("47",angles_Tap_LeftRight ); keyCodeMap.put("47",new String[]{"/","\b"," "});
	     angleMap.put("58",angles_Tap_LeftRight ); keyCodeMap.put("58",new String[]{":","\b"," "});
	     angleMap.put("59",angles_Tap_LeftRight ); keyCodeMap.put("59",new String[]{";","\b"," "});
	     angleMap.put("40",angles_Tap_LeftRight ); keyCodeMap.put("40",new String[]{"(","\b"," "});
	     angleMap.put("41",angles_Tap_LeftRight ); keyCodeMap.put("41",new String[]{")","\b"," "});
	     angleMap.put("36",angles_Tap_LeftRight ); keyCodeMap.put("36",new String[]{"$","\b"," "});
	     angleMap.put("38",angles_Tap_LeftRight ); keyCodeMap.put("38",new String[]{"&","\b"," "});
	     angleMap.put("64",angles_Tap_LeftRight ); keyCodeMap.put("64",new String[]{"@","\b"," "});
	     angleMap.put("37",angles_Tap_LeftRight ); keyCodeMap.put("37",new String[]{"%","\b"," "});
	     angleMap.put( "-7",angles_Tap_LeftRight ); keyCodeMap.put( "-7",new String[]{"Special","\b"," "});
	     
	     angleMap.put("320",angles_Tap_Left ); keyCodeMap.put("320",new String[]{"<","ABC"});
	     angleMap.put("321",angles_Tap_Right ); keyCodeMap.put("321",new String[]{">","Talk"});
	     angleMap.put("322",angles_Tap_LeftRight ); keyCodeMap.put("322",new String[]{".","\b"," "});
	     angleMap.put("323",angles_Tap_LeftRight ); keyCodeMap.put("323",new String[]{"!","\b"," "});
	     angleMap.put("324",angles_Tap_LeftRight ); keyCodeMap.put("324",new String[]{"~","\b"," "});
	     angleMap.put("325",angles_Tap_LeftRight ); keyCodeMap.put("325",new String[]{"*","\b"," "});
	     angleMap.put("326",angles_Tap_LeftRight ); keyCodeMap.put("326",new String[]{"`","\b"," "});
	     angleMap.put("327",angles_Tap_LeftRight ); keyCodeMap.put( "327",new String[]{";","\b"," "});
	     angleMap.put("328",angles_Tap_Left); keyCodeMap.put("328",new String[]{"\"","Other"});
	     angleMap.put("329",angles_Tap_Right ); keyCodeMap.put("329",new String[]{"Shift","\n"});
	     
	     //end new full
	     
	     //angleMap.put("43",angles_TapOnly ); keyCodeMap.put("43",new String[]{"+"});
	     //angleMap.put("35",angles_TapOnly ); keyCodeMap.put("35",new String[]{"#"});
	     
	     //angleMap.put("44",angles_TapOnly ); keyCodeMap.put("44",new String[]{","});
	     //angleMap.put("63",angles_TapOnly ); keyCodeMap.put("63",new String[]{"?"});
	     
	     //angleMap.put("45",angles_TapOnly ); keyCodeMap.put("45",new String[]{"-"});
	     
	     
	     
	     
	     //angleMap.put("34",angles_TapOnly ); keyCodeMap.put("34",new String[]{"\""});
	     
	     
//qwerty full keyboard
	     
	     angleMap.put("113",angles_Tap_LeftRight_Up ); keyCodeMap.put("113",new String[]{"q","\b"," ","1"});
	     angleMap.put("119",angles_Tap_LeftRight_Up ); keyCodeMap.put("119",new String[]{"w","\b"," ","2"});
	     angleMap.put("101",angles_Tap_LeftRight_Up ); keyCodeMap.put("101",new String[]{"e","\b"," ","3"});
	     angleMap.put("114",angles_Tap_LeftRight_Up ); keyCodeMap.put("114",new String[]{"r","\b"," ","4"});
	     angleMap.put("116",angles_Tap_LeftRight_Up ); keyCodeMap.put("116",new String[]{"t","\b"," ","5"});
	     angleMap.put("121",angles_Tap_LeftRight_Up ); keyCodeMap.put("121",new String[]{"y","\b"," ","6"});
	     angleMap.put("117",angles_Tap_LeftRight_Up ); keyCodeMap.put("117",new String[]{"u","\b"," ","7"});
	     angleMap.put("105",angles_Tap_LeftRight_Up ); keyCodeMap.put("105",new String[]{"i","\b"," ","8"});
	     angleMap.put("111",angles_Tap_LeftRight_Up ); keyCodeMap.put("111",new String[]{"o","\b"," ","9"});
	     angleMap.put("112",angles_Tap_LeftRight_Up ); keyCodeMap.put("112",new String[]{"p","\b"," ","0"});	
	     
	     angleMap.put( "97",angles_Tap_LeftRight ); keyCodeMap.put( "97",new String[]{"a","\b"," "});
	     angleMap.put("115",angles_Tap_LeftRight ); keyCodeMap.put("115",new String[]{"s","\b"," "});
	     angleMap.put("100",angles_Tap_LeftRight ); keyCodeMap.put("100",new String[]{"d","\b"," "});
	     angleMap.put("102",angles_Tap_LeftRight ); keyCodeMap.put("102",new String[]{"f","\b"," "});
	     angleMap.put("103",angles_Tap_LeftRight ); keyCodeMap.put("103",new String[]{"g","\b"," "});
	     angleMap.put("104",angles_Tap_LeftRight ); keyCodeMap.put("104",new String[]{"h","\b"," "});
	     angleMap.put("106",angles_Tap_LeftRight ); keyCodeMap.put("106",new String[]{"j","\b"," "});
	     angleMap.put("107",angles_Tap_LeftRight ); keyCodeMap.put("107",new String[]{"k","\b"," "});
	     angleMap.put("108",angles_Tap_LeftRight ); keyCodeMap.put("108",new String[]{"l","\b"," "});
	     angleMap.put( "59",angles_Tap_LeftRight ); keyCodeMap.put( "59",new String[]{" ","Space"," "});
	     
	     angleMap.put("300",angles_Tap_Left ); keyCodeMap.put("300",new String[]{"z","123"});
	     angleMap.put("301",angles_Tap_Right ); keyCodeMap.put("301",new String[]{"x","talk"});
	     angleMap.put("302",angles_Tap_LeftRight ); keyCodeMap.put("302",new String[]{"c","\b"," "});
	     angleMap.put("303",angles_Tap_LeftRight ); keyCodeMap.put("303",new String[]{"v","\b"," "});
	     angleMap.put("304",angles_Tap_LeftRight ); keyCodeMap.put("304",new String[]{"b","\b"," "});
	     angleMap.put("305",angles_Tap_LeftRight ); keyCodeMap.put("305",new String[]{"n","\b"," "});
	     angleMap.put("306",angles_Tap_LeftRight ); keyCodeMap.put("306",new String[]{"m","\b"," "}); 
	     angleMap.put("307",angles_Tap_LeftRight ); keyCodeMap.put("307",new String[]{",","\b"," "}); 
	     angleMap.put("308",angles_Tap_Left ); keyCodeMap.put("308",new String[]{".","Other"});
	     angleMap.put("309",angles_Tap_Right ); keyCodeMap.put("309",new String[]{"Shift","\n"});
	     
	     //qwerty shifted
	     
	     
	     angleMap.put("600",angles_Tap_LeftRight_Up ); keyCodeMap.put("600",new String[]{"Q","\b"," ","1"});
	     angleMap.put("601",angles_Tap_LeftRight_Up ); keyCodeMap.put("601",new String[]{"W","\b"," ","2"});
	     angleMap.put("602",angles_Tap_LeftRight_Up ); keyCodeMap.put("602",new String[]{"E","\b"," ","3"});
	     angleMap.put("603",angles_Tap_LeftRight_Up ); keyCodeMap.put("603",new String[]{"R","\b"," ","4"});
	     angleMap.put("604",angles_Tap_LeftRight_Up ); keyCodeMap.put("604",new String[]{"T","\b"," ","5"});
	     angleMap.put("605",angles_Tap_LeftRight_Up ); keyCodeMap.put("605",new String[]{"Y","\b"," ","6"});
	     angleMap.put("606",angles_Tap_LeftRight_Up ); keyCodeMap.put("606",new String[]{"U","\b"," ","7"});
	     angleMap.put("607",angles_Tap_LeftRight_Up ); keyCodeMap.put("607",new String[]{"I","\b"," ","8"});
	     angleMap.put("608",angles_Tap_LeftRight_Up ); keyCodeMap.put("608",new String[]{"O","\b"," ","9"});
	     angleMap.put("609",angles_Tap_LeftRight_Up ); keyCodeMap.put("609",new String[]{"P","\b"," ","0"});
	     
	     angleMap.put("620",angles_Tap_LeftRight ); keyCodeMap.put("620",new String[]{"A","\b"," "});
	     angleMap.put("621",angles_Tap_LeftRight ); keyCodeMap.put("621",new String[]{"S","\b"," "});
	     angleMap.put("622",angles_Tap_LeftRight ); keyCodeMap.put("622",new String[]{"D","\b"," "});
	     angleMap.put("623",angles_Tap_LeftRight ); keyCodeMap.put("623",new String[]{"F","\b"," "});
	     angleMap.put("624",angles_Tap_LeftRight ); keyCodeMap.put("624",new String[]{"G","\b"," "});
	     angleMap.put("625",angles_Tap_LeftRight ); keyCodeMap.put("625",new String[]{"H","\b"," "});
	     angleMap.put("626",angles_Tap_LeftRight ); keyCodeMap.put("626",new String[]{"J","\b"," "});
	     angleMap.put("627",angles_Tap_LeftRight ); keyCodeMap.put("627",new String[]{"K","\b"," "});
	     angleMap.put("628",angles_Tap_LeftRight ); keyCodeMap.put("628",new String[]{"L","\b"," "});
	     angleMap.put("629",angles_Tap_LeftRight ); keyCodeMap.put("629",new String[]{" ","Space"," "});
	     
	     angleMap.put("640",angles_Tap_Left ); keyCodeMap.put("640",new String[]{"Z","123"});
	     angleMap.put("641",angles_Tap_Right ); keyCodeMap.put("641",new String[]{"X","Talk"});
	     angleMap.put("642",angles_Tap_LeftRight ); keyCodeMap.put("642",new String[]{"C","\b"," "});
	     angleMap.put("643",angles_Tap_LeftRight ); keyCodeMap.put("643",new String[]{"V","\b"," "});
	     angleMap.put("644",angles_Tap_LeftRight ); keyCodeMap.put("644",new String[]{"B","\b"," "});
	     angleMap.put("645",angles_Tap_LeftRight ); keyCodeMap.put("645",new String[]{"N","\b"," "});
	     angleMap.put("646",angles_Tap_LeftRight ); keyCodeMap.put("646",new String[]{"M","\b"," "});
	     angleMap.put("647",angles_Tap_LeftRight ); keyCodeMap.put("647",new String[]{",","\b"," "});
	     angleMap.put("648",angles_Tap_Left ); keyCodeMap.put("648",new String[]{".","Other"});
	     angleMap.put("649",angles_Tap_Right ); keyCodeMap.put("649",new String[]{"Shift","\n"});
	     
	     
	     
//	     angleMap.put("122",angles_TapOnly ); keyCodeMap.put("122",new String[]{"z"});
//	     angleMap.put("120",angles_TapOnly ); keyCodeMap.put("120",new String[]{"x"});
//	     angleMap.put( "99",angles_TapOnly ); keyCodeMap.put( "99",new String[]{"c"});
//	     angleMap.put("118",angles_TapOnly ); keyCodeMap.put("118",new String[]{"v"});
//	     angleMap.put( "98",angles_TapOnly ); keyCodeMap.put( "98",new String[]{"b"});
//	     angleMap.put("110",angles_TapOnly ); keyCodeMap.put("110",new String[]{"n"});
//	     angleMap.put("109",angles_TapOnly ); keyCodeMap.put("109",new String[]{"m"}); 
//	     angleMap.put( "44",angles_TapOnly ); keyCodeMap.put( "44",new String[]{","}); 
//	     angleMap.put( "46",angles_TapOnly ); keyCodeMap.put( "46",new String[]{"."});
//	     angleMap.put( "10",angles_TapOnly ); keyCodeMap.put( "10",new String[]{"\n"});
	     
	     //special full keyboard 500 keys
	     
	     angleMap.put("500",angles_Tap_LeftRight ); keyCodeMap.put("500",new String[]{"[","\b"," "});
	     angleMap.put("501",angles_Tap_LeftRight ); keyCodeMap.put("501",new String[]{"]","\b"," "});
	     angleMap.put("502",angles_Tap_LeftRight ); keyCodeMap.put("502",new String[]{"{","\b"," "});
	     angleMap.put("503",angles_Tap_LeftRight ); keyCodeMap.put("503",new String[]{"}","\b"," "});
	     angleMap.put("504",angles_Tap_LeftRight ); keyCodeMap.put("504",new String[]{"#","\b"," "});
	     angleMap.put("505",angles_Tap_LeftRight ); keyCodeMap.put("505",new String[]{"%","\b"," "});
	     angleMap.put("506",angles_Tap_LeftRight ); keyCodeMap.put("506",new String[]{"^","\b"," "});
	     angleMap.put("507",angles_Tap_LeftRight ); keyCodeMap.put("507",new String[]{"*","\b"," "});
	     angleMap.put("508",angles_Tap_LeftRight ); keyCodeMap.put("508",new String[]{"+","\b"," "});
	     angleMap.put("509",angles_Tap_LeftRight ); keyCodeMap.put("509",new String[]{"=","\b"," "});
	     
	     angleMap.put("520",angles_Tap_LeftRight ); keyCodeMap.put("520",new String[]{"\\","\b"," "});
	     angleMap.put("521",angles_Tap_LeftRight ); keyCodeMap.put("521",new String[]{"|","\b"," "});
	     angleMap.put("522",angles_Tap_LeftRight ); keyCodeMap.put("522",new String[]{"~","\b"," "});
	     angleMap.put("523",angles_Tap_LeftRight ); keyCodeMap.put("523",new String[]{"<","\b"," "});
	     angleMap.put("524",angles_Tap_LeftRight ); keyCodeMap.put("524",new String[]{">","\b"," "});
	     angleMap.put("525",angles_Tap_LeftRight ); keyCodeMap.put("525",new String[]{"α","\b"," "});
	     angleMap.put("526",angles_Tap_LeftRight ); keyCodeMap.put("526",new String[]{"β","\b"," "});
	     angleMap.put("527",angles_Tap_LeftRight ); keyCodeMap.put("527",new String[]{"","\b"," "});
	     angleMap.put("528",angles_Tap_LeftRight ); keyCodeMap.put("528",new String[]{"Ω","\b"," "});
	     angleMap.put("529",angles_Tap_LeftRight ); keyCodeMap.put("529",new String[]{"123","\b"," "});
	     
	     angleMap.put("540",angles_TapOnly ); keyCodeMap.put("540",new String[]{"Redo"});
	     angleMap.put("541",angles_TapOnly ); keyCodeMap.put("541",new String[]{":)"});
	     angleMap.put("542",angles_Tap_LeftRight ); keyCodeMap.put("542",new String[]{".","\b"," "});
	     angleMap.put("543",angles_Tap_LeftRight ); keyCodeMap.put("543",new String[]{",","\b"," "});
	     angleMap.put("544",angles_Tap_LeftRight ); keyCodeMap.put("544",new String[]{";","\b"," "});
	     angleMap.put("545",angles_Tap_LeftRight ); keyCodeMap.put("545",new String[]{"Find","\b"," "});
	     angleMap.put("546",angles_Tap_LeftRight ); keyCodeMap.put("546",new String[]{".com","\b"," "});
	     angleMap.put("547",angles_Tap_LeftRight ); keyCodeMap.put("547",new String[]{"","\b"," "});
	     angleMap.put("548",angles_TapOnly ); keyCodeMap.put("548",new String[]{"γ"});
	     angleMap.put("549",angles_TapOnly ); keyCodeMap.put("549",new String[]{"\n"});
	     
	     
	
	    
	     
	  // right half-keyboard qwerty
	     angleMap.put("216",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("216",new String[]{"y","t","y","\b"," "});
	     angleMap.put("214",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("214",new String[]{"u","r","u","\b"," "});
	     angleMap.put("201",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("201",new String[]{"i","e","i","\b"," "});
	     angleMap.put("219",angles_Tap_DiagsDown); keyCodeMap.put("219",new String[]{"o","w","o"});
	     angleMap.put("213",angles_Tap_DiagsDown); keyCodeMap.put("213",new String[]{"p","q","p"});
	     
	     angleMap.put("203",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("203",new String[]{"h","g","h","\b"," "});
	     angleMap.put("202",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("202",new String[]{"j","f","j","\b"," "});
	     angleMap.put("200",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("200",new String[]{"k","d","k","\b"," "});
	     angleMap.put("215",angles_Tap_DiagsDown); keyCodeMap.put("215",new String[]{"l","s","l"});
	     angleMap.put("197",angles_Tap_DiagsDown); keyCodeMap.put("197",new String[]{"Shift","a","Shift"});
	     
	     angleMap.put("198",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("198",new String[]{"n","b","n","\b"," "});
	     angleMap.put("218",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("218",new String[]{"m","v","m","\b"," "});
	     angleMap.put("199",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("199",new String[]{",","c",",","\b"," "});
	     angleMap.put("220",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("220",new String[]{".","x",".","Other","Talk"});
	     angleMap.put("222",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("222",new String[]{"\n","z","\n","123","Hide"});
	     
	     
	  // right half-keyboard qwerty Shifted
	     angleMap.put("230",angles_Tap_DiagsDown); keyCodeMap.put("230",new String[]{"Y","T","Y"});
	     angleMap.put("231",angles_Tap_DiagsDown); keyCodeMap.put("231",new String[]{"U","R","U"});
	     angleMap.put("232",angles_Tap_DiagsDown); keyCodeMap.put("232",new String[]{"I","E","I"});
	     angleMap.put("233",angles_Tap_DiagsDown); keyCodeMap.put("233",new String[]{"O","W","O"});
	     angleMap.put("234",angles_Tap_DiagsDown); keyCodeMap.put("234",new String[]{"P","Q","P"});
	     
	     angleMap.put("235",angles_Tap_DiagsDown); keyCodeMap.put("235",new String[]{"H","G","H"});
	     angleMap.put("236",angles_Tap_DiagsDown); keyCodeMap.put("236",new String[]{"J","F","J"});
	     angleMap.put("237",angles_Tap_DiagsDown); keyCodeMap.put("237",new String[]{"K","D","K"});
	     angleMap.put("238",angles_Tap_DiagsDown); keyCodeMap.put("238",new String[]{"L","S","L"});
	     angleMap.put("239",angles_Tap_DiagsDown); keyCodeMap.put("239",new String[]{"Shift","A","Shift"});
	     
	     angleMap.put("240",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("240",new String[]{"N","B","N","\b"," "});
	     angleMap.put("241",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("241",new String[]{"M","V","M","\b"," "});
	     angleMap.put("242",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("242",new String[]{",","C",",","\b"," "});
	     angleMap.put("243",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("243",new String[]{".","X",".","Other","Talk"});
	     angleMap.put("244",angles_Tap_DiagsDown_LeftRight); keyCodeMap.put("244",new String[]{"\n","Z","\n","123","Hide"});
	     
	     
	     // right half-keyboard 1234
	     angleMap.put("260",angles_DiagsDown); keyCodeMap.put("260",new String[]{"1","2"});
	     angleMap.put("261",angles_DiagsDown); keyCodeMap.put("261",new String[]{"3","4"});
	     angleMap.put("262",angles_DiagsDown); keyCodeMap.put("262",new String[]{"5","6"});
	     angleMap.put("263",angles_DiagsDown); keyCodeMap.put("263",new String[]{"7","8"});
	     angleMap.put("264",angles_DiagsDown); keyCodeMap.put("264",new String[]{"8","0"});
	     
	     angleMap.put("265",angles_DiagsDown); keyCodeMap.put("265",new String[]{"-","_"});
	     angleMap.put("266",angles_DiagsDown); keyCodeMap.put("266",new String[]{"/",":"});
	     angleMap.put("267",angles_DiagsDown); keyCodeMap.put("267",new String[]{"(",")"});
	     angleMap.put("268",angles_DiagsDown); keyCodeMap.put("268",new String[]{"$","&"});
	     angleMap.put("269",angles_DiagsDown); keyCodeMap.put("269",new String[]{"@","Special"});
	     
	     angleMap.put("270",angles_DiagsDown_LeftRight); keyCodeMap.put("270",new String[]{"<",">","\b"," "});
	     angleMap.put("271",angles_DiagsDown_LeftRight); keyCodeMap.put("271",new String[]{".","!","\b"," "});
	     angleMap.put("272",angles_DiagsDown_LeftRight); keyCodeMap.put("272",new String[]{"~","*","\b"," "});
	     angleMap.put("273",angles_DiagsDown_LeftRight); keyCodeMap.put("273",new String[]{"`",";","Other","Talk"});
	     angleMap.put("274",angles_DiagsDown_LeftRight); keyCodeMap.put("274",new String[]{"\"","\n","ABC","Hide"});
	     
//	     angleMap.put("-105",angles_LeftRight); keyCodeMap.put("-105",new String[]{"ABC","Hide"});

	     // right half-keyboard special
	     angleMap.put("275",angles_DiagsDown); keyCodeMap.put("275",new String[]{"[","]"});
	     angleMap.put("276",angles_DiagsDown); keyCodeMap.put("276",new String[]{"{","}"});
	     angleMap.put("277",angles_DiagsDown); keyCodeMap.put("277",new String[]{"#","%"});
	     angleMap.put("278",angles_DiagsDown); keyCodeMap.put("278",new String[]{"^","*"});
	     angleMap.put("279",angles_DiagsDown); keyCodeMap.put("279",new String[]{"+","="});//1234
	    
	     angleMap.put("280",angles_DiagsDown); keyCodeMap.put("280",new String[]{"\\","|"});
	     angleMap.put("281",angles_DiagsDown); keyCodeMap.put("281",new String[]{"~","<"});
	     angleMap.put("282",angles_DiagsDown); keyCodeMap.put("282",new String[]{">","α"});//γ
	     angleMap.put("283",angles_DiagsDown); keyCodeMap.put("283",new String[]{"β","δ"});
	     angleMap.put("284",angles_DiagsDown); keyCodeMap.put("284",new String[]{"Ω","123"});
	     
	     angleMap.put("285",angles_DiagsDown_LeftRight); keyCodeMap.put("285",new String[]{"b","n","\b"," "});//redo
	     angleMap.put("286",angles_DiagsDown_LeftRight); keyCodeMap.put("286",new String[]{"_","%","\b"," "});//character _ used twice % used twice
	     angleMap.put("287",angles_DiagsDown_LeftRight); keyCodeMap.put("287",new String[]{"-",",","\b"," "}); //used twice
	     angleMap.put("288",angles_DiagsDown_LeftRight); keyCodeMap.put("288",new String[]{".","signs","Other","Talk"}); //used twice
	     angleMap.put("289",angles_DiagsDown_LeftRight); keyCodeMap.put("289",new String[]{"Find","\n","ABC","Hide"});	     	     
	     
	     
	    
	     
	}//initiateHashMap

}//MorningStar.java