/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.ResponseDTO.BackendOperationResultDTO;
import com.fenoreste.rest.ResponseDTO.TransactionToOwnAccountsDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.WsTDD.TarjetaDeDebito;
import static com.fenoreste.rest.dao.FacadeProductos.ruta;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.AuxiliaresPK;
import com.fenoreste.rest.entidades.Productos_bankingly;
import com.fenoreste.rest.entidades.Origenes;
import com.fenoreste.rest.entidades.Procesa_pago_movimientos;
import com.fenoreste.rest.entidades.Productos;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.Transferencias;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetas1;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetasPK1;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.syc.ws.endpoint.siscoop.BalanceQueryResponseDto;
import com.syc.ws.endpoint.siscoop.DoWithdrawalAccountResponse;
import com.syc.ws.endpoint.siscoop.LoadBalanceResponse;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 *
 * @author Elliot
 */
public abstract class FacadeTransaction<T> {

    EntityManagerFactory emf;
    TarjetaDeDebito WSTDD = new TarjetaDeDebito();

    public FacadeTransaction(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }

    //El identificador de trasnferencia me dice 1=entre mis cuentas 2=a terceros 3=Pago a prestamos
    public BackendOperationResultDTO transferencias(TransactionToOwnAccountsDTO transactionOWN, int identificadorTransferencia) {
        EntityManager em = emf.createEntityManager();
        Date hoy = new Date();

        System.out.println("TransactionFecha:" + transactionOWN.getValueDate());
        BackendOperationResultDTO backendResponse = new BackendOperationResultDTO();
        backendResponse.setBackendCode("2");
        backendResponse.setBackendMessage("Incorrecto");
        backendResponse.setBackendReference(null);
        backendResponse.setIsError(true);
        backendResponse.setTransactionIdenty("0");
        int idorigenp = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(1, 6));
        int idproducto = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(6, 11));
        int idauxiliar = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(11, 19));

        System.out.println("" + idorigenp + "-" + idproducto + idauxiliar);

        boolean retiro = false;
        boolean bandera = false;
        //Identifico la caja que esta usando los WS 
        String mss = "";

        //Si la caja es CSN validamos y el retiro es de TDD
        if (caja().contains("SANNICOLAS")) {
            try {
                LoadBalanceResponse.Return loadBalanceResponse = new LoadBalanceResponse.Return();
                TablasDTO tablaProductoTDD = WSTDD.productoTddwebservice();
                System.out.println("ProductoTDD:" + tablaProductoTDD.getDato2());

                if (Integer.parseInt(tablaProductoTDD.getDato2()) == idproducto) {
                    bandera = true;
                    WsSiscoopFoliosTarjetas1 tarjetas = WSTDD.buscaTarjetaTDD(idorigenp, idproducto, idauxiliar);
                    System.out.println("Tarjeta Siscoop:" + WSTDD);
                    DoWithdrawalAccountResponse.Return doWithdrawalAccountResponse = new DoWithdrawalAccountResponse.Return();
                    if (tarjetas.getWsSiscoopFoliosTarjetasPK() != null) {
                        if (tarjetas.getActiva()) {
                            try {

                                String message = "";
                                //Aplicamos validaciones segun sea tipo de transferencia(En cada metodo se valdia que la TDD tenga saldo > al que se intenta transferir)
                                //Cuentas propias
                                if (identificadorTransferencia == 1) {
                                    message = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                }
                                //Terceros dentro de la entidad
                                if (identificadorTransferencia == 2) {
                                    message = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                    System.out.println("Message:" + message);
                                }
                                //Pago de prestamo propio ---Falta pago de prestamo tercero
                                if (identificadorTransferencia == 3) {
                                    message = validarPagoAPrestamos(transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                    System.out.println("Message:" + message);
                                }

                                if (message.toUpperCase().contains("EXITO")) {
                                    //ejecutamos el retiro de la TDD
                                    doWithdrawalAccountResponse = WSTDD.retiroTDD(tarjetas, transactionOWN.getAmount());
                                    if (doWithdrawalAccountResponse.getCode() == 0) {
                                        mss = "ERROR AL PROCESAR RETIRO DE WSTDD";
                                        //Existe Error
                                        retiro = false;
                                    } else {
                                        retiro = true;
                                        mss = "VALIDADO CON EXITO";
                                    }
                                } else {
                                    mss = message;
                                    retiro = false;
                                }

                            } catch (Exception e) {
                                System.out.println("Error al validar TDD:" + e.getMessage());
                            }
                        } else {
                            System.out.println("Tarjeta inactiva");
                        }
                    } else {
                        System.out.println("Sin folios registrados");
                    }
                }

            } catch (Exception e) {
                System.out.println("Error producido en buscar tjeta:" + e.getMessage());

            }

        }
        backendResponse.setBackendMessage(mss);

        //Si no es TDD pasa directo hasta aca
        //Si es una transferencia entre mis cuentas
        if (identificadorTransferencia == 1 & retiro == false & bandera == false) {
            //Valido la trasnferencia y devuelvo el mensaje que se produce
            String messageBackend = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es una transferencia terceros dentro de la entidad
        if (identificadorTransferencia == 2 & retiro == false & bandera == false) {
            String messageBackend = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es pago a un prestamo
        if (identificadorTransferencia == 3 & retiro == false & bandera == false) {
            String backendMessage = validarPagoAPrestamos(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            backendResponse.setBackendMessage(backendMessage);
        }
        System.out.println("backendMessage:" + backendResponse.getBackendMessage());
        try {
            if (backendResponse.getBackendMessage().toUpperCase().contains("EXITO")) {
                Transferencias transaction = new Transferencias();
                transaction.setSubtransactiontypeid(transactionOWN.getSubTransactionTypeId());
                transaction.setCurrencyid(transactionOWN.getCurrencyId());
                transaction.setValuedate(transactionOWN.getValueDate());
                transaction.setTransactiontypeid(transactionOWN.getTransactionTypeId());
                transaction.setTransactionstatusid(transactionOWN.getTransactionStatusId());
                transaction.setClientbankidentifier(transactionOWN.getClientBankIdentifier());
                transaction.setDebitproductbankidentifier(transactionOWN.getDebitProductBankIdentifier());
                transaction.setDebitproducttypeid(transactionOWN.getDebitProductTypeId());
                transaction.setDebitcurrencyid(transactionOWN.getDebitCurrencyId());
                transaction.setCreditproductbankidentifier(transactionOWN.getCreditProductBankIdentifier());
                transaction.setCreditproducttypeid(transactionOWN.getCreditProductTypeId());
                transaction.setCreditcurrencyid(transactionOWN.getCreditCurrencyId());
                transaction.setAmount(transactionOWN.getAmount());
                transaction.setNotifyto(transactionOWN.getNotifyTo());
                transaction.setNotificationchannelid(transactionOWN.getNotificationChannelId());
                transaction.setTransactionid(transactionOWN.getTransactionId());
                transaction.setDestinationname(transactionOWN.getDestinationName());
                transaction.setDestinationbank(transactionOWN.getDestinationBank());
                transaction.setDescription(transactionOWN.getDescription());
                transaction.setBankroutingnumber(transactionOWN.getBankRoutingNumber());
                transaction.setSourcename(transactionOWN.getSourceName());
                transaction.setSourcebank(transactionOWN.getSourceBank());
                transaction.setRegulationamountexceeded(transactionOWN.isRegulationAmountExceeded());
                transaction.setSourcefunds(transactionOWN.getSourceFunds());
                transaction.setDestinationfunds(transactionOWN.getDestinationFunds());
                transaction.setTransactioncost(transactionOWN.getTransactionCost());
                transaction.setTransactioncostcurrencyid(transactionOWN.getTransactionCostCurrencyId());
                transaction.setExchangerate(transactionOWN.getExchangeRate());
                transaction.setDestinationdocumentid_documentnumber(transactionOWN.getDestinationDocumentId().getDocumentNumber());
                transaction.setDestinationdocumentid_documenttype(transactionOWN.getDestinationDocumentId().getDocumentType());
                transaction.setSourcedocumentid_documentnumber(transactionOWN.getSourceDocumentId().getDocumentNumber());
                transaction.setSourcedocumentid_documenttype(transactionOWN.getSourceDocumentId().getDocumentType());
                transaction.setUserdocumentid_documentnumber(transactionOWN.getUserDocumentId().getDocumentNumber());
                transaction.setUserdocumentid_documenttype(transactionOWN.getUserDocumentId().getDocumentType());
                transaction.setFechaejecucion(hoy);

                boolean banderaEstatusTransferencia = false;

                //Obtengo los productos origen y destino
                //Origen
                String origenP = "SELECT * FROM auxiliares WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "'";
                Query queryOrigen = em.createNativeQuery(origenP, Auxiliares.class);
                Auxiliares aOrigen = (Auxiliares) queryOrigen.getSingleResult();
                //Destino
                String destinoP = "SELECT * FROM auxiliares WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "'";
                Query queryDestino = em.createNativeQuery(destinoP, Auxiliares.class);
                Auxiliares aDestino = (Auxiliares) queryDestino.getSingleResult();

                //Obtengo el producto 
                Productos prDestino = em.find(Productos.class, aDestino.getAuxiliaresPK().getIdproducto());

                //Procesa_pago_movimientos procesaDestino = new Procesa_pago_movimientos();
                Procesa_pago_movimientos procesaOrigen = new Procesa_pago_movimientos();
                
                //Obtener los datos para procesar la transaccion
                long time = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(time);
                Query sesion = em.createNativeQuery("select text(pg_backend_pid())||'-'||trim(to_char(now(),'ddmmyy'))");
                String sesionc = String.valueOf(sesion.getSingleResult());
                int rn = (int) (Math.random() * 999999 + 1);
                //Obtener HH:mm:ss.microsegundos
                String fechahhmmss="";
                String fechaArray[] = timestamp.toString().substring(0, 10).split("-");
                String fReal = fechaArray[2] + "/" + fechaArray[1] + "/" + fechaArray[0];
                String referencia = String.valueOf(rn) + "" + String.valueOf(transaction.getSubtransactiontypeid()) + "" + String.valueOf(transaction.getTransactiontypeid() + "" + fReal.replace("/", ""));

                //Insertamos a la tabla donde se obtienen los datos a procesar
                //Origen
                procesaOrigen.setAuxiliaresPK(aOrigen.getAuxiliaresPK());
                procesaOrigen.setFecha(timestamp);
                procesaOrigen.setIdusuario(999);
                procesaOrigen.setSesion(sesionc);
                procesaOrigen.setReferencia(referencia);
                procesaOrigen.setIdorigen(aOrigen.getIdorigen());
                procesaOrigen.setIdgrupo(aOrigen.getIdgrupo());
                procesaOrigen.setIdsocio(aOrigen.getIdsocio());
                procesaOrigen.setCargoabono(0);
                procesaOrigen.setMonto(transaction.getAmount());
                procesaOrigen.setIva(Double.parseDouble(aOrigen.getIva().toString()));
                procesaOrigen.setTipo_amort(Integer.parseInt(String.valueOf(aOrigen.getTipoamortizacion())));
                procesaOrigen.setAplicado(true);
                procesaOrigen.setSai_aux("");

                em.getTransaction().begin();
                em.persist(procesaOrigen);
                em.getTransaction().commit();

                em.clear();

                //Destino
                procesaOrigen.setAuxiliaresPK(aDestino.getAuxiliaresPK());
                procesaOrigen.setFecha(timestamp);
                procesaOrigen.setIdusuario(999);
                procesaOrigen.setSesion(sesionc);
                procesaOrigen.setReferencia(referencia);
                procesaOrigen.setIdorigen(aDestino.getIdorigen());
                procesaOrigen.setIdgrupo(aDestino.getIdgrupo());
                procesaOrigen.setIdsocio(aDestino.getIdsocio());
                procesaOrigen.setCargoabono(1);
                procesaOrigen.setMonto(transaction.getAmount());
                procesaOrigen.setIva(Double.parseDouble(aDestino.getIva().toString()));
                procesaOrigen.setTipo_amort(Integer.parseInt(String.valueOf(aDestino.getTipoamortizacion())));
                procesaOrigen.setAplicado(true);
                procesaOrigen.setSai_aux("");
                em.getTransaction().begin();
                em.persist(procesaOrigen);
                em.getTransaction().commit();

                String mensajeBackendResult="";
                //Si la cuenta a la que se esta transfiriendo es un prestamo
                if (prDestino.getTipoproducto() == 2) {
                    //Obtengo los datos(Seguro hipotecario,comisones cobranza,interes ect.)
                    String distribucion = "SELECT sai_distribucion_prestamo(" + aDestino.getAuxiliaresPK().getIdorigenp() + "," + aDestino.getAuxiliaresPK().getIdproducto() + "," + aDestino.getAuxiliaresPK().getIdauxiliar() + ",'" + fReal + "'," + transaction.getAmount() + ")";
                    Query procesa_distribucion = em.createNativeQuery(distribucion);
                    String distribucionProcesada = String.valueOf(procesa_distribucion.getSingleResult());
                    System.out.println("Distribucion_Procesada:" +distribucionProcesada);
                    String ArrayDistribucion[] = distribucionProcesada.split("\\|");      
                    //Retorno: Seguro hipotecario | Comision cobranza | IM | Iva IM | IO | Iva IO | A Capital
                    if(prDestino.getTipoproducto()==5){
                        mensajeBackendResult="PAGO EXITOSA"  +"\n"+
                                         "SEGURO HIPOTECARIO    :"+ArrayDistribucion[0]+"\n "+
                                         "COMISON COBRANZA      :"+ArrayDistribucion[1]+"\n "+
                                         "INTERES MORATORIO     :"+ArrayDistribucion[2]+"\n "+
                                         "IVA INTERES MORATORIO :"+ArrayDistribucion[3]+"\n"+
                                         "INTERES ORDINARIO     :"+ArrayDistribucion[4]+"\n"+
                                         "IVA INTERES ORDINARIO :"+ArrayDistribucion[5]+"\n"+
                                         "A CAPITAL             :"+ArrayDistribucion[6]+"\n";
                    }else{
                        mensajeBackendResult="PAGO EXITOSA"  +"\n"+
                                         "INTERES MORATORIO     :"+ArrayDistribucion[2]+"\n "+
                                         "IVA INTERES MORATORIO :"+ArrayDistribucion[3]+"\n"+
                                         "INTERES ORDINARIO     :"+ArrayDistribucion[4]+"\n"+
                                         "IVA INTERES ORDINARIO :"+ArrayDistribucion[5]+"\n"+
                                         "A CAPITAL             :"+ArrayDistribucion[6]+"\n";
                    }                    
                }else{
                   mensajeBackendResult="TRANSACION EXITOSA";
                }
                //Ejecuto la distribucion del monto(Funciona final)
                String procesar = "SELECT sai_procesa_movs_banca_movil('" + fReal + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                System.out.println("consulta:"+procesar);
                Query procesa_pago = em.createNativeQuery(procesar);
                String respuestaProcesada = String.valueOf(procesa_pago.getSingleResult());
                if(Integer.parseInt(respuestaProcesada)>=2){
                    banderaEstatusTransferencia=true;
                }
                if (banderaEstatusTransferencia) {
                    //Aplico la distribucion
                    em.getTransaction().begin();
                    em.persist(transaction);
                    em.getTransaction().commit();
                    backendResponse.setBackendCode("1");
                    backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                    backendResponse.setIsError(false);
                    backendResponse.setTransactionIdenty(transaction.getTransactionid().toString());
                    backendResponse.setBackendMessage(mensajeBackendResult);
                    backendResponse.setBackendReference(transaction.getTransactionid().toString());                   
                }
            }
        } catch (Exception e) {
            if (backendResponse.getBackendMessage().contains("EXITO")) {
                backendResponse.setBackendMessage("Error:" + e.getMessage());
            }
            em.close();
            System.out.println("Al ejecutar transferencia:" + e.getMessage());
            return backendResponse;
        }
        em.close();
        return backendResponse;
    }

    public String voucherFileCreate(String idtransaccion) {
        EntityManager em = emf.createEntityManager();
        try {
            String cosulta = "SELECT * FROM transferencias_bankingly WHERE transactionid='" + idtransaccion + "'";
            System.out.println("aqui:" + cosulta);
            Query query = em.createNativeQuery(cosulta, Transferencias.class);
            Transferencias transferencia = (Transferencias) query.getSingleResult();
            Query nombreMatriz = em.createNativeQuery("SELECT nombre FROM origenes WHERE matriz=0");
            String nombre = String.valueOf(nombreMatriz.getSingleResult());
            String bProp = "SELECT nombre||' '||appaterno||' '||apmaterno FROM personas WHERE replace(to_char(idorigen,'099999')||to_char(idgrupo,'09')||to_char(idsocio,'099999'),' ','')='" + transferencia.getClientbankidentifier() + "'";
            Query prop = em.createNativeQuery(bProp);
            String propietario = String.valueOf(prop.getSingleResult());
            String vouch = voucher(String.valueOf(transferencia.getTransactionid()),
                    transferencia.getValuedate(),
                    transferencia.getDebitproductbankidentifier(),
                    String.valueOf(transferencia.getAmount()),
                    nombre,
                    propietario, transferencia.getDescription());
            return vouch;

        } catch (Exception e) {
            System.out.println("Error al crear voucher:" + e.getMessage());
            em.close();
            return "";
        } finally {
            em.close();
        }

    }

    //Metodo para validar transferencia entre cuentas propias
    public String validarTransferenciaEntreMisCuentas(String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'"
                + " AND  replace(to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999'),' ','')='" + clientBankIdentifier + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        System.out.println("Si llego");
        try {
            Auxiliares ctaOrigen = null;
            boolean bOrigen = false;
            System.out.println("ConsultaParaCuentaOrigen:" + cuentaOrigen);
            try {
                Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
                //Obtengo el producto origen
                ctaOrigen = (Auxiliares) query.getSingleResult();
                bOrigen = true;
            } catch (Exception e) {
                System.out.println("No existe Cuenta Origen");
                bOrigen = false;
            }

            //Si existe el auxiliar origen en tabla auxiliares
            if (bOrigen) {
                Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
                TablasDTO tablaProductoTDD = WSTDD.productoTddwebservice();
                if (caja().contains("SANNICOLAS") & ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                    TarjetaDeDebito tarjeta = new TarjetaDeDebito();
                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = tarjeta.buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar());
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = tarjeta.saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK());
                        saldo = saldoTDD.getAvailableAmount();
                        //saldo = 200.0;
                        System.out.println("Saldo TDD:" + saldo);
                    } catch (Exception e) {
                        System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                        return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                    }
                }
                //obtengo el saldo del producto origen

                //Busco descripcion del idproducto origen
                Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                //Valido que el producto origen se trabaje en banca movil
                Productos_bankingly cuentasBankingly = em.find(Productos_bankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                if (cuentasBankingly != null) {
                    //si el producto no es un prestamo            
                    if (prOrigen.getTipoproducto() == 0) {
                        //Verifico el estatus de la cuenta origen
                        if (ctaOrigen.getEstatus() == 2) {
                            //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                            if (saldo > monto) {
                                Auxiliares ctaDestino = null;
                                boolean bDestino = false;
                                //Busco la cuenta destino
                                System.out.println("CuentaDestino:" + cuentaDestino);
                                try {
                                    Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                                    ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                                    bDestino = true;
                                } catch (Exception e) {
                                    System.out.println("Error al encontrar productoDestino:" + e.getMessage());
                                    bDestino = false;
                                }
                                if (bDestino) {
                                    //Busco el producto destino
                                    Productos productoDestino = em.find(Productos.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                    //Valido que la cuenta destino este activa
                                    if (ctaDestino.getEstatus() == 2) {
                                        //Valido que producto destino opera para banca movil
                                        Productos_bankingly cuentaBankinglyDestino = em.find(Productos_bankingly.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                        if (cuentaBankinglyDestino != null) {
                                            //Valido que el producto destino no sea un prestamo
                                            if (productoDestino.getTipoproducto() == 0) {
                                                //Valido que realmente el el producto destino pertenezca al mismo socio 
                                                if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
                                                    //valido el minimo o maximo para banca movil
                                                    if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                        //Valido el monto maximo por dia
                                                        if (MaxPordia(opaOrigen, monto)) {
                                                            message = "validado con exito";
                                                        } else {
                                                            message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                        }
                                                    } else {
                                                        message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(monto).toUpperCase() + " AL PERMITIDO";
                                                    }
                                                } else {
                                                    message = "PRODUCTO DESTINO NO PERTENECE AL MISMO SOCIO";
                                                }
                                            } else {
                                                message = "PRODUCTO DESTINO NO ACEPTA SOBRECARGOS";
                                            }
                                        } else {
                                            message = "PRODUCTO DESTINO NO OPERA PARA BANCA MOVIL";
                                        }
                                    } else {
                                        message = "PRODUCTO DESTINO ESTA INACTIVA";
                                    }
                                } else {
                                    message = "NO SE ENCONTRO PRODUCTO DESTINO";
                                }
                            } else {
                                message = "FONDOS INSUFICIENTES PARA COMPLETAR LA TRANSACCION";
                            }
                        } else {
                            message = "PRODUCTO ORIGEN INACTIVO";
                        }

                    } else {
                        message = "PRODUCTO ORIGEN NO PERMITE SOBRECARGOS";
                    }
                } else {
                    message = "PRODUCTO ORIGEN NO OPERA PARA BANCA MOVIL";
                }
            } else {
                message = "PRODUCTO ORIGEN NO PERTENECE AL SOCIO:" + clientBankIdentifier;
            }

        } catch (Exception e) {
            em.close();
            System.out.println("meee:" + message);
            message = "ERROR AL PROCESAR CONSULTA";
            System.out.println("Error en transferencia entre mis cuentas:" + e.getMessage());
            return message;
        } finally {
            em.clear();
            em.close();
        }

        return message.toUpperCase();
    }

    //Metodo para validar transferencia a otras cuentas
    public String validarTransferenciaATerceros(String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'"
                + " AND  replace(to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999'),' ','')='" + clientBankIdentifier + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        Double salxdo = 0.0;
        try {
            Auxiliares ctaOrigen = null;
            boolean bOrigen = false;
            try {
                Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
                //Obtengo el producto origen
                ctaOrigen = (Auxiliares) query.getSingleResult();
                bOrigen = true;
            } catch (Exception e) {
                System.out.println("Error al buscar producto origen:" + e.getMessage());
                bOrigen = false;
            }

            if (bOrigen) {
                Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
                TablasDTO tablaProductoTDD = WSTDD.productoTddwebservice();
                if (caja().toUpperCase().contains("SANNICOLAS") & ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                    TarjetaDeDebito tarjeta = new TarjetaDeDebito();
                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = tarjeta.buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar());
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = tarjeta.saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK());
                        saldo = saldoTDD.getAvailableAmount();
                        //saldo = 200.0;
                    } catch (Exception e) {
                        System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                        return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                    }
                }

                //Busco descripcion del idproducto origen
                Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                //Valido que el producto origen se trabaje en banca movil
                Productos_bankingly cuentasBankingly = em.find(Productos_bankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                if (cuentasBankingly != null) {
                    //si el producto no es un prestamo            
                    if (prOrigen.getTipoproducto() != 2) {
                        //Verifico el estatus de la cuenta origen
                        if (ctaOrigen.getEstatus() == 2) {
                            //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                            if (saldo >= monto) {
                                Auxiliares ctaDestino = null;
                                boolean bDestino = false;
                                try {
                                    //Busco la cuenta destino
                                    Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                                    ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                                    bDestino = true;
                                } catch (Exception e) {
                                    System.out.println("Error al buscar producto destino:" + e.getMessage());
                                    bDestino = false;
                                }
                                if (bDestino) {
                                    //Busco el producto destino
                                    Productos productoDestino = em.find(Productos.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                    //Valido que la cuenta destino este activa
                                    if (ctaDestino.getEstatus() == 2) {
                                        //Busco si existe el producto destino en el catalogo de banca movil
                                        Productos_bankingly catalogoDestino = em.find(Productos_bankingly.class, productoDestino.getIdproducto());
                                        if (catalogoDestino != null) {
                                            //Valido que el producto destino no sea un prestamo
                                            if (productoDestino.getTipoproducto() == 0) {
                                                //valido el minimo o maximo para banca movil
                                                if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                    //Valido el monto maximo por dia
                                                    if (MaxPordia(opaOrigen, monto)) {
                                                        message = "VALIDADO CON EXITO";
                                                    } else {
                                                        message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                    }
                                                } else {
                                                    message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(monto).toUpperCase() + " AL PERMITIDO";
                                                }
                                            } else {
                                                message = "PRODUCTO DESTINO NO ACEPTA SOBRECARGOS";
                                            }
                                        } else {
                                            message = "PRODUCTO DESTINO NO OPERA PARA BANCA MOVIL";
                                        }
                                    } else {
                                        message = "PRODUCTO DESTINO ESTA INACTIVA";
                                    }

                                } else {
                                    message = "FONDOS INSUFICIENTES PARA COMPLETAR LA TRANSACCION";
                                }
                            } else {
                                message = "PRODUCTO DESTINO NO EXISTE";
                            }
                        } else {
                            message = "PRODUCTO ORIGEN INACTIVO";
                        }

                    } else {
                        message = "PRODUCTO ORIGEN NO PERMITE SOBRECARGOS";
                    }
                } else {
                    message = "PRODUCTO ORIGEN NO OPERA PARA BANCA MOVIL";
                }
            } else {
                message = "PRODUCTO OrigEN no pertenece al socio:" + clientBankIdentifier;
            }
        } catch (Exception e) {
            em.close();
            message = e.getMessage();
            System.out.println("Errro al validar transferencia a terceros:" + e.getMessage());
            return message;
        } finally {
            em.close();
        }
        return message.toUpperCase();
    }

    //Metodo para validar pago a prestamos
    public String validarPagoAPrestamos(String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'"
                + " AND  replace(to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999'),' ','')='" + clientBankIdentifier + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        try {
            Auxiliares ctaOrigen = null;
            boolean bOrigen = false;
            try {
                Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
                //Obtengo el producto origen
                ctaOrigen = (Auxiliares) query.getSingleResult();
                bOrigen = true;
            } catch (Exception e) {
                bOrigen = false;
            }
            if (bOrigen) {
                Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
                TablasDTO tablaProductoTDD = WSTDD.productoTddwebservice();
                if (caja().toUpperCase().contains("SANNICOLAS") & ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                    TarjetaDeDebito tarjeta = new TarjetaDeDebito();
                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = tarjeta.buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar());
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = tarjeta.saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK());
                        saldo = saldoTDD.getAvailableAmount();

                    } catch (Exception e) {
                        System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                        return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                    }
                }
                //Valido que el producto origen se trabaje en banca movil
                Productos_bankingly cuentasBankingly = em.find(Productos_bankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                if (cuentasBankingly != null) {
                    //Busco descripcion del idproducto origen
                    Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                    //si el producto no es un prestamo            
                    if (prOrigen.getTipoproducto() == 0) {
                        //Verifico el estatus de la cuenta origen
                        if (ctaOrigen.getEstatus() == 2) {
                            //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                            if (saldo >= monto) {
                                Auxiliares ctaDestino = null;
                                boolean bDestino = false;
                                try {
                                    //Busco la cuenta destino
                                    Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                                    ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                                    bDestino = true;
                                } catch (Exception e) {
                                    System.out.println("Error al buscar cuenta destino:" + e.getMessage());
                                    bDestino = false;
                                }
                                if (bDestino) {
                                    //Busco el producto destino
                                    Productos productoDestino = em.find(Productos.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                    //Valido que la cuenta destino este activa
                                    if (ctaDestino.getEstatus() == 2) {
                                        //Valido que cuenta destino pertenezca al mismo socio
                                        if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
                                            //Valido que el producto destino sea un prestamo
                                            if (productoDestino.getTipoproducto() == 2) {
                                                //valido el minimo o maximo para banca movil
                                                if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                    //Valido el monto maximo por dia
                                                    if (MaxPordia(opaOrigen, monto)) {
                                                        message = "VALIDADO CON EXITO";
                                                    } else {
                                                        message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                    }
                                                } else {
                                                    message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(monto).toUpperCase() + " AL PERMITIDO";
                                                }
                                            } else {
                                                message = "PRODUCTO DESTINO NO ES UN PRESTAMO";
                                            }
                                        } else {
                                            message = "PRODUCTO DESTINO NO PERTENCE AL MISMO SOCIO";
                                        }
                                    } else {
                                        message = "PRODUCTO DESTINO ESTA INACTIVO";
                                    }
                                } else {
                                    message = "NO SE ENCONTRO PRODUCTO DESTINO";
                                }
                            } else {
                                message = "FONDOS INSUFICIENTES PARA COMPLETAR LA TRANSACCION";
                            }
                        } else {
                            message = "PRODUCTO ORIGEN INACTIVO";
                        }

                    } else {
                        message = "PRODUCTO ORIGEN NO PERMITE SOBRECARGOS";
                    }
                } else {
                    message = "PRODUCTO ORIGEN NO OPERA PARA BANCA MOVIL";
                }
            } else {
                message = "PRODUCTO ORIGEN NO PERTENECE AL SOCIO:" + clientBankIdentifier;
            }
        } catch (Exception e) {
            em.close();
            message = e.getMessage();
            System.out.println("Error al realizar pago a prestamo:" + e.getMessage());
            return message;
        } finally {
            em.close();
        }
        return message.toUpperCase();
    }

    //Metodo para validar pago a prestamos
    public String EnviarOrdenSPEI(String opaOrigen, Double monto, @Context UriInfo ui) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'";
        System.out.println("Consulta:" + cuentaOrigen);
        String message = "";
        try {
            Auxiliares ctaOrigen = null;
            boolean bOrigen = false;
            try {
                Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
                //Obtengo el producto origen
                ctaOrigen = (Auxiliares) query.getSingleResult();
                bOrigen = true;
            } catch (Exception e) {
                System.out.println("Error al buscar producto origen:" + e.getMessage());
                bOrigen = false;
            }
            if (bOrigen) {
                //obtengo el saldo del producto origen
                Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
                //Valido que el producto origen se trabaje en banca movil
                Productos_bankingly cuentasBankingly = em.find(Productos_bankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                if (cuentasBankingly != null) {
                    //Busco descripcion del idproducto origen
                    Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                    //si el producto no es un prestamo            
                    if (prOrigen.getTipoproducto() == 0) {
                        //Verifico el estatus de la cuenta origen
                        if (ctaOrigen.getEstatus() == 2) {
                            //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                            if (saldo >= monto) {
                                //Busco origen para saber quien esta usando el ws service
                                Query or = em.createNativeQuery("SELECT * FROM origenes WHERE matriz=0", Origenes.class);
                                Origenes ori = (Origenes) or.getSingleResult();
                                //Si es CSN voy a los servicios de SPEI CSN
                                if (ori.getNombre().toUpperCase().replace(" ", "").contains("SANNICOLAS")) {
                                    JsonObject request = new JsonObject();
                                    /*//Parto la url completa para obtener basePath
                                    String[] partesUrl = ui.getBaseUri().toString().split("Ws");
                                    String urlBase = partesUrl[0];
                                    //al UrlPath le pongo el nombre del proyecto de SPEI-CSN
                                    String urlComplete = urlBase + "SPEI-CSN/spei/v1.0/srvEnviaOrden";*/
                                    //Busco la url en tablas
                                    TablasPK tbPK = new TablasPK("bankingly_banca_movil", "speipath");
                                    Tablas tb = em.find(Tablas.class, tbPK);
                                    String url = tb.getDato1();
                                    String urlSPEI = url + "srvEnviaOrden";
                                    request.put("cliente", opaOrigen);
                                    request.put("monto", monto);
                                    message = metodoEnviarSPEI(urlSPEI, request.toString().replace("=", ":"));
                                }

                            } else {
                                message = "FONDOS INSUFICIENTES PARA COMPLETAR LA TRANSACCION";
                            }
                        } else {
                            message = "PRODUCTO ORIGEN INACTIVO";
                        }

                    } else {
                        message = "PRODUCTO ORIGEN NO PERMITE SOBRECARGOS";
                    }
                } else {
                    message = "PRODUCTO ORIGEN NO OPERA PARA BANCA MOVIL";
                }
            } else {
                message = "NO SE ENCONTRO PRODUCTO ORIGEN";
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error al realizar tranferenciasSPEI:" + e.getMessage());
            return message;
        }
        em.close();
        return message.toUpperCase();
    }

    //Se consume un servicio que yo desarrolle donde consumo API STP en caso de CSN si alguuien mas usara SPEI desarrollaria otro proyecto especficamente para la caja
    private String metodoEnviarSPEI(String url, String request) {
        URL urlB = null;
        String output = "";
        String salida = "";
        System.out.println("Request:" + request);
        System.out.println("UrlSPEI:" + url);
        try {
            urlB = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlB.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            //String request=" {\"clientBankIdentifiers\":[{\"value\":\"03020710217963\"}],\"productBankIdentifiers\":[{\"value\":\"0302070011027916986\"}]}";
            OutputStream os = conn.getOutputStream();
            os.write(request.getBytes());
            os.flush();
            int codigoHTTP = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            System.out.println("Output from Server .... \n");
            if (codigoHTTP == 200) {
                while ((output = br.readLine()) != null) {
                    salida = output;
                }
            }
            conn.disconnect();
        } catch (Exception ex) {
            System.out.println("e:" + ex.getMessage());
        }
        return salida;
    }

    //valida el monto para banca movil total de transferencia
    public String minMax(Double amount) {
        EntityManager em = emf.createEntityManager();
        String mensaje = "";
        try {
            TablasPK tbPk = new TablasPK("bankingly_banca_movil", "montomaximominimo");
            Tablas tb = em.find(Tablas.class, tbPk);
            if (amount > Double.parseDouble(tb.getDato1())) {
                mensaje = "MAYOR";
            } else if (amount < Double.parseDouble(tb.getDato2())) {
                mensaje = "MENOR";
            } else {
                mensaje = "VALIDO";
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error al validar monto min-max:" + e.getMessage());
        } finally {
            em.close();
        }
        return mensaje;
    }

    public String voucher(String idtransaccion, String fecha, String cuenta, String monto, String caja, String propietario, String comentario) {

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(ruta() + "vo" + idtransaccion + cuenta + ".pdf"));
            document.open();
            // Left
            Paragraph paragraph1 = new Paragraph(caja);
            paragraph1.setAlignment(Element.ALIGN_CENTER);

            Font fuente = new Font();
            fuente.setSize(2);
            fuente.setStyle(Font.BOLD);
            paragraph1.setSpacingBefore(20);

            paragraph1.setFont(fuente);
            document.add(paragraph1);

            Paragraph paragraph2 = new Paragraph(
                    "No.Transferencia:" + idtransaccion + "\n"
                    + "Fecha:" + fecha + "\n"
                    + "Cuenta:" + cuenta + "\n"
                    + "Monto operacion:" + monto + "\n"
                    + "Propietario cuenta:" + propietario + "\n"
                    + "Motivo tranferencia:" + comentario
            );
            paragraph2.setSpacingBefore(20);
            paragraph2.setAlignment(Element.ALIGN_LEFT);
            document.add(paragraph2);
            document.close();
            return ruta() + "vo" + idtransaccion + cuenta + ".pdf";
        } catch (Exception e) {
            System.out.println("Error al crear voucher:" + e.getMessage());
            e.printStackTrace();
            return "";
        }
    }

    //Valida el monto maximo permitido por dia
    public boolean MaxPordia(String opa, Double montoI) {
        EntityManager em = emf.createEntityManager();
        Calendar c1 = Calendar.getInstance();
        String dia = Integer.toString(c1.get(5));
        String mes = Integer.toString(c1.get(2) + 1);
        String annio = Integer.toString(c1.get(1));
        String fechaActual = String.format("%04d", Integer.parseInt(annio)) + "/" + String.format("%02d", Integer.parseInt(mes)) + "/" + String.format("%02d", Integer.parseInt(dia));
        TablasPK tbPk = new TablasPK("bankingly_banca_movil", "montomaximo");
        Tablas tb = em.find(Tablas.class, tbPk);
        try {
            //Busco el total de monto de transferencias por dia
            String consultaTransferencias = "SELECT sum(amount) FROM transferencias_bankingly WHERE"
                    + " debitproductbankidentifier='" + opa + "' AND to_char(date(fechaejecucion),'yyyy/MM/dd')='" + fechaActual + "'";
            Query query = em.createNativeQuery(consultaTransferencias);
            String montoObtenidodb = "";
            if (query.getSingleResult() != null) {
                montoObtenidodb = String.valueOf(query.getSingleResult());
            } else {
                montoObtenidodb = "0";
            }
            Double monto = Double.parseDouble(String.valueOf(montoObtenidodb)) + montoI;
            System.out.println("monto:" + monto);
            if (monto <= Double.parseDouble(tb.getDato1())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error al validar permitido diario:" + e.getMessage());
        } finally {
            em.close();
        }
        return false;
    }

    public String caja() {
        EntityManager em = emf.createEntityManager();
        String nombreOrigen = "";
        try {
            String consulta = "SELECT replace(nombre,' ','') FROM origenes WHERE matriz=0";
            System.out.println("ConsultaOrigen:" + consulta);
            Query query = em.createNativeQuery(consulta);
            nombreOrigen = String.valueOf(query.getSingleResult());
        } catch (Exception e) {
            em.clear();
            em.close();
            System.out.println("Error al crear origen trabajando:" + e.getMessage());
            return "";
        } finally {
            em.clear();
            em.close();
        }
        return nombreOrigen.replace(" ", "").toUpperCase();
    }

    public void cerrar() {
        emf.close();
    }
}
