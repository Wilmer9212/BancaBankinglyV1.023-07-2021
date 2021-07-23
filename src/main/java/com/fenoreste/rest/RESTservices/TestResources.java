/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.fenoreste.rest.RESTservices;

import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.Util.AbstractFacade_1;
import com.fenoreste.rest.dao.DAOTDD;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.Persona;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.syc.ws.endpoint.siscoop.BalanceQueryResponseDto;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.POST;
import org.json.JSONObject;

/**
 *
 * @author Elliot
 */
@Path("Test")
public class TestResources {

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public Response pruebasWS() {
        String mensaje = "";
        String c="SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='0302100011027918858' AND a.estatus=2";
        EntityManager em = AbstractFacade_1.conexion();
        Persona p=new Persona();
        Query q=em.createNativeQuery(c,Auxiliares.class);
        Auxiliares a=(Auxiliares) q.getSingleResult();
        
        String consulta="SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='0302100011027918858' AND a.estatus=2";
            System.out.println("consulta:"+consulta);
            Query querya=em.createNativeQuery(consulta,Auxiliares.class);
        System.out.println("a:"+a);
        try {
            if (!em.getTransaction().isActive()) {
                em.clear();
                em.getTransaction().begin();
                Query query = em.createNativeQuery("SELECT * FROM personas limit 1", Persona.class);
                 p= (Persona) query.getSingleResult();

            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }

            em.getTransaction().begin();


            /*   System.out.println("pan:"+pan);
     DAOTDD tdd=new DAOTDD();
      BalanceQueryResponseDto dto=new BalanceQueryResponseDto();
      JSONObject json=new JSONObject(pan);
      String idcuenta=json.getString("idcuenta");
      
        try {
           dto=tdd.balanceQuery(idcuenta);
            System.out.println("dtoResource:"+dto);
           JsonObject jsonR=new JsonObject();
           jsonR.put("Descripcion",dto.getDescription());
           jsonR.put("Disponible",dto.getAvailableAmount());
           
           return Response.status(Response.Status.OK).entity(jsonR).build();
        } catch (Exception e) {
            System.out.println("Error al construir:"+e.getMessage());
        }finally{
        tdd.cerrar();
    }*/
           
           

        }
         return Response.status(Response.Status.OK).entity("Hola:" + p.getNombre()).build();
    }
}
