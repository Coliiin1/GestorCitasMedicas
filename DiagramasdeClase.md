
```mermaid
classDiagram
    direction LR

    namespace modelo {
        class Estado {
            <<enumeration>>
            AGENDADA
            ATENDIDA
            CANCELADA
            +etiqueta(estado: Estado)$ String
        }

        class DiaSemana {
            <<enumeration>>
            LUNES
            MARTES
            MIERCOLES
            JUEVES
            VIERNES
            SABADO
            DOMINGO
            +getNumero() int
            +getEtiqueta() String
            +getEtiquetaCorta() String
            +desdeNumero(numero: int)$ DiaSemana
            +desdeDayOfWeek(dayOfWeek: DayOfWeek)$ DiaSemana
        }

        class Usuario {
            <<abstract>>
            -String nombre
            -String apellidoPaterno
            -String apellidoMaterno
            +getNombre() String
            +getApellidoPaterno() String
            +getApellidoMaterno() String
            +setNombre(nombre: String) void
            +setApellidoPaterno(apellidoPaterno: String) void
            +setApellidoMaterno(apellidoMaterno: String) void
        }

        class Personal {
            <<abstract>>
            +autenticar(idUsuario: int, contrasena: String)$ String
        }

        class Administrador {
            -int idAdministrador
            -String cedulaProfesional
            -String institucionAcademica
            -String domicilio
            -String profesion
            -String especialidad
            -String registroEspecialidad
            -String nombreEstablecimiento
            -String domicilioEstablecimiento
            +getIdAdministrador() int
            +getCedulaProfesional() String
            +getInstitucionAcademica() String
            +getDomicilio() String
            +setIdAdministrador(idAdministrador: int) void
            +setCedulaProfesional(cedulaProfesional: String) void
            +setInstitucionAcademica(institucionAcademica: String) void
            +setDomicilio(domicilio: String) void
            +get/set de profesion, especialidad, registroEspecialidad y establecimiento
            +buscar(idAdministrador: int)$ Administrador
            +actualizar() boolean
            +usuarioDisponible(nuevoId: int, idActual: int)$ boolean
            +cambiarCredenciales(idActual: int, nuevoId: int, nuevaContrasena: String)$ boolean
        }

        class Asistente {
            -int idAsistente
            -String contrasena
            +Asistente()
            +Asistente(idAsistente: int)
            +Asistente(nombre: String, apellidoPaterno: String, apellidoMaterno: String, contrasena: String)
            +getIdAsistente() int
            +getContrasena() String
            +setIdAsistente(idAsistente: int) void
            +setContrasena(contrasena: String) void
            +validar(nombre: String, apellidoPaterno: String, apellidoMaterno: String, contrasena: String, esNuevo: boolean)$ String
            +registrar() boolean
            +actualizar() boolean
            +eliminar() boolean
            +todos()$ ArrayList~Asistente~
        }

        class Paciente {
            -String curp
            -Date fechaNacimiento
            -String sexo
            -String domicilio
            -String telefono
            -String correo
            +Paciente(curp: String, fechaNacimiento: Date, sexo: String, domicilio: String, telefono: String, correo: String)
            +getCurp() String
            +getFechaNacimiento() Date
            +getSexo() String
            +getDomicilio() String
            +getTelefono() String
            +getCorreo() String
            +setCurp(curp: String) void
            +setFechaNacimiento(fechaNacimiento: Date) void
            +setSexo(sexo: String) void
            +setDomicilio(domicilio: String) void
            +setTelefono(telefono: String) void
            +setCorreo(correo: String) void
            +nombreParaMostrar(paciente: Paciente, curp: String)$ String
            +edadTexto(paciente: Paciente)$ String
            +validar(nombre: String, apellidoPaterno: String, apellidoMaterno: String, curp: String, fechaNacimiento: LocalDate, telefono: String, correo: String, domicilio: String)$ String
            +registrar() boolean
            +actualizar() boolean
            +eliminar() boolean
            +todos()$ HashMap~String, Paciente~
            +buscar(curp: String)$ Paciente
        }

        class Cita {
            -int idCita
            -Date fechaCita
            -Estado estado
            -String curp
            +getIdCita() int
            +getFechaCita() Date
            +getEstado() Estado
            +getCurp() String
            +setIdCita(idCita: int) void
            +setFechaCita(fechaCita: Date) void
            +setEstado(estado: Estado) void
            +setCurp(curp: String) void
            +fechaHora() LocalDateTime
            +agendar() boolean
            +reprogramar(nuevaFecha: Date) boolean
            +cancelar() boolean
            +todas()$ HashMap~Integer, Cita~
            +buscar(idCita: int)$ Cita
            +entre(inicio: LocalDate, fin: LocalDate)$ ArrayList~Cita~
            +activasEnRango(inicio: LocalDateTime, fin: LocalDateTime)$ ArrayList~Cita~
            +ocupadaEn(fecha: LocalDate, hora: LocalTime, idExcluir: int)$ Cita
        }

        class Horario {
            -int idHorario
            -LocalTime horaInicio
            -LocalTime horaFin
            -DiaSemana dia
            -boolean estado
            +getIdHorario() int
            +getHoraInicio() LocalTime
            +getHoraFin() LocalTime
            +getDia() DiaSemana
            +isEstado() boolean
            +setIdHorario(idHorario: int) void
            +setHoraInicio(horaInicio: LocalTime) void
            +setHoraFin(horaFin: LocalTime) void
            +setDia(dia: DiaSemana) void
            +setEstado(estado: boolean) void
            +guardar() boolean
            +eliminar() boolean
            +citasActivas() ArrayList~Cita~
            +todos()$ ArrayList~Horario~
            +activos()$ ArrayList~Horario~
        }

        class HorarioBloqueado {
            -int idHorarioBloqueado
            -LocalDateTime fechaInicio
            -LocalDateTime fechaFin
            +getIdHorarioBloqueado() int
            +getFechaInicio() LocalDateTime
            +getFechaFin() LocalDateTime
            +setIdHorarioBloqueado(idHorarioBloqueado: int) void
            +setFechaInicio(fechaInicio: LocalDateTime) void
            +setFechaFin(fechaFin: LocalDateTime) void
            +guardar() boolean
            +eliminar() boolean
            +todos()$ ArrayList~HorarioBloqueado~
        }

        class PlanificadorAgenda {
            <<utility>>
            +String[] MESES$
            +String[] MESES_CORTO$
            +int DURACION_CITA_MINUTOS$
            +horariosDelDia(fecha: LocalDate, horarios: List~Horario~)$ List~Horario~
            +iniciosDisponibles(fecha: LocalDate, horarios: List~Horario~, bloqueos: List~HorarioBloqueado~)$ List~LocalTime~
            +horasDeTrabajo(horarios: List~Horario~)$ List~LocalTime~
            +esInicioDisponible(fecha: LocalDate, hora: LocalTime, horarios: List~Horario~, bloqueos: List~HorarioBloqueado~)$ boolean
            +estaBloqueado(inicio: LocalDateTime, fin: LocalDateTime, bloqueos: List~HorarioBloqueado~)$ boolean
            +tieneBloqueo(fecha: LocalDate, bloqueos: List~HorarioBloqueado~)$ boolean
            +seSolapan(inicioA: LocalDateTime, finA: LocalDateTime, inicioB: LocalDateTime, finB: LocalDateTime)$ boolean
            +tramoSeSolapa(a: Horario, b: Horario)$ boolean
            +esDiaHabil(fecha: LocalDate, horarios: List~Horario~)$ boolean
            +horasDelDia()$ List~LocalTime~
            +formatoHora(hora: LocalTime)$ String
            +formatoRango(inicio: LocalDateTime, fin: LocalDateTime)$ String
            +lunesDe(fecha: LocalDate)$ LocalDate
        }

        class Expediente {
            -int idExpediente
            -Paciente datosPaciente
            -Date fechaApertura
            -String antecedentesFamiliares
            -String antecedentesPersonales
            -String alergias
            -String observaciones
            -String tipoSangre
            -HashMap~Integer, Receta~ recetas
            -ArrayList~Consulta~ consultas
            -String tipoEstablecimiento
            -String nombreEstablecimiento
            -String domicilioEstablecimiento
            -String institucionEstablecimiento
            -String propietario
            -String grupoEtnico
            -String antecedentesNoPatologicos
            -String consumoSustancias
            -String padecimientoActual
            -String interrogatorioSistemas
            -String exploracionFisica
            -String resultadosEstudios
            -String diagnostico
            -String pronostico
            -String indicacionTerapeutica
            -String autorHistoria
            -String cedulaHistoria
            -String fechaHoraHistoria
            -String signosHistoria
            +getIdExpediente() int
            +getDatosPaciente() Paciente
            +getFechaApertura() Date
            +getAntecedentesFamiliares() String
            +getAntecedentesPersonales() String
            +getAlergias() String
            +getObservaciones() String
            +getTipoSangre() String
            +getRecetas() HashMap~Integer, Receta~
            +getConsultas() ArrayList~Consulta~
            +setIdExpediente(idExpediente: int) void
            +setDatosPaciente(datosPaciente: Paciente) void
            +setFechaApertura(fechaApertura: Date) void
            +setAntecedentesFamiliares(antecedentesFamiliares: String) void
            +setAntecedentesPersonales(antecedentesPersonales: String) void
            +setAlergias(alergias: String) void
            +setObservaciones(observaciones: String) void
            +setTipoSangre(tipoSangre: String) void
            +setRecetas(recetas: HashMap~Integer, Receta~) void
            +setConsultas(consultas: ArrayList~Consulta~) void
            +get/set de cada campo de la historia clinica
            +guardar() boolean
            +dePaciente(curp: String)$ Expediente
        }

        class Consulta {
            -int idConsulta
            -int idExpediente
            -int idCita
            -LocalDateTime fechaConsulta
            -String padecimiento
            -String motivo
            -String observaciones
            -Receta receta
            -SignosVitales signosVitales
            -String evolucion
            -String exploracionFisica
            -String resultadosEstudios
            -String diagnostico
            -String pronostico
            -String indicaciones
            -String nombreMedico
            -String cedulaMedico
            -String nombrePaciente
            -String edadPaciente
            -String sexoPaciente
            +getIdConsulta() int
            +getIdExpediente() int
            +getIdCita() int
            +getFechaConsulta() LocalDateTime
            +getPadecimiento() String
            +getMotivo() String
            +getObservaciones() String
            +getReceta() Receta
            +getSignosVitales() SignosVitales
            +setIdConsulta(idConsulta: int) void
            +setIdExpediente(idExpediente: int) void
            +setIdCita(idCita: int) void
            +setFechaConsulta(fechaConsulta: LocalDateTime) void
            +setPadecimiento(padecimiento: String) void
            +setMotivo(motivo: String) void
            +setObservaciones(observaciones: String) void
            +setReceta(receta: Receta) void
            +setSignosVitales(signosVitales: SignosVitales) void
            +get/set de evolucion, nota medica y datos del medico y paciente
            +deCita(idCita: int)$ Consulta
            +guardarAtencion(cita: Cita, receta: Receta, finalizar: boolean) boolean
        }

        class Receta {
            -int idReceta
            -int idCita
            -int idAdministrador
            -String curp
            -Date fecha
            -String diagnostico
            -ArrayList~Medicamento~ medicamentos
            -SignosVitales signosVitales
            -String nombreMedico
            -String cedulaMedico
            -String profesion
            -String institucionTitulo
            -String domicilioMedico
            -String especialidad
            -String registroEspecialidad
            -String nombreEstablecimiento
            -String domicilioEstablecimiento
            -String nombrePaciente
            -String edadPaciente
            -String sexoPaciente
            -String indicaciones
            +getIdReceta() int
            +getIdCita() int
            +getIdAdministrador() int
            +getCurp() String
            +getFecha() Date
            +getDiagnostico() String
            +getMedicamentos() ArrayList~Medicamento~
            +getSignosVitales() SignosVitales
            +setIdReceta(idReceta: int) void
            +setIdCita(idCita: int) void
            +setIdAdministrador(idAdministrador: int) void
            +setCurp(curp: String) void
            +setFecha(fecha: Date) void
            +setDiagnostico(diagnostico: String) void
            +setMedicamentos(medicamentos: ArrayList~Medicamento~) void
            +setSignosVitales(signosVitales: SignosVitales) void
            +get/set de los datos del prescriptor, establecimiento, paciente e indicaciones
            +deCita(idCita: int)$ Receta
            +todas()$ HashMap~Integer, Receta~
            +cargarDatosPrescriptor() void
        }

        class Medicamento {
            -int idDetalleReceta
            -int idReceta
            -String nombreMedicamento
            -String dosis
            -String concentracion
            -String frecuencia
            -String duradcion
            -String presentacion
            -String viaAdministracion
            -String denominacionDistintiva
            -String cantidad
            -String grupoRegulatorio
            +getIdDetalleReceta() int
            +getIdReceta() int
            +getNombreMedicamento() String
            +getDosis() String
            +getConcentracion() String
            +getFrecuencia() String
            +getDuradcion() String
            +setIdDetalleReceta(idDetalleReceta: int) void
            +setIdReceta(idReceta: int) void
            +setNombreMedicamento(nombreMedicamento: String) void
            +setDosis(dosis: String) void
            +setConcentracion(concentracion: String) void
            +setFrecuencia(frecuencia: String) void
            +setDuradcion(duradcion: String) void
            +get/set de presentacion, viaAdministracion, denominacionDistintiva, cantidad y grupoRegulatorio
        }

        class SignosVitales {
            -String presionArterial
            -int frecuenciaCardiaca
            -int frecuenciaRespiratoria
            -float temperaturaCorporal
            -int saturacionOxigeno
            -float peso
            -int talla
            +SignosVitales(presionArterial: String, frecuenciaCardiaca: int, frecuenciaRespiratoria: int, temperaturaCorporal: float, saturacionOxigeno: int, peso: float, talla: int)
            +getPresionArterial() String
            +getFrecuenciaCardiaca() int
            +getFrecuenciaRespiratoria() int
            +getTemperatura() float
            +getSaturacionOxigeno() int
            +getPeso() float
            +getTalla() int
            +setPresionArterial(presionArterial: String) void
            +setFrecuenciaCardiaca(frecuenciaCardiaca: int) void
            +setFrecuenciaRespiratoria(frecuenciaRespiratoria: int) void
            +setTemperatura(temperaturaCorporal: float) void
            +setSaturacionOxigeno(saturacionOxigeno: int) void
            +setPeso(peso: float) void
            +setTalla(talla: int) void
        }

        class BaseDatos {
            -Connection conexion$
            -String urlActual$
            +getConexion()$ Connection
            +cerrarConexion()$ void
            +autenticar(id: int, contrasena: String) String
            +obtenerConfig(clave: String, porDefecto: String) String
            +guardarConfig(clave: String, valor: String) boolean
            +obtenerAdministrador(id: int) Administrador
            +actualizarAdministrador(a: Administrador) boolean
            +idUsuarioLibre(id: int, idAdminActual: int) boolean
            +actualizarCredencialesAdmin(idActual: int, nuevoId: int, nuevaContrasena: String) boolean
            +insertarAsistente(asistente: Asistente) boolean
            +eliminarAsistente(idAsistente: int) boolean
            +actualizarAsistente(asistente: Asistente) boolean
            +cargarAsistentes() ArrayList~Asistente~
            +insertarPaciente(paciente: Paciente) boolean
            +eliminarPaciente(idPaciente: String) boolean
            +actualizarPaciente(paciente: Paciente) boolean
            +cargarPacientes() HashMap~String, Paciente~
            +cargarPacientePorCurp(curp: String) Paciente
            +insertarCita(cita: Cita) boolean
            +eliminarCita(idCita: int) boolean
            +actualizarCita(cita: Cita) boolean
            +cargarCitas() HashMap~Integer, Cita~
            +cargarCitasEntre(inicio: LocalDate, fin: LocalDate) ArrayList~Cita~
            +citasActivasEnRango(inicio: LocalDateTime, fin: LocalDateTime) ArrayList~Cita~
            +citasActivasEnHorario(horario: Horario) ArrayList~Cita~
            +cargarHorarios() ArrayList~Horario~
            +cargarHorariosActivos() ArrayList~Horario~
            +guardarHorario(horario: Horario) boolean
            +eliminarHorario(idHorario: int) boolean
            +cargarBloqueos() ArrayList~HorarioBloqueado~
            +cargarBloqueosEntre(inicio: LocalDateTime, fin: LocalDateTime) ArrayList~HorarioBloqueado~
            +guardarBloqueo(bloqueo: HorarioBloqueado) boolean
            +eliminarBloqueo(idBloqueo: int) boolean
            +insertarExpediente(expediente: Expediente) boolean
            +eliminarExpediente(idExpediente: String) boolean
            +actualizarExpediente(expediente: Expediente) boolean
            +cargarExpedientePorCurp(curp: String) Expediente
            +cargarConsultasPorExpediente(idExpediente: int) ArrayList~Consulta~
            +insertarConsulta(consulta: Consulta) boolean
            +actualizarConsulta(consulta: Consulta) boolean
            +eliminarConsulta(idConsulta: int) boolean
            +cargarConsultaPorId(idConsulta: int) Consulta
            +cargarConsultaPorCita(idCita: int) Consulta
            +finalizarConsulta(cita: Cita, consulta: Consulta, receta: Receta) boolean
            +guardarAtencion(cita: Cita, consulta: Consulta, receta: Receta, finalizar: boolean) boolean
            +insertarReceta(receta: Receta) boolean
            +actualizarReceta(receta: Receta) boolean
            +eliminarReceta(idReceta: int) boolean
            +cargarRecetas() HashMap~Integer, Receta~
            +cargarRecetasPorCurp(curp: String) HashMap~Integer, Receta~
            +cargarRecetaPorCita(idCita: int) Receta
            +cargarDatosPrescriptor(receta: Receta) void
            +insertarMedicamento(medicamento: Medicamento) boolean
            +cargarMedicamentos() ArrayList~Medicamento~
        }

        class MigracionAgenda {
            <<utility>>
            aplicar(con: Connection)$ void
            columnas(con: Connection, tabla: String)$ List~String~
        }

        class MigracionClinica {
            <<utility>>
            aplicar(con: Connection)$ void
        }
    }

    namespace servicio {
        class ControladorCorreo {
            -String servidor
            -String puerto
            -String usuario
            -String contrasena
            -String remitente
            -boolean ssl
            -boolean starttls
            +ControladorCorreo()
            +enviarCorreo(destinatario: String, asunto: String, cuerpoHtml: String) boolean
            +enviarConfirmacionCita(paciente: Paciente, cita: Cita) boolean
            +enviarReprogramacionCita(paciente: Paciente, cita: Cita, fechaAnterior: Date) boolean
            +enviarCancelacionCita(paciente: Paciente, cita: Cita) boolean
            +notificarCancelaciones(citas: List~Cita~, pacientes: Map~String, Paciente~, alTerminar: BiConsumer~Integer, Integer~)$ void
            +estaConfigurado() boolean
        }

        class ControladorPDF {
            +String CLAVE_CARPETA$
            +carpetaSalida()$ String
            +guardarCarpetaSalida(ruta: String)$ boolean
            +abrir(archivo: File)$ void
            +generarPDF(receta: Receta) boolean
            +generarReceta(archivo: File, receta: Receta) void
            +generarExpediente(archivo: File, expediente: Expediente) void
        }

        class TextoExpediente {
            <<utility>>
            +valor(texto: String)$ String
            +fecha(fecha: Date)$ String
            +nombre(p: Paciente)$ String
            +edad(p: Paciente, fecha: Date)$ String
            +antecedentes(e: Expediente)$ String
            +historia(e: Expediente)$ String
            +consultas(e: Expediente)$ String
            +recetasOrdenadas(e: Expediente)$ List~Receta~
            +recetas(e: Expediente)$ String
            +signos(s: SignosVitales)$ String
            +signos(e: Expediente)$ String
        }

        class ValidacionClinica {
            <<utility>>
            +receta(r: Receta, requiereMedicamentos: boolean)$ List~String~
            +atencion(c: Consulta, r: Receta, requiereMedicamentos: boolean)$ List~String~
            +historia(e: Expediente)$ List~String~
        }
    }

    %% Herencia
    Usuario <|-- Paciente : hereda
    Usuario <|-- Personal : hereda
    Personal <|-- Administrador : hereda
    Personal <|-- Asistente : hereda

    %% Asociaciones del dominio
    Cita --> Estado : utiliza
    Cita "0..*" --> "1" Paciente : agendada para (curp)
    Horario --> DiaSemana : dia
    Expediente "1" o-- "1" Paciente : datos del paciente
    Expediente "1" *-- "0..*" Receta : contiene
    Expediente "1" *-- "0..*" Consulta : registra
    Consulta "1" --> "0..1" Receta : genera
    Consulta "1" --> "0..1" SignosVitales : registra
    Consulta "0..*" --> "1" Cita : corresponde a
    Receta "1" *-- "0..*" Medicamento : prescribe
    Receta "1" *-- "1" SignosVitales : registra
    Receta "0..1" --> "1" Cita : pertenece a
    PlanificadorAgenda ..> Horario : calcula disponibilidad
    PlanificadorAgenda ..> HorarioBloqueado : respeta bloqueos

    %% Logica de negocio en las entidades: delegan la persistencia
    Personal ..> BaseDatos : autentica
    Administrador ..> BaseDatos : delega persistencia
    Asistente ..> BaseDatos : delega persistencia
    Paciente ..> BaseDatos : delega persistencia
    Cita ..> BaseDatos : delega persistencia
    Horario ..> BaseDatos : delega persistencia
    HorarioBloqueado ..> BaseDatos : delega persistencia
    Expediente ..> BaseDatos : delega persistencia
    Consulta ..> BaseDatos : guarda atencion (transaccion)
    Receta ..> BaseDatos : delega persistencia
    BaseDatos ..> MigracionClinica : migra esquema
    BaseDatos ..> MigracionAgenda : migra esquema

    %% Servicios
    ControladorCorreo ..> Paciente : notifica
    ControladorCorreo ..> Cita : confirma / reprograma / cancela
    ControladorPDF ..> Receta : genera PDF
    ControladorPDF ..> Expediente : genera PDF
    ControladorPDF ..> TextoExpediente : da formato
    ControladorPDF ..> ValidacionClinica : valida
    ControladorPDF ..> BaseDatos : carpeta de salida
    TextoExpediente ..> Expediente : resume
    TextoExpediente ..> SignosVitales : resume
    ValidacionClinica ..> Receta : valida
    ValidacionClinica ..> Consulta : valida
    ValidacionClinica ..> Expediente : valida
```
