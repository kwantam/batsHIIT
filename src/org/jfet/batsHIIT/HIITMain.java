package org.jfet.batsHIIT;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class HIITMain extends Activity {
	public static final String M_WORK = "org.jfet.batsHIIT.M_WORK";
	public static final String M_BREAK = "org.jfet.batsHIIT.M_BREAK";
	public static final String M_REST = "org.jfet.batsHIIT.M_REST";
	public static final String M_INTV = "org.jfet.batsHIIT.M_INTV";
	public static final String M_BLOCK = "org.jfet.batsHIIT.M_BLOCK";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hiitmain);
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hiitmain, menu);
		return true;
	}
*/
	
    public void hiitRun (View view) {
    	Intent intent = new Intent (this, HIITRun.class);

    	try {	// catch exceptions in converting user input; if anything fails, just reset the view
    		intent.putExtra(HIITMain.M_WORK,Integer.parseInt(((EditText) findViewById(R.id.edit_work)).getText().toString()));
    		intent.putExtra(HIITMain.M_BREAK,Integer.parseInt(((EditText) findViewById(R.id.edit_break)).getText().toString()));
    		intent.putExtra(HIITMain.M_REST,Integer.parseInt(((EditText) findViewById(R.id.edit_rest)).getText().toString()));
    		intent.putExtra(HIITMain.M_INTV,Integer.parseInt(((EditText) findViewById(R.id.edit_intv)).getText().toString()));
    		intent.putExtra(HIITMain.M_BLOCK,Integer.parseInt(((EditText) findViewById(R.id.edit_block)).getText().toString()));
    	}
    	catch (NumberFormatException ex) {
    		setContentView(R.layout.activity_hiitmain);
    		return;
    	}

    	startActivity(intent);
    }
}
