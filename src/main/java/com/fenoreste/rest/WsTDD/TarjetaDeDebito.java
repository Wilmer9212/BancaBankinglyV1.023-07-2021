package com.fenoreste.rest.WsTDD;


import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetas1;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetasPK1;
import javax.persistence.EntityManager;
import com.syc.ws.endpoint.siscoop.BalanceQueryResponseDto;
import com.syc.ws.endpoint.siscoop.DoWithdrawalAccountResponse;
import com.syc.ws.endpoint.siscoop.LoadBalanceResponse;
import java.sql.Timestamp;
import javax.persistence.Query;
import wssyctdd.SiscoopTDD;




/**
 *
 * @author Elliot
 */
public class TarjetaDeDebito {
    // CONSULTA Y ACTUALIZA EL SALDO DE LA TarjetaDeDebito
      public Tablas productoTddWS(EntityManager em) {
        try {
            TablasPK pkt = new TablasPK("identificador_uso_tdd", "activa");
            Tablas tb = em.find(Tablas.class, pkt);
            if (tb != null) {
                return tb;
            } else {
                return null;
            }
        } catch (Exception e) {
            System.out.println("No existe producto activo para tdd:" + e.getMessage());
        } 
        return null;
    }

    // PRODUCTO VALIDO PARA LA TDD
    public Tablas productoTddwebservice(EntityManager em) {
       Tablas tabla=null;
        System.out.println("Llegando a buscar el producto para Tarjeta de debito....");
        try {
            EntityManager d=AbstractFacade.conexion();
            // Producto de la tdd
            
            TablasPK tablasPK = new TablasPK("bankingly_banca_movil", "producto_tdd");
            tabla = d.find(Tablas.class, tablasPK);
            
        } catch (NumberFormatException e) {            
            System.out.println("Error en consultar producto en producto_para_webservice de TarjetaDeDebito." + e.getMessage());
            return tabla;
        }
        return tabla;
    }
    
    
    public WsSiscoopFoliosTarjetas1 buscaTarjetaTDD(int idorigenp, int idproducto, int idauxiliar,EntityManager em) {
        WsSiscoopFoliosTarjetasPK1 foliosPK1 = new WsSiscoopFoliosTarjetasPK1(idorigenp, idproducto, idauxiliar);
        WsSiscoopFoliosTarjetas1 wsSiscoopFoliosTarjetas = new WsSiscoopFoliosTarjetas1();
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
            wsSiscoopFoliosTarjetas = (WsSiscoopFoliosTarjetas1) query.getSingleResult();
            if (wsSiscoopFoliosTarjetas != null) {
                wsSiscoopFoliosTarjetas.setActiva(wsSiscoopFoliosTarjetas.getActiva());
                wsSiscoopFoliosTarjetas.setAsignada(wsSiscoopFoliosTarjetas.getAsignada());
                wsSiscoopFoliosTarjetas.setBloqueada(wsSiscoopFoliosTarjetas.getBloqueada());
                wsSiscoopFoliosTarjetas.setWsSiscoopFoliosTarjetasPK(foliosPK1);
            }
        } catch (Exception e) {
            System.out.println("Error en buscaTarjetaTDD de WsSiscoopFoliosTarjetas: " + e.getMessage());  
            return wsSiscoopFoliosTarjetas;
        }
        return wsSiscoopFoliosTarjetas;
    }
    
    public BalanceQueryResponseDto saldoTDD(WsSiscoopFoliosTarjetasPK1 foliosPK,EntityManager em) {     
        BalanceQueryResponseDto response = new BalanceQueryResponseDto();
        WsSiscoopFoliosTarjetas1 tarjeta = em.find(WsSiscoopFoliosTarjetas1.class, foliosPK);
        try {
            System.out.println("Estatus de la tarjeta de debito:" + tarjeta.getActiva());
            if (tarjeta.getActiva()) {
                
                
                /*response.setAvailableAmount(20);                     
                response.setCode(1);
                response.setDescription("activa");*/
                response = conexionSiscoop().getSiscoop().getBalanceQuery(tarjeta.getIdtarjeta());
                
            } else {
                response.setDescription("La tarjeta esta inactiva: " + tarjeta.getIdtarjeta());
            }
        } catch (Exception e) {
            System.out.println("Error al buscar Saldo tdd:" + e.getMessage());
            response.setDescription("Tarjeta Inactiva");
            return response;
        } 
        return response;
    }

    public DoWithdrawalAccountResponse.Return retiroTDD(WsSiscoopFoliosTarjetas1 tarjeta, Double monto) {
        DoWithdrawalAccountResponse.Return doWithdrawalAccountResponse = new DoWithdrawalAccountResponse.Return();
        System.out.println("WS...Restiro");
        try {
            doWithdrawalAccountResponse.setBalance(200);
            doWithdrawalAccountResponse.setCode(1);
            //doWithdrawalAccountResponse=conexionSiscoop().getSiscoop().doWithdrawalAccount(tarjeta.getIdtarjeta(), monto);
        } catch (Exception e) {         
            return doWithdrawalAccountResponse;
        }
        return doWithdrawalAccountResponse;
    }

    public SiscoopTDD conexionSiscoop() {
//        EntityManagerFactory emf = AbstractFacade.conexion();
   EntityManager em = AbstractFacade.conexion();//      EntityManager em = emf.createEntityManager();
        SiscoopTDD conexionWSTDD = null;
        try {
            //Obtengo el usuario y la contraseña para conectar al WS de Alestra        
            TablasPK tablasPK = new TablasPK("siscoop_banca_movil", "wsdl_parametros");
            Tablas parametros = em.find(Tablas.class, tablasPK);
            if (parametros != null) {
                System.out.println("Conectando ws ALestra....");
                conexionWSTDD = new SiscoopTDD(parametros.getDato1(), parametros.getDato2());
                
            }
        } catch (Exception e) {     
            System.out.println("No existen parametros para conexion:" + e.getMessage());
        } 
        return conexionWSTDD;
    }

    public void actualizarSaldoTDD(SaldoTddPK saldoTddPK, Double saldo,EntityManager em) {
  
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
            e.printStackTrace();
            System.out.println("Error al actualizar saldo de la TDD:" + e.getMessage());
        }
    }

    // ERROR AL CONSULTAR SYC TIEMPO AGOTADO
    public boolean errorRetiroDespositoSYC(LoadBalanceResponse.Return loadBalanceResponse, Exception e) {
        System.out.println("Error al consultar SYC, tiempo agotado. " + e.getMessage());
        loadBalanceResponse.setDescription("Connect timed out");
        return false;
    }
    
    

}
