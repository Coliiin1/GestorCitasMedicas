# Requerimientos del Gestor de Citas Médicas

Este documento describe los requerimientos que el sistema cumple hoy y los que siguen pendientes. Refleja el código actual del proyecto, no una propuesta previa.

- **Última actualización:** 2026-11-04
- **Tipo de aplicación:** escritorio con JavaFX 21 y Maven
- **Persistencia:** SQLite en `citas_medicas.db`, accedida mediante `com.gestor.gestordecitasmedicas.modelo.BaseDatos`
- **Documentos relacionados:** `DiagramasdeClase.md` (modelo de clases) y `funcionalidades.md` (seguimiento de la conexión a la base de datos)

## Leyenda

| Estado | Significado |
|---|---|
| Implementado | Funciona en la base de datos y está conectado a la interfaz. |
| Parcial | Existe una parte de la funcionalidad, pero falta conectarla a la interfaz o completar alguna operación. |
| Pendiente | No existe la funcionalidad o solo hay código de modelo sin uso. |

## 1. Actores

| Actor | Responsabilidad en el sistema | Clase del modelo |
|---|---|---|
| Administrador | Responsable clínico. Según el modelo, atiende citas, genera recetas y administra pacientes y expedientes. En la interfaz, la consulta y la receta las registra el sistema con el administrador por defecto (RN-16). | `Administrador` (extiende `Personal`) |
| Asistente | Según el modelo, gestiona la agenda (agendar, reprogramar, cancelar y consultar citas). Hoy esas operaciones están en los controladores de agenda, y los métodos de `Asistente` no están implementados. | `Asistente` (extiende `Personal`) |

Observación: la aplicación todavía no separa permisos por rol. Todas las pantallas están disponibles para cualquier usuario que abra el sistema. La pantalla de acceso no valida credenciales (ver RF-01).

## 2. Requerimientos funcionales

### RF-01 Acceso al sistema

- **Estado:** Parcial.
- La pantalla de inicio de sesión existe (`LoginController`, `login-view.fxml`), pero no valida usuario ni contraseña. Al presionar el botón, abre el menú principal sin verificar nada.
- Pendiente: validar las credenciales contra los registros de `Administrador` y `Asistente`, y limitar las pantallas según el rol.

### RF-02 Gestión de asistentes

- **Estado:** Implementado (`AsistentesController`, `AsistenteEditarController`).
- Registrar, listar y buscar asistentes por nombre.
- Editar datos del asistente. Si la contraseña se deja vacía al editar, se conserva la actual.
- Eliminar asistente con confirmación previa.

### RF-03 Gestión de pacientes

- **Estado:** Implementado (`PacientesController`, `PacienteEditarController`).
- Registrar, listar, buscar por nombre o CURP, editar y eliminar pacientes.
- Al eliminar un paciente, la aplicación advierte que también se borran su expediente, sus citas, consultas y recetas.
- Al registrar un paciente se crea automáticamente su expediente vacío.
- Las reglas de validación se describen en la sección 3.

### RF-04 Expediente clínico

- **Estado:** Implementado (`ExpedienteController`). Eliminación desde la interfaz pendiente.
- Consultar el expediente de un paciente por CURP.
- Mostrar los datos del paciente, alergias, antecedentes familiares y personales, y observaciones.
- Editar alergias, antecedentes y observaciones.
- Mostrar el historial de consultas y de recetas del paciente, con sus medicamentos y signos vitales.
- Exportar el expediente y su historial a PDF (`ControladorPDF.generarExpediente`).
- Pendiente: eliminar el expediente desde la interfaz. El método `eliminarExpediente` existe en `BaseDatos`, pero no tiene uso en la interfaz.

### RF-05 Agenda de citas

- **Estado:** Implementado (`AgendaController`, `agenda-panel.fxml`).
- Mostrar un calendario mensual y una vista semanal con las citas reales de SQLite.
- Crear una cita desde un espacio libre del calendario.
- Reprogramar una cita existente.
- Cancelar una cita.
- Mostrar un resumen semanal con el número de citas atendidas, agendadas, canceladas y libres.
- Iniciar la cita clínica desde la ficha de la cita.

### RF-06 Estados de la cita

- **Estado:** Implementado (`CitaController`, `cita-panel.fxml`, `Estado`).
- Una cita puede estar en uno de estos estados: `AGENDADA`, `ATENDIDA` o `CANCELADA`.
- Las transiciones y restricciones aplicadas se describen en las reglas RN-09 a RN-11.

### RF-07 Consulta médica (cita en curso)

- **Estado:** Parcial (`ConsultaController`, `consulta-panel.fxml`).
- Capturar signos vitales (presión arterial, frecuencias cardíaca y respiratoria, temperatura, saturación de oxígeno, peso y talla).
- Capturar diagnóstico y medicamentos (nombre, concentración, dosis, frecuencia y duración).
- Guardar la receta con sus medicamentos y signos vitales. Al actualizar, se reemplazan los medicamentos sin duplicarlos.
- Generar la receta en PDF.
- Finalizar la consulta: la cita pasa a `ATENDIDA`.
- Pendiente: conectar la pantalla con `BaseDatos.finalizarConsulta` para guardar también el motivo, el padecimiento y las observaciones de la consulta. Hoy la pantalla guarda la receta y cambia el estado de la cita en pasos separados, no en una sola transacción.

### RF-08 Historial de consultas

- **Estado:** Implementado.
- El expediente muestra las consultas registradas para el paciente, sin mezclar datos de otros pacientes.

### RF-09 Receta médica

- **Estado:** Implementado.
- Una receta se vincula a una cita, al paciente (CURP) y al administrador que la emite.
- Contiene un diagnóstico, una lista de medicamentos y un registro de signos vitales.
- Se exporta a PDF (`ControladorPDF.generarPDF`).

### RF-10 Consulta de signos vitales

- **Estado:** Parcial.
- Los signos vitales se guardan y se leen únicamente como parte de una receta.
- Pendiente: CRUD independiente de signos vitales.

### RF-11 Medicamentos de la receta

- **Estado:** Parcial.
- Se pueden insertar, cargar, reemplazar y eliminar junto con la receta.
- Pendiente: actualizar o eliminar un medicamento de forma individual en `BaseDatos`.

### RF-12 Gestión del administrador

- **Estado:** Pendiente.
- Existe solo el registro del administrador por defecto (ver RN-16). La clase `Administrador` define métodos como `registrarPaciente`, `generarReceta` o `iniciarCita` que hoy devuelven valores fijos (`false` o `null`). La lógica real está en los controladores y en `BaseDatos`.
- Pendiente: registrar, consultar, actualizar y eliminar administradores, y conectar el perfil con la interfaz.

### RF-13 Horarios de atención

- **Estado:** Pendiente.
- La tabla `Horario` y la clase `Horario` existen, pero no tienen CRUD ni conexión con la agenda.
- Hoy la agenda usa horas de inicio fijas (ver RN-07).
- Pendiente: CRUD de horarios y uso de los horarios configurados en la agenda.

### RF-14 Bloqueo de horarios

- **Estado:** Pendiente.
- La tabla `Horario_bloqueado` y la clase `HorarioBloqueado` existen, sin CRUD ni efecto en la agenda.
- Pendiente: bloquear periodos y evitar citas dentro de ellos.

### RF-15 Confirmación de cita por correo

- **Estado:** Parcial (`ControladorCorreo`).
- El método `enviarConfirmacionCita` arma y envía un correo HTML con los datos de la cita al correo del paciente.
- La configuración SMTP se toma de `correo.properties` o de variables de entorno. El archivo `correo.properties` está excluido de git.
- Pendiente: llamar al envío desde la agenda al crear o reprogramar una cita. Hoy solo se usa en la clase de prueba `main` de `ControladorCorreo`.

## 3. Reglas de negocio

### Pacientes

- **RN-01 CURP.** Debe tener exactamente 18 caracteres en mayúsculas o dígitos (`[A-Z0-9]{18}`). Es la clave primaria del paciente. La validación no revisa el dígito verificador.
- **RN-02 Fecha de nacimiento.** Es obligatoria y no puede ser futura.
- **RN-03 Teléfono.** Es opcional. Si se captura, debe tener 10 dígitos.
- **RN-04 Correo.** Es opcional. Si se captura, debe tener formato `usuario@dominio.ext` y no exceder 50 caracteres.
- **RN-05 Nombre y apellidos.** El nombre y el apellido paterno son obligatorios. Nombre, apellido paterno y apellido materno no deben exceder 20 caracteres.

### Asistentes

- **RN-06 Asistentes.** La contraseña es obligatoria al registrar un asistente. Al editarlo, un campo vacío conserva la contraseña actual. Las reglas de longitud de nombre y apellidos son las de RN-05.

### Agenda y citas

- **RN-07 Horarios.** Una cita dura 45 minutos. Las horas de inicio disponibles son 09:00, 10:00, 11:00, 12:00, 16:00 y 17:00. Este conjunto está fijo en `AgendaController`.
- **RN-08 Horario ocupado.** No puede haber dos citas no canceladas en la misma fecha y en la misma hora. La comparación se hace por hora completa.
- **RN-09 Citas canceladas.** Una cita cancelada no ocupa su horario. Se puede reprogramar, y al hacerlo vuelve al estado `AGENDADA`.
- **RN-10 Citas atendidas.** Una cita atendida no se puede cancelar ni reprogramar.
- **RN-11 Citas canceladas en curso.** No se puede iniciar la consulta de una cita cancelada.

### Consulta y expediente

- **RN-12 Una consulta y una receta por cita.** Al finalizar de nuevo una misma cita, se actualizan la consulta y la receta existentes. No se crean registros duplicados.
- **RN-13 Finalización de la consulta.** Al finalizar, la cita pasa a `ATENDIDA`. El método `BaseDatos.finalizarConsulta` guarda receta, consulta y estado en una sola transacción, pero la interfaz aún no lo usa (ver RF-07).
- **RN-14 Expediente.** Cada paciente tiene un expediente. Se crea al registrar al paciente, o bien al guardar datos clínicos o finalizar una consulta si todavía no existe.
- **RN-15 Borrado en cascada.** Al eliminar un paciente se eliminan su expediente, sus citas, consultas y recetas.

### Base de datos

- **RN-16 Administrador por defecto.** Si la tabla `Administrador` está vacía al iniciar, se inserta un registro con nombre "Administrador", apellido paterno "Sistema", contraseña `admin` y cédula `00000000`.
- **RN-17 Claves foráneas.** Se activa `PRAGMA foreign_keys = ON` al abrir la conexión.

## 4. Requerimientos no funcionales

- **RNF-01 Plataforma.** Aplicación de escritorio con JavaFX 21, compilada con Maven. Se ejecuta con `mvnw clean javafx:run` o desde el IDE.
- **RNF-02 Versión de Java.** El `README.md` indica JDK 17 o superior, pero `pom.xml` compila con `source` y `target` 8. Pendiente alinear ambos valores.
- **RNF-03 Persistencia local.** Los datos se guardan en el archivo SQLite `citas_medicas.db`, que no se versiona en git.
- **RNF-04 Pruebas.** Las pruebas de JUnit usan una base temporal mediante la propiedad `gestor.db.url`, así que no modifican `citas_medicas.db`. Existen pruebas para expediente, citas, recetas y consultas.
- **RNF-05 Seguridad de contraseñas.** Las contraseñas de asistentes y administradores se guardan en texto plano en SQLite. Pendiente aplicar hash y forzar el cambio de la contraseña por defecto de `Administrador`.
- **RNF-06 Datos clínicos en PDF.** Los PDF de recetas y expedientes se escriben en la carpeta local `pdf` sin cifrado.
- **RNF-07 Idioma.** La interfaz y la documentación están en español.

## 5. Pendientes priorizados

| Prioridad | Requerimiento | Referencia |
|---|---|---|
| Alta | Validar credenciales e imponer permisos por rol | RF-01, RNF-05 |
| Alta | Conectar la finalización de consulta con `finalizarConsulta` | RF-07, RN-13 |
| Media | Enviar la confirmación por correo desde la agenda | RF-15 |
| Media | CRUD de horarios y horarios bloqueados en la agenda | RF-13, RF-14 |
| Media | Gestión de administradores | RF-12 |
| Baja | Eliminar expediente desde la interfaz | RF-04 |
| Baja | CRUD independiente de signos vitales y actualización individual de medicamentos | RF-10, RF-11 |
| Baja | Alinear la versión de Java entre `README.md` y `pom.xml` | RNF-02 |

## 6. Trazabilidad

| Requerimiento | Clases y archivos principales |
|---|---|
| RF-01 | `LoginController`, `login-view.fxml`, `main-view.fxml` |
| RF-02 | `AsistentesController`, `AsistenteEditarController`, `Asistente` |
| RF-03 | `PacientesController`, `PacienteEditarController`, `Paciente` |
| RF-04 | `ExpedienteController`, `Expediente`, `ControladorPDF` |
| RF-05, RF-06 | `AgendaController`, `CitaController`, `Cita`, `Estado` |
| RF-07, RF-09, RF-11 | `ConsultaController`, `consulta-panel.fxml`, `Receta`, `Medicamento`, `SignosVitales` |
| RF-08 | `ExpedienteController`, `Consulta` |
| RF-12 | `Administrador` (métodos sin implementar) |
| RF-13, RF-14 | `Horario`, `HorarioBloqueado`, `DiaSemana` (sin uso en la interfaz) |
| RF-15 | `ControladorCorreo` |
| Persistencia | `BaseDatos` |
