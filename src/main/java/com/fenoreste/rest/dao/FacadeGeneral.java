/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 *
 * @author wilmer
 */
public abstract class FacadeGeneral<T> {

    EntityManagerFactory emf;

    public FacadeGeneral(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }
    
    public String obtenerOrigen(){
        EntityManager em=emf.createEntityManager();
        String origen="";
        try {
            Query query=em.createNativeQuery("SELECT nombre FROM origenes WHERE matriz=0");
            origen=String.valueOf(query.getSingleResult());
         } catch (Exception e) {
             System.out.println("Error al buscar origen:"+e.getMessage());
             em.close();
             emf.close();
             return origen;            
        }finally{
            em.close();
            emf.close();
        }
        return origen;
    }
  
  public Tablas busquedaTabla(String idtabla,String idelemento){
      System.out.println("si llego:"+emf.isOpen());
      EntityManager em=emf.createEntityManager();
      System.out.println("si paso");
      Tablas tb=null;
      try {
          System.out.println("IdTabla:"+idtabla+"IdElemento:"+idelemento);
          TablasPK tbPK=new TablasPK(idtabla, idelemento);
          tb=em.find(Tablas.class, tbPK);
      } catch (Exception e) {
          System.out.println("Error al buscar tabla:"+ e.getMessage());
          em.close();
          emf.close();
          return tb;
      }finally{
          em.close();
          emf.close();
      }
      return tb;      
  }
  
}
