package com.coogeetech.morningstar;



import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.Keyboard.Row;

public class MyFullKeyboard extends Keyboard{

	public MyFullKeyboard(Context context, int xmlLayoutResId, int modeId,
			int width, int height) {
		super(context, xmlLayoutResId, modeId, width, height);
		// TODO Auto-generated constructor stub
	}
	
	 @Override
	    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, 
	            XmlResourceParser parser) {
	        Key key = new MyKey(res, parent, x, y, parser);
	        if (key.codes[0] == 10) {
	            Key mEnterKey = key;
	        } else if (key.codes[0] == ' ') {
	            Key mSpaceKey = key;
	        }
	        return key;
	    }
	
	static class MyKey extends Keyboard.Key{

		public MyKey(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
			super(res, parent, x, y, parser);
			// TODO Auto-generated constructor stub
		}
		
	}

}
