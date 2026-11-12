package com.gestor.gestordecitasmedicas.servicio;

import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Expediente;
import com.gestor.gestordecitasmedicas.modelo.Paciente;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.ColumnText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.gestor.gestordecitasmedicas.modelo.BaseDatos;

public class ControladorPDF {

    public static final String CLAVE_CARPETA = "directorio.pdf";

    public static String carpetaSalida() {
        return new BaseDatos().obtenerConfig(CLAVE_CARPETA, "pdf");
    }

    /** Guarda la carpeta donde se generan los PDF. */
    public static boolean guardarCarpetaSalida(String ruta) {
        return new BaseDatos().guardarConfig(CLAVE_CARPETA, ruta);
    }

    private static final BaseColor COLOR_PRIMARIO = new BaseColor(26, 117, 188);
    private static final Font FUENTE_TITULO = new Font(Font.FontFamily.HELVETICA, 17, Font.BOLD, COLOR_PRIMARIO);
    private static final Font FUENTE_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font FUENTE_SECCION = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
    private static final Font FUENTE_ENCABEZADO = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARIO);
    private static final Font FUENTE_BLANCA = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.WHITE);
    private static final Font FUENTE_BLANCA_NEGRITA = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
    private static final Font FUENTE_DATO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

    public boolean generarPDF(Receta receta) {
        if (!ValidacionClinica.receta(receta, true).isEmpty()) return false;
        File carpeta = new File(carpetaSalida());
        if (!carpeta.exists() && !carpeta.mkdirs()) return false;
        try {
            generarReceta(archivoSalida(receta), receta);
            return true;
        } catch (IOException | DocumentException e) {
            System.err.println("Error al generar la receta: " + e.getMessage());
            return false;
        }
    }

    private static final BaseColor FONDO = new BaseColor(232, 240, 248);

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
        com.gestor.gestordecitasmedicas.modelo.SignosVitales sv = receta.getSignosVitales();

        Document documento = new Document(PageSize.A4, 0, 0, 0, 0);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            documento.open();
            try {
                com.itextpdf.text.pdf.PdfContentByte cb = writer.getDirectContent();
                float h = PageSize.A4.getHeight();
                Font negrita = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, COLOR_PRIMARIO);
                Font chica = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, COLOR_PRIMARIO);
                Font dato = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
                Font nombre = new Font(Font.FontFamily.HELVETICA, 17, Font.BOLD, COLOR_PRIMARIO);

                // Bandas de fondo (y medido desde arriba)
                cb.setColorFill(FONDO);
                cb.rectangle(0, h - 130, 595, 130);
                cb.rectangle(0, h - 620, 595, 365);
                cb.fill();
                cb.setColorFill(COLOR_PRIMARIO);
                cb.roundRectangle(20, h - 800, 555, 60, 25);
                cb.fill();
                cb.setColorStroke(COLOR_PRIMARIO);
                cb.moveTo(205, h - 680); cb.lineTo(390, h - 680); cb.stroke();

                dibujarLogo(cb, cargarLogo(), h - 65);

                String esp = receta.getEspecialidad() == null || receta.getEspecialidad().trim().isEmpty()
                        ? receta.getProfesion() : receta.getEspecialidad();
                texto(cb, "Dr. " + receta.getNombreMedico(), 115, h - 55, nombre);
                texto(cb, texto(esp), 115, h - 75, negrita);
                campo(cb, "FECHA:", formatearFecha(receta.getFecha()), 115, h - 115, negrita, dato);
                campo(cb, "CÉDULA PROFESIONAL", texto(receta.getCedulaMedico()), 340, h - 45, negrita, negrita);
                texto(cb, texto(receta.getNombreEstablecimiento()), 340, h - 60, chica);
                texto(cb, texto(receta.getDomicilioEstablecimiento()), 340, h - 72, chica);
                campo(cb, "FOLIO:", String.valueOf(receta.getIdReceta()), 340, h - 115, negrita, dato);

                campo(cb, "PACIENTE:", texto(receta.getNombrePaciente()), 60, h - 160, negrita, dato);
                campo(cb, "CURP:", texto(receta.getCurp()), 340, h - 160, negrita, dato);
                campo(cb, "FECHA NACIMIENTO:", paciente == null ? "-" : TextoExpediente.fecha(paciente.getFechaNacimiento()), 60, h - 182, negrita, dato);
                campo(cb, "PESO:", sv == null || sv.getPeso() <= 0 ? "-" : sv.getPeso() + " kg", 250, h - 182, negrita, dato);
                campo(cb, "ESTATURA:", sv == null || sv.getTalla() <= 0 ? "-" : sv.getTalla() + " cm", 340, h - 182, negrita, dato);
                campo(cb, "SIGNOS VITALES:", sv == null ? "-" : "PA " + texto(sv.getPresionArterial()) + " | FC " + sv.getFrecuenciaCardiaca()
                        + " | FR " + sv.getFrecuenciaRespiratoria() + " | T " + sv.getTemperatura() + " °C | SpO2 " + sv.getSaturacionOxigeno() + "%",
                        60, h - 204, negrita, new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK));
                campo(cb, "DOMICILIO DEL PACIENTE:", paciente == null ? "-" : texto(paciente.getDomicilio()), 60, h - 226, negrita, dato);

                // Diagnóstico, medicamentos e indicaciones: texto con ajuste de línea dentro de la banda
                ColumnText col = new ColumnText(cb);
                col.setSimpleColumn(60, h - 605, 535, h - 275);
                Paragraph p = new Paragraph("DIAGNOSTICO: ", negrita);
                p.add(new Chunk(texto(receta.getDiagnostico()), dato));
                p.setSpacingAfter(14);
                col.addElement(p);
                col.addElement(new Paragraph("MEDICAMENTOS:", negrita));
                for (Medicamento m : receta.getMedicamentos()) {
                    Paragraph li = new Paragraph("•  " + lineaMedicamento(m), dato);
                    li.setIndentationLeft(12);
                    col.addElement(li);
                }
                String ind = TextoExpediente.valor(receta.getIndicaciones());
                if (ind != null && !ind.trim().isEmpty() && !"-".equals(ind.trim())) {
                    Paragraph pi = new Paragraph("INDICACIONES: ", negrita);
                    pi.setSpacingBefore(10);
                    pi.add(new Chunk(ind, dato));
                    col.addElement(pi);
                }
                col.go(); // shortcut: una sola página, el excedente se recorta; paginar si hace falta

                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase("FIRMA", negrita), 297, h - 695, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(texto(receta.getNombreEstablecimiento()),
                        FUENTE_BLANCA_NEGRITA), 297, h - 765, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, new Phrase(texto(receta.getDomicilioEstablecimiento()), FUENTE_BLANCA), 297, h - 782, 0);
            } finally { documento.close(); }
        }
    }

    /** Se carga una vez por documento para no incrustar la imagen en cada página. */
    private com.itextpdf.text.Image cargarLogo() {
        try {
            java.net.URL url = getClass().getResource("/Imagenes/LogoUaemex.png");
            if (url == null) return null;
            com.itextpdf.text.Image img = com.itextpdf.text.Image.getInstance(url);
            img.scaleToFit(75, 85);
            return img;
        } catch (IOException | DocumentException e) { return null; } // shortcut: logo opcional
    }

    private void dibujarLogo(com.itextpdf.text.pdf.PdfContentByte cb, com.itextpdf.text.Image img, float centroY) {
        if (img == null) return;
        img.setAbsolutePosition(30, centroY - img.getScaledHeight() / 2);
        try { cb.addImage(img); } catch (DocumentException e) { /* shortcut: logo opcional */ }
    }

    private void texto(com.itextpdf.text.pdf.PdfContentByte cb, String t, float x, float y, Font f) {
        ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, new Phrase(t, f), x, y, 0);
    }

    private void campo(com.itextpdf.text.pdf.PdfContentByte cb, String etiqueta, String valor, float x, float y, Font fe, Font fv) {
        texto(cb, etiqueta, x, y, fe);
        texto(cb, valor, x + fe.getCalculatedBaseFont(false).getWidthPoint(etiqueta, fe.getSize()) + 5, y, fv);
    }

    private String lineaMedicamento(Medicamento m) {
        StringBuilder sb = new StringBuilder(texto(m.getNombreMedicamento()));
        for (String v : new String[]{m.getConcentracion(), m.getPresentacion(), m.getDosis(), m.getViaAdministracion(),
                m.getFrecuencia(), m.getDuradcion(), m.getCantidad()})
            if (v != null && !v.trim().isEmpty()) sb.append(" | ").append(v.trim());
        return sb.toString();
    }

    private PdfPTable tablaDatos() throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1f, 2f});
        return tabla;
    }

    /** Encabezado y pie con el mismo estilo de la receta, repetidos en cada página. */
    private void identificarPaginas(PdfWriter writer, Expediente expediente, Paciente paciente) {
        final com.itextpdf.text.Image logo = cargarLogo();
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w, Document d) {
                float h = d.getPageSize().getHeight();
                com.itextpdf.text.pdf.PdfContentByte fondo = w.getDirectContentUnder();
                fondo.saveState();
                fondo.setColorFill(FONDO);
                fondo.rectangle(0, h - 110, 595, 110);
                fondo.fill();
                fondo.setColorFill(COLOR_PRIMARIO);
                fondo.roundRectangle(20, 20, 555, 55, 25);
                fondo.fill();
                fondo.restoreState();

                com.itextpdf.text.pdf.PdfContentByte cb = w.getDirectContent();
                dibujarLogo(cb, logo, h - 55);
                texto(cb, "Expediente clínico", 115, h - 50, FUENTE_TITULO);
                texto(cb, TextoExpediente.nombre(paciente), 115, h - 72, FUENTE_ENCABEZADO);
                campo(cb, "EXPEDIENTE:", expediente.getIdExpediente() > 0 ? "#" + expediente.getIdExpediente() : "-",
                        400, h - 50, FUENTE_ENCABEZADO, FUENTE_DATO);
                campo(cb, "PÁGINA:", String.valueOf(w.getPageNumber()), 400, h - 72, FUENTE_ENCABEZADO, FUENTE_DATO);

                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase("Confidencial | Pendiente de firma autógrafa", FUENTE_SUBTITULO), 297, 85, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(texto(expediente.getNombreEstablecimiento()), FUENTE_BLANCA_NEGRITA), 297, 52, 0);
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                        new Phrase(texto(expediente.getDomicilioEstablecimiento()), FUENTE_BLANCA), 297, 35, 0);
            }
        });
    }

    public void generarExpediente(File archivo, Expediente expediente)
            throws IOException, DocumentException {
        if (expediente == null || expediente.getDatosPaciente() == null) {
            throw new IllegalArgumentException("El expediente debe incluir los datos del paciente.");
        }
        Paciente paciente = expediente.getDatosPaciente();
        Document documento = new Document(PageSize.A4, 60, 60, 125, 105);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            identificarPaginas(writer, expediente, paciente);
            documento.open();
            try {
                agregarSeccion(documento, "Ficha de identidad");
                PdfPTable datos = tablaDatos();
                agregarFila(datos, "Nombre", TextoExpediente.nombre(paciente));
                agregarFila(datos, "Expediente", expediente.getIdExpediente() > 0
                        ? String.valueOf(expediente.getIdExpediente()) : "Sin expediente registrado");
                agregarFila(datos, "Fecha de apertura", TextoExpediente.fecha(expediente.getFechaApertura()));
                agregarFila(datos, "CURP", texto(paciente.getCurp()));
                agregarFila(datos, "Teléfono", texto(paciente.getTelefono()));
                agregarFila(datos, "Fecha de nacimiento", TextoExpediente.fecha(paciente.getFechaNacimiento()));
                agregarFila(datos, "Sexo", texto(paciente.getSexo()));
                agregarFila(datos, "Edad a la apertura", TextoExpediente.edad(paciente, expediente.getFechaApertura()));
                agregarFila(datos, "Domicilio", texto(paciente.getDomicilio()));
                agregarFila(datos, "Correo", texto(paciente.getCorreo()));
                documento.add(datos);
                agregarBloqueClinico(documento, "Historia clínica ambulatoria", TextoExpediente.historia(expediente));
                agregarBloqueClinico(documento, "Alergias y advertencias", TextoExpediente.valor(expediente.getAlergias()));
                agregarSeccion(documento, "Notas de evolución");
                if (expediente.getConsultas().isEmpty()) documento.add(new Paragraph("No hay consultas registradas para este paciente.", FUENTE_DATO));
                for (com.gestor.gestordecitasmedicas.modelo.Consulta consulta : expediente.getConsultas()) {
                    Expediente nota = new Expediente();
                    nota.getConsultas().add(consulta);
                    agregarBloqueClinico(documento, "Nota de evolución #" + consulta.getIdConsulta(), TextoExpediente.consultas(nota));
                }
                agregarBloqueClinico(documento, "Recetas y farmacología", TextoExpediente.recetas(expediente));
                agregarBloqueClinico(documento, "Evolución de signos vitales", TextoExpediente.signos(expediente));
            } finally {
                documento.close();
            }
        }
    }
    /** Recuadro con el fondo claro de la receta; se parte entre páginas si el texto es largo. */
    private void agregarBloqueClinico(Document documento, String titulo, String contenido) throws DocumentException {
        PdfPCell celda = new PdfPCell();
        celda.setBorder(PdfPCell.NO_BORDER);
        celda.setBackgroundColor(FONDO);
        celda.setPadding(12);
        celda.addElement(new Paragraph(titulo.toUpperCase(), FUENTE_ENCABEZADO));
        Paragraph cuerpo = new Paragraph(contenido, FUENTE_DATO);
        cuerpo.setSpacingBefore(4);
        celda.addElement(cuerpo);
        PdfPTable bloque = new PdfPTable(1);
        bloque.setWidthPercentage(100);
        bloque.setSpacingBefore(10);
        bloque.setSplitLate(false);
        bloque.addCell(celda);
        documento.add(bloque);
    }

    private void agregarSeccion(Document documento, String titulo) throws DocumentException {
        Paragraph seccion = new Paragraph(titulo.toUpperCase(), FUENTE_SECCION);
        seccion.setSpacingBefore(16);
        seccion.setSpacingAfter(4);
        documento.add(seccion);
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta.toUpperCase() + ":", FUENTE_ENCABEZADO));
        celdaEtiqueta.setBorder(PdfPCell.NO_BORDER);
        celdaEtiqueta.setPadding(5);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FUENTE_DATO));
        celdaValor.setBorder(PdfPCell.BOTTOM);
        celdaValor.setBorderColor(FONDO);
        celdaValor.setPadding(5);
        tabla.addCell(celdaValor);
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "-";
        }
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
    }

    private String texto(String valor) {
        return valor == null || valor.trim().isEmpty() ? "-" : valor;
    }

    private File archivoSalida(Receta receta) {
        return new File(new File(carpetaSalida()), "receta_" + receta.getIdReceta() + ".pdf");
    }

}
