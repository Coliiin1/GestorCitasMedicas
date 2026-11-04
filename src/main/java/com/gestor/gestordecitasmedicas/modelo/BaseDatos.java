package com.gestor.gestordecitasmedicas.modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class BaseDatos {
    private String contraseña;
    private static final String URL = System.getProperty("gestor.db.url", "jdbc:sqlite:citas_medicas.db");
    private static Connection conexion = null;

    public static Connection getConexion() {
        try {
            if (conexion == null || conexion.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                conexion = DriverManager.getConnection(URL);
                System.out.println("Conexión a la base de datos SQLite establecida con éxito.");

                try (Statement stmt = conexion.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }

                inicializarBaseDatos();
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.err.println("Error de conexión a la base de datos: " + e.getMessage());
        }
        return conexion;
    }

    private static void inicializarBaseDatos() {
        String[] sentencias = {
                "CREATE TABLE IF NOT EXISTS Administrador (" +
                        "id_administrador INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre VARCHAR(20) NOT NULL, " +
                        "apellido_paterno VARCHAR(20) NOT NULL, " +
                        "apellido_materno VARCHAR(20), " +
                        "contraseña VARCHAR(255) NOT NULL, " +
                        "cedula VARCHAR(8), " +
                        "institución VARCHAR(20), " +
                        "domicilio VARCHAR(20))",
                "CREATE TABLE IF NOT EXISTS Asistente (" +
                        "id_asistente INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "nombre VARCHAR(20) NOT NULL, " +
                        "apellido_paterno VARCHAR(20) NOT NULL, " +
                        "apellido_materno VARCHAR(20), " +
                        "contraseña VARCHAR(255) NOT NULL)",
                "CREATE TABLE IF NOT EXISTS Paciente (" +
                        "CURP VARCHAR(18) PRIMARY KEY, " +
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
                        "CURP VARCHAR(18) NOT NULL, " +
                        "fecha_apertura DATE, " +
                        "antecedentes_familiares TEXT, " +
                        "antecedentes_personales TEXT, " +
                        "alergias TEXT, " +
                        "observaciones TEXT, " +
                        "FOREIGN KEY (CURP) REFERENCES Paciente(CURP) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Cita (" +
                        "id_cita INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "CURP VARCHAR(18) NOT NULL, " +
                        "fecha DATE, " +
                        "estado VARCHAR(10), " +
                        "FOREIGN KEY (CURP) REFERENCES Paciente(CURP) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Consulta (" +
                        "id_consulta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_cita INTEGER NOT NULL, " +
                        "id_expediente INTEGER NOT NULL, " +
                        "fecha DATE, " +
                        "padecimiento TEXT, " +
                        "motivo TEXT, " +
                        "observaciones TEXT, " +
                        "FOREIGN KEY (id_cita) REFERENCES Cita(id_cita) ON DELETE CASCADE, " +
                        "FOREIGN KEY (id_expediente) REFERENCES Expediente(id_expediente) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Receta (" +
                        "id_receta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_administrador INTEGER NOT NULL, " +
                        "id_cita INTEGER NOT NULL, " +
                        "fecha DATE, " +
                        "diagnostico TEXT, " +
                        "FOREIGN KEY (id_administrador) REFERENCES Administrador(id_administrador) ON DELETE RESTRICT, " +
                        "FOREIGN KEY (id_cita) REFERENCES Cita(id_cita) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Detalle_receta (" +
                        "id_detalle_receta INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_receta INTEGER NOT NULL, " +
                        "nombre_medicamento TEXT, " +
                        "concentracion VARCHAR(20), " +
                        "dosis VARCHAR(20), " +
                        "frecuencia VARCHAR(20), " +
                        "duracion VARCHAR(20), " +
                        "FOREIGN KEY (id_receta) REFERENCES Receta(id_receta) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Signos_vitales (" +
                        "id_signos_vitales INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "id_receta INTEGER NOT NULL, " +
                        "presion_arterial VARCHAR(3), " +
                        "frecuencia_cardiaca VARCHAR(3), " +
                        "frecuencia_respiratoria VARCHAR(3), " +
                        "temperatura_corporal REAL, " +
                        "peso REAL, " +
                        "talla REAL, " +
                        "saturacion_oxigeno INTEGER, " +
                        "FOREIGN KEY (id_receta) REFERENCES Receta(id_receta) ON DELETE CASCADE)",
                "CREATE TABLE IF NOT EXISTS Horario (" +
                        "id_horario INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "dia INTEGER, " +
                        "hora_inicio TIME, " +
                        "hora_fin TIME, " +
                        "estado BOOLEAN)",
                "CREATE TABLE IF NOT EXISTS Horario_bloqueado (" +
                        "id_horario INTEGER PRIMARY KEY, " +
                        "fecha_inicio TIMESTAMP, " +
                        "fecha_fin TIMESTAMP, " +
                        "FOREIGN KEY (id_horario) REFERENCES Horario(id_horario) ON DELETE CASCADE)"
        };

        try (Statement stmt = conexion.createStatement()) {
            for (String sentencia : sentencias) {
                stmt.execute(sentencia);
            }
            insertarAdministradorPorDefecto(stmt);
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
        }
    }

    private static void insertarAdministradorPorDefecto(Statement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Administrador")) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO Administrador " +
                        "(nombre, apellido_paterno, apellido_materno, \"contraseña\", cedula, \"institución\", domicilio) " +
                        "VALUES ('Administrador', 'Sistema', '', 'admin', '00000000', 'Sistema', '')");
            }
        }
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

        String sql = "INSERT INTO Asistente (nombre, apellido_paterno, apellido_materno, \"contraseña\") " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, valor(asistente.getNombre()));
            ps.setString(2, valor(asistente.getApellidoPaterno()));
            ps.setString(3, asistente.getApellidoMaterno());
            ps.setString(4, valor(asistente.getContrasena()));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        asistente.setIdAsistente(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error al insertar asistente: " + e.getMessage());
        }
        return false;
    }

    public boolean eliminarAsistente(int idAsistente) {
        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "DELETE FROM Asistente WHERE id_asistente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idAsistente);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar asistente: " + e.getMessage());
        }
        return false;
    }

    public boolean actualizarAsistente(Asistente asistente) {
        if (asistente == null || asistente.getIdAsistente() <= 0) {
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
            ps.setInt(5, asistente.getIdAsistente());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar asistente: " + e.getMessage());
        }
        return false;
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
                Asistente asistente = new Asistente(rs.getInt("id_asistente"));
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
            ps.setDate(2, toSqlDate(cita.getFechaCita()));
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
            ps.setDate(2, toSqlDate(cita.getFechaCita()));
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
                Cita cita = new Cita();
                cita.setIdCita(rs.getInt("id_cita"));
                cita.setCurp(rs.getString("CURP"));
                cita.setFechaCita(toUtilDate(rs.getDate("fecha")));
                cita.setEstado(parseEstado(rs.getString("estado")));
                citas.put(cita.getIdCita(), cita);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar citas: " + e.getMessage());
        }
        return citas;
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

        String sql = "INSERT INTO Expediente " +
                "(CURP, fecha_apertura, antecedentes_familiares, antecedentes_personales, alergias, observaciones) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, expediente.getDatosPaciente().getCurp());
            ps.setDate(2, toSqlDate(expediente.getFechaApertura() != null
                    ? expediente.getFechaApertura()
                    : new java.util.Date()));
            ps.setString(3, expediente.getAntecedentesFamiliares());
            ps.setString(4, expediente.getAntecedentesPersonales());
            ps.setString(5, expediente.getAlergias());
            ps.setString(6, expediente.getObservaciones());

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

    public boolean actualizarExpediente(Expediente expediente) {
        if (expediente == null || expediente.getIdExpediente() <= 0 ||
                expediente.getDatosPaciente() == null || expediente.getDatosPaciente().getCurp() == null) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "UPDATE Expediente SET CURP = ?, fecha_apertura = ?, antecedentes_familiares = ?, " +
                "antecedentes_personales = ?, alergias = ?, observaciones = ? WHERE id_expediente = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, expediente.getDatosPaciente().getCurp());
            ps.setDate(2, toSqlDate(expediente.getFechaApertura()));
            ps.setString(3, expediente.getAntecedentesFamiliares());
            ps.setString(4, expediente.getAntecedentesPersonales());
            ps.setString(5, expediente.getAlergias());
            ps.setString(6, expediente.getObservaciones());
            ps.setInt(7, expediente.getIdExpediente());
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

        String sql = "SELECT id_expediente, CURP, fecha_apertura, antecedentes_familiares, " +
                "antecedentes_personales, alergias, observaciones FROM Expediente " +
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
                    expediente.setObservaciones(rs.getString("observaciones"));
                    expediente.setDatosPaciente(cargarPacientePorCurp(curp));
                    expediente.setConsultas(cargarConsultasPorExpediente(expediente.getIdExpediente()));
                    expediente.setRecetas(cargarRecetasPorCurp(curp));
                    return expediente;
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo cargar el expediente.", e);
        }
        return null;
    }

    public ArrayList<Consulta> cargarConsultasPorExpediente(int idExpediente) {
        ArrayList<Consulta> consultas = new ArrayList<>();
        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar las consultas.");
        }
        String sql = "SELECT id_consulta, id_cita, fecha, padecimiento, motivo, observaciones "
                + "FROM Consulta WHERE id_expediente = ? ORDER BY fecha DESC, id_consulta DESC";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idExpediente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Consulta consulta = new Consulta();
                    consulta.setIdConsulta(rs.getInt("id_consulta"));
                    consulta.setIdExpediente(idExpediente);
                    consulta.setIdCita(rs.getInt("id_cita"));
                    Object fecha = rs.getObject("fecha");
                    if (fecha instanceof Number) {
                        consulta.setFechaConsulta(new java.sql.Timestamp(((Number) fecha).longValue()).toLocalDateTime());
                    } else if (fecha != null) {
                        String textoFecha = fecha.toString().trim().replace(' ', 'T');
                        consulta.setFechaConsulta(textoFecha.length() == 10
                                ? java.time.LocalDate.parse(textoFecha).atStartOfDay()
                                : java.time.LocalDateTime.parse(textoFecha));
                    }
                    consulta.setPadecimiento(rs.getString("padecimiento"));
                    consulta.setMotivo(rs.getString("motivo"));
                    consulta.setObservaciones(rs.getString("observaciones"));
                    consultas.add(consulta);
                }
            }
        } catch (SQLException | java.time.format.DateTimeParseException e) {
            throw new IllegalStateException("No se pudo cargar el historial de consultas.", e);
        }
        consultas.sort(java.util.Comparator.comparing(Consulta::getFechaConsulta,
                java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()))
                .thenComparing(java.util.Comparator.comparingInt(Consulta::getIdConsulta).reversed()));
        return consultas;
    }

    public boolean insertarReceta(Receta receta) {
        if (receta == null || receta.getIdCita() <= 0) {
            return false;
        }

        Connection con = getConexion();
        if (con == null) {
            return false;
        }

        String sql = "INSERT INTO Receta (id_administrador, id_cita, fecha, diagnostico) VALUES (?, ?, ?, ?)";

        try {
            con.setAutoCommit(false);

            int idAdministrador = receta.getIdAdministrador() > 0
                    ? receta.getIdAdministrador()
                    : obtenerIdAdministradorPorDefecto(con);

            if (idAdministrador <= 0) {
                con.rollback();
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idAdministrador);
                ps.setInt(2, receta.getIdCita());
                ps.setDate(3, toSqlDate(receta.getFecha() != null ? receta.getFecha() : new java.util.Date()));
                ps.setString(4, receta.getDiagnostico());

                if (ps.executeUpdate() == 0) {
                    con.rollback();
                    return false;
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
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackError) {
                System.err.println("Error al revertir la transacción de receta: " + rollbackError.getMessage());
            }
            System.err.println("Error al insertar receta: " + e.getMessage());
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Error al restaurar el autocommit: " + e.getMessage());
            }
        }
        return false;
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

    private HashMap<Integer, Receta> cargarRecetasFiltradas(String curp) {
        HashMap<Integer, Receta> recetas = new HashMap<>();
        Connection con = getConexion();
        if (con == null) {
            throw new IllegalStateException("No hay conexión para cargar las recetas.");
        }

        String sql = "SELECT r.id_receta, r.id_administrador, r.id_cita, r.fecha, r.diagnostico, c.CURP " +
                "FROM Receta r JOIN Cita c ON c.id_cita = r.id_cita " +
                (curp == null ? "" : "WHERE c.CURP = ? ") + "ORDER BY r.id_receta";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (curp != null) {
                ps.setString(1, curp);
            }
            try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Receta receta = new Receta();
                receta.setIdReceta(rs.getInt("id_receta"));
                receta.setIdAdministrador(rs.getInt("id_administrador"));
                receta.setIdCita(rs.getInt("id_cita"));
                receta.setFecha(toUtilDate(rs.getDate("fecha")));
                receta.setDiagnostico(rs.getString("diagnostico"));
                receta.setCurp(rs.getString("CURP"));
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

        String sql = "INSERT INTO Detalle_receta " +
                "(id_receta, nombre_medicamento, concentracion, dosis, frecuencia, duracion) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, medicamento.getIdReceta());
            ps.setString(2, medicamento.getNombreMedicamento());
            ps.setString(3, medicamento.getConcentracion());
            ps.setString(4, medicamento.getDosis());
            ps.setString(5, medicamento.getFrecuencia());
            ps.setString(6, medicamento.getDuradcion());

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

        String sql = "SELECT id_detalle_receta, id_receta, nombre_medicamento, concentracion, dosis, " +
                "frecuencia, duracion FROM Detalle_receta ORDER BY id_detalle_receta";

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

    public void convertirHoras(LocalTime hora) {
        if (hora != null) {
            System.out.println(hora.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
    }

    private static int obtenerIdAdministradorPorDefecto(Connection con) throws SQLException {
        String sql = "SELECT id_administrador FROM Administrador ORDER BY id_administrador LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    private static void insertarDetallesReceta(Connection con, Receta receta) throws SQLException {
        if (receta.getMedicamentos() == null || receta.getMedicamentos().isEmpty()) {
            return;
        }

        String sql = "INSERT INTO Detalle_receta " +
                "(id_receta, nombre_medicamento, concentracion, dosis, frecuencia, duracion) VALUES (?, ?, ?, ?, ?, ?)";

        for (Medicamento medicamento : receta.getMedicamentos()) {
            if (medicamento == null) {
                continue;
            }

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, receta.getIdReceta());
                ps.setString(2, medicamento.getNombreMedicamento());
                ps.setString(3, medicamento.getConcentracion());
                ps.setString(4, medicamento.getDosis());
                ps.setString(5, medicamento.getFrecuencia());
                ps.setString(6, medicamento.getDuradcion());
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
                "temperatura_corporal, peso, talla, saturacion_oxigeno) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
        String sql = "SELECT id_detalle_receta, id_receta, nombre_medicamento, concentracion, dosis, " +
                "frecuencia, duracion FROM Detalle_receta WHERE id_receta = ? ORDER BY id_detalle_receta";

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
        medicamento.setDuradcion(rs.getString("duracion"));
        return medicamento;
    }

    private static java.sql.Date toSqlDate(java.util.Date fecha) {
        return fecha == null ? null : new java.sql.Date(fecha.getTime());
    }

    private static java.util.Date toUtilDate(java.sql.Date fecha) {
        return fecha == null ? null : new java.util.Date(fecha.getTime());
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
