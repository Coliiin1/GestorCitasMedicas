package com.gestor.gestordecitasmedicas.modelo;

import javax.swing.*;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Properties;

public class BaseDatos {
    private static Connection conexion = null;
    private static String urlActual = null;
    private static final int VERSION_ESQUEMA = 2;

    private static Properties cargarConfig() {
        Properties p = new Properties();
        java.nio.file.Path archivo = java.nio.file.Paths.get("db.properties");

        try {
            if (java.nio.file.Files.exists(archivo)) {
                // Lee desde la raíz del proyecto (donde lo tienes ahora)
                try (InputStream in = java.nio.file.Files.newInputStream(archivo)) {
                    p.load(in);
                }
            } else {
                // Plan de respaldo: busca en src/main/resources
                try (InputStream in = BaseDatos.class.getResourceAsStream("/db.properties")) {
                    if (in != null) p.load(in);
                }
            }
        } catch (java.io.IOException e) {
            System.err.println("No se pudo leer db.properties: " + e.getMessage());
        }
        return p;
    }

    public static Connection getConexion() {
        try {
            String dbUrl = System.getProperty("gestor.db.url");
            Properties connectionProps = new Properties();

            if (dbUrl == null) {
                Properties config = cargarConfig();
                dbUrl = config.getProperty("db.url");
                String password = config.getProperty("db.password");

                if (dbUrl == null || password == null || password.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "Falta el archivo de configuración db.properties o la contraseña.\nEl sistema se cerrará por seguridad.",
                            "Error Crítico de Seguridad",
                            JOptionPane.ERROR_MESSAGE);

                    System.exit(1);
                }
                connectionProps.setProperty("password", password);
            }

            boolean mismaConexion = conexion != null && !conexion.isClosed()
                    && dbUrl.equals(urlActual);
            if (mismaConexion) {
                return conexion;
            }

            if (conexion != null && !conexion.isClosed()) {
                cerrarConexion();
            }

            Class.forName("org.sqlite.JDBC");

            conexion = DriverManager.getConnection(dbUrl, connectionProps);
            urlActual = dbUrl;
            System.out.println("Conexión segura a la base de datos SQLite establecida con éxito.");

            try (Statement stmt = conexion.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            inicializarBaseDatos();
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error de conexión a la base de datos: " + e.getMessage());
            cerrarConexion();
            conexion = null;
            urlActual = null;
        }
        return conexion;
    }

    private static void inicializarBaseDatos() throws SQLException {
        String[] sentencias = {
                "CREATE TABLE IF NOT EXISTS Administrador (" +
                        "id_administrador VARCHAR(10) PRIMARY KEY NOT NULL CHECK (length(id_administrador) = 10), " +
                        "nombre VARCHAR(20) NOT NULL, " +
                        "apellido_paterno VARCHAR(20) NOT NULL, " +
                        "apellido_materno VARCHAR(20), " +
                        "contraseña VARCHAR(255) NOT NULL, " +
                        "cedula VARCHAR(8), " +
                        "institución VARCHAR(20), " +
                        "domicilio VARCHAR(100), " +
                        "profesion TEXT, " +
                        "especialidad TEXT, " +
                        "registro_especialidad TEXT, " +
                        "nombre_establecimiento TEXT, " +
                        "domicilio_establecimiento TEXT)",
                "CREATE TABLE IF NOT EXISTS Asistente (" +
                        "id_asistente VARCHAR(10) PRIMARY KEY NOT NULL CHECK (length(id_asistente) = 10), " +
                        "nombre VARCHAR(20) NOT NULL, " +
                        "apellido_paterno VARCHAR(20) NOT NULL, " +
                        "apellido_materno VARCHAR(20), " +
                        "contraseña VARCHAR(255) NOT NULL)",
                // El login busca en ambas tablas: un id de acceso no puede existir en las dos.
                "CREATE TRIGGER IF NOT EXISTS id_acceso_unico_asistente BEFORE INSERT ON Asistente " +
                        "WHEN EXISTS (SELECT 1 FROM Administrador WHERE id_administrador = NEW.id_asistente) " +
                        "BEGIN SELECT RAISE(ABORT, 'id de acceso en uso'); END",
                "CREATE TRIGGER IF NOT EXISTS id_acceso_unico_administrador BEFORE INSERT ON Administrador " +
                        "WHEN EXISTS (SELECT 1 FROM Asistente WHERE id_asistente = NEW.id_administrador) " +
                        "BEGIN SELECT RAISE(ABORT, 'id de acceso en uso'); END",
                "CREATE TABLE IF NOT EXISTS Paciente (" +
                        "CURP VARCHAR(18) PRIMARY KEY NOT NULL, " +
                        "nombre VARCHAR(20) NOT NULL, " +
                        "apellido_paterno VARCHAR(20) NOT NULL, " +
                        "apellido_materno VARCHAR(20), " +
                        "fecha_nacimiento DATE, " +
                        "sexo VARCHAR(20), " +
                        "domicilio VARCHAR(100), " +
                        "telefono VARCHAR(10), " +
                        "correo VARCHAR(50))",
                "CREATE TABLE IF NOT EXISTS Expediente (" +
                        "id_expediente INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "CURP VARCHAR(18) NOT NULL UNIQUE, " +
                        "fecha_apertura DATE, " +
                        "antecedentes_familiares TEXT, " +
                        "antecedentes_personales TEXT, " +
                        "alergias TEXT, " +
                        "observaciones TEXT, " +
                        "tipo_sangre TEXT, " +
                        "tipo_establecimiento TEXT, " +
                        "nombre_establecimiento TEXT, " +
                        "domicilio_establecimiento TEXT, " +
                        "institucion_establecimiento TEXT, " +
                        "propietario TEXT, " +
                        "grupo_etnico TEXT, " +
                        "antecedentes_no_patologicos TEXT, " +
                        "consumo_sustancias TEXT, " +
                        "padecimiento_actual TEXT, " +
                        "interrogatorio_sistemas TEXT, " +
                        "exploracion_fisica TEXT, " +
                        "resultados_estudios TEXT, " +
                        "diagnostico TEXT, " +
                        "pronostico TEXT, " +
                        "indicacion_terapeutica TEXT, " +
                        "autor_historia TEXT, " +
                        "cedula_historia TEXT, " +
                        "fecha_hora_historia TEXT, " +
                        "signos_historia TEXT, " +
                        "FOREIGN KEY (CURP) REFERENCES Paciente(CURP) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Cita (" +
                        "id_cita INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "CURP VARCHAR(18) NOT NULL, " +
                        "fecha TIMESTAMP NOT NULL, " +
                        "estado VARCHAR(10) NOT NULL CHECK (estado IN ('AGENDADA', 'ATENDIDA', 'CANCELADA')), " +
                        "FOREIGN KEY (CURP) REFERENCES Paciente(CURP) ON DELETE CASCADE)",
                // Diagnóstico, indicaciones y datos de médico/paciente viven en la Receta de la misma cita.
                "CREATE TABLE IF NOT EXISTS Consulta (" +
                        "id_consulta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_cita INTEGER NOT NULL UNIQUE, " +
                        "fecha TIMESTAMP NOT NULL, " +
                        "padecimiento TEXT, " +
                        "motivo TEXT, " +
                        "observaciones TEXT, " +
                        "evolucion TEXT, " +
                        "exploracion_fisica TEXT, " +
                        "resultados_estudios TEXT, " +
                        "pronostico TEXT, " +
                        "FOREIGN KEY (id_cita) REFERENCES Cita(id_cita) ON DELETE CASCADE)",
                // Los datos de médico, consultorio y paciente son la fotografía del momento de la atención.
                "CREATE TABLE IF NOT EXISTS Receta (" +
                        "id_receta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_administrador VARCHAR(10) NOT NULL, " +
                        "id_cita INTEGER NOT NULL UNIQUE, " +
                        "fecha TIMESTAMP NOT NULL, " +
                        "diagnostico TEXT, " +
                        "indicaciones TEXT, " +
                        "nombre_medico TEXT, " +
                        "cedula_medico TEXT, " +
                        "profesion TEXT, " +
                        "institucion_titulo TEXT, " +
                        "domicilio_medico TEXT, " +
                        "especialidad TEXT, " +
                        "registro_especialidad TEXT, " +
                        "nombre_establecimiento TEXT, " +
                        "domicilio_establecimiento TEXT, " +
                        "nombre_paciente TEXT, " +
                        "edad_paciente TEXT, " +
                        "sexo_paciente TEXT, " +
                        "FOREIGN KEY (id_administrador) REFERENCES Administrador(id_administrador) ON DELETE RESTRICT, " +
                        "FOREIGN KEY (id_cita) REFERENCES Cita(id_cita) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Detalle_receta (" +
                        "id_detalle_receta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_receta INTEGER NOT NULL, " +
                        "nombre_medicamento TEXT NOT NULL, " +
                        "concentracion VARCHAR(20), " +
                        "dosis VARCHAR(20), " +
                        "frecuencia VARCHAR(20), " +
                        "duracion VARCHAR(20), " +
                        "presentacion TEXT, " +
                        "via_administracion TEXT, " +
                        "denominacion_distintiva TEXT, " +
                        "cantidad TEXT, " +
                        "grupo_regulatorio TEXT, " +
                        "FOREIGN KEY (id_receta) REFERENCES Receta(id_receta) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Signos_vitales (" +
                        "id_signos_vitales INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_receta INTEGER NOT NULL UNIQUE, " +
                        "presion_arterial VARCHAR(7), " +
                        "frecuencia_cardiaca INTEGER, " +
                        "frecuencia_respiratoria INTEGER, " +
                        "temperatura_corporal REAL, " +
                        "peso REAL, " +
                        "talla INTEGER, " +
                        "saturacion_oxigeno INTEGER, " +
                        "FOREIGN KEY (id_receta) REFERENCES Receta(id_receta) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Horario (" +
                        "id_horario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "dia INTEGER NOT NULL, " +
                        "hora_inicio TIME NOT NULL, " +
                        "hora_fin TIME NOT NULL, " +
                        "estado BOOLEAN NOT NULL, " +
                        "UNIQUE (dia, hora_inicio, hora_fin), " +
                        "CHECK (hora_inicio < hora_fin))",
                "CREATE TABLE IF NOT EXISTS Horario_bloqueado (" +
                        "id_bloqueo INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "fecha_inicio TIMESTAMP NOT NULL, " +
                        "fecha_fin TIMESTAMP NOT NULL, " +
                        "CHECK (fecha_inicio < fecha_fin))",
                "CREATE TABLE IF NOT EXISTS Configuracion (clave TEXT PRIMARY KEY, valor TEXT)",
                // Un turno solo admite una cita vigente (misma regla que Cita.ocupadaEn).
                "CREATE UNIQUE INDEX IF NOT EXISTS ux_cita_turno ON Cita(fecha) WHERE estado <> 'CANCELADA'",
                "CREATE INDEX IF NOT EXISTS idx_cita_curp ON Cita(CURP)",
                "CREATE INDEX IF NOT EXISTS idx_detalle_receta ON Detalle_receta(id_receta)",
                "CREATE INDEX IF NOT EXISTS idx_horario_dia_estado ON Horario(dia, estado)",
                "CREATE INDEX IF NOT EXISTS idx_bloqueo_rango ON Horario_bloqueado(fecha_inicio, fecha_fin)"
        };

        conexion.setAutoCommit(false);
        try (Statement stmt = conexion.createStatement()) {
            exigirEsquemaActual(stmt);
            for (String sentencia : sentencias) {
                stmt.execute(sentencia);
            }
            stmt.execute("PRAGMA user_version = " + VERSION_ESQUEMA);
            MigracionClinica.aplicar(conexion);
            MigracionAgenda.aplicar(conexion);
            insertarAdministradorPorDefecto(stmt);
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }

    /** Una base creada con otro esquema no se migra: se rechaza para no mezclar estructuras. */
    private static void exigirEsquemaActual(Statement stmt) throws SQLException {
        int version;
        int tablas;
        try (ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
            version = rs.getInt(1);
        }
        try (ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'")) {
            tablas = rs.getInt(1);
        }
        if (tablas > 0 && version != VERSION_ESQUEMA) {
            throw new SQLException("la base es de una versión anterior del sistema; "
                    + "borra el archivo .db para que se regenere.");
        }
    }

    private static void insertarAdministradorPorDefecto(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Administrador")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO Administrador (id_administrador, " +
                        "nombre, apellido_paterno, apellido_materno, \"contraseña\", cedula, \"institución\", domicilio) " +
                        "VALUES ('" + Personal.generarUsuario("Administrador", "Sistema", "", new java.util.HashSet<>())
                        + "', 'Administrador', 'Sistema', '', 'admin', '00000000', 'Sistema', '')");
            }
        }
    }

    /** Ids de acceso ya usados por administradores y asistentes. */
    private static java.util.Set<String> idsDeAcceso(Connection con) throws SQLException {
        java.util.Set<String> ids = new java.util.HashSet<>();
        try (Statement s = con.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id_asistente FROM Asistente UNION SELECT id_administrador FROM Administrador")) {
            while (rs.next()) {
                ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    public static void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                System.out.println("Conexión cerrada de forma segura.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar la conexión: " + e.getMessage());
        }
    }

    public boolean insertarAsistente(Asistente asistente) {
        if (asistente == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "INSERT INTO Asistente (id_asistente, nombre, apellido_paterno, apellido_materno, \"contraseña\") " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            String id = Personal.generarUsuario(asistente.getNombre(), asistente.getApellidoPaterno(),
                    asistente.getApellidoMaterno(), idsDeAcceso(con));
            if (id == null) {
                return false;
            }
            ps.setString(1, id);
            ps.setString(2, valor(asistente.getNombre()));
            ps.setString(3, valor(asistente.getApellidoPaterno()));
            ps.setString(4, asistente.getApellidoMaterno());
            ps.setString(5, valor(asistente.getContrasena()));

            if (ps.executeUpdate() > 0) {
                asistente.setIdAsistente(id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar asistente: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarAsistente(String idAsistente) {
        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Asistente WHERE id_asistente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idAsistente);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar asistente: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarAsistente(Asistente asistente) {
        if (asistente == null || asistente.getIdAsistente() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "UPDATE Asistente SET nombre = ?, apellido_paterno = ?, apellido_materno = ?, " +
                "\"contraseña\" = ? WHERE id_asistente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor(asistente.getNombre()));
            ps.setString(2, valor(asistente.getApellidoPaterno()));
            ps.setString(3, asistente.getApellidoMaterno());
            ps.setString(4, valor(asistente.getContrasena()));
            ps.setString(5, asistente.getIdAsistente());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar asistente: " + e.getMessage());
        }
        return false;
    }

    /** {@code id} null devuelve el primer administrador (el consultorio es de un solo médico). */
    public Administrador obtenerAdministrador(String id) {
        Connection con = getConexion();
        if (con == null) return null;
        String sql = "SELECT nombre, apellido_paterno, apellido_materno, cedula, \"institución\", domicilio, " +
                "profesion, especialidad, registro_especialidad, nombre_establecimiento, domicilio_establecimiento " +
                "FROM Administrador WHERE id_administrador = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (id == null) id = obtenerIdAdministradorPorDefecto(con);
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Administrador a = new Administrador();
                a.setIdAdministrador(id);
                a.setNombre(rs.getString(1));
                a.setApellidoPaterno(rs.getString(2));
                a.setApellidoMaterno(rs.getString(3));
                a.setCedulaProfesional(rs.getString(4));
                a.setInstitucionAcademica(rs.getString(5));
                a.setDomicilio(rs.getString(6));
                a.setProfesion(rs.getString(7));
                a.setEspecialidad(rs.getString(8));
                a.setRegistroEspecialidad(rs.getString(9));
                a.setNombreEstablecimiento(rs.getString(10));
                a.setDomicilioEstablecimiento(rs.getString(11));
                return a;
            }
        } catch (SQLException e) {
            System.err.println("Error al leer administrador: " + e.getMessage());
        }
        return null;
    }

    public boolean actualizarAdministrador(Administrador a) {
        Connection con = getConexion();
        if (con == null || a == null) return false;
        String sql = "UPDATE Administrador SET nombre = ?, apellido_paterno = ?, apellido_materno = ?, cedula = ?, " +
                "\"institución\" = ?, domicilio = ?, profesion = ?, especialidad = ?, registro_especialidad = ?, " +
                "nombre_establecimiento = ?, domicilio_establecimiento = ? WHERE id_administrador = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor(a.getNombre()));
            ps.setString(2, valor(a.getApellidoPaterno()));
            ps.setString(3, a.getApellidoMaterno());
            ps.setString(4, a.getCedulaProfesional());
            ps.setString(5, a.getInstitucionAcademica());
            ps.setString(6, a.getDomicilio());
            ps.setString(7, a.getProfesion());
            ps.setString(8, a.getEspecialidad());
            ps.setString(9, a.getRegistroEspecialidad());
            ps.setString(10, a.getNombreEstablecimiento());
            ps.setString(11, a.getDomicilioEstablecimiento());
            ps.setString(12, a.getIdAdministrador());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar administrador: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarContrasenaAdmin(String id, String nuevaContrasena) {
        Connection con = getConexion();
        if (con == null || nuevaContrasena == null || nuevaContrasena.isEmpty()) return false;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE Administrador SET \"contraseña\" = ? WHERE id_administrador = ?")) {
            ps.setString(1, nuevaContrasena);
            ps.setString(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar la contraseña: " + e.getMessage());
        }
        return false;
    }

    public String obtenerConfig(String clave, String porDefecto) {
        Connection con = getConexion();
        if (con == null) return porDefecto;
        try (PreparedStatement ps = con.prepareStatement("SELECT valor FROM Configuracion WHERE clave = ?")) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getString(1) != null) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("Error al leer configuración: " + e.getMessage());
        }
        return porDefecto;
    }

    public boolean guardarConfig(String clave, String valor) {
        Connection con = getConexion();
        if (con == null) return false;
        try (PreparedStatement ps = con.prepareStatement("INSERT OR REPLACE INTO Configuracion (clave, valor) VALUES (?, ?)")) {
            ps.setString(1, clave);
            ps.setString(2, valor);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al guardar configuración: " + e.getMessage());
        }
        return false;
    }

    /** Devuelve "admin", "asis" o null si las credenciales no coinciden. */
    public String autenticar(String id, String contrasena) {
        Connection con = getConexion();
        if (con == null || id == null || contrasena == null || contrasena.isEmpty()) {
            return null;
        }
        String[][] tablas = {
                {"Administrador", "id_administrador", "admin"},
                {"Asistente", "id_asistente", "asis"}};
        for (String[] t : tablas) {
            String sql = "SELECT 1 FROM " + t[0] + " WHERE " + t[1] + " = ? AND \"contraseña\" = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, id);
                ps.setString(2, contrasena);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return t[2];
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error al autenticar: " + e.getMessage());
            }
        }
        return null;
    }

    public ArrayList<Asistente> cargarAsistentes() {
        ArrayList<Asistente> asistentes = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            return asistentes;
        }

        String sql = "SELECT id_asistente, nombre, apellido_paterno, apellido_materno, \"contraseña\" " +
                "FROM Asistente ORDER BY id_asistente";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Asistente asistente = new Asistente(rs.getString("id_asistente"));
                asistente.setNombre(rs.getString("nombre"));
                asistente.setApellidoPaterno(rs.getString("apellido_paterno"));
                asistente.setApellidoMaterno(rs.getString("apellido_materno"));
                asistente.setContrasena(rs.getString("contraseña"));
                asistentes.add(asistente);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar asistentes: " + e.getMessage());
        }
        return asistentes;
    }

    public boolean insertarPaciente(Paciente paciente) {
        if (paciente == null || paciente.getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "INSERT INTO Paciente " +
                "(CURP, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, sexo, domicilio, telefono, correo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, paciente.getCurp());
            ps.setString(2, valor(paciente.getNombre()));
            ps.setString(3, valor(paciente.getApellidoPaterno()));
            ps.setString(4, paciente.getApellidoMaterno());
            ps.setDate(5, toSqlDate(paciente.getFechaNacimiento()));
            ps.setString(6, paciente.getSexo());
            ps.setString(7, paciente.getDomicilio());
            ps.setString(8, paciente.getTelefono());
            ps.setString(9, paciente.getCorreo());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar paciente: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarPaciente(String idPaciente) {
        if (idPaciente == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Paciente WHERE CURP = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idPaciente);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar paciente: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarPaciente(Paciente paciente) {
        if (paciente == null || paciente.getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "UPDATE Paciente SET nombre = ?, apellido_paterno = ?, apellido_materno = ?, " +
                "fecha_nacimiento = ?, sexo = ?, domicilio = ?, telefono = ?, correo = ? WHERE CURP = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor(paciente.getNombre()));
            ps.setString(2, valor(paciente.getApellidoPaterno()));
            ps.setString(3, paciente.getApellidoMaterno());
            ps.setDate(4, toSqlDate(paciente.getFechaNacimiento()));
            ps.setString(5, paciente.getSexo());
            ps.setString(6, paciente.getDomicilio());
            ps.setString(7, paciente.getTelefono());
            ps.setString(8, paciente.getCorreo());
            ps.setString(9, paciente.getCurp());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar paciente: " + e.getMessage());
        }
        return false;
    }

    public HashMap<String, Paciente> cargarPacientes() {
        try {
            return cargarPacientesFiltrados(null);
        } catch (IllegalStateException e) {
            System.err.println(e.getMessage());
            return new HashMap<>();
        }
    }

    public Paciente cargarPacientePorCurp(String curp) {
        if (curp == null || curp.trim().isEmpty()) return null;
        return cargarPacientesFiltrados(curp).get(curp);
    }

    private HashMap<String, Paciente> cargarPacientesFiltrados(String curp) {
        HashMap<String, Paciente> pacientes = new HashMap<>();
        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar los pacientes.");
        }

        String sql = "SELECT CURP, nombre, apellido_paterno, apellido_materno, fecha_nacimiento, sexo, " +
                "domicilio, telefono, correo FROM Paciente " + (curp == null ? "" : "WHERE CURP = ? ") +
                "ORDER BY apellido_paterno, apellido_materno, nombre";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (curp != null) ps.setString(1, curp);
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Paciente paciente = new Paciente(
                        rs.getString("CURP"),
                        toUtilDate(rs.getDate("fecha_nacimiento")),
                        rs.getString("sexo"),
                        rs.getString("domicilio"),
                        rs.getString("telefono"),
                        rs.getString("correo"));
                paciente.setNombre(rs.getString("nombre"));
                paciente.setApellidoPaterno(rs.getString("apellido_paterno"));
                paciente.setApellidoMaterno(rs.getString("apellido_materno"));
                pacientes.put(paciente.getCurp(), paciente);
            }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar los pacientes.", e);
        }
        return pacientes;
    }

    public boolean insertarCita(Cita cita) {
        if (cita == null || cita.getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "INSERT INTO Cita (CURP, fecha, estado) VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cita.getCurp());
            ps.setTimestamp(2, toSqlTimestamp(cita.getFechaCita()));
            ps.setString(3, estado(cita.getEstado()).name());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        cita.setIdCita(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar cita: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarCita(int idCita) {
        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Cita WHERE id_cita = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar cita: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarCita(Cita cita) {
        if (cita == null || cita.getIdCita() <= 0 || cita.getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "UPDATE Cita SET CURP = ?, fecha = ?, estado = ? WHERE id_cita = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cita.getCurp());
            ps.setTimestamp(2, toSqlTimestamp(cita.getFechaCita()));
            ps.setString(3, estado(cita.getEstado()).name());
            ps.setInt(4, cita.getIdCita());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar cita: " + e.getMessage());
        }
        return false;
    }

    public HashMap<Integer, Cita> cargarCitas() {
        HashMap<Integer, Cita> citas = new HashMap<>();
        Connection con = getConexion();
        if (con == null) {
            return citas;
        }

        String sql = "SELECT id_cita, CURP, fecha, estado FROM Cita ORDER BY fecha, id_cita";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cita cita = crearCita(rs);
                citas.put(cita.getIdCita(), cita);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar citas: " + e.getMessage());
        }
        return citas;
    }

    public ArrayList<Cita> cargarCitasEntre(LocalDate inicio, LocalDate fin) {
        ArrayList<Cita> resultado = new ArrayList<>();
        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            return resultado;
        }
        for (Cita cita : cargarCitas().values()) {
            if (cita.getFechaCita() == null) {
                continue;
            }
            LocalDate dia = cita.getFechaCita().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (!dia.isBefore(inicio) && !dia.isAfter(fin)) {
                resultado.add(cita);
            }
        }
        resultado.sort(Comparator.comparing(Cita::getFechaCita, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingInt(Cita::getIdCita));
        return resultado;
    }

    public ArrayList<Horario> cargarHorarios() {
        ArrayList<Horario> horarios = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            return horarios;
        }

        try {
            for (Horario horario : buscarHorarios(con)) {
                if (horario.getHoraInicio() != null && horario.getHoraFin() != null) {
                    horarios.add(horario);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar horarios: " + e.getMessage());
        }
        return horarios;
    }

    public ArrayList<Horario> cargarHorariosActivos() {
        ArrayList<Horario> activos = new ArrayList<>();
        for (Horario horario : cargarHorarios()) {
            if (horario.isEstado()) {
                activos.add(horario);
            }
        }
        return activos;
    }

    public boolean guardarHorario(Horario horario) {
        if (!horarioValido(horario)) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);

            if (existeTramoSuperpuesto(con, horario)) {
                con.rollback();
                return false;
            }

            boolean guardado;
            if (horario.getIdHorario() > 0) {
                // Las citas que el tramo editado (o desactivado) deja fuera se cancelan.
                Horario anterior = buscarHorario(con, horario.getIdHorario());
                if (anterior != null) {
                    for (Cita cita : citasActivasEnHorario(anterior)) {
                        if ((!horario.isEstado() || !horario.cubre(cita.fechaHora()))
                                && !actualizarEstadoCita(con, cita.getIdCita(), Estado.CANCELADA)) {
                            con.rollback();
                            return false;
                        }
                    }
                }
                String sql = "UPDATE Horario SET dia = ?, hora_inicio = ?, hora_fin = ?, estado = ? " +
                        "WHERE id_horario = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, horario.getDia().getNumero());
                    ps.setString(2, horaTexto(horario.getHoraInicio()));
                    ps.setString(3, horaTexto(horario.getHoraFin()));
                    ps.setInt(4, horario.isEstado() ? 1 : 0);
                    ps.setInt(5, horario.getIdHorario());
                    guardado = ps.executeUpdate() > 0;
                }
            } else {
                String sql = "INSERT INTO Horario (dia, hora_inicio, hora_fin, estado) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, horario.getDia().getNumero());
                    ps.setString(2, horaTexto(horario.getHoraInicio()));
                    ps.setString(3, horaTexto(horario.getHoraFin()));
                    ps.setInt(4, horario.isEstado() ? 1 : 0);
                    guardado = ps.executeUpdate() > 0;
                    if (guardado) {
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                horario.setIdHorario(rs.getInt(1));
                            }
                        }
                    }
                }
            }

            if (!guardado) {
                con.rollback();
                return false;
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            revertir(con, "guardar horario", e);
        } finally {
            restaurarAutoCommit(con);
        }
        return false;
    }

    public boolean eliminarHorario(int idHorario) {
        if (idHorario <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);
            Horario horario = buscarHorario(con, idHorario);
            if (horario == null) {
                con.rollback();
                return false;
            }
            // Sin el tramo las citas quedan fuera de horario: se cancelan en la misma transacción.
            for (Cita cita : citasActivasEnHorario(horario)) {
                if (!actualizarEstadoCita(con, cita.getIdCita(), Estado.CANCELADA)) {
                    con.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = con.prepareStatement("DELETE FROM Horario WHERE id_horario = ?")) {
                ps.setInt(1, idHorario);
                boolean eliminado = ps.executeUpdate() > 0;
                if (!eliminado) {
                    con.rollback();
                    return false;
                }
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            revertir(con, "eliminar horario", e);
        } finally {
            restaurarAutoCommit(con);
        }
        return false;
    }

    public ArrayList<HorarioBloqueado> cargarBloqueos() {
        ArrayList<HorarioBloqueado> bloqueos = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            return bloqueos;
        }

        String sql = "SELECT id_bloqueo, fecha_inicio, fecha_fin FROM Horario_bloqueado " +
                "ORDER BY fecha_inicio, id_bloqueo";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                HorarioBloqueado bloqueo = new HorarioBloqueado();
                bloqueo.setIdHorarioBloqueado(rs.getInt("id_bloqueo"));
                bloqueo.setFechaInicio(leerLocalDateTime(rs.getObject("fecha_inicio")));
                bloqueo.setFechaFin(leerLocalDateTime(rs.getObject("fecha_fin")));
                if (bloqueo.getFechaInicio() != null && bloqueo.getFechaFin() != null) {
                    bloqueos.add(bloqueo);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar bloqueos: " + e.getMessage());
        }
        return bloqueos;
    }

    public ArrayList<HorarioBloqueado> cargarBloqueosEntre(LocalDateTime inicio, LocalDateTime fin) {
        ArrayList<HorarioBloqueado> resultado = new ArrayList<>();
        for (HorarioBloqueado bloqueo : cargarBloqueos()) {
            if (PlanificadorAgenda.seSolapan(inicio, fin,
                    bloqueo.getFechaInicio(), bloqueo.getFechaFin())) {
                resultado.add(bloqueo);
            }
        }
        return resultado;
    }

    public boolean guardarBloqueo(HorarioBloqueado bloqueo) {
        return guardarBloqueos(java.util.Collections.singletonList(bloqueo));
    }

    /** Guarda todos los bloqueos en una sola transacción: si uno es inválido o se cruza, no se guarda ninguno. */
    public boolean guardarBloqueos(java.util.List<HorarioBloqueado> lista) {
        if (lista == null || lista.isEmpty()) {
            return false;
        }
        for (HorarioBloqueado bloqueo : lista) {
            if (bloqueo == null || bloqueo.getFechaInicio() == null || bloqueo.getFechaFin() == null
                    || !bloqueo.getFechaInicio().isBefore(bloqueo.getFechaFin())) {
                return false;
            }
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);
            for (HorarioBloqueado bloqueo : lista) {
                if (!guardarBloqueo(con, bloqueo)) {
                    con.rollback();
                    return false;
                }
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            revertir(con, "guardar bloqueo", e);
        } finally {
            restaurarAutoCommit(con);
        }
        return false;
    }

    private boolean guardarBloqueo(Connection con, HorarioBloqueado bloqueo) throws SQLException {
        if (existeBloqueoSuperpuesto(con, bloqueo)) {
            return false;
        }

        // El bloqueo cancela las citas agendadas del intervalo en la misma transacción.
        for (Cita cita : citasActivasEnRango(con, bloqueo.getFechaInicio(), bloqueo.getFechaFin())) {
            if (!actualizarEstadoCita(con, cita.getIdCita(), Estado.CANCELADA)) {
                return false;
            }
        }

        if (bloqueo.getIdHorarioBloqueado() > 0) {
            String sql = "UPDATE Horario_bloqueado SET fecha_inicio = ?, fecha_fin = ? " +
                    "WHERE id_bloqueo = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setTimestamp(1, toSqlTimestamp(bloqueo.getFechaInicio()));
                ps.setTimestamp(2, toSqlTimestamp(bloqueo.getFechaFin()));
                ps.setInt(3, bloqueo.getIdHorarioBloqueado());
                return ps.executeUpdate() > 0;
            }
        }
        String sql = "INSERT INTO Horario_bloqueado (fecha_inicio, fecha_fin) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, toSqlTimestamp(bloqueo.getFechaInicio()));
            ps.setTimestamp(2, toSqlTimestamp(bloqueo.getFechaFin()));
            if (ps.executeUpdate() == 0) {
                return false;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    bloqueo.setIdHorarioBloqueado(rs.getInt(1));
                }
            }
            return true;
        }
    }

    public boolean eliminarBloqueo(int idBloqueo) {
        if (idBloqueo <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM Horario_bloqueado WHERE id_bloqueo = ?")) {
            ps.setInt(1, idBloqueo);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar bloqueo: " + e.getMessage());
        }
        return false;
    }

    public ArrayList<Cita> citasActivasEnRango(LocalDateTime inicio, LocalDateTime fin) {
        Connection con = getConexion();
        if (con == null) {
            return new ArrayList<>();
        }
        return citasActivasEnRango(con, inicio, fin);
    }

    public ArrayList<Cita> citasActivasEnHorario(Horario horario) {
        ArrayList<Cita> resultado = new ArrayList<>();
        if (horario == null || horario.getDia() == null || horario.getHoraInicio() == null
                || horario.getHoraFin() == null) {
            return resultado;
        }

        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        for (Cita cita : cargarCitas().values()) {
            if (cita.getFechaCita() == null || cita.getEstado() != Estado.AGENDADA) {
                continue;
            }
            LocalDateTime inicioCita = LocalDateTime.ofInstant(
                    cita.getFechaCita().toInstant(), ZoneId.systemDefault());
            if (inicioCita.isBefore(hoy)) {
                continue;
            }
            DiaSemana dia = DiaSemana.desdeDayOfWeek(inicioCita.getDayOfWeek());
            if (dia != horario.getDia()) {
                continue;
            }
            LocalDateTime inicioTramo = inicioCita.toLocalDate().atTime(horario.getHoraInicio());
            LocalDateTime finTramo = inicioCita.toLocalDate().atTime(horario.getHoraFin());
            if (PlanificadorAgenda.seSolapan(inicioCita,
                    inicioCita.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS),
                    inicioTramo, finTramo)) {
                resultado.add(cita);
            }
        }
        return resultado;
    }

    private ArrayList<Cita> citasActivasEnRango(Connection con, LocalDateTime inicio, LocalDateTime fin) {
        ArrayList<Cita> resultado = new ArrayList<>();
        if (inicio == null || fin == null) {
            return resultado;
        }

        LocalDateTime hoy = LocalDate.now().atStartOfDay();
        String sql = "SELECT id_cita, CURP, fecha, estado FROM Cita";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cita cita = crearCita(rs);
                // Solo AGENDADA: las ATENDIDA son historial y las CANCELADA ya no ocupan.
                if (cita.getFechaCita() == null || cita.getEstado() != Estado.AGENDADA) {
                    continue;
                }
                LocalDateTime inicioCita = LocalDateTime.ofInstant(
                        cita.getFechaCita().toInstant(), ZoneId.systemDefault());
                LocalDateTime finCita = inicioCita.plusMinutes(PlanificadorAgenda.DURACION_CITA_MINUTOS);
                // Las citas de días anteriores son historial; las de hoy sí se cancelan aunque ya haya pasado su hora.
                if (inicioCita.isBefore(hoy)) {
                    continue;
                }
                if (PlanificadorAgenda.seSolapan(inicioCita, finCita, inicio, fin)) {
                    resultado.add(cita);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar citas del rango: " + e.getMessage());
        }
        return resultado;
    }

    private static boolean horarioValido(Horario horario) {
        return horario != null && horario.getDia() != null
                && horario.getHoraInicio() != null && horario.getHoraFin() != null
                && horario.getHoraInicio().isBefore(horario.getHoraFin());
    }

    private boolean existeTramoSuperpuesto(Connection con, Horario nuevo) throws SQLException {
        for (Horario existente : buscarHorarios(con)) {
            if (existente.getIdHorario() != nuevo.getIdHorario() && existente.isEstado()
                    && PlanificadorAgenda.tramoSeSolapa(existente, nuevo)) {
                return true;
            }
        }
        return false;
    }

    private boolean existeBloqueoSuperpuesto(Connection con, HorarioBloqueado nuevo) throws SQLException {
        String sql = "SELECT id_bloqueo, fecha_inicio, fecha_fin FROM Horario_bloqueado " +
                "WHERE id_bloqueo <> ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevo.getIdHorarioBloqueado());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDateTime inicio = leerLocalDateTime(rs.getObject("fecha_inicio"));
                    LocalDateTime fin = leerLocalDateTime(rs.getObject("fecha_fin"));
                    if (PlanificadorAgenda.seSolapan(nuevo.getFechaInicio(), nuevo.getFechaFin(),
                            inicio, fin)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Horario buscarHorario(Connection con, int idHorario) throws SQLException {
        for (Horario horario : buscarHorarios(con)) {
            if (horario.getIdHorario() == idHorario) {
                return horario;
            }
        }
        return null;
    }

    private static Cita crearCita(ResultSet rs) throws SQLException {
        Cita cita = new Cita();
        cita.setIdCita(rs.getInt("id_cita"));
        cita.setCurp(rs.getString("CURP"));
        cita.setFechaCita(leerFechaHora(rs.getObject("fecha")));
        cita.setEstado(parseEstado(rs.getString("estado")));
        return cita;
    }

    /** Horarios con día válido, ordenados por día y hora de inicio. */
    private ArrayList<Horario> buscarHorarios(Connection con) throws SQLException {
        ArrayList<Horario> horarios = new ArrayList<>();
        String sql = "SELECT id_horario, dia, hora_inicio, hora_fin, estado FROM Horario " +
                "ORDER BY dia, hora_inicio";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Horario horario = new Horario();
                horario.setIdHorario(rs.getInt("id_horario"));
                horario.setDia(DiaSemana.desdeNumero(rs.getInt("dia")));
                horario.setHoraInicio(leerHora(rs.getObject("hora_inicio")));
                horario.setHoraFin(leerHora(rs.getObject("hora_fin")));
                horario.setEstado(leerBooleano(rs.getObject("estado")));
                if (horario.getDia() != null) {
                    horarios.add(horario);
                }
            }
        }
        return horarios;
    }

    public boolean insertarExpediente(Expediente expediente) {
        if (expediente == null || expediente.getDatosPaciente() == null ||
                expediente.getDatosPaciente().getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "INSERT INTO Expediente (CURP, fecha_apertura, antecedentes_familiares, antecedentes_personales, alergias, " +
                "observaciones, tipo_establecimiento, nombre_establecimiento, domicilio_establecimiento, " +
                "institucion_establecimiento, propietario, grupo_etnico, antecedentes_no_patologicos, consumo_sustancias, " +
                "padecimiento_actual, interrogatorio_sistemas, exploracion_fisica, resultados_estudios, diagnostico, " +
                "pronostico, indicacion_terapeutica, autor_historia, cedula_historia, fecha_hora_historia, " +
                "signos_historia, tipo_sangre) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametrosExpediente(ps, expediente, expediente.getFechaApertura() != null
                    ? expediente.getFechaApertura() : new java.util.Date());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        expediente.setIdExpediente(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar expediente: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarExpediente(String idExpediente) {
        int id;
        try {
            id = Integer.parseInt(idExpediente);
        } catch (NumberFormatException e) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Expediente WHERE id_expediente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar expediente: " + e.getMessage());
        }
        return false;
    }

    /** Parámetros 1-26 comunes a INSERT y UPDATE de Expediente. */
    private static void parametrosExpediente(PreparedStatement ps, Expediente expediente, java.util.Date fechaApertura)
            throws SQLException {
        ps.setString(1, expediente.getDatosPaciente().getCurp());
        ps.setDate(2, toSqlDate(fechaApertura));
        ps.setString(3, expediente.getAntecedentesFamiliares());
        ps.setString(4, expediente.getAntecedentesPersonales());
        ps.setString(5, expediente.getAlergias());
        ps.setString(6, expediente.getObservaciones());
        ps.setString(7, expediente.getTipoEstablecimiento());
        ps.setString(8, expediente.getNombreEstablecimiento());
        ps.setString(9, expediente.getDomicilioEstablecimiento());
        ps.setString(10, expediente.getInstitucionEstablecimiento());
        ps.setString(11, expediente.getPropietario());
        ps.setString(12, expediente.getGrupoEtnico());
        ps.setString(13, expediente.getAntecedentesNoPatologicos());
        ps.setString(14, expediente.getConsumoSustancias());
        ps.setString(15, expediente.getPadecimientoActual());
        ps.setString(16, expediente.getInterrogatorioSistemas());
        ps.setString(17, expediente.getExploracionFisica());
        ps.setString(18, expediente.getResultadosEstudios());
        ps.setString(19, expediente.getDiagnostico());
        ps.setString(20, expediente.getPronostico());
        ps.setString(21, expediente.getIndicacionTerapeutica());
        ps.setString(22, expediente.getAutorHistoria());
        ps.setString(23, expediente.getCedulaHistoria());
        ps.setString(24, expediente.getFechaHoraHistoria());
        ps.setString(25, expediente.getSignosHistoria());
        ps.setString(26, expediente.getTipoSangre());
    }

    public boolean actualizarExpediente(Expediente expediente) {
        if (expediente == null || expediente.getIdExpediente() <= 0 ||
                expediente.getDatosPaciente() == null || expediente.getDatosPaciente().getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "UPDATE Expediente SET CURP = ?, fecha_apertura = ?, antecedentes_familiares = ?, antecedentes_personales " +
                "= ?, alergias = ?, observaciones = ?, tipo_establecimiento = ?, nombre_establecimiento = ?, " +
                "domicilio_establecimiento = ?, institucion_establecimiento = ?, propietario = ?, grupo_etnico = ?, " +
                "antecedentes_no_patologicos = ?, consumo_sustancias = ?, padecimiento_actual = ?, interrogatorio_sistemas " +
                "= ?, exploracion_fisica = ?, resultados_estudios = ?, diagnostico = ?, pronostico = ?, " +
                "indicacion_terapeutica = ?, autor_historia = ?, cedula_historia = ?, fecha_hora_historia = ?, " +
                "signos_historia = ?, tipo_sangre = ? WHERE id_expediente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            parametrosExpediente(ps, expediente, expediente.getFechaApertura());
            ps.setInt(27, expediente.getIdExpediente());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar expediente: " + e.getMessage());
        }
        return false;
    }

    public Expediente cargarExpedientePorCurp(String curp) {
        if (curp == null || curp.trim().isEmpty()) {
            return null;
        }

        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar el expediente.");
        }

        String sql = "SELECT * FROM Expediente " +
                "WHERE CURP = ? ORDER BY id_expediente DESC LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, curp);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Expediente expediente = new Expediente();
                    expediente.setIdExpediente(rs.getInt("id_expediente"));
                    expediente.setFechaApertura(toUtilDate(rs.getDate("fecha_apertura")));
                    expediente.setAntecedentesFamiliares(rs.getString("antecedentes_familiares"));
                    expediente.setAntecedentesPersonales(rs.getString("antecedentes_personales"));
                    expediente.setAlergias(rs.getString("alergias"));
                    expediente.setTipoSangre(rs.getString("tipo_sangre"));
                    expediente.setObservaciones(rs.getString("observaciones"));
                    expediente.setTipoEstablecimiento(rs.getString("tipo_establecimiento"));
                    expediente.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                    expediente.setDomicilioEstablecimiento(rs.getString("domicilio_establecimiento"));
                    expediente.setInstitucionEstablecimiento(rs.getString("institucion_establecimiento"));
                    expediente.setPropietario(rs.getString("propietario"));
                    expediente.setGrupoEtnico(rs.getString("grupo_etnico"));
                    expediente.setAntecedentesNoPatologicos(rs.getString("antecedentes_no_patologicos"));
                    expediente.setConsumoSustancias(rs.getString("consumo_sustancias"));
                    expediente.setPadecimientoActual(rs.getString("padecimiento_actual"));
                    expediente.setInterrogatorioSistemas(rs.getString("interrogatorio_sistemas"));
                    expediente.setExploracionFisica(rs.getString("exploracion_fisica"));
                    expediente.setResultadosEstudios(rs.getString("resultados_estudios"));
                    expediente.setDiagnostico(rs.getString("diagnostico"));
                    expediente.setPronostico(rs.getString("pronostico"));
                    expediente.setIndicacionTerapeutica(rs.getString("indicacion_terapeutica"));
                    expediente.setAutorHistoria(rs.getString("autor_historia"));
                    expediente.setCedulaHistoria(rs.getString("cedula_historia"));
                    expediente.setFechaHoraHistoria(rs.getString("fecha_hora_historia"));
                    expediente.setSignosHistoria(rs.getString("signos_historia"));
                    expediente.setDatosPaciente(cargarPacientePorCurp(curp));
                    expediente.setConsultas(cargarConsultasPorCurp(curp));
                    expediente.setRecetas(cargarRecetasPorCurp(curp));
                    return expediente;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar el expediente.", e);
        }
        return null;
    }

    /** Notas de todas las citas del paciente, de la más reciente a la más antigua, con su receta. */
    public ArrayList<Consulta> cargarConsultasPorCurp(String curp) {
        ArrayList<Consulta> consultas = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar las consultas.");
        }
        String sql = "SELECT k.* FROM Consulta k JOIN Cita c ON c.id_cita = k.id_cita "
                + "WHERE c.CURP = ? ORDER BY k.fecha DESC, k.id_consulta DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, curp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consulta consulta = crearConsulta(rs);
                    // Una fecha ilegible no debe verse como historial vacío ni desordenar la lista.
                    if (consulta.getFechaConsulta() == null) {
                        throw new IllegalStateException("Fecha de consulta inválida: " + rs.getObject("fecha"));
                    }
                    consulta.setReceta(cargarRecetaPorCita(consulta.getIdCita()));
                    consultas.add(consulta);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar el historial de consultas.", e);
        }
        consultas.sort(java.util.Comparator.comparing(Consulta::getFechaConsulta,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(java.util.Comparator.comparingInt(Consulta::getIdConsulta).reversed()));
        return consultas;
    }

    public boolean insertarConsulta(Consulta consulta) {
        if (consulta == null || consulta.getIdCita() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            return insertarConsultaInterna(con, consulta);
        } catch (SQLException e) {
            System.err.println("Error al insertar consulta: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarConsulta(Consulta consulta) {
        if (consulta == null || consulta.getIdConsulta() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            return actualizarConsultaInterna(con, consulta);
        } catch (SQLException e) {
            System.err.println("Error al actualizar consulta: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarConsulta(int idConsulta) {
        if (idConsulta <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Consulta WHERE id_consulta = ?")) {
            ps.setInt(1, idConsulta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar consulta: " + e.getMessage());
        }
        return false;
    }

    public Consulta cargarConsultaPorId(int idConsulta) {
        if (idConsulta <= 0) {
            return null;
        }

        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar la consulta.");
        }

        try {
            return conReceta(consultaPorIdInterna(con, idConsulta));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar la consulta.", e);
        }
    }

    public Consulta cargarConsultaPorCita(int idCita) {
        if (idCita <= 0) {
            return null;
        }

        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar la consulta.");
        }

        try {
            return conReceta(consultaPorCitaInterna(con, idCita));
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar la consulta de la cita.", e);
        }
    }

    /** Diagnóstico, indicaciones, signos y datos de médico/paciente de la nota salen de su receta. */
    private Consulta conReceta(Consulta consulta) {
        if (consulta != null) {
            consulta.setReceta(cargarRecetaPorCita(consulta.getIdCita()));
        }
        return consulta;
    }

    /**
     * Guarda receta, medicamentos y signos, registra o actualiza la consulta
     * de la cita y la marca como atendida dentro de una sola transacción.
     */
    public boolean finalizarConsulta(Cita cita, Consulta consulta, Receta receta) {
        return guardarAtencion(cita, consulta, receta, true);
    }

    public boolean guardarAtencion(Cita cita, Consulta consulta, Receta receta, boolean finalizar) {
        if (cita == null || cita.getIdCita() <= 0 || consulta == null || receta == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        int idRecetaAnterior = receta.getIdReceta();
        int idConsultaAnterior = consulta.getIdConsulta();
        boolean confirmado = false;
        try {
            con.setAutoCommit(false);

            int idCita = cita.getIdCita();
            String curp = curpDeCita(con, idCita);
            if (curp == null || curp.trim().isEmpty()
                    || (cita.getCurp() != null && !curp.equals(cita.getCurp()))) {
                con.rollback();
                return false;
            }
            if (receta.getIdReceta() > 0) {
                try (PreparedStatement ps = con.prepareStatement("SELECT id_cita FROM Receta WHERE id_receta = ?")) {
                    ps.setInt(1, receta.getIdReceta());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getInt(1) != idCita) { con.rollback(); return false; }
                    }
                }
            }
            cita.setCurp(curp);

            // Toda atención queda dentro de un expediente: si el paciente no tiene, se abre vacío.
            if (idExpedientePorCurp(con, curp) <= 0 && insertarExpedienteVacio(con, curp) <= 0) {
                con.rollback();
                return false;
            }

            receta.setIdCita(idCita);
            if (receta.getFecha() == null) {
                receta.setFecha(new java.util.Date());
            }

            boolean recetaGuardada = receta.getIdReceta() > 0
                    ? actualizarRecetaInterna(con, receta)
                    : insertarRecetaInterna(con, receta) > 0;
            if (!recetaGuardada) {
                con.rollback();
                return false;
            }

            Consulta existente = consultaPorCitaInterna(con, idCita);
            consulta.setIdCita(idCita);
            consulta.setIdConsulta(existente == null ? 0 : existente.getIdConsulta());
            if (consulta.getFechaConsulta() == null) {
                consulta.setFechaConsulta(existente != null && existente.getFechaConsulta() != null
                        ? existente.getFechaConsulta()
                        : LocalDateTime.ofInstant(receta.getFecha().toInstant(), ZoneId.systemDefault()));
            }

            boolean consultaGuardada = existente == null
                    ? insertarConsultaInterna(con, consulta)
                    : actualizarConsultaInterna(con, consulta);
            if (!consultaGuardada) {
                con.rollback();
                return false;
            }

            if (finalizar && !actualizarEstadoCita(con, idCita, Estado.ATENDIDA)) {
                con.rollback();
                return false;
            }

            con.commit();
            confirmado = true;
            if (finalizar) cita.setEstado(Estado.ATENDIDA);
            consulta.setReceta(receta);
            return true;
        } catch (SQLException e) {
            revertir(con, "finalizar consulta", e);
        } finally {
            if (!confirmado) {
                receta.setIdReceta(idRecetaAnterior);
                consulta.setIdConsulta(idConsultaAnterior);
            }
            restaurarAutoCommit(con);
        }
        return false;
    }

    public boolean insertarReceta(Receta receta) {
        if (receta == null || receta.getIdCita() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);
            if (insertarRecetaInterna(con, receta) <= 0) {
                con.rollback();
                return false;
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            revertir(con, "insertar receta", e);
        } finally {
            restaurarAutoCommit(con);
        }
        return false;
    }

    private static int insertarRecetaInterna(Connection con, Receta receta) throws SQLException {
        String sql = "INSERT INTO Receta (id_administrador, id_cita, fecha, diagnostico, nombre_medico, cedula_medico, " +
                "profesion, institucion_titulo, domicilio_medico, especialidad, registro_especialidad, " +
                "nombre_establecimiento, domicilio_establecimiento, nombre_paciente, edad_paciente, sexo_paciente, " +
                "indicaciones) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String idAdministrador = receta.getIdAdministrador() != null
                ? receta.getIdAdministrador()
                : obtenerIdAdministradorPorDefecto(con);

        if (idAdministrador == null) {
            return -1;
        }

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametrosReceta(ps, receta, idAdministrador);

            if (ps.executeUpdate() == 0) {
                return -1;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    receta.setIdReceta(rs.getInt(1));
                }
            }
        }

        receta.setIdAdministrador(idAdministrador);
        insertarDetallesReceta(con, receta);
        insertarSignosVitales(con, receta);
        return receta.getIdReceta();
    }

    public boolean actualizarReceta(Receta receta) {
        if (receta == null || receta.getIdReceta() <= 0 || receta.getIdCita() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        try {
            con.setAutoCommit(false);
            if (!actualizarRecetaInterna(con, receta)) {
                con.rollback();
                return false;
            }
            con.commit();
            return true;
        } catch (SQLException e) {
            revertir(con, "actualizar receta", e);
        } finally {
            restaurarAutoCommit(con);
        }
        return false;
    }

    private static boolean actualizarRecetaInterna(Connection con, Receta receta) throws SQLException {
        String sql = "UPDATE Receta SET id_administrador = ?, id_cita = ?, fecha = ?, diagnostico = ?, nombre_medico = ?, " +
                "cedula_medico = ?, profesion = ?, institucion_titulo = ?, domicilio_medico = ?, especialidad = ?, " +
                "registro_especialidad = ?, nombre_establecimiento = ?, domicilio_establecimiento = ?, nombre_paciente = " +
                "?, edad_paciente = ?, sexo_paciente = ?, indicaciones = ? WHERE id_receta = ?";

        String idAdministrador = receta.getIdAdministrador() != null
                ? receta.getIdAdministrador()
                : obtenerIdAdministradorPorDefecto(con);

        if (idAdministrador == null) {
            return false;
        }

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            parametrosReceta(ps, receta, idAdministrador);
            ps.setInt(18, receta.getIdReceta());

            if (ps.executeUpdate() == 0) {
                return false;
            }
        }

        receta.setIdAdministrador(idAdministrador);
        eliminarDetalleReceta(con, receta);
        insertarDetallesReceta(con, receta);
        insertarSignosVitales(con, receta);
        return true;
    }

    public boolean eliminarReceta(int idReceta) {
        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Receta WHERE id_receta = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar receta: " + e.getMessage());
        }
        return false;
    }

    public HashMap<Integer, Receta> cargarRecetas() {
        return cargarRecetasFiltradas(null);
    }

    public HashMap<Integer, Receta> cargarRecetasPorCurp(String curp) {
        if (curp == null || curp.trim().isEmpty()) {
            throw new IllegalArgumentException("La CURP es obligatoria.");
        }
        return cargarRecetasFiltradas(curp);
    }

    public Receta cargarRecetaPorCita(int idCita) {
        if (idCita <= 0) {
            return null;
        }

        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar la receta.");
        }

        String sql = "SELECT r.*, c.CURP " +
                "FROM Receta r JOIN Cita c ON c.id_cita = r.id_cita " +
                "WHERE r.id_cita = ? ORDER BY r.id_receta DESC LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Receta receta = crearReceta(rs);
                    receta.setMedicamentos(cargarMedicamentosPorReceta(con, receta.getIdReceta()));
                    receta.setSignosVitales(cargarSignosVitalesPorReceta(con, receta.getIdReceta()));
                    return receta;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar la receta de la cita.", e);
        }
        return null;
    }

    private HashMap<Integer, Receta> cargarRecetasFiltradas(String curp) {
        HashMap<Integer, Receta> recetas = new HashMap<>();
        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar las recetas.");
        }

        String sql = "SELECT r.*, c.CURP " +
                "FROM Receta r JOIN Cita c ON c.id_cita = r.id_cita " +
                (curp == null ? "" : "WHERE c.CURP = ? ") + "ORDER BY r.id_receta";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (curp != null) {
                ps.setString(1, curp);
            }
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Receta receta = crearReceta(rs);
                recetas.put(receta.getIdReceta(), receta);
            }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudieron cargar las recetas.", e);
        }

        for (Receta receta : recetas.values()) {
            try {
                receta.setMedicamentos(cargarMedicamentosPorReceta(con, receta.getIdReceta()));
                receta.setSignosVitales(cargarSignosVitalesPorReceta(con, receta.getIdReceta()));
            } catch (SQLException e) {
                throw new IllegalStateException("No se pudo cargar el detalle de la receta " + receta.getIdReceta(), e);
            }
        }
        return recetas;
    }

    public boolean insertarMedicamento(Medicamento medicamento) {
        if (medicamento == null || medicamento.getIdReceta() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }


        try (PreparedStatement ps = con.prepareStatement(SQL_MEDICAMENTO, Statement.RETURN_GENERATED_KEYS)) {
            parametrosMedicamento(ps, medicamento.getIdReceta(), medicamento);

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        medicamento.setIdDetalleReceta(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar medicamento: " + e.getMessage());
        }
        return false;
    }

    public ArrayList<Medicamento> cargarMedicamentos() {
        ArrayList<Medicamento> medicamentos = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            return medicamentos;
        }

        String sql = "SELECT * FROM Detalle_receta ORDER BY id_detalle_receta";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                medicamentos.add(crearMedicamento(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar medicamentos: " + e.getMessage());
        }
        return medicamentos;
    }

    private static final String SQL_MEDICAMENTO = "INSERT INTO Detalle_receta (id_receta, nombre_medicamento, " +
            "concentracion, dosis, frecuencia, duracion, presentacion, via_administracion, denominacion_distintiva, " +
            "cantidad, grupo_regulatorio) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static void parametrosMedicamento(PreparedStatement ps, int idReceta, Medicamento medicamento)
            throws SQLException {
        ps.setInt(1, idReceta);
        ps.setString(2, medicamento.getNombreMedicamento());
        ps.setString(3, medicamento.getConcentracion());
        ps.setString(4, medicamento.getDosis());
        ps.setString(5, medicamento.getFrecuencia());
        ps.setString(6, medicamento.getDuracion());
        ps.setString(7, medicamento.getPresentacion());
        ps.setString(8, medicamento.getViaAdministracion());
        ps.setString(9, medicamento.getDenominacionDistintiva());
        ps.setString(10, medicamento.getCantidad());
        ps.setString(11, medicamento.getGrupoRegulatorio());
    }

    /** Parámetros 1-17 comunes a INSERT y UPDATE de Receta. */
    private static void parametrosReceta(PreparedStatement ps, Receta receta, String idAdministrador) throws SQLException {
        ps.setString(1, idAdministrador);
        ps.setInt(2, receta.getIdCita());
        ps.setTimestamp(3, toSqlTimestamp(receta.getFecha() != null ? receta.getFecha() : new java.util.Date()));
        ps.setString(4, receta.getDiagnostico());
        ps.setString(5, receta.getNombreMedico());
        ps.setString(6, receta.getCedulaMedico());
        ps.setString(7, receta.getProfesion());
        ps.setString(8, receta.getInstitucionTitulo());
        ps.setString(9, receta.getDomicilioMedico());
        ps.setString(10, receta.getEspecialidad());
        ps.setString(11, receta.getRegistroEspecialidad());
        ps.setString(12, receta.getNombreEstablecimiento());
        ps.setString(13, receta.getDomicilioEstablecimiento());
        ps.setString(14, receta.getNombrePaciente());
        ps.setString(15, receta.getEdadPaciente());
        ps.setString(16, receta.getSexoPaciente());
        ps.setString(17, receta.getIndicaciones());
    }

    /** Parámetros 1-9 comunes a INSERT y UPDATE de Consulta. */
    private static void parametrosConsulta(PreparedStatement ps, Consulta consulta, LocalDateTime fecha) throws SQLException {
        ps.setInt(1, consulta.getIdCita());
        ps.setTimestamp(2, java.sql.Timestamp.valueOf(fecha));
        ps.setString(3, consulta.getPadecimiento());
        ps.setString(4, consulta.getMotivo());
        ps.setString(5, consulta.getObservaciones());
        ps.setString(6, consulta.getEvolucion());
        ps.setString(7, consulta.getExploracionFisica());
        ps.setString(8, consulta.getResultadosEstudios());
        ps.setString(9, consulta.getPronostico());
    }

    private static boolean insertarConsultaInterna(Connection con, Consulta consulta) throws SQLException {
        String sql = "INSERT INTO Consulta (id_cita, fecha, padecimiento, motivo, observaciones, evolucion, " +
                "exploracion_fisica, resultados_estudios, pronostico) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime fecha = consulta.getFechaConsulta();
        if (fecha == null) {
            fecha = LocalDateTime.now();
            consulta.setFechaConsulta(fecha);
        }

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            parametrosConsulta(ps, consulta, fecha);

            if (ps.executeUpdate() == 0) {
                return false;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    consulta.setIdConsulta(rs.getInt(1));
                }
            }
        }
        return true;
    }

    private static boolean actualizarConsultaInterna(Connection con, Consulta consulta) throws SQLException {
        String sql = "UPDATE Consulta SET id_cita = ?, fecha = ?, padecimiento = ?, motivo = ?, observaciones = ?, " +
                "evolucion = ?, exploracion_fisica = ?, resultados_estudios = ?, pronostico = ? WHERE id_consulta = ?";

        LocalDateTime fecha = consulta.getFechaConsulta() == null
                ? LocalDateTime.now()
                : consulta.getFechaConsulta();
        consulta.setFechaConsulta(fecha);

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            parametrosConsulta(ps, consulta, fecha);
            ps.setInt(10, consulta.getIdConsulta());
            return ps.executeUpdate() > 0;
        }
    }

    private static Consulta consultaPorIdInterna(Connection con, int idConsulta) throws SQLException {
        String sql = "SELECT * " +
                "FROM Consulta WHERE id_consulta = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idConsulta);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? crearConsulta(rs) : null;
            }
        }
    }

    private static Consulta consultaPorCitaInterna(Connection con, int idCita) throws SQLException {
        String sql = "SELECT * " +
                "FROM Consulta WHERE id_cita = ? ORDER BY fecha DESC, id_consulta DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? crearConsulta(rs) : null;
            }
        }
    }

    private static Receta crearReceta(ResultSet rs) throws SQLException {
        Receta receta = new Receta();
        receta.setIdReceta(rs.getInt("id_receta"));
        receta.setIdAdministrador(rs.getString("id_administrador"));
        receta.setIdCita(rs.getInt("id_cita"));
        receta.setFecha(leerFechaHora(rs.getObject("fecha")));
        receta.setDiagnostico(rs.getString("diagnostico"));
        receta.setCurp(rs.getString("CURP"));
        receta.setNombreMedico(rs.getString("nombre_medico"));
        receta.setCedulaMedico(rs.getString("cedula_medico"));
        receta.setProfesion(rs.getString("profesion"));
        receta.setInstitucionTitulo(rs.getString("institucion_titulo"));
        receta.setDomicilioMedico(rs.getString("domicilio_medico"));
        receta.setEspecialidad(rs.getString("especialidad"));
        receta.setRegistroEspecialidad(rs.getString("registro_especialidad"));
        receta.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
        receta.setDomicilioEstablecimiento(rs.getString("domicilio_establecimiento"));
        receta.setNombrePaciente(rs.getString("nombre_paciente"));
        receta.setEdadPaciente(rs.getString("edad_paciente"));
        receta.setSexoPaciente(rs.getString("sexo_paciente"));
        receta.setIndicaciones(rs.getString("indicaciones"));
        return receta;
    }

    private static Consulta crearConsulta(ResultSet rs) throws SQLException {
        Consulta consulta = new Consulta();
        consulta.setIdConsulta(rs.getInt("id_consulta"));
        consulta.setIdCita(rs.getInt("id_cita"));

        consulta.setFechaConsulta(leerLocalDateTime(rs.getObject("fecha")));

        consulta.setPadecimiento(rs.getString("padecimiento"));
        consulta.setMotivo(rs.getString("motivo"));
        consulta.setObservaciones(rs.getString("observaciones"));
        consulta.setEvolucion(rs.getString("evolucion"));
        consulta.setExploracionFisica(rs.getString("exploracion_fisica"));
        consulta.setResultadosEstudios(rs.getString("resultados_estudios"));
        consulta.setPronostico(rs.getString("pronostico"));
        return consulta;
    }

    private static String curpDeCita(Connection con, int idCita) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT CURP FROM Cita WHERE id_cita = ?")) {
            ps.setInt(1, idCita);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("CURP") : null;
            }
        }
    }

    private static int idExpedientePorCurp(Connection con, String curp) throws SQLException {
        String sql = "SELECT id_expediente FROM Expediente WHERE CURP = ? ORDER BY id_expediente DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, curp);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static int insertarExpedienteVacio(Connection con, String curp) throws SQLException {
        String sql = "INSERT INTO Expediente (CURP, fecha_apertura) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, curp);
            ps.setDate(2, toSqlDate(new java.util.Date()));
            if (ps.executeUpdate() == 0) {
                return -1;
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    private static boolean actualizarEstadoCita(Connection con, int idCita, Estado estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE Cita SET estado = ? WHERE id_cita = ?")) {
            ps.setString(1, estado(estado).name());
            ps.setInt(2, idCita);
            return ps.executeUpdate() > 0;
        }
    }

    private static void revertir(Connection con, String operacion, SQLException e) {
        try {
            con.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("Error al revertir " + operacion + ": " + rollbackError.getMessage());
        }
        System.err.println("Error al " + operacion + ": " + e.getMessage());
    }

    private static void restaurarAutoCommit(Connection con) {
        try {
            con.setAutoCommit(true);
        } catch (SQLException e) {
            System.err.println("Error al restaurar el autocommit: " + e.getMessage());
        }
    }

    public void cargarDatosPrescriptor(Receta receta) {
        Connection con = getConexion();
        if (con == null) throw new IllegalStateException("No hay conexión para cargar los datos del prescriptor.");
        try {
            String id = receta.getIdAdministrador() != null ? receta.getIdAdministrador() : obtenerIdAdministradorPorDefecto(con);
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM Administrador WHERE id_administrador = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new IllegalStateException("No hay un prescriptor configurado en la base de datos.");
                    receta.setIdAdministrador(id);
                    receta.setNombreMedico((valor(rs.getString("nombre")) + " " + valor(rs.getString("apellido_paterno"))
                            + " " + valor(rs.getString("apellido_materno"))).trim());
                    receta.setCedulaMedico(rs.getString("cedula"));
                    receta.setProfesion(rs.getString("profesion"));
                    receta.setInstitucionTitulo(rs.getString("institución"));
                    receta.setDomicilioMedico(rs.getString("domicilio"));
                    receta.setEspecialidad(rs.getString("especialidad"));
                    receta.setRegistroEspecialidad(rs.getString("registro_especialidad"));
                    receta.setNombreEstablecimiento(rs.getString("nombre_establecimiento"));
                    receta.setDomicilioEstablecimiento(rs.getString("domicilio_establecimiento"));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("No se pudieron cargar los datos del prescriptor.", ex);
        }
    }

    private static String obtenerIdAdministradorPorDefecto(Connection con) throws SQLException {
        String sql = "SELECT id_administrador FROM Administrador ORDER BY id_administrador LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static void eliminarDetalleReceta(Connection con, Receta receta) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Detalle_receta WHERE id_receta = ?")) {
            ps.setInt(1, receta.getIdReceta());
            ps.executeUpdate();
        }
        // Los signos se actualizan sobre su fila (ON CONFLICT); solo se borran si la receta ya no los trae.
        if (receta.getSignosVitales() != null) {
            return;
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM Signos_vitales WHERE id_receta = ?")) {
            ps.setInt(1, receta.getIdReceta());
            ps.executeUpdate();
        }
    }

    private static void insertarDetallesReceta(Connection con, Receta receta) throws SQLException {
        if (receta.getMedicamentos() == null || receta.getMedicamentos().isEmpty()) {
            return;
        }


        for (Medicamento medicamento : receta.getMedicamentos()) {
            if (medicamento == null) {
                continue;
            }

            try (PreparedStatement ps = con.prepareStatement(SQL_MEDICAMENTO, Statement.RETURN_GENERATED_KEYS)) {
                parametrosMedicamento(ps, receta.getIdReceta(), medicamento);
                ps.executeUpdate();

                medicamento.setIdReceta(receta.getIdReceta());
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        medicamento.setIdDetalleReceta(rs.getInt(1));
                    }
                }
            }
        }
    }

    private static void insertarSignosVitales(Connection con, Receta receta) throws SQLException {
        SignosVitales signos = receta.getSignosVitales();
        if (signos == null) {
            return;
        }

        String sql = "INSERT INTO Signos_vitales " +
                "(id_receta, presion_arterial, frecuencia_cardiaca, frecuencia_respiratoria, " +
                "temperatura_corporal, peso, talla, saturacion_oxigeno) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id_receta) DO UPDATE SET presion_arterial = excluded.presion_arterial, " +
                "frecuencia_cardiaca = excluded.frecuencia_cardiaca, " +
                "frecuencia_respiratoria = excluded.frecuencia_respiratoria, " +
                "temperatura_corporal = excluded.temperatura_corporal, peso = excluded.peso, " +
                "talla = excluded.talla, saturacion_oxigeno = excluded.saturacion_oxigeno";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, receta.getIdReceta());
            ps.setString(2, signos.getPresionArterial());
            ps.setInt(3, signos.getFrecuenciaCardiaca());
            ps.setInt(4, signos.getFrecuenciaRespiratoria());
            ps.setDouble(5, signos.getTemperatura());
            ps.setDouble(6, signos.getPeso());
            ps.setInt(7, signos.getTalla());
            ps.setInt(8, signos.getSaturacionOxigeno());
            ps.executeUpdate();
        }
    }

    private static ArrayList<Medicamento> cargarMedicamentosPorReceta(Connection con, int idReceta) throws SQLException {
        ArrayList<Medicamento> medicamentos = new ArrayList<>();
        String sql = "SELECT * FROM Detalle_receta WHERE id_receta = ? ORDER BY id_detalle_receta";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicamentos.add(crearMedicamento(rs));
                }
            }
        }
        return medicamentos;
    }

    private static SignosVitales cargarSignosVitalesPorReceta(Connection con, int idReceta) throws SQLException {
        String sql = "SELECT presion_arterial, frecuencia_cardiaca, frecuencia_respiratoria, " +
                "temperatura_corporal, peso, talla, saturacion_oxigeno FROM Signos_vitales " +
                "WHERE id_receta = ? ORDER BY id_signos_vitales DESC LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idReceta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new SignosVitales(
                            rs.getString("presion_arterial"),
                            rs.getInt("frecuencia_cardiaca"),
                            rs.getInt("frecuencia_respiratoria"),
                            rs.getFloat("temperatura_corporal"),
                            rs.getInt("saturacion_oxigeno"),
                            rs.getFloat("peso"),
                            rs.getInt("talla"));
                }
            }
        }
        return null;
    }

    private static Medicamento crearMedicamento(ResultSet rs) throws SQLException {
        Medicamento medicamento = new Medicamento();
        medicamento.setIdDetalleReceta(rs.getInt("id_detalle_receta"));
        medicamento.setIdReceta(rs.getInt("id_receta"));
        medicamento.setNombreMedicamento(rs.getString("nombre_medicamento"));
        medicamento.setConcentracion(rs.getString("concentracion"));
        medicamento.setDosis(rs.getString("dosis"));
        medicamento.setFrecuencia(rs.getString("frecuencia"));
        medicamento.setDuracion(rs.getString("duracion"));
        medicamento.setPresentacion(rs.getString("presentacion"));
        medicamento.setViaAdministracion(rs.getString("via_administracion"));
        medicamento.setDenominacionDistintiva(rs.getString("denominacion_distintiva"));
        medicamento.setCantidad(rs.getString("cantidad"));
        medicamento.setGrupoRegulatorio(rs.getString("grupo_regulatorio"));
        return medicamento;
    }

    private static String horaTexto(java.time.LocalTime hora) {
        return hora == null ? null : hora.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static java.time.LocalTime leerHora(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.time.LocalTime) {
            return (java.time.LocalTime) valor;
        }
        if (valor instanceof java.sql.Time) {
            return ((java.sql.Time) valor).toLocalTime();
        }
        String texto = valor.toString().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return java.time.LocalTime.parse(texto.length() > 8 ? texto.substring(0, 8) : texto);
        } catch (java.time.format.DateTimeParseException e) {
            String[] partes = texto.split(":");
            if (partes.length >= 2) {
                try {
                    return java.time.LocalTime.of(Integer.parseInt(partes[0].trim()),
                            Integer.parseInt(partes[1].trim()));
                } catch (NumberFormatException | java.time.DateTimeException ignored) {
                    return null;
                }
            }
            return null;
        }
    }

    private static boolean leerBooleano(Object valor) {
        if (valor == null) {
            return false;
        }
        if (valor instanceof Boolean) {
            return (Boolean) valor;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue() != 0;
        }
        String texto = valor.toString().trim().toLowerCase();
        return "1".equals(texto) || "true".equals(texto) || "si".equals(texto) || "sí".equals(texto);
    }

    private static java.time.LocalDateTime leerLocalDateTime(Object valor) {
        java.util.Date fecha = leerFechaHora(valor);
        return fecha == null ? null : java.time.LocalDateTime.ofInstant(
                fecha.toInstant(), ZoneId.systemDefault());
    }

    private static java.sql.Timestamp toSqlTimestamp(java.time.LocalDateTime fecha) {
        return fecha == null ? null : java.sql.Timestamp.valueOf(fecha);
    }

    private static java.sql.Date toSqlDate(java.util.Date fecha) {
        return fecha == null ? null : new java.sql.Date(fecha.getTime());
    }

    private static java.sql.Timestamp toSqlTimestamp(java.util.Date fecha) {
        return fecha == null ? null : new java.sql.Timestamp(fecha.getTime());
    }

    private static java.util.Date toUtilDate(java.sql.Date fecha) {
        return fecha == null ? null : new java.util.Date(fecha.getTime());
    }

    private static java.util.Date leerFechaHora(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return new java.util.Date(((Number) valor).longValue());
        }
        if (valor instanceof java.util.Date) {
            return new java.util.Date(((java.util.Date) valor).getTime());
        }
        String texto = valor.toString().trim().replace(' ', 'T');
        try {
            if (texto.length() == 10) {
                return java.util.Date.from(LocalDate.parse(texto)
                        .atStartOfDay(ZoneId.systemDefault()).toInstant());
            }
            return java.util.Date.from(java.time.LocalDateTime.parse(texto)
                    .atZone(ZoneId.systemDefault()).toInstant());
        } catch (java.time.format.DateTimeParseException e) {
            System.err.println("Fecha de cita inválida: " + valor);
            return null;
        }
    }

    private static String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private static Estado estado(Estado estado) {
        return estado == null ? Estado.AGENDADA : estado;
    }

    private static Estado parseEstado(String estado) {
        if (estado == null) {
            return Estado.AGENDADA;
        }

        try {
            return Estado.valueOf(estado.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Estado.AGENDADA;
        }
    }
}
