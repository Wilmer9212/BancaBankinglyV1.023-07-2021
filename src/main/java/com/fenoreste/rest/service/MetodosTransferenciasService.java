/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.service;

import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.Util.AbstractFacade_1;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.entidades.Productos;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 *
 * @author wilmer
 */
public class MetodosTransferenciasService {
  
    
    public String comprobarCuentaOrigen(String opa, Double monto, String ogs) {
          EntityManager em = AbstractFacade_1.conexion();
        String mensaje = "";
        try {
            //Busco la persona que se esta identificando
            Persona p = BusquedaPersona(ogs);
            //Si la persona existe y esta activo
            if (p != null) {
                //Buscamos el folio que esta ingresado 
                Query query=em.createNativeQuery("SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='"+opa+"' AND a.estatus=2",Auxiliares.class);
                Auxiliares a = (Auxiliares) query.getSingleResult();
                //comparamos ogs para saber si el producto pertenece al socio 
                if (p.getPersonasPK().getIdorigen() == a.getIdorigen() && p.getPersonasPK().getIdgrupo() == a.getIdgrupo() && p.getPersonasPK().getIdsocio() == a.getIdsocio()) {
                    //Validamos el estatus del prpducto
                    if (a.getEstatus() == 2) {
                        //validamos que el producto no sea un prestamo
                        Productos pr=em.find(Productos.class,a.getAuxiliaresPK().getIdorigenp());
                        if(pr.getTipoproducto()!=2){
                        //validamos que el monto a transferir sea mayor o igual al saldo que tiene el producto
                           if (Double.parseDouble(a.getSaldo().toString()) >= monto) {
                               mensaje = "VALIDADO CON EXITO";
                           } else {
                               mensaje = "FONDOS INSUFICIENTES PARA COMPLETAR LA OPERACION";
                           }
                       }else{
                            mensaje="PRODUCTO ORIGEN NO ACEPTADO PARA TRANSFERIR";
                        }
                    } else {
                        mensaje = "PRODUCTO ORIGEN INACTIVO";
                    }
                } else {
                    mensaje = "PRODUCTO ORIGEN NO PERTENECE AL SOCIO:" + ogs;
                }
            } else {
                mensaje = "PERSONA NO EXISTE";
            }
        } catch (Exception e) {
            System.out.println("Mensaje de error:" + e.getMessage());
            mensaje = "SIN RESULTADOS PARA LA BUSQUEDA";
           
        } 
        return mensaje;
    }

    public String comprobarCuentaDestino(String opa,String ogs) {
        String mensaje = "";
          EntityManager em = AbstractFacade_1.conexion();
        try {           
            //Busco la persona que se esta identificando
            Query query=em.createNativeQuery("SELECT * FROM personas p WHERE replace(to_char(p.idorigen,'099999')||to_char(p.idgrupo,'09')||to_char(p.idsocio,'099999'),' ','')='"+ogs+"'",Persona.class);
            Persona p=(Persona)query.getSingleResult();               
            //Si la persona existe y esta activo
            if (p != null) {
                //Buscamos el folio que esta ingresado 
               Auxiliares a = BusquedaFolio(opa);
               System.out.println("oasi");
                //comparamos ogs para saber si el producto pertenece al socio 
                if (p.getPersonasPK().getIdorigen() == a.getIdorigen() && p.getPersonasPK().getIdgrupo() == a.getIdgrupo() && p.getPersonasPK().getIdsocio() == a.getIdsocio()) {
                    //Validamos el estatus del prpducto
                    if (a.getEstatus() == 2) {
                        //validamos que el producto no sea un prestamo
                        Productos pr=em.find(Productos.class,a.getAuxiliaresPK().getIdproducto());                        
                        if (pr.getTipoproducto()!=2) {
                            mensaje = "VALIDADO CON EXITO";
                        } else {
                            mensaje = "EL PRODUCTO DESTINO NO ACEPTA TRANSFERENCIAS";
                        }
                    } else {
                        mensaje = "PRODUCTO DESTINO INACTIVO";
                    }
                } else {
                    mensaje = "PRODUCTO DESTINO NO PERTENECE AL SOCIO:" + ogs;
                }
            } else {
                mensaje = "PERSONA NO EXISTE";
            }
        } catch (Exception e) {
            System.out.println("Mensaje de error:" + e.getMessage());            
            mensaje = "SIN RESULTADOS PARA LA BUSQUEDA";
        } 
        return mensaje;
    }
    
    
    public Persona BusquedaPersona(String ogs){
        EntityManager em = AbstractFacade_1.conexion();
        Persona p=new Persona();
        try {
            em.getTransaction().begin();
            Query query=em.createNativeQuery("SELECT * FROM personas p WHERE replace(to_char(p.idorigen,'099999')||to_char(p.idgrupo,'09')||to_char(p.idsocio,'099999'),' ','')='"+ogs+"'",Persona.class);
            p=(Persona)query.getSingleResult();  
            
            System.out.println("salio con perosona:"+p.getNombre());
        } catch (Exception e) {
            System.out.println("Error en busqueda de persona:"+e.getMessage());
        }
       return p; 
    }
    
    public Auxiliares BusquedaFolio(String opa){    
        EntityManager em = AbstractFacade_1.conexion();
        Auxiliares a=null;
        try {
            String consulta="SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='"+opa+"' AND a.estatus=2";
            System.out.println("consulta:"+consulta);
            Query query=em.createNativeQuery(consulta,Auxiliares.class);
            a=(Auxiliares)query.getSingleResult();  
          } catch (Exception e) {
            System.out.println("Error al buscar Folio:"+e.getMessage());         
        }
        System.out.println("a:"+a);
       return a; 
    }
}
