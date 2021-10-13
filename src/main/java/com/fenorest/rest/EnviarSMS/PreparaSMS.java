/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenorest.rest.EnviarSMS;

import com.fenoreste.rest.DTO.OgsDTO;
import com.fenoreste.rest.Util.Utilidades;
import com.fenoreste.rest.Util.UtilidadesGenerales;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.entidades.PersonasPK;
import com.fenoreste.rest.entidades.Tablas;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.EntityManager;

/**
 *
 * @author wilmer
 */
public class PreparaSMS {

    UtilidadesGenerales util = new UtilidadesGenerales();
    Utilidades util2 = new Utilidades();
    EnviarSMS sendSMS = new EnviarSMS();

    // ENVIA EL SMS METODO PRA CSN
    public void enviaSMS_CSN(EntityManager em, String montoAbono, int identificadorOperacion, String debitAccount, String creditAccount, String numeroSocio) {
        System.out.println("preparando el sms");
        //consulto en tablas si existe la url del script de san nicolas para envio de mensajes
        try {

            Tablas tablasUrlSMS = util.busquedaTabla(em, "bankingly_banca_movil", "liga_envio_mensajes");
            if (tablasUrlSMS.getDato2().length() > 0) {
                System.out.println("se encontro la url para envio de sms");
                //Obtengo el celular del socio 
                OgsDTO ogs = util2.ogs(numeroSocio);
                PersonasPK personaPK = new PersonasPK(ogs.getIdorigen(), ogs.getIdgrupo(), ogs.getIdsocio());
                Persona p = em.find(Persona.class, personaPK);
                Tablas tablaContenidoSMS = null;
                String contenidoSMS = "";
                //Se identifica para transferencias a cuentas propias
                if (identificadorOperacion == 1) {
                    tablaContenidoSMS = util.busquedaTabla(em, "bankingly_banca_movil", "sms_retiro_cuenta_propia");
                    System.out.println("tabla contenido sms:" + tablaContenidoSMS);
                    contenidoSMS = contenidoSMS(tablaContenidoSMS.getDato2(), montoAbono, debitAccount, creditAccount, "", "");
                    System.out.println("El contenido de tu mensaje es:" + contenidoSMS);
                    sendSMS.enviar(tablasUrlSMS.getDato2(), p.getCelular(), contenidoSMS);
                    //Transferencia a terceros dentro de la entidad
                } else if (identificadorOperacion == 2) {
                    System.out.println("Tercero");
                    tablaContenidoSMS = util.busquedaTabla(em, "bankingly_banca_movil", "sms_retiro_cuenta_tercero");
                    System.out.println("tabla contenido sms:" + tablaContenidoSMS);
                    contenidoSMS = contenidoSMS(tablaContenidoSMS.getDato2(), montoAbono, debitAccount, creditAccount, "", "");
                    System.out.println("El contenido de tu mensaje es:" + contenidoSMS);
                    sendSMS.enviar(tablasUrlSMS.getDato2(), p.getCelular(), contenidoSMS);
                    //Pago de prestamos
                } else if (identificadorOperacion == 3) {
                    System.out.println("Pago prestamo propio");
                    tablaContenidoSMS = util.busquedaTabla(em, "bankingly_banca_movil", "sms_retiro_cuenta_propia");
                    System.out.println("tabla contenido sms:" + tablaContenidoSMS);
                    contenidoSMS = contenidoSMS(tablaContenidoSMS.getDato2(), montoAbono, debitAccount, creditAccount, "", "");
                    System.out.println("El contenido de tu mensaje es:" + contenidoSMS);
                    sendSMS.enviar(tablasUrlSMS.getDato2(), p.getCelular(), contenidoSMS);                  
                }else if (identificadorOperacion==4){
                   System.out.println("Pago prestamo tercero");
                    tablaContenidoSMS = util.busquedaTabla(em, "bankingly_banca_movil", "sms_retiro_cuenta_tercero");
                    System.out.println("tabla contenido sms:" + tablaContenidoSMS);
                    contenidoSMS = contenidoSMS(tablaContenidoSMS.getDato2(), montoAbono, debitAccount, creditAccount, "", "");
                    System.out.println("El contenido de tu mensaje es:" + contenidoSMS);
                    sendSMS.enviar(tablasUrlSMS.getDato2(), p.getCelular(), contenidoSMS); 
                }else{ if(identificadorOperacion == 5){
                    System.out.println("Pago orden SPEI");
                    tablaContenidoSMS = util.busquedaTabla(em, "bankingly_banca_movil", "sms_retiro_cuenta_tercero");
                    System.out.println("tabla contenido sms:" + tablaContenidoSMS);
                    contenidoSMS = contenidoSMS(tablaContenidoSMS.getDato2(), montoAbono, debitAccount, creditAccount, "", "");
                    System.out.println("El contenido de tu mensaje es:" + contenidoSMS);
                    sendSMS.enviar(tablasUrlSMS.getDato2(), p.getCelular(), contenidoSMS);
                }
                    
                }

            }
        } catch (Exception e) {
            System.out.println("Error en sms:" + e.getMessage());
        }

    }

    // RELLENA EL CONTENIDO DEL SMS
    private String contenidoSMS(String contenidoSMS, String monto, String productoOrigen, String productoDestino, String authOrigen, String authDestino) {

        Date hoy = new Date();
        System.out.println("Formando contenido fecha:" + hoy);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyy HH:MM:ss");
        String fecha = sdf.format(hoy);
        System.out.println("Fecha String:" + fecha);
        return contenidoSMS.replace("@monto@", monto)
                .replace("@fechayHora@", fecha)
                .replace("@productoOrigen@", productoOrigen)
                .replace("@productoDestino@", productoDestino)
                .replace("@autorizacionOrigen@", authOrigen)
                .replace("@autorizacionDestino@", authDestino);
    }

}
