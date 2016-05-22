package com.softcrafted.disambiguation;

import android.content.Context;
import android.widget.EditText;

public class OutputBuffer {
	
	EditText myOutput; //the GUI object which will display our text to user

	public OutputBuffer(Context context, EditText passEditText){
		myOutput = passEditText;
	}//constructor
	
	public void pushText(String passInput){
		
		// algorithm here
		
		//output results to screen
		appendToScreen(passInput);
		

	}
	
	private void appendToScreen(String passOutput){
		myOutput.append(passOutput);
	}
}
