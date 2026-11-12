package com.gestor.gestordecitasmedicas.servicio;

import com.gestor.gestordecitasmedicas.modelo.Cita;
import com.gestor.gestordecitasmedicas.modelo.Estado;
import com.gestor.gestordecitasmedicas.modelo.Paciente;

import javafx.application.Platform;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

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

    public boolean enviarReprogramacionCita(Paciente paciente, Cita cita, Date fechaAnterior) {
        if (paciente == null || cita == null || vacio(paciente.getCorreo())) {
            return false;
        }
        String contenido = saludo(paciente, "tu cita se movió a una nueva fecha. Estos son los detalles.")
                + bloqueFecha(cita.getFechaCita(), "Nueva fecha y hora", false, "Fecha anterior",
                "<span style=\"color:#64748b;text-decoration:line-through;\">"
                        + escaparHtml(formatearFecha(fechaAnterior)) + "</span>")
                + detalles(cita.getIdCita(), cita.getEstado())
                + nota("Si no puedes asistir en el nuevo horario, comunícate con el consultorio.");
        return enviarCorreo(paciente.getCorreo(), "Cita reprogramada - Folio " + cita.getIdCita(),
                plantilla("Cita reprogramada", "#b45309", "#fffbeb", "#fde68a", "Tu cita cambió de horario", contenido));
    }

    /** {@code motivo} es opcional (null o vacío = no se muestra); solo viaja en el correo, no se guarda. */
    public boolean enviarCancelacionCita(Paciente paciente, Cita cita, String motivo) {
        if (paciente == null || cita == null || vacio(paciente.getCorreo())) {
            return false;
        }
        String contenido = saludo(paciente, "lamentamos informarte que la siguiente cita ya no se llevará a cabo.")
                + bloqueFecha(cita.getFechaCita(), "Fecha y hora canceladas", true,
                vacio(motivo) ? null : "Motivo", vacio(motivo) ? null : escaparHtml(motivo.trim()))
                + detalles(cita.getIdCita(), Estado.CANCELADA)
                + nota("Comunícate con el consultorio para reagendar tu cita. Disculpa las molestias.");
        return enviarCorreo(paciente.getCorreo(), "Cita cancelada - Folio " + cita.getIdCita(),
                plantilla("Cita cancelada", "#be123c", "#fff1f2", "#fecdd3", "Tu cita fue cancelada", contenido));
    }

    /**
     * Notifica en segundo plano la cancelación de cada cita (omite pacientes sin correo).
     * alTerminar(enviados, total) se ejecuta en el hilo de JavaFX.
     */
    public static void notificarCancelaciones(List<Cita> citas, Map<String, Paciente> pacientes, String motivo,
                                              BiConsumer<Integer, Integer> alTerminar) {
        List<Cita> copia = new ArrayList<>(citas);
        Thread hilo = new Thread(() -> {
            ControladorCorreo controlador = new ControladorCorreo();
            int enviados = 0;
            if (controlador.estaConfigurado()) {
                for (Cita cita : copia) {
                    if (controlador.enviarCancelacionCita(pacientes.get(cita.getCurp()), cita, motivo)) {
                        enviados++;
                    }
                }
            }
            int total = copia.size();
            int ok = enviados;
            Platform.runLater(() -> alTerminar.accept(ok, total));
        }, "correo-cancelacion");
        hilo.setDaemon(true);
        hilo.start();
    }

    public boolean estaConfigurado() {
        return !vacio(servidor) && !vacio(puerto) && !vacio(usuario) && !vacio(contrasena) && !vacio(remitente);
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

    private static final String FUENTE = "'Segoe UI',-apple-system,BlinkMacSystemFont,Roboto,Helvetica,Arial,sans-serif";
    private static final String ROTULO = "font-size:12px;line-height:16px;letter-spacing:1px;text-transform:uppercase;color:#475569;";
    private static final String TABLA = "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" ";

    private String saludo(Paciente paciente, String introduccion) {
        return "<p style=\"margin:0 0 20px;font-size:15px;line-height:24px;color:#475569;\">Hola <strong style=\"color:#0f172a;\">"
                + escaparHtml(unirNombre(paciente)) + "</strong>, " + introduccion + "</p>";
    }

    private String construirCuerpoConfirmacion(Paciente paciente, Cita cita) {
        String contenido = saludo(paciente, "tu cita quedó registrada. Estos son los detalles.")
                + bloqueFecha(cita.getFechaCita(), "Fecha y hora", false, null, null)
                + detalles(cita.getIdCita(), cita.getEstado())
                + nota("Si no reconoces esta cita o necesitas cambiarla, comunícate con el consultorio.");
        return plantilla("Cita confirmada", "#047857", "#ecfdf5", "#a7f3d0", "Tu cita está agendada", contenido);
    }

    /**
     * Hoja de calendario con la fecha como dato principal. {@code cancelada} la muestra tachada y en gris;
     * {@code pieHtml} (ya escapado) agrega un renglón inferior opcional, p. ej. la fecha anterior o el motivo.
     */
    private String bloqueFecha(Date fecha, String etiqueta, boolean cancelada, String pieEtiqueta, String pieHtml) {
        String acento = cancelada ? "#64748b" : "#0369a1";
        String tachado = cancelada ? "text-decoration:line-through;" : "";
        String grande = "font-size:18px;line-height:26px;font-weight:700;" + tachado;
        return TABLA + "width=\"100%\" style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;\">"
                + "<tr><td style=\"padding:16px;\">" + TABLA + "><tr>"
                + "<td width=\"76\" valign=\"middle\">" + TABLA + "width=\"76\" style=\"border:1px solid " + acento
                + ";border-radius:8px;background:#ffffff;text-align:center;\">"
                + "<tr><td style=\"background:" + acento + ";color:#ffffff;font-size:12px;line-height:22px;font-weight:700;"
                + "letter-spacing:1.5px;border-radius:7px 7px 0 0;\">" + parte(fecha, "MMM").replace(".", "").toUpperCase() + "</td></tr>"
                + "<tr><td style=\"padding-top:2px;font-size:30px;line-height:40px;font-weight:700;color:"
                + (cancelada ? "#64748b" : "#0f172a") + ";" + tachado + "\">" + parte(fecha, "d") + "</td></tr>"
                + "<tr><td style=\"padding-bottom:6px;font-size:12px;line-height:16px;color:#475569;\">" + parte(fecha, "EEEE") + "</td></tr>"
                + "</table></td>"
                + "<td valign=\"middle\" style=\"padding-left:16px;\">"
                + "<div style=\"" + ROTULO + "\">" + escaparHtml(etiqueta) + "</div>"
                + "<div style=\"" + grande + "color:" + (cancelada ? "#475569" : "#0f172a") + ";\">"
                + parte(fecha, "EEEE d 'de' MMMM 'de' yyyy") + "</div>"
                + "<div style=\"" + grande + "color:" + (cancelada ? "#475569" : "#0369a1") + ";\">" + parte(fecha, "HH:mm 'h'") + "</div>"
                + "</td></tr></table></td></tr>"
                + (pieHtml == null ? "" : "<tr><td style=\"padding:12px 16px;border-top:1px solid #e2e8f0;\">"
                + "<div style=\"" + ROTULO + "\">" + escaparHtml(pieEtiqueta) + "</div>"
                + "<div style=\"font-size:14px;line-height:20px;color:#0f172a;\">" + pieHtml + "</div></td></tr>")
                + "</table>";
    }

    private String detalles(int folio, Estado estado) {
        return TABLA + "width=\"100%\" style=\"margin-top:20px;\"><tr>"
                + "<td width=\"50%\"><div style=\"" + ROTULO + "\">Folio</div>"
                + "<div style=\"font-size:15px;line-height:24px;font-weight:700;color:#0f172a;\">#" + folio + "</div></td>"
                + "<td width=\"50%\"><div style=\"" + ROTULO + "\">Estado</div>" + textoEstado(estado) + "</td>"
                + "</tr></table>";
    }

    private String nota(String texto) {
        return "<p style=\"margin:20px 0 0;padding-top:16px;border-top:1px solid #e2e8f0;font-size:13px;line-height:20px;color:#475569;\">"
                + texto + "</p>";
    }

    /**
     * Envoltorio común: tarjeta blanca sobre fondo gris con los colores de cita.css. El tipo de aviso va en una
     * etiqueta con su terna tinta/fondo/borde; el cuerpo queda neutro (el color señala, no inunda).
     */
    private String plantilla(String tipo, String tinta, String fondo, String borde, String titulo, String contenidoHtml) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:" + FUENTE + ";\">"
                + TABLA + "width=\"100%\" style=\"background:#f1f5f9;padding:28px 12px;\"><tr><td align=\"center\">"
                + TABLA + "width=\"100%\" style=\"max-width:560px;background:#ffffff;border:1px solid #e2e8f0;"
                + "border-radius:12px;overflow:hidden;font-family:" + FUENTE + ";\">"
                + "<tr><td style=\"padding:20px 32px;border-bottom:1px solid #e2e8f0;\">" + TABLA + "width=\"100%\"><tr>"
                + "<td style=\"font-size:13px;line-height:18px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#475569;\">"
                + "Gestor de Citas Médicas</td>"
                + "<td align=\"right\"><span style=\"display:inline-block;padding:3px 12px;border-radius:8px;font-size:13px;"
                + "line-height:18px;font-weight:700;white-space:nowrap;color:" + tinta + ";background:" + fondo
                + ";border:1px solid " + borde + ";\">" + escaparHtml(tipo) + "</span></td>"
                + "</tr></table></td></tr>"
                + "<tr><td style=\"padding:28px 32px 32px;\">"
                + "<h1 style=\"margin:0 0 8px;font-size:24px;line-height:32px;font-weight:700;color:#0f172a;\">"
                + escaparHtml(titulo) + "</h1>" + contenidoHtml + "</td></tr>"
                + "<tr><td style=\"padding:16px 32px;background:#f8fafc;border-top:1px solid #e2e8f0;"
                + "font-size:12px;line-height:18px;color:#64748b;text-align:center;\">"
                + "Gestor de Citas Médicas · Mensaje automático, no respondas a este correo</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String textoEstado(Estado estado) {
        String texto = "Agendada";
        String color = "#047857";
        if (estado == Estado.CANCELADA) {
            texto = "Cancelada";
            color = "#be123c";
        } else if (estado == Estado.ATENDIDA) {
            texto = "Atendida";
            color = "#0369a1";
        }
        return "<div style=\"font-size:15px;line-height:24px;font-weight:700;color:" + color + ";\">" + texto + "</div>";
    }

    /** Parte de la fecha en español de México; "-" si no hay fecha. */
    private String parte(Date fecha, String patron) {
        return fecha == null ? "-" : escaparHtml(new SimpleDateFormat(patron, new Locale("es", "MX")).format(fecha));
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

}
