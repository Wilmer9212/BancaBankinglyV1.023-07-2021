package com.fenoreste.rest.dao;

import com.fenoreste.rest.ResponseDTO.ClientByDocumentDTO;
import com.fenoreste.rest.ResponseDTO.usuarios_banca_bankinglyDTO;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.PersonasPK;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.banca_movil_usuarios;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

public abstract class FacadeCustomer<T> {

    private static EntityManagerFactory emf;
    DAOGeneral metodosGeneral = new DAOGeneral();

    List<Object[]> lista = null;

    public FacadeCustomer(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
    }

    public ClientByDocumentDTO getClientByDocument(Persona p, String username) {
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
            client.setClientName(p.getNombre() + " " + p.getAppaterno() + " " + p.getApmaterno());
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

        Persona persona = new Persona();
        String consulta = "";
        System.out.println("LastNAME:" + LastName);
        persona.setNombre("SIN DATOS ");
        if (metodosGeneral.obtenerOrigen().contains("MITRAS") && LastName.contains("%")) {
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
                    + " AND UPPER(p.nombre)='" + Name.toUpperCase().replace(" ", "").trim() + "'"
                    + " AND UPPER(p.appaterno)||''||UPER(p.apmaterno) LIKE ('%" + LastName.toUpperCase().replace(" ", "") + "%')"
                    + " AND (CASE WHEN email IS NULL THEN '' ELSE trim(UPPER(email)) END)='" + Mail.toUpperCase() + "'"
                    + " AND (CASE WHEN telefono IS NULL THEN '' ELSE trim(telefono) END)='" + Phone + "'"
                    + " AND (CASE WHEN celular IS NULL THEN '' ELSE trim(celular) END)='" + CellPhone + "' LIMIT 1";

        } else {

            consulta = "SELECT * FROM personas p WHERE "
                    + "replace((p." + IdentClientType.toUpperCase() + "),' ','')='" + documentId.replace(" ", "").trim() + "'"
                    + " AND UPPER(p.nombre)='" + Name.toUpperCase().replace(" ", "").trim() + "'"
                    + " AND UPPER(appaterno)||''||UPPER(p.apmaterno) LIKE ('%" + LastName.toUpperCase().replace(" ", "") + "%')"
                    + " AND (CASE WHEN email IS NULL THEN '' ELSE trim(email) END)='" + Mail + "'"
                    + " AND (CASE WHEN telefono IS NULL THEN '' ELSE trim(telefono) END)='" + Phone + "'"
                    + " AND (CASE WHEN celular IS NULL THEN '' ELSE trim(celular) END)='" + CellPhone + "' LIMIT 1";
        }
        System.out.println("Consulta:" + consulta);
        try {   //Se deberia buscar por telefono,celular,email pero Mitras solicito que solo sea x curp y nombre esta en prueba            
            Query query = em.createNativeQuery(consulta, Persona.class);
            persona = (Persona) query.getSingleResult();
            //String encodedWithISO88591 = persona.getAppaterno();

            //String appaternoDecodedToUTF8 = new String(encodedWithISO88591.getBytes("ISO-8859-1"), "UTF-8");
            //Result, decodedToUTF8 --> "üzüm bağları"
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
    public String BuscarUsuario(String user) {
        String mensaje = "";
        EntityManager em = emf.createEntityManager();
        try {
            String consulta = "SELECT count(*) FROM banca_movil_usuarios WHERE alias_usuario='" + user + "'";
            Query query = em.createNativeQuery(consulta);
            int count = Integer.parseInt(query.getSingleResult().toString());
            if (count > 0) {
                mensaje = "usuario ya esta asignado";
            } else {
                mensaje = "usuario validado con exito";
            }
        } catch (Exception e) {
            em.close();
            System.out.println("Error en metodo para buscar usuario:" + e.getMessage());
            return e.getMessage().toUpperCase();
        } finally {
            em.clear();
            em.close();
        }

        return mensaje.toUpperCase();
    }

    //Buscamos si el socio no aparezca con otro usuario
    public String BuscarSocioRegistrado(int idorigen, int idgrupo, int idsocio,String username) {
        String mensaje = "";
        EntityManager em = emf.createEntityManager();
        try {
            /*int o=Integer.parseInt(customer.substring(0,6));
            int g=Integer.parseInt(customer.substring(6,8));
            int s=Integer.parseInt(customer.substring(8,14));*/

            //Busco la tabla donde guarda el producto para banca movil
            TablasPK tablasPK = new TablasPK("bankingly_banca_movil", "acceso_aplicacion");
            Tablas tablaProducto = em.find(Tablas.class, tablasPK);

            System.out.println("TablaProducto:" + tablaProducto);
            //Buscamos que el socio tenga el producto para banca movil aperturado en auxiliares            
            String busquedaFolio = "SELECT * FROM auxiliares WHERE idorigen=" + idorigen + " AND idgrupo=" + idgrupo + " AND idsocio=" + idsocio + " AND idproducto=" + tablaProducto.getDato1();
            Query busquedaFolioQuery = em.createNativeQuery(busquedaFolio, Auxiliares.class);
            Auxiliares a = (Auxiliares) busquedaFolioQuery.getSingleResult();
            if (a != null) {
                //mensaje = "validado con exito";
                //Guardamos el socio o si ya esta nada mas actualizamos el username
                PersonasPK pk=new PersonasPK(idorigen, idgrupo, idsocio);
                banca_movil_usuarios usersBanca = em.find(banca_movil_usuarios.class,pk);
                if (usersBanca != null) {
                    usersBanca.setAlias_usuario(username);
                    em.getTransaction().begin();
                    em.persist(usersBanca);
                    em.getTransaction().commit();
                } else {
                    
                    String update="INSERT INTO banca_movil_usuarios VALUES(?,?,?,?,?,?,?,?)";
                    em.getTransaction().begin();
                    int execute=em.createNativeQuery(update)
                                  .setParameter(1,pk.getIdorigen())
                                  .setParameter(2, pk.getIdgrupo())
                                  .setParameter(3, pk.getIdgrupo())
                                  .setParameter(4, username)
                                  .setParameter(5, true)
                                  .setParameter(6,a.getAuxiliaresPK().getIdorigenp())
                                  .setParameter(7,a.getAuxiliaresPK().getIdproducto())
                                  .setParameter(8,a.getAuxiliaresPK().getIdauxiliar()).executeUpdate();
                    
                    em.getTransaction().commit();
                                  
                    mensaje="usuario validado con exito";
                    
                    
                }
            }
        } catch (Exception e) {
            mensaje = "El usuario no tiene habilitado el producto para banca movil";
            System.out.println("Error en metodo para buscar persona registrada:" + e.getMessage());
            em.getTransaction().rollback();
            return mensaje.toUpperCase();
        } finally {
            em.clear();
            em.close();
        }
        return mensaje.toUpperCase();
    }

    //Guardamos el usuario en la base de datos
    public boolean saveUsername(String username, int idorigen, int idgrupo, int idsocio, int idorigenp, int idproducto, int idaxuliar) {
        EntityManager em = emf.createEntityManager();
        try {
            banca_movil_usuarios userDB = new banca_movil_usuarios();
            PersonasPK personaPK = new PersonasPK(idorigen, idgrupo, idsocio);
            userDB.setPersonasPK(personaPK);
            userDB.setAlias_usuario(username);
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

    public void pruebasPrezzta() {
        EntityManager em = emf.createEntityManager();
        try {
            Query qe = em.createNativeQuery("SELECT * FROM trabajo where idorigen=30301 AND idgrupo=10 AND idsocio=515");

            lista = qe.getResultList();
            System.out.println("Size:" + lista);
            long time = System.currentTimeMillis();
            Timestamp timestamp = new Timestamp(time);
            Instant instant = timestamp.toInstant();
            System.out.println("Current Time Stamp: " + timestamp);

            em.close();
        } catch (Exception e) {
            System.out.println("Error en pruebas para Prezzta:" + e.getMessage());
            em.close();
        }

    }

    public void cerrar() {
        emf.close();
    }

}
