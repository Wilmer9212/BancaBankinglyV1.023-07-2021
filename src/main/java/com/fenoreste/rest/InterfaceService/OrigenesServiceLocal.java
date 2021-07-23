/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.InterfaceService;

import com.fenoreste.rest.DTO.OrigenesDTO;
import com.fenoreste.rest.entidades.Origenes;


/**
 *
 * @author wilmer
 */
public interface OrigenesServiceLocal {
    OrigenesDTO buscarOrigen(Integer idorigen);
    Origenes cajaUsuario();
    boolean estatusProducto(String opa);

}
