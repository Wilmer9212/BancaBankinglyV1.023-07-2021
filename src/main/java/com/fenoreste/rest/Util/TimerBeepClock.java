/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Util;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileFilter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author wilmer
 */
public class TimerBeepClock implements Runnable {

    public void run() {
        Toolkit.getDefaultToolkit().beep();
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("HH:mm:ss a");
        String hora = dateFormatLocal.format(new Date());
        //eliminamos todos los PDF a las 4:00AM
        if (hora.replace(" ", "").equals("04:00:00AM")) {
            eliminarPorExtension(ruta(), "pdf");
            eliminarPorExtension(ruta(), "html");
            eliminarPorExtension(ruta(), "txt");
        }        
        if (hora.replace(" ", "").equals("13:29:00PM")) {
            actualizarFechaServidorBD();
        }
    }

    //Metodo para eliminar todos los pdf 
    public static void eliminarPorExtension(String path, final String extension) {
        File[] archivos = new File(path).listFiles(new FileFilter() {
            public boolean accept(File archivo) {
                if (archivo.isFile()) {
                    return archivo.getName().endsWith('.' + extension);
                }
                return false;
            }
        });
        for (File archivo : archivos) {
            archivo.delete();
        }
    }
    
    //Para las pruebas
    public void actualizarFechaServidorBD(){
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String fechaActual = dateFormatLocal.format(new Date());
        Timestamp timestamp = Timestamp.valueOf(fechaActual);
        EntityManagerFactory emf=AbstractFacade.conexion();
        EntityManager em=emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery("UPDATE origenes SET fechatrabajo =:fecha").setParameter("fecha",timestamp).executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            emf.close();
            em.close();
            System.out.println("Error al actualizar hora en servidor:"+e.getMessage());
        }finally{
            emf.close();
            em.close();
        }
    }

    //Parao obtener la ruta del servidor
    public static String ruta() {
        String home = System.getProperty("user.home");
        String separador = System.getProperty("file.separator");
        String actualRuta = home + separador + "Banca" + separador;
        return actualRuta;
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
