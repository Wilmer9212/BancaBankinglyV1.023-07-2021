package com.fenoreste.rest.entidades;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author wilmer
 */
public class ws_siscoop_tarjetas {
    
    @Id
    @Column(name="idtarjeta")
    private String idtarjeta;
    @Column(name = "fecha_hora")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha;
    @Column(name = "seleccionada")
    private boolean seleccionada;
    @Column(name = "asignada")
    private boolean asignada;
    @Column(name = "eliminada")
    private boolean eliminada;
    @Column(name = "fecha_vencimiento")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha_vencimiento;

    public ws_siscoop_tarjetas() {
    }

    public ws_siscoop_tarjetas(String idtarjeta, Date fecha, boolean seleccionada, boolean asignada, boolean eliminada, Date fecha_vencimiento) {
        this.idtarjeta = idtarjeta;
        this.fecha = fecha;
        this.seleccionada = seleccionada;
        this.asignada = asignada;
        this.eliminada = eliminada;
        this.fecha_vencimiento = fecha_vencimiento;
    }

    public String getIdtarjeta() {
        return idtarjeta;
    }

    public void setIdtarjeta(String idtarjeta) {
        this.idtarjeta = idtarjeta;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public boolean isSeleccionada() {
        return seleccionada;
    }

    public void setSeleccionada(boolean seleccionada) {
        this.seleccionada = seleccionada;
    }

    public boolean isAsignada() {
        return asignada;
    }

    public void setAsignada(boolean asignada) {
        this.asignada = asignada;
    }

    public boolean isEliminada() {
        return eliminada;
    }

    public void setEliminada(boolean eliminada) {
        this.eliminada = eliminada;
    }

    public Date getFecha_vencimiento() {
        return fecha_vencimiento;
    }

    public void setFecha_vencimiento(Date fecha_vencimiento) {
        this.fecha_vencimiento = fecha_vencimiento;
    }

    @Override
    public String toString() {
        return "ws_siscoop_tarjetas{" + "idtarjeta=" + idtarjeta + ", fecha=" + fecha + ", seleccionada=" + seleccionada + ", asignada=" + asignada + ", eliminada=" + eliminada + ", fecha_vencimiento=" + fecha_vencimiento + '}';
    }
    
    
}
