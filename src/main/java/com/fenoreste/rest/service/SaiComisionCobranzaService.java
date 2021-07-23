/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.service;

import com.fenoreste.rest.DTO.SaiComisionCobranzaDTO;
import com.fenoreste.rest.InterfaceService.SaiComisionCobranzaServiceLocal;
import com.fenoreste.rest.Util.AbstractFacade_1;
import com.fenoreste.rest.entidades.SaiComisionCobranza;
import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 *
 * @author wilmer
 */
public class SaiComisionCobranzaService implements SaiComisionCobranzaServiceLocal {

    
    private AbstractFacade_1 saiComisionCobranzaFacade;

    EntityManager entity;

  
    public SaiComisionCobranzaDTO buscaComisionCobranza(int idorigenp, int idproducto, int idauxiliar) {
        SaiComisionCobranzaDTO salida = new SaiComisionCobranzaDTO();
        try {
            SaiComisionCobranza saiComisionCobranza = buscaComision(idorigenp, idproducto, idauxiliar);
            if (saiComisionCobranza != null) {
               salida.setEsMontoModificado(saiComisionCobranza.getEsMontoModificado());
               salida.setMontoComision(saiComisionCobranza.getMontoComision());
               salida.setMontoFijo(saiComisionCobranza.getMontoFijo());
               salida.setProporcional(saiComisionCobranza.getProporcional());
               salida.setTasaComision(saiComisionCobranza.getTasaComision());
               salida.setTasaProporcional(saiComisionCobranza.getTasaProporcional());
              
            }
        } catch (Exception e) {
            //System.out.println("Error en buscaComisionCobranza de SaiComisionCobranzaService: " + e.getMessage());
        }
        return salida;
    }

    // BUSCA LA COMISION DEL OPA
    private SaiComisionCobranza buscaComision(int idorigenp, int idproducto, int idauxiliar) {
        entity = saiComisionCobranzaFacade.conexion();
        String consulta = "SELECT * FROM sai_calculos_de_comision_cobranza(?, ?, ?)";
        Query query = entity.createNativeQuery(consulta); // SaiComisionCobranza.class
        query.setParameter(1, idorigenp);
        query.setParameter(2, idproducto);
        query.setParameter(3, idauxiliar);
        SaiComisionCobranza comision = (SaiComisionCobranza) query.getSingleResult();
        entity.close();
        return comision;
    }

   
}
