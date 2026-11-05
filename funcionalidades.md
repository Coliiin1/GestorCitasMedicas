# Funcionalidades de la base de datos

Documento de seguimiento de las funcionalidades de conexión a la base de datos que ya están implementadas y las que permanecen pendientes. Debe actualizarse cada vez que se conecte un nuevo módulo a SQLite.

- **Última actualización:** 2026-11-04
- **Base de datos:** `citas_medicas.db` (SQLite)
- **Acceso:** `com.gestor.gestordecitasmedicas.modelo.BaseDatos`

## Leyenda

| Estado | Significado |
|---|---|
| `[x] Implementado` | Operación disponible en la base de datos y conectada a la interfaz. |
| `[~] Parcial` | Existe parte de la operación en la base de datos, pero falta interfaz o alguna operación CRUD. |
| `[ ] Pendiente` | No existe la operación o no está conectada a la interfaz. |

## Conexión y estructura de la base de datos

- `[x]` Creación automática de las tablas al iniciar la aplicación.
- `[x]` Activación de llaves foráneas con `PRAGMA foreign_keys = ON`.
- `[x]` Registro de un administrador por defecto cuando la tabla está vacía.
- `[x]` Cierre seguro de la conexión.
- `[x]` Eliminación en cascada de expedientes, citas, consultas y recetas al eliminar un paciente.

## Funcionalidades implementadas

### Asistentes

- `[x]` Registrar asistente.
- `[x]` Listar asistentes desde la base de datos.
- `[x]` Buscar asistentes por nombre.
- `[x]` Editar datos del asistente.
- `[x]` Eliminar asistente con confirmación.
- `[x]` Conservar la contraseña actual cuando la edición se guarda con el campo vacío.

Interfaz conectada: `AsistentesController`, `AsistenteEditarController`.

### Pacientes

- `[x]` Registrar paciente.
- `[x]` Listar pacientes desde la base de datos.
- `[x]` Buscar pacientes por nombre o CURP.
- `[x]` Editar datos del paciente.
- `[x]` Eliminar paciente con advertencia de borrado en cascada.
- `[x]` Validar CURP, fecha de nacimiento, teléfono y correo.
- `[x]` Crear automáticamente el expediente vacío al registrar un paciente.

Interfaz conectada: `PacientesController`, `PacienteEditarController`.

### Expediente

- `[x]` Consultar el expediente de un paciente por CURP.
- `[x]` Mostrar datos reales del paciente en el expediente.
- `[x]` Mostrar alergias, antecedentes y observaciones registradas.
- `[x]` Crear el expediente automáticamente al dar de alta al paciente.
- `[x]` Editar y guardar alergias, antecedentes familiares/personales y observaciones.
- `[x]` Crear el expediente al guardar datos clínicos si el paciente aún no tiene uno.
- `[x]` Leer el historial de consultas del expediente y las recetas del paciente, con medicamentos y signos vitales.
- `[x]` Exportar los datos reales del expediente y su historial en PDF.
- `[x]` Refrescar el expediente al volver al panel y distinguir errores de lectura de historiales vacíos.

Interfaz conectada: `ExpedienteController`.

### Citas

- `[x]` Conectar la agenda médica al CRUD de citas.
- `[x]` Crear citas desde los espacios disponibles del calendario.
- `[x]` Reprogramar citas.
- `[x]` Cancelar citas y actualizar su estado.
- `[x]` Cargar las citas reales por día o semana en lugar de los datos de ejemplo.
- `[x]` Mostrar el resumen semanal con citas reales.
- `[x]` Conservar la fecha y la hora de la cita en SQLite.
- `[x]` Iniciar la cita clínica desde la ficha de la agenda.

Interfaz conectada: `AgendaController`, `CitaController`, `agenda-panel.fxml`, `cita-panel.fxml`.

### Receta

- `[x]` Conectar el panel de consulta con el guardado de recetas.
- `[x]` Guardar el diagnóstico, los medicamentos y los signos vitales desde la interfaz.
- `[x]` Actualizar una receta existente sin duplicar sus medicamentos.
- `[x]` Generar y exportar la receta en PDF desde la consulta.
- `[x]` Finalizar la consulta y marcar la cita como atendida.

Interfaz conectada: `ConsultaController`, `consulta-panel.fxml`, `ControladorPDF`.

## Funcionalidades pendientes

### Consulta

- `[x]` Implementar métodos CRUD de consultas en `BaseDatos`.
- `[x]` Registrar la consulta al finalizar la atención mediante `finalizarConsulta`.
- `[x]` Asociar la consulta con la cita y el expediente correspondientes.
- `[x]` Resolver el expediente por CURP y crearlo vacío si aún no existe.
- `[x]` Guardar receta, consulta y estado `ATENDIDA` en una sola transacción.
- `[x]` Reutilizar la consulta y la receta existentes al finalizar de nuevo la misma cita.
- `[ ]` Conectar `ConsultaController` con `finalizarConsulta` para guardar motivo, padecimiento y observaciones.
- `[x]` Mostrar el historial de consultas en el expediente.

### Medicamentos y detalle de receta

- `[~]` La base de datos ya permite insertar y cargar medicamentos.
- `[ ]` Implementar la actualización individual de medicamentos.
- `[ ]` Implementar la eliminación individual de medicamentos en `BaseDatos`.
- `[x]` Conectar la captura y eliminación de medicamentos con el panel de consulta.

La actualización de una receta reemplaza sus medicamentos y signos vitales en una sola transacción.

### Signos vitales

- `[~]` Se guardan y cargan únicamente como parte de una receta.
- `[ ]` Implementar métodos CRUD independientes para signos vitales.
- `[x]` Guardar los signos vitales capturados en “Cita en curso”.
- `[x]` Mostrar la evolución de signos vitales en el expediente (mediciones vinculadas a recetas).

### Expediente completo

- `[x]` Conectar la edición del expediente desde la interfaz.
- `[~]` La eliminación de expedientes existe en la base de datos; falta interfaz.
- `[ ]` Conectar la eliminación del expediente desde la interfaz.
- `[x]` Mostrar en el expediente las consultas, recetas y signos vitales del paciente.

Método existente sin uso en la interfaz: `eliminarExpediente`.

### Administrador

- `[~]` Solo existe el registro del administrador por defecto.
- `[ ]` Implementar registro, consulta, actualización y eliminación de administradores.
- `[ ]` Conectar el perfil del administrador con la interfaz.

### Horarios

- `[ ]` Implementar CRUD de horarios en `BaseDatos`.
- `[ ]` Conectar la configuración de horarios con la agenda médica.

### Horarios bloqueados

- `[ ]` Implementar CRUD de horarios bloqueados en `BaseDatos`.
- `[ ]` Bloquear periodos en la agenda y evitar citas en esos horarios.

## Roadmap por fases

| Fase | Alcance | Estado |
|---|---|---|
| Fase 1 | Asistentes | Completada |
| Fase 2 | Pacientes y expediente básico | Completada |
| Fase 3 | Agenda y citas | Completada |
| Fase 4 | Cita en curso y signos vitales | Parcial: captura y guardado con la receta; CRUD independiente pendiente |
| Fase 5 | Consulta, receta y medicamentos | Parcial: backend de consulta, receta, medicamentos y PDF listos; conectar la consulta desde la UI y CRUD individual pendiente |
| Fase 6 | Expediente completo | Parcial: edición, historial y PDF conectados; eliminación independiente pendiente |
| Fase 7 | Administrador y horarios | Pendiente |

## Registro de cambios

### 2026-11-04 (backend de consulta)

- Se agregó el CRUD de consultas en `BaseDatos`: insertar, actualizar, eliminar, cargar por id y cargar por cita.
- `finalizarConsulta` guarda receta, medicamentos, signos vitales, consulta y estado `ATENDIDA` en una sola transacción.
- El expediente se resuelve por CURP y se crea vacío si el paciente aún no tiene uno.
- Una cita conserva una sola consulta y una sola receta: al finalizar de nuevo se actualizan los registros existentes.
- Se agregó `ConsultaTest` con CRUD, lectura por cita/expediente, finalización, creación de expediente y rollback; total 13 pruebas.
- `ConsultaController` aún usa el guardado anterior de receta y estado; queda pendiente llamar a `finalizarConsulta` para persistir motivo, padecimiento y observaciones desde la UI.

### 2026-11-04 (citas y receta)

- Se conectó la agenda médica al CRUD real de citas en SQLite: alta desde espacios libres, reprogramación, cancelación y resumen semanal.
- La cita conserva fecha y hora; la ficha permite iniciar la cita clínica con los datos reales del paciente.
- Se conectó el panel de consulta con el guardado de la receta, sus medicamentos y signos vitales, actualizando sin duplicar detalles.
- Se habilitó la exportación de la receta a PDF y la finalización de la consulta con estado `ATENDIDA`.
- Se mantiene el modelo actual de `Receta` (lista de medicamentos y `SignosVitales`) y no se agregó la columna `motivo` a `Cita`.
- Se agregó `CitaRecetaTest` para alta, hora, reprogramación, cancelación, receta, actualización y eliminación.

### 2026-11-04

- Se integró `main` en `shuha` y se completó la edición clínica del expediente.
- Se agregó la lectura de consultas por expediente y recetas por CURP, sin mezclar pacientes.
- El PDF ahora incluye identidad, alergias, antecedentes, consultas, medicamentos y signos vitales reales.
- Se agregaron pruebas de persistencia tras reabrir SQLite, aislamiento de pacientes, expediente vacío/faltante, fechas SQLite, editor JavaFX y contenido del PDF.
- Ejecutar `./mvnw.cmd test` con `JAVA_HOME` apuntando a un JDK compatible. Las pruebas usan una base temporal mediante `gestor.db.url`; no modifican `citas_medicas.db`.
- La captura de consultas y recetas desde la atención médica sigue pendiente; el expediente muestra los registros que ya existan en SQLite.

### 2026-11-03

- Se completó la conexión de la base de datos con las pantallas de Asistentes y Pacientes.
- Se agregó la lectura de expedientes por CURP.
- Se agregó la creación automática del expediente al registrar un paciente.
- Se documentaron las funcionalidades pendientes de citas, consultas, recetas, signos vitales, administrador y horarios.
