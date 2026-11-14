package com.gestor.gestordecitasmedicas.servicio;

import com.gestor.gestordecitasmedicas.modelo.BaseDatos;
import com.gestor.gestordecitasmedicas.modelo.Consulta;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ControladorPDF {

    public static final String CLAVE_CARPETA = "directorio.pdf";

    public static String carpetaSalida() {
        return new BaseDatos().obtenerConfig(CLAVE_CARPETA, "pdf");
    }

    /** Guarda la carpeta donde se generan los PDF. */
    public static boolean guardarCarpetaSalida(String ruta) {
        return new BaseDatos().guardarConfig(CLAVE_CARPETA, ruta);
    }

    // Mismos colores que cita.css (tokens del sistema de diseño).
    private static final BaseColor TINTA = new BaseColor(15, 23, 42);
    private static final BaseColor PRIMARIO = new BaseColor(3, 105, 161);
    private static final BaseColor GRIS = new BaseColor(71, 85, 105);
    private static final BaseColor BORDE = new BaseColor(203, 213, 225);
    private static final BaseColor BORDE_SUAVE = new BaseColor(226, 232, 240);
    private static final BaseColor SUPERFICIE = new BaseColor(248, 250, 252);
    private static final BaseColor ALERTA = new BaseColor(180, 83, 9);
    private static final BaseColor ALERTA_FONDO = new BaseColor(255, 251, 235);
    private static final BaseColor EXITO = new BaseColor(4, 120, 87);

    private static final BaseFont BASE_REGULAR = cargarFuenteBase(
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Regular.ttf", "Manrope-Regular.ttf");
    private static final BaseFont BASE_BOLD = cargarFuenteBase(
            "/com/gestor/gestordecitasmedicas/fonts/Manrope/static/Manrope-Bold.ttf", "Manrope-Bold.ttf");

    private static final Font F_TITULO = fuente(16.5f, Font.BOLD, TINTA);
    private static final Font F_SUBTITULO = fuente(10.5f, Font.BOLD, PRIMARIO);
    private static final Font F_ETIQUETA = fuente(9, Font.BOLD, PRIMARIO);
    private static final Font F_NOTA = fuente(9, Font.NORMAL, GRIS);
    private static final Font F_NOTA_NEGRITA = fuente(9, Font.BOLD, GRIS);
    private static final Font F_GRIS = fuente(10, Font.NORMAL, GRIS);
    private static final Font F_DATO = fuente(10, Font.NORMAL, TINTA);
    private static final Font F_DATO_NEGRITA = fuente(10, Font.BOLD, TINTA);
    private static final Font F_FUERTE = fuente(11, Font.BOLD, TINTA);

    private static final float MARGEN = 36;
    private static final float DERECHA = PageSize.A4.getWidth() - MARGEN;

    private static BaseFont cargarFuenteBase(String recurso, String nombre) {
        try (java.io.InputStream in = ControladorPDF.class.getResourceAsStream(recurso)) {
            if (in == null) return null;
            return BaseFont.createFont(nombre, BaseFont.WINANSI, BaseFont.EMBEDDED, true, in.readAllBytes(), null);
        } catch (IOException | DocumentException e) {
            return null;
        }
    }

    private static Font fuente(float tamano, int estilo, BaseColor color) {
        BaseFont base = (estilo & Font.BOLD) != 0 ? BASE_BOLD : BASE_REGULAR;
        if (base != null) {
            return new Font(base, tamano, Font.NORMAL, color);
        }
        return new Font(Font.FontFamily.HELVETICA, tamano, estilo, color);
    }

    public boolean generarPDF(Receta receta) {
        if (!ValidacionClinica.receta(receta, true).isEmpty()) return false;
        try {
            generarReceta(archivoSalida(receta), receta);
            return true;
        } catch (IOException | DocumentException e) {
            System.err.println("Error al generar la receta: " + e.getMessage());
            return false;
        }
    }

    /** Abre el PDF en el visor predeterminado sin bloquear la interfaz; falla en silencio si no hay visor. */
    public static void abrir(File archivo) {
        Thread t = new Thread(() -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()
                        && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN))
                    java.awt.Desktop.getDesktop().open(archivo);
            } catch (IOException | RuntimeException e) {
                System.err.println("No se pudo abrir el PDF: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** Genera un formato para firma autógrafa, nunca una receta electrónica firmada. */
    public void generarReceta(File archivo, Receta receta) throws IOException, DocumentException {
        List<String> errores = ValidacionClinica.receta(receta, true);
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join("\n", errores));
        Paciente paciente = null;
        try { paciente = Paciente.buscar(receta.getCurp()); }
        catch (RuntimeException e) { System.err.println("Receta sin datos extra del paciente: " + e.getMessage()); }
        SignosVitales sv = receta.getSignosVitales();

        String medico = "Dr. " + texto(receta.getNombreMedico());
        String cedula = "Cédula profesional " + texto(receta.getCedulaMedico());
        String especialidad = vacio(receta.getEspecialidad()) ? texto(receta.getProfesion())
                : unir(" · ", receta.getEspecialidad(),
                        vacio(receta.getRegistroEspecialidad()) ? null : "Reg. esp. " + receta.getRegistroEspecialidad());

        Document documento = nuevoDocumento();
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new Membrete(medico, especialidad, unir(" · ", cedula, receta.getInstitucionTitulo()),
                    "RECETA MÉDICA", "Folio " + receta.getIdReceta(), formatearFecha(receta.getFecha()),
                    receta.getNombreEstablecimiento(), receta.getDomicilioEstablecimiento(),
                    texto(receta.getNombrePaciente()) + " · Folio " + receta.getIdReceta(), true));
            documento.open();
            try {
                PdfPTable ficha = tabla(1, 1, 1, 1);
                ficha.addCell(campo("Paciente", texto(receta.getNombrePaciente()), 2, F_FUERTE));
                ficha.addCell(campo("CURP", texto(receta.getCurp()), 2, F_DATO));
                ficha.addCell(campo("Fecha de nacimiento",
                        paciente == null ? "-" : TextoExpediente.fecha(paciente.getFechaNacimiento()), 1, F_DATO));
                ficha.addCell(campo("Peso", sv == null || sv.getPeso() <= 0 ? "-" : sv.getPeso() + " kg", 1, F_DATO));
                ficha.addCell(campo("Estatura", sv == null || sv.getTalla() <= 0 ? "-" : sv.getTalla() + " cm", 2, F_DATO));
                ficha.addCell(campo("Domicilio", paciente == null ? "-" : texto(paciente.getDomicilio()), 4, F_DATO));
                documento.add(caja(ficha, BORDE, SUPERFICIE));

                if (sv != null) {
                    PdfPTable signos = tabla(1, 1, 1, 1, 1);
                    signos.setSpacingBefore(12);
                    String[][] valores = {
                            {"Presión arterial", texto(sv.getPresionArterial()) + " mmHg"},
                            {"Frec. cardíaca", sv.getFrecuenciaCardiaca() + " lpm"},
                            {"Frec. respiratoria", sv.getFrecuenciaRespiratoria() + " rpm"},
                            {"Temperatura", sv.getTemperatura() + " °C"},
                            {"SpO2", sv.getSaturacionOxigeno() + " %"}};
                    for (String[] v : valores) {
                        Phrase p = new Phrase();
                        p.add(new Chunk(v[0] + "\n", F_NOTA));
                        p.add(new Chunk(v[1], F_DATO_NEGRITA));
                        PdfPCell c = celda(p, Rectangle.BOX, BORDE);
                        c.setPaddingLeft(8);
                        signos.addCell(c);
                    }
                    documento.add(signos);
                }

                documento.add(bloque("Diagnóstico", texto(receta.getDiagnostico())));

                Phrase rx = new Phrase();
                rx.add(new Chunk("Rx  ", fuente(19, Font.BOLD, PRIMARIO)));
                rx.add(new Chunk("MEDICAMENTOS", F_ETIQUETA));
                PdfPTable encabezadoRx = tabla(1);
                encabezadoRx.setSpacingBefore(14);
                encabezadoRx.addCell(sinSangria(celda(rx, Rectangle.BOTTOM, TINTA)));
                documento.add(encabezadoRx);

                PdfPTable medicamentos = tabla(18, 385, 120);
                int numero = 1;
                for (Medicamento m : receta.getMedicamentos()) {
                    medicamentos.addCell(sinSangria(celda(new Phrase(numero++ + ".", fuente(10, Font.BOLD, GRIS)),
                            Rectangle.BOTTOM, BORDE)));
                    Phrase p = new Phrase();
                    p.add(new Chunk(unir(" ", texto(m.getNombreMedicamento()), m.getConcentracion()), F_FUERTE));
                    if (!vacio(m.getPresentacion())) p.add(new Chunk(" · " + m.getPresentacion().trim(), F_DATO));
                    String posologia = unir(" · ", m.getDosis(), m.getViaAdministracion(), m.getFrecuencia(), m.getDuracion());
                    if (!posologia.isEmpty()) {
                        p.add(Chunk.NEWLINE);
                        p.add(new Chunk(posologia, F_DATO));
                    }
                    medicamentos.addCell(celda(p, Rectangle.BOTTOM, BORDE));
                    Phrase cantidad = new Phrase();
                    if (!vacio(m.getCantidad())) {
                        cantidad.add(new Chunk("Cantidad\n", F_NOTA));
                        cantidad.add(new Chunk(m.getCantidad().trim(), F_DATO_NEGRITA));
                    }
                    PdfPCell c = celda(cantidad, Rectangle.BOTTOM, BORDE);
                    c.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    c.setMinimumHeight(48);
                    medicamentos.addCell(c);
                }
                documento.add(medicamentos);

                String indicaciones = receta.getIndicaciones();
                if (!vacio(indicaciones)) documento.add(bloque("Indicaciones", indicaciones.trim()));

                // La firma va al pie de la última hoja; si no cabe, en una hoja nueva.
                PdfPTable firma = firma(medico, cedula, "Firma autógrafa del médico");
                firma.setTotalWidth(210);
                firma.setLockedWidth(true);
                float alto = firma.getTotalHeight();
                if (writer.getVerticalPosition(true) - documento.bottomMargin() < alto + 36) documento.newPage();
                writer.setPageEmpty(false);
                firma.writeSelectedRows(0, -1, (PageSize.A4.getWidth() - 210) / 2, documento.bottomMargin() + alto,
                        writer.getDirectContent());
            } finally { documento.close(); }
        }
    }

    public void generarExpediente(File archivo, Expediente expediente)
            throws IOException, DocumentException {
        if (expediente == null || expediente.getDatosPaciente() == null) {
            throw new IllegalArgumentException("El expediente debe incluir los datos del paciente.");
        }
        Paciente paciente = expediente.getDatosPaciente();
        Document documento = nuevoDocumento();
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new Membrete("Expediente clínico", TextoExpediente.nombre(paciente), null,
                    "EXPEDIENTE", expediente.getIdExpediente() > 0 ? "#" + expediente.getIdExpediente() : "-", null,
                    expediente.getNombreEstablecimiento(), expediente.getDomicilioEstablecimiento(),
                    "Confidencial · Pendiente de firma autógrafa", false));
            documento.open();
            try {
                Paragraph rotulo = new Paragraph("FICHA DE IDENTIDAD", F_ETIQUETA);
                rotulo.setSpacingAfter(6);
                documento.add(rotulo);
                PdfPTable ficha = tabla(1, 1, 1);
                ficha.addCell(campo("Nombre", TextoExpediente.nombre(paciente), 2, F_FUERTE));
                ficha.addCell(campo("Fecha de apertura", TextoExpediente.fecha(expediente.getFechaApertura()), 1, F_DATO));
                ficha.addCell(campo("CURP", TextoExpediente.valor(paciente.getCurp()), 1, F_DATO));
                ficha.addCell(campo("Fecha de nacimiento", TextoExpediente.fecha(paciente.getFechaNacimiento()), 1, F_DATO));
                ficha.addCell(campo("Sexo", TextoExpediente.valor(paciente.getSexo()), 1, F_DATO));
                ficha.addCell(campo("Edad a la apertura", TextoExpediente.edad(paciente, expediente.getFechaApertura()), 1, F_DATO));
                ficha.addCell(campo("Teléfono", TextoExpediente.valor(paciente.getTelefono()), 1, F_DATO));
                ficha.addCell(campo("Correo", TextoExpediente.valor(paciente.getCorreo()), 1, F_DATO));
                ficha.addCell(campo("Domicilio", TextoExpediente.valor(paciente.getDomicilio()), 3, F_DATO));
                documento.add(caja(ficha, BORDE, SUPERFICIE));

                PdfPTable alergias = tabla(1);
                Phrase aviso = new Phrase();
                aviso.add(new Chunk("ALERGIAS Y ADVERTENCIAS\n", fuente(9, Font.BOLD, ALERTA)));
                aviso.add(new Chunk(TextoExpediente.valor(expediente.getAlergias()), F_FUERTE));
                alergias.addCell(celda(aviso, Rectangle.NO_BORDER, ALERTA));
                PdfPTable cajaAlergias = caja(alergias, ALERTA, ALERTA_FONDO);
                cajaAlergias.setSpacingBefore(12);
                documento.add(cajaAlergias);

                agregarHistoria(documento, expediente, paciente);

                seccion(documento, "Notas de evolución", null);
                if (expediente.getConsultas().isEmpty()) {
                    documento.add(new Paragraph("No hay consultas registradas para este paciente.", F_DATO));
                }
                for (Consulta consulta : expediente.getConsultas()) agregarNota(documento, consulta);

                seccion(documento, "Recetas y farmacología", null);
                List<Receta> recetas = TextoExpediente.recetasOrdenadas(expediente);
                if (recetas.isEmpty()) {
                    documento.add(new Paragraph("No hay recetas registradas para este paciente.", F_DATO));
                }
                for (Receta receta : recetas) agregarReceta(documento, receta);

                seccion(documento, "Evolución de signos vitales", null);
                agregarSignos(documento, recetas);
            } finally {
                documento.close();
            }
        }
    }

    private void agregarHistoria(Document documento, Expediente e, Paciente paciente) throws DocumentException {
        int pendientes = ValidacionClinica.historia(e).size();
        seccion(documento, "Historia clínica ambulatoria", pendientes == 0
                ? new Phrase("Captura completa para revisión y firma", fuente(9, Font.NORMAL, EXITO))
                : new Phrase("BORRADOR: " + pendientes + " datos pendientes", fuente(9, Font.BOLD, ALERTA)));
        subseccion(documento, "1. Establecimiento", new String[][]{
                {"Tipo de establecimiento", e.getTipoEstablecimiento()},
                {"Nombre", e.getNombreEstablecimiento()},
                {"Domicilio completo", e.getDomicilioEstablecimiento()},
                {"Institución de pertenencia", e.getInstitucionEstablecimiento()},
                {"Razón o denominación social", e.getPropietario()}});
        subseccion(documento, "2. Interrogatorio y antecedentes", new String[][]{
                {"Edad a la elaboración", TextoExpediente.edadHistoria(e)},
                {"Sexo", paciente.getSexo()},
                {"Grupo étnico", e.getGrupoEtnico()},
                {"Heredo-familiares", e.getAntecedentesFamiliares()},
                {"Personales patológicos", e.getAntecedentesPersonales()},
                {"Personales no patológicos", e.getAntecedentesNoPatologicos()},
                {"Tabaco, alcohol y otras sustancias", e.getConsumoSustancias()},
                {"Tipo de sangre", e.getTipoSangre()}});
        subseccion(documento, "3. Padecimiento y exploración", new String[][]{
                {"Padecimiento actual y tratamientos previos", e.getPadecimientoActual()},
                {"Interrogatorio por aparatos y sistemas", e.getInterrogatorioSistemas()},
                {"Exploración física", e.getExploracionFisica()},
                {"Signos vitales", e.getSignosHistoria()}});
        subseccion(documento, "4. Estudios, diagnóstico y tratamiento", new String[][]{
                {"Resultados de estudios", e.getResultadosEstudios()},
                {"Diagnósticos o problemas clínicos", e.getDiagnostico()},
                {"Pronóstico", e.getPronostico()},
                {"Indicación terapéutica", e.getIndicacionTerapeutica()},
                {"Observaciones", e.getObservaciones()}});
        subseccion(documento, "5. Autor y fecha de elaboración", new String[][]{
                {"Elaboró", e.getAutorHistoria()},
                {"Cédula profesional", e.getCedulaHistoria()},
                {"Fecha y hora de elaboración", e.getFechaHoraHistoria()}});

        PdfPTable firma = firma(TextoExpediente.valor(e.getAutorHistoria()), "Firma autógrafa de quien elabora",
                "La captura del nombre no constituye firma electrónica.");
        firma.setWidthPercentage(45);
        firma.setHorizontalAlignment(Element.ALIGN_RIGHT);
        firma.setSpacingBefore(40);
        documento.add(firma);
    }

    private void agregarNota(Document documento, Consulta c) throws DocumentException {
        PdfPTable nota = tabla(130, 393);
        nota.setSpacingBefore(8);
        Phrase titulo = new Phrase();
        titulo.add(new Chunk("Nota de evolución #" + c.getIdConsulta(), F_FUERTE));
        titulo.add(new Chunk("    " + unir(" · ", c.getFechaConsulta() == null ? "Sin fecha"
                : c.getFechaConsulta().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                c.getEdadPaciente(), c.getSexoPaciente()), F_NOTA));
        PdfPCell cabecera = celda(titulo, Rectangle.BOX, BORDE);
        cabecera.setColspan(2);
        cabecera.setBackgroundColor(SUPERFICIE);
        cabecera.setPaddingLeft(8);
        nota.addCell(cabecera);

        filaNota(nota, "Motivo", TextoExpediente.valor(c.getMotivo()), F_DATO);
        filaNota(nota, "Padecimiento", TextoExpediente.valor(c.getPadecimiento()), F_DATO);
        if (!vacio(c.getEvolucion())) filaNota(nota, "Evolución", c.getEvolucion(), F_DATO);
        filaNota(nota, "Exploración física", TextoExpediente.valor(c.getExploracionFisica()), F_DATO);
        if (!vacio(c.getResultadosEstudios())) filaNota(nota, "Resultados de estudios", c.getResultadosEstudios(), F_DATO);
        filaNota(nota, "Diagnósticos", TextoExpediente.valor(c.getDiagnostico()), F_DATO_NEGRITA);
        if (!vacio(c.getPronostico())) filaNota(nota, "Pronóstico", c.getPronostico(), F_DATO);
        filaNota(nota, "Tratamiento e indicaciones", TextoExpediente.valor(c.getIndicaciones()), F_DATO);
        filaNota(nota, "Observaciones previas", TextoExpediente.valor(c.getObservaciones()), F_DATO);
        filaNota(nota, "Signos vitales", signosCortos(c.getSignosVitales()), F_DATO);

        PdfPCell pie = celda(new Phrase(TextoExpediente.valor(c.getNombreMedico()) + " · Cédula profesional "
                + TextoExpediente.valor(c.getCedulaMedico()) + "\n\n\n______________________________  Firma autógrafa (pendiente)",
                F_NOTA), Rectangle.NO_BORDER, BORDE);
        pie.setColspan(2);
        pie.setPaddingLeft(8);
        nota.addCell(pie);
        documento.add(nota);
    }

    private void filaNota(PdfPTable tabla, String etiqueta, String valor, Font fuente) {
        PdfPCell c = celda(new Phrase(etiqueta, F_GRIS), Rectangle.BOTTOM, BORDE_SUAVE);
        c.setPaddingLeft(8);
        tabla.addCell(c);
        tabla.addCell(celda(new Phrase(valor.trim(), fuente), Rectangle.BOTTOM, BORDE_SUAVE));
    }

    private void agregarReceta(Document documento, Receta r) throws DocumentException {
        Paragraph titulo = new Paragraph();
        titulo.add(new Chunk("Receta #" + r.getIdReceta(), F_FUERTE));
        titulo.add(new Chunk(" · " + TextoExpediente.fecha(r.getFecha())
                + "    Diagnóstico: " + TextoExpediente.valor(r.getDiagnostico()), F_GRIS));
        titulo.setSpacingBefore(6);
        titulo.setSpacingAfter(5);
        documento.add(titulo);
        if (r.getMedicamentos().isEmpty()) {
            documento.add(new Paragraph("Sin medicamentos registrados.", F_DATO));
            return;
        }
        PdfPTable tabla = tabla(34, 28, 10, 12, 16);
        tabla.setHeaderRows(1);
        for (String columna : new String[]{"Medicamento", "Dosis y posología", "Vía", "Duración", "Cantidad"}) {
            tabla.addCell(cabecera(columna, "Cantidad".equals(columna)));
        }
        for (Medicamento m : r.getMedicamentos()) {
            Phrase nombre = new Phrase();
            nombre.add(new Chunk(unir(", ", unir(" ", TextoExpediente.valor(m.getNombreMedicamento()), m.getConcentracion()),
                    m.getPresentacion()), F_DATO_NEGRITA));
            String extra = unir(" · ", vacio(m.getDenominacionDistintiva()) ? null : "Marca: " + m.getDenominacionDistintiva().trim(),
                    vacio(m.getGrupoRegulatorio()) ? null : "Tipo de receta: " + m.getGrupoRegulatorio().trim());
            if (!extra.isEmpty()) {
                nombre.add(Chunk.NEWLINE);
                nombre.add(new Chunk(extra, F_NOTA));
            }
            tabla.addCell(celda(nombre, Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(celda(new Phrase(guion(unir(" · ", m.getDosis(), m.getFrecuencia())), F_DATO), Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(celda(new Phrase(guion(m.getViaAdministracion()), F_DATO), Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(celda(new Phrase(guion(m.getDuracion()), F_DATO), Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(numero(guion(m.getCantidad())));
        }
        documento.add(tabla);
    }

    private void agregarSignos(Document documento, List<Receta> recetas) throws DocumentException {
        PdfPTable tabla = tabla(14, 10, 13, 10, 10, 10, 11, 11, 11);
        tabla.setHeaderRows(1);
        String[] columnas = {"Fecha", "Receta", "PA (mmHg)", "FC (lpm)", "FR (rpm)", "T (°C)", "SpO2 (%)", "Peso (kg)", "Talla (cm)"};
        for (int i = 0; i < columnas.length; i++) tabla.addCell(cabecera(columnas[i], i > 1));
        boolean hay = false;
        for (Receta r : recetas) {
            SignosVitales s = r.getSignosVitales();
            if (s == null) continue;
            hay = true;
            tabla.addCell(celda(new Phrase(TextoExpediente.fecha(r.getFecha()), F_DATO), Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(celda(new Phrase("#" + r.getIdReceta(), F_DATO), Rectangle.BOTTOM, BORDE_SUAVE));
            tabla.addCell(numero(guion(s.getPresionArterial())));
            tabla.addCell(numero(String.valueOf(s.getFrecuenciaCardiaca())));
            tabla.addCell(numero(String.valueOf(s.getFrecuenciaRespiratoria())));
            tabla.addCell(numero(String.valueOf(s.getTemperatura())));
            tabla.addCell(numero(String.valueOf(s.getSaturacionOxigeno())));
            tabla.addCell(numero(String.valueOf(s.getPeso())));
            tabla.addCell(numero(String.valueOf(s.getTalla())));
        }
        if (hay) documento.add(tabla);
        else documento.add(new Paragraph("No hay signos vitales registrados para este paciente.", F_DATO));
    }

    private String signosCortos(SignosVitales s) {
        if (s == null) return "No registrados";
        return "PA " + TextoExpediente.valor(s.getPresionArterial()) + " mmHg · FC " + s.getFrecuenciaCardiaca()
                + " lpm · FR " + s.getFrecuenciaRespiratoria() + " rpm · T " + s.getTemperatura() + " °C · SpO2 "
                + s.getSaturacionOxigeno() + " % · " + s.getPeso() + " kg · " + s.getTalla() + " cm";
    }

    // --- Piezas de maquetación ---

    private Document nuevoDocumento() {
        return new Document(PageSize.A4, MARGEN, MARGEN, 116, 86);
    }

    private PdfPTable tabla(float... anchos) {
        PdfPTable tabla = new PdfPTable(anchos);
        tabla.setWidthPercentage(100);
        return tabla;
    }

    private PdfPCell celda(Phrase contenido, int bordes, BaseColor colorBorde) {
        PdfPCell celda = new PdfPCell(contenido);
        celda.setBorder(bordes);
        celda.setBorderColor(colorBorde);
        celda.setBorderWidth(0.75f);
        celda.setPadding(4);
        celda.setPaddingBottom(6);
        celda.setLeading(0, 1.3f);
        return celda;
    }

    private PdfPCell sinSangria(PdfPCell celda) {
        celda.setPaddingLeft(0);
        return celda;
    }

    private PdfPCell numero(String valor) {
        PdfPCell celda = celda(new Phrase(valor, F_DATO), Rectangle.BOTTOM, BORDE_SUAVE);
        celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    private PdfPCell cabecera(String titulo, boolean derecha) {
        PdfPCell celda = celda(new Phrase(titulo, F_NOTA_NEGRITA), Rectangle.TOP | Rectangle.BOTTOM, BORDE);
        celda.setBackgroundColor(SUPERFICIE);
        if (derecha) celda.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return celda;
    }

    /** Etiqueta pequeña en mayúsculas con su valor debajo. */
    private PdfPCell campo(String etiqueta, String valor, int columnas, Font fuenteValor) {
        Phrase p = new Phrase();
        p.add(new Chunk(etiqueta.toUpperCase() + "\n", F_NOTA));
        p.add(new Chunk(valor, fuenteValor));
        PdfPCell celda = celda(p, Rectangle.NO_BORDER, BORDE);
        celda.setColspan(columnas);
        return celda;
    }

    private PdfPTable caja(PdfPTable interior, BaseColor borde, BaseColor fondo) {
        PdfPCell celda = new PdfPCell(interior);
        celda.setBorderColor(borde);
        celda.setBorderWidth(0.75f);
        celda.setBackgroundColor(fondo);
        celda.setPadding(6);
        PdfPTable caja = tabla(1);
        caja.addCell(celda);
        return caja;
    }

    private Paragraph bloque(String etiqueta, String contenido) {
        Paragraph p = new Paragraph(15);
        p.add(new Chunk(etiqueta.toUpperCase() + "\n", F_ETIQUETA));
        p.add(new Chunk(contenido, F_DATO));
        p.setSpacingBefore(12);
        return p;
    }

    /** Título de sección con filete inferior; {@code derecha} es un estado opcional alineado a la derecha. */
    private void seccion(Document documento, String titulo, Phrase derecha) throws DocumentException {
        PdfPTable t = tabla(3, 2);
        t.setSpacingBefore(18);
        t.setSpacingAfter(4);
        t.addCell(sinSangria(celda(new Phrase(titulo.toUpperCase(), F_ETIQUETA), Rectangle.BOTTOM, TINTA)));
        PdfPCell estado = celda(derecha == null ? new Phrase("") : derecha, Rectangle.BOTTOM, TINTA);
        estado.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(estado);
        documento.add(t);
    }

    private void subseccion(Document documento, String titulo, String[][] filas) throws DocumentException {
        Paragraph p = new Paragraph(titulo, F_DATO_NEGRITA);
        p.setSpacingBefore(8);
        p.setSpacingAfter(4);
        documento.add(p);
        PdfPTable t = tabla(150, 373);
        for (String[] fila : filas) {
            t.addCell(sinSangria(celda(new Phrase(fila[0], F_GRIS), Rectangle.TOP, BORDE_SUAVE)));
            t.addCell(celda(new Phrase(TextoExpediente.valor(fila[1]), F_DATO), Rectangle.TOP, BORDE_SUAVE));
        }
        documento.add(t);
    }

    /** Línea de firma con el nombre en negrita y hasta dos renglones de apoyo. */
    private PdfPTable firma(String nombre, String linea1, String linea2) {
        Phrase p = new Phrase();
        p.add(new Chunk(nombre + "\n", F_DATO_NEGRITA));
        p.add(new Chunk(linea1 + "\n" + linea2, F_NOTA));
        PdfPCell celda = celda(p, Rectangle.TOP, TINTA);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPaddingTop(6);
        PdfPTable t = new PdfPTable(1);
        t.setKeepTogether(true);
        t.addCell(celda);
        return t;
    }

    /** Encabezado y pie repetidos en cada hoja, con "Página X de N". */
    private final class Membrete extends PdfPageEventHelper {
        private final Image logo = cargarLogo();
        private final String titulo, subtitulo, linea, etiqueta, valor, derecha, pieNombre, pieDomicilio, pieDerecha;
        private final boolean paginaEnPie;
        private PdfTemplate total;

        /** {@code paginaEnPie}: la numeración va en el pie; si no, bajo el folio del encabezado. */
        Membrete(String titulo, String subtitulo, String linea, String etiqueta, String valor, String derecha,
                 String pieNombre, String pieDomicilio, String pieDerecha, boolean paginaEnPie) {
            this.titulo = titulo; this.subtitulo = subtitulo; this.linea = linea; this.etiqueta = etiqueta;
            this.valor = valor; this.derecha = derecha; this.pieNombre = pieNombre; this.pieDomicilio = pieDomicilio;
            this.pieDerecha = pieDerecha; this.paginaEnPie = paginaEnPie;
        }

        @Override public void onOpenDocument(PdfWriter w, Document d) {
            total = w.getDirectContent().createTemplate(20, 12);
        }

        @Override public void onEndPage(PdfWriter w, Document d) {
            PdfContentByte cb = w.getDirectContent();
            float alto = d.getPageSize().getHeight();
            float x = MARGEN;
            if (logo != null) {
                logo.setAbsolutePosition(MARGEN, alto - 36 - 26 - logo.getScaledHeight() / 2);
                try { cb.addImage(logo); x += logo.getScaledWidth() + 12; }
                catch (DocumentException e) { /* shortcut: logo opcional */ }
            }
            izquierda(cb, titulo, x, alto - 52, F_TITULO);
            izquierda(cb, subtitulo, x, alto - 68, F_SUBTITULO);
            izquierda(cb, linea, x, alto - 81, F_NOTA);
            derecha(cb, etiqueta, alto - 48, F_ETIQUETA);
            derecha(cb, valor, alto - 66, F_TITULO);
            if (paginaEnPie) derecha(cb, derecha, alto - 81, F_NOTA);
            else pagina(cb, w, "", alto - 81);

            cb.saveState();
            cb.setColorStroke(PRIMARIO);
            cb.setLineWidth(1.5f);
            cb.moveTo(MARGEN, alto - 100); cb.lineTo(DERECHA, alto - 100); cb.stroke();
            cb.setColorStroke(BORDE);
            cb.setLineWidth(0.75f);
            cb.moveTo(MARGEN, 72); cb.lineTo(DERECHA, 72); cb.stroke();
            cb.restoreState();

            izquierda(cb, pieNombre, MARGEN, 58, fuente(9.75f, Font.BOLD, TINTA));
            izquierda(cb, pieDomicilio, MARGEN, 46, F_NOTA);
            if (paginaEnPie) pagina(cb, w, pieDerecha + " · ", 58);
            else derecha(cb, pieDerecha, 58, F_NOTA);
        }

        @Override public void onCloseDocument(PdfWriter w, Document d) {
            ColumnText.showTextAligned(total, Element.ALIGN_LEFT,
                    new Phrase(String.valueOf(w.getPageNumber() - 1), F_NOTA), 0, 0, 0);
        }

        private void izquierda(PdfContentByte cb, String t, float x, float y, Font f) {
            if (!vacio(t)) ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(t, f), x, y, 0);
        }

        private void derecha(PdfContentByte cb, String t, float y, Font f) {
            if (!vacio(t)) ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, new Phrase(t, f), DERECHA, y, 0);
        }

        /** El total se rellena al cerrar el documento, por eso va en una plantilla aparte. */
        private void pagina(PdfContentByte cb, PdfWriter w, String prefijo, float y) {
            float anchoTotal = F_NOTA.getCalculatedBaseFont(false)
                    .getWidthPoint("00", F_NOTA.getSize()); // shortcut: reserva dos dígitos, hasta 99 páginas
            // El espacio final de "de " no cuenta al alinear a la derecha, de ahí la separación manual.
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(prefijo + "Página " + w.getPageNumber() + " de", F_NOTA), DERECHA - anchoTotal - 3, y, 0);
            cb.addTemplate(total, DERECHA - anchoTotal, y);
        }
    }

    private Image cargarLogo() {
        try {
            java.net.URL url = getClass().getResource("/Imagenes/LogoUaemex.png");
            if (url == null) return null;
            Image img = Image.getInstance(url);
            img.scaleToFit(56, 52);
            return img;
        } catch (IOException | DocumentException e) { return null; } // shortcut: logo opcional
    }

    /** Une las partes no vacías con el separador. */
    private static String unir(String separador, String... partes) {
        List<String> presentes = new ArrayList<>();
        for (String parte : partes) if (!vacio(parte)) presentes.add(parte.trim());
        return String.join(separador, presentes);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private String guion(String valor) {
        return vacio(valor) ? "-" : valor.trim();
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
    }

    private String texto(String valor) {
        return vacio(valor) ? "-" : valor;
    }

    private File archivoSalida(Receta receta) throws IOException {
        return archivoReceta(receta);
    }

    /** Nombre del paciente para su carpeta: BD si existe; si no, el que trae la receta. */
    private static String nombreCarpeta(String curp, String respaldo) {
        try {
            Paciente p = Paciente.buscar(curp);
            if (p != null) return TextoExpediente.nombre(p);
        } catch (RuntimeException e) {
            System.err.println("Carpeta del paciente sin datos de la BD: " + e.getMessage());
        }
        return respaldo;
    }

    /** Ruta de la receta: {@code <carpeta>/<paciente>/recetas/receta_<folio>_<dd-MM-yyyy>.pdf}. */
    public File archivoReceta(Receta receta) throws IOException {
        File paciente = CarpetaPaciente.resolver(new File(carpetaSalida()), receta.getCurp(),
                nombreCarpeta(receta.getCurp(), receta.getNombrePaciente()));
        String fecha = new SimpleDateFormat("dd-MM-yyyy")
                .format(receta.getFecha() == null ? new Date() : receta.getFecha());
        return new File(CarpetaPaciente.recetas(paciente), "receta_" + receta.getIdReceta() + "_" + fecha + ".pdf");
    }

    /** Genera (o sobrescribe) {@code expediente.pdf} en la carpeta del paciente. */
    public File exportarExpediente(Expediente expediente) throws IOException, DocumentException {
        if (expediente == null || expediente.getDatosPaciente() == null) {
            throw new IllegalArgumentException("El expediente debe incluir los datos del paciente.");
        }
        Paciente p = expediente.getDatosPaciente();
        File carpeta = CarpetaPaciente.resolver(new File(carpetaSalida()), p.getCurp(), TextoExpediente.nombre(p));
        File archivo = new File(carpeta, "expediente.pdf");
        generarExpediente(archivo, expediente);
        return archivo;
    }

    /** Regenera el expediente del paciente en silencio; nunca interrumpe el flujo que lo invoca. */
    public static void actualizarExpediente(String curp) {
        try {
            Expediente e = new BaseDatos().cargarExpedientePorCurp(curp);
            if (e != null) new ControladorPDF().exportarExpediente(e);
        } catch (IOException | DocumentException | RuntimeException ex) {
            System.err.println("No se pudo actualizar el expediente en PDF: " + ex.getMessage());
        }
    }

}
