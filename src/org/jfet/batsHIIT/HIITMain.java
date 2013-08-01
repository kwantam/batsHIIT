package org.jfet.batsHIIT;

import java.util.Scanner;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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
    private HIITInputWatcher hiWatcher;
    private int iWork;
    private int iBreak;
    private int iRest;
    private int iIntv;
    private int iBlock;
    
    private class HIITInputWatcher implements TextWatcher {
        // need a reference to the parent activity
        // so we can tell it to recompute the workout time
        private final HIITMain parentActivity;
        public HIITInputWatcher (HIITMain p) { parentActivity = p; }
        
        @Override
        public void afterTextChanged (Editable s) { parentActivity.recomputeHIITTime(); }
        
        // have to implement these for a full TextWatcher implementation, but we don't care about them
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { return; }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { return; }
        
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        // always required first

        // set up the view, populate the elements
        setContentView(R.layout.activity_hiitmain);
        eWork = (EditText) findViewById(R.id.edit_work);
        eBreak = (EditText) findViewById(R.id.edit_break);
        eRest = (EditText) findViewById(R.id.edit_rest);
        eIntv = (EditText) findViewById(R.id.edit_intv);
        eBlock = (EditText) findViewById(R.id.edit_block);
        hiWatcher = new HIITInputWatcher(this);
        setupListener(true);

        restoreSettings(getString(R.string.lastWorkout));

        populateLoadSpinner();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);    // volume changes "music"
    }
    
    private void setupListener (final Boolean set) {
        if (set) {
            eWork.addTextChangedListener(hiWatcher);
            eBreak.addTextChangedListener(hiWatcher);
            eRest.addTextChangedListener(hiWatcher);
            eBlock.addTextChangedListener(hiWatcher);
            eIntv.addTextChangedListener(hiWatcher);
        } else {
            eWork.removeTextChangedListener(hiWatcher);
            eBreak.removeTextChangedListener(hiWatcher);
            eRest.removeTextChangedListener(hiWatcher);
            eBlock.removeTextChangedListener(hiWatcher);
            eIntv.removeTextChangedListener(hiWatcher);
        }
    }
    
    private void recomputeHIITTime() {
        // catch exceptions in converting user input; if anything fails, reset to last known good settings
        try { 
            convertSettings();
        } catch (NumberFormatException ex) {
            return;
        }
        
        final int totalTime = (1 + iBlock)*iRest + iBlock*iIntv*(iWork+iBreak);
        
        ((TextView) findViewById(R.id.hiit_time)).setText(
                String.format("%d:%02d"
                             ,totalTime / 60
                             ,totalTime % 60));
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

    private void convertSettings() throws NumberFormatException {
        try {    // if this breaks...
            // (note) we do not allow values less than 1 for these!
            // silently set the converted representation to 1
            iWork = Math.max(Integer.parseInt(eWork.getText().toString()),1);
            iBreak = Math.max(Integer.parseInt(eBreak.getText().toString()),1);
            iRest = Math.max(Integer.parseInt(eRest.getText().toString()),1);
            iIntv = Math.max(Integer.parseInt(eIntv.getText().toString()),1);
            iBlock = Math.max(Integer.parseInt(eBlock.getText().toString()),1);
        } catch (NumberFormatException ex) {
            // ...pass the buck
            throw ex;
        }
    }
    
    public void hiitRun (View view) {
        // catch exceptions in converting user input; if anything fails, reset to last known good settings
        try { 
            convertSettings();
        } catch (NumberFormatException ex) {
            restoreSettings(getString(R.string.lastWorkout));
            return;
        }

        // build up the intent
        final Intent intent = new Intent (this, HIITRun.class);
           intent.putExtra(M_WORK,iWork);
        intent.putExtra(M_BREAK,iBreak);
        intent.putExtra(M_REST,iRest);
        intent.putExtra(M_INTV,iIntv);
        intent.putExtra(M_BLOCK,iBlock);

        // if we got here, these settings are reasonably sensible
        saveSettings(getString(R.string.lastWorkout));
        startActivity(intent);
    }
    
    // saving settings
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
    
    private void restoreSettings(final String sName) {
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
        
        // make sure we don't do a bunch of unnecessary callbacks when we change these
        setupListener(false);
        eWork.setText(setScan.next());
        eBreak.setText(setScan.next());
        eRest.setText(setScan.next());
        eIntv.setText(setScan.next());
        eBlock.setText(setScan.next());
        recomputeHIITTime();
        setupListener(true);
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

    // menu stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hiitmain, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem mi) {
        super.onOptionsItemSelected(mi);    // call this first; it will fall through to us
        showHelpDialog();
        return true;
    }
    
    private void showHelpDialog() {
        final AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        final DialogInterface.OnClickListener ocl = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dlg, int w) {
                return;
            }
        };
        dlg.setTitle(R.string.action_help);
        dlg.setMessage(R.string.help_message);
        dlg.setPositiveButton(R.string.action_help_ok,ocl);
        dlg.show();
        return;
    }
}
