/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.entidades;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 *
 * @author Elliot
 */
@Entity
@Table(name = "ws_siscoop_folios_tarjetas")
public class WsSiscoopFoliosTarjetas implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    protected WsSiscoopFoliosTarjetasPK wsFoliosTarjetasSyCPK;
    @Column(name = "asignada")
    private Boolean asignada;
    @Column(name = "activa")
    private Boolean activa;
    @Column(name = "bloqueada")
    private Boolean bloqueada;

    public WsSiscoopFoliosTarjetas() {
    }

    public WsSiscoopFoliosTarjetas(WsSiscoopFoliosTarjetasPK wsFoliosTarjetasSyCPK) {
        this.wsFoliosTarjetasSyCPK = wsFoliosTarjetasSyCPK;
    }

    public WsSiscoopFoliosTarjetas(int idorigenp, int idproducto, int idauxiliar, String idtarjeta, Date fechaHora) {
        this.wsFoliosTarjetasSyCPK = new WsSiscoopFoliosTarjetasPK(idorigenp, idproducto, idauxiliar, idtarjeta, fechaHora);
    }

    public WsSiscoopFoliosTarjetasPK getWsSiscoopFoliosTarjetasPK() {
        return wsFoliosTarjetasSyCPK;
    }

    public void setWsSiscoopFoliosTarjetasPK(WsSiscoopFoliosTarjetasPK wsFoliosTarjetasSyCPK) {
        this.wsFoliosTarjetasSyCPK = wsFoliosTarjetasSyCPK;
    }

    public Boolean getAsignada() {
        return asignada;
    }

    public void setAsignada(Boolean asignada) {
        this.asignada = asignada;
    }

    public Boolean getActiva() {
        return activa;
    }

    public void setActiva(Boolean activa) {
        this.activa = activa;
    }

    public Boolean getBloqueada() {
        return bloqueada;
    }

    public void setBloqueada(Boolean bloqueada) {
        this.bloqueada = bloqueada;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (wsFoliosTarjetasSyCPK != null ? wsFoliosTarjetasSyCPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof WsSiscoopFoliosTarjetas)) {
            return false;
        }
        WsSiscoopFoliosTarjetas other = (WsSiscoopFoliosTarjetas) object;
        return !((this.wsFoliosTarjetasSyCPK == null && other.wsFoliosTarjetasSyCPK != null) || (this.wsFoliosTarjetasSyCPK != null && !this.wsFoliosTarjetasSyCPK.equals(other.wsFoliosTarjetasSyCPK)));
    }

    @Override
    public String toString() {
        return "com.fenoreste.modelo.entidad.WsSiscoopFoliosTarjetas[ wsSiscoopFoliosTarjetasPK=" + wsFoliosTarjetasSyCPK + " ]";
    }

}
