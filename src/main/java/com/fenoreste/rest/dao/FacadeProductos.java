package com.fenoreste.rest.dao;

import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.ResponseDTO.ProductBankStatementDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.ResponseDTO.ProductsConsolidatePositionDTO;
import com.fenoreste.rest.ResponseDTO.ProductsDTO;
import com.fenoreste.rest.Util.TimerBeepClock;
import com.fenoreste.rest.WsTDD.SaldoTddPK;
import com.fenoreste.rest.WsTDD.TarjetaDeDebito;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.Catalog_Status_Bankingly;
import com.fenoreste.rest.entidades.Productos_bankingly;
import com.fenoreste.rest.entidades.Persona;
import com.fenoreste.rest.entidades.PersonasPK;
import com.fenoreste.rest.entidades.Productos;
import com.fenoreste.rest.entidades.Tablas;
import com.fenoreste.rest.entidades.TablasPK;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetas1;
import com.fenoreste.rest.entidades.WsSiscoopFoliosTarjetasPK1;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.syc.ws.endpoint.siscoop.BalanceQueryResponseDto;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.xml.namespace.QName;
import wssyctdd.SiscoopTDD;

public abstract class FacadeProductos<T> {

    private static EntityManagerFactory emf;
    private final Date hoy = new Date();

    public FacadeProductos(Class<T> entityClass) {
        emf = AbstractFacade.conexion();
        System.out.println("hoy:" + hoy);
        eliminarArchivosTemporaralesEstadosCuenta();
    }

    public List<ProductsDTO> getProductos(String clientBankIdentifiers, Integer productTypes) {
        List<ProductsDTO> ListagetP = new ArrayList<ProductsDTO>();
        EntityManager em = emf.createEntityManager();
        String productTypeId = "", descripcion = "";
        try {
            String consulta = "";
            Productos_bankingly ccb = null;
            if (!clientBankIdentifiers.equals("") && productTypes != null) {
                consulta = "SELECT * FROM auxiliares a INNER JOIN tipos_cuenta_bankingly pr USING(idproducto) WHERE replace((to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999')),' ','')='" + clientBankIdentifiers + "' AND (SELECT producttypeid FROM tipos_cuenta_bankingly cb WHERE a.idproducto=cb.idproducto)=" + productTypes + " AND a.estatus=2";
            } else if (!clientBankIdentifiers.equals("") && productTypes == null) {
                consulta = "SELECT * FROM auxiliares a INNER JOIN tipos_cuenta_bankingly USING(idproducto) WHERE replace((to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999')),' ','')='" + clientBankIdentifiers + "' AND a.estatus=2";
            }
            System.out.println("Consulta:" + consulta);
            Query query = em.createNativeQuery(consulta, Auxiliares.class);
            List<Auxiliares> ListaA = query.getResultList();

            for (int i = 0; i < ListaA.size(); i++) {
                ProductsDTO auxi = new ProductsDTO();
                Auxiliares a = ListaA.get(i);

                try {
                    ccb = em.find(Productos_bankingly.class, a.getAuxiliaresPK().getIdproducto());
                    productTypeId = String.valueOf(ccb.getProductTypeId());
                    descripcion = ccb.getDescripcion();
                } catch (Exception e) {
                    productTypeId = "";
                    descripcion = "";
                }

                String og = String.format("%06d", a.getIdorigen()) + String.format("%02d", a.getIdgrupo());
                String s = String.format("%06d", a.getIdsocio());

                String op = String.format("%06d", a.getAuxiliaresPK().getIdorigenp()) + String.format("%05d", a.getAuxiliaresPK().getIdproducto());
                String aa = String.format("%08d", a.getAuxiliaresPK().getIdauxiliar());

                Catalog_Status_Bankingly ctb = new Catalog_Status_Bankingly();
                int sttt = 0;
                try {
                    ctb = em.find(Catalog_Status_Bankingly.class, Integer.parseInt(a.getEstatus().toString()));
                    sttt = ctb.getProductstatusid();
                } catch (Exception ex) {
                    sttt = 0;
                }
                auxi = new ProductsDTO(
                        og + s,
                        op + aa,
                        String.valueOf(a.getAuxiliaresPK().getIdproducto()),
                        sttt,
                        productTypeId,
                        descripcion,
                        "1",
                        "1");
                ListagetP.add(auxi);
                productTypeId = "";
                descripcion = "";
            }
            System.out.println("Lista:" + ListagetP);

            return ListagetP;

        } catch (Exception e) {
            e.getStackTrace();
            System.out.println("Error Producido:" + e.getMessage());
        } finally {
            em.clear();
            em.close();
        }
        return null;
    }

    public List<ProductsConsolidatePositionDTO> ProductsConsolidatePosition(String clientBankIdentifier, List<String> productsBank) {
        EntityManager em = emf.createEntityManager();
        List<ProductsConsolidatePositionDTO> ListaReturn = new ArrayList<ProductsConsolidatePositionDTO>();
        String productTypeId = "";

        try {
            for (int ii = 0; ii < productsBank.size(); ii++) {
                String consulta = "SELECT * FROM auxiliares "
                        + " WHERE replace((to_char(idorigen,'099999')||to_char(idgrupo,'09')||to_char(idsocio,'099999')),' ','')='" + clientBankIdentifier
                        + "' AND replace((to_char(idorigenp,'099999')||to_char(idproducto,'09999')||to_char(idauxiliar,'09999999')),' ','')='" + productsBank.get(ii) + "' AND estatus=2";
                System.out.println("consulta:" + consulta);

                Query query = em.createNativeQuery(consulta, Auxiliares.class);
                List<Auxiliares> listaA = new ArrayList<>();
                listaA = query.getResultList();

                boolean prA = false;
                //Identifico la caja para la TDD
                Double saldo = 0.0;
                TarjetaDeDebito serviciosTdd = new TarjetaDeDebito();
                for (int i = 0; i < listaA.size(); i++) {
                    Auxiliares a = listaA.get(i);
                    saldo = Double.parseDouble(a.getSaldo().toString());
                    if (caja().contains("SANNICOLAS") && a.getAuxiliaresPK().getIdproducto() == 133) {
                        WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(a.getAuxiliaresPK().getIdorigenp(), a.getAuxiliaresPK().getIdproducto(), a.getAuxiliaresPK().getIdauxiliar());
                        WsSiscoopFoliosTarjetas1 tarjeta = em.find(WsSiscoopFoliosTarjetas1.class, foliosPK);
                        try {
                            TablasDTO tablaDTO = serviciosTdd.productoTddwebservice();
                            if (Integer.parseInt(tablaDTO.getDato2()) == a.getAuxiliaresPK().getIdproducto()) {
                                saldo = 0.0;
                                BalanceQueryResponseDto saldoWS = serviciosTdd.saldoTDD(foliosPK);
                                if (saldoWS.getCode() == 1) {
                                    saldo = saldoWS.getAvailableAmount();
                                    SaldoTddPK saldoTddPK = new SaldoTddPK(a.getAuxiliaresPK().getIdorigenp(), a.getAuxiliaresPK().getIdproducto(), a.getAuxiliaresPK().getIdauxiliar());
                                    serviciosTdd.actualizarSaldoTDD(saldoTddPK, saldo);
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("Error en consultar tdd:" + e.getMessage());
                        }

                    }

                    Productos pr = em.find(Productos.class, a.getAuxiliaresPK().getIdproducto());
                    String sai = "";
                    String vencimiento = "";
                    Double tasa = 0.0;
                    Productos_bankingly ctb = em.find(Productos_bankingly.class, a.getAuxiliaresPK().getIdproducto());
                    int cpagadas = 0;
                    Double totalam = 0.0;
                    String vencep = "";

                    //si es una inversion
                    if (ctb.getProductTypeId() == 4) {
                        try {
                            Query queryA = em.createNativeQuery("SELECT * FROM sai_auxiliar(" + a.getAuxiliaresPK().getIdorigenp() + ","
                                    + a.getAuxiliaresPK().getIdproducto() + "," + a.getAuxiliaresPK().getIdauxiliar() + ",(SELECT date(fechatrabajo) FROM origenes LIMIT 1))");
                            System.out.println("Consulta:" + queryA.getSingleResult());
                            sai = (String) queryA.getSingleResult();
                            String[] parts = sai.split("\\|");
                            List list = Arrays.asList(parts);
                            vencimiento = (String) list.get(2);
                            tasa = Double.parseDouble(a.getTasaio().toString());

                        } catch (Exception e) {
                            System.out.println("Error en amor:" + e.getMessage());
                            prA = false;
                        }
                    } else if (ctb.getProductTypeId() == 5) {//Si es un prestamo
                        String consulta1 = "SELECT count(*) FROM amortizaciones am WHERE idorigenp=" + a.getAuxiliaresPK().getIdorigenp()
                                + " AND idproducto=" + a.getAuxiliaresPK().getIdproducto()
                                + " AND idauxiliar=" + a.getAuxiliaresPK().getIdauxiliar()
                                + " AND todopag=true";

                        System.out.println("Consulta Prestamo:" + consulta1);
                        Query query1 = em.createNativeQuery(consulta1);
                        cpagadas = Integer.parseInt(query1.getSingleResult().toString());

                        String consulta2 = "SELECT count(*) FROM amortizaciones am WHERE idorigenp=" + a.getAuxiliaresPK().getIdorigenp()
                                + " AND idproducto=" + a.getAuxiliaresPK().getIdproducto()
                                + " AND idauxiliar=" + a.getAuxiliaresPK().getIdauxiliar();
                        System.out.println("Consulta Prestamo:" + consulta2);
                        Query query2 = em.createNativeQuery(consulta2);
                        totalam = Double.parseDouble(query2.getSingleResult().toString());
                        System.out.println("totalAm:" + totalam);

                        Query queryA = em.createNativeQuery("SELECT * FROM sai_auxiliar(" + a.getAuxiliaresPK().getIdorigenp() + ","
                                + a.getAuxiliaresPK().getIdproducto() + "," + a.getAuxiliaresPK().getIdauxiliar() + ",(SELECT date(fechatrabajo) FROM origenes LIMIT 1))");

                        sai = (String) queryA.getSingleResult();
                        String[] parts = sai.split("\\|");
                        List list = Arrays.asList(parts);

                        vencep = (String) list.get(10);
                        System.out.println("Vence:" + vencep);
                    }
                    PersonasPK pk = new PersonasPK(a.getIdorigen(), a.getIdgrupo(), a.getIdsocio());
                    Persona p = em.find(Persona.class, pk);
                    String nmsucursal = "";

                    try {
                        String consultaO = "SELECT nombre FROM origenes WHERE idorigen=" + a.getAuxiliaresPK().getIdorigenp();
                        Query queryO = em.createNativeQuery(consultaO);
                        nmsucursal = String.valueOf(queryO.getSingleResult());
                    } catch (Exception e) {
                        System.out.println("Error en buscar nombre de la sucursal:" + e.getMessage());
                    }
                    ProductsConsolidatePositionDTO dto = new ProductsConsolidatePositionDTO();
                    dto.setClientBankIdentifier(clientBankIdentifier);
                    dto.setProductBankIdentifier(productsBank.get(ii));
                    dto.setProductTypeId(String.valueOf(ctb.getProductTypeId()));
                    dto.setProductAlias(pr.getNombre());
                    dto.setProductNumber(String.valueOf(a.getAuxiliaresPK().getIdproducto()));
                    dto.setLocalCurrencyId(1);
                    dto.setLocalBalance(saldo);
                    dto.setInternationalCurrencyId(1);
                    dto.setInternationalBalance(0.0);
                    dto.setRate(tasa);
                    dto.setExpirationDate(vencimiento);
                    dto.setPaidFees(cpagadas);
                    dto.setTerm(totalam);
                    dto.setNextFeeDueDate(vencep);
                    dto.setProductOwnerName(p.getNombre() + " " + p.getAppaterno() + " " + p.getApmaterno());
                    dto.setProductBranchName(nmsucursal.toUpperCase());
                    dto.setCanTransact(1);
                    dto.setSubsidiaryId(1);
                    dto.setSubsidiaryName("");
                    dto.setBackendId(1);
                    ListaReturn.add(dto);
                }
            }
            System.out.println("Lista:" + ListaReturn);

        } catch (Exception e) {
            em.close();
            System.out.println("Error produucido:" + e.getMessage());
        } finally {
            em.close();
        }
        return ListaReturn;

    }

    public List<ProductBankStatementDTO> statements(String cliente, String productBankIdentifier, int productType) {
        EntityManager em = emf.createEntityManager();
        List<ProductBankStatementDTO> listaEstadosDeCuenta = new ArrayList<>();

        try {
            boolean ba = false;
            try {
                String BusquedaProducto = "SELECT * FROM auxiliares a WHERE replace(to_char(a.idorigenp,'099999')||to_char(a.idproducto,'09999')||to_char(a.idauxiliar,'09999999'),' ','')='" + productBankIdentifier + "'"
                        + " AND replace(to_char(a.idorigen,'099999')||to_char(a.idgrupo,'09')||to_char(a.idsocio,'099999'),' ','')='" + cliente + "' AND estatus=2";
                System.out.println("Consulta:" + BusquedaProducto);
                Query queryB = em.createNativeQuery(BusquedaProducto, Auxiliares.class);
                Auxiliares a = (Auxiliares) queryB.getSingleResult();
                if (a != null) {
                    ba = true;
                }
            } catch (Exception e) {
                System.out.println("El error al buscar auxiliar:" + e.getMessage());
            }
            //Si existe el producto auxiliar
            if (ba) {

                File file = null;

                String periodo_ = "SELECT to_char(date(fechatrabajo),'yyyy-MM') FROM origenes limit 1";
                Query queryF = em.createNativeQuery(periodo_);
                String fechaServidorDB = String.valueOf(queryF.getSingleResult()) + "-01";
                try {
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDate local = LocalDate.parse(fechaServidorDB);
                    //Date date = Date.from(local.atStartOfDay(defaultZoneId).toInstant());

                    String fi = "";
                    String ff = "";
                    LocalDateTime localDate = LocalDateTime.parse(fechaServidorDB + " 00:00:00", dtf);
                    TablasPK tbEstados_cuentaPK = new TablasPK("bankingly_banca_movil", "total_estados_cuenta");
                    Tablas tbEstados_Cuenta = em.find(Tablas.class, tbEstados_cuentaPK);

                    int total_estados = Integer.parseInt(String.valueOf(tbEstados_Cuenta.getDato1()));
                    for (int i = 0; i < total_estados; i++) {
                        ProductBankStatementDTO estadoCuenta = new ProductBankStatementDTO();
                        ff = String.valueOf(localDate.plusMonths(-i));
                        fi = String.valueOf(localDate.plusMonths(-i - 1));
                        //System.out.println("LocaDate:"+localDate);
                        System.out.println("yyyy/MM/dd HH:mm:ss-> " + dtf.format(LocalDateTime.now()));
                        file = crear_llenar_txt(productBankIdentifier, fi, ff, productType);

                        //file=new File(ruta()+"e_cuenta_ahorro_0101010011000010667_2.txt");          
                        File fileTxt = new File(ruta() + file.getName());
                        if (fileTxt.exists()) {
                            File fileHTML = crear_llenar_html(fileTxt, fileTxt.getName().replace(".txt", ".html"));
                            if (crearPDF(ruta(), fileHTML.getName())) {
                                estadoCuenta.setProductBankIdentifier(dtf.format(LocalDateTime.now()));
                                estadoCuenta.setProductBankIdentifier(productBankIdentifier);
                                estadoCuenta.setProductBankStatementId(file.getName().replace(".txt", "").replace("T", "").replace(":", "").replace("-", ""));
                                estadoCuenta.setProductType(productType);
                                estadoCuenta.setProductBankStatementDate(dtf.format(LocalDateTime.now()));
                                listaEstadosDeCuenta.add(estadoCuenta);
                                fileTxt.delete();
                                fileHTML.delete();
                            }
                        }
                    }
                    /*file=new File(ruta()+"test.txt");
                    crear_llenar_html(file,"test.txt");
                    crearPDF(ruta(),"test.html");
                     */
                } catch (Exception e) {
                    em.close();
                    System.out.println("Error en crear estado de cuenta:" + e.getMessage());
                }

            }
        } catch (Exception ex) {
            em.close();
            System.out.println("si");
        } finally {
            em.close();
        }
        System.out.println("ListaECuenta:" + listaEstadosDeCuenta);
        return listaEstadosDeCuenta;
    }

    /*
    private Timer temporizador;
    private TimerTask tarea;
    private Handler handler = new Handler();

    private void IniciarTemporizador() {
        temporizador = new Timer();
        tarea = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        //tu código aqui
                    }
                });
            }
        };
        temporizador.schedule(tarea, tiempo_delay, tiempo_bucle);
    }*/
    //Creamos y lleanamos un PDF
    public File crear_llenar_txt(String opa, String FInicio, String FFinal, int tipoproducto) {
        int numeroAleatorio = (int) (Math.random() * 9 + 1);
        String NProducto = "";
        if (tipoproducto == 1) {
            NProducto = "e_cuenta_ahorros";
        } else if (tipoproducto == 5) {
            NProducto = "e_cuenta_prestamos";
        } else if (tipoproducto == 4) {
            NProducto = "e_cuenta_dpfs_ind";
        }

        String nombre_txt = NProducto + "-" + FInicio.replace("-", "") + "" + FFinal.replace("/", "") + String.valueOf(numeroAleatorio) + ".txt";
        System.out.println("nombreTxt:" + nombre_txt);
        EntityManager em = emf.createEntityManager();
        File file = null;
        try {
            String o = opa.substring(0, 6);
            String p = opa.substring(6, 11);
            String a = opa.substring(11, 19);
            String fichero_txt = ruta() + nombre_txt;
            String contenido;

            String consulta = "SELECT sai_estado_" + NProducto.replace("e_", "") + "(" + o + "," + p + "," + a + ",'" + FInicio.replace("-", "/") + "','" + FFinal + "')";
            System.out.println("Consulta Statements:" + consulta);
            Query query = em.createNativeQuery(consulta);
            contenido = String.valueOf(query.getSingleResult());
            file = new File(fichero_txt);
            // Si el archivo no existe es creado
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file);
            BufferedWriter bw = new BufferedWriter(fw);

            bw.write(contenido);
            bw.close();
        } catch (Exception e) {
            em.close();
        } finally {
            em.close();
        }
        return file;
    }

    //Creamos y llenamos el HTML
    public File crear_llenar_html(File file, String nombre) throws FileNotFoundException {
        String nombre_html = nombre;//=nombre_txt.replace(".txt",".html");
        String html = ruta() + nombre_html.replace(".txt", ".html");
        File fi = new File(html);
        FileOutputStream fs = new FileOutputStream(fi);
        OutputStreamWriter out = new OutputStreamWriter(fs);
        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String linea;
            while ((linea = br.readLine()) != null) {
                //System.out.println("linea-------: " + linea);
                if (linea.contains("usr/local/saicoop")) {
                }
                if (linea.contains("/usr/local/saicoop/img_estado_cuenta_ahorros/")) {
                    String cade = ruta();
                    //System.out.println("Cade:" + cade.replace("\\", "/"));
                    linea = linea.replace("/usr/local/saicoop/img_estado_cuenta_ahorros/", cade.replace("\\", "/"));
                }
                if (linea.contains(" & ")) {
                    //System.out.println("si tele");
                    linea = linea.replace(" & ", " y ");
                }
                out.write(linea);

            }
            out.close();
        } catch (Exception e) {
            System.out.println("Excepcion leyendo txt" + ": " + e.getMessage());
        }
        return fi;
    }

    public boolean crearPDF(String ruta, String nombreDelHTMLAConvertir) {
        try {
            //ruta donde esta el html a convertir
            String ficheroHTML = ruta + nombreDelHTMLAConvertir;

            String url = new File(ficheroHTML).toURI().toURL().toString();
            //ruta donde se almacenara el pdf y que nombre se le data
            String ficheroPDF = ruta + nombreDelHTMLAConvertir.replace("T", "").replace("-", "").replace(":", "").replace(".html", ".pdf");
            /* OutputStream os = new FileOutputStream(ficheroPDF);
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocument(url);
            renderer.layout();
            renderer.createPDF(os);
            os.close();*/
            File htmlSource = new File(ficheroHTML);
            File pdfDest = new File(ficheroPDF);
            // pdfHTML specific code
            ConverterProperties converterProperties = new ConverterProperties();

            HtmlConverter.convertToPdf(new FileInputStream(htmlSource), new FileOutputStream(pdfDest), converterProperties);
            return true;
        } catch (Exception e) {
            System.out.println("Error al crear PDF:" + e.getMessage());
            return false;
        }

    }

    //Parao obtener la ruta del servidor
    public static String ruta() {
        String home = System.getProperty("user.home");
        String separador = System.getProperty("file.separator");
        String actualRuta = home + separador + "Banca" + separador;
        return actualRuta;
    }

    public static Date str(String fecha) {
        Date date = null;
        try {
            System.out.println("fecha:" + fecha);
            SimpleDateFormat formato = new SimpleDateFormat("yyyy/MM/dd");
            date = formato.parse(fecha);
        } catch (Exception ex) {
            System.out.println("Error al convertir fecha:" + ex.getMessage());
        }
        System.out.println("date:" + date);
        return date;
    }

    /*========================================================================================================================*/

 /*========================================================================
               Servicios S&C
    ========++++++=++++++++++++++++++++++++++++++++++++++++++++++++==+++++++=*/
    public BalanceQueryResponseDto balanceQuery(String pan) {
        EntityManager em = emf.createEntityManager();
        double saldo;
        try {
            TablasPK pk = new TablasPK("siscoop_banca_movil", "wsdl_parametros");
            Tablas tb = em.find(Tablas.class,
                     pk);
            if (authSyC(tb.getDato1(), tb.getDato2())) {
                SiscoopTDD tdd = new SiscoopTDD(tb.getDato1(), tb.getDato2());
                BalanceQueryResponseDto dto = tdd.getSiscoop().getBalanceQuery(pan);
                return dto;
            }
        } catch (Exception e) {
            em.clear();
            em.close();
            System.out.println("Error al consultar saldo de TDD:" + e.getMessage());
        }
        em.clear();
        em.close();
        return null;
    }

    public boolean authSyC(String user, String pass) {
        System.out.println("llego a auth");
        System.out.println("user:" + user + ",pass:" + pass);
        boolean bandera = true;
        try {
            System.out.println("entro a try");
            SiscoopTDD syc = new SiscoopTDD(user, pass);
            System.out.println("salio");
            bandera = true;
        } catch (Exception e) {
            System.out.println("Error al autenticar:" + e.getMessage());
        }
        System.out.println("fin");
        return bandera;
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
            System.out.println("Error al conectarse a SYC: " + ex.getMessage());
        }
        return false;
    }

    public boolean pingging() throws MalformedURLException {
        boolean bandera = false;

        EntityManager em = emf.createEntityManager();
        TablasPK tablasPK = new TablasPK("siscoop_banca_movil", "wsdl");
        Tablas tb = em.find(Tablas.class,
                 tablasPK);
        System.out.println("tablas encontrdas:" + tb);
        String wsdlLocation = "http://" + tb.getDato1() + ":" + tb.getDato3() + "/syc/webservice/" + tb.getDato2() + "?wsdl";
        System.out.println("wsdlLocation:" + wsdlLocation);
        //"http://200.15.1.143:8080/syc/webservice/siscoopAlternativeService/?wsld"
        QName QNAME = new QName("http://impl.siscoop.endpoint.ws.syc.com/", "SiscoopAlternativeEndpointImplService");
        URL url = new URL(wsdlLocation);
        System.out.println("ur:" + url);
        System.out.println("tbdato4:" + tb.getDato4());
        if (pingURL(url, tb.getDato4())) {
            System.out.println("si");
            bandera = true;
        } else {
            System.out.println("no");
            bandera = false;
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
    
    
    
    
  //Para eliminar PDF
  public void eliminarArchivosTemporaralesEstadosCuenta(){
      TimerBeepClock time=new TimerBeepClock();
      Toolkit.getDefaultToolkit().beep();
      System.out.println("entro");
       
  }
    public void cerrar() {
        emf.close();
    }

}
