/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.DTO.OgsDTO;
import com.fenoreste.rest.DTO.OpaDTO;
import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.Request.RequestDataOrdenPagoDTO;
import com.fenoreste.rest.ResponseDTO.BackendOperationResultDTO;
import com.fenoreste.rest.ResponseDTO.ResponseSPEIDTO;
import com.fenoreste.rest.ResponseDTO.TransactionToOwnAccountsDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.Util.Utilidades;
import com.fenoreste.rest.WsTDD.TarjetaDeDebito;
import static com.fenoreste.rest.dao.FacadeProductos.ruta;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.AuxiliaresPK;
import com.fenoreste.rest.entidades.Productos_bankingly;
import com.fenoreste.rest.entidades.Procesa_pago_movimientos;
import com.fenoreste.rest.entidades.Productos;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.Transferencias;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetas1;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetasPK1;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.json.JSONObject;

/**
 *
 * @author Elliot
 */
public abstract class FacadeTransaction<T> {

    Utilidades util = new Utilidades();
    EntityManagerFactory emf;

    public FacadeTransaction(Class<T> entityClass) {
        emf = AbstractFacade.conexion();

    }

    //El identificador de trasnferencia me dice 1=entre mis cuentas 2=a terceros 3=Pago a prestamos
    public BackendOperationResultDTO transferencias(TransactionToOwnAccountsDTO transactionOWN, int identificadorTransferencia, RequestDataOrdenPagoDTO SPEIOrden) {
        EntityManager em = emf.createEntityManager();
        Date hoy = new Date();

        System.out.println("TransactionFecha:" + transactionOWN.getValueDate());
        BackendOperationResultDTO backendResponse = new BackendOperationResultDTO();
        backendResponse.setBackendCode("2");
        backendResponse.setBackendMessage("Incorrecto");
        backendResponse.setBackendReference(null);
        backendResponse.setIsError(true);
        backendResponse.setTransactionIdenty("0");
        /*int idorigenp = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(1, 6));
        int idproducto = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(6, 11));
        int idauxiliar = Integer.parseInt(transactionOWN.getDebitProductBankIdentifier().substring(11, 19));*/
        OpaDTO opaOrigen = util.opa(transactionOWN.getDebitProductBankIdentifier());
        System.out.println(opaOrigen.getIdorigenp() + "-" + opaOrigen.getIdproducto() + "-" + opaOrigen.getIdauxiliar());

        boolean retiro = false;
        boolean bandera = false;
        //Identifico la caja que esta usando los WS 
        String mss = "";
        ResponseSPEIDTO response = null;
        //Si la caja es CSN validamos y el retiro es de TDD
        if (caja().contains("SANNICOLAS")) {
            WsSiscoopFoliosTarjetas1 tarjetas = null;
            try {
                LoadBalanceResponse.Return loadBalanceResponse = new LoadBalanceResponse.Return();
                //Buscamos el producto para TDD en tablas 
                TablasDTO tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);

                System.out.println("Producto_para_tarjeta_de_debito:" + tablaProductoTDD.getDato2());
                //Creo una bandera para no generar error al buscar tarjeta
                boolean busquedaTDD = false;

                if (Integer.parseInt(tablaProductoTDD.getDato2()) == opaOrigen.getIdproducto()) {
                    bandera = true;
                    try {
                        //Buscando la tarjeta de debito 
                        tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(opaOrigen.getIdorigenp(), opaOrigen.getIdproducto(), opaOrigen.getIdauxiliar(), em);
                        System.out.println("Tarjeta Siscoop:" + tarjetas);
                        busquedaTDD = true;
                    } catch (Exception e) {
                        busquedaTDD = false;
                    }
                    //si se encontro la Tarjeta de debito
                    if (busquedaTDD) {
                        DoWithdrawalAccountResponse.Return doWithdrawalAccountResponse = new DoWithdrawalAccountResponse.Return();
                        //Si la tarjeta esta activa
                        
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
                                if (identificadorTransferencia == 3 || identificadorTransferencia == 4) {
                                    message = validarPagoAPrestamos(identificadorTransferencia, transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                    System.out.println("Message:" + message);
                                }
                                if (identificadorTransferencia == 5) {//Si es una orden SPEI  
                                    //Validamos la orden SPEI
                                    message = validaOrdenSPEI(SPEIOrden);
                                    System.out.println("Mensage de validacion SPEI:" + message);
                                }
                                
                                if (message.toUpperCase().contains("EXITO")) {
                                    //ejecutamos el retiro de la TDD
                                    if (identificadorTransferencia == 5) {
                                        //enviamos la orden SPEI al proyecto propio de Fenoreste
                                        response = metodoEnviarSPEI(SPEIOrden);
                                        System.out.println("Ordennnnnnnnnnnnnnnnnnn enviada con exito esta comentado para retirar el saldo de la TDD SPEI ID:"+response.getId());
                                        if (response.getId() > 3) {
                                            doWithdrawalAccountResponse = null;// new TarjetaDeDebito().retiroTDD(tarjetas, transactionOWN.getAmount());
                                            System.out.println("Code::::::::::::::"+doWithdrawalAccountResponse.getCode());
                                            if (doWithdrawalAccountResponse.getCode() == 0) {
                                                mss = "ERROR AL PROCESAR RETIRO DE TARJETA DE DEBITO";
                                                //mss = "VALIDADO CON EXITO";
                                                //Existe Error
                                                retiro = false;
                                            } else {
                                                retiro = true;
                                                mss = "VALIDADO CON EXITO";
                                            }
                                        } else {                                            
                                            mss = response.getError();
                                        }

                                    } else {
                                        //doWithdrawalAccountResponse = WSTDD.retiroTDD(tarjetas, transactionOWN.getAmount());
                                        if (doWithdrawalAccountResponse.getCode() == 0) {
                                            mss = "ERROR AL PROCESAR RETIRO DE WSTDD";
                                            //mss = "VALIDADO CON EXITO";
                                            //Existe Error
                                            //mss = "VALIDADO CON EXITO";
                                            retiro = false;
                                        } else {
                                            retiro = true;
                                            mss = "VALIDADO CON EXITO";
                                        }
                                    }

                                } else {
                                    mss = message;
                                    retiro = false;
                                }

                            } catch (Exception e) {
                                mss = "NO SE PUDO VERIFICAR TARJETA:"+e.getMessage();

                            }
                        } else {
                            mss = "TARJETA INACTIVA";
                        }
                    } else {
                        mss = "VERIFIQUE EL ESTATUS DE SU TARJETA";
                    }
                }

            } catch (Exception e) {
                mss = e.getMessage();
                System.out.println("Error producido en buscar tjeta:" + e.getMessage());

            }

        }

        backendResponse.setBackendMessage(mss);

        //Si no es TDD pasa directo hasta aca
        //Si es una transferencia entre mis cuentas
        if (identificadorTransferencia == 1 && retiro == false && bandera == false) {
            //Valido la transferencia y devuelvo el mensaje que se produce
            String messageBackend = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es una transferencia terceros dentro de la entidad
        if (identificadorTransferencia == 2 && retiro == false && bandera == false) {
            String messageBackend = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es pago a un prestamo
        if ((identificadorTransferencia == 3 || identificadorTransferencia == 4) && retiro == false && bandera == false) {

            String backendMessage = validarPagoAPrestamos(identificadorTransferencia, transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
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
                OpaDTO opa = util.opa(transaction.getDebitproductbankidentifier());

                //Obtengo los productos origen y destino
                //Origen
                String origenP = "SELECT * FROM auxiliares WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar();
                Query queryOrigen = em.createNativeQuery(origenP, Auxiliares.class);
                Auxiliares aOrigen = (Auxiliares) queryOrigen.getSingleResult();
                Procesa_pago_movimientos procesaOrigen = new Procesa_pago_movimientos();
                //Obtener los datos para procesar la transaccion
                long time = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(time);
                Query sesion = em.createNativeQuery("select text(pg_backend_pid())||'-'||trim(to_char(now(),'ddmmyy'))");
                String sesionc = String.valueOf(sesion.getSingleResult());
                int rn = (int) (Math.random() * 999999 + 1);
                //Obtener HH:mm:ss.microsegundos

                String fechaArray[] = timestamp.toString().substring(0, 10).split("-");
                String fReal = fechaArray[2] + "/" + fechaArray[1] + "/" + fechaArray[0];
                String referencia = String.valueOf(rn) + "" + String.valueOf(transaction.getSubtransactiontypeid()) + "" + String.valueOf(transaction.getTransactiontypeid() + "" + fReal.replace("/", ""));

                //Leemos fechatrabajo e idusuario
                String fechaTrabajo = "SELECT to_char(fechatrabajo,'yyyy-MM-dd HH:mm:ss') FROM ORIGENES LIMIT 1";
                Query fechaTrabajo_ = em.createNativeQuery(fechaTrabajo);
                String fechaTr_ = String.valueOf(fechaTrabajo_.getSingleResult());
                TablasPK idusuarioPK = new TablasPK("bankingly_banca_movil", "usuario_banca_movil");
                Tablas tbUsuario_ = em.find(Tablas.class, idusuarioPK);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDate = LocalDateTime.parse(fechaTr_, dtf);
                Timestamp fecha_transferencia = Timestamp.valueOf(localDate);
                Productos prDestino = null;

                if (identificadorTransferencia == 5) {//tipo de orden SPEI
                    //Guardamos el origen
                    procesaOrigen.setAuxiliaresPK(aOrigen.getAuxiliaresPK());
                    procesaOrigen.setFecha(fecha_transferencia);
                    procesaOrigen.setIdusuario(Integer.parseInt(tbUsuario_.getDato1()));
                    procesaOrigen.setSesion(sesionc);
                    procesaOrigen.setReferencia(referencia);
                    procesaOrigen.setIdorigen(aOrigen.getIdorigen());
                    procesaOrigen.setIdgrupo(aOrigen.getIdgrupo());
                    procesaOrigen.setIdsocio(aOrigen.getIdsocio());
                    procesaOrigen.setCargoabono(0);
                    procesaOrigen.setMonto(transaction.getAmount());
                    procesaOrigen.setIva(Double.parseDouble(aOrigen.getIva().toString()));
                    procesaOrigen.setTipo_amort(Integer.parseInt(String.valueOf(aOrigen.getTipoamortizacion())));
                    procesaOrigen.setSai_aux("");

                    em.getTransaction().begin();
                    em.persist(procesaOrigen);
                    em.getTransaction().commit();

                    em.clear();

                    //Guardamos el destino
                    AuxiliaresPK aPKSPEI = new AuxiliaresPK(0, 1, 0);
                    procesaOrigen.setAuxiliaresPK(aPKSPEI);
                    procesaOrigen.setFecha(fecha_transferencia);
                    procesaOrigen.setIdusuario(Integer.parseInt(tbUsuario_.getDato1()));
                    procesaOrigen.setSesion(sesionc);
                    procesaOrigen.setReferencia(referencia);
                    procesaOrigen.setIdorigen(aOrigen.getIdorigen());
                    procesaOrigen.setIdgrupo(aOrigen.getIdgrupo());
                    procesaOrigen.setIdsocio(aOrigen.getIdsocio());
                    procesaOrigen.setCargoabono(1);
                    procesaOrigen.setIdcuenta("20407160101067");
                    procesaOrigen.setMonto(transaction.getAmount());
                    procesaOrigen.setIva(0.0);
                    procesaOrigen.setTipo_amort(0);
                    procesaOrigen.setSai_aux("");
                    em.getTransaction().begin();
                    em.persist(procesaOrigen);
                    em.getTransaction().commit();

                } else {
                    OpaDTO opaD = util.opa(transaction.getCreditproductbankidentifier());
                    //Destino
                    String destinoP = "SELECT * FROM auxiliares WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
                    Query queryDestino = em.createNativeQuery(destinoP, Auxiliares.class);
                    Auxiliares aDestino = (Auxiliares) queryDestino.getSingleResult();
                    //Obtengo el producto 
                    prDestino = em.find(Productos.class, aDestino.getAuxiliaresPK().getIdproducto());
                    Procesa_pago_movimientos procesaDestino = new Procesa_pago_movimientos();
                    procesaOrigen.setAuxiliaresPK(aOrigen.getAuxiliaresPK());
                    procesaOrigen.setFecha(fecha_transferencia);
                    procesaOrigen.setIdusuario(Integer.parseInt(tbUsuario_.getDato1()));
                    procesaOrigen.setSesion(sesionc);
                    procesaOrigen.setReferencia(referencia);
                    procesaOrigen.setIdorigen(aOrigen.getIdorigen());
                    procesaOrigen.setIdgrupo(aOrigen.getIdgrupo());
                    procesaOrigen.setIdsocio(aOrigen.getIdsocio());
                    procesaOrigen.setCargoabono(0);
                    procesaOrigen.setMonto(transaction.getAmount());
                    procesaOrigen.setIva(Double.parseDouble(aOrigen.getIva().toString()));
                    procesaOrigen.setTipo_amort(Integer.parseInt(String.valueOf(aOrigen.getTipoamortizacion())));

                    procesaOrigen.setSai_aux("");

                    em.getTransaction().begin();
                    em.persist(procesaOrigen);
                    em.getTransaction().commit();

                    em.clear();
                    //Destino
                    procesaDestino.setAuxiliaresPK(aDestino.getAuxiliaresPK());
                    procesaDestino.setFecha(fecha_transferencia);
                    procesaDestino.setIdusuario(Integer.parseInt(tbUsuario_.getDato1()));
                    procesaDestino.setSesion(sesionc);
                    procesaDestino.setReferencia(referencia);
                    procesaDestino.setIdorigen(aDestino.getIdorigen());
                    procesaDestino.setIdgrupo(aDestino.getIdgrupo());
                    procesaDestino.setIdsocio(aDestino.getIdsocio());
                    procesaDestino.setCargoabono(1);
                    procesaDestino.setMonto(transaction.getAmount());
                    procesaDestino.setIva(Double.parseDouble(aDestino.getIva().toString()));
                    procesaDestino.setTipo_amort(Integer.parseInt(String.valueOf(aDestino.getTipoamortizacion())));

                    procesaDestino.setSai_aux("");
                    em.getTransaction().begin();
                    em.persist(procesaDestino);
                    em.getTransaction().commit();

                }

                //Ejecuto la distribucion del monto(Funciona final)
                String procesar = "SELECT sai_bankingly_aplica_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                Query procesa_pago = em.createNativeQuery(procesar);
                int respuestaProcesada = Integer.parseInt(String.valueOf(procesa_pago.getSingleResult()));

                System.out.println("RespuestaProcesada:" + respuestaProcesada);
                String mensajeBackendResult = "";
                //Si la cuenta a la que se esta transfiriendo es un prestamo

                //Si es un spei
                if (identificadorTransferencia == 5) {
                    mensajeBackendResult = "TRANSACCION EXITOSA";
                    backendResponse.setTransactionIdenty(String.valueOf(response.getId()));
                    System.out.println("Transaccion exitosa");
                } else {
                    if (prDestino.getTipoproducto() == 2){
                        //Obtengo los datos(Seguro hipotecario,comisones cobranza,interes ect.)
                        String distribucion = "SELECT sai_bankingly_detalle_transaccion_aplicada('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                        System.out.println("DistribucionConsulta:" + distribucion);
                        Query procesa_distribucion = em.createNativeQuery(distribucion);
                        String distribucionProcesada = String.valueOf(procesa_distribucion.getSingleResult());
                        System.out.println("Distribucion_Procesada:" + distribucionProcesada);
                        String ArrayDistribucion[] = distribucionProcesada.split("\\|");

                        mensajeBackendResult = "PAGO EXITOSO" + "\n"
                                + "SEGURO HIPOTECARIO    :" + ArrayDistribucion[0] + "\n "
                                + "COMISON COBRANZA      :" + ArrayDistribucion[1] + "\n "
                                + "INTERES MORATORIO     :" + ArrayDistribucion[2] + "\n "
                                + "IVA INTERES MORATORIO :" + ArrayDistribucion[3] + "\n"
                                + "INTERES ORDINARIO     :" + ArrayDistribucion[4] + "\n"
                                + "IVA INTERES ORDINARIO :" + ArrayDistribucion[5] + "\n"
                                + "CAPITAL               :" + ArrayDistribucion[5] + "\n"
                                + "ADELANTO DE INTERES   :" + ArrayDistribucion[6] + "\n";//Para adelanto de interese solo aplicaria para los productos configurados

                    } else if (prDestino.getTipoproducto() == 0) {
                        mensajeBackendResult = "TRANSACCION EXITOSA";
                        System.out.println("Transaccion exitosa");
                    }

                }

                if (respuestaProcesada >0) {
                    banderaEstatusTransferencia = true;
                }
                if (banderaEstatusTransferencia) {
                    //Aplico la distribucion
                    String clean = "SELECT sai_bankingly_termina_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                    Query queryL = em.createNativeQuery(clean);
                    int registrosLimpiados = Integer.parseInt(String.valueOf(queryL.getSingleResult()));
                    System.out.println("Registros Limpiados con exito:" + registrosLimpiados);
                    em.getTransaction().begin();
                    em.persist(transaction);
                    em.getTransaction().commit();
                    backendResponse.setBackendCode("1");
                    backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                    backendResponse.setIsError(false);
                    //Si fuera transferencia SPEI se devuelve el idTransaccion
                    if(identificadorTransferencia!=5){
                        backendResponse.setTransactionIdenty(transaction.getTransactionid().toString());
                    }                    
                    backendResponse.setBackendMessage(mensajeBackendResult);
                    backendResponse.setBackendReference(transaction.getTransactionid().toString());

                }
            }
        } catch (Exception e) {
            if (backendResponse.getBackendMessage().contains("EXITO")) {
                backendResponse.setBackendMessage("Error:" + e.getMessage());
            }
            em.close();
            System.out.println("Error Al ejecutar transferencia:" + e.getMessage());
            return backendResponse;
        } finally {
            em.close();
        }

        return backendResponse;
    }

    public String voucherFileCreate(String idtransaccion) {
        EntityManager em = emf.createEntityManager();

        try {
            String cosulta = "SELECT * FROM transferencias_bankingly WHERE transactionid='" + idtransaccion + "'";
            System.out.println("aqui:" + cosulta);
            Query query = em.createNativeQuery(cosulta, Transferencias.class);
            Transferencias transferencia = (Transferencias) query.getSingleResult();
            OgsDTO ogs = util.ogs(transferencia.getClientbankidentifier());
            Query nombreMatriz = em.createNativeQuery("SELECT nombre FROM origenes WHERE matriz=0");
            String nombre = String.valueOf(nombreMatriz.getSingleResult());
            String bProp = "SELECT nombre||' '||appaterno||' '||apmaterno FROM personas WHERE idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();
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
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        String message = "";

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
                TablasDTO tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                if (caja().contains("SANNICOLAS") & ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
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
                            if (saldo >= monto) {
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
                                    System.out.println("TIpoproductoDestinooooo:" + productoDestino.getTipoproducto());
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
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();

        String cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
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
                TablasDTO tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                if (caja().toUpperCase().contains("SANNICOLAS") & ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {

                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
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
    public String validarPagoAPrestamos(int identificadorTr, String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = emf.createEntityManager();
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();

        String cuentaDestino = "";
        boolean banderaC = false;
        if (identificadorTr == 3) {
            cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar()
                    + " AND idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();
        } else if (identificadorTr == 4) {
            cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        }

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

                if (caja().toUpperCase().contains("SANNICOLAS")) {
                    TablasDTO tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                    //Si el pago del prestamo se esta haciendo desde la TDD
                    if (ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {                       
                        WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                        WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                        try {
                            System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                            BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
                            saldo = saldoTDD.getAvailableAmount();

                        } catch (Exception e) {
                            System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                            return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                        }
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
                                boolean identificadorCO = false;
                                boolean identificadorCD = false;
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
                                        if (identificadorTr == 3) {
                                            if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
                                                identificadorCO = true;
                                            }
                                        } else if (identificadorTr == 4) {
                                            if (bDestino) {
                                                identificadorCD = true;
                                            }
                                        }
                                        if (identificadorCO) {//=============================
                                            //Valido que el producto destino sea un prestamo
                                            if (productoDestino.getTipoproducto() == 2) {
                                                //valido el minimo o maximo para banca movil
                                                if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                    //Valido el monto maximo por dia
                                                    if (MaxPordia(opaOrigen, monto)) {
                                                        if (monto <= Double.parseDouble(ctaDestino.getSaldo().toString())) {
                                                            if (monto > 0) {
                                                                message = "VALIDADO CON EXITO";
                                                            } else {
                                                                message = "NO SE PUEDEN PAGAR SALDO NEGATIVO";
                                                            }
                                                        } else {
                                                            message = "EL SALDO QUE INTENTA PAGAR SOBREPASA A LA DEUDA";
                                                        }
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
                                        }//========================

                                        if (identificadorCD) {//=============================
                                            //Valido que el producto destino sea un prestamo
                                            if (productoDestino.getTipoproducto() == 2) {
                                                //valido el minimo o maximo para banca movil
                                                if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                    //Valido el monto maximo por dia
                                                    if (MaxPordia(opaOrigen, monto)) {
                                                        if (monto <= Double.parseDouble(ctaDestino.getSaldo().toString())) {
                                                            if (monto > 0) {
                                                                message = "VALIDADO CON EXITO";
                                                            } else {
                                                                message = "NO SE PUEDEN PAGAR SALDO NEGATIVO";
                                                            }
                                                        } else {
                                                            message = "EL SALDO QUE INTENTA PAGAR SOBREPASA A LA DEUDA";
                                                        }
                                                    } else {
                                                        message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                    }
                                                } else {
                                                    message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(monto).toUpperCase() + " AL PERMITIDO";
                                                }
                                            } else {
                                                message = "PRODUCTO DESTINO NO ES UN PRESTAMO";
                                            }
                                        }//========================

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

    //Metodo para validar pago orden SPEI
    public String validaOrdenSPEI(RequestDataOrdenPagoDTO orden /*@Context UriInfo ui*/) throws MalformedURLException {
        System.out.println("Entrando a validar las ordenes SPEI");
        EntityManager em = emf.createEntityManager();
        OpaDTO opa = util.opa(orden.getClienteClabe());
        OgsDTO ogs = util.ogs(orden.getOrdernante());
        String folio_origen_spei = "SELECT * FROM auxiliares a WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar() + " AND estatus=2";
        System.out.println("Consulta de validaciones de cuenta :" + folio_origen_spei);
        String message = "";
        ResponseSPEIDTO dtoSPEI = new ResponseSPEIDTO();
        TablasPK urlTbPK = new TablasPK("bankingly_banca_movil", "speipath");

        //Busco en tablas,la tabla para url de SPEI en otro proyecto propio de Fenoreste que conecta a STP
        Tablas tablaSpeiPath = em.find(Tablas.class, urlTbPK);
        URL url = new URL(tablaSpeiPath.getDato1());
        System.out.println("urlFenoreste:" + url);

        try {
            //Hago ping a los servicios de SPEI alojado en otro proyecto
            if (pingURL(url, tablaSpeiPath.getDato3())) {
                try {
                    Auxiliares folio_origen_ = null;
                    boolean bOrigen = false;
                    try {
                        Query query = em.createNativeQuery(folio_origen_spei, Auxiliares.class);
                        //Obtengo el folio origen para tarjeta de debito
                        folio_origen_ = (Auxiliares) query.getSingleResult();
                        bOrigen = true;

                    } catch (Exception e) {
                        System.out.println("Error al buscar producto origen:" + e.getMessage());
                        bOrigen = false;
                    }
                    //Si existe el producto origen
                    if (bOrigen) {
                        double saldo = 0.0;

                        if (caja().toUpperCase().contains("SANNICOLAS")) {
                            //Buscamos el producto para tarjeta de debito
                            TablasDTO tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                            System.out.println("El producto para Tarjeta de debito es:" + tablaProductoTDD);

                            //Si el retiro debe ser de Tarjeta de debito
                            if (folio_origen_.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                                WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(folio_origen_.getAuxiliaresPK().getIdorigenp(), folio_origen_.getAuxiliaresPK().getIdproducto(), folio_origen_.getAuxiliaresPK().getIdauxiliar());
                                //Busco el folio de la tarjeta de debito
                                WsSiscoopFoliosTarjetas1 tarjeta = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                                try {
                                    System.out.println("consultando saldo para idtarjeta:" + tarjeta.getIdtarjeta());
                                    //Obtengo el saldo desde ws de el idTarjeta encontrada
                                    BalanceQueryResponseDto saldo_tarjeta_de_debito = new TarjetaDeDebito().saldoTDD(tarjeta.getWsSiscoopFoliosTarjetasPK(), em);
                                    saldo = saldo_tarjeta_de_debito.getAvailableAmount();
                                    System.out.println("Saldo de la tarjeta de debito es:" + saldo);
                                } catch (Exception e) {
                                    System.out.println("Error al buscar saldo de TDD:" + folio_origen_.getAuxiliaresPK().getIdproducto());
                                    return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";
                                }
                            }

                        }
                        //Valido que el producto de tarjeta de debito si deba operar para banca movil
                        Productos_bankingly cuentasBankingly = em.find(Productos_bankingly.class, folio_origen_.getAuxiliaresPK().getIdproducto());
                        if (cuentasBankingly != null) {
                            //Busco el producto del folio de tarjeta de debito en la tabla productos
                            Productos prOrigen = em.find(Productos.class, folio_origen_.getAuxiliaresPK().getIdproducto());
                            //si el producto no es tipo 2 es decir no es un prestamo            
                            if (prOrigen.getTipoproducto() == 0) {
                                //verifico que el socio le pertenezca ese producto
                                if (folio_origen_.getIdorigen() == ogs.getIdorigen() && folio_origen_.getIdgrupo() == ogs.getIdgrupo() && folio_origen_.getIdsocio() == ogs.getIdsocio()) {
                                    //Verifico el estatus de la cuenta origen
                                    if (folio_origen_.getEstatus() == 2) {
                                        //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir(saldo obtenido desde WS de Alestra)
                                        if (saldo >= orden.getMonto()) {
                                            //valido el minimo o maximo para banca movil
                                            if (minMax(orden.getMonto()).toUpperCase().contains("VALIDO")) {
                                                //Valido el monto maximo por dia
                                                if (MaxPordia(orden.getClienteClabe(), orden.getMonto())) {
                                                    message = "VALIDADO CON EXITO";
                                                } else {
                                                    message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                }
                                            } else {
                                                message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(orden.getMonto()).toUpperCase() + " AL PERMITIDO";
                                            }
                                            //Busco origen para saber quien esta usando el ws service
                                            //Query or = em.createNativeQuery("SELECT * FROM origenes WHERE matriz=0", Origenes.class);
                                            //Origenes ori = (Origenes) or.getSingleResult();
                                            /*//Parto la url completa para obtener basePath
                                    String[] partesUrl = ui.getBaseUri().toString().split("Ws");
                                    String urlBase = partesUrl[0];
                                    //al UrlPath le pongo el nombre del proyecto de SPEI-CSN
                                    String urlComplete = urlBase + "SPEI-CSN/spei/v1.0/srvEnviaOrden";*/
                                            //Busco la url en tablas

                                            /* ResponseSPEIDTO response = metodoEnviarSPEI(orden, url + tablaSpeiPath.getDato2());
                                            System.out.println("Si conecto y el id de la orden es:" + response.getId());*/
                                        } else {
                                            message = "FONDOS INSUFICIENTES PARA COMPLETAR LA TRANSACCION";
                                            dtoSPEI.setId(0);
                                            dtoSPEI.setError(message);
                                        }
                                    } else {
                                        message = "PRODUCTO ORIGEN INACTIVO";
                                        dtoSPEI.setId(0);
                                        dtoSPEI.setError(message);
                                    }
                                } else {
                                    message = "PRODUCTO ORIGEN NO PERTENECE AL SOCIO:" + orden.getOrdernante();

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
                    System.out.println("Error al validar orden SPEI:" + e.getMessage());
                }
            } else {
                message = "SIN CONEXION AL HOST DESTINO:" + url;
            }

        } catch (Exception e) {
            message = e.getMessage();
            System.out.println("Error al realizar tranferenciasSPEI:" + e.getMessage());
            em.close();
            return message;
        } finally {
            em.close();
        }
        return message.toUpperCase();
    }

    //Se consume un servicio que yo desarrolle donde consumo API STP en caso de CSN si alguuien mas usara SPEI desarrollaria otro proyecto especficamente para la caja
    private ResponseSPEIDTO metodoEnviarSPEI(RequestDataOrdenPagoDTO orden) {
        System.out.println("Entrando a metodo para enviar orden SPEI");
        URL urlEndpoint = null;
        String output = "";
        String salida = "";
        ResponseSPEIDTO response = new ResponseSPEIDTO();
        EntityManager em = emf.createEntityManager();
        try {
            org.json.JSONObject request = new JSONObject();
            //Preparo JSON para request en mi propio servicio
            request.put("clabeCliente", orden.getClienteClabe());
            request.put("monto", orden.getMonto());
            request.put("conceptoPago", orden.getConceptoPago());
            request.put("banco", orden.getInstitucionContraparte());
            request.put("beneficiario", orden.getNombreBeneficiario());
            request.put("rfcCurpBeneficiario", orden.getRfcCurpBeneficiario());
            request.put("cuentaBeneficiario", orden.getCuentaBeneficiario());

            //Busco la tabla para el proyecto SPEI 
            TablasPK urlTablaPK = new TablasPK("bankingly_banca_movil", "speipath");
            Tablas tablaSpeiPath = em.find(Tablas.class, urlTablaPK);
            //Obtengo los datos de la tabla para generar la URL de conexion
            URL url = new URL(tablaSpeiPath.getDato1() + tablaSpeiPath.getDato2());
            //Una ves generada la url contacteno el parametro para enviar orden(srvEnviarOrden)
            urlEndpoint = new URL(url + "srvEnviaOrden");
            //Se genera la conexion
            HttpURLConnection conn = (HttpURLConnection) urlEndpoint.openConnection();
            conn.setDoOutput(true);
            //El metodo que utilizo
            conn.setRequestMethod("POST");
            //Tipo de contenido aceptado por el WS
            conn.setRequestProperty("Content-Type", "application/json");
            //Obtengo el Stream
            OutputStream os = conn.getOutputStream();
            //Al stream le paso el request
            os.write(request.toString().getBytes());
            os.flush();

            JSONObject responseJSON = new JSONObject();
            //Obtengo el codigo de respuesta
            int codigoHTTP = conn.getResponseCode();

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            System.out.println("Output from Server .... \n");
            System.out.println("El codigo de respuesta es:" + codigoHTTP);
            if (codigoHTTP == 200) {
                while ((output = br.readLine()) != null) {
                    salida = output;
                    responseJSON = new JSONObject(salida);
                    response.setId(responseJSON.getInt("id"));
                    response.setError(responseJSON.getString("error"));
                }
            } else {
                while ((output = br.readLine()) != null) {
                    salida = output;
                    responseJSON = new JSONObject(salida);
                    response.setId(responseJSON.getInt("id"));
                    response.setError(responseJSON.getString("error"));
                }
            }
            conn.disconnect();
        } catch (Exception ex) {
            response.setId(500);
            System.out.println("Error en conectar al EndPoint SPEI:" + ex.getMessage());
            response.setError("GENERAL:" + ex.getMessage());
            em.close();
            return response;
        } finally {
            em.close();
        }
        return response;
    }

    //valida el monto para banca movil total de transferencia
    public String minMax(Double amount) {
        EntityManager em = emf.createEntityManager();
        String mensaje = "";
        try {
            TablasPK tbPk = new TablasPK("bankingly_banca_movil", "montomaximominimo");
            Tablas tb = em.find(Tablas.class,
                    tbPk);
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
        Tablas tb = em.find(Tablas.class,
                tbPk);
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

    // REALIZA UN PING A LA URL DEL WSDL
    private boolean pingURL(URL url, String tiempo) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(Integer.parseInt(tiempo));
            connection.setReadTimeout(Integer.parseInt(tiempo));
            int codigo = connection.getResponseCode();
            if (codigo == 200) {
                return true;
            }
        } catch (IOException ex) {
            System.out.println("Error al conectarse a URL SPEI: " + ex.getMessage());
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
            em.close();
            System.out.println("Error al crear origen trabajando:" + e.getMessage());
            return "";
        } finally {
            em.close();
        }
        return nombreOrigen.replace(" ", "").toUpperCase();
    }

    public void cerrar() {
        emf.close();
    }
}
