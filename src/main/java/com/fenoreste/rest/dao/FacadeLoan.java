package com.fenoreste.rest.dao;

import com.fenoreste.rest.Util.UtilidadesGenerales;
import com.fenoreste.rest.DTO.OpaDTO;
import com.fenoreste.rest.ResponseDTO.FeesDueData;
import com.fenoreste.rest.Util.AbstractFacade;
import com.fenoreste.rest.ResponseDTO.AccountDetailsDTO;
import com.fenoreste.rest.ResponseDTO.LoanDTO;
import com.fenoreste.rest.ResponseDTO.LoanFee;
import com.fenoreste.rest.ResponseDTO.LoanPayment;
import com.fenoreste.rest.ResponseDTO.LoanRate;
import com.fenoreste.rest.Util.Utilidades;
import com.fenoreste.rest.entidades.Amortizaciones;
import com.fenoreste.rest.entidades.AuxiliaresPK;
import com.fenoreste.rest.entidades.Auxiliares;
import com.fenoreste.rest.entidades.AuxiliaresD;
import com.fenoreste.rest.entidades.Catalog_Status_Bankingly;
import com.fenoreste.rest.entidades.LoanRates;
import com.fenoreste.rest.entidades.Loan_Fee_Status;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

public abstract class FacadeLoan<T> {

    Utilidades util = new Utilidades();
    Calendar calendar = Calendar.getInstance();
    Date hoy = calendar.getTime();
    UtilidadesGenerales util2 = new UtilidadesGenerales();

    public FacadeLoan(Class<T> entityClass) {
    }

    public LoanDTO Loan(String productBankIdentifier) {
        EntityManager em = AbstractFacade.conexion();
        OpaDTO opa = util.opa(productBankIdentifier);
        AccountDetailsDTO cuenta = null;
        System.out.println("O:" + opa.getIdorigenp() + ",P:" + opa.getIdproducto() + ",A:" + opa.getIdauxiliar());
        LoanDTO dto = null;
        try {
            AuxiliaresPK auxpk = new AuxiliaresPK(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar());
            Auxiliares aux = em.find(Auxiliares.class, auxpk);
            if (aux.getEstatus() == 2) {
                Catalog_Status_Bankingly sts = em.find(Catalog_Status_Bankingly.class, Integer.parseInt(aux.getEstatus().toString()));
                Double currentBalance = Double.parseDouble(aux.getSaldo().toString());
                Double currentRate = Double.parseDouble(aux.getTasaio().toString()) + Double.parseDouble(aux.getTasaim().toString()) + Double.parseDouble(aux.getTasaiod().toString());
                int loanStatusId = sts.getProductstatusid();

                Query query = em.createNativeQuery("SELECT count(*) FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp()
                        + " AND idproducto=" + opa.getIdproducto()
                        + " AND idauxiliar=" + opa.getIdauxiliar()
                        + " AND todopag=true");
                int amPag = Integer.parseInt(String.valueOf(query.getSingleResult()));

                Query query1 = em.createNativeQuery("SELECT count(*) FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp()
                        + " AND idproducto=" + opa.getIdproducto()
                        + " AND idauxiliar=" + opa.getIdauxiliar());
                int tam = Integer.parseInt(String.valueOf(query1.getSingleResult()));
                System.out.println("Amortizaciones Pagadas:" + amPag);
                dto = new LoanDTO(
                        productBankIdentifier,
                        currentBalance,//Saldo o balance actual del prestamo
                        currentRate,//Tasa de interes aplicada al prestamo
                        RSFeesDue(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar()),//Cantidad de cuotas vencidas
                        RSFeesDueData(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar()),//Campo FeesDueData   Informacion sobre el vencimiento de las cuotas
                        loanStatusId, //Estatus del prestamo
                        nextFee(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar()), //Informacion sobre la proxima cuota
                        Double.parseDouble(aux.getMontoprestado().toString()),
                        RSOverdueDays(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar()),
                        amPag,
                        0.0,
                        0.0,
                        productBankIdentifier,
                        tam,
                        true,
                        null,
                        null,
                        null);
                System.out.println("LoanPre:" + dto);
            }
        } catch (Exception e) {
            System.out.println("Error en GetAccountDetails:" + e.getMessage());
        } finally {
            em.close();
        }

        return dto;//cuenta;

    }

    public LoanFee LoanFee(String productBankIdentifier, int feeNumber) {
        EntityManager em = AbstractFacade.conexion();
        LoanFee loanFee = null;
        OpaDTO opa = util.opa(productBankIdentifier);
        try {
            AuxiliaresPK pk = new AuxiliaresPK(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar());
            Auxiliares aux = em.find(Auxiliares.class, pk);
            //Obtengo informacion con el sai_auxiliar hasta la fecha actual, si hay dudas checar el catalogo o atributos que devuelve la funcion
            String sai_auxiliar = "SELECT  sai_auxiliar(" + opa.getIdorigenp() + "," + opa.getIdproducto() + "," + opa.getIdauxiliar() + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);
            //Obtengo la amortizacion que se vence
            String consultaA = "SELECT * FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp()
                    + " AND idproducto=" + opa.getIdproducto()
                    + " AND idauxiliar=" + opa.getIdauxiliar()
                    + " AND idorigenp+idproducto+idauxiliar+idamortizacion=" + feeNumber;
            System.out.println("La consulta es para la amortizacion es:" + consultaA);
            Query queryA = em.createNativeQuery(consultaA, Amortizaciones.class);
            Amortizaciones amm = (Amortizaciones) queryA.getSingleResult();
            Double iovencido = Double.parseDouble(list.get(6).toString()) + Double.parseDouble(list.get(17).toString());
            Double imvencido = Double.parseDouble(list.get(15).toString()) + Double.parseDouble(list.get(18).toString());

            int loanfeests = 0;
            if (Double.parseDouble(amm.getAbono().toString()) == Double.parseDouble(amm.getAbonopag().toString())) {
                loanfeests = 3;
            } else if (Double.parseDouble(amm.getAbono().toString()) > Double.parseDouble(amm.getAbonopag().toString())
                    && amm.getTodopag() == false) {
                loanfeests = 1;
            } else if (!list.get(14).toString().equals("C")) {
                loanfeests = 2;
            }

            Loan_Fee_Status loanf = em.find(Loan_Fee_Status.class, loanfeests);
            LocalDateTime now = LocalDateTime.now();
            Double abonoT = Double.parseDouble(amm.getAbono().toString()) + iovencido + imvencido;
            System.out.println("ammmVenceeeeeeeeeee:" + convertToLocalDateViaInstant(amm.getVence()));
            System.out.println("ammmVenceeeeeeeeeee:" + convertToLocalDateTimeViaInstant(amm.getVence()) + ":00.000Z");
            String converted = String.valueOf(convertToLocalDateTimeViaInstant(amm.getVence()) + ":00.000Z");
            System.out.println("convrteddddddd:" + converted);
            Date d = amm.getVence();
            Date today = amm.getVence();
            LocalDateTime ldt = LocalDateTime.ofInstant(today.toInstant(), ZoneId.systemDefault());

            loanFee = new LoanFee(
                    Double.parseDouble(aux.getSaldo().toString()),//Saldo o balance del prestamo principal
                    amm.getAmortizacionesPK().getIdorigenp() + amm.getAmortizacionesPK().getIdproducto() + amm.getAmortizacionesPK().getIdauxiliar() + amm.getAmortizacionesPK().getIdamortizacion(),
                    Double.parseDouble(amm.getAbono().toString()),
                    converted,//String.valueOf(ldt),//amm.getVence().toString(),
                    iovencido,
                    imvencido,
                    loanf.getId(),
                    Double.parseDouble(amm.getAbono().toString()),
                    abonoT);
        } catch (Exception e) {
            System.out.println("Error en LoanFee:" + e.getMessage());
        } finally {
            em.close();
        }
        return loanFee;
    }

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public List<LoanFee> LoanFees(String productBankIdentifier, int feesStatus, int pageSize, int pageStartIndex, String order) {
        EntityManager em = AbstractFacade.conexion();
        LoanFee loanFee = null;
        OpaDTO opa = util.opa(productBankIdentifier);
        List<Amortizaciones> ListaAmortizaciones = new ArrayList<>();
        List<LoanFee> listaFees = new ArrayList<>();
        try {
            System.out.println("dento de try");

            AuxiliaresPK pk = new AuxiliaresPK(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar());
            Auxiliares aux = em.find(Auxiliares.class, pk);
            System.out.println("todavia:");
            //Obtengo informacion con el sai_auxiliar hasta la fecha actual, si hay dudas checar el catalogo o atributos que devuelve la funcion
            String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + opa.getIdorigenp() + "," + opa.getIdproducto() + "," + opa.getIdauxiliar() + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            System.out.println("sai:" + sai);
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);
            String complemento = "";

            if (feesStatus == 0 && order.equalsIgnoreCase("feenumber")) {
                complemento = "ORDER BY (idorigenp+idproducto+idauxiliar+idamortizacion) ASC";
            } else if (feesStatus == 1 && order.equalsIgnoreCase("feenumber")) {
                complemento = "AND todopag=true ORDER BY (idorigenp+idproducto+idauxiliar+idamortizacion) ASC";
            } else if (feesStatus == 1 && order.equals("")) {
                complemento = "AND todopag=true";
            } else if (feesStatus == 2 && order.equalsIgnoreCase("feenumber")) {
                complemento = "AND todopag=false ORDER BY (idorigenp+idproducto+idauxiliar+idamortizacion) ASC";

            } else if (feesStatus == 2 && order.equals("")) {
                complemento = "AND todopag=false";
            }
            System.out.println("complemento:" + complemento);
            //Obtengo la amortizacion que se vence
            String consultaA = "SELECT * FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp()
                    + " AND idproducto=" + opa.getIdproducto()
                    + " AND idauxiliar=" + opa.getIdauxiliar() + " " + complemento;
            System.out.println("La consulta es:" + consultaA);
            int inicioB = 0;
            /*
            if (pageStartIndex == 1 || pageStartIndex == 0) {
                inicioB = 0;
            } else if (pageStartIndex > 1) {
                inicioB = ((pageStartIndex -1) * pageSize);
                               
            }*/
            inicioB = ((pageStartIndex * pageSize) - pageSize);
            if (inicioB < 0) {
                inicioB = 0;
            }
            Query queryA = em.createNativeQuery(consultaA, Amortizaciones.class);
            queryA.setFirstResult(pageStartIndex);
            queryA.setMaxResults(pageSize);
            ListaAmortizaciones = queryA.getResultList();
            Double iovencido = Double.parseDouble(list.get(6).toString()) + Double.parseDouble(list.get(17).toString());
            Double imvencido = Double.parseDouble(list.get(15).toString()) + Double.parseDouble(list.get(18).toString());

            for (int x = 0; x < ListaAmortizaciones.size(); x++) {
                Amortizaciones amm = ListaAmortizaciones.get(x);
                int loanfeests = 0;
                if (Double.parseDouble(amm.getAbono().toString()) == Double.parseDouble(amm.getAbonopag().toString())) {
                    loanfeests = 3;
                } else if (Double.parseDouble(amm.getAbono().toString()) > Double.parseDouble(amm.getAbonopag().toString())
                        && amm.getTodopag() == false) {
                    loanfeests = 1;
                } else if (!list.get(14).toString().equals("C")) {
                    loanfeests = 2;
                }
                Loan_Fee_Status loanf = em.find(Loan_Fee_Status.class, loanfeests);
                LocalDateTime now = LocalDateTime.now();
                Double abonoT = Double.parseDouble(amm.getAbono().toString()) + iovencido + imvencido;
                Date d = amm.getVence();
                String converted = String.valueOf(convertToLocalDateTimeViaInstant(amm.getVence()) + ":00.000Z");
                loanFee = new LoanFee(
                        Double.parseDouble(aux.getSaldo().toString()),//Saldo o balance del prestamo principal
                        amm.getAmortizacionesPK().getIdorigenp() + amm.getAmortizacionesPK().getIdproducto() + amm.getAmortizacionesPK().getIdauxiliar() + amm.getAmortizacionesPK().getIdamortizacion(),
                        Double.parseDouble(amm.getAbono().toString()),
                        converted,//String.valueOf(now+"Z"),//amm.getVence().toString(),
                        iovencido,
                        imvencido,
                        loanf.getId(),
                        Double.parseDouble(amm.getAbono().toString()),
                        abonoT);
                listaFees.add(loanFee);
            }

        } catch (Exception e) {
            System.out.println("Error en LoanFee:" + e.getMessage());
        } finally {
            em.close();
        }
        System.out.println("ListaFees:" + listaFees);
        return listaFees;
    }

    public List<LoanRate> LoanRates(String productBankIdentifier, int pageSize, int pageStartIndex) {
        EntityManager em = AbstractFacade.conexion();
        LoanRate loanRate = null;
        OpaDTO opa = util.opa(productBankIdentifier);
        List<LoanRate> listaRates = new ArrayList<>();
        try {
            String consulta = "SELECT fechaactivacion,tasaio,tasaim,tasaiod FROM auxiliares WHERE"
                    + " idorigenp=" + opa.getIdorigenp() + " AND"
                    + " idproducto=" + opa.getIdproducto() + " AND"
                    + " idauxiliar=" + opa.getIdauxiliar();
            System.out.println("Consulta:" + consulta);

            Query queryA = em.createNativeQuery(consulta);
            List<Object[]> MiLista = queryA.getResultList();
            System.out.println("siiiiiiiiiiiiiiiiii pasoooooooooo");
            try {
                EntityTransaction tr1 = em.getTransaction();
                tr1.begin();
                em.createNativeQuery("DELETE FROM loanrates WHERE opa='" + opa.getIdorigenp() + "-" + opa.getIdproducto() + "-" + opa.getIdauxiliar() + "'").executeUpdate();
                if (tr1.isActive()) {
                    tr1.commit();
                }
            } catch (Exception e) {
                System.out.println("Error en eliminar:" + e.getMessage());
            }
            System.out.println("aunuuuu pasoooooooooooooooooo");
            LocalDateTime now = LocalDateTime.now();
            System.out.println("nowcccccccccccccccccccccccccccccccccccccc:" + String.valueOf(now) + "Z");
            for (Object[] lista : MiLista) {

                for (int x = 0; x < lista.length; x++) {
                    if (x > 0) {
                        try {
                            System.out.println("Listax:" + lista[x]);
                            double xc = Double.parseDouble(lista[x].toString());

                            EntityTransaction tr = em.getTransaction();
                            LoanRates lor = new LoanRates();
                            tr.begin();
                            lor.setOpa(opa.getIdorigenp() + "-" + opa.getIdproducto() + "-" + opa.getIdauxiliar());
                            lor.setInitialdate(lista[0].toString());
                            lor.setRate(xc);
                            em.persist(lor);
                            if (tr.isActive()) {
                                tr.commit();
                                em.clear();
                            }
                        } catch (Exception e) {
                            System.out.println("Error:" + e.getMessage());
                        }
                    }
                }
            }

            try {
                String c = "SELECT * FROM loanrates WHERE opa='" + opa.getIdorigenp() + "-" + opa.getIdproducto() + "-" + opa.getIdauxiliar() + "'";
                Query queryLoan = em.createNativeQuery(c, LoanRates.class);
                queryLoan.setFirstResult(pageStartIndex);
                queryLoan.setMaxResults(pageSize);
                List<LoanRates> listaRatess = queryLoan.getResultList();
                String converted = "";//String.valueOf(convertToLocalDateTimeViaInstant(amm.getVence())+":00.000Z");
                for (int j = 0; j < listaRatess.size(); j++) {

                    LoanRates loanrtt = listaRatess.get(j);
                    String str = "2016-03-04 11:30:40";
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime localDate = LocalDateTime.parse("2021-10-07" + " 00:00:00", dtf);
                    converted = loanrtt.getInitialdate() + "T00:00:00.000Z";////String.valueOf(convertToLocalDateTimeViaInstant(.getInitialdate()))+":00.000Z");
                    loanRate = new LoanRate(/*String.valueOf(String.valueOf(now)+"Z")*/converted, loanrtt.getRate());
                    System.out.println("LoanRattt:" + loanrtt);
                    listaRates.add(loanRate);
                }
                System.out.println("ListaRte:" + listaRates);
            } catch (Exception e) {
                System.out.println("Error en obtener loanRates:" + e.getMessage());
            }
            /*Map<Integer, String> map = IntStream
                    .range(pageStartIndex, listaRates.size())
                    .boxed()
                    .collect(groupingBy(
                            i -> i / pageSize,
                            mapping(i -> listaRates.get((int) i).toString(), joining(","))
                    ));
            
            ObjectMapper mapper = new ObjectMapper();
            ArrayList<String> list = new ArrayList<String>(map.values());
            
            String[]arreglo=map.get(pageStartIndex).split("},");
            System.out.println("listaReal:"+arreglo);
            for(int j=0;j<arreglo.length;j++){
                String[]jk=arreglo[j].split(",");
                String initialDates=jk[0];
                String rates=jk[1];
                String[]rate=rates.split("=");
                String[] initialDate=initialDates.split("=");
                String initialDateR=initialDate[1];
                Double rateR=Double.parseDouble(rate[1].replace("}",""));
                LoanRate loanrr=new LoanRate(initialDateR,rateR);
                loanr.add(loanrr);
                
            }
             */

        } catch (Exception e) {

            System.out.println("Error en LoanFee:" + e.getMessage());
        }
        em.close();

        return listaRates;
    }

    public List<LoanPayment> loanPayments(String productBankIdentifier, int pageSize, int startPageIndex) {
        EntityManager em = AbstractFacade.conexion();
        OpaDTO opa = util.opa(productBankIdentifier);

        List<LoanPayment> listPayment = new ArrayList<LoanPayment>();
        LoanPayment loanp = null;

        try {
            AuxiliaresPK auxpk = new AuxiliaresPK(opa.getIdorigenp(), opa.getIdproducto(), opa.getIdauxiliar());
            Auxiliares auxiliares = em.find(Auxiliares.class, auxpk);
            String con = "SELECT * FROM auxiliares_d WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + " AND idauxiliar=" + opa.getIdauxiliar() + " AND cargoabono=1";
            System.out.println("CON:" + con);
            Query query = em.createNativeQuery(con, AuxiliaresD.class);
            query.setFirstResult(startPageIndex);
            query.setMaxResults(pageSize);
            List<AuxiliaresD> MiLista = query.getResultList();
            for (int i = 0; i < MiLista.size(); i++) {
                AuxiliaresD io = MiLista.get(i);
                System.out.println("io:" + io);
            }

            String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + opa.getIdorigenp() + "," + opa.getIdproducto() + "," + opa.getIdauxiliar() + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
            Double montoVencido = 0.0;
            int feeNumber = 0;
            try {
                Query RsSai = em.createNativeQuery(sai_auxiliar);
                String sai = RsSai.getSingleResult().toString();
                String[] parts = sai.split("\\|");
                List list = Arrays.asList(parts);
                montoVencido = Double.parseDouble(list.get(4).toString());
                String fechaNumber = "";
                if (!list.get(8).toString().equals("")) {
                    fechaNumber = list.get(8).toString();
                    String c = ("SELECT idamortizacion FROM amortizaciones WHERE "
                            + "(idorigenp,idproducto,idauxiliar)=(" + opa.getIdorigenp() + "," + opa.getIdproducto() + "," + opa.getIdauxiliar() + ") AND vence='" + fechaNumber + "'");

                    System.out.println("FechaNumber:" + fechaNumber);
                    Query queryfe = em.createNativeQuery(c);
                    feeNumber = Integer.parseInt(String.valueOf(queryfe.getSingleResult()));
                    System.out.println("Consulta e:" + c);
                } else {
                    String c1 = ("SELECT idamortizacion FROM amortizaciones WHERE "
                            + "(idorigenp,idproducto,idauxiliar)=" + (opa.getIdorigenp() + "," + opa.getIdproducto() + "," + opa.getIdauxiliar()) + "ORDER BY vence ASC limit 1");
                    Query queryfecha = em.createNativeQuery(c1);
                    feeNumber = Integer.parseInt(String.valueOf(queryfecha.getSingleResult()));
                    System.out.println("Consulta e1:" + c1);
                }
            } catch (Exception e) {
                System.out.println("Error al correr SAI:" + e.getMessage());

            }
            int payEstatus = 0;
            String converted = "";
            for (int i = 0; i < MiLista.size(); i++) {
                System.out.println("aun");
                AuxiliaresD auxd = MiLista.get(i);
                if (Double.parseDouble(auxd.getSaldoec().toString()) == 0) {
                    payEstatus = 1;
                } else {
                    payEstatus = 2;
                }
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime localDate = LocalDateTime.parse("2021-10-07" + " 00:00:00", dtf);
                converted = String.valueOf(convertToLocalDateTimeViaInstant(auxd.getAuxiliaresDPK().getFecha()) + "Z");
                LocalDateTime now = LocalDateTime.now();
                loanp = new LoanPayment(Double.parseDouble(auxiliares.getSaldo().toString()),
                        0,
                        payEstatus,
                        Double.parseDouble(auxd.getMontoio().toString()),
                        Double.parseDouble(auxd.getMontoiva().toString()),
                        Double.parseDouble(auxd.getMontoim().toString()),
                        converted,//String.valueOf(now+"Z"),//auxd.getAuxiliaresDPK().getFecha()),
                        Double.parseDouble(auxd.getMonto().toString()),
                        Double.parseDouble(auxd.getMonto().toString()) + Double.parseDouble(auxd.getMontoio().toString()) + Double.parseDouble(auxd.getMontoim().toString()));

                System.out.println("LoanP:" + String.valueOf(localDate));
                listPayment.add(loanp);
            }
            System.out.println("listaPayments:" + listPayment);

        } catch (Exception e) {
            System.out.println("Error al buscar auxiliares d:" + e.getMessage());
        } finally {
            em.close();
        }
        return listPayment;
    }

    //Obetner cuota vencida
    public FeesDueData RSFeesDueData(int o, int p, int a) {
        EntityManager em = AbstractFacade.conexion();
        String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + o + "," + p + "," + a + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
        FeesDueData FeesDueDataRS = null;
        try {
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);

            Double iovencido = Double.parseDouble(list.get(6).toString()) + Double.parseDouble(list.get(17).toString());
            Double imvencido = Double.parseDouble(list.get(15).toString()) + Double.parseDouble(list.get(18).toString());
            Double montovencido = Double.parseDouble(list.get(4).toString());
            Double mnttotalcv = iovencido + imvencido + montovencido;
            System.out.println("Iovencido:" + iovencido + ", imvencido:" + imvencido + ",montovencido:" + montovencido + "Total vencido:" + mnttotalcv);

            FeesDueDataRS = new FeesDueData(
                    iovencido,
                    Double.parseDouble(list.get(6).toString()),
                    imvencido,
                    montovencido,
                    mnttotalcv);
            System.out.println("FeesDueData:" + FeesDueDataRS);
        } catch (Exception e) {
            System.out.println("Error en FeesDueData:" + e.getMessage());

        } finally {
            em.close();
        }
        System.out.println("FeesDueData:" + FeesDueDataRS);
        return FeesDueDataRS;
    }

    //Metodo para devolver abonos vencidos
    public int RSFeesDue(int o, int p, int a) {
        EntityManager em = AbstractFacade.conexion();
        String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + o + "," + p + "," + a + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
        int abonosVencidos = 0;
        try {
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);
            abonosVencidos = Integer.parseInt(list.get(3).toString());
            System.out.println("Abonos Vencidos:" + abonosVencidos);
        } catch (Exception e) {
            System.out.println("Error en FeesDueData:" + e.getMessage());
        } finally {
            em.close();
        }
        return abonosVencidos;
    }

    //Devuelve LoanFee para apoyo en GetLoanPrincipal para obtener la proxima amortizacion
    public LoanFee nextFee(int o, int p, int a) {
        EntityManager em = AbstractFacade.conexion();
        //Obejeto para cuota
        LoanFee loanFee = new LoanFee();
        try {
            AuxiliaresPK pk = new AuxiliaresPK(o, p, a);
            Auxiliares aux = em.find(Auxiliares.class, pk);
            //Obtengo informacion con el sai_auxiliar hasta la fecha actual, si hay dudas checar el catalogo o atributos que devuelve la funcion(
            //Porque con el sai auxiliar obtengo fecha de la proxima amortizacion y voy a buscarla a la tabla de amortizaciones
            String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + o + "," + p + "," + a + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);
            Query query_next_fee = null;
            Amortizaciones amm = null;
            Double imvencido = 0.0, iovencido = 0.0, montoCuota = 0.0;
            
            
            //::io_calculado+iva_io_total
            iovencido = Double.parseDouble(list.get(6).toString()) + Double.parseDouble(list.get(17).toString());
            imvencido = Double.parseDouble(list.get(15).toString()) + Double.parseDouble(list.get(18).toString());

            
            int idamortizacion = 0;
            int estatus_amortizacion = 0;//El estatus de la amortizacion
            if (list.get(13).toString().equals("C")) {
                //Obtengo la amortizacion que se vence
                String consultaA = "SELECT * FROM amortizaciones WHERE idorigenp=" + o
                        + " AND idproducto=" + p
                        + " AND idauxiliar=" + a
                        + " AND vence='" + list.get(10) + "'";
                System.out.println("consulta_amortizacion:" + consultaA);
                query_next_fee = em.createNativeQuery(consultaA, Amortizaciones.class);
                amm = (Amortizaciones) query_next_fee.getSingleResult();
                if (Double.parseDouble(amm.getAbono().toString()) == Double.parseDouble(amm.getAbonopag().toString())) {
                    estatus_amortizacion = 3;
                } else if (Double.parseDouble(amm.getAbono().toString()) > Double.parseDouble(amm.getAbonopag().toString()) && amm.getTodopag() == false) {
                    estatus_amortizacion = 1;
                } else if (!list.get(13).toString().equals("C")) {//Si esta vencido
                    estatus_amortizacion = 2;
                }
                montoCuota = Double.parseDouble(String.valueOf(list.get(11))) + Double.parseDouble(String.valueOf(list.get(4))) + iovencido + imvencido;
                idamortizacion = amm.getAmortizacionesPK().getIdamortizacion();
                //Double montovencido = Double.parseDouble(list.get(4).toString());
            } else {
                //montoCuota = Double.parseDouble(String.valueOf(list.get(5)));//Double.parseDouble(String.valueOf(total_a_cubrir_hoy.getSingleResult())) + iovencido + imvencido;
                montoCuota = Double.parseDouble(String.valueOf(list.get(11))) + iovencido + imvencido;
                //Obtengo el idamortizacion que no hay que cubrir
                String consulta_id_amortizaciones = "SELECT idamortizacion FROM amortizaciones WHERE idorigenp=" + o
                        + " AND idproducto=" + p
                        + " AND idauxiliar=" + a
                        + " AND todopag=false ORDER BY vence limit 1";
                System.out.println("consulta_amortizacion_moroso:" + consulta_id_amortizaciones);
                Query id_amortizacion = em.createNativeQuery(consulta_id_amortizaciones);
                idamortizacion = Integer.parseInt(String.valueOf(id_amortizacion.getSingleResult()));

            }
           
            //Double abonoT = Double.parseDouble(amm.getAbono().toString()) + iovencido + imvencido;
            loanFee.setCapitalBalance(Double.parseDouble(aux.getSaldo().toString()));//Saldo o balance actual del prestamo
            loanFee.setFeeNumber(idamortizacion);//(amm.getAmortizacionesPK().getIdorigenp() + amm.getAmortizacionesPK().getIdproducto() + amm.getAmortizacionesPK().getIdauxiliar() + amm.getAmortizacionesPK().getIdamortizacion());//Numero de cuota
            loanFee.setPrincipalAmount(Double.parseDouble(String.valueOf(list.get(11))));//monto de la cuota
            loanFee.setDueDate(String.valueOf(list.get(10)));//fecha de vencimiento
            loanFee.setInterestAmount(iovencido);//Monto de interes
            loanFee.setOverdueAmount(imvencido);//Monto de mora
            loanFee.setFeeStatusId(estatus_amortizacion);//Estado de la amortizacion
            loanFee.setOthersAmount(0.0);//Otros conceptos asociados
            loanFee.setTotalAmount(montoCuota);//Monto total de la cuota

            /*loanFee = new LoanFee(
                    Double.parseDouble(aux.getSaldo().toString()),//Saldo o balance del prestamo principal
                    amm.getAmortizacionesPK().getIdorigenp() + amm.getAmortizacionesPK().getIdproducto() + amm.getAmortizacionesPK().getIdauxiliar() + amm.getAmortizacionesPK().getIdamortizacion(),
                    Double.parseDouble(amm.getAbono().toString()),
                    amm.getVence().toString(),
                    iovencido,
                    imvencido,
                    loanf.getId(),
                    Double.parseDouble(amm.getAbono().toString()),
                    abonoT);*/
        } catch (Exception e) {
            System.out.println("Error en LoanFee:" + e.getMessage());
        } finally {
            em.close();
        }

        return loanFee;
    }

    //Metodo para devolver dias vencidos
    public int RSOverdueDays(int o, int p, int a) {
        EntityManager em = AbstractFacade.conexion();
        String sai_auxiliar = "SELECT * FROM sai_auxiliar(" + o + "," + p + "," + a + ",(SELECT date(fechatrabajo) FROM origenes limit 1))";
        int diasVencidos = 0;
        try {
            Query RsSai = em.createNativeQuery(sai_auxiliar);
            String sai = RsSai.getSingleResult().toString();
            String[] parts = sai.split("\\|");
            List list = Arrays.asList(parts);
            diasVencidos = Integer.parseInt(list.get(3).toString());
        } catch (Exception e) {
            System.out.println("Error en FeesDueData:" + e.getMessage());
        } finally {
            em.close();
        }

        return diasVencidos;
    }

    public int tipoproducto(int idproducto) {
        EntityManager em = AbstractFacade.conexion();
        int tipoproducto = 0;
        try {
            String consulta = "SELECT tipoproducto FROM productos WHERE idproducto=" + idproducto;
            Query query = em.createNativeQuery(consulta);
            tipoproducto = Integer.parseInt(String.valueOf(query.getSingleResult()));
        } catch (Exception e) {

            System.out.println("Error en buscar tipoproducto:" + e.getMessage());
        } finally {
            em.close();
        }
        return tipoproducto;
    }

    public static Date stringTodate(String fecha) {
        Date date = null;
        try {
            SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy");
            date = formato.parse(fecha);
        } catch (ParseException ex) {
            System.out.println("Error al convertir fecha:" + ex.getMessage());
        }
        System.out.println("date:" + date);
        return date;
    }

    public LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public int contadorGeneral(String productBankIdentifier, int identificador, int feesstatus) {
        EntityManager em = AbstractFacade.conexion();
        int cont = 0;
        OpaDTO opa = util.opa(productBankIdentifier);
        try {
            String consulta = "";
            if (feesstatus <= 2) {
                if (identificador == 1) {

                    if (feesstatus == 1) {
                        consulta = "SELECT count(*) FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + "AND idauxiliar=" + opa.getIdauxiliar() + " AND todopag=true";
                    } else if (feesstatus == 2) {
                        consulta = "SELECT count(*) FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + "AND idauxiliar=" + opa.getIdauxiliar() + " AND todopag=false";
                    } else if (feesstatus == 0) {
                        consulta = "SELECT count(*) FROM amortizaciones WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + "AND idauxiliar=" + opa.getIdauxiliar();
                    }

                } else if (identificador == 2) {
                    consulta = "SELECT count(*) FROM auxiliares_d WHERE idorigenp=" + opa.getIdorigenp() + " AND idproducto=" + opa.getIdproducto() + "AND idauxiliar=" + opa.getIdauxiliar() + "AND cargoabono=1";
                }
                Query query = em.createNativeQuery(consulta);
                cont = Integer.parseInt(String.valueOf(query.getSingleResult()));
            } else {
                cont = 0;
            }

            System.out.println("cont:" + cont);
        } catch (Exception e) {
            System.out.println("Error al obtener contador general:" + e.getMessage());
            return 0;
        } finally {
            em.close();
        }
        return cont;
    }

    public String dateToString(Date cadena) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String cadenaStr = sdf.format(cadena);
        return cadenaStr;
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
        } finally {
            em.close();
        }
        return bandera_;
    }

}
