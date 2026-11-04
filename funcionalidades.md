# Funcionalidades de la base de datos

Documento de seguimiento de las funcionalidades de conexión a la base de datos que ya están implementadas y las que permanecen pendientes. Debe actualizarse cada vez que se conecte un nuevo módulo a SQLite.

- **Última actualización:** 2026-11-03
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

Interfaz conectada: `ExpedienteController`.

## Funcionalidades pendientes

### Citas

- `[~]` La base de datos ya permite insertar, actualizar, eliminar y cargar citas.
- `[ ]` Conectar la agenda médica al CRUD de citas.
- `[ ]` Crear citas desde los espacios disponibles del calendario.
- `[ ]` Reprogramar citas.
- `[ ]` Cancelar citas y actualizar su estado.
- `[ ]` Cargar las citas reales por día o semana en lugar de los datos de ejemplo.
- `[ ]` Mostrar el resumen semanal con citas reales.

Métodos existentes sin uso en la interfaz: `insertarCita`, `actualizarCita`, `eliminarCita`, `cargarCitas`.

### Consulta

- `[ ]` Implementar métodos CRUD de consultas en `BaseDatos`.
- `[ ]` Registrar la consulta al finalizar la atención.
- `[ ]` Asociar la consulta con la cita y el expediente correspondientes.
- `[ ]` Mostrar el historial de consultas en el expediente.

### Receta

- `[~]` La base de datos ya permite insertar, eliminar y cargar recetas.
- `[ ]` Conectar el panel de consulta con el guardado de recetas.
- `[ ]` Guardar el diagnóstico y los medicamentos desde la interfaz.
- `[ ]` Generar y exportar la receta en PDF desde la consulta.

Métodos existentes sin uso en la interfaz: `insertarReceta`, `eliminarReceta`, `cargarRecetas`.

### Medicamentos y detalle de receta

- `[~]` La base de datos ya permite insertar y cargar medicamentos.
- `[ ]` Implementar la actualización de medicamentos.
- `[ ]` Implementar la eliminación individual de medicamentos.
- `[ ]` Conectar la captura y eliminación de medicamentos con el panel de consulta.

Métodos existentes sin uso en la interfaz: `insertarMedicamento`, `cargarMedicamentos`.

### Signos vitales

- `[~]` Se guardan y cargan únicamente como parte de una receta.
- `[ ]` Implementar métodos CRUD independientes para signos vitales.
- `[ ]` Guardar los signos vitales capturados en “Cita en curso”.
- `[ ]` Mostrar la evolución de signos vitales en el expediente.

### Expediente completo

- `[~]` La base de datos ya permite actualizar y eliminar expedientes.
- `[ ]` Conectar la edición del expediente desde la interfaz.
- `[ ]` Conectar la eliminación del expediente desde la interfaz.
- `[ ]` Mostrar en el expediente las consultas, recetas y signos vitales del paciente.

Métodos existentes sin uso en la interfaz: `actualizarExpediente`, `eliminarExpediente`.

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
| Fase 3 | Agenda y citas | Pendiente |
| Fase 4 | Cita en curso y signos vitales | Pendiente |
| Fase 5 | Consulta, receta y medicamentos | Pendiente |
| Fase 6 | Expediente completo | Pendiente |
| Fase 7 | Administrador y horarios | Pendiente |

## Registro de cambios

### 2026-11-03

- Se completó la conexión de la base de datos con las pantallas de Asistentes y Pacientes.
- Se agregó la lectura de expedientes por CURP.
- Se agregó la creación automática del expediente al registrar un paciente.
- Se documentaron las funcionalidades pendientes de citas, consultas, recetas, signos vitales, administrador y horarios.
