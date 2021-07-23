/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.service;

import com.fenoreste.rest.DTO.AuxiliaresDTO;
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
public class MTTransferencias {

    public String ComprobarCtaOrigen(String opa, Double monto, String ogs) {

        String mensaje = "";
       
        try {
            busquedaSocio(ogs);
            //   Persona p =io(ogs);
            //Si la persona existe y esta activo
            //if (p != null) {
            //mensaje="si";
            //Buscamos el folio que esta ingresado 
            /* Auxiliares a = busquedaFolio(opa);
                //comparamos ogs para saber si el producto pertenece al socio 
                if (p.getPersonasPK().getIdorigen() == a.getIdorigen() && p.getPersonasPK().getIdgrupo() == a.getIdgrupo() && p.getPersonasPK().getIdsocio() == a.getIdsocio()) {
                    //Validamos el estatus del prpducto
                    if (a.getEstatus() == 2) {
                        //validamos que el producto no sea un prestamo
                        Productos pr = em.find(Productos.class, a.getAuxiliaresPK().getIdproducto());
                        if (pr.getTipoproducto() != 2) {
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
             */
        } catch (Exception e) {
            return e.getMessage();
        } 
        return mensaje;
    }

    public Persona busquedaSocio(String ogs) {
         EntityManagerFactory emf = AbstractFacade.conexion();
        EntityManager em = emf.createEntityManager();
        try {
            String consulta = "SELECT * FROM personas p WHERE replace(to_char(p.idorigen,'099999')||to_char(p.idgrupo,'09')||to_char(p.idsocio,'099999'),' ','')='" + ogs + "'";
            Query query = em.createNativeQuery(consulta, Persona.class).setHint("javax.persistence.query.timeout", 50);
            Persona p = (Persona) query.getSingleResult();
            if (p != null) {
                return p;
            }
            em.getTransaction().commit();
            em.clear();
            em.close();
            emf.close();
        } catch (Exception e) {

            System.out.println("Error al buscar persona:" + e.getMessage());
        }
        return null;
    }

    public Auxiliares busquedaFolio(String opa) {
         EntityManagerFactory emf = AbstractFacade.conexion();
        EntityManager em = emf.createEntityManager();
        try {
            String folio = "SELECT * FROM auxiliares a INNER JOIN tipos_cuenta_bankingly tps USING(idproducto) WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opa + "'";
            em.getTransaction().begin();
            Query query = em.createNativeQuery(folio, Auxiliares.class).setHint("javax.persistence.query.timeout", 50);
            Auxiliares a = (Auxiliares) query.getSingleResult();
            em.getTransaction().commit();
            if (a != null) {
                return a;
            }
        } catch (Exception e) {
            System.out.println("Error al buscar tu folio:" + e.getMessage());
        } finally {
            if (em != null) {
                em.close();
            }
        }
        return null;
    }
}
