/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.RESTservices;

import com.fenoreste.rest.ResponseDTO.ClientByDocumentDTO;
import com.fenoreste.rest.dao.CustomerDAO;
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
        System.out.println("Cadena Json:" + cadenaJson);
        JSONObject JsonRequest_ = new JSONObject(cadenaJson);
        JsonObject JsonError_ = new JsonObject();
        JsonObject JsonReponse_ = new JsonObject();
        String DocumentId = JsonRequest_.getString("documentId");
        int ClientType = JsonRequest_.getInt("clientType");
        String Name = JsonRequest_.getString("name");
        String LastName = JsonRequest_.getString("lastName");
        String Mail = JsonRequest_.getString("mail");
        String Phone = JsonRequest_.getString("phone");
        String CellPhone = JsonRequest_.getString("cellPhone");
        String UserName = JsonRequest_.getString("userName");
        CustomerDAO metodos = new CustomerDAO();
        boolean bande = metodos.BuscarUsuario(UserName);
        Persona persona=null;
        metodos.detectarCodificacionBD();
        try{
           persona = metodos.BuscarPersona(ClientType, DocumentId, Name, LastName, Mail, Phone, CellPhone);
        }catch(Exception e){
            
        }
        
        try {
            //Si es usuario no existe en la base sigue el proceso
            if(bande==false){
            if (persona != null) {
                //Buscamos que la persona no se halla registrado antes
                if(metodos.BuscarSocioRegistrado(String.format("%06d",persona.getPersonasPK().getIdorigen())+""+
                                                 String.format("%02d",persona.getPersonasPK().getIdgrupo())+""+
                                                 String.format("%06d",persona.getPersonasPK().getIdsocio()))==false){
                  ClientByDocumentDTO cliente = null;               
                    //Buscamos al socio 
                    cliente = metodos.getClientByDocument(persona);
                    //Si todo salio bien retornamos el cliente
                    if (cliente != null) {
                        //Persisitimos el usuario a la base de datos(Guardar)
                        //if(metodos.saveUsername(UserName,persona.getPersonasPK().getIdorigen(),persona.getPersonasPK().getIdgrupo(),persona.getPersonasPK().getIdsocio())){
                        JsonReponse_.put("customers", cliente);
                        return Response.status(Response.Status.OK).entity(JsonReponse_).build();
                       /* }else{
                            JsonError_.put("Error","NO SE ALMACENO USUARIO");
                            return Response.status(Response.Status.BAD_GATEWAY).entity(JsonError_).build();
                        }*/
                     } else {
                        JsonError_.put("Error", "ERROR AL DEVOLVER DATOS DEL SOCIO");
                        return Response.status(Response.Status.BAD_REQUEST).entity(JsonError_).build();
                    }  
                }else{
                        JsonError_.put("Error", "SOCIO YA SE HA REGISTRADO");
                        return Response.status(Response.Status.BAD_REQUEST).entity(JsonError_).build();
                }
                
            } else {
                JsonError_.put("Error", "SOCIO NO EXISTE,VERIFIQUE DATOS");
                return Response.status(Response.Status.BAD_REQUEST).entity(JsonError_).build();
            }
            }else{
                JsonError_.put("Error", "ERROR USUARIO YA ESTA REGISTRADO");
                return Response.status(Response.Status.BAD_REQUEST).entity(JsonError_).build();
            }
        } catch (Exception e) {
            System.out.println("Error:" + e.getMessage());
            JsonError_.put("Error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(JsonError_).build();
        } finally {
            metodos.cerrar();
        }

    }
}
