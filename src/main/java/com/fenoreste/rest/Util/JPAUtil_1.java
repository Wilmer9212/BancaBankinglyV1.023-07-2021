package com.fenoreste.rest.Util;

import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author Wilmer
 */
public class JPAUtil_1{
      String  usuario = "saicoop";
      String pass = "slufpana?";
      String PU = "conexion";
    
    public EntityManager getEntityManager(String ip, String bd) {
       System.out.println("ip.-" + ip + "\nbd.-" + bd);
        try {
            Properties properties = new Properties();
            
            properties.put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
            properties.put("javax.persistence.jdbc.url", "jdbc:postgresql://" + ip + ":5432/" + bd);
            properties.put("javax.persistence.jdbc.user", usuario);
            properties.put("javax.persistence.jdbc.password", pass);
            /*properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");
            properties.put("hibernate.connection.url", "jdbc:postgresql://" + ip + ":5432/" + bd);
            properties.put("hibernate.connection.username", usuario);
            properties.put("hibernate.connection.password", pass);*/
            return Persistence.createEntityManagerFactory(PU, properties).createEntityManager();
        } catch (Throwable e) {
            System.err.println("Error en createEntityManager de JpaUtil tipo " + e.getMessage());
            return null;
        }
    }

}