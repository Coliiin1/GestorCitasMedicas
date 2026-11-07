package com.gestor.gestordecitasmedicas.controlador;

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

public class ControladorPDF {

    public static final String CARPETA_SALIDA = "pdf";

    private static final BaseColor COLOR_PRIMARIO = new BaseColor(30, 64, 175);
    private static final Font FUENTE_TITULO = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, COLOR_PRIMARIO);
    private static final Font FUENTE_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font FUENTE_SECCION = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, COLOR_PRIMARIO);
    private static final Font FUENTE_ENCABEZADO = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final Font FUENTE_DATO = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);

    public boolean generarPDF(Receta receta) {
        if (!ValidacionClinica.receta(receta, true).isEmpty()) return false;
        File carpeta = new File(CARPETA_SALIDA);
        if (!carpeta.exists() && !carpeta.mkdirs()) return false;
        try {
            generarReceta(archivoSalida(receta), receta);
            return true;
        } catch (IOException | DocumentException e) {
            System.err.println("Error al generar la receta: " + e.getMessage());
            return false;
        }
    }

    /** Genera un formato para firma autógrafa, nunca una receta electrónica firmada. */
    public void generarReceta(File archivo, Receta receta) throws IOException, DocumentException {
        List<String> errores = ValidacionClinica.receta(receta, true);
        if (!errores.isEmpty()) throw new IllegalArgumentException(String.join("\n", errores));
        Document documento = new Document(PageSize.A4, 40, 40, 65, 60);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            identificarPaginas(writer, "Receta #" + receta.getIdReceta() + " | " + receta.getNombrePaciente());
            documento.open();
            try {
                documento.add(new Paragraph("Receta médica ordinaria", FUENTE_TITULO));
                documento.add(new Paragraph("Formato para firma autógrafa - pendiente de firma", FUENTE_SUBTITULO));
                agregarSeccion(documento, "Prescriptor y establecimiento");
                PdfPTable emisor = tablaDatos();
                agregarFila(emisor, "Profesional que prescribe", receta.getNombreMedico());
                agregarFila(emisor, "Profesión y cédula profesional", receta.getProfesion() + " | " + receta.getCedulaMedico());
                agregarFila(emisor, "Institución que expidió el título", receta.getInstitucionTitulo());
                agregarFila(emisor, "Domicilio del prescriptor", receta.getDomicilioMedico());
                if (receta.getEspecialidad() != null && !receta.getEspecialidad().trim().isEmpty())
                    agregarFila(emisor, "Especialidad y registro", receta.getEspecialidad() + " | " + receta.getRegistroEspecialidad());
                agregarFila(emisor, "Establecimiento", receta.getNombreEstablecimiento());
                agregarFila(emisor, "Domicilio del establecimiento", receta.getDomicilioEstablecimiento());
                documento.add(emisor);
                agregarSeccion(documento, "Paciente y prescripción");
                PdfPTable datos = tablaDatos();
                agregarFila(datos, "Folio interno", String.valueOf(receta.getIdReceta()));
                agregarFila(datos, "Fecha y hora de expedición", formatearFecha(receta.getFecha()));
                agregarFila(datos, "Nombre completo del paciente", receta.getNombrePaciente());
                agregarFila(datos, "Edad y sexo", receta.getEdadPaciente() + " | " + receta.getSexoPaciente());
                agregarFila(datos, "CURP", texto(receta.getCurp()));
                agregarFila(datos, "Diagnóstico", receta.getDiagnostico());
                documento.add(datos);
                agregarMedicamentos(documento, receta.getMedicamentos());
                agregarBloqueClinico(documento, "Indicaciones generales y seguimiento", TextoExpediente.valor(receta.getIndicaciones()));
                Paragraph firma = new Paragraph("\n\n________________________________________\nFirma autógrafa de " + receta.getNombreMedico()
                        + "\nCédula profesional: " + receta.getCedulaMedico()
                        + "\nImprimir y firmar por el profesional que prescribe.", FUENTE_DATO);
                firma.setKeepTogether(true);
                firma.setAlignment(Element.ALIGN_CENTER);
                documento.add(firma);
            } finally { documento.close(); }
        }
    }

    private PdfPTable tablaDatos() throws DocumentException {
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1f, 2f});
        return tabla;
    }

    private void identificarPaginas(PdfWriter writer, final String identificador) {
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override public void onEndPage(PdfWriter w, Document d) {
                w.getDirectContent().saveState();
                w.getDirectContent().setColorFill(BaseColor.BLACK);
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_LEFT,
                        new Phrase(identificador, FUENTE_DATO), d.left(), d.top() + 22, 0);
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_LEFT,
                        new Phrase("Confidencial | Pendiente de firma autógrafa", FUENTE_SUBTITULO), d.left(), 30, 0);
                ColumnText.showTextAligned(w.getDirectContent(), Element.ALIGN_RIGHT,
                        new Phrase("Página " + w.getPageNumber(), FUENTE_DATO), d.right(), 30, 0);
                w.getDirectContent().restoreState();
            }
        });
    }

    public void generarExpediente(File archivo, Expediente expediente)
            throws IOException, DocumentException {
        if (expediente == null || expediente.getDatosPaciente() == null) {
            throw new IllegalArgumentException("El expediente debe incluir los datos del paciente.");
        }
        Paciente paciente = expediente.getDatosPaciente();
        Document documento = new Document(PageSize.A4, 40, 40, 65, 60);
        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            identificarPaginas(writer, "Expediente #" + expediente.getIdExpediente() + " | " + TextoExpediente.nombre(paciente));
            documento.open();
            try {
                documento.add(new Paragraph("Expediente del paciente", FUENTE_TITULO));
                documento.add(new Paragraph("Consulta externa - copia para revisión y firma autógrafa", FUENTE_SUBTITULO));
                agregarSeccion(documento, "Ficha de identidad");
                PdfPTable datos = new PdfPTable(2);
                datos.setWidthPercentage(100);
                datos.setWidths(new float[]{1f, 2f});
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
                for (com.itextpdf.text.pdf.PdfPRow fila : datos.getRows()) {
                    for (PdfPCell celda : fila.getCells()) {
                        if (celda != null) celda.setPadding(5);
                    }
                }
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
    private void agregarBloqueClinico(Document documento, String titulo, String contenido) throws DocumentException {
        Paragraph bloque = new Paragraph();
        bloque.setSpacingBefore(14);
        bloque.setKeepTogether(false);
        bloque.add(new Chunk(titulo + "\n", FUENTE_SECCION));
        bloque.add(new Chunk(contenido, FUENTE_DATO));
        documento.add(bloque);
    }

    private void agregarSeccion(Document documento, String titulo) throws DocumentException {
        Paragraph seccion = new Paragraph(titulo, FUENTE_SECCION);
        seccion.setSpacingBefore(14);
        seccion.setSpacingAfter(6);
        documento.add(seccion);
    }

    private void agregarMedicamentos(Document documento, List<Medicamento> medicamentos) throws DocumentException {
        agregarSeccion(documento, "Prescripción farmacológica");
        int i = 0;
        for (Medicamento m : medicamentos) {
            PdfPTable tabla = tablaDatos();
            tabla.setSpacingBefore(8);
            tabla.setSplitLate(false);
            tabla.setKeepTogether(true);
            agregarFila(tabla, "Medicamento " + (++i) + " / denominación genérica", m.getNombreMedicamento());
            if (m.getDenominacionDistintiva() != null && !m.getDenominacionDistintiva().trim().isEmpty())
                agregarFila(tabla, "Denominación distintiva", m.getDenominacionDistintiva());
            agregarFila(tabla, "Concentración y presentación", m.getConcentracion() + " | " + m.getPresentacion());
            agregarFila(tabla, "Dosis y vía de administración", m.getDosis() + " | " + m.getViaAdministracion());
            agregarFila(tabla, "Frecuencia y duración", m.getFrecuencia() + " | " + m.getDuradcion());
            agregarFila(tabla, "Cantidad a dispensar", m.getCantidad());
            agregarFila(tabla, "Fracción regulatoria declarada", m.getGrupoRegulatorio());
            tabla.setHeaderRows(1);
            documento.add(tabla);
        }
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FUENTE_ENCABEZADO));
        celdaEtiqueta.setBackgroundColor(COLOR_PRIMARIO);
        celdaEtiqueta.setPadding(5);
        celdaEtiqueta.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FUENTE_DATO));
        celdaValor.setPadding(5);
        celdaValor.setHorizontalAlignment(Element.ALIGN_LEFT);
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
        return new File(new File(CARPETA_SALIDA), "receta_" + receta.getIdReceta() + ".pdf");
    }

}
