package com.fenoreste.rest.RESTservices;

import com.fenoreste.rest.ResponseDTO.ProductsConsolidatePositionDTO;
import com.fenoreste.rest.ResponseDTO.ProductsDTO;
import com.fenoreste.rest.Util.Authorization;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import com.fenoreste.rest.dao.ProductsDAO;
import com.github.cliftonlabs.json_simple.JsonObject;
import java.util.ArrayList;
import javax.ws.rs.HeaderParam;
import org.json.JSONArray;
import org.json.JSONObject;
import com.fenoreste.rest.ResponseDTO.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import javax.json.Json;

/**
 *
 * @author Elliot
 */
@Path("/Products")
public class ProductsResources {

    Authorization auth = new Authorization();

    @POST
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    @Consumes({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    public Response GetPRoducts(String cadena, @HeaderParam("authorization") String authString) {
        String ClientBankIdentifiers = "";
        Integer ProductTypes = null;
        JsonObject jsonError = new JsonObject();

        //-------------------------Obtiene el request json---------------------*/
        try {
            JSONObject Object = new JSONObject(cadena);
            JSONArray jsonCB = Object.getJSONArray("clientBankIdentifiers");
            JSONArray jsonPB = Object.getJSONArray("productTypes");

            for (int i = 0; i < jsonCB.length(); i++) {
                JSONObject jCB = (JSONObject) jsonCB.get(i);
                ClientBankIdentifiers = jCB.getString("value");
            }
            for (int x = 0; x < jsonPB.length(); x++) {
                JSONObject jPB = (JSONObject) jsonPB.get(x);
                ProductTypes = jPB.getInt("value");
            }
        } catch (Exception e) {
            jsonError.put("Error", "Request Failed");

            return Response.status(Response.Status.BAD_REQUEST).entity(jsonError).build();

        }
        /*-------------------------------------------------------*/

 /*================================================================================================
        Valida las credenciales
       ==================================================================================================*/
 /*if(!auth.isUserAuthenticated(authString)){
            return Response.status(Response.Status.UNAUTHORIZED).entity("credenciales incorrectas").build();
        }*/
 /*================================================================================================
          Si las credenciales son correctas avanza        
         ================================================================================================*/
        ProductsDAO dao = new ProductsDAO();
        try {
            List<ProductsDTO> listaDTO = dao.getProductos(ClientBankIdentifiers, ProductTypes);
            if (listaDTO != null) {
                JsonObject jsonD = new JsonObject();
                jsonD.put("Products", listaDTO);
                return Response.status(Response.Status.OK).entity(jsonD).build();
            } else {
                jsonError.put("Error", "DATOS NO ENCONTRADOS");
                return Response.status(Response.Status.NO_CONTENT).entity(jsonError).build();
            }
        } catch (Exception e) {
            System.out.println("Error interno en el servidor");
            dao.cerrar();
        } finally {
            dao.cerrar();
        }

        return null;

    }

    @POST
    @Path("/ConsolidatedPosition")
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    @Consumes({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    public Response getProductsConsolidatedPosition(String cadena) {
        /*SOLO FALTA DEL CATALOGO CAN TRANSACT ID*/
        System.out.println("Cadena:" + cadena);
        String ClientBankIdentifiers = "", ProductBankIdentifiers = "";
        JsonObject jsonError = new JsonObject();
        List<String> productsBank = new ArrayList<String>();
        try {
            JSONObject Object = new JSONObject(cadena);
            JSONArray jsonCB = Object.getJSONArray("clientBankIdentifiers");
            JSONArray jsonPB = Object.getJSONArray("productBankIdentifiers");

            for (int i = 0; i < jsonCB.length(); i++) {
                System.out.println("si");
                JSONObject jCB = (JSONObject) jsonCB.get(i);
                ClientBankIdentifiers = jCB.getString("value");

                System.out.println("ClientBankIdentifiers:" + ClientBankIdentifiers);
            }
            for (int x = 0; x < jsonPB.length(); x++) {
                JSONObject jPB = jsonPB.getJSONObject(x);
                ProductBankIdentifiers = jPB.getString("value");

                System.out.println("ProductBankIdentifiers:" + ProductBankIdentifiers);
                productsBank.add(ProductBankIdentifiers);
            }
        } catch (Exception e) {
            System.out.println("Error al convertir Json:" + e.getMessage());
        }
        System.out.println("Lista de opas:" + productsBank);
        ProductsDAO dao = new ProductsDAO();
        try {
            List<ProductsConsolidatePositionDTO> ListPC = dao.ProductsConsolidatePosition(ClientBankIdentifiers, productsBank);
            if (ListPC != null) {
                System.out.println("ListaPC:" + ListPC);
                JsonObject k = new JsonObject();
                k.put("ConsolidatedPosition", ListPC);
                return Response.status(Response.Status.OK).entity(k).build();

            } else {
                jsonError.put("Error", "DATOS NO ENCONTRADOS");
                return Response.status(Response.Status.NO_CONTENT).entity(jsonError).build();
            }
        } catch (Exception e) {
            System.out.println("Error aqui:" + e.getMessage());
            dao.cerrar();
        } finally {
            dao.cerrar();
        }

        return null;

    }

    @POST
    @Path("/BankStatements")
    @Produces({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    @Consumes({MediaType.APPLICATION_JSON + ";charset=utf-8"})
    public Response bankStatements(String cadena) {
        JSONObject request_ = new JSONObject(cadena);
        String clientBankIdentifier_ = "";
        String productBankIdentifier_ = "";
        int productType_ = 0;
        ProductsDAO dao = new ProductsDAO();
        try {
            clientBankIdentifier_ = request_.getString("clientBankIdentifier");
            productBankIdentifier_ = request_.getString("productBankIdentifier");
            productType_ = request_.getInt("productType");
            List<ProductBankStatementDTO> listaECuentas = dao.statements(clientBankIdentifier_, productBankIdentifier_, productType_);
            JsonObject json = new JsonObject();
            json.put("bankStatements", listaECuentas);
            return Response.status(Response.Status.OK).entity(json).build();
        } catch (Exception e) {
            System.out.println("Error al leer json:" + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } finally {
            dao.cerrar();
        }
    }

    @POST
    @Path("/BankStatementsFile")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response fileDownload(String cadena) {
        System.out.println("cadena:" + cadena);
        JSONObject RequestData = new JSONObject(cadena);
        String fileId = "";
        try {
            fileId = RequestData.getString("productBankStatementId");
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_GATEWAY).entity(e.getMessage()).build();
        }
        JsonObject jsonMessage=new JsonObject();         
        try {
            String filePath = ruta() + fileId + ".pdf";
            File fileA = new File(filePath);
            if (fileA.exists()) {
                byte[] input_file = Files.readAllBytes(Paths.get(filePath));
                byte[] encodedBytesFile = Base64.getEncoder().encode(input_file);
                String bytesFileId = new String(encodedBytesFile);
                jsonMessage.put("productBankStatementFile",bytesFileId);
                jsonMessage.put("productBankStatementFileName",fileId+".pdf");
            }else{
                jsonMessage.put("Error","EL ARCHIVO QUE INTENTA DESCARGAR NO EXISTE");
            }
            
        } catch (Exception e) {
            jsonMessage.put("Error",e.getMessage());
            return Response.status(Response.Status.BAD_GATEWAY).entity(jsonMessage).build();
        }

        
        return Response.status(Response.Status.OK).entity(jsonMessage).build();
    }

    //Parao obtener la ruta del servidor
    public static String ruta() {
        String home = System.getProperty("user.home");
        String separador = System.getProperty("file.separator");
        String actualRuta = home + separador + "Banca" + separador;
        return actualRuta;
    }

}
