/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Util;

import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author wilmer
 */
public class TimerBeepClock  implements Runnable {

    public void run() {
        Toolkit.getDefaultToolkit().beep();
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("HH:mm a");
        String hora=dateFormatLocal.format(new Date());
        System.out.println("hora:"+hora);
        if(hora.replace(" ","").equals("10:46:50AM")){
          
        }       
    }

    /*public static void main(String[] args) {
        ScheduledExecutorService scheduler
                = Executors.newSingleThreadScheduledExecutor();

        Runnable task = new TimerBeepClock();
        int initialDelay = 1;
        int periodicDelay = 1;
        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay,TimeUnit.SECONDS);
    }*/
}
