/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.entidades;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author wilmer
 */
@Entity
@Table(name = "banca_movil_usuarios_bankingly")
public class Usuarios_Banca_Movil implements Serializable {

    @Id
    private PersonasPK personasPK;
    @Column(name = "alias_usuario")
    private String alias_usuario;
    @Column(name = "idorigenp")
    private Integer idorigenp;
    @Column(name = "idproducto")
    private Integer idproducto;
    @Column(name = "idauxiliar")
    private Integer idauxiliar;
    @Column(name = "estatus")
    private boolean estatus;

    public Usuarios_Banca_Movil() {
    }

    public Usuarios_Banca_Movil(PersonasPK personasPK, String alias_usuario, Integer idorigenp, Integer idproducto, Integer idauxiliar, boolean estatus) {
        this.personasPK = personasPK;
        this.alias_usuario = alias_usuario;
        this.idorigenp = idorigenp;
        this.idproducto = idproducto;
        this.idauxiliar = idauxiliar;
        this.estatus = estatus;
    }

    public PersonasPK getPersonasPK() {
        return personasPK;
    }

    public void setPersonasPK(PersonasPK personasPK) {
        this.personasPK = personasPK;
    }

    public String getAlias_usuario() {
        return alias_usuario;
    }

    public void setAlias_usuario(String alias_usuario) {
        this.alias_usuario = alias_usuario;
    }

    public Integer getIdorigenp() {
        return idorigenp;
    }

    public void setIdorigenp(Integer idorigenp) {
        this.idorigenp = idorigenp;
    }

    public Integer getIdproducto() {
        return idproducto;
    }

    public void setIdproducto(Integer idproducto) {
        this.idproducto = idproducto;
    }

    public Integer getIdauxiliar() {
        return idauxiliar;
    }

    public void setIdauxiliar(Integer idauxiliar) {
        this.idauxiliar = idauxiliar;
    }

    public boolean isEstatus() {
        return estatus;
    }

    public void setEstatus(boolean estatus) {
        this.estatus = estatus;
    }

    @Override
    public String toString() {
        return "Usuarios_Banca_Movil{" + "personasPK=" + personasPK + ", alias_usuario=" + alias_usuario + ", idorigenp=" + idorigenp + ", idproducto=" + idproducto + ", idauxiliar=" + idauxiliar + ", estatus=" + estatus + '}';
    }

}
