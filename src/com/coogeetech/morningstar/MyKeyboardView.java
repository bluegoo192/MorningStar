package com.coogeetech.morningstar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.graphics.Matrix;
import android.renderscript.Font.Style;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class MyKeyboardView extends KeyboardView{

	//==============================================
	//         Class wide variable declaration
	//==============================================
	private Boolean isInitiated = false;
	MorningStar _my;
	
	private int dX;
	private int dY;
	
	Paint shadowPaint;
	
	//draw keyboard Background method
	Paint keyboardBackgroundPaint;
	Path backgroundShapeLeft;
	Path backgroundShapeRight;
	
	//draw keys method
	private int keyCornerRadius = 6;
	private int keyPadding = 20;
	private int shadowBottom = 4;
	private int shadowRight = 2;
	private int shadowTop = 1;
	private int shadowLeft = 1;
	private int keyWidth_visible;
	private int keyHeight_visible;
	Keyboard.Key exampleKey;
	
	Paint keyPaint;
	Paint keyPaint2;
	Paint keyPaint3;
	Paint keyPaint4;
	
	//draw Text method
	Paint mainTextPaint;
	Paint topNumberPaint;
	Paint functionPaint;
	Paint wordTextPaint;
	
	Paint diagRight;
	Paint diagLeft;
	
	//======END DECLARATION SECTION=================
	
	public MyKeyboardView(Context context, AttributeSet attrs) {
		super(context, attrs);
		_my = (MorningStar) context;
		//init();
	}
	
	public MyKeyboardView(Context context) {
		super(context, null);
		_my = (MorningStar) context;
		//init();
	}
	
	//initiates and defines 
	private void init(){
		
		
		//program wide
		dX = _my.keyboardView.getPaddingLeft();  //the width of the left margin
		dY = _my.keyboardView.getPaddingTop();   //the width(vert) of the top margin
		
		shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		shadowPaint.setColor(Color.BLACK);
		shadowPaint.setAlpha(20);
		
		//keyboard background
		keyboardBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		keyboardBackgroundPaint.setStyle(Paint.Style.FILL_AND_STROKE);;
		keyboardBackgroundPaint.setStrokeJoin(Paint.Join.ROUND);
		keyboardBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
		keyboardBackgroundPaint.setDither(true);
		keyboardBackgroundPaint.setStrokeWidth(30);
		keyboardBackgroundPaint.setShader(new LinearGradient(
				0, 0, 
				0, getHeight(), 
				Color.argb(255,42,42,58), 
				Color.argb(255,22,22,38), 
				Shader.TileMode.MIRROR));
		
		//key colors
		exampleKey = _my.keyboard_keys.get(0);
		keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		keyPaint2 = new Paint();
		keyPaint2.setColor(Color.WHITE);
		keyPaint2.setAlpha(50);
		keyPaint3 = new Paint();
		keyPaint3.setColor(Color.BLACK);
		keyPaint3.setAlpha(50);
		
		//text formats
		mainTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		mainTextPaint.setColor(Color.BLACK);
		mainTextPaint.setTextSize(MorningStar.KEY_WIDTH/3);
		mainTextPaint.setTextAlign(Align.CENTER);
		//mainTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
		topNumberPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		topNumberPaint.setTextSize(MorningStar.KEY_WIDTH/5);
		topNumberPaint.setColor(Color.BLUE);
		topNumberPaint.setTextAlign(Align.CENTER);
		functionPaint = new Paint();
		//the Paint used for words such as "shift"
		wordTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
		wordTextPaint.setColor(Color.BLACK);
		wordTextPaint.setTextSize(MorningStar.KEY_WIDTH/5);
		wordTextPaint.setTextAlign(Align.CENTER);
		


		
		diagRight = new Paint();
		diagRight.setTextSize(mainTextPaint.getTextSize());
		diagRight.setColor(Color.BLACK);
		diagRight.setTextAlign(Align.CENTER);
		diagLeft = new Paint();
		diagLeft.setTextSize(topNumberPaint.getTextSize());
		diagLeft.setColor(Color.BLUE);
		diagLeft.setTextAlign(Align.CENTER);
		
		
		
	}//init
	
	@Override
	public boolean onLongPress(Keyboard.Key popupKey){	
		
		return super.onLongPress(popupKey);

	}
	
	
	@Override
	public void onDraw(Canvas passCanvas){
		if(!isInitiated){
			init();
			isInitiated = false;
		}
		
		//super.onDraw(passCanvas);
		passCanvas.drawColor(Color.BLACK);
		if (_my.isRightHandMode) onDrawHalf(passCanvas);
		else onDrawFull(passCanvas);
		
		//return;
		
		

		
	}//onDraw
	
	private void onDrawFull(Canvas passCanvas){
		drawKeyboardBackgroundFull(passCanvas);
		drawKeyShapes(passCanvas);
		drawFunctionBarsFull(passCanvas);
		drawRegText(passCanvas);
		//drawSecialTextPicsFull(passCanvas);
		
	}//onDrawFull
	
	private void onDrawHalf(Canvas passCanvas){
		drawKeyboardBackgroundHalf(passCanvas);
		drawKeyShapes(passCanvas);
		//drawFunctionBarsHalf(passCanvas);
		drawRegText(passCanvas);
		//drawSepcialTextPicsFull(passCanvas);
	}//onDrawHalf
	
	private void drawKeyboardBackgroundHalf(Canvas passCanvas){
		backgroundShapeRight = new Path();
		
		backgroundShapeRight.moveTo(_my.keyboard_keys.get(0).x, _my.keyboard_keys.get(0).y);
		backgroundShapeRight.rLineTo(5*MorningStar.KEY_WIDTH, 0);
		backgroundShapeRight.rLineTo(0, MorningStar.KEY_WIDTH *3);
		backgroundShapeRight.rLineTo(-MorningStar.KEY_WIDTH *5,0);
		backgroundShapeRight.rLineTo(0, -MorningStar.KEY_WIDTH *3);
		backgroundShapeRight.offset(dX , dY);
		
		passCanvas.drawPath(backgroundShapeRight, keyboardBackgroundPaint);
		
	}//drawKeyboardBackgroundHalf
	
	private void drawFunctionBarsFull(Canvas passCanvas){
		
	}//drawFunctionsBarsFull
	
	private void drawRegText(Canvas passCanvas){
		
		Key mKey = _my.keyboard_keys.get(1);
		String thisLetter;
		int thisAngle;
		for (int i=0; i<_my.keyboard_keys.size();i++){					//for loop which cycles through all keys
			mKey = _my.keyboard_keys.get(i);							//the specific key we will be working with
			int midX = mKey.x + dX + mKey.width/2;
			int midY = (int) (mKey.y + dY + mKey.height/2 + mainTextPaint.getTextSize()/2);
			for (int j=0; j<_my.angleMap.get(String.valueOf(mKey.codes[0])).length;j++){//for loop which cycles possible values a key can activate
				thisAngle = Integer.valueOf(_my.angleMap.get(String.valueOf(mKey.codes[0]))[j]);
				thisLetter = _my.keyCodeMap.get(String.valueOf(mKey.codes[0]))[j];
				switch(thisAngle){
				case -1: //tap letter
					if (_my.isRightHandMode) break;
					if (thisLetter.length() > 1) {  //if it is a word then draw it smaller to fit inside the key
						passCanvas.drawText(thisLetter, midX, midY - (wordTextPaint.getTextSize()/2), wordTextPaint);
					} else {
						passCanvas.drawText(thisLetter,midX,midY, mainTextPaint);
					}
					break;
				case 180: //up letter
					//passCanvas.drawText(thisLetter,midX, mKey.y+dY+(int)(topNumberPaint.getTextSize()*1.3), topNumberPaint);
					break;
				case 0: //down letter
					//passCanvas.drawText(thisLetter,midX, mKey.y+dY+mKey.height-mainTextPaint.getTextSize()/2, mainTextPaint);
					break;
				case -90: //left letter
					//passCanvas.drawText(thisLetter,mKey.x+dX + mainTextPaint.getTextSize(),midY, mainTextPaint);
					break;
				case 90: //right letter
					if (thisLetter.length() > 1) {  //if it is a word then draw it smaller to fit inside the key
						passCanvas.drawText(thisLetter, midX, midY - (wordTextPaint.getTextSize()/2), wordTextPaint);
					} else {
						passCanvas.drawText(thisLetter,mKey.x+dX + mKey.width-mainTextPaint.getTextSize(), midY, mainTextPaint);
					}
						break;
				case -45://downward left diagonal
					if (thisLetter.length() > 1) {  //if it is a word then draw it smaller to fit inside the key
						passCanvas.drawText(thisLetter, midX, midY - (wordTextPaint.getTextSize()/2), wordTextPaint);
					} else {
						passCanvas.drawText(thisLetter,mKey.x+dX + mainTextPaint.getTextSize(), mKey.y+dY+mKey.height-mainTextPaint.getTextSize()/2, diagLeft);
					}
						break;
				case 45://downward right diagonal
					if (thisLetter.length() > 1) {  //if it is a word then draw it smaller to fit inside the key
						passCanvas.drawText(thisLetter, midX, midY - (wordTextPaint.getTextSize()/2), wordTextPaint);
					} else {
						passCanvas.drawText(thisLetter,mKey.x+dX + mKey.width-mainTextPaint.getTextSize(), mKey.y+dY+mKey.height-mainTextPaint.getTextSize()/2, diagRight);
					}
						break;
				}//switch
								
			}//for loop which cycles through all possible values a key can activate
		}//for loop which cycles through all keys
	}//drawRegText
	
	private void drawKeyboardBackgroundFull(Canvas passCanvas){
			
		backgroundShapeLeft = new Path();
						
		backgroundShapeLeft.moveTo(_my.keyboard_keys.get(0).x, _my.keyboard_keys.get(0).y);
		backgroundShapeLeft.rLineTo(exampleKey.width*4 + _my.keyboard_keys.get(4).width, 0);
		backgroundShapeLeft.lineTo(_my.keyboard_keys.get(24).x+_my.keyboard_keys.get(24).width, 
				_my.keyboard_keys.get(24).y);
		backgroundShapeLeft.rLineTo(0, _my.keyboard_keys.get(24).height);
		backgroundShapeLeft.rLineTo(-exampleKey.width*4 - _my.keyboard_keys.get(24).width, 0);
		backgroundShapeLeft.lineTo(_my.keyboard_keys.get(0).x, _my.keyboard_keys.get(0).y + _my.keyboard_keys.get(0).height);
		backgroundShapeLeft.lineTo(_my.keyboard_keys.get(0).x, _my.keyboard_keys.get(0).y);
		backgroundShapeLeft.offset(dX , dY);
		
		passCanvas.drawPath(backgroundShapeLeft, keyboardBackgroundPaint);
		
		backgroundShapeRight = new Path();
		
		backgroundShapeRight.moveTo(_my.keyboard_keys.get(5).x, _my.keyboard_keys.get(5).y);
		backgroundShapeRight.rLineTo(exampleKey.width*4 + _my.keyboard_keys.get(5).width, 0);
		backgroundShapeRight.lineTo(_my.keyboard_keys.get(29).x+_my.keyboard_keys.get(29).width, 
				_my.keyboard_keys.get(29).y);
		backgroundShapeRight.rLineTo(0, _my.keyboard_keys.get(29).height);
		backgroundShapeRight.rLineTo(-exampleKey.width*4 - _my.keyboard_keys.get(25).width, 0);
		backgroundShapeRight.lineTo(_my.keyboard_keys.get(5).x, _my.keyboard_keys.get(5).y + _my.keyboard_keys.get(5).height);		
		backgroundShapeRight.lineTo(_my.keyboard_keys.get(5).x, _my.keyboard_keys.get(5).y);
		backgroundShapeRight.offset(dX , dY);
		
		passCanvas.drawPath(backgroundShapeRight, keyboardBackgroundPaint);
	}//drawKeyboardBackgroundFull
	
	private void drawKeyShapes(Canvas passCanvas){
		
		RectF myRectangle;
		RectF shadowRect;
		
		
		Key mKey;
		for(int i =0; i<_my.keyboard_keys.size();i++){
			mKey = _my.keyboard_keys.get(i);
			myRectangle = new RectF(mKey.x + keyPadding +dX,
					mKey.y + keyPadding +dY,
					mKey.x+mKey.width -keyPadding + dX,
					mKey.y+mKey.height - keyPadding + dY);
			
			shadowRect = new RectF(myRectangle);
			shadowRect.bottom += shadowBottom;
			shadowRect.right += shadowRight;
			shadowRect.top -= shadowTop;
			shadowRect.left -= shadowLeft;
			passCanvas.drawRoundRect(shadowRect, keyCornerRadius, keyCornerRadius, shadowPaint);
			keyPaint.setShader(new LinearGradient(
				0, mKey.y, 
				0, mKey.y+mKey.height, 
				Color.argb(255, 162, 162,178 ), 
				Color.argb(255,90,90,100), 
				Shader.TileMode.MIRROR));
			passCanvas.drawRoundRect(myRectangle, keyCornerRadius, keyCornerRadius, keyPaint);
			
			//light line on top and dark line on bottom of key gives more realistic effect
			passCanvas.drawLine(mKey.x + keyCornerRadius +dX +keyPadding, 
					mKey.y +dY + keyPadding, 
					mKey.x+mKey.width - keyCornerRadius - keyPadding +dX, 
					mKey.y +dY +keyPadding, 
					keyPaint2);
			passCanvas.drawLine(mKey.x + keyCornerRadius +dX +keyPadding, 
					mKey.y +mKey.height-keyPadding +dY, 
					mKey.x+mKey.width - keyCornerRadius - keyPadding +dX, 
					mKey.y +dY +mKey.height - keyPadding, 
					keyPaint3);
			
		}//for
		
	}//drawKeyShapes

}
