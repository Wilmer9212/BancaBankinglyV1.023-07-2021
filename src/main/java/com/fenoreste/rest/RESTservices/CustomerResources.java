/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.RESTservices;

import com.fenoreste.rest.ResponseDTO.ClientByDocumentDTO;
import com.fenoreste.rest.dao.CustomerDAO;
import com.fenoreste.rest.dao.DAOGeneral;
import com.fenoreste.rest.entidades.Persona;
import com.github.cliftonlabs.json_simple.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;

/**
 *
 * @author Elliot
 */
@Path("/Clients")
public class CustomerResources {

    @POST
    @Path("/auth")
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    @Consumes({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    public Response credencialesCliente(String cadena) {
        /*   boolean bandera = false;
        JSONObject request = new JSONObject(cadena);
        CustomerDAO dao = new CustomerDAO();
        try {String user = request.getString("username");
            String ogs = dao.findOGS(user);
            JsonObject json = new JsonObject();
            json.put("ogs", ogs);
            return Response.status(Response.Status.OK).entity(ogs).build();
        } catch (Exception e) {
            System.out.println("error:" + e.getMessage());
        } finally {
            dao.cerrar();
        }*/
        return null;
    }

    /*===============================================================================
          METODO PARA BUSCAR SOCIO CON BASE A DOCUMENTOS QUE SE TRAE EN EL REQUEST
    =================================================================================*/
    @POST
    @Path("ByDocuments")
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    @Consumes({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    public Response getClientsByDocument(String cadenaJson) throws Throwable {
        System.out.println("Request Json clientByDocuments:" + cadenaJson);
        JSONObject JsonRequest_ = new JSONObject(cadenaJson);
        JsonObject JsonError_ = new JsonObject();
        JsonObject JsonResponse_ = new JsonObject();
        String DocumentId = JsonRequest_.getString("documentId");
        int ClientType = JsonRequest_.getInt("clientType");
        String Name = JsonRequest_.getString("name");
        String LastName = JsonRequest_.getString("lastName");
        String Mail = JsonRequest_.getString("mail");
        String Phone = JsonRequest_.getString("phone");
        String CellPhone = JsonRequest_.getString("cellPhone");
        String UserName = JsonRequest_.getString("userName");
        CustomerDAO metodos = new CustomerDAO();

        try {
            Persona persona = null;
            try {
                persona = metodos.BuscarPersona(ClientType, DocumentId, Name, LastName, Mail, Phone, CellPhone);
            } catch (Exception e) {
                
            }
            if (persona != null) {
                //Validamos que el socio ya halla asistido a sudcursal a aperturar el producto para banca movil
                String buscarSocioRegistrado = metodos.BuscarSocioRegistrado(persona.getPersonasPK().getIdorigen(), persona.getPersonasPK().getIdgrupo(), persona.getPersonasPK().getIdsocio(),UserName);
                if (buscarSocioRegistrado.contains("EXITO")) {
                    //Validamos que el usuario no se halla registrado en la base de datos
                    String validacionUsuaurio = metodos.BuscarUsuario(UserName);
                    if (validacionUsuaurio.contains("EXITO")) {
                        ClientByDocumentDTO cliente = null;
                        //Buscamos que la persona no se halla registrado antes
                        //-----String mensajeValidacionUsuario = metodos.BuscarSocioRegistrado(persona.getPersonasPK().getIdorigen(), persona.getPersonasPK().getIdgrupo(), persona.getPersonasPK().getIdsocio());

                        //Retornamos el dto para GetClientByDocument y en ese mismo metodo buscamos el socio en la tabla banca_movil_usuarios 
                        cliente = metodos.getClientByDocument(persona,UserName);
                        if (cliente != null) {
                            JsonResponse_.put("customers", cliente);
                            return Response.ok(JsonResponse_).build();
                        } else {
                            JsonResponse_.put("Error", "NO SE PUDIERON CARGAR DATOS,CONTACTE AL PROVEEDOR");
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(JsonResponse_).build();
                        }
                    } else {
                        JsonResponse_.put("Error", validacionUsuaurio);
                        return Response.status(Response.Status.BAD_REQUEST).entity(JsonResponse_).build();
                    }
                  }else {
                    JsonResponse_.put("Error",buscarSocioRegistrado);
                    return Response.status(Response.Status.OK).entity(JsonResponse_).build();
                }
            } else {
                JsonResponse_.put("Error", "SOCIO NO EXISTE,VERIFIQUE DATOS");
                return Response.status(Response.Status.OK).entity(JsonResponse_).build();
            }
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
            JsonError_.put("Error", e.getMessage());
            metodos.cerrar();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(JsonError_).build();
        } finally {
            metodos.cerrar();
        }

    }
}
