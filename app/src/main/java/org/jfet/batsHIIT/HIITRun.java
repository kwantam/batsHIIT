package org.jfet.batsHIIT;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HIITRun extends AppCompatActivity {
    // sound manager
    private SoundManager sndMan;
    // resource reference and SoundManager IDs for the sounds we'll use
    private final static int beep1Snd = R.raw.beep1;
    private final static int beep2Snd = R.raw.beep2;
    private final static int chirpSnd = R.raw.chirp;
    private int beep1;
    private int beep2;
    private int chirp;
    private LinearLayout lLayout;
    private TextView nSeconds;
    private TextView nIntervals;
    private TextView nBlocks;
    private Handler uiHandler;
    private HIITRunner hiitRunner;
    private int workSeconds;
    private int breakSeconds;
    private int restSeconds;
    private int intervalCount;
    private int blockCount;
    private WakeLock scrUnLock;
    private boolean hiitDone = false;
    
    private static enum HIITState { WORK, BREAK, REST };
    
    private class HIITRunner extends Thread {
        // volatile because the UI thread signals us via this flag
        private volatile boolean runLoop;
        private volatile boolean continueLoop;
        
        // signal from UI thread to shut down
        public void stopRunner() {
            continueLoop = false;
        }
        
        public void pauseRunner() {
            runLoop = false;
        }
        
        public void resumeRunner() {
            runLoop = true;
        }
        
        private void hangThread() {
            synchronized (HIITRun.this) {
                while(!runLoop)
                    try { HIITRun.this.wait(); } catch (InterruptedException ex) { continue; }
            }
        }
        
        @Override
        public void run() {
            // don't run the loop until we're told to start!
               runLoop = true;
               continueLoop = true;
               // initialize state machine
            int timeRemaining = HIITRun.this.restSeconds;
            int blockRemaining = HIITRun.this.blockCount;
            int intvRemaining;
            HIITState hiitState = HIITRun.HIITState.REST;
            // variables for delay-locked loop
            final long sleepTarget = 1000000000L;
            long sleepDelay = sleepTarget;
            long wakeupError;
            long lastWakeup;
            long thisWakeup;


               HIITRun.this.sndMan.pauseUntilLoaded(HIITRun.this.beep1 +
                                                    HIITRun.this.beep2 +
                                                 HIITRun.this.chirp);            
               
               // start out with 5 warning beeps, then the workout begins
               for (intvRemaining = 5; (intvRemaining > 0); intvRemaining--) {
                   // every iteration, check whether we should be pausing or dying
                   if (!runLoop) hangThread();
                   if (!continueLoop) return;

                   try {
                       HIITRun.this.sndMan.playSound(HIITRun.this.beep1);
                       Thread.sleep(1000);
                   } catch (InterruptedException ex) { continue; }
               }

               lastWakeup = System.nanoTime() - sleepTarget;
            while (true) {
                // every iteration, check whether we should be pausing or dying
                if (!runLoop) {
                    hangThread();
                    // once we come back from a hung thread, make sure we don't blow up the delay-locked loop!
                    lastWakeup = System.nanoTime() - sleepTarget;
                }
                if (!continueLoop) return;

                thisWakeup = System.nanoTime();
                if (timeRemaining == 1) { // finished this subinterval
                    switch (hiitState) {
                    case WORK:
                    // WORK -> BREAK : update state, update time; no change to blocks or intvs
                        hiitState = HIITRun.HIITState.BREAK;
                        timeRemaining = HIITRun.this.breakSeconds;
                        HIITRun.this.sndMan.playSound(HIITRun.this.beep2);
                        HIITRun.this.uiHandler.obtainMessage(1).sendToTarget();
                        break;
                    
                    case BREAK:
                    // BREAK transition, either to WORK or to REST
                        if (intvRemaining == 0) {    // to REST
                            hiitState = HIITRun.HIITState.REST;
                            timeRemaining = HIITRun.this.restSeconds;
                            HIITRun.this.uiHandler.obtainMessage(2,0,blockRemaining).sendToTarget();
                        } else {                    // back to WORK
                            hiitState = HIITRun.HIITState.WORK;
                            timeRemaining = HIITRun.this.workSeconds;
                            intvRemaining--;
                            HIITRun.this.sndMan.playSound(HIITRun.this.beep1);
                            HIITRun.this.uiHandler.obtainMessage(0,intvRemaining,blockRemaining).sendToTarget();
                        }
                        break;
                    
                    case REST:
                    // REST transition, either to WORK or done
                        if (blockRemaining == 0) {    // all done!
                            HIITRun.this.uiHandler.obtainMessage(4).sendToTarget();
                            return;
                        } else {                    // back to WORK
                            hiitState = HIITRun.HIITState.WORK;
                            timeRemaining = HIITRun.this.workSeconds;
                            intvRemaining = HIITRun.this.intervalCount - 1;
                            blockRemaining--;
                            HIITRun.this.sndMan.playSound(HIITRun.this.beep1);
                            HIITRun.this.uiHandler.obtainMessage(0,intvRemaining,blockRemaining).sendToTarget();
                        }
                    }
                } else { // no transition; just update the timer display and possibly play the warning chirps
                    timeRemaining--;
                    HIITRun.this.uiHandler.obtainMessage(3,timeRemaining,0).sendToTarget();
                    if (timeRemaining < 5) HIITRun.this.sndMan.playSound(HIITRun.this.chirp);
                }
                
                /*
                // delay-locked loop
                // reference delay is 1 second (sleepTarget, in nanoseconds)
                // measure delay each cycle by looking at new value from System.nanoTime()
                // integrator:
                //   y[n] = y[n-1] + x[n]/ki
                //   Y = Y*z^-1 + X/ki
                //   Y/X = 1/ki/(1 - z^-1)
                // feedforward (zero)
                //   z[n] = x[n]/kz
                //   Z/X = 1/kz
                // total transfer function
                //   w[n] = y[n] + z[n]
                //   W/X = Y/X + Z/X
                //       = 1/ki/(1 - z^-1) + 1/kz
                //         = (1 + (ki*(1 - z^-1))/kz)/(ki*(1 - z^-1))
                //         = (z + (ki*(z - 1)/kz) / (ki*(z - 1))
                //         = ((ki+kz)*z/kz - ki/kz) / (ki*(z - 1))
                //         = ((ki+kz)*z - ki) / (ki*kz*(z -1))
                //         = ((ki+kz)/ki/kz - ki*z^-1/ki/kz) / (1 - z^-1)
                // kz needs to be somewhat greater than 1 for stability (need high frequency gain < 0dB)
                // ki wants to be bigger than kz, but not too much s.t. loop bandwidth is relatively high
                // kz = 3, ki = 5 should be stable and should give decent loop bandwidth
                // if we're willing to add another singularity (pole at higher frequency),
                // we could make kz < 1 and have very good transient response, e.g.,
                // responding to the additional delay that results from playing sounds
                // but we already do a pretty good job in this respect so probably it's good enough
                // it would be nice to have higher bandwidth to push down noise but we do a good job at
                // low frequency, viz., taking care of offset from our own code and slowly varying system load
                */
                wakeupError = sleepTarget - (thisWakeup - lastWakeup);        // error signal
                // log the error; how well are we doing?
                sleepDelay = sleepDelay + (wakeupError / 5);                // integrator
                wakeupError = sleepDelay + (wakeupError / 3);                // feedforward zero
                lastWakeup = thisWakeup;                                    // save most recent wakeup

                try { Thread.sleep(wakeupError / 1000000L); }    // sleep 1 second
                catch (InterruptedException ex) { continue; }
            }
        }
    };
    
    // UI handler for messages from the Runner thread
    // note: we're OK to suppress the HandlerLeak because we never send delayed messages
    // the leak will only happen until the last message is delivered, which in our case
    // should be more or less instantaneously
    @SuppressLint("HandlerLeak")
    private class HIITUIHandler extends Handler {
        public HIITUIHandler (Looper l) { super(l); }

        @Override
        public void handleMessage (Message m) {
            // if the message type is different than last time, update the view to the new one
            switch (m.what) {

            case 0:
                // change UI to WORK
                setContentView(R.layout.activity_hiitrun);
                lLayout = (LinearLayout) findViewById(R.id.hiitRunLayout);
                nSeconds = (TextView) findViewById(R.id.nSeconds);
                nIntervals = (TextView) findViewById(R.id.nIntervals);
                nBlocks = (TextView) findViewById(R.id.nBlocks);
                // update values
                lLayout.setBackgroundColor(Color.GREEN);
                nSeconds.setText(String.format("%d",workSeconds));
                nIntervals.setText(String.format("%d",m.arg1));
                nBlocks.setText(String.format("%d",m.arg2));
                break;

            case 1:
                // change UI to BREAK
                lLayout.setBackgroundColor(Color.YELLOW);
                nSeconds.setText(String.format("%d", breakSeconds));
                break;

            case 2:
                // change UI to REST
                setContentView(R.layout.activity_hiitrun_rest);
                lLayout = (LinearLayout) findViewById(R.id.hiitRunLayoutRest);
                nSeconds = (TextView) findViewById(R.id.nSecondsRest);
                nBlocks = (TextView) findViewById(R.id.nBlocksRest);
                // update values
                lLayout.setBackgroundColor(Color.RED);
                nSeconds.setText(String.format("%d",restSeconds));
                nBlocks.setText(String.format("%d",m.arg2));
                break;

            case 3:
                // update seconds only
                nSeconds.setText(String.format("%d",m.arg1));
                break;

            case 4:
                // change UI to done
                setContentView(R.layout.activity_hiitrun_done);
                lLayout = (LinearLayout) findViewById(R.id.hiitRunLayoutDone);
                nSeconds = (TextView) findViewById(R.id.hiit_time_done);
                hiitDone = true;
                supportInvalidateOptionsMenu();
                // update values
                int workoutTime = (1 + blockCount) * restSeconds + blockCount * intervalCount * (workSeconds + breakSeconds);
                lLayout.setBackgroundColor(Color.CYAN);
                nSeconds.setText(String.format("%d:%02d",workoutTime/60,workoutTime%60));
                // release screen lock, if held; we don't need this any more
                if (scrUnLock.isHeld()) scrUnLock.release();
                break;
            }
        }
    }

    // create the activity. In addition to standard activities
    // we create a soundpool and tell it to preload the sounds
    // don't warn about SCREEN_DIM_WAKE_LOCK, I know it's deprecated but it's preferable to keeping the screen at full brightness
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // setup the action bar with a back button (compat version)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // create the sound manager instance
        sndMan = new SoundManager(this);
        // load the sounds to initialize the sound manager
        beep1 = sndMan.loadSound(beep1Snd);
        beep2 = sndMan.loadSound(beep2Snd);
        chirp = sndMan.loadSound(chirpSnd);
        
        // set up the workout parameters
        Intent itt = getIntent();
        workSeconds = itt.getIntExtra(HIITMain.M_WORK, Integer.parseInt(getString(R.string.work_dflt)));
        breakSeconds = itt.getIntExtra(HIITMain.M_BREAK, Integer.parseInt(getString(R.string.break_dflt)));
        restSeconds = itt.getIntExtra(HIITMain.M_REST, Integer.parseInt(getString(R.string.rest_dflt)));
        intervalCount = itt.getIntExtra(HIITMain.M_INTV, Integer.parseInt(getString(R.string.intv_dflt)));
        blockCount = itt.getIntExtra(HIITMain.M_BLOCK, Integer.parseInt(getString(R.string.block_dflt)));

        // make sure the screen stays on through the workout
        // this works, but always keeps the screen at 100% brightness
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        scrUnLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
                                  |PowerManager.ON_AFTER_RELEASE
                                  ,"org.jfet.batsHIIT.HIITRun.scrUnLock"
                                  );

        // create a handler for hiitRunner to send us UI updates
        uiHandler = new HIITUIHandler(Looper.getMainLooper());
        // immediately use the handler to set up the view
        uiHandler.obtainMessage(2,0,blockCount).sendToTarget();

        hiitRunner = new HIITRunner();
        // when we create this activity, also start the Runner thread
        // never call start() twice on the same thread!
        hiitRunner.start();
    }
    
    // menu stuff
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (hiitDone) {
            // inflate the premade menu
            getMenuInflater().inflate(R.menu.hiitrun, menu);
            // now connect the ShareActionProvider with our sharing intent
            final MenuItem mItem = (MenuItem) menu.findItem(R.id.share_menu);
            final ShareActionProvider sActPro = (ShareActionProvider) MenuItemCompat.getActionProvider(mItem);
            sActPro.setShareIntent(getHIITIntent());
            return true;
        } else {
            return false;
        }
    }
    
    // onResume is what happens *just* before we start running the thread
    @Override
    protected void onResume() { 
        super.onResume();
        // just before we start executing, make sure the screen never goes to sleep
        if (!scrUnLock.isHeld() & hiitRunner.isAlive()) scrUnLock.acquire();
        
        // tell the Runner thread to continue
        // no harm if it's already running and we do this
        hiitRunner.resumeRunner();
        synchronized (this) { notify(); }    // break it out of its wait();
    }
    
    // onPause is always called when the activity is undisplayed
    // stop the counter thread here
    @Override
    protected void onPause() {
        super.onPause();
        hiitRunner.pauseRunner();    // tell it to pause
        hiitRunner.interrupt();        // cancel the current timeout, if any
        // if there isn't a timeout, the exception will be raised without harm inside hangThread()

        // just after we stop executing, release the screen lock
        if (scrUnLock.isHeld()) scrUnLock.release();
    }
    
    @Override protected void onDestroy() {
        super.onDestroy();

        // if we're here, that means we've already paused the Runner thread
        // thus, we set the flag for it to die and then pull it out of wait() so it returns
        hiitRunner.resumeRunner();    // should not be necessary, but make sure it does not hang again
        hiitRunner.stopRunner();    // next time through any loop it will see this and kill itself
        synchronized (this) { notify(); }    // wake it up from its wait() so that it kills itself
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
            case android.R.id.home:
                    // This ID represents the Home or Up button.
                    NavUtils.navigateUpFromSameTask(this);
                    return true;
            }
            return super.onOptionsItemSelected(item);
    }
    
    // create an intent for sharing our workout
    // passed to the ShareActionProvider
    private Intent getHIITIntent () {
        final Intent itt = new Intent(android.content.Intent.ACTION_SEND);
        itt.setType("text/plain");
        itt.putExtra(Intent.EXTRA_SUBJECT,R.string.share_subject);
        itt.putExtra(Intent.EXTRA_TEXT,String.format(
                "%s %s %s",
                getString(R.string.share_contents1),
                nSeconds.getText().toString(),
                getString(R.string.share_contents2)));
        return itt;
    }
}
