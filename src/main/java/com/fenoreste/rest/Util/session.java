/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Util;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author wilmer
 */
public class session implements HttpSessionListener{
    public void sessionCreated(HttpSessionEvent event){ 
        event.getSession().setMaxInactiveInterval(15*60); 
    }
        public void sessionDestroyed(HttpSessionEvent event){}
    
}
