/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.InterfaceService;

import javax.ejb.LocalBean;

/**
 *
 * @author wilmer
 */
@LocalBean
public interface TransferenciasServiceLocal {
    
    public boolean comprodarCuentaOrigen(String opa,Double monto,String ogs);
    public boolean comprodarCuentaDestino(String opa,String ogs);
    public boolean aplicarCargos(String opaOrigen,String opaDestino,Double montoO,Double montoD,int tipoMovOrigen,int tipoMovDestino);
    
}
