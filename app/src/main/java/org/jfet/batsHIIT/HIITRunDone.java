package org.jfet.batsHIIT;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class HIITRunDone extends AppCompatActivity {
    // create an intent for sharing our workout
    // passed to the ShareActionProvider
    private Intent getHIITIntent () {
        final Intent itt = new Intent(android.content.Intent.ACTION_SEND);
        itt.setType("text/plain");
        itt.putExtra(Intent.EXTRA_SUBJECT,R.string.share_subject);
        itt.putExtra(Intent.EXTRA_TEXT,String.format(
                "%s %s %s",
                getString(R.string.share_contents1),
                getIntent().getStringExtra("RESULT"),
                getString(R.string.share_contents2)));
        return itt;
    }
    // menu stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the premade menu
        getMenuInflater().inflate(R.menu.hiitrun, menu);
        // now connect the ShareActionProvider with our sharing intent
        final MenuItem mItem = (MenuItem) menu.findItem(R.id.share_menu);
        final ShareActionProvider sActPro = (ShareActionProvider) MenuItemCompat.getActionProvider(mItem);
        sActPro.setShareIntent(getHIITIntent());
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // change UI to WORK
        setContentView(R.layout.activity_hiitrun_done);
        setSupportActionBar((Toolbar) findViewById(R.id.rundone_toolbar));
        // setup the action bar with a back button (compat version)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((TextView) findViewById(R.id.hiit_time_done)).setText(getIntent().getStringExtra("RESULT"));
    }
}
