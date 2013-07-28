package org.jfet.batsHIIT;

import java.util.Scanner;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class HIITMain extends Activity {
	// keys for the intent we send to Run
	public static final String M_WORK = "org.jfet.batsHIIT.M_WORK";
	public static final String M_BREAK = "org.jfet.batsHIIT.M_BREAK";
	public static final String M_REST = "org.jfet.batsHIIT.M_REST";
	public static final String M_INTV = "org.jfet.batsHIIT.M_INTV";
	public static final String M_BLOCK = "org.jfet.batsHIIT.M_BLOCK";
	// keys for the saved configurations
	public static final String S_LAST = "org.jfet.batsHIIT.S_LAST";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hiitmain);
		restoreSettings(S_LAST);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
    	final Intent intent = new Intent (this, HIITRun.class);
    	int i_work, i_break, i_rest, i_intv, i_block;

    	try { // catch exceptions in converting user input; if anything fails, just reset the view
    		i_work = Integer.parseInt(((EditText) findViewById(R.id.edit_work)).getText().toString());
    		i_break = Integer.parseInt(((EditText) findViewById(R.id.edit_break)).getText().toString());
    		i_rest = Integer.parseInt(((EditText) findViewById(R.id.edit_rest)).getText().toString());
    		i_intv = Integer.parseInt(((EditText) findViewById(R.id.edit_intv)).getText().toString());
    		i_block = Integer.parseInt(((EditText) findViewById(R.id.edit_block)).getText().toString());
    	} catch (NumberFormatException ex) {
    		setContentView(R.layout.activity_hiitmain);
    		return;
    	}

   		intent.putExtra(M_WORK,i_work);
    	intent.putExtra(M_BREAK,i_break);
    	intent.putExtra(M_REST,i_rest);
    	intent.putExtra(M_INTV,i_intv);
    	intent.putExtra(M_BLOCK,i_block);

    	saveLastSettings(i_work,i_break,i_rest,i_intv,i_block);

    	startActivity(intent);
    }
    
	private void saveLastSettings(int w, int b, int r, int i, int l) {
    	final String lastSettings = String.format("%d:%d:%d:%d:%d",w,b,r,i,l);
    	
    	// shared settings - get the settings, then an editor for them
    	final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
    	final SharedPreferences.Editor pEdit = prefs.edit();
    	
    	// insert last settings
    	pEdit.putString(S_LAST, lastSettings);
    	pEdit.commit();
    }
	
	private void restoreSettings(String sName) {
		final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		final String dfltSettings = String.format("%s:%s:%s:%s:%s"
												 ,getString(R.string.work_dflt)
												 ,getString(R.string.break_dflt)
												 ,getString(R.string.rest_dflt)
												 ,getString(R.string.intv_dflt)
												 ,getString(R.string.block_dflt)
												 );
		// retrieve the requested
		final String setString = prefs.getString(sName, dfltSettings);

		// parse it and dump the strings into the EditText boxes
		final Scanner setScan = new Scanner(setString);
		setScan.useDelimiter(Pattern.compile(":"));
		((EditText) findViewById(R.id.edit_work)).setText(setScan.next());
		((EditText) findViewById(R.id.edit_break)).setText(setScan.next());
		((EditText) findViewById(R.id.edit_rest)).setText(setScan.next());
		((EditText) findViewById(R.id.edit_intv)).setText(setScan.next());
		((EditText) findViewById(R.id.edit_block)).setText(setScan.next());
	}
}