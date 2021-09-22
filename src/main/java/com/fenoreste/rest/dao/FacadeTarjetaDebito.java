/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.WsTDD.SaldoTddPK;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetas1;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetasPK1;
import com.syc.ws.endpoint.siscoop.BalanceQueryResponseDto;
import com.syc.ws.endpoint.siscoop.DoWithdrawalAccountResponse;
import com.syc.ws.endpoint.siscoop.LoadBalanceResponse;
import java.sql.Timestamp;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import wssyctdd.SiscoopTDD;

/**
 *
 * @author wilmer
 */
public abstract class FacadeTarjetaDebito<T>{
    
    EntityManagerFactory emf = null;

    public FacadeTarjetaDebito(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }
    
    public Tablas productoTddWS() {
        EntityManager em = emf.createEntityManager();
        try {
            TablasPK pkt = new TablasPK("identificador_uso_tdd", "activa");
            Tablas tb = em.find(Tablas.class, pkt);
            if (tb != null) {
                return tb;
            } else {
                return null;
            }
        } catch (Exception e) {
            em.close();
            System.out.println("No existe producto activo para tdd:" + e.getMessage());
        } finally {
            em.close();
         }
        return null;
    }

    // PRODUCTO VALIDO PARA LA TDD
    public TablasDTO productoTddwebservice() {
        EntityManager em = emf.createEntityManager();
        TablasDTO tablaDTO = new TablasDTO();
        try {
            // Producto de la tdd
            TablasPK tablasPK = new TablasPK("param", "producto_para_webservice");
            Tablas tabla = em.find(Tablas.class, tablasPK);
            if (tabla != null) {
                tablaDTO.setDato1(tabla.getDato1());
                tablaDTO.setDato2(tabla.getDato2());
                tablaDTO.setDato3(tabla.getDato3());
                tablaDTO.setDato4(tabla.getDato3());
                tablaDTO.setDato5(tabla.getDato5());
                System.out.println("TABLA DE PRODUCTO PARA WS:" + tablaDTO);

            }
        } catch (NumberFormatException e) {
            em.close();
            System.out.println("Error en consultar producto en producto_para_webservice de TarjetaDeDebito." + e.getMessage());
           return tablaDTO;
        } finally {
            em.close();
           
        }
        return tablaDTO;
    }

    public WsSiscoopFoliosTarjetas1 buscaTarjetaTDD(int idorigenp, int idproducto, int idauxiliar) {
        System.out.println("llllllllllllllllllllllllllwgoooooooooooooooooooooooooooooooo");
        EntityManager em = emf.createEntityManager();
        WsSiscoopFoliosTarjetasPK1 foliosPK1 = new WsSiscoopFoliosTarjetasPK1(idorigenp, idproducto, idauxiliar);
        System.out.println("foliosPK:"+foliosPK1);
        WsSiscoopFoliosTarjetas1 wsSiscoopFoliosTarjetas = new WsSiscoopFoliosTarjetas1();
        System.out.println("pasoooooooooooooooooooooooooooooooooooooooooooooooooooooo");
        try {
            String consulta = " SELECT w.* "
                    + "         FROM ws_siscoop_folios_tarjetas w "
                    + "         INNER JOIN ws_siscoop_tarjetas td using(idtarjeta)"
                    + "         WHERE w.idorigenp = ? "
                    + "         AND w.idproducto = ?"
                    + "         AND w.idauxiliar = ?"
                    + "          AND td.fecha_vencimiento > (select distinct fechatrabajo from origenes limit 1) ";
     
            Query query = em.createNativeQuery(consulta, WsSiscoopFoliosTarjetas1.class);
            query.setParameter(1, idorigenp);
            query.setParameter(2, idproducto);
            query.setParameter(3, idauxiliar);
            System.out.println("Consulta:" + consulta);
            wsSiscoopFoliosTarjetas = (WsSiscoopFoliosTarjetas1) query.getSingleResult();
            if (wsSiscoopFoliosTarjetas != null) {
                wsSiscoopFoliosTarjetas.setActiva(wsSiscoopFoliosTarjetas.getActiva());
                wsSiscoopFoliosTarjetas.setAsignada(wsSiscoopFoliosTarjetas.getAsignada());
                wsSiscoopFoliosTarjetas.setBloqueada(wsSiscoopFoliosTarjetas.getBloqueada());
                wsSiscoopFoliosTarjetas.setWsSiscoopFoliosTarjetasPK(foliosPK1);
            }
        } catch (Exception e) {
            System.out.println("Error en buscaTarjetaTDD de WsSiscoopFoliosTarjetas: " + e.getMessage());           
            em.close();
            return wsSiscoopFoliosTarjetas;
        } finally {
            System.out.println("assssssssssssssssssssssssssssssss");
            em.close();
        }
        return wsSiscoopFoliosTarjetas;
    }

    /*public String FolioTarjeta(String idTarjeta){
        EntityManagerFactory emf=AbstractFacade.conexion();
        EntityManager em=emf.createEntityManager();
        ws_siscoop_tarjetas folio=null;
        try {
            
        } catch (Exception e) {
          
        }finally{
            
        }
        
    }*/
    public BalanceQueryResponseDto saldoTDD(WsSiscoopFoliosTarjetasPK1 foliosPK) {
        EntityManager em = emf.createEntityManager();
        BalanceQueryResponseDto response = new BalanceQueryResponseDto();
        WsSiscoopFoliosTarjetas1 tarjeta = em.find(WsSiscoopFoliosTarjetas1.class, foliosPK);
        try {
            System.out.println("Estatus tarjeta en buscar saldo:" + tarjeta.getActiva());
            if (tarjeta.getActiva()) {
                /*response.setAvailableAmount(20);
                response.setCode(1);
                response.setDescription("activa");*/
                //////// response = conexionSiscoop().getSiscoop().getBalanceQuery(tarjeta.getIdtarjeta());
                response.setAvailableAmount(200);
                response.setCode(1);
                response.setDescription("Tarjeta activa");
                System.out.println("response:" + response.getDescription());
            } else {
                response.setDescription("La tarjeta esta inactiva: " + tarjeta.getIdtarjeta());
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error al buscar Saldo tdd:" + e.getMessage());
        } finally {
            em.close();
        }

        System.out.println("responseDTO:" + response);
        return response;
    }

    public DoWithdrawalAccountResponse.Return retiroTDD(WsSiscoopFoliosTarjetas1 tarjeta, Double monto) {
        DoWithdrawalAccountResponse.Return doWithdrawalAccountResponse = new DoWithdrawalAccountResponse.Return();
        boolean retiro = false;
        System.out.println("WS...Restiro");
        try {
            /*doWithdrawalAccountResponse.setAuthorization("");
            doWithdrawalAccountResponse.setBalance(200);
            doWithdrawalAccountResponse.setCode(1);*/

            //doWithdrawalAccountResponse=conexionSiscoop().getSiscoop().doWithdrawalAccount(tarjeta.getIdtarjeta(), monto);
            if (doWithdrawalAccountResponse.getCode() == 0) { // 0 = Existe error
                retiro = true;
            } else {
                retiro = true;
            }
        } catch (Exception e) {
            return doWithdrawalAccountResponse;
        }
        return doWithdrawalAccountResponse;
    }

    public SiscoopTDD conexionSiscoop() {
        EntityManager em = emf.createEntityManager();
        SiscoopTDD conexionWSTDD = null;
        try {
            TablasPK tablasPK = new TablasPK("siscoop_banca_movil", "wsdl_parametros");
            Tablas parametros = em.find(Tablas.class, tablasPK);
            if (parametros != null) {
                System.out.println("Conectando ws ALestra....");
                conexionWSTDD = new SiscoopTDD(parametros.getDato1(), parametros.getDato2());
            }
        } catch (Exception e) {
            em.close();
            System.out.println("No existen parametros para conexion:" + e.getMessage());
        } finally {
            em.close();
        }
        return conexionWSTDD;
    }

    public void actualizarSaldoTDD(SaldoTddPK saldoTddPK, Double saldo) {
        EntityManagerFactory emf = AbstractFacade.conexion();
        EntityManager em = emf.createEntityManager();
        try {
            if (saldo > 0) {
                long time = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(time);
                //DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                //System.out.println("yyyy/MM/dd HH:mm:ss-> " + dtf.format(LocalDateTime.now()));
                em.getTransaction().begin();
                Query q = em.createNativeQuery("UPDATE saldo_tdd "
                        + " SET "
                        + " saldo = ?,"
                        + " fecha = ?"
                        //+ "fecha = ?, "
                        + " WHERE idorigenp= ?  AND idproducto = ? and idauxiliar = ?");
                q.setParameter(1, saldo);
                q.setParameter(2, timestamp);
                q.setParameter(3, saldoTddPK.getIdorigenp());
                q.setParameter(4, saldoTddPK.getIdproducto());
                q.setParameter(5, saldoTddPK.getIdauxiliar());
                int ac = q.executeUpdate();

                /*int queryUpdateSaldo = em.createNativeQuery("UPDATE saldo_tdd SET saldo=" + saldo + "WHERE "
                        + " idorigenp=" + saldoTddPK.getIdorigenp()
                        + " AND idproducto=" + saldoTddPK.getIdproducto()
                        + " AND idauxiliar=" + saldoTddPK.getIdauxiliar()).executeUpdate();
                 */
                em.getTransaction().commit();
            }

            /*em.getTransaction().begin();
            
              if(queryUpdateSaldo>0){
                return true;
            }*/
        } catch (Exception e) {
            em.close();
            System.out.println("Error al actualizar saldo de la TDD:" + e.getMessage());
        } finally {
            em.close();
        }
    }

    // ERROR AL CONSULTAR SYC TIEMPO AGOTADO
    public boolean errorRetiroDespositoSYC(LoadBalanceResponse.Return loadBalanceResponse, Exception e) {
        System.out.println("Error al consultar SYC, tiempo agotado. " + e.getMessage());
        loadBalanceResponse.setDescription("Connect timed out");
        return false;
    }
    
    
    
    
}
