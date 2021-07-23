/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.InterfaceService;
import com.fenoreste.rest.DTO.*;
import javax.ejb.LocalBean;
/**
 *
 * @author wilmer
 */
@LocalBean
public interface SaiComisionCobranzaServiceLocal {
    SaiComisionCobranzaDTO buscaComisionCobranza(int idorigenp, int idproducto, int idauxiliar);
}
