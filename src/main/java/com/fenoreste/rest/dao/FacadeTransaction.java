/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.dao;

import com.fenoreste.rest.ResponseDTO.BackendOperationResultDTO;
import com.fenoreste.rest.ResponseDTO.TransactionToOwnAccountsDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.CuentasBankingly;
import com.fenoreste.rest.entidades.Origenes;
import com.fenoreste.rest.entidades.Productos;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.Transferencias;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import static javax.ws.rs.client.Entity.json;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 *
 * @author Elliot
 */
public abstract class FacadeTransaction<T> {

    EntityManagerFactory emf;

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
        //Si es una trasnferencia entre mis cuentas
        if (identificadorTransferencia == 1) {
            //Valido la trasnferencia y devuelvo el mensaje que se produce
            String messageBackend = TransferenciaEntreMisCuentas(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es una transferencia terceros dentro de la entidad
        if (identificadorTransferencia == 2) {
            String messageBackend = TransferenciaATerceros(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier());
            backendResponse.setBackendMessage(messageBackend);
        }
        //Si es pago a un prestamo
        if (identificadorTransferencia == 3) {
            String backendMessage = PagoAPrestamos(transactionOWN.getDebitProductBankIdentifier(), transactionOWN.getAmount(), transactionOWN.getCreditProductBankIdentifier());
            backendResponse.setBackendMessage(backendMessage);
        }
        System.out.println("BackendMessage:" + backendResponse.getBackendMessage());
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
                int rowsUpdated = 0;
                int rowsUpdated2 = 0;
                if (identificadorTransferencia == 1) {
                    try {
                        em.getTransaction().begin();
                        rowsUpdated = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                + "(SELECT saldo FROM auxiliares WHERE "
                                + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "')-" + transaction.getAmount()
                                + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "'").executeUpdate();
                        if (rowsUpdated > 0) {
                            rowsUpdated2 = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                    + "(SELECT saldo FROM auxiliares WHERE "
                                    + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "')+" + transaction.getAmount()
                                    + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "'").executeUpdate();
                            if (rowsUpdated2 > 0) {
                                em.persist(transaction);
                                backendResponse.setBackendCode("1");
                                backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                                backendResponse.setIsError(false);
                                backendResponse.setTransactionIdenty(transaction.getTransactionid().toString());
                                backendResponse.setBackendMessage("TRANSACCION EXITOSA");
                                backendResponse.setBackendReference(transaction.getTransactionid().toString());
                                em.getTransaction().commit();
                            }
                        }
                    } catch (Exception e) {
                        em.getTransaction().rollback();
                        System.out.println("Error al procesarTransaccion:" + e.getMessage());
                    }
                }
                if (identificadorTransferencia == 2) {
                    try {
                        em.getTransaction().begin();
                        rowsUpdated = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                + "(SELECT saldo FROM auxiliares WHERE "
                                + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "')-" + transaction.getAmount()
                                + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "'").executeUpdate();
                        if (rowsUpdated > 0) {
                            rowsUpdated2 = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                    + "(SELECT saldo FROM auxiliares WHERE "
                                    + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "')+" + transaction.getAmount()
                                    + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "'").executeUpdate();
                            if (rowsUpdated2 > 0) {
                                em.persist(transaction);
                                backendResponse.setBackendCode("1");
                                backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                                backendResponse.setIsError(false);
                                backendResponse.setTransactionIdenty(transaction.getTransactionid().toString());
                                backendResponse.setBackendMessage("TRANSACCION EXITOSA");
                                backendResponse.setBackendReference(transaction.getTransactionid().toString());
                                em.getTransaction().commit();
                            }
                        }
                    } catch (Exception e) {
                        em.getTransaction().rollback();
                        System.out.println("Error al procesarTransaccion:" + e.getMessage());
                    }
                }
                if (identificadorTransferencia == 3) {
                    try {
                        em.getTransaction().begin();
                        rowsUpdated = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                + "(SELECT saldo FROM auxiliares WHERE "
                                + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "')-" + transaction.getAmount()
                                + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getDebitproductbankidentifier() + "'").executeUpdate();
                        if (rowsUpdated > 0) {
                            rowsUpdated2 = em.createNativeQuery("UPDATE auxiliares a SET saldo="
                                    + "(SELECT saldo FROM auxiliares WHERE "
                                    + "replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "')-" + transaction.getAmount()
                                    + " WHERE replace(to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999'),' ','')='" + transaction.getCreditproductbankidentifier() + "'").executeUpdate();
                            if (rowsUpdated2 > 0) {
                                em.persist(transaction);
                                backendResponse.setBackendCode("1");
                                backendResponse.setBackendReference(transaction.getTransactiontypeid().toString());
                                backendResponse.setIsError(false);
                                backendResponse.setTransactionIdenty(transaction.getTransactionid().toString());
                                backendResponse.setBackendMessage("TRANSACCION EXITOSA");
                                backendResponse.setBackendReference(transaction.getTransactionid().toString());
                                em.getTransaction().commit();
                            }
                        }
                    } catch (Exception e) {
                        em.getTransaction().rollback();
                        System.out.println("Error al procesarTransaccion:" + e.getMessage());
                    }
                }
                
            }
        } catch (Exception e) {
            if (backendResponse.getBackendMessage().contains("EXITO")) {
                backendResponse.setBackendMessage("Error:" + e.getMessage());
            }
            System.out.println("Al ejecutar transferencia:" + e.getMessage());
            return backendResponse;
        }
        em.close();
        return backendResponse;
    }

    //Metodo para validar transferencia entre cuentas propias
    public String TransferenciaEntreMisCuentas(String opaOrigen, Double monto, String opaDestino) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        try {
            Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
            //Obtengo el producto origen
            Auxiliares ctaOrigen = (Auxiliares) query.getSingleResult();
            //obtengo el saldo del producto origen
            Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
            //Busco descripcion del idproducto origen
            Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
            //Valido que el producto origen se trabaje en banca movil
            CuentasBankingly cuentasBankingly = em.find(CuentasBankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
            System.out.println("CeuntaB:" + cuentasBankingly);
            if (cuentasBankingly != null) {
                //si el producto no es un prestamo            
                if (prOrigen.getTipoproducto() == 0) {
                    //Verifico el estatus de la cuenta origen
                    if (ctaOrigen.getEstatus() == 2) {
                        //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                        if (saldo >= monto) {
                            //Busco la cuenta destino
                            Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                            Auxiliares ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                            if (ctaDestino != null) {
                                //Busco el producto destino
                                Productos productoDestino = em.find(Productos.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                //Valido que la cuenta destino este activa
                                if (ctaDestino.getEstatus() == 2) {
                                    //Valido que producto destino opera para banca movil
                                    CuentasBankingly cuentaBankinglyDestino = em.find(CuentasBankingly.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                                    if (cuentaBankinglyDestino != null) {
                                        //Valido que el producto destino no sea un prestamo
                                        if (productoDestino.getTipoproducto() == 0) {
                                            //Valido que realmente el el producto destino pertenezca al mismo socio 
                                            if (ctaOrigen.getIdorigen() == ctaDestino.getIdorigen() && ctaOrigen.getIdgrupo() == ctaDestino.getIdgrupo() && ctaOrigen.getIdsocio() == ctaDestino.getIdsocio()) {
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

        } catch (Exception e) {
            em.close();
            System.out.println("meee:" + message);
            message = "ERROR AL PROCESAR CONSULTA";
            System.out.println("Error en transferencia entre mis cuentas:" + e.getMessage());
            return message;
        }
        em.close();
        return message.toUpperCase();
    }

    //Metodo para validar transferencia a otras cuentas
    public String TransferenciaATerceros(String opaOrigen, Double monto, String opaDestino) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        try {
            Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
            //Obtengo el producto origen
            Auxiliares ctaOrigen = (Auxiliares) query.getSingleResult();
            //obtengo el saldo del producto origen
            Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
            //Busco descripcion del idproducto origen
            Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
            //Valido que el producto origen se trabaje en banca movil
            CuentasBankingly cuentasBankingly = em.find(CuentasBankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
            if (cuentasBankingly != null) {
                //si el producto no es un prestamo            
                if (prOrigen.getTipoproducto() == 0) {
                    //Verifico el estatus de la cuenta origen
                    if (ctaOrigen.getEstatus() == 2) {
                        //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                        if (saldo >= monto) {
                            //Busco la cuenta destino
                            Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                            Auxiliares ctaDestino = (Auxiliares) queryDestino.getSingleResult();
                            //Busco el producto destino
                            Productos productoDestino = em.find(Productos.class, ctaDestino.getAuxiliaresPK().getIdproducto());
                            //Valido que la cuenta destino este activa
                            if (ctaDestino.getEstatus() == 2) {
                                //Busco si existe el producto destino en el catalogo de banca movil
                                CuentasBankingly catalogoDestino = em.find(CuentasBankingly.class, productoDestino.getIdproducto());
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
                        message = "PRODUCTO ORIGEN INACTIVO";
                    }

                } else {
                    message = "PRODUCTO ORIGEN NO PERMITE SOBRECARGOS";
                }
            } else {
                message = "PRODUCTO ORIGEN NO OPERA PARA BANCA MOVIL";
            }
        } catch (Exception e) {
            em.close();
            message = e.getMessage();
            System.out.println("Errro al validar transferencia a terceros:" + e.getMessage());
            return message;
        }
        em.close();
        return message.toUpperCase();
    }

    //Metodo para validar pago a prestamos
    public String PagoAPrestamos(String opaOrigen, Double monto, String opaDestino) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'";
        String cuentaDestino = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaDestino + "'";
        String message = "";
        try {
            Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
            //Obtengo el producto origen
            Auxiliares ctaOrigen = (Auxiliares) query.getSingleResult();
            //obtengo el saldo del producto origen
            Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
            //Valido que el producto origen se trabaje en banca movil
            CuentasBankingly cuentasBankingly = em.find(CuentasBankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
            if (cuentasBankingly != null) {
                //Busco descripcion del idproducto origen
                Productos prOrigen = em.find(Productos.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
                //si el producto no es un prestamo            
                if (prOrigen.getTipoproducto() == 0) {
                    //Verifico el estatus de la cuenta origen
                    if (ctaOrigen.getEstatus() == 2) {
                        //verifico que el saldo del producto origen es mayor o igual a lo que se intenta transferir
                        if (saldo >= monto) {
                            //Busco la cuenta destino
                            Query queryDestino = em.createNativeQuery(cuentaDestino, Auxiliares.class);
                            Auxiliares ctaDestino = (Auxiliares) queryDestino.getSingleResult();
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
                                    message = "PRESTAMO DESTINO NO PERTENCE AL MISMO SOCIO";
                                }
                            } else {
                                message = "PRODUCTO DESTINO ESTA INACTIVO";
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
        } catch (Exception e) {
            em.close();
            message = e.getMessage();
            System.out.println("Error al realizar pago a prestamo:" + e.getMessage());
            return message;
        }
        em.close();
        return message.toUpperCase();
    }

    //Metodo para validar pago a prestamos
    public String EnviarOrdenSPEI(String opaOrigen, Double monto,@Context UriInfo ui) {
        EntityManager em = emf.createEntityManager();
        String cuentaOrigen = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + opaOrigen + "'";
        System.out.println("Consulta:"+cuentaOrigen);
        String message = "";
        try {
            Query query = em.createNativeQuery(cuentaOrigen, Auxiliares.class);
            //Obtengo el producto origen
            Auxiliares ctaOrigen = (Auxiliares) query.getSingleResult();
            //obtengo el saldo del producto origen
            Double saldo = Double.parseDouble(ctaOrigen.getSaldo().toString());
            //Valido que el producto origen se trabaje en banca movil
            CuentasBankingly cuentasBankingly = em.find(CuentasBankingly.class, ctaOrigen.getAuxiliaresPK().getIdproducto());
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
                                JsonObject request=new JsonObject();
                                //Parto la url completa para obtener basePath
                                String []partesUrl=ui.getBaseUri().toString().split("Ws");
                                String urlBase=partesUrl[0];
                                //al UrlPath le pongo el nombre del proyecto de SPEI-CSN
                                String urlComplete=urlBase+"SPEI-CSN/spei/v1.0/srvEnviaOrden";
                                request.put("cliente",opaOrigen);
                                request.put("monto",monto);
                                message=metodoEnviarSPEI(urlComplete,request.toString().replace("=",":")); 
                                
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
        } catch (Exception e) {
            em.close();
            System.out.println("Error al realizar pago a prestamo:" + e.getMessage());
            return message;
        }
        em.close();
        return message.toUpperCase();
    }
   
    //Se consume un servicio que yo desarrolle donde consumo API STP en caso de CSN si alguuien mas usara SPEI desarrollaria otro proyecto especficamente para la caja
    private String metodoEnviarSPEI(String url,String request) {
        URL urlB=null;
        String output="";
        String salida="";
        System.out.println("siiiiiiiiiiiiiii:"+request);
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
                    salida=output;              
                }
            }            
            conn.disconnect();
        } catch (Exception ex) {
            System.out.println("e:"+ex.getMessage());
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

            if (amount > Integer.parseInt(tb.getDato1())) {
                mensaje = "MAYOR";
            } else if (amount < Integer.parseInt(tb.getDato2())) {
                mensaje = "MENOR";
            } else {
                mensaje = "VALIDO";
            }

        } catch (Exception e) {
            em.close();
            System.out.println("Error al validar montos:" + e.getMessage());
        }
        em.close();
        return mensaje;
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
        }
        return false;
    }

    public void cerrar() {
        emf.close();
    }
}
