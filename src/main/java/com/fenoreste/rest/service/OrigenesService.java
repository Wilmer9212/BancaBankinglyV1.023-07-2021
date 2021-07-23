/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.service;

import com.fenoreste.rest.DTO.OrigenesDTO;
import com.fenoreste.rest.InterfaceService.OrigenesServiceLocal;
import com.fenoreste.rest.entidades.Origenes;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Auxiliares;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 *
 * @author gerardo
 */

public class OrigenesService implements OrigenesServiceLocal {
   private AbstractFacade origenesFacade;
    EntityManager entity;

    public OrigenesDTO buscarOrigen(Integer idorigen) {
        EntityManagerFactory emf=origenesFacade.conexion2();
        entity = emf.createEntityManager();
        //EntityManagerFactory emf=origenesFacade.getEntityManager();
        //entity=emf.createEntityManager();
        OrigenesDTO origenesDTO = new OrigenesDTO();
        try {
            String consulta = " SELECT o.* "
                    + "         FROM origenes o"
                    + "         WHERE o.idorigen = ? ";
            Query query = entity.createNativeQuery(consulta, Origenes.class);
            query.setParameter(1, idorigen);
            Origenes origenes = (Origenes) query.getSingleResult();
            
        } catch (Exception e) {
            System.out.println("Error en buscarOrigen de OrigenesService: " + e.getMessage());
        }
        entity.close();
        return origenesDTO;
    }

   @Override
    public Origenes cajaUsuario() {
         EntityManagerFactory emf=origenesFacade.conexion2();
         entity = emf.createEntityManager();
        //EntityManagerFactory emf=origenesFacade.getEntityManager();
        //entity=emf.createEntityManager();
        Origenes origenes=null;
        try {
            String consulta = " SELECT o.* "
                    + "         FROM origenes o"
                    + "         WHERE o.idorigen = ? ";
            Query query = entity.createNativeQuery(consulta, Origenes.class);            
            origenes = (Origenes) query.getSingleResult();
            
            
        } catch (Exception e) {
            System.out.println("Error en buscarOrigen de OrigenesService: " + e.getMessage());
        }
        entity.close();
        return origenes;  
    }
    
    @Override
    public boolean estatusProducto(String opa){
        EntityManagerFactory emf=origenesFacade.conexion2();
        entity=emf.createEntityManager();
        try {
             String consulta = " SELECT a.* "
                    + "         FROM  auxiliares a"
                    + "         WHERE replace(to_char(a.idorigenp,'099999')||"
                    + "                       to_char(a.idproducto,'09999')||"
                    + "                      to_char(a.idauxiliar,'09999999')||"
                    + "         ,' ','')='"+opa+"'";
             Query query = entity.createNativeQuery(consulta, Auxiliares.class); 
             Auxiliares a=(Auxiliares) query.getSingleResult();
             if(a.getEstatus()==2){
                 return true;
             }        
        } catch (Exception e) {
            return false;
        }
        return false;
    }


}
