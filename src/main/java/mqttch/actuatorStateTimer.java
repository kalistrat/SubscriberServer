package mqttch;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by kalistrat on 27.10.2017.
 */
public class actuatorStateTimer {
    Date startedTime;
    Integer commitedTime;
    ScheduledExecutorService ses;
    Runnable stepper;

    public actuatorStateTimer(){
        startedTime = new java.util.Date();
        commitedTime = 0;
        ses = Executors.newScheduledThreadPool(1);
        stepper = new Runnable() {
            public void run() {
                commitedTime = commitedTime + 1;
            }
        };

    }

    public int getCommitedTime(){
        return commitedTime;
    }

    public Date getCurrentDate(){
        Calendar cal = Calendar.getInstance();
        cal.setTime(startedTime);
        cal.add(Calendar.SECOND, commitedTime);
        return cal.getTime();
    }

    public void stopExecution(){
        ses.shutdown();
        commitedTime = 0;
        //ses = null;
    }

    public void startExecution(){
        //commitedTime = 0;
        startedTime = new java.util.Date();
        ses = Executors.newScheduledThreadPool(1);
        ses.scheduleAtFixedRate(stepper, 0, 1, TimeUnit.SECONDS);
    }
}
