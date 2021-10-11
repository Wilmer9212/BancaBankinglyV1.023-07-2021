package com.fenoreste.rest.dao;

import com.fenoreste.rest.Util.UtilidadesGenerales;
import com.fenoreste.rest.DTO.OgsDTO;
import com.fenoreste.rest.DTO.OpaDTO;
import com.fenoreste.rest.DTO.TablasDTO;
import com.fenoreste.rest.ResponseDTO.ProductBankStatementDTO;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.ResponseDTO.ProductsConsolidatePositionDTO;
import com.fenoreste.rest.ResponseDTO.ProductsDTO;
import com.fenoreste.rest.Util.Utilidades;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.xml.namespace.QName;
import wssyctdd.SiscoopTDD;

public abstract class FacadeProductos<T> {

    private final Date hoy = new Date();

    UtilidadesGenerales util2 = new UtilidadesGenerales();

    public FacadeProductos(Class<T> entityClass) {
    }
    Utilidades util = new Utilidades();

    public List<ProductsDTO> getProductos(String clientBankIdentifiers, Integer productTypes) {
        List<ProductsDTO> ListagetP = new ArrayList<ProductsDTO>();
        EntityManager em = AbstractFacade.conexion();
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
        } 
        return null;
    }

    public List<ProductsConsolidatePositionDTO> ProductsConsolidatePosition(String clientBankIdentifier, List<String> productsBank) {
        EntityManager em = AbstractFacade.conexion();
        List<ProductsConsolidatePositionDTO> ListaReturn = new ArrayList<>();
        OgsDTO ogs = util.ogs(clientBankIdentifier);
        try {
            for (int ii = 0; ii < productsBank.size(); ii++) {
                OpaDTO opa = util.opa(productsBank.get(ii));
                String consulta = "SELECT * FROM auxiliares "
                        + " WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar()
                        + " AND  idorigen=" + ogs.getIdorigen() + "AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio() + " AND estatus=2";
                System.out.println("consulta:" + consulta);

                Query query = em.createNativeQuery(consulta, Auxiliares.class);
                List<Auxiliares> listaA = new ArrayList<>();
                listaA = query.getResultList();

                boolean prA = false;
                //Identifico la caja para la TDD
                Double saldo = 0.0;
                for (int i = 0; i < listaA.size(); i++) {
                    Auxiliares a = listaA.get(i);
                    saldo = Double.parseDouble(a.getSaldo().toString());
                    Tablas tablaTDD = util2.busquedaTabla(em, "bankingly_banca_movil", "producto_tdd");
                    if (util2.obtenerOrigen(em).contains("SANNICOLAS") && a.getAuxiliaresPK().getIdproducto() == Integer.parseInt(tablaTDD.getDato2())) {
                        WsSiscoopFoliosTarjetasPK1 foliosPK = new WsSiscoopFoliosTarjetasPK1(a.getAuxiliaresPK().getIdorigenp(), a.getAuxiliaresPK().getIdproducto(), a.getAuxiliaresPK().getIdauxiliar());
                        WsSiscoopFoliosTarjetas1 tarjeta = em.find(WsSiscoopFoliosTarjetas1.class, foliosPK);
                        try {
                            Tablas tablaDTO = new TarjetaDeDebito().productoTddwebservice(em);
                            if (Integer.parseInt(tablaDTO.getDato2()) == a.getAuxiliaresPK().getIdproducto()) {
                                saldo = 0.0;
                                BalanceQueryResponseDto saldoWS = new TarjetaDeDebito().saldoTDD(foliosPK, em);
                                if (saldoWS.getCode() == 1) {
                                    saldo = saldoWS.getAvailableAmount();
                                    SaldoTddPK saldoTddPK = new SaldoTddPK(a.getAuxiliaresPK().getIdorigenp(), a.getAuxiliaresPK().getIdproducto(), a.getAuxiliaresPK().getIdauxiliar());
                                    new TarjetaDeDebito().actualizarSaldoTDD(saldoTddPK, saldo, em);
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
            
            System.out.println("Error produucido:" + e.getMessage());
        } 
        return ListaReturn;

    }

    public List<ProductBankStatementDTO> statements(String cliente, String productBankIdentifier, int productType) {
        EntityManager em = AbstractFacade.conexion();
        List<ProductBankStatementDTO> listaEstadosDeCuenta = new ArrayList<>();
        OpaDTO opa = util.opa(productBankIdentifier);
        OgsDTO ogs = util.ogs(cliente);
        try {
            boolean ba = false;
            try {
                String BusquedaProducto = "SELECT * FROM auxiliares a WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar()
                        + " AND idorigen=" + ogs.getIdorigen() + " AND idgrupo=" + ogs.getIdgrupo() + " AND idsocio=" + ogs.getIdsocio() + " AND estatus=2";
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

                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
                            estadoCuenta.setProductBankStatementDate(ff.substring(0, 10));
                            listaEstadosDeCuenta.add(estadoCuenta);
                        }
                    }
                }
                /*file=new File(ruta()+"test.txt");
                    crear_llenar_html(file,"test.txt");
                    crearPDF(ruta(),"test.html");
                 */

            }
        } catch (Exception ex) {
           
            return listaEstadosDeCuenta;
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
        if (tipoproducto == 2) {
            NProducto = "e_cuenta_ahorros";
        } else if (tipoproducto == 5) {
            NProducto = "e_cuenta_prestamos";
        } else if (tipoproducto == 4) {
            NProducto = "e_cuenta_dpfs_ind";
        }
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("HH:mm:ss.SSS");
        String hora = dateFormatLocal.format(new Date());
        String nombre_txt = NProducto + "-" + FInicio.substring(0, 10).replace("-", "") + "" + FFinal.substring(0, 10).replace("-", "") + hora.replace(":", "") + String.valueOf(numeroAleatorio) + ".txt";
        System.out.println("nombreTxt:" + nombre_txt);
        EntityManager em = AbstractFacade.conexion();
        File file = null;
        try {
            String o = opa.substring(0, 6);
            String p = opa.substring(6, 11);
            String a = opa.substring(11, 19);
            String fichero_txt = ruta() + nombre_txt;
            String contenido;

            String consulta = "SELECT sai_estado_" + NProducto.replace("e_", "") + "(" + o + "," + p + "," + a + ",'" + FInicio + "','" + FFinal + "')";
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
            System.out.println("Error en crear estado de cuenta a TXT:" + e.getMessage());
          
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
        System.out.println("Ruta:" + actualRuta);
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

}
