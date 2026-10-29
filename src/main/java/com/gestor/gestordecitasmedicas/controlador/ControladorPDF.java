package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Medicamento;
import com.gestor.gestordecitasmedicas.modelo.Receta;
import com.gestor.gestordecitasmedicas.modelo.SignosVitales;
import com.itextpdf.text.BaseColor;
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
        if (receta == null) {
            System.err.println("No se puede generar el PDF: la receta es nula.");
            return false;
        }

        File carpeta = new File(CARPETA_SALIDA);
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            System.err.println("No se pudo crear la carpeta de salida: " + carpeta.getAbsolutePath());
            return false;
        }

        File archivo = archivoSalida(receta);
        Document documento = new Document(PageSize.A4, 40, 40, 40, 40);
        boolean generado = false;

        try (FileOutputStream salida = new FileOutputStream(archivo)) {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Paragraph titulo = new Paragraph("Receta Médica", FUENTE_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            Paragraph subtitulo = new Paragraph("Gestor de Citas Médicas", FUENTE_SUBTITULO);
            subtitulo.setAlignment(Element.ALIGN_CENTER);
            subtitulo.setSpacingAfter(20);
            documento.add(subtitulo);

            agregarSeccion(documento, "Datos de la receta");
            PdfPTable datos = new PdfPTable(2);
            datos.setWidthPercentage(100);
            datos.setWidths(new float[]{1f, 2f});
            agregarFila(datos, "Folio de receta", String.valueOf(receta.getIdReceta()));
            agregarFila(datos, "Fecha", formatearFecha(receta.getFecha()));
            agregarFila(datos, "CURP del paciente", texto(receta.getCurp()));
            documento.add(datos);

            agregarSignosVitales(documento, receta.getSignosVitales());
            agregarMedicamentos(documento, receta.getMedicamentos());

            Paragraph pie = new Paragraph("Documento generado automáticamente por el sistema.", FUENTE_SUBTITULO);
            pie.setSpacingBefore(20);
            pie.setAlignment(Element.ALIGN_CENTER);
            documento.add(pie);

            documento.close();
            generado = true;
        } catch (DocumentException | IOException e) {
            System.err.println("Error al generar el PDF: " + e.getMessage());
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }

        return generado && archivo.exists() && archivo.length() > 0;
    }

    private void agregarSeccion(Document documento, String titulo) throws DocumentException {
        Paragraph seccion = new Paragraph(titulo, FUENTE_SECCION);
        seccion.setSpacingBefore(14);
        seccion.setSpacingAfter(6);
        documento.add(seccion);
    }

    private void agregarSignosVitales(Document documento, SignosVitales signos) throws DocumentException {
        agregarSeccion(documento, "Signos vitales");

        if (signos == null) {
            documento.add(new Paragraph("No hay signos vitales registrados.", FUENTE_DATO));
            return;
        }

        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{1f, 1f});
        agregarFila(tabla, "Presión arterial", texto(signos.getPresionArterial()));
        agregarFila(tabla, "Frecuencia cardíaca", signos.getFrecuenciaCardiaca() + " lpm");
        agregarFila(tabla, "Frecuencia respiratoria", signos.getFrecuenciaRespiratoria() + " rpm");
        agregarFila(tabla, "Temperatura", signos.getTemperatura() + " °C");
        agregarFila(tabla, "Saturación de oxígeno", signos.getSaturacionOxigeno() + " %");
        agregarFila(tabla, "Peso", signos.getPeso() + " kg");
        agregarFila(tabla, "Talla", signos.getTalla() + " cm");
        documento.add(tabla);
    }

    private void agregarMedicamentos(Document documento, List<Medicamento> medicamentos) throws DocumentException {
        agregarSeccion(documento, "Medicamentos");

        if (medicamentos == null || medicamentos.isEmpty()) {
            documento.add(new Paragraph("No hay medicamentos registrados.", FUENTE_DATO));
            return;
        }

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{2.2f, 1.4f, 1.6f, 1.8f, 1.4f});
        agregarEncabezado(tabla, "Medicamento");
        agregarEncabezado(tabla, "Dosis");
        agregarEncabezado(tabla, "Concentración");
        agregarEncabezado(tabla, "Frecuencia");
        agregarEncabezado(tabla, "Duración");

        for (Medicamento medicamento : medicamentos) {
            if (medicamento == null) {
                agregarCelda(tabla, "-");
                agregarCelda(tabla, "-");
                agregarCelda(tabla, "-");
                agregarCelda(tabla, "-");
                agregarCelda(tabla, "-");
                continue;
            }
            agregarCelda(tabla, texto(medicamento.getNombreMedicamento()));
            agregarCelda(tabla, texto(medicamento.getDosis()));
            agregarCelda(tabla, texto(medicamento.getConcentracion()));
            agregarCelda(tabla, texto(medicamento.getFrecuencia()));
            agregarCelda(tabla, texto(medicamento.getDuradcion()));
        }

        documento.add(tabla);
    }

    private void agregarFila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FUENTE_ENCABEZADO));
        celdaEtiqueta.setBackgroundColor(COLOR_PRIMARIO);
        celdaEtiqueta.setPadding(8);
        celdaEtiqueta.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaEtiqueta);

        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FUENTE_DATO));
        celdaValor.setPadding(8);
        celdaValor.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celdaValor);
    }

    private void agregarEncabezado(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FUENTE_ENCABEZADO));
        celda.setBackgroundColor(COLOR_PRIMARIO);
        celda.setPadding(6);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.addCell(celda);
    }

    private void agregarCelda(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, FUENTE_DATO));
        celda.setPadding(6);
        celda.setHorizontalAlignment(Element.ALIGN_LEFT);
        tabla.addCell(celda);
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

    public static void main(String[] args) {
        Receta receta = new Receta();
        receta.setIdReceta(1);
        receta.setCurp("MEND800101HDFRRN01");
        receta.setFecha(new Date());

        Medicamento paracetamol = new Medicamento();
        paracetamol.setNombreMedicamento("Paracetamol");
        paracetamol.setDosis("500 mg");
        paracetamol.setConcentracion("Tableta");
        paracetamol.setFrecuencia("Cada 8 horas");
        paracetamol.setDuradcion("5 días");
        receta.getMedicamentos().add(paracetamol);

        Medicamento amoxicilina = new Medicamento();
        amoxicilina.setNombreMedicamento("Amoxicilina");
        amoxicilina.setDosis("500 mg");
        amoxicilina.setConcentracion("Cápsula");
        amoxicilina.setFrecuencia("Cada 12 horas");
        amoxicilina.setDuradcion("7 días");
        receta.getMedicamentos().add(amoxicilina);

        receta.setSignosVitales(new SignosVitales("120/80", 72, 18, 36.5f, 98, 70.5f, 170));

        ControladorPDF controlador = new ControladorPDF();
        boolean generado = controlador.generarPDF(receta);

        File archivo = controlador.archivoSalida(receta);
        if (generado) {
            System.out.println("PDF generado correctamente: " + archivo.getAbsolutePath());
        } else {
            System.out.println("No se pudo generar el PDF.");
        }
    }
}
