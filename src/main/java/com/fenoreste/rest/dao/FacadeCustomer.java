package com.fenoreste.rest.dao;

import com.fenoreste.rest.DTO.PersonasDTO;
import com.fenoreste.rest.ResponseDTO.ClientByDocumentDTO;
import com.fenoreste.rest.ResponseDTO.usuarios_banca_bankinglyDTO;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.usuarios_banca_bankingly;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

public abstract class FacadeCustomer<T> {

    private static EntityManagerFactory emf;

    List<Object[]> lista = null;

    public FacadeCustomer(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }

    public ClientByDocumentDTO getClientByDocument(Persona p) {
        EntityManager em = emf.createEntityManager();
        ClientByDocumentDTO client = null;
        usuarios_banca_bankinglyDTO socio = null;
        /*String c="SELECT sai_convierte_caracteres_especiales_iso88591_utf8(appaterno) FROM personas WHERE idorigen="+p.getPersonasPK().getIdorigen()+
                                                                                                      " AND idgrupo="+p.getPersonasPK().getIdgrupo()+
                                                                                                      " AND idsocio="+p.getPersonasPK().getIdsocio();
         */
        try {
            System.out.println("esta es la persona:" + p.getNombre());
            int clientType = 0;
            System.out.println("Razon social:" + p.getRazonSocial());
            if (p.getRazonSocial() == null) {
                clientType = 0;
            } else {
                clientType = 1;
            }
            client = new ClientByDocumentDTO();
            client.setClientBankIdentifier(String.format("%06d", p.getPersonasPK().getIdorigen()) + "" + String.format("%02d", p.getPersonasPK().getIdgrupo()) + "" + String.format("%06d", p.getPersonasPK().getIdsocio()));
            client.setClientName(p.getNombre() + " " +p.getAppaterno() + " " + p.getApmaterno());
            client.setClientType(String.valueOf(clientType));
            client.setDocumentId(p.getCurp());
            System.out.println("Persona Fisica:" + client);
        } catch (Exception e) {
            em.close();
            System.out.println("Error leer socio:" + e.getMessage());
            return client;
        } finally {
            em.close();
        }
        return client;
    }

    public ClientByDocumentDTO getClientByDocumentTest(Persona p) {
        EntityManager em = emf.createEntityManager();
        ClientByDocumentDTO client = null;
        usuarios_banca_bankinglyDTO socio = null;
        try {
            int clientType = 0;
            System.out.println("Persona:" + p.getAppaterno());
            if (!p.getRazonSocial().equals("")) {
                clientType = 0;
            } else {
                clientType = 1;
            }
            client = new ClientByDocumentDTO();
            client.setClientBankIdentifier(String.format("%06d", p.getPersonasPK().getIdorigen()) + "" + String.format("%02d", p.getPersonasPK().getIdgrupo()) + "" + String.format("%06d", p.getPersonasPK().getIdsocio()));
            client.setClientName(p.getNombre() + " " + p.getAppaterno() + " " + p.getApmaterno());
            client.setClientType(String.valueOf(clientType));
            client.setDocumentId(p.getCurp());
            
        } catch (Exception e) {
            em.close();
            System.out.println("Error leer socio:" + e.getMessage());
            return client;
        } finally {
            em.close();
        }
        return client;
    }

    //Metodo para saber si la personas realmente existe en la base de datos
    public Persona BuscarPersona(int clientType, String documentId, String Name, String LastName, String Mail, String Phone, String CellPhone) throws UnsupportedEncodingException, IOException {
        EntityManager em = emf.createEntityManager();
        String IdentClientType = "";
        /*Identificamos el tipo de cliente si es 
        1.- persona fisica buscamos por Curp
        2.- persona moral RFC
         */
        if (LastName.replace(" ", "").toUpperCase().contains("Ñ")) {
            LastName = LastName.toUpperCase().replace("Ñ", "%");
        }
        if (clientType == 1) {
            IdentClientType = "curp";
        } else if (clientType == 2) {
            IdentClientType = "rfc";
        }
        
        Persona persona = null;
        String consulta="";
        System.out.println("LastNAME:"+LastName);
        if (caja().contains("MITRAS") && LastName.contains("%")) {
            System.out.println("NEtro");
           consulta = "SELECT "
                    + "idorigen,"
                    + "idgrupo,"
                    + "idsocio,"
                    + "calle,"
                    + "numeroext,"
                    + "numeroint,"
                    + "entrecalles,"
                    + "fechanacimiento,"
                    + "lugarnacimiento,"
                    + "efnacimiento,"
                    + "sexo,"
                    + "telefono,"
                    + "telefonorecados,"
                    + "listanegra,"
                    + "estadocivil,"
                    + "idcoop,"
                    + "idsector,"
                    + "estatus,"
                    + "aceptado,"
                    + "fechaingreso,"
                    + "fecharetiro,"
                    + "fechaciudad,"
                    + "regimen_Mat,"
                    + "nombre,"
                    + "medio_Inf,"
                    + "requisitos,"
                    + "sai_convierte_caracteres_especiales_iso88591_utf8(appaterno) as appaterno,"
                    + "sai_convierte_caracteres_especiales_iso88591_utf8(apmaterno) as apmaterno,"
                    + "nacionalidad,"
                    + "grado_Estudios,"
                    + "categoria,"
                    + "rfc,"
                    + "curp,"
                    + "email,"
                    + "razon_Social,"
                    + "causa_Baja,"
                    + "nivel_Riesgo,"
                    + "celular,"
                    + "rfc_Valido,"
                    + "curp_Valido,"
                    + "idcolonia"
                    + " FROM personas p WHERE "
                    + " replace((p." + IdentClientType.toUpperCase() + "),' ','')='" + documentId.replace(" ", "").trim() + "'"
                    + " AND replace(UPPER(p.nombre),' ','')='" + Name.toUpperCase().replace(" ", "").trim() + "'"
                    + " AND UPPER(replace(appaterno,' ','')||''||replace(UPPER(p.apmaterno),' ','')) LIKE ('%" + LastName.toUpperCase().replace(" ", "") + "%')"
                    + " AND (CASE WHEN email IS NULL THEN '' ELSE trim(UPPER(email)) END)='" + Mail.toUpperCase() + "'"
                    + " AND (CASE WHEN telefono IS NULL THEN '' ELSE trim(telefono) END)='" + Phone + "'"
                    + " AND (CASE WHEN celular IS NULL THEN '' ELSE trim(celular) END)='" + CellPhone + "' LIMIT 1";
            System.out.println("Consulta:"+consulta);
          
        }else{
            System.out.println("auiiii");
            consulta = "SELECT * FROM personas p WHERE "
                    + "replace((p." + IdentClientType.toUpperCase() + "),' ','')='" + documentId.replace(" ", "").trim() + "'"
                    + " AND replace(UPPER(p.nombre),' ','')='" + Name.toUpperCase().replace(" ", "").trim() + "'"
                    + " AND UPPER(replace(appaterno,' ','')||''||replace(p.apmaterno,' ','')) LIKE ('%" + LastName.toUpperCase().replace(" ", "") + "%')"
                    + " AND (CASE WHEN email IS NULL THEN '' ELSE trim(email) END)='" + Mail + "'"
                    + " AND (CASE WHEN telefono IS NULL THEN '' ELSE trim(telefono) END)='" + Phone + "'"
                    + " AND (CASE WHEN celular IS NULL THEN '' ELSE trim(celular) END)='" + CellPhone + "' LIMIT 1";
        }
        
        try {   //Se deberia buscar por telefono,celular,email pero Mitras solicito que solo sea x curp y nombre esta en prueba            
            Query query = em.createNativeQuery(consulta,Persona.class);
            persona=(Persona) query.getSingleResult();
            System.out.println("Persona::" + persona.getAppaterno());
            //p = (Persona) query.getSingleResult();
        } catch (Exception e) {
            em.close();
            System.out.println("Error al Buscar personas:" + e.getMessage());
            return persona;
        } finally {
            em.close();
        }
        return persona;
    }

    //Buscamos si el usuario no existe aun en la base de datos
    public boolean BuscarUsuario(String user) {
        boolean bandera = false;
        EntityManager em = emf.createEntityManager();
        try {
            String consulta = "SELECT count(*) FROM usuarios_bancam_bankingly WHERE username='" + user + "'";
            Query query = em.createNativeQuery(consulta);
            int count = Integer.parseInt(query.getSingleResult().toString());
            if (count > 0) {
                bandera = true;
            }
        } catch (Exception e) {
            em.clear();
            em.close();
            System.out.println("Error en metodo para buscar usuario:" + e.getMessage());
            return false;
        } finally {
            em.clear();
            em.close();
        }
        return bandera;
    }

    //Buscamos si el socio no aparezca con otro usuario
    public boolean BuscarSocioRegistrado(String customer) {
        boolean bandera = false;
        EntityManager em = emf.createEntityManager();
        try {

            String consulta = "SELECT count(*) FROM usuarios_bancam_bankingly WHERE socio='" + customer + "'";
            Query query = em.createNativeQuery(consulta);
            int count = Integer.parseInt(query.getSingleResult().toString());
            if (count > 0) {
                bandera = true;
            }
        } catch (Exception e) {
            em.clear();
            em.close();
            System.out.println("Error en metodo para buscar usuario:" + e.getMessage());
            return false;
        } finally {
            em.clear();
            em.close();
        }
        return bandera;
    }

    //Guardamos el usuario en la base de datos
    public boolean saveUsername(String username, int idorigen, int idgrupo, int idsocio) {
        EntityManager em = emf.createEntityManager();
        try {
            usuarios_banca_bankingly userDB = new usuarios_banca_bankingly();
            String socio = String.format("%06d", idorigen) + ""
                    + String.format("%02d", idgrupo) + ""
                    + String.format("%06d", idsocio);
            userDB.setSocio(socio);
            userDB.setUsername(username);
            userDB.setEstatus(true);
            em.getTransaction().begin();
            em.persist(userDB);
            em.getTransaction().commit();
            return true;
        } catch (Exception e) {
            em.close();
            System.out.println("Error al persistir usuario:" + username + ":" + e.getMessage());
            return false;
        } finally {
            em.clear();
            em.close();
        }
    }

    public String detectarCodificacionBD() {
        EntityManager em = emf.createEntityManager();
        String serverEncoding = "";
        try {
            Query query = em.createNativeQuery("SHOW server_encoding");
            serverEncoding = String.valueOf(query.getSingleResult());
            System.out.println("Codificacion:" + serverEncoding);
        } catch (Exception e) {
            System.out.println("Error al buscar codificacion:" + e.getMessage());
        }
        return serverEncoding.replace(" ", "").toUpperCase();
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
        System.out.println("NombreOrigen:"+nombreOrigen);
        return nombreOrigen.replace(" ", "").toUpperCase();
    }

    public void cerrar() {
        emf.close();
    }

}
