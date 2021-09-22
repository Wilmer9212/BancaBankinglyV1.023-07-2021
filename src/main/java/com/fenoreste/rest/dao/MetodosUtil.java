/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.ResponseDTO.ClientByDocumentDTO;
import com.fenoreste.rest.ResponseDTO.usuarios_banca_bankinglyDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.entidades.banca_movil_usuarios;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 *
 * @author wilmer
 */
public abstract class MetodosUtil{

    private static EntityManagerFactory emf;

    List<Object[]> lista = null;
    

    public MetodosUtil() {
        emf = AbstractFacade.conexion();
    }   
    
    public boolean actividad(){
        boolean bandera=false;
        EntityManager em=emf.createEntityManager();
        try {
            String actividad="SELECT sai_bankingly_servicio_activo_inactivo()";
            Query query=em.createNativeQuery(actividad);
            bandera =(boolean) query.getSingleResult();
        } catch (Exception e) {
            em.close();
            cerrar();
            System.out.println("Error al recuperar el tiempo de actividad:"+e.getMessage());
        }finally{
            em.close();
            cerrar();
        }
        return bandera;
    }
  
    public void cerrar() {
        emf.close();
    }



}
