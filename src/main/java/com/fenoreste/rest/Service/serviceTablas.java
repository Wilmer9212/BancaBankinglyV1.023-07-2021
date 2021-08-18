/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Service;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.InterfaceService.ServiceTablas;
import com.fenoreste.rest.entidades.TablasPK;

/**
 *
 * @author wilmer
 */
public class serviceTablas implements ServiceTablas{

    @Override
    public TablasDTO buscaTabla(TablasPK tablasPk) {
        System.out.println("si llega");
        return null;
    }

    @Override
    public TablasDTO buscaValorUDIS() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public TablasDTO buscaTablaPuntomania() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
