/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenorest.rest.Auth;

import com.fenoreste.rest.ResponseDTO.PersonaDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.Util.MetodosUtil;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.entidades.PersonasPK;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 *
 * @author wilmer
 */
public class NewClass {
    
    public static void main(String[] args) {
        //EntityManager em=AbstractFacade.conexion().createEntityManager();
        /*try {
           
           
            Query e=em.createNativeQuery("SELECT idorigen,igrupo,idsocio,nombre,sai_convierte_caracteres_especiales_iso8859-utf8(appaterno),sai_convierte_caracteres_especiales_iso8859-utf8(apmaterno),"
                                       + " FROM personas WHERE idorigen=30301 and idgrupo=10 and idsocio=60",PersonaDTO.class);
           PersonaDTO dto=(PersonaDTO)e.getSingleResult();
            System.out.println("DTO:"+dto);
        } catch (Exception e) {
            
        }finally{
            em.close();
        }*/
        MetodosUtil mt=new MetodosUtil();
        System.out.println("MT:"+mt.StringToDate("21/03/2021"));
    
        
}
    
}
