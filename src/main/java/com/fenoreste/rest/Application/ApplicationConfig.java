/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Application;

import com.fenoreste.rest.Util.TimerBeepClock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.Application;

/**
 *
 * @author wilmer
 */
@javax.ws.rs.ApplicationPath("services")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        hora();
        return resources;
    }

    
     public void hora(){
        ScheduledExecutorService scheduler=Executors.newSingleThreadScheduledExecutor();
        Runnable task = new TimerBeepClock();
        int initialDelay = 1;
        int periodicDelay = 1;
        scheduler.scheduleAtFixedRate(task, initialDelay, periodicDelay,TimeUnit.MINUTES);
        
        
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider.class);
        resources.add(com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider.class);
        resources.add(com.fenoreste.rest.RESTservices.AccountsResources.class);
        resources.add(com.fenoreste.rest.RESTservices.CustomerResources.class);
        resources.add(com.fenoreste.rest.RESTservices.LoanResources.class);
        resources.add(com.fenoreste.rest.RESTservices.ProductsResources.class);
        resources.add(com.fenoreste.rest.RESTservices.TercerosResources.class);
        resources.add(com.fenoreste.rest.RESTservices.TestResources.class);
        resources.add(com.fenoreste.rest.RESTservices.TransactionResources.class);
    }
    
}
