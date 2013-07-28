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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class HIITMain extends Activity {
	// keys for the intent we send to Run
	public static final String M_WORK = "org.jfet.batsHIIT.M_WORK";
	public static final String M_BREAK = "org.jfet.batsHIIT.M_BREAK";
	public static final String M_REST = "org.jfet.batsHIIT.M_REST";
	public static final String M_INTV = "org.jfet.batsHIIT.M_INTV";
	public static final String M_BLOCK = "org.jfet.batsHIIT.M_BLOCK";
	private EditText eWork;
	private EditText eBreak;
	private EditText eRest;
	private EditText eIntv;
	private EditText eBlock;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		// always required first
		setupView();							// set up the view
		setVolumeControlStream(AudioManager.STREAM_MUSIC);	// volume changes "music"
	}
	
	// set up the view using the hiitmain XML file, then
	// save off the view IDs for the relevant fields
	private void setupView() {
		setContentView(R.layout.activity_hiitmain);
		eWork = (EditText) findViewById(R.id.edit_work);
		eBreak = (EditText) findViewById(R.id.edit_break);
		eRest = (EditText) findViewById(R.id.edit_rest);
		eIntv = (EditText) findViewById(R.id.edit_intv);
		eBlock = (EditText) findViewById(R.id.edit_block);
		restoreSettings(getString(R.string.lastWorkout));
		populateLoadSpinner();
	}
	
	private void populateLoadSpinner() {
		// retrieve the preferences, and get all the keys
    	final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
    	final String[] prefsNames = prefs.getAll().keySet().toArray(new String[0]);

    	if (prefsNames.length > 0) {
    		// trim off the prefix from the keys
    		final int trimLength = canonicalizeSettingsName("").length();
    		for (int i=0; i<prefsNames.length; i++) {
    			if (prefsNames[i].length() > trimLength)
    				prefsNames[i] = prefsNames[i].substring(trimLength);
    		}
    		final ArrayAdapter<String> pNAdapter = 
    				new ArrayAdapter<String> (this,android.R.layout.simple_spinner_dropdown_item,prefsNames);
    		final Spinner pSpin = (Spinner) findViewById(R.id.spin_lName);
    		pSpin.setAdapter(pNAdapter);
    	}
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
    	// catch exceptions in converting user input; if anything fails, just reset the view
    	int i_work, i_break, i_rest, i_intv, i_block;
    	try { 
    		i_work = Integer.parseInt(eWork.getText().toString());
    		i_break = Integer.parseInt(eBreak.getText().toString());
    		i_rest = Integer.parseInt(eRest.getText().toString());
    		i_intv = Integer.parseInt(eIntv.getText().toString());
    		i_block = Integer.parseInt(eBlock.getText().toString());
    	} catch (NumberFormatException ex) {
    		setupView();
    		return;
    	}

    	// build up the intent
    	final Intent intent = new Intent (this, HIITRun.class);
   		intent.putExtra(M_WORK,i_work);
    	intent.putExtra(M_BREAK,i_break);
    	intent.putExtra(M_REST,i_rest);
    	intent.putExtra(M_INTV,i_intv);
    	intent.putExtra(M_BLOCK,i_block);

    	// if we got here, these settings are reasonably sensible
    	saveSettings(getString(R.string.lastWorkout));
    	startActivity(intent);
    }
    
    private String canonicalizeSettingsName(final String sName) {
    	return ("org.jfet.batsHIIT." + sName);
    }
    
	private void saveSettings(final String sName) {
		// grab the appropriate field contents
		final String setString = String.format("%s:%s:%s:%s:%s"
											  ,eWork.getText().toString()
											  ,eBreak.getText().toString()
											  ,eRest.getText().toString()
											  ,eIntv.getText().toString()
											  ,eBlock.getText().toString()
											  );
		
		// shared settings - get the settings, then an editor for them
    	final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
    	final SharedPreferences.Editor pEdit = prefs.edit();
    	
    	// insert last settings
    	pEdit.putString(canonicalizeSettingsName(sName), setString);
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
		final String setString = prefs.getString(canonicalizeSettingsName(sName), dfltSettings);

		// parse it and dump the strings into the EditText boxes
		final Scanner setScan = new Scanner(setString);
		// yes yes yes now we have two problems
		setScan.useDelimiter(Pattern.compile(":"));
		eWork.setText(setScan.next());
		eBreak.setText(setScan.next());
		eRest.setText(setScan.next());
		eIntv.setText(setScan.next());
		eBlock.setText(setScan.next());
	}
	
	public void saveButton (View view) {
		final String sName = ((EditText) findViewById(R.id.edit_sName)).getText().toString();
		if (sName.length() > 0)
			saveSettings(sName);
		populateLoadSpinner();
	}
	
	public void loadButton (View view) {
		// spinner view
		final Spinner pSpin = (Spinner) findViewById(R.id.spin_lName);
		final String sName = pSpin.getSelectedItem().toString();
		((EditText) findViewById(R.id.edit_sName)).setText(sName);
		restoreSettings(sName);
	}
	
	public void delButton (View view) {
		// spinner view
    	final Spinner pSpin = (Spinner) findViewById(R.id.spin_lName);

		// shared settings - get the settings, then an editor for them
    	final SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
    	final SharedPreferences.Editor pEdit = prefs.edit();

    	// remove
    	pEdit.remove(canonicalizeSettingsName(pSpin.getSelectedItem().toString()));
    	pEdit.commit();
    	populateLoadSpinner();
	}
}