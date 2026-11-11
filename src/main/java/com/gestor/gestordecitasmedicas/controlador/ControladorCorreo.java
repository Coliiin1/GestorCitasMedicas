package com.gestor.gestordecitasmedicas.controlador;

import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

public class ControladorCorreo {

    private static final String ARCHIVO_CONFIGURACION = "correo.properties";
    private static final String SERVIDOR_POR_DEFECTO = "smtp.gmail.com";
    private static final String PUERTO_POR_DEFECTO = "587";

    private String servidor;
    private String puerto;
    private String usuario;
    private String contrasena;
    private String remitente;
    private boolean ssl;
    private boolean starttls;

    public ControladorCorreo() {
        cargarConfiguracion();
    }

    public boolean enviarCorreo(String destinatario, String asunto, String cuerpoHtml) {
        if (!estaConfigurado()) {
            System.err.println("La configuración de correo está incompleta. Revisa SMTP_USER, SMTP_PASSWORD y SMTP_FROM.");
            return false;
        }
        if (vacio(destinatario)) {
            System.err.println("El destinatario del correo está vacío.");
            return false;
        }

        Properties propiedades = new Properties();
        propiedades.put("mail.smtp.auth", "true");
        propiedades.put("mail.smtp.host", servidor);
        propiedades.put("mail.smtp.port", puerto);
        propiedades.put("mail.smtp.ssl.enable", String.valueOf(ssl));
        propiedades.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        propiedades.put("mail.smtp.starttls.required", String.valueOf(starttls));
        propiedades.put("mail.smtp.ssl.checkserveridentity", "true");
        propiedades.put("mail.smtp.connectiontimeout", "15000");
        propiedades.put("mail.smtp.timeout", "15000");
        propiedades.put("mail.smtp.writetimeout", "15000");

        Session sesion = Session.getInstance(propiedades, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, contrasena);
            }
        });

        try {
            MimeMessage mensaje = new MimeMessage(sesion);
            mensaje.setFrom(new InternetAddress(remitente));
            mensaje.setReplyTo(new InternetAddress[]{new InternetAddress(remitente)});
            mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario, true));
            mensaje.setSubject(asunto == null ? "" : asunto, "UTF-8");
            mensaje.setContent(cuerpoHtml == null ? "" : cuerpoHtml, "text/html; charset=UTF-8");
            mensaje.setSentDate(new Date());
            Transport.send(mensaje);
            return true;
        } catch (MessagingException e) {
            System.err.println("No se pudo enviar el correo: " + e.getMessage());
            return false;
        }
    }

    public boolean enviarConfirmacionCita(Paciente paciente, Cita cita) {
        if (paciente == null || cita == null) {
            System.err.println("No se puede enviar la confirmación: el paciente o la cita son nulos.");
            return false;
        }
        if (vacio(paciente.getCorreo())) {
            System.err.println("El paciente no tiene un correo registrado.");
            return false;
        }

        String asunto = "Confirmación de cita médica - Folio " + cita.getIdCita();
        return enviarCorreo(paciente.getCorreo(), asunto, construirCuerpoConfirmacion(paciente, cita));
    }

    public boolean estaConfigurado() {
        return !vacio(servidor) && !vacio(puerto) && !vacio(usuario) && !vacio(contrasena) && !vacio(remitente);
    }

    public static boolean mandarCorreoElectronico() {
        ControladorCorreo controlador = new ControladorCorreo();
        String destino = controlador.remitente;
        String asunto = "Prueba de correo - Gestor de Citas Médicas";
        String cuerpo = controlador.plantilla("Prueba de correo",
                "<p style=\"margin:0;font-size:15px;color:#475569;\">La configuración SMTP funciona correctamente.</p>");
        return controlador.enviarCorreo(destino, asunto, cuerpo);
    }

    public static boolean mandarCorreoElectronico(Paciente paciente, Cita cita) {
        return new ControladorCorreo().enviarConfirmacionCita(paciente, cita);
    }

    private void cargarConfiguracion() {
        Properties propiedades = new Properties();
        cargarArchivo(propiedades);

        servidor = valor("SMTP_HOST", propiedades.getProperty("smtp.host"), SERVIDOR_POR_DEFECTO);
        puerto = valor("SMTP_PORT", propiedades.getProperty("smtp.port"), PUERTO_POR_DEFECTO);
        usuario = valor("SMTP_USER", propiedades.getProperty("smtp.user"), "");
        contrasena = valor("SMTP_PASSWORD", propiedades.getProperty("smtp.password"), "").replaceAll("\\s+", "");
        remitente = valor("SMTP_FROM", propiedades.getProperty("smtp.from"), usuario);
        ssl = Boolean.parseBoolean(valor("SMTP_SSL", propiedades.getProperty("smtp.ssl"), "false"));
        starttls = Boolean.parseBoolean(valor("SMTP_STARTTLS", propiedades.getProperty("smtp.starttls"), ssl ? "false" : "true"));
    }

    private void cargarArchivo(Properties propiedades) {
        File archivo = new File(ARCHIVO_CONFIGURACION);
        if (archivo.isFile()) {
            try (InputStream entrada = new FileInputStream(archivo)) {
                propiedades.load(entrada);
            } catch (IOException e) {
                System.err.println("No se pudo leer " + ARCHIVO_CONFIGURACION + ": " + e.getMessage());
            }
            return;
        }

        try (InputStream entrada = ControladorCorreo.class.getResourceAsStream("/" + ARCHIVO_CONFIGURACION)) {
            if (entrada != null) {
                propiedades.load(entrada);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ARCHIVO_CONFIGURACION + " del classpath: " + e.getMessage());
        }
    }

    private String valor(String variableEntorno, String valorArchivo, String valorDefecto) {
        String valor = System.getenv(variableEntorno);
        if (vacio(valor)) {
            valor = valorArchivo;
        }
        if (vacio(valor)) {
            valor = valorDefecto;
        }
        return valor == null ? "" : valor.trim();
    }

    private String construirCuerpoConfirmacion(Paciente paciente, Cita cita) {
        String nombre = unirNombre(paciente);

        String contenido = "<p style=\"margin:0 0 6px;font-size:16px;color:#0f172a;\">Hola <strong>"
                + escaparHtml(nombre) + "</strong>,</p>"
                + "<p style=\"margin:0 0 22px;font-size:15px;color:#475569;line-height:1.6;\">"
                + "Tu cita fue agendada correctamente. Estos son los detalles:</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;\">"
                + fila("Fecha y hora", "<span style=\"font-size:16px;font-weight:600;color:#0369a1;\">"
                + escaparHtml(formatearFecha(cita.getFechaCita())) + "</span>", true)
                + fila("Folio", "#" + cita.getIdCita(), true)
                + fila("Estado", pastillaEstado(cita.getEstado()), true)
                + fila("CURP", escaparHtml(texto(paciente.getCurp())), false)
                + "</table>"
                + "<p style=\"margin:22px 0 0;font-size:13px;color:#64748b;line-height:1.6;\">"
                + "Si no reconoces esta cita, comunícate con el consultorio.</p>";
        return plantilla("Cita confirmada", contenido);
    }

    /** Envoltorio común: fondo gris, tarjeta redondeada, cabecera oscura y pie (mismos colores que cita.css). */
    private String plantilla(String titulo, String contenidoHtml) {
        String fuente = "'Segoe UI',-apple-system,BlinkMacSystemFont,Roboto,Helvetica,Arial,sans-serif";
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:" + fuente + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f1f5f9;padding:28px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:560px;background:#ffffff;border:1px solid #e2e8f0;border-radius:16px;overflow:hidden;\">"
                + "<tr><td style=\"background:#1e293b;padding:26px 32px;\">"
                + "<div style=\"font-size:12px;letter-spacing:1.5px;text-transform:uppercase;color:#38bdf8;font-weight:600;\">"
                + "✚ Gestor de Citas Médicas</div>"
                + "<div style=\"margin-top:8px;font-size:24px;font-weight:600;color:#ffffff;\">" + escaparHtml(titulo) + "</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:30px 32px;\">" + contenidoHtml + "</td></tr>"
                + "<tr><td style=\"padding:18px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;"
                + "font-size:12px;color:#94a3b8;text-align:center;\">Gestor de Citas Médicas · Mensaje automático, no respondas a este correo</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    /** valorHtml ya debe venir escapado. */
    private String fila(String etiqueta, String valorHtml, boolean separador) {
        return "<tr><td style=\"padding:14px 20px;" + (separador ? "border-bottom:1px solid #e2e8f0;" : "") + "\">"
                + "<div style=\"font-size:11px;letter-spacing:1px;text-transform:uppercase;color:#64748b;\">"
                + escaparHtml(etiqueta) + "</div>"
                + "<div style=\"margin-top:4px;font-size:15px;font-weight:600;color:#0f172a;\">" + valorHtml + "</div>"
                + "</td></tr>";
    }

    private String pastillaEstado(Estado estado) {
        String texto = "Agendada";
        String fondo = "#ecfdf5";
        String color = "#065f46";
        if (estado == Estado.CANCELADA) {
            texto = "Cancelada";
            fondo = "#fff1f2";
            color = "#be123c";
        } else if (estado == Estado.ATENDIDA) {
            texto = "Atendida";
            fondo = "#e0f2fe";
            color = "#0369a1";
        }
        return "<span style=\"display:inline-block;padding:3px 12px;border-radius:999px;font-size:13px;"
                + "background:" + fondo + ";color:" + color + ";\">" + texto + "</span>";
    }

    private String unirNombre(Paciente paciente) {
        StringBuilder nombre = new StringBuilder();
        agregarParte(nombre, paciente.getNombre());
        agregarParte(nombre, paciente.getApellidoPaterno());
        agregarParte(nombre, paciente.getApellidoMaterno());
        return nombre.length() == 0 ? "paciente" : nombre.toString();
    }

    private void agregarParte(StringBuilder nombre, String parte) {
        if (!vacio(parte)) {
            if (nombre.length() > 0) {
                nombre.append(' ');
            }
            nombre.append(parte.trim());
        }
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "-";
        }
        SimpleDateFormat formato = new SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy 'a las' HH:mm", new Locale("es", "MX"));
        return formato.format(fecha);
    }

    private String texto(String valor) {
        return vacio(valor) ? "-" : valor.trim();
    }

    private String escaparHtml(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    public static void main(String[] args) {
        String correoPaciente = "carlosdanielavilacolin@gmail.com"; //aqui va el correo del destinatario

        ControladorCorreo controlador = new ControladorCorreo();
        if (!controlador.estaConfigurado()) {
            System.out.println("La configuración de correo está incompleta.");
            System.out.println("Define SMTP_USER, SMTP_PASSWORD y SMTP_FROM, o crea correo.properties en la raíz del proyecto.");
            return;
        }

        System.out.println("Servidor SMTP: " + controlador.servidor + ":" + controlador.puerto);
        System.out.println("Remitente: " + controlador.remitente);
        System.out.println("Correo del paciente: " + correoPaciente);

        Paciente paciente = new Paciente("MEND800101HDFRRN01", new Date(), "N/A", "Domicilio de prueba", "0000000000", correoPaciente);
        paciente.setNombre("Paciente");
        paciente.setApellidoPaterno("de Prueba");
        paciente.setApellidoMaterno("Gmail");

        Calendar calendario = Calendar.getInstance();
        calendario.add(Calendar.DAY_OF_MONTH, 1);

        Cita cita = new Cita();
        cita.setIdCita(1);
        cita.setFechaCita(calendario.getTime());
        cita.setEstado(Estado.AGENDADA);
        cita.setCurp(paciente.getCurp());

        boolean enviado = controlador.enviarConfirmacionCita(paciente, cita);
        if (enviado) {
            System.out.println("Correo de confirmación enviado correctamente a " + correoPaciente);
        } else {
            System.out.println("No se pudo enviar el correo. Revisa la configuración y el mensaje de error.");
        }
    }
}
