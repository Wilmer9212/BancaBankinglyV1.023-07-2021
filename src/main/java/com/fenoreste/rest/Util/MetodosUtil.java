/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Util;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.InterfaceService.OrigenesServiceLocal;
import com.fenoreste.rest.InterfaceService.TablasServiceLocal;
import com.fenoreste.rest.entidades.Origenes;
import com.fenoreste.rest.entidades.TablasPK;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author wilmer
 */
public class MetodosUtil {
   
    TablasServiceLocal tablasService;
    saiFunciones saiFunciones;
    OrigenesServiceLocal origenesService;
    
    public boolean ServicioActivo(String idTabla) {
        try {
            LocalTime h0 = LocalTime.parse("00:00");
            LocalTime h24 = LocalTime.parse("23:59");
            SimpleDateFormat formatter = new SimpleDateFormat("HH:mm");
            // Saco la hora de la base de datos
            TablasPK tablasPK = new TablasPK(idTabla, "hora_cierre");
            TablasDTO tabActivo = tablasService.buscaTabla(tablasPK);
            if (tabActivo.getDato1() != null) {
                int horaBloqueo = Integer.valueOf(tabActivo.getDato3());
                String hora1 = tabActivo.getDato1();

                // Convierte el string de la hora en date
                Calendar cal = Calendar.getInstance();
                Date date1 = formatter.parse(tabActivo.getDato1());
                // Le suma dato3 las hora a la hora que se asigno en la base
                cal.setTime(date1);
                cal.set(Calendar.MINUTE, cal.get(Calendar.MINUTE) + (horaBloqueo * 60));

                // Obtenemos en string las horas de inicio, fin y la de la base de datos
                String hora2 = formatter.format(cal.getTime());
                String horaBase = formatter.format(saiFunciones.saiFechaDB("24"));
                // Se convierten las horas de string a localtime (hh:mm)
                DateTimeFormatter parseFormat = new DateTimeFormatterBuilder().appendPattern("HH:mm").toFormatter();
                LocalTime localTime1 = LocalTime.parse(hora1, parseFormat);
                LocalTime localTime2 = LocalTime.parse(hora2, parseFormat);
                LocalTime localTimeBase = LocalTime.parse(horaBase, parseFormat);
                //prometeo: si mi hora esta despues de la hora de cierre y antes de las 12 de la noche ó 
                //si mi hora esta despues de las 0 horas y antes de la hora de inicio de actividades
                //entonces retorna false( no puede hacer peticiones ).
                //if ((localTimeBase.isAfter(00:00) && localTimeBase.isBefore(08:00)) || (localTimeBase.isAfter(22:00) && localTimeBase.isBefore(23:59))) {
                
                System.out.println("hora que se suma el dato3 localTime2: " + localTime2);
                System.out.println("hora de cierre localTime1: " + localTime1);
                System.out.println("hora de la base localTimeBase: " + localTimeBase);
                
                if ((localTimeBase.isAfter(h0) && localTimeBase.isBefore(localTime2)) || (localTimeBase.isAfter(localTime1) && localTimeBase.isBefore(h24))) {
                    return false;
                }
            } else {
                System.out.println("Error en fechas en la tabla: " + idTabla + "- idelemento: hora_cierre. ");
                return false;
            }
        } catch (ParseException e) {
            System.out.println("Error en ServicioActivo de SiscoopService. " + e);
            return false;
        }
        return true;
    }
    
    public boolean estatusOrigen(int idorigen) {
        try {
            return origenesService.buscarOrigen(idorigen).isEstatus();
        } catch (Exception e) {
            System.out.println("Error en estatusOrigen de SiscoopService: " + e.getMessage());
        }
        return false;
    }
    public boolean statusProducto(String opa){
        try {
            return origenesService.estatusProducto(opa);
        } catch (Exception e) {
            System.out.println("error al obtener origen en uso");
        }
        return false;
    }
    
    
    
    
}
