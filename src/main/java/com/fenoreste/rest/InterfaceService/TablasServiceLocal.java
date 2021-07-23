/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.InterfaceService;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.entidades.TablasPK;

/**
 *
 * @author prometeo
 */

public interface TablasServiceLocal {

    TablasDTO buscaTabla(TablasPK tablasPk);

    TablasDTO buscaValorUDIS();
    
    TablasDTO buscaTablaPuntomania();
    
}
