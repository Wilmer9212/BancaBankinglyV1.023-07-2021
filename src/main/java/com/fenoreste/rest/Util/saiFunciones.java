/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.Util;

import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.InterfaceService.SaiComisionCobranzaServiceLocal;
import com.fenoreste.rest.InterfaceService.SaiSeguroHipotecarioServiceLocal;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import com.fenoreste.rest.entidades.AuxiliaresPK;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.List;
import com.fenoreste.rest.DTO.SeguroComisionesDTO;
import com.fenoreste.rest.DTO.SaiAuxiliarAhorroDTO;
import com.fenoreste.rest.DTO.DistribucionPrestamoDTO;
import com.fenoreste.rest.DTO.SaiComisionCobranzaDTO;
import com.fenoreste.rest.DTO.SaiSeguroHipotecarioDTO;
import com.fenoreste.rest.DTO.SaiAuxiliarPrestamoDTO;
import com.fenoreste.rest.DTO.AuxiliaresDTO;

import java.util.ArrayList;

/**
 *
 * @author wilmer
 */

public class saiFunciones extends AbstractFacade_1<Auxiliares> {

    private SaiComisionCobranzaServiceLocal saiComisionCobranzaService;


    private SaiSeguroHipotecarioServiceLocal saiSeguroHipotecarioService;


    private Convertidor convertidor;

    EntityManager entity;

    SimpleDateFormat fechaHora = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SS");
    SimpleDateFormat fechaSimple = new SimpleDateFormat("dd/MM/yyyy");

    public saiFunciones() {
        super(Auxiliares.class);
    }

    public int crearPoliza(String param) {
        entity = conexion();
        int npol = 0;
        try {
            String query = "SELECT sai_folio_definitivo('" + param + "')";
            npol = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
        } catch (NumberFormatException | NullPointerException e) {
            System.out.println("Error en sai_folio_definitivo de SaiFunciones: " + e);
        }
        entity.close();
        return npol;
    }

    // PROCESA LOS DATOS DE TEMPORAL
    public int procesaTemporal(int idusuario, String sesion, Date fecha, int origen, int tipo, String concepto, String pingreso, boolean condonar, boolean esResumen) {
        entity = conexion();
        EntityTransaction txn = entity.getTransaction();
        int is = 0;
        try {
            String numeroPoliza;
            String generaPoliza = "POL" + convertidor.formatoFecha(fecha, "yyyyMM") + String.format("%06d", origen) + tipo;
            // BEGIN
            txn.begin();
            if (tipo == 1) {
                // Se acutaliza el ticket
                String actualizaTicket = " UPDATE usuarios SET ticket = ticket + 1 WHERE idusuario = ? ";
                Query queryTicket = entity.createNativeQuery(actualizaTicket);
                queryTicket.setParameter(1, idusuario);
                queryTicket.executeUpdate();
                Object pingresoObject = pingreso; // Lo paso a objeto para saber si esta null
                if (pingresoObject == null || pingreso.trim().length() == 0) {
                    // Se crea el folio (idpoliza)
                    String folioDefinitivo = " SELECT sai_folio_definitivo(?) ";
                    Query queryFolio = entity.createNativeQuery(folioDefinitivo);
                    queryFolio.setParameter(1, generaPoliza);
                    numeroPoliza = queryFolio.getSingleResult().toString();
                    // Se actualiza la poliza al usuario
                    pingreso = convertidor.formatoFecha(fecha, "dd/MM/yyyy") + "|" + origen + "|" + tipo + "|" + numeroPoliza;
                    // Se acutaliza pingreso
                    String actualizaPingreso = " UPDATE usuarios SET pingreso = ? WHERE idusuario = ? ";
                    Query queryPingreso = entity.createNativeQuery(actualizaPingreso);
                    queryPingreso.setParameter(1, pingreso);
                    queryPingreso.setParameter(2, idusuario);
                    queryPingreso.executeUpdate();
                } else {
                    // Si existe la poliza se asigna
                    String[] arrPingre = pingreso.split("\\|");
                    numeroPoliza = arrPingre[arrPingre.length - 1];
                }
            } else {
                // Si es tipo 3 se genera una poliza por cada movimiento (idpoliza)
                String folioDefinitivo = " SELECT sai_folio_definitivo(?) ";
                Query query = entity.createNativeQuery(folioDefinitivo);
                query.setParameter(1, generaPoliza);
                numeroPoliza = query.getSingleResult().toString();
            }
            // Se procesa el temporal
            String procesaTemporal = " SELECT sai_temporal_procesa(" + idusuario + ",'" + sesion + "','" + fecha + "'," + origen + "," + numeroPoliza + "," + tipo + ",'" + concepto + "'," + condonar + "," + esResumen + ") ";
            is = Integer.parseInt(entity.createNativeQuery(procesaTemporal).getSingleResult().toString());
            // COMMIT
            txn.commit();
        } catch (Exception e) {
            System.out.println("Error en procesa temporal. " + e.getMessage());
            txn.rollback();
        }
        entity.close();
        return is;
    }

    // DATOS DEL SORTEO DE BUENOS AIRES
    public int saiBuenosairesSorteoEntreAmigos(String idusuario, String sesion, String idorigenc, String periodo, int tipo, String idpoliza) {
        entity = conexion();
        int is = 0;
        try {
            String query = "SELECT sai_buenosaires_sorteo_entre_amigos('" + idusuario + "','" + sesion + "','" + idorigenc + "','" + periodo + "'," + tipo + ",'" + idpoliza + "')";
            is = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.out.println("Error en saiBuenosairesSorteoEntreAmigos de SaiFunciones (Exception) : " + e.getMessage());
            }
        }
        entity.close();
        return is;
    }

    /*SELECT  sai_ahorro_crea_apertura ('{030101,10,10,030101,130,06/01/2014,999,0}');
     Origen, Grupo, Socio, Idorigenp, Idproducto, fecha_hoy, usuario, cero fijo*/
    public AuxiliaresPK saiAhorroCreaApertura(int idorigen, int idgrupo, int idsocio, int idorigenp, int idproducto, Date fechaTrabajo, int idusuario) {
        //convertidor.
        String fecha = convertidor.formatoDeFecha.format(fechaTrabajo);
        entity = conexion();
        AuxiliaresPK auxpk = new AuxiliaresPK();
        auxpk.setIdorigenp(idorigenp);
        auxpk.setIdproducto(idproducto);
        try {
            int idauxiliar;
            String query = "SELECT sai_ahorro_crea_apertura(" + "'" + "{" + idorigen + "," + idgrupo + "," + idsocio + "," + idorigenp + "," + idproducto + "," + fecha + "," + idusuario + ",0}" + "'" + ")";
            idauxiliar = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
            auxpk.setIdauxiliar(idauxiliar);
        } catch (Exception e) {
            auxpk = null;
        }
        entity.close();
        return auxpk;
    }

    // PRESTAMO
    public SaiAuxiliarPrestamoDTO saiAuxiliarPrestamo(int idorigenp, int idproducto, int idauxiliar, Date fechaTrabajo) {
        SaiAuxiliarPrestamoDTO sapDTO = new SaiAuxiliarPrestamoDTO();
        try {
            String[] arrSaiAuxiliar = saiAuxiliar(idorigenp, idproducto, idauxiliar, fechaTrabajo);
            sapDTO.setTipoProducto(Integer.parseInt(arrSaiAuxiliar[0]));
            sapDTO.setFechaUmi(arrSaiAuxiliar[1]);
            sapDTO.setAbonosVencidos(new BigDecimal(arrSaiAuxiliar[2]));
            sapDTO.setDiasVencidos(Integer.parseInt(arrSaiAuxiliar[3]));
            sapDTO.setMontoVencido(new BigDecimal(arrSaiAuxiliar[4]));
            sapDTO.setDiasVencidosIntord(Integer.parseInt(arrSaiAuxiliar[5]));
            sapDTO.setMontoIoTotal(new BigDecimal(arrSaiAuxiliar[6]));
            sapDTO.setMontoBonificacion(new BigDecimal(arrSaiAuxiliar[7]));
            sapDTO.setFechaVencimiento(arrSaiAuxiliar[8]);
            sapDTO.setImCalculado(new BigDecimal(arrSaiAuxiliar[9]));
            sapDTO.setFechaSigAbono(arrSaiAuxiliar[10]);
            sapDTO.setMontoPorVencer(new BigDecimal(arrSaiAuxiliar[11]));
            sapDTO.setIoCalculado(new BigDecimal(arrSaiAuxiliar[12]));
            sapDTO.setEstatusCartera(arrSaiAuxiliar[13]);
            sapDTO.setIdncCalculado(new BigDecimal(arrSaiAuxiliar[14]));
            sapDTO.setMontoImTotal(new BigDecimal(arrSaiAuxiliar[15]));
            sapDTO.setFechaLimite(arrSaiAuxiliar[16]);
            sapDTO.setIvaIoTotal(new BigDecimal(arrSaiAuxiliar[17]));
            sapDTO.setIvaImTotal(new BigDecimal(arrSaiAuxiliar[18]));
            sapDTO.setDifDesctoIo(new BigDecimal(arrSaiAuxiliar[19]));
            sapDTO.setComisionNpCalc(new BigDecimal(arrSaiAuxiliar[20]));
            sapDTO.setComisionNpTotal(new BigDecimal(arrSaiAuxiliar[21]));
            sapDTO.setDiasVencidosCapital(Integer.parseInt(arrSaiAuxiliar[22]));
        } catch (Exception e) {
            System.out.println("Error en saiAuxiliarPrestamo de SaiFunciones. " + e.getMessage());
        }
        return sapDTO;
    }

    // AHORRO
    public SaiAuxiliarAhorroDTO saiAuxiliarAhorro(int idorigenp, int idproducto, int idauxiliar, Date fechaTrabajo) {
        SaiAuxiliarAhorroDTO saaDTO = new SaiAuxiliarAhorroDTO();
        String[] arrSaiAuxiliar = saiAuxiliar(idorigenp, idproducto, idauxiliar, fechaTrabajo);
        saaDTO.setTipoProducto(Integer.parseInt(arrSaiAuxiliar[0]));
        saaDTO.setMontoIo(new BigDecimal(arrSaiAuxiliar[1]));
        saaDTO.setSaldoPromedioDiario(new BigDecimal(arrSaiAuxiliar[2]));
        saaDTO.setRetencion(new BigDecimal(arrSaiAuxiliar[3]));
        saaDTO.setSaldoDiarioAcumulado(new BigDecimal(arrSaiAuxiliar[4]));
        saaDTO.setFechaUpi(arrSaiAuxiliar[5]);
        saaDTO.setGat(new BigDecimal(arrSaiAuxiliar[6]));
        return saaDTO;
    }

    // Ejecuta el sai_auxiliar y lo retorna en un array de string
    public String[] saiAuxiliar(int idorigenp, int idproducto, int idauxiliar, Date fechaTrabajo) {
        entity = conexion();
        String query = "SELECT sai_auxiliar(" + idorigenp + "," + idproducto + "," + idauxiliar + ",'" + fechaTrabajo + "')";
        String saiAuxiliar = entity.createNativeQuery(query).getSingleResult().toString();
        String[] resultadoSaiAuxiliar = saiAuxiliar.split("\\|");
        entity.close();
        return resultadoSaiAuxiliar;
    }

    // Ejecuta el sai_auxiliar y lo retorna en un array de string
    /*
    select monto_interes_para_siguiente_fecha_de_pago(
    <idorigenp>, 
    <idproducto>, 
    <idauxiliar>, 
    date('<fecha de hoy>'), 
    (<montovencido> + <proximo abono>), 
    <tasa de descuento del auxiliar>)
     */
    public String[] monto_interes_para_siguiente_fecha_de_pago(int idorigenp, int idproducto, int idauxiliar, Date fechahoy, BigDecimal montovencidoMasProximoAbono, BigDecimal tasaDeDescuentoDelAuxiliar) {
        entity = conexion();
        String query = "SELECT monto_interes_para_siguiente_fecha_de_pago(" + idorigenp + "," + idproducto + "," + idauxiliar + ",'" + fechahoy + "'," + montovencidoMasProximoAbono + "," + tasaDeDescuentoDelAuxiliar + ");";
        String saiAuxiliar = entity.createNativeQuery(query).getSingleResult().toString();
        String[] resultadoSaiAuxiliar = saiAuxiliar.split("\\|");
        entity.close();
        return resultadoSaiAuxiliar;
    }

// Ejecuta el sai_auxiliar y lo retorna en un array de string
    public String[] fecha_correcta_siguiente_pago(int idorigenp, int idproducto, int idauxiliar, int diasvencidos, int diasinteres) {
        entity = conexion();
        String query = "select fecha_correcta_siguiente_pago(" + idorigenp + "," + idproducto + "," + idauxiliar + ",date(now())," + diasvencidos + "," + diasinteres + ");";
        String saiAuxiliar = entity.createNativeQuery(query).getSingleResult().toString();
        String[] resultadoSaiAuxiliar = saiAuxiliar.split("\\|");
        entity.close();
        return resultadoSaiAuxiliar;
    }

    // Ejecuta el sai_auxiliar y lo retorna en un string
    public String saiAuxiliarOriginal(int idorigenp, int idproducto, int idauxiliar, Date fechaTrabajo) {
        if (idorigenp > 0 && idproducto > 0 && idauxiliar > 0) {
            entity = conexion();
            String query = "SELECT sai_auxiliar(" + idorigenp + "," + idproducto + "," + idauxiliar + ",'" + fechaTrabajo + "')";
            String saiAuxiliar = entity.createNativeQuery(query).getSingleResult().toString();
            entity.close();
            return saiAuxiliar;
        } else {
            return null;
        }
    }

    public Date saiFechaDB(String formato) {
        entity = conexion();
        Date now = null;
        String query;
        if (formato.equals("12")) {
            query = "SELECT TRIM(TO_CHAR(current_timestamp, 'dd/MM/yyyy HH12:mi:ss.SS'))";
        } else {
            query = "SELECT TRIM(TO_CHAR(current_timestamp, 'dd/MM/yyyy HH24:mi:ss.SS'))";
        }
        String fechaDB = entity.createNativeQuery(query).getSingleResult().toString();
        try {
            now = fechaHora.parse(fechaDB);
        } catch (ParseException ex) {
            System.out.println("Error al obtener fecha del servidor de SaiFunciones: " + ex);
        }
        entity.close();
        return now;
    }
    
    // AGREGADO POR FREDY 11/06/2021 PARA SAN NICOLAS
    public int saiJavaMovilAdelantoHipotecario( int idorigenp, int idproducto, int idauxiliar, Double montoDepositado, Double montoMaximoCubrir, String Columna){
        if (idorigenp > 0 && idproducto > 0 && idauxiliar > 0){
            entity = conexion();
            String query = "select "+ Columna + " "
                         + "  from sai_java_movil_adelanto_hipotecarios (" + idorigenp + "," 
                                                                           + idproducto + "," 
                                                                           + idauxiliar + "," 
                                                                           + montoDepositado + "," 
                                                                           + montoMaximoCubrir + ");"; 
            String resultado = entity.createNativeQuery(query).getSingleResult().toString();
            int ResultadoColumna = Integer.parseInt(resultado);
            entity.close();
            return ResultadoColumna;
        } else {
            return 0;
        }
    }
    
    public String conceptoPoliza (Integer idorigenc, String periodo, Short idtipo, Integer idpoliza, Short cargoabono, int tipoproducto, String conceptoPropia, String conceptoTerceros, AuxiliaresPK opa, Short tipomov){
        entity = conexion();
        String consulta;
        String concepto = "";
        String query = "";
        //int Resultado = 0;
        try {
            query = "SELECT COUNT(*) FROM sai_java_movil_concepto (" + idorigenc           + ", '" 
                                                                     + periodo             + "', " 
                                                                     + idtipo              + ", "
                                                                     + idpoliza            + ", "
                                                                     + opa.getIdorigenp()  + ", " 
                                                                     + opa.getIdproducto() + ", "
                                                                     + opa.getIdauxiliar() + ", "
                                                                     + cargoabono          + ", "
                                                                     + tipoproducto        + ", '"
                                                                     + conceptoPropia      + "', '"
                                                                     + conceptoTerceros    + "');";
            int conteo = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
            
            if (conteo > 0) {
            /*
                consulta = "SELECT (CASE WHEN concepto  = '" + conceptoPropia + "' AND " + cargoabono + " = 1 AND " + tipoproducto + " = 0 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'deposito_cuenta_propia') "
                         + "             WHEN concepto  = '" + conceptoPropia + "' AND " + cargoabono + " = 1 AND " + tipoproducto + " = 2 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'abono_cuenta_propia') "
                         + "             WHEN concepto  = '" + conceptoPropia + "' AND " + cargoabono + " = 0 AND " + tipoproducto + " = 0 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'retiro_cuenta_propia') " 
                         + "             WHEN concepto  = '" + conceptoPropia + "' AND " + cargoabono + " = 0 AND " + tipoproducto + " = 2 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'cargo_cuenta_propia') "
                         + "             WHEN concepto = '" + conceptoTerceros + "' AND " + cargoabono + " = 1 AND " + tipoproducto + " = 0 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'deposito_cuenta_tercero') " 
                         + "             WHEN concepto = '" + conceptoTerceros + "' AND " + cargoabono + " = 1 AND " + tipoproducto + " = 2 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'abono_cuenta_tercero') " 
                         + "             WHEN concepto = '" + conceptoTerceros + "' AND " + cargoabono + " = 0 AND " + tipoproducto + " = 0 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'retiro_cuenta_tercero') " 
                         + "             WHEN concepto = '" + conceptoTerceros + "' AND " + cargoabono + " = 0 AND " + tipoproducto + " = 2 "
                         + "                  THEN (select dato2 from tablas where idtabla = 'siscoop_banca_movil' AND idelemento = 'cargo_cuenta_tercero') " 
                         + "        END) "
                         + "  FROM polizas "
                         + " WHERE idorigenc = " + idorigenc + " AND periodo = '" + periodo + "' AND idtipo = " + idtipo + " AND idpoliza = " + idpoliza 
                         + "   AND (concepto = '" + conceptoPropia + "' OR concepto = '" + conceptoTerceros + "' )";
                */
            
                consulta = "SELECT * FROM sai_java_movil_concepto (" + idorigenc           + ", '" 
                                                                     + periodo             + "', " 
                                                                     + idtipo              + ", "
                                                                     + idpoliza            + ", "
                                                                     + opa.getIdorigenp()  + ", " 
                                                                     + opa.getIdproducto() + ", "
                                                                     + opa.getIdauxiliar() + ", "
                                                                     + cargoabono          + ", "
                                                                     + tipoproducto        + ", '"
                                                                     + conceptoPropia      + "', '"
                                                                     + conceptoTerceros    + "');";
                concepto = entity.createNativeQuery(consulta).getSingleResult().toString();
            } else {
                entity.close();
                return "";
            }
        } catch (Exception ex){
            System.out.println("Error al obetener el concepto de la poliza de SaiFunciones: " + ex);
            entity.close();
            return "";
        }
        entity.close();
        return concepto;
    }

    public Date saiFechaTrabajo(int idorigen) {
        entity = conexion();
        Date fechaTraajo;
        try {
            String query = "SELECT TRIM(TO_CHAR((SELECT fechatrabajo FROM origenes limit 1), 'dd/MM/yyyy HH24:mi:ss.SS'))";
            String fechaDB = entity.createNativeQuery(query).getSingleResult().toString();
            fechaTraajo = fechaHora.parse(fechaDB);
        } catch (ParseException ex) {
            fechaTraajo = new Date("01/01/1900 00:00:00.00");
        }
        entity.close();
        return fechaTraajo;
    }

    public DistribucionPrestamoDTO distribucionMonto(int idorigenp, int idproducto, int idauxiliar, Date fechaTrabajo, BigDecimal monto) {
        entity = conexion();
        DistribucionPrestamoDTO dpDTO = new DistribucionPrestamoDTO();
        String query = "SELECT sai_distribucion_prestamo(" + idorigenp + "," + idproducto + "," + idauxiliar + ",'" + fechaTrabajo + "'," + monto + ")";
        String montoDis = entity.createNativeQuery(query).getSingleResult().toString();
        String[] arrMonto = montoDis.split("\\|");
        entity.close();
        // Se obtienen los valores del arreglo y se asignan a las varaibles
        dpDTO.setSegHip(new BigDecimal(arrMonto[0]));
        dpDTO.setComCob(new BigDecimal(arrMonto[1]));
        dpDTO.setIm(new BigDecimal(arrMonto[2]));
        dpDTO.setIvaIm(new BigDecimal(arrMonto[3]));
        dpDTO.setIo(new BigDecimal(arrMonto[4]));
        dpDTO.setIvaIo(new BigDecimal(arrMonto[5]));
        dpDTO.setaCapital(new BigDecimal(arrMonto[6]));
        return dpDTO;
    }

    // Obtiene el saldo minimo que debe tener
    public  String sai_limite_de_saldo_minimo(int idproducto, String datos) {
        entity = conexion();
        String query = "select sai_limite_de_saldo_minimo (" + idproducto + "," + datos + ")";
        String saldoMinimo = entity.createNativeQuery(query).getSingleResult().toString();
        entity.close();
        return saldoMinimo;
    }

    // Obtiene el saldo maximo que debe tener
    public String sai_limite_de_saldo_maximo(int idproducto, String datos) {
        entity = conexion();
        String query = "select sai_limite_de_saldo_minimo (" + idproducto + "," + datos + ")";
        String saldoMaximo = entity.createNativeQuery(query).getSingleResult().toString();
        entity.close();
        return saldoMaximo;
    }

    // PUNTOMANIA DE BUENOS AIRES
    public int saiPromocionPuntosSaicoop(int idusuario, String sesion, String fechaTrabajo, int idorigenc, int idtipo, int idpoliza) {
        entity = conexion();
        int is = 0;
        try {
            String query = "SELECT sai_promocion_puntos_saicoop(" + idusuario + ",'" + sesion + "','" + fechaTrabajo + "'," + idorigenc + "," + idtipo + "," + idpoliza + ")";
            is = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.out.println("Error en saiPromocionPuntosSaicoop de SaiFunciones: " + e.getMessage());
            }
        }
        entity.close();
        return is;
    }

    // PUNTOMANIA DE BUENOS AIRES
    /*public SaiPromocionPuntosSaicoopDTO saiPromocionPuntosSaicoopImprimeTicket(int idusuario, String sesion, String fechaTrabajo, int ticket) {
        entity = conexion();
        SaiPromocionPuntosSaicoopDTO sppsDTO = new SaiPromocionPuntosSaicoopDTO();
        String query = "SELECT sai_promocion_puntos_saicoop_imprime_ticket(" + idusuario + ",'" + sesion + "','" + fechaTrabajo + "'," + ticket + ")";
        String saiPuntos = entity.createNativeQuery(query).getSingleResult().toString();
        String[] arrMonto = saiPuntos.split(",");
        // Se obtienen los valores del arreglo y se asignan a las varaibles
        sppsDTO.setOgs_nombre(arrMonto[0].substring(1, arrMonto[0].length()).replace("\"", ""));
        sppsDTO.setPuntos_ganados(arrMonto[1].replace("\"", ""));
        sppsDTO.setPuntos_globales(arrMonto[2].replace("\"", ""));
        sppsDTO.setDescalificacion(arrMonto[3].replace("\"", ""));
        sppsDTO.setMotivodesc(arrMonto[4].substring(0, arrMonto[4].length() - 1).replace("\"", ""));
        entity.close();
        return sppsDTO;
    }*/

    // Eliminar pingreso
    public void eliminaPingreso(int idusuario) {
        entity = conexion();
        EntityTransaction txn = entity.getTransaction();
        try {
            txn.begin();
            // Se elimina pingreso
            String eliminaPingreso = " UPDATE usuarios SET pingreso = null WHERE idusuario = ? ";
            Query queryEliminaPingreso = entity.createNativeQuery(eliminaPingreso);
            queryEliminaPingreso.setParameter(1, idusuario);
            queryEliminaPingreso.executeUpdate();
            txn.commit();
        } catch (Exception e) {
            System.out.println("Error en eliminaPingreso de SaiFunciones: " + e.getMessage());
        }
        entity.close();
    }

    // Distribuye el monto que viene de la funcion sai_distribucion_prestamo, en base a esas cantidades se reparte el monto entre seguros y comisiones
    public List<SeguroComisionesDTO> distribucionSegurosComisionCobranza(AuxiliaresDTO auxiliaresDTO, Date fechaTrabajo, DistribucionPrestamoDTO distribucion, int diasvencidos) {
        List<SeguroComisionesDTO> segurosComisionesDTO = new ArrayList<>(0);
        Double montoDistribuir = distribucion.getSegHip().doubleValue();
        // SEGUROS -------------------------------------------------------------
        if (distribucion.getSegHip().doubleValue() > 0) {
            List<SaiSeguroHipotecarioDTO> seguroHipotecario = saiSeguroHipotecarioService.buscarPagoSeguroHipotecario(auxiliaresDTO.getAuxiliaresPK().getIdorigenp(), auxiliaresDTO.getAuxiliaresPK().getIdproducto(), auxiliaresDTO.getAuxiliaresPK().getIdauxiliar(), fechaTrabajo);
            for (SaiSeguroHipotecarioDTO saiSeguroHipotecarioDTO : seguroHipotecario) {
                SeguroComisionesDTO seguroComision = new SeguroComisionesDTO();
                seguroComision.setIdorigenp(saiSeguroHipotecarioDTO.getSaiSeguroHipotecarioPK().getIdorigenpr());
                seguroComision.setIdproducto(saiSeguroHipotecarioDTO.getSaiSeguroHipotecarioPK().getIdproductor());
                seguroComision.setIdauxiliar(saiSeguroHipotecarioDTO.getSaiSeguroHipotecarioPK().getIdauxiliarr());
                // Total a pagar con iva
                Double totalApagarConIva = Redondear((saiSeguroHipotecarioDTO.getApagar() + saiSeguroHipotecarioDTO.getIvaapagar()), 2);
                if (saiSeguroHipotecarioDTO.getTasa_iva() > 0) {
                    // Si el total a cubir del seguro es menor o igual al monto restante
                    if (totalApagarConIva <= montoDistribuir) {
                        seguroComision.setApagar(new BigDecimal(saiSeguroHipotecarioDTO.getApagar()).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        seguroComision.setIvaapagar(new BigDecimal(saiSeguroHipotecarioDTO.getIvaapagar()).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        montoDistribuir = montoDistribuir - totalApagarConIva;
                    } else {
                        // Redondear a dos decimales, parte proporcional del resto del monto  ----
                        Double ivaPagar = (montoDistribuir / ((saiSeguroHipotecarioDTO.getTasa_iva() / 100) + 1));
                        // Si el iva a cubrir es menor al monto restante
                        if (ivaPagar <= montoDistribuir) {
                            montoDistribuir = montoDistribuir - ivaPagar;
                            seguroComision.setApagar(new BigDecimal(montoDistribuir).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                            seguroComision.setIvaapagar(new BigDecimal(ivaPagar).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        } else {
                            seguroComision.setApagar(new BigDecimal(0.00).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                            seguroComision.setIvaapagar(new BigDecimal(montoDistribuir).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                            montoDistribuir = 0.00;
                        }
                    }
                } else // Si el monto total a cubrir es menor al monto restante
                 if (totalApagarConIva <= montoDistribuir) {
                        seguroComision.setApagar(new BigDecimal(saiSeguroHipotecarioDTO.getApagar()).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        seguroComision.setIvaapagar(new BigDecimal(saiSeguroHipotecarioDTO.getIvaapagar()).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        montoDistribuir = montoDistribuir - totalApagarConIva;
                    } else {
                        seguroComision.setApagar(new BigDecimal(montoDistribuir).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        seguroComision.setIvaapagar(new BigDecimal(0.00).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                        montoDistribuir = 0.00;
                    }
                // Si a pagar o iva a pagar son mayor a cero se inserta
                if (seguroComision.getApagar().doubleValue() > 0 || seguroComision.getIvaapagar().doubleValue() > 0) {
                    segurosComisionesDTO.add(seguroComision);
                }
            }
        }
        // COMISIONES ----------------------------------------------------------
        if (distribucion.getComCob().doubleValue() > 0) {
            // Si aun queda monoto y sai_cual_comision es 2 se procesa
            if (saiCualComision(auxiliaresDTO.getAuxiliaresPK().getIdproducto(), diasvencidos) == 2) {
                // Datos para calcular la comision
                SaiComisionCobranzaDTO saiComisionCobranzaDTO = saiComisionCobranzaService.buscaComisionCobranza(auxiliaresDTO.getAuxiliaresPK().getIdorigenp(), auxiliaresDTO.getAuxiliaresPK().getIdproducto(), auxiliaresDTO.getAuxiliaresPK().getIdauxiliar());
                if (saiComisionCobranzaDTO != null) {
                    if (saiComisionCobranzaDTO.getTasaComision() > 0 && saiComisionCobranzaDTO.getMontoComision() > 0) {
                        List<String> comisiones = saiComisionCobranza(distribucion.getComCob().doubleValue(), saiComisionCobranzaDTO.getTasaComision(), diasvencidos, auxiliaresDTO.getIdorigen(), auxiliaresDTO.getIdgrupo(), auxiliaresDTO.getIdsocio(), auxiliaresDTO.getAuxiliaresPK().getIdorigenp(), auxiliaresDTO.getAuxiliaresPK().getIdproducto(), auxiliaresDTO.getAuxiliaresPK().getIdauxiliar());
                        for (String comision : comisiones) {
                            String[] datos = comision.split(",");
                            String monotoParaComision = datos[0]; // Monto asignado a la comision
                            String productoComision = datos[1]; // Producto asigando a comision
                            // Se crea el DTO para almacenar la lista de comisiones
                            SeguroComisionesDTO comisionCobranza = new SeguroComisionesDTO();
                            comisionCobranza.setIdorigenp(0);
                            comisionCobranza.setIdproducto(Integer.parseInt(productoComision));
                            comisionCobranza.setIdauxiliar(0);
                            comisionCobranza.setApagar(new BigDecimal(monotoParaComision).setScale(2, BigDecimal.ROUND_HALF_EVEN));
                            comisionCobranza.setIvaapagar(new BigDecimal(0.00).setScale(2, BigDecimal.ROUND_HALF_EVEN)); // Iva es 0 y que no tiene
                            // Agregarlo al arreglo
                            segurosComisionesDTO.add(comisionCobranza);
                        }
                    }
                }

            }
        }
        // Retorna los prodcutos con sus montos
        return segurosComisionesDTO;
    }

    // -------------------------------------------------------------------------
    // --- SAI_CUAL_COMISION
    private int saiCualComision(int idproducto, int diasvencidos) {
        entity = conexion();
        String query = "SELECT sai_cual_comision(" + idproducto + "," + diasvencidos + ")";
        int cualComision = Integer.parseInt(entity.createNativeQuery(query).getSingleResult().toString());
        entity.close();
        return cualComision;
    }

    // --- SAI_COMISION_COBRANZA
    private List<String> saiComisionCobranza(Double montoComision, Double tasaComision, int diasVencidos, int idorigen, int idgrupo, int idsocio, int idorigenp, int idproducto, int idauxiliar) {
        entity = conexion();
        String query = "SELECT montoc || ',' || producto FROM (SELECT ROUND((" + montoComision + "/" + tasaComision + ") * (tasa * 1.000000), 2) AS montoc, producto "
                + " FROM sai_comision_cobranza(" + diasVencidos + ", " + idorigen + ", " + idgrupo + ", " + idsocio + ", " + idorigenp + ", " + idproducto + ", " + idauxiliar + ")) AS z WHERE montoc > 0";
        List<String> comisionCobranza = entity.createNativeQuery(query).getResultList();
        entity.close();
        return comisionCobranza;
    }
    // -------------------------------------------------------------------------

    // Redondear Double
    public static double Redondear(double numero, int digitos) {
        int cifras = (int) Math.pow(10, digitos);
        return Math.rint(numero * cifras) / cifras;
    }

}
