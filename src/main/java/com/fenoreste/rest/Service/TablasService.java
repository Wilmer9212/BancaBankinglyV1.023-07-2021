/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Service;


import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.InterfaceService.TablasServiceLocal;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import java.lang.reflect.InvocationTargetException;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

/**
 *
 * @author gerardo
 */

public class TablasService implements TablasServiceLocal {

    EntityManagerFactory emf=AbstractFacade.conexion();
    EntityManager em;
    @Override
    public TablasDTO buscaTabla(TablasPK tablaspk) {
        System.out.println("si que si");
        em= emf.createEntityManager();
       // EntityManagerFactory emf=tablasFacade.getEntityManager();
        System.out.println("Buscando tabla...");
       // entity=emf.createEntityManager();
        
        System.out.println("IdTabla:"+tablaspk.getIdtabla()+" , IdElemento:"+tablaspk.getIdelemento());
        TablasDTO tablasDTO = new TablasDTO();
        try {
            String consulta = " SELECT t.* "
                    + "         FROM tablas t "
                    + "         WHERE t.idtabla = ? "
                    + "           AND t.idelemento = ? ";
            Query query = em.createNativeQuery(consulta, Tablas.class);
            query.setParameter(1, tablaspk.getIdtabla());
            query.setParameter(2, tablaspk.getIdelemento());
            Tablas tablas = (Tablas) query.getSingleResult();
            tablasDTO.setDato1(tablas.getDato1());
            tablasDTO.setDato2(tablas.getDato2());
            tablasDTO.setDato3(tablas.getDato3());
            tablasDTO.setDato4(tablas.getDato4());
            tablasDTO.setDato5(tablas.getDato5());
            
        } catch (Exception e) {
            System.out.println("No se pudo encontrar la tabla en (Tablas Services): " + e.getMessage());
        }
        em.close();
        return tablasDTO;
    }

    @Override
    public TablasDTO buscaValorUDIS() {
        em = emf.createEntityManager();
       // EntityManagerFactory emf=tablasFacade.getEntityManager();
        //entity=emf.createEntityManager();
        TablasDTO tablasDTO = new TablasDTO();
        try {
            String consulta = " SELECT t.* "
                    + "         FROM tablas t "
                    + "         WHERE t.idtabla = 'valor_udi' "
                    + "         ORDER BY idelemento "
                    + "         DESC limit 1 ";
            Query query = em.createNativeQuery(consulta, Tablas.class);
            Tablas tablas = (Tablas) query.getSingleResult();
            
        } catch (Exception e) {
            System.out.println("Error en buscar UDIS de buscaValorUDIS: " + e.getMessage());
        }
        em.close();
        return tablasDTO;
    }
   
     
   
    
    @Override
    public TablasDTO buscaTablaPuntomania() {
        em = emf.createEntityManager();
        TablasDTO tablasDTO = new TablasDTO();
        try {
            String consulta = " SELECT t.* "
                    + "         FROM tablas t "
                    + "         WHERE t.idtabla = 'param' "
                    + "           AND t.idelemento = 'programa_puntos' "
                    + "           AND dato1 IS NOT NULL "
                    + "           AND dato1 != '' "
                    + "           AND dato1 = '1' "
                    + "           AND dato3 IS NOT NULL "
                    + "           AND dato3 != ''  "
                    + "           AND dato4 IS NOT NULL "
                    + "           AND dato4 != ''  "
                    + "           AND date((select fechatrabajo from origenes limit 1)) between date(dato3) and date(dato4) "
                    + "           limit 1 ";
            Query query = em.createNativeQuery(consulta, Tablas.class);
            Tablas tablas = (Tablas) query.getSingleResult();
            
        } catch (Exception e) {
            //System.out.println("Error en buscaTabla de TablasService: " + e.getMessage());
        }
        em.close();
        return tablasDTO;
        
        
    }
   
}
