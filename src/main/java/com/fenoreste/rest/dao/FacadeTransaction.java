/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenorest.rest.EnviarSMS.PreparaSMS;
import com.fenoreste.rest.Util.UtilidadesGenerales;
import com.fenoreste.rest.DTO.OgsDTO;
import com.fenoreste.rest.DTO.OpaDTO;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.json.JSONObject;

/**
 *
 * @author Elliot
 */
public abstract class FacadeTransaction<T> {

    Utilidades util = new Utilidades();
    UtilidadesGenerales util2 = new UtilidadesGenerales();

    public FacadeTransaction(Class<T> entityClass) {
    }

    //El identificador de trasnferencia me dice 1=entre mis cuentas 2=a terceros 3=Pago a prestamos
    public BackendOperationResultDTO transferencias(TransactionToOwnAccountsDTO transactionOWN, int identificadorTransferencia, RequestDataOrdenPagoDTO SPEIOrden) {
        EntityManager em = AbstractFacade.conexion();
        Date hoy = new Date();

        System.out.println("TransactionFecha:" + transactionOWN.getValueDate());
        BackendOperationResultDTO backendResponse = new BackendOperationResultDTO();
        backendResponse.setBackendCode("2");
        backendResponse.setBackendMessage("Incorrecto");
        backendResponse.setBackendReference(null);
        backendResponse.setIsError(true);
        backendResponse.setTransactionIdenty("0");
        OpaDTO opaOrigen = util.opa(transactionOWN.getDebitProductBankIdentifier());
        System.out.println(opaOrigen.getIdorigenp() + "-" + opaOrigen.getIdproducto() + "-" + opaOrigen.getIdauxiliar());

        boolean banderaCSN = false;
        ResponseSPEIDTO response = null;
        String messageBackend = "";
        String mensajeBackendResult = "";

        banderaCSN = false;
        boolean banderaTDD = false;
        //Si no es TDD pasa directo hasta aca
        //Si es una transferencia entre mis cuentas
        //if (identificadorTransferencia == 1 && retiro == false && banderaCSN == false) {
        if (identificadorTransferencia == 1 && banderaCSN == false) {
            //Valido la transferencia y devuelvo el mensaje que se produce
            //Valido el origen si es CSN 
            if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                //Valido el producto para retiro
                //Busco el producto configurado para retiros
                messageBackend = validarTransferenciaCSN(transactionOWN, identificadorTransferencia, null);
                backendResponse.setBackendMessage(messageBackend);
                banderaCSN = true;
            } else {
                messageBackend = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
                backendResponse.setBackendMessage(messageBackend);
            }
        }
        //Si es una transferencia terceros dentro de la entidad
        if (identificadorTransferencia == 2) {
            //Valido la transferencia y devuelvo el mensaje que se produce
            //Valido el origen si es CSN 
            if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                //Valido el producto para retiro
                //Busco el producto configurado para retiros
                messageBackend = validarTransferenciaCSN(transactionOWN, identificadorTransferencia, null);
                backendResponse.setBackendMessage(messageBackend);
                banderaCSN = true;
                if (messageBackend.contains("TDD")) {
                    banderaTDD = true;
                }
            } else {
                messageBackend = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
                backendResponse.setBackendMessage(messageBackend);
            }
        }
        //Si es pago a un prestamo
        if (identificadorTransferencia == 3 || identificadorTransferencia == 4) {
            //Valido la transferencia y devuelvo el mensaje que se produce
            //Valido el origen si es CSN 
            if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                //Valido el producto para retiro
                //Busco el producto configurado para retiros
                messageBackend = validarTransferenciaCSN(transactionOWN, identificadorTransferencia, null);
                System.out.println("");
                backendResponse.setBackendMessage(messageBackend);
                banderaCSN = true;
                System.out.println("mensageBakc:" + messageBackend);
                if (messageBackend.contains("TDD")) {
                    banderaTDD = true;
                }
            } else {
                messageBackend = validarPagoAPrestamos(identificadorTransferencia, transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier(), transactionOWN.getClientBankIdentifier());
            }
        } else if (identificadorTransferencia == 5) {
            //Valido la transferencia y devuelvo el mensaje que se produce
            //Valido el origen si es CSN 
            if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                System.out.println("Ordennnnnnnnnnnnnnnnnnnnnnnnnnn SPEIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
                //Valido el producto para retiro
                //Busco el producto configurado para retiros
                messageBackend = validarTransferenciaCSN(transactionOWN, identificadorTransferencia, SPEIOrden);
                System.out.println("mensajeeeee spei:" + messageBackend);
                backendResponse.setBackendMessage(messageBackend);
                banderaCSN = true;
                if (messageBackend.toUpperCase().contains("TDD")) {
                    banderaTDD = true;
                }
            }
        }

        System.out.println("backendMessage:" + backendResponse.getBackendMessage());
        try {
            if (backendResponse.getBackendMessage().toUpperCase().contains("EXITO")) {

                Transferencias transaction = new Transferencias();
                //Si la valicadion se realizo de manera corracta preparo una tabla ttabla historial

                transaction.setTransactionid(transactionOWN.getTransactionId());
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

                OpaDTO opa = util.opa(transaction.getDebitproductbankidentifier());

                //Obtengo los productos origen y destino
                //Origen
                String origenP = "SELECT * FROM auxiliares WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar();
                Query queryOrigen = em.createNativeQuery(origenP, Auxiliares.class);
                Auxiliares aOrigen = (Auxiliares) queryOrigen.getSingleResult();

                //Lo utlizon para los datos a procesar 
                long time = System.currentTimeMillis();
                Timestamp timestamp = new Timestamp(time);

                //Obtengo la sesion para los datos a procesar
                Query sesion = em.createNativeQuery("select text(pg_backend_pid())||'-'||trim(to_char(now(),'ddmmyy'))");
                String sesionc = String.valueOf(sesion.getSingleResult());

                //Obtengo un random que uso en conplemento con referencia
                int rn = (int) (Math.random() * 999999 + 1);

                //Obtener HH:mm:ss.microsegundos
                String fechaArray[] = timestamp.toString().substring(0, 10).split("-");
                String fReal = fechaArray[2] + "/" + fechaArray[1] + "/" + fechaArray[0];
                String referencia = String.valueOf(rn) + "" + String.valueOf(transaction.getSubtransactiontypeid()) + "" + String.valueOf(transaction.getTransactiontypeid() + "" + fReal.replace("/", ""));

                //Leemos fechatrabajo e idusuario
                String fechaTrabajo = "SELECT to_char(fechatrabajo,'yyyy-MM-dd HH:mm:ss') FROM ORIGENES LIMIT 1";
                Query fechaTrabajo_ = em.createNativeQuery(fechaTrabajo);
                String fechaTr_ = String.valueOf(fechaTrabajo_.getSingleResult());

                //Buscamos el usuario para la banca movil para la tabla de datos a procesar y para las polizas
                TablasPK idusuarioPK = new TablasPK("bankingly_banca_movil", "usuario_banca_movil");
                Tablas tbUsuario_ = em.find(Tablas.class, idusuarioPK);

                //Conviento a DateTime la fecha de trabajo
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDate = LocalDateTime.parse(fechaTr_, dtf);

                Timestamp fecha_transferencia = Timestamp.valueOf(localDate);
                Productos prDestino = null;

                //Leo la tabla donde se almacenan datos temporales de los datos a procesar
                Procesa_pago_movimientos procesaOrigen = new Procesa_pago_movimientos();
                //Preoaro el registro del abono
                Procesa_pago_movimientos procesaDestino = new Procesa_pago_movimientos();

                //Si es un spei lo identifico porque aqui va a una cuenta contable
                if (identificadorTransferencia == 5) {//tipo de orden SPEI
                    //Preparo en temporales los datos para el cargo
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
                    procesaDestino.setAuxiliaresPK(aPKSPEI);
                    procesaDestino.setFecha(fecha_transferencia);
                    procesaDestino.setIdusuario(Integer.parseInt(tbUsuario_.getDato1()));
                    procesaDestino.setSesion(sesionc);
                    procesaDestino.setReferencia(referencia);
                    procesaDestino.setIdorigen(aOrigen.getIdorigen());
                    procesaDestino.setIdgrupo(aOrigen.getIdgrupo());
                    procesaDestino.setIdsocio(aOrigen.getIdsocio());
                    procesaDestino.setCargoabono(1);
                    procesaDestino.setIdcuenta("20407160101067");//Falta parametrizarla
                    procesaDestino.setMonto(transaction.getAmount());
                    procesaDestino.setIva(0.0);
                    procesaDestino.setTipo_amort(0);
                    procesaDestino.setSai_aux("");
                    em.getTransaction().begin();
                    em.persist(procesaDestino);
                    em.getTransaction().commit();

                } else {
                    //Obtengo el opa destino
                    OpaDTO opaD = util.opa(transaction.getCreditproductbankidentifier());
                    //Destino
                    String destinoP = "SELECT * FROM auxiliares WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
                    Query queryDestino = em.createNativeQuery(destinoP, Auxiliares.class);
                    Auxiliares aDestino = (Auxiliares) queryDestino.getSingleResult();

                    //Obtengo el producto Destino
                    prDestino = em.find(Productos.class, aDestino.getAuxiliaresPK().getIdproducto());

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

                    //Guardo registros para abono
                    em.getTransaction().begin();
                    em.persist(procesaDestino);
                    em.getTransaction().commit();

                }

                String consulta_datos_procesar = "";
                int total_procesados = 0;
                boolean finish = false;
                boolean clean = false;
                Query procesa_movimiento = null;
                //Si los datos en la tabla temporal el cargo y abono se guardaron correctamente
                //Ejecutamos la funcion para distribuir el capital
                //Solo para CSN
                if (banderaCSN) {
                    DoWithdrawalAccountResponse.Return doWithdrawalAccountResponse = new DoWithdrawalAccountResponse.Return();
                    WsSiscoopFoliosTarjetas1 tarjetas = null;
                    if (identificadorTransferencia != 5) {//Entran todos los tipos de transferencia excepto SPEI
                        //Si es desde la TDD(Aqui ya se leyo el ws de Alestra y esta levantado por eso trae la etiqueta de TDD)
                        if (backendResponse.getBackendMessage().contains("TDD")) {                            //Retiramos de la TDD
                            tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(opaOrigen.getIdorigenp(), opaOrigen.getIdproducto(), opaOrigen.getIdauxiliar(), em);
                            doWithdrawalAccountResponse = new TarjetaDeDebito().retiroTDD(tarjetas, procesaOrigen.getMonto());
                            if (doWithdrawalAccountResponse.getCode() > 0) {
                                consulta_datos_procesar = "SELECT sai_bankingly_aplica_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                                procesa_movimiento = em.createNativeQuery(consulta_datos_procesar);
                                total_procesados = Integer.parseInt(String.valueOf(procesa_movimiento.getSingleResult()));
                                //Si la operacion se aplico de manera corracta lo dejamos asi
                                if (total_procesados > 0) {
                                    finish = true;
                                } else {//Si no se aplico el movimiento pero como ya se habia retirado de la TDD entonce se lo devolvemos
                                    System.out.println("Aqui programa el deposito a la tdd nuevamente");
                                }
                            } else {
                                backendResponse.setBackendMessage("LOS WS PARA RETIRO DE LA TARJETA DE DEBITO NO ESTAN ACTIVOS");
                            }
                        } else {//Solo para pruebas
                            consulta_datos_procesar = "SELECT sai_bankingly_aplica_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                            Query procesa_pago = em.createNativeQuery(consulta_datos_procesar);
                            total_procesados = Integer.parseInt(String.valueOf(procesa_pago.getSingleResult()));
                            if (total_procesados > 0) {
                                finish = true;
                            } else {
                                backendResponse.setBackendMessage("ERROR AL PROCESAR LOS MOVIMIENTOS");
                            }
                        }
                    } else {
                        //entra SPEI
                        tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(opaOrigen.getIdorigenp(), opaOrigen.getIdproducto(), opaOrigen.getIdauxiliar(), em);
                        //Retiramos de la TDD
                        doWithdrawalAccountResponse = new TarjetaDeDebito().retiroTDD(tarjetas, procesaOrigen.getMonto());
                        //si el retiro se efectuo
                        if (doWithdrawalAccountResponse.getCode() > 0) {
                            //Enviamos la orden SPEI 
                            response = metodoEnviarSPEI(SPEIOrden);
                            //Si la orden se envio correctamente
                            if (response.getId() > 0) {
                                //Ahora si aplicamos la operacion
                                consulta_datos_procesar = "SELECT sai_bankingly_aplica_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";

                                procesa_movimiento = em.createNativeQuery(consulta_datos_procesar);
                                total_procesados = Integer.parseInt(String.valueOf(procesa_movimiento.getSingleResult()));
                                //Si la operacion se aplico de manera corracta lo dejamos asi
                                if (total_procesados > 0) {
                                    mensajeBackendResult = "TRANSACCION EXITOSA";
                                    clean = true;
                                } else {//Si no se aplico el movimiento pero como ya se habia retirado de la TDD entonce se lo devolvemos
                                    backendResponse.setBackendMessage("NO SE APLICO EL MOVIMIENTO DEVOLVER SLDO A LA TDD");
                                    System.out.println("Aqui programa el deposito a la tdd nuevamente");
                                }
                            } else {
                                backendResponse.setBackendMessage(response.getError() + "SE REGRESO EL SALDO DE LA TDD");
                            }

                        } else {
                            backendResponse.setBackendMessage("LOS WS PARA RETIRO DE LA TARJETA DE DEBITO NO ESTAN ACTIVOS");
                        }

                    }
                } else {
                    //El resto de las cajas
                    consulta_datos_procesar = "SELECT sai_bankingly_aplica_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                    procesa_movimiento = em.createNativeQuery(consulta_datos_procesar);
                    total_procesados = Integer.parseInt(String.valueOf(procesa_movimiento.getSingleResult()));
                    if (total_procesados > 0) {
                        finish = true;
                    } else {
                        backendResponse.setBackendMessage("ERROR AL PROCESAR LOS MOVIMIENTOS");
                    }
                }

                if (finish) {
                    if (identificadorTransferencia != 5) {
                        //Si fue un pago a prestamo propio o de Tercero
                        if (prDestino.getTipoproducto() == 2) {
                            //Obtengo los datos(Seguro hipotecario,comisones cobranza,interes ect.) Me muestra de que manera se distribuyo mi pago
                            String distribucion = "SELECT sai_bankingly_detalle_transaccion_aplicada('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                            System.out.println("DistribucionConsulta:" + distribucion);
                            Query procesa_distribucion = em.createNativeQuery(distribucion);
                            String distribucionProcesada = String.valueOf(procesa_distribucion.getSingleResult());
                            System.out.println("Distribucion_Procesada:" + distribucionProcesada);
                            //Guardo en una lista los montos que se han procesado
                            String ArrayDistribucion[] = distribucionProcesada.split("\\|");

                            //Mensaje personalizado para CSN
                            if (banderaCSN) {
                                mensajeBackendResult = "PAGO EXITOSO" + "\n"
                                        + "SEGURO HIPOTECARIO    :" + ArrayDistribucion[0] + "\n "
                                        + "COMISON COBRANZA      :" + ArrayDistribucion[1] + "\n "
                                        + "INTERES MORATORIO     :" + ArrayDistribucion[2] + "\n "
                                        + "IVA INTERES MORATORIO :" + ArrayDistribucion[3] + "\n"
                                        + "INTERES ORDINARIO     :" + ArrayDistribucion[4] + "\n"
                                        + "IVA INTERES ORDINARIO :" + ArrayDistribucion[5] + "\n"
                                        + "CAPITAL               :" + ArrayDistribucion[5] + "\n"
                                        + "ADELANTO DE INTERES   :" + ArrayDistribucion[6] + "\n \n \n \n"
                                        + "EVITA ATRASOS, EN TU PRESTAMO EL PAGO DE INTERESES DEBBE SER MENSUAL";//Para adelanto de interese solo aplicaria para los productos configurados
                            } else {
                                mensajeBackendResult = "PAGO EXITOSO" + "\n"
                                        + "SEGURO HIPOTECARIO    :" + ArrayDistribucion[0] + "\n "
                                        + "COMISON COBRANZA      :" + ArrayDistribucion[1] + "\n "
                                        + "INTERES MORATORIO     :" + ArrayDistribucion[2] + "\n "
                                        + "IVA INTERES MORATORIO :" + ArrayDistribucion[3] + "\n"
                                        + "INTERES ORDINARIO     :" + ArrayDistribucion[4] + "\n"
                                        + "IVA INTERES ORDINARIO :" + ArrayDistribucion[5] + "\n"
                                        + "CAPITAL               :" + ArrayDistribucion[5] + "\n"
                                        + "ADELANTO DE INTERES   :" + ArrayDistribucion[6] + "\n \n \n \n \n \n";//Para adelanto de interese solo aplicaria para los productos configurados
                            }
                            clean = true;
                        } else if (prDestino.getTipoproducto() == 0) {
                            mensajeBackendResult = "TRANSACCION EXITOSA";
                            clean = true;
                        }
                    }
                }

                if (clean) {
                    //Aplico la  funcion para limpiar la tabla donde estaban los pagos cargo y abono
                    String consulta_termina_transaccion = "SELECT sai_bankingly_termina_transaccion('" + fechaTr_.substring(0, 10) + "'," + procesaOrigen.getIdusuario() + ",'" + procesaOrigen.getSesion() + "','" + procesaOrigen.getReferencia() + "')";
                    Query termina_transaccion = em.createNativeQuery(consulta_termina_transaccion);
                    int registros_limpiados = Integer.parseInt(String.valueOf(termina_transaccion.getSingleResult()));
                    System.out.println("Registros Limpiados con exito:" + registros_limpiados);
                    if(util2.obtenerOrigen(em).toUpperCase().replace(" ","").contains("SANNICOLAS")){
                        if(transaction.getSubtransactiontypeid()==1 && transaction.getTransactionid()==1){
                            new PreparaSMS().enviaSMS_CSN(em,String.valueOf(transaction.getAmount()),1,transaction.getDebitproductbankidentifier(),transaction.getCreditproductbankidentifier(),transaction.getClientbankidentifier());
                        }
                        
                    }

                    //Guardo en una tabla el hisotiral de la operacion realizada
                    em.getTransaction().begin();
                    em.persist(transaction);
                    em.getTransaction().commit();

                    backendResponse.setBackendCode("1");
                    backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                    backendResponse.setIsError(false);
                    //Si fuera transferencia SPEI se devuelve el idTransaccion(Devuelvo el id de la orden SPEI)
                    if (identificadorTransferencia == 5) {
                        backendResponse.setTransactionIdenty(String.valueOf(response.getId()));
                    } else {
                        backendResponse.setTransactionIdenty(String.valueOf(transaction.getIdtransaction()));
                    }

                    backendResponse.setBackendMessage(mensajeBackendResult);
                    backendResponse.setBackendReference(transaction.getTransactionid().toString());

                }

            }

        } catch (Exception e) {
            if (backendResponse.getBackendMessage().contains("EXITO")) {
                backendResponse.setBackendMessage("Error:" + e.getMessage());
            } else {
                backendResponse.setBackendMessage(e.getMessage());
            }
            System.out.println("Error Al ejecutar transferencia:" + e.getMessage());
            return backendResponse;
        }

        return backendResponse;
    }

    public String voucherFileCreate(String idtransaccion) {
        EntityManager em = AbstractFacade.conexion();
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

            return "";
        }

    }

    //Metodo para validar transferencia entre cuentas propias
    public String validarTransferenciaEntreMisCuentas(String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = AbstractFacade.conexion();
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        String message = "";
        boolean banderaGrupo = false;
        boolean banderaProductosDeposito = false;
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
                if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                    Tablas tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                    if (ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato1())) {
                        //Si es la TDD               
                        WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                        WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                        try {
                            System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                            BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
                            saldo = saldoTDD.getAvailableAmount();
                            message = "TDD";
                            //saldo = 200.0;
                            System.out.println("Saldo TDD:" + saldo);

                        } catch (Exception e) {
                            System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                            return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                        }
                    }
                }

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

                                                            /*=======================REGLAS DE NEGOCIO==================================*/
                                                            //Valido que la cuenta origen para CSN esat un grupo de retiro configurado
                                                            if (util2.obtenerOrigen(em).contains("SANNICOL")) {
                                                                //Buscamos que el producto origen pertenezca al grupo de retiro
                                                                Tablas tb = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_retiro");
                                                                if (ctaOrigen.getIdgrupo() == Integer.parseInt(tb.getDato1())) {
                                                                    //Ahora verifico que el destino perteneneza al grupo de depositos
                                                                    Tablas tbRetiro = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_deposito");
                                                                    String cadena[] = tbRetiro.getDato1().split("\\|");

                                                                    List list = Arrays.asList(cadena);
                                                                    for (int i = 0; i < list.size(); i++) {
                                                                        if (ctaOrigen.getIdgrupo() == Integer.parseInt(String.valueOf(list.get(i)))) {
                                                                            banderaGrupo = true;
                                                                        }
                                                                    }
                                                                    if (banderaGrupo) {
                                                                        //valido que el producto acepte depositos
                                                                        Tablas tbDeposito = util2.busquedaTabla(em, "bankingly_banca_movil", "productos_deposito");
                                                                        System.out.println("tabla Productos deposito:" + tbDeposito.getDato2());
                                                                        String productos_deposito[] = tbDeposito.getDato2().split("\\|");

                                                                        List list_deposito = Arrays.asList(productos_deposito);
                                                                        for (int i = 0; i < list_deposito.size(); i++) {
                                                                            System.out.println("prod pos " + i + ":" + list_deposito.get(i));
                                                                            if (ctaDestino.getAuxiliaresPK().getIdproducto() == Integer.parseInt(String.valueOf(list_deposito.get(i)))) {
                                                                                banderaProductosDeposito = true;
                                                                            }
                                                                        }
                                                                        if (banderaProductosDeposito) {
                                                                            message = message + " VALIDADO CON EXITO";
                                                                        } else {
                                                                            message = "PRODUCTO NO CONFIGURADO PARA RECIBIR DEPOSITOS";
                                                                        }
                                                                    } else {
                                                                        message = "GRUPO NO CONFIGURADO PARA DEPOSITOS";
                                                                    }
                                                                } else {
                                                                    message = "SOCIO NO PERTENECE AL GRUPO DE RETIRO";
                                                                }

                                                            } else {
                                                                message = "VALIDADO CON EXITO";
                                                            }
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
                                                message = "PRODUCTO DENTINO SOLO ACEPTA PAGOS(PRESTAMO)";
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
            message = "ERROR AL PROCESAR CONSULTA VALIDACIONES DE DATOS";
            System.out.println("Error en validacion transferencia entre mis cuentas:" + e.getMessage());
            return message;
        }
        return message.toUpperCase();
    }

    //Metodo para validar transferencia a otras cuentas
    public String validarTransferenciaATerceros(String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = AbstractFacade.conexion();
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();

        String cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        String message = "";
        boolean banderaGrupo = false;
        boolean banderaProductosDeposito = false;
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
                if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                     Tablas tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                    if (ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato1())) {
                    
                    WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                    WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                    try {
                        System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                        BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
                        message = "TDD";
                        saldo = saldoTDD.getAvailableAmount();
                        //saldo = 200.0;
                    } catch (Exception e) {
                        System.out.println("Error al buscar saldo de TDD:" + ctaOrigen.getAuxiliaresPK().getIdproducto());
                        return message = "ERROR AL CONSUMIR WS TDD Y OBTENER SALDO DEL PRODUCTO";

                    }
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
                                            //Verifico si de verdad cuenta destinono pertence al mismo socio ya que es transferencia a tercero
                                            if (ctaOrigen.getIdsocio() != ctaDestino.getIdsocio()) {
                                                //Valido que el producto destino no sea un prestamo
                                                if (productoDestino.getTipoproducto() == 0) {
                                                    //valido el minimo o maximo para banca movil
                                                    if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                        //Valido el monto maximo por dia
                                                        if (MaxPordia(opaOrigen, monto)) {

                                                            /*=======================REGLAS DE NEGOCIO==================================*/
                                                            //Valido que la cuenta origen para CSN esat un grupo de retiro configurado
                                                            if (util2.obtenerOrigen(em).contains("SANNICOL")) {
                                                                //Buscamos que el producto origen pertenezca al grupo de retiro
                                                                Tablas tb = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_retiro");
                                                                System.out.println("tb:" + tb);
                                                                if (ctaOrigen.getIdgrupo() == Integer.parseInt(tb.getDato1())) {
                                                                    //Ahora verifico que el destino perteneneza al grupo de depositos
                                                                    Tablas tbRetiro = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_deposito");
                                                                    String cadena[] = tbRetiro.getDato1().split("\\|");
                                                                    List list = Arrays.asList(cadena);
                                                                    for (int i = 0; i < list.size(); i++) {
                                                                        if (ctaOrigen.getIdgrupo() == Integer.parseInt(String.valueOf(list.get(i)))) {
                                                                            banderaGrupo = true;
                                                                        }
                                                                    }
                                                                    if (banderaGrupo) {
                                                                        //valido que el producto acepte depositos
                                                                        Tablas tbDeposito = util2.busquedaTabla(em, "bankingly_banca_movil", "productos_deposito");
                                                                        String productos_deposito[] = tbDeposito.getDato2().split("\\|");
                                                                        List list_deposito = Arrays.asList(productos_deposito);
                                                                        for (int i = 0; i < list_deposito.size(); i++) {
                                                                            if (ctaDestino.getAuxiliaresPK().getIdproducto() == Integer.parseInt(String.valueOf(list_deposito.get(i)))) {
                                                                                banderaProductosDeposito = true;
                                                                            }
                                                                        }
                                                                        if (banderaProductosDeposito) {
                                                                            message = message + " VALIDADO CON EXITO";
                                                                        } else {
                                                                            message = "PRODUCTO NO CONFIGURADO PARA RECIBIR DEPOSITOS";
                                                                        }
                                                                    } else {
                                                                        message = "GRUPO NO CONFIGURADO PARA DEPOSITOS";
                                                                    }
                                                                } else {
                                                                    message = "SOCIO NO PERTENECE AL GRUPO DE RETIRO";
                                                                }
                                                                ///Terminan validaciones para CSN
                                                            } else {
                                                                message = "VALIDADO CON EXITO";
                                                            }
                                                        } else {
                                                            message = "MONTO TRASPASA EL PERMITIDO DIARIO";
                                                        }
                                                    } else {
                                                        message = "EL SALDO QUE INTENTA TRANSFERIR ES " + minMax(monto).toUpperCase() + " AL PERMITIDO";
                                                    }
                                                } else {
                                                    message = "PRODUCTO DESTINO SOLO ACEPTA PAGOS(PRESTAMO)";
                                                }
                                            } else {
                                                message = "EL TIPO DE TRANSFERENCIA ES A TERCEROS PERO TU CUENTA DESTINO PERTENECE AL MISMO SOCIO";
                                            }

                                        } else {
                                            message = "PRODUCTO DESTINO NO OPERA PARA BANCA MOVIL";
                                        }
                                    } else {
                                        message = "PRODUCTO DESTINO ESTA INACTIVO";
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
            message = e.getMessage();
            System.out.println("Errro al validar transferencia a terceros:" + e.getMessage());
            return message;
        }
        return message.toUpperCase();
    }
    //Metodo para validar pago a prestamos

    public String validarPagoAPrestamos(int identificadorTr, String opaOrigen, Double monto, String opaDestino, String clientBankIdentifier) {
        EntityManager em = AbstractFacade.conexion();
        OpaDTO opaO = util.opa(opaOrigen);
        OpaDTO opaD = util.opa(opaDestino);
        OgsDTO ogs = util.ogs(clientBankIdentifier);

        boolean identificador_prestamo_propio = false;
        boolean identificador_prestamo_tercero = false;
        boolean validador_cuentas_destino = false;
        boolean bDestino = false;
        boolean banderaProductosDeposito = false;
        boolean banderaGrupo = false;
        Auxiliares ctaOrigen = null;
        Productos productoDestino = null;

        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaO.getIdorigenp() + " AND idproducto=" + opaO.getIdproducto() + " AND idauxiliar=" + opaO.getIdauxiliar()
                + " AND  idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio();

        String cuentaDestino = "";
        if (identificadorTr == 3) {
            cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        } else if (identificadorTr == 4) {
            cuentaDestino = "SELECT * FROM auxiliares a WHERE idorigenp=" + opaD.getIdorigenp() + " AND idproducto=" + opaD.getIdproducto() + " AND idauxiliar=" + opaD.getIdauxiliar();
        }
        String message = "";
        try {
            boolean bOrigen = false;
            try {
                Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
                //Obtengo el producto origen
                ctaOrigen = (Auxiliares) query.getSingleResult();
                bOrigen = true;
            } catch (Exception e) {
                System.out.println("Error cuando se intento validar el origen");
            }
            if (bOrigen) {

                Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());

                if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                    Tablas tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
                    //Si el pago del prestamo se esta haciendo desde la TDD
                    if (ctaOrigen.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaProductoTDD.getDato2())) {
                        WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(ctaOrigen.getAuxiliaresPK().getIdorigenp(), ctaOrigen.getAuxiliaresPK().getIdproducto(), ctaOrigen.getAuxiliaresPK().getIdauxiliar());
                        WsSiscoopFoliosTarjetas1 tarjetas = new TarjetaDeDebito().buscaTarjetaTDD(foliosPK.getIdorigenp(), foliosPK.getIdproducto(), foliosPK.getIdauxiliar(), em);
                        try {
                            System.out.println("consultando saldo para idtarjeta:" + tarjetas.getIdtarjeta());
                            BalanceQueryResponseDto saldoTDD = new TarjetaDeDebito().saldoTDD(tarjetas.getWsSiscoopFoliosTarjetasPK(), em);
                            message = "TDD";
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
                                try {
                                    //Busco la cuenta destino
                                    Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                                    ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                                    System.out.println("opaD:" + opaD.getIdproducto());
                                    productoDestino = em.find(Productos.class, opaD.getIdproducto());
                                    bDestino = true;
                                } catch (Exception e) {
                                    System.out.println("Error al buscar cuenta destino:" + e.getMessage());
                                }

                                if (bDestino) {
                                    //Busco el producto destino
                                    productoDestino = em.find(Productos.class, opaD.getIdproducto());
                                    //Valido que la cuenta destino este activa
                                    System.out.println("hasta aqui llego:" + productoDestino.getTipoproducto());
                                    if (ctaDestino.getEstatus() == 2) {
                                        //Valido que cuenta destino pertenezca al mismo socio
                                        if (identificadorTr == 3) {
                                            if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
                                                identificador_prestamo_propio = true;
                                            } else {
                                                message = "CUENTA DESTINO NO PERTENECE AL MISMO SOCIO";
                                            }
                                        } else if (identificadorTr == 4) {
                                            if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
                                                message = "TU TIPO DE PAGO SE IDENTIFICA COMO TERCERO PERO LA CUENTA DESTINO PERTENECE AL MISMO SOCIO";
                                            } else {
                                                identificador_prestamo_tercero = true;
                                            }
                                        }

                                        if (identificador_prestamo_propio || identificador_prestamo_tercero) {
                                            validador_cuentas_destino = true;
                                        }

                                        if (validador_cuentas_destino) {
                                            //Valido que el producto destino tercero sea un prestamo
                                            if (productoDestino.getTipoproducto() == 2) {
                                                //valido el minimo o maximo para banca movil
                                                if (minMax(monto).toUpperCase().contains("VALIDO")) {
                                                    //Valido el monto maximo por dia
                                                    if (MaxPordia(opaOrigen, monto)) {
                                                        if (monto <= Double.parseDouble(ctaDestino.getSaldo().toString())) {
                                                            if (monto > 0) {
                                                                /*=======================REGLAS DE NEGOCIO==================================*/
                                                                //Valido que la cuenta origen para CSN esta un grupo de retiro configurado
                                                                if (util2.obtenerOrigen(em).toUpperCase().contains("SANNICOL")) {
                                                                    //Buscamos que el producto origen pertenezca al grupo de retiro
                                                                    Tablas tb = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_retiro");
                                                                    if (ctaOrigen.getIdgrupo() == Integer.parseInt(tb.getDato1())) {
                                                                        //Ahora verifico que el destino perteneneza al grupo de depositos
                                                                        Tablas tbRetiro = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_deposito");
                                                                        String cadena[] = tbRetiro.getDato1().split("\\|");
                                                                        List list = Arrays.asList(cadena);
                                                                        for (int i = 0; i < list.size(); i++) {
                                                                            if (ctaOrigen.getIdgrupo() == Integer.parseInt(String.valueOf(list.get(i)))) {
                                                                                banderaGrupo = true;
                                                                            }
                                                                        }
                                                                        if (banderaGrupo) {
                                                                            //valido que el producto acepte depositos
                                                                            Tablas tbDeposito = util2.busquedaTabla(em, "bankingly_banca_movil", "productos_deposito");
                                                                            String productos_deposito[] = tbDeposito.getDato2().split("\\|");
                                                                            List list_deposito = Arrays.asList(productos_deposito);
                                                                            for (int i = 0; i < list_deposito.size(); i++) {
                                                                                if (ctaDestino.getAuxiliaresPK().getIdproducto() == Integer.parseInt(String.valueOf(list_deposito.get(i)))) {
                                                                                    banderaProductosDeposito = true;
                                                                                }
                                                                            }
                                                                            if (banderaProductosDeposito) {
                                                                                //Para CSN el producto de prestamos ya sea tercero o propio no debe estar en Moroso
                                                                                String b_cartera = "SELECT cartera FROM carteravencida WHERE "
                                                                                        + " idorigenp=" + ctaDestino.getAuxiliaresPK().getIdorigenp()
                                                                                        + " AND idproducto=" + ctaDestino.getAuxiliaresPK().getIdproducto()
                                                                                        + " AND idauxiliar=" + ctaDestino.getAuxiliaresPK().getIdauxiliar();

                                                                                Query query_cartera = em.createNativeQuery(b_cartera);
                                                                                String cartera = String.valueOf(query_cartera.getSingleResult());
                                                                                if (cartera.toUpperCase().equals("M")) {
                                                                                    message = "ESTATUS DE PRODUCTO:MOROSO";
                                                                                } else {
                                                                                    message = message + " VALIDADO CON EXITO";
                                                                                }
                                                                            } else {
                                                                                message = "PRODUCTO NO CONFIGURADO PARA RECIBIR DEPOSITOS";
                                                                            }
                                                                        } else {
                                                                            message = "GRUPO NO CONFIGURADO PARA RECIBIR DEPOSITOS";
                                                                        }
                                                                    } else {
                                                                        message = "SOCIO NO PERTENECE AL GRUPO DE RETIRO";
                                                                    }

                                                                } else {
                                                                    message = "VALIDADO CON EXITO";
                                                                }
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
                                            message = message;
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
            message = e.getMessage();
            System.out.println("Error al realizar pago a prestamo:" + e.getMessage());
            return message;
        }
        System.out.println("el mensaje es:" + message);

        return message.toUpperCase();
    }
    //Metodo para validar pago orden SPEI

    public String validaOrdenSPEI(RequestDataOrdenPagoDTO orden /*@Context UriInfo ui*/) throws MalformedURLException {
        System.out.println("Entrando a validar las ordenes SPEI");
        EntityManager em = AbstractFacade.conexion();
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
                System.out.println("Si hay Ping a spei");
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

                        if (util2.obtenerOrigen(em).contains("SANNICOLAS")) {
                            //Buscamos el producto para tarjeta de debito
                            Tablas tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
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
                                    message = "TDD";
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
                                        //Sumamos el moto de comision+el monto a enviar
                                        //Buscamos el registro en tablas
                                        Tablas tb_comision_spei = util2.busquedaTabla(em, "bankingly_banca_movil", "comision_spei");
                                        double total_pago = orden.getMonto() + (Double.parseDouble(tb_comision_spei.getDato1()) * 0.16);//El monto de comision por el IVA
                                        if (saldo >= total_pago) {
                                            //valido el minimo o maximo para banca movil
                                            if (minMax(orden.getMonto()).toUpperCase().contains("VALIDO")) {
                                                //Valido el monto maximo por dia
                                                if (MaxPordia(orden.getClienteClabe(), orden.getMonto())) {
                                                    //Valido que la cuenta origen para CSN esat un grupo de retiro configurado
                                                    if (util2.obtenerOrigen(em).contains("SANNICOL")) {
                                                        //Buscamos que el producto origen pertenezca al grupo de retiro
                                                        Tablas tb = util2.busquedaTabla(em, "bankingly_banca_movil", "grupo_retiro");
                                                        if (folio_origen_.getIdgrupo() == Integer.parseInt(tb.getDato1())) {
                                                            message = message + " VALIDADO CON EXITO";
                                                        } else {
                                                            message = "SOCIO NO PERTENECE AL GRUPO DE RETIRO";
                                                        }
                                                    } else {
                                                        message = "VALIDADO CON EXITO";
                                                    }
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
            return message;
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
        EntityManager em = AbstractFacade.conexion();
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

            return response;
        }
        return response;
    }

    //valida el monto para banca movil total de transferencia
    public String minMax(Double amount) {
        EntityManager em = AbstractFacade.conexion();
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

            System.out.println("Error al validar monto min-max:" + e.getMessage());
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
        EntityManager em = AbstractFacade.conexion();
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

            System.out.println("Error al validar permitido diario:" + e.getMessage());
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

    public boolean actividad_horario() {
        EntityManager em = AbstractFacade.conexion();
        boolean bandera_ = false;
        try {
            if (util2.actividad(em)) {
                bandera_ = true;
            }
        } catch (Exception e) {
            System.out.println("Error al verificar el horario de actividad");
        }

        return bandera_;
    }

    //Metodo solo para CSN aplicando reglas
    public String validarTransferenciaCSN(TransactionToOwnAccountsDTO transactionOWN, int identificadorTransferencia, RequestDataOrdenPagoDTO SPEIOrden) {
        EntityManager em = AbstractFacade.conexion();
        String mensaje = "";
        String comple = "";
        try {
            System.out.println("El identificado de transferencia es:" + identificadorTransferencia);
            WsSiscoopFoliosTarjetas1 tarjeta = null;
            //Buscamos el producto para TDD en tablas 
            Tablas tablaProductoTDD = new TarjetaDeDebito().productoTddwebservice(em);
            System.out.println("Producto_para_tarjeta_de_debito:" + tablaProductoTDD.getDato2());
            //Busco el producto configurado para retiros
            Tablas tabla_retiro = util2.busquedaTabla(em, "bankingly_banca_movil", "producto_retiro");
            System.out.println("Producto para retiro es:" + tabla_retiro.getDato1());
            //Bandera que me sirve para decir si existe o no la tdd
            boolean tddEncontrada = false;
            OpaDTO opaOrigen = util.opa(transactionOWN.getDebitProductBankIdentifier());
            //Si el producto configurado para retiros no es la tdd entra aqui
            if (opaOrigen.getIdproducto() == Integer.parseInt(tabla_retiro.getDato1())) {
                System.out.println("Es el producto configurado para retiro");
                try {
                    //Si la tdd es el producto conffgurado para              retiros
                    if (Integer.parseInt(tablaProductoTDD.getDato2()) == Integer.parseInt(tabla_retiro.getDato1())) {
                        System.out.println("si es la TDD");
                        try {
                            //Buscando la tarjeta de debito 
                            tarjeta = new TarjetaDeDebito().buscaTarjetaTDD(opaOrigen.getIdorigenp(), opaOrigen.getIdproducto(), opaOrigen.getIdauxiliar(), em);
                            System.out.println("Los registros para el Folio son:" + tarjeta);

                            tddEncontrada = true;
                        } catch (Exception e) {
                            System.out.println("El folio para TDD no existe");
                        }
                        //si se encontro la Tarjeta de debito
                        if (tddEncontrada) {
                            //Verifico el estatuso de la TDD
                            //Si la tarjeta esta activa
                            if (tarjeta.getActiva()) {
                                //Valido segun sea el tipo de transferencia
                                //Cuentas propias
                                if (identificadorTransferencia == 1) {
                                    System.out.println("Es una transferencia a cuenta propia");
                                    mensaje = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getCreditProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                }
                                //Terceros dentro de la entidad
                                if (identificadorTransferencia == 2) {
                                    System.out.println("Es una transferencia a tercero dentro de la entidad");
                                    mensaje = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getCreditProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                }
                                //Pago de prestamo propio ---Falta pago de prestamo tercero
                                if (identificadorTransferencia == 3 || identificadorTransferencia == 4) {
                                    System.out.println("Es un prestamo");
                                    mensaje = validarPagoAPrestamos(identificadorTransferencia, transactionOWN.getDebitProductBankIdentifier(),
                                            transactionOWN.getAmount(),
                                            transactionOWN.getCreditProductBankIdentifier(),
                                            transactionOWN.getClientBankIdentifier());
                                }
                                if (identificadorTransferencia == 5) {//Si es una orden SPEI  
                                    System.out.println("Es una orden SPEI");
                                    //Validamos la orden SPEI
                                    mensaje = validaOrdenSPEI(SPEIOrden);
                                }
                            } else {
                                mensaje = "ESTATUS TARJETA DE DEBITO:INACTIVA";
                            }
                        } else {
                            mensaje = "NO EXISTE FOLIO PARA LA TARJETA DE DEBITO";
                        }

                    } else {//Solo para pruebas
                        //Si no TDD de donde se esta transfiriendo
                        //Valido segun sea el tipo de transferencia
                        //Cuentas propias
                        if (identificadorTransferencia == 1) {
                            mensaje = validarTransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(),
                                    transactionOWN.getAmount(),
                                    transactionOWN.getCreditProductBankIdentifier(),
                                    transactionOWN.getClientBankIdentifier());
                        }
                        //Terceros dentro de la entidad
                        if (identificadorTransferencia == 2) {
                            mensaje = validarTransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(),
                                    transactionOWN.getAmount(),
                                    transactionOWN.getCreditProductBankIdentifier(),
                                    transactionOWN.getClientBankIdentifier());
                        }
                        //Pago de prestamo propio ---Falta pago de prestamo tercero
                        if (identificadorTransferencia == 3 || identificadorTransferencia == 4) {
                            mensaje = validarPagoAPrestamos(identificadorTransferencia, transactionOWN.getDebitProductBankIdentifier(),
                                    transactionOWN.getAmount(),
                                    transactionOWN.getCreditProductBankIdentifier(),
                                    transactionOWN.getClientBankIdentifier());
                        }
                        if (identificadorTransferencia == 5) {
                            mensaje = "SOLO SE PERMITEN ENVIAR ORDENES SPEI DESDE TARJETA DE DEBITO";
                        }
                    }
                } catch (Exception e) {
                    System.out.println("NO SE PUDIERON VALIDAR LOS DATOS PARA LA TRANSFERENCIA:" + e.getMessage());
                    mensaje = e.getMessage();
                }
            } else {
                mensaje = "PRODUCTO NO CONFIGURADO PARA RETIROS";
            }
        } catch (Exception e) {
            System.out.println("Error al validar transferencia a CSN:" + e.getMessage());
            mensaje = e.getMessage();

            return mensaje.toUpperCase();
        }
        return mensaje.toUpperCase();

    }

    public void cerrar() {
//        emf.close();
    }
}
