/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.entidades.Auxiliares;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author Elliot
 */
public class DAOTDD extends FacadeTarjetaDebito<Auxiliares> {

    public DAOTDD() {
        super(Auxiliares.class);
    }
}
