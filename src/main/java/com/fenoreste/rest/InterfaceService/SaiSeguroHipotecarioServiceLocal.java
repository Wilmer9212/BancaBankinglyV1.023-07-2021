/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.InterfaceService;

import com.fenoreste.rest.DTO.SaiSeguroHipotecarioDTO;
import java.util.Date;
import java.util.List;
import javax.ejb.LocalBean;

/**
 *
 * @author wilmer
 */
@LocalBean
public interface SaiSeguroHipotecarioServiceLocal {
    List<SaiSeguroHipotecarioDTO> buscarPagoSeguroHipotecario(int idorigenp, int idproducto, int idauxiliar, Date fecha);

}
