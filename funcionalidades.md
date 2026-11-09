# Funcionalidades, flujo clínico y cambios de la base de datos

Documento de seguimiento del flujo clínico, sus pantallas, exportaciones y persistencia en SQLite. Distingue implementación, verificación y pendientes. Debe actualizarse al conectar o modificar un módulo.

- **Última actualización:** 2026-11-06
- **Base de datos:** `citas_medicas.db` (SQLite)
- **Acceso:** `com.gestor.gestordecitasmedicas.modelo.BaseDatos`

Plan técnico detallado de los pendientes: [Plan de implementación por entregas](docs/plan-implementacion-pendientes.md). Incluye dependencias, cambios propuestos de interfaz/base de datos, recuperación ante fallos y criterios de aceptación; todavía no está implementado.

## Leyenda

| Estado | Significado |
|---|---|
| `[x] Implementado` | Operación disponible en la base de datos y conectada a la interfaz. |
| `[~] Parcial` | Existe parte de la operación en la base de datos, pero falta interfaz o alguna operación CRUD. |
| `[ ] Pendiente` | No existe la operación o no está conectada a la interfaz. |

## Revisión de integración del 2026-11-06

**Resultado:** el camino principal de cita → consulta → guardado → historial está conectado y ambos generadores PDF funcionan con los datos de prueba. No se declara el flujo completamente cerrado: quedan los hallazgos de integración descritos abajo y la configuración real del profesional/establecimiento. Las 22 pruebas actuales pasan, pero no cubren todas las transiciones de recepción ni equivalen a una certificación normativa o a una validación del consultorio real.

### Flujo declarado y comportamiento comprobado

| Paso | Entrada y destino | Estado y observaciones |
|---|---|---|
| Alta del paciente | `PacienteEditarController` → `Paciente`; creación de `Expediente` vacío | Conectado. También puede iniciarse desde la agenda; al guardar se retoma la cita con fecha/hora y paciente preseleccionado. La creación de paciente y expediente son operaciones separadas; existe aviso si solo se registra el paciente. |
| Agendar/reprogramar/cancelar | `AgendaController` → CRUD de `Cita` | Conectado. Si el paciente no está registrado, la agenda abre el alta y después reabre la cita. La cita referencia la CURP del paciente y conserva fecha/hora; no duplica sus datos demográficos. |
| Horario y bloqueos | `AgendaController` → botón admin → `HorarioConfigController` | Conectado y visible solo para administrador. Configura tramos semanales con varios bloques por día y bloqueos por rango de fecha/hora. Las citas duran 60 minutos y no se agendan fuera de los tramos activos ni dentro de bloqueos. |
| Iniciar atención | `AgendaController.onIniciarCita` → `CitaController.cargarCita` | Conectado para el rol administrador. Carga al paciente desde SQLite y signos de una receta previa, si existe. Las citas canceladas no se inician desde la ficha. |
| Recepción y signos | `cita-panel.fxml` → `leerSignos()` → `ConsultaController.cargarConsulta` | Conectado para signos de una atención nueva. El motivo de recepción no se transfiere; revisar también la prioridad de signos al reabrir. |
| Consulta y receta | `consulta-panel.fxml` → `guardarAtencion` del controlador | Conectado. Captura clínica separada, identidad automática de solo lectura y medicamentos estructurados. |
| Datos fijos | `BaseDatos.cargarDatosPrescriptor` → `Administrador` | Conectado. Las nuevas recetas usan el administrador predeterminado; una receta existente conserva su prescriptor salvo que le falte cédula. No hay edición de esos datos en la receta. |
| Guardar y exportar PDF | `onGuardarPdf` → `guardarAtencion(false, true)` → `ControladorPDF.generarPDF` | Primero guarda la atención y después genera el archivo. Exige al menos un medicamento. No marca la cita como atendida. |
| Finalizar | `onFinalizar` → `guardarAtencion(true, false)` | Guarda y marca `ATENDIDA` en la misma transacción; vuelve a la agenda. Admite atención sin medicamentos. **No genera ni actualiza el PDF automáticamente.** |
| Consultar expediente | Desde pacientes o atención → `ExpedienteController.mostrarPaciente` | Carga identidad, historia clínica, consultas, recetas y signos desde SQLite. El historial se obtiene por expediente y CURP. |
| Editar expediente | Botón Editar → panel integrado → guardar/cancelar | Conectado. Valida el formato de fecha/hora; guarda, recarga y muestra confirmación verde durante cinco segundos. Cancelar no persiste cambios. |
| Exportar expediente | `exportarPdf` → selector de archivo → `generarExpediente` | Conectado. Usa el expediente cargado y permite elegir destino; no modifica el estado de la cita. |

La navegación actual distingue los roles provisionales `admin` y `asis`: el administrador inicia atención y el asistente queda limitado a agenda, aunque ambos pueden abrir el alta de paciente desde el diálogo de cita. El login decide el rol por el texto ingresado, sin autenticar la contraseña contra SQLite. El menú no ofrece un listado independiente de recetas: el acceso operativo a la consulta parte de una cita.

### Hallazgos que impiden declarar el flujo cerrado

| Prioridad | Hallazgo confirmado por lectura del código | Consecuencia y pendiente |
|---|---|---|
| Alta | `CitaController.onSiguiente` no envía `txtMotivo`; el guardado de consulta asigna `motivo = "Consulta externa"`. Al recargar recepción se usa el diagnóstico de receta en ese campo. | El texto real del motivo de recepción no se conserva. Transferirlo y recuperarlo desde `Consulta`, sin confundirlo con el diagnóstico. |
| Alta | Si ya existe una receta con signos, `ConsultaController.cargarConsulta` reemplaza los signos recibidos por los guardados. | Los signos recién corregidos en recepción pueden descartarse al reabrir una atención. Definir y probar la prioridad entre captura nueva y datos previos. |
| Media | Los conversores numéricos de recepción usan cero ante valores vacíos o inválidos. | Puede guardarse cero sin distinguir ausencia de medición y error de captura. Añadir validación explícita y tratamiento de valores no medidos. |
| Media | La acción Finalizar no exporta; el PDF se genera únicamente con Guardar y exportar. | Si se exporta, se cambia el tratamiento y luego solo se finaliza, el archivo anterior no refleja los últimos cambios. Regenerarlo explícitamente o completar la sincronización de emisión. |
| Media | Las recetas usan siempre `pdf/receta_<id>.pdf`; volver a exportar reemplaza ese archivo. | El historial SQLite no es un archivo de versiones PDF firmadas. Completar la custodia del original y el versionado de documentos emitidos. |

Estos hallazgos se documentan como pendientes; esta revisión no los marca corregidos. Además, no hay restricciones `UNIQUE` de una receta/consulta por cita: el flujo de interfaz reutiliza los identificadores existentes, pero esa conducta no garantiza unicidad para escrituras externas o simultáneas.

## Formatos y exportaciones PDF

### Receta médica ordinaria

- Generador: `ControladorPDF.generarPDF(Receta)`; la sobrecarga `generarReceta(File, Receta)` permite indicar un archivo y se usa en pruebas.
- Destino de la interfaz: `pdf/receta_<id_receta>.pdf`, relativo al directorio desde el que se ejecuta la aplicación. Se crea la carpeta si no existe y se informa la ruta absoluta al usuario.
- Formato A4, con identificación del documento/paciente en cada página, aviso de confidencialidad, estado pendiente de firma y número de página.
- Contenido: nombre del prescriptor, profesión, cédula, institución del título, domicilios, establecimiento, especialidad/registro cuando corresponda; folio, fecha/hora, paciente, edad, sexo, CURP y diagnóstico.
- Por medicamento: denominación genérica, marca opcional, concentración, presentación, dosis, vía, frecuencia, duración, cantidad y fracción declarada. Las tablas pueden continuar en otras páginas, conservando la identificación del medicamento.
- Incluye indicaciones generales y seguimiento, además del espacio para firma autógrafa. El nombre impreso no constituye firma electrónica.
- La validación rechaza la receta incompleta, la cédula de ejemplo formada por ceros y las fracciones fuera del alcance ordinario IV/V/VI. Es validación de captura; no comprueba autenticidad de cédula ni idoneidad clínica del tratamiento.
- Los signos vitales se conservan en la atención y el expediente; no se imprimen como una sección de esta receta. El diagnóstico y las indicaciones ya no se mezclan en un único campo.
- Si falla el PDF después de guardar, la atención permanece en SQLite y se muestra el error de exportación: el guardado de base de datos y la escritura del archivo no son una sola transacción.

### Expediente clínico

- Generador: `ControladorPDF.generarExpediente(File, Expediente)`.
- Destino: elegido mediante `FileChooser`; nombre propuesto `expediente_<CURP>.pdf`.
- Formato A4 con identificación y numeración en cada página, pie de confidencialidad y firma pendiente.
- Contenido: ficha del paciente, fecha de apertura, edad a la apertura, historia clínica ambulatoria, alergias, notas de evolución, recetas/medicamentos y evolución de signos vitales.
- La historia incluye establecimiento, interrogatorio/antecedentes, consumo de sustancias, padecimiento y tratamientos previos, exploración, signos, estudios, diagnóstico, pronóstico, terapéutica, autor, cédula y fecha/hora. Cada nota contiene sus datos de atención y espacio de firma.
- Las historias incompletas se identifican como **BORRADOR**; los datos ausentes se muestran como no registrados. La exportación del expediente permite revisar información incompleta y no equivale a validar o firmar su contenido.
- La sección de recetas del expediente es una transcripción del historial, no la inserción de los PDF de receta firmados. Las versiones históricas `Version_*` tampoco se imprimen automáticamente.
- La pantalla recarga el expediente al entrar y tras guardar. El exportador utiliza ese objeto cargado; no ofrece sincronización en tiempo real con cambios externos hechos directamente en SQLite.

### Verificación realizada

- Se ejecutó `.\mvnw.cmd test` con bases temporales: **22 pruebas aprobadas**, sin fallos ni errores.
- `AgendaRegistroPacienteTest`: 1 prueba del alta de paciente desde la agenda, navegación al formulario y conservación de la CURP para retomar la cita.
- `CitaRecetaTest`: 3 pruebas de citas/fecha-hora, guardado/actualización de receta y bajas.
- `ConsultaTest`: 4 pruebas de CRUD, finalización, creación de expediente faltante y rollback.
- `ExpedienteTest`: 6 pruebas de recarga, aislamiento de pacientes, fechas, PDF, panel de edición, guardar/cancelar y ocultamiento real de la confirmación a los cinco segundos. También comprueba captura/recarga/finalización de la consulta desde JavaFX y carga de datos fijos.
- `FormatoClinicoTest`: 8 pruebas de validación, atención sin fármacos, persistencia y versiones, rollback, aislamiento entre citas, PDF de una o varias páginas, historia clínica y migración antigua.
- Se regeneraron y revisaron visualmente la receta de un medicamento (una página), la receta de seis medicamentos (dos páginas), el expediente con historial y la historia completa (dos páginas en los ejemplos). Se comprobaron legibilidad, tablas, continuidad, firma e identificación.
- Las pruebas usan datos ficticios y la propiedad `gestor.db.url`; no validan los datos reales del médico/establecimiento ni la impresión física y firma. Tampoco comprueban que se transfiera el motivo de recepción o se respeten los signos nuevos al reabrir: esos casos son los pendientes anteriores.

## Cambios detallados de base de datos y persistencia

### Migración de formatos clínicos

`MigracionClinica.aplicar(Connection)` se ejecuta al abrir la conexión, después de crear las tablas base. Consulta `PRAGMA table_info` y agrega únicamente las columnas que faltan mediante `ALTER TABLE ... ADD COLUMN ... TEXT`. La inicialización y la migración están dentro de una transacción; si fallan se hace rollback y se cierra la conexión fallida. Reabrir una base ya migrada no vuelve a crear esas columnas ni sustituye los datos existentes.

Se añadieron **53 columnas TEXT en cinco tablas**. Los valores previos permanecen y las nuevas columnas de filas antiguas pueden quedar `NULL`; la migración no inventa datos clínicos ni del profesional.

| Tabla | Columnas añadidas | Uso |
|---|---|---|
| `Administrador` (5) | `profesion`, `especialidad`, `registro_especialidad`, `nombre_establecimiento`, `domicilio_establecimiento` | Datos fijos de emisión. Reutiliza `nombre`, apellidos, `cedula`, `institución` y `domicilio` existentes. |
| `Expediente` (19) | `tipo_establecimiento`, `nombre_establecimiento`, `domicilio_establecimiento`, `institucion_establecimiento`, `propietario`, `grupo_etnico`, `antecedentes_no_patologicos`, `consumo_sustancias`, `padecimiento_actual`, `interrogatorio_sistemas`, `exploracion_fisica`, `resultados_estudios`, `diagnostico`, `pronostico`, `indicacion_terapeutica`, `autor_historia`, `cedula_historia`, `fecha_hora_historia`, `signos_historia` | Historia clínica ambulatoria. Reutiliza antecedentes familiares/personales, alergias y observaciones anteriores. |
| `Consulta` (11) | `evolucion`, `exploracion_fisica`, `resultados_estudios`, `diagnostico`, `pronostico`, `indicaciones`, `nombre_medico`, `cedula_medico`, `nombre_paciente`, `edad_paciente`, `sexo_paciente` | Nota de evolución y datos de identidad de la atención. Conserva las columnas previas de motivo, padecimiento, observaciones y fecha. |
| `Receta` (13) | `nombre_medico`, `cedula_medico`, `profesion`, `institucion_titulo`, `domicilio_medico`, `especialidad`, `registro_especialidad`, `nombre_establecimiento`, `domicilio_establecimiento`, `nombre_paciente`, `edad_paciente`, `sexo_paciente`, `indicaciones` | Datos impresos guardados con la receta para evitar que cambios posteriores del perfil alteren el documento original. |
| `Detalle_receta` (5) | `presentacion`, `via_administracion`, `denominacion_distintiva`, `cantidad`, `grupo_regulatorio` | Completa los campos existentes de nombre, concentración, dosis, frecuencia y duración. |

No se renombró `institución` en `Administrador`. El modelo `Medicamento` conserva por compatibilidad el método heredado `getDuradcion`/`setDuradcion`, que corresponde a la columna `duracion`; el formulario muestra “Duración”. Los datos fijos de `Administrador` y los del establecimiento del expediente son almacenamientos separados: no se sincronizan automáticamente.

### Versiones anteriores

Se crearon cinco tablas: `Version_Expediente`, `Version_Consulta`, `Version_Receta`, `Version_Detalle_receta` y `Version_Signos_vitales`.

Cada una conserva las columnas de su tabla original y agrega `archivado_en` y `operacion`. Diez disparadores —`version_<Tabla>_UPDATE` y `version_<Tabla>_DELETE`— copian el registro **anterior** antes de actualizarlo o borrarlo. La fecha se registra en UTC y la operación es `UPDATE` o `DELETE`. Las versiones también se revierten si falla la transacción de la que forman parte.

Las tablas de versiones no tienen las bajas en cascada de los registros operativos; permiten conservar el contenido anterior al reemplazo de detalles y otras bajas. No reconstruyen cambios anteriores a su creación, no registran la identidad autenticada de quien realizó la operación y no constituyen una bitácora inviolable. Aún no hay una pantalla de consulta/restauración de versiones ni una política automatizada de archivo.

### Relaciones y operaciones

- Se mantienen las relaciones: paciente por CURP → citas/expedientes; consulta → cita/expediente; receta → cita/administrador; medicamentos y signos → receta. No se añadió columna `motivo` a `Cita`.
- `guardarAtencion(cita, consulta, receta, finalizar)` comprueba la CURP real de la cita y que una receta existente pertenezca a esa cita, resuelve o crea el expediente, guarda los detalles y la nota, y solo si `finalizar` es verdadero actualiza `ATENDIDA`.
- `finalizarConsulta` permanece como método de conveniencia que llama a `guardarAtencion(..., true)`; la interfaz usa `guardarAtencion` para poder exportar sin finalizar.
- Al fallar, se hace rollback, se restaura autocommit y se recuperan los identificadores originales de receta, consulta y expediente en los objetos de esa operación.
- Las actualizaciones reemplazan medicamentos/signos de la receta dentro de la transacción; las versiones conservan sus valores anteriores. El flujo normal actualiza la nota existente de la misma cita.
- Las fechas de receta se escriben como timestamp para conservar hora. La lectura acepta representaciones SQLite previas; la fecha/hora de historia se conserva como texto ISO y el editor valida ese formato.
- `cargarDatosPrescriptor` lee el administrador vinculado o, para una nueva receta sin identificador, el primero por `id_administrador`. No vincula todavía la emisión con un profesional autenticado.
- Las lecturas del expediente incluyen los campos nuevos y asocian cada consulta del historial con la receta/signos de su cita. Los datos faltantes se distinguen de un historial vacío y los errores de lectura no se presentan como ausencia de consultas.
- No se modificaron las bajas operativas en cascada ni se agregó una restricción de conservación por antigüedad. Deben revisarse antes de declarar completa la custodia clínica.

## Cambios detallados de interfaz y formatos realizados

| Área | Cambios |
|---|---|
| Receta e indicaciones | Identificación automática del paciente al inicio, campos de solo lectura, edad a la emisión, carga de SQLite por CURP y conservación de identidad previa cuando ya existe una receta. |
| Datos del profesional | Retiro de los campos manuales de prescriptor/establecimiento del formulario; lectura de los datos fijos desde `Administrador`, manteniéndolos en el PDF. |
| Nota clínica | Captura independiente de diagnóstico, padecimiento, evolución, exploración, estudios, pronóstico e indicaciones; el guardado deja de concatenar todo en diagnóstico. |
| Medicamentos | Filas con campos independientes y eliminación desde la interfaz; contador de fármacos, elección manual de fracción y validación del alcance ordinario. |
| Expediente, lectura | Pestañas separadas visualmente para consultas, recetas e historia/signos; ficha del paciente y alertas clínicas visibles. |
| Expediente, edición | Panel integrado en la misma vista, sin diálogo modal de edición; cinco grupos en dos columnas: establecimiento, interrogatorio/antecedentes, padecimiento/exploración, estudios/diagnóstico/tratamiento, autor/fecha. |
| Campos de edición | Campos cortos de una línea para datos administrativos y autoría; áreas multilínea para narración clínica; 23 títulos resaltados, ayudas de captura y fecha ISO validada. |
| Estilo | Encabezado de edición y cinco títulos de grupo con fondo celeste, borde y texto destacado, usando la paleta existente. |
| Retroalimentación | Errores de fecha o guardado dentro del editor; confirmación verde fija al volver al expediente tras guardar. Desaparece a los cinco segundos y cada nuevo guardado reinicia el temporizador. |
| Exportaciones | Receta ordinaria estructurada con firma pendiente; expediente con historia, notas, medicamentos y signos; identificación y numeración en continuaciones. |
| Documentación | `docs/normativa-mexico.md` con trazabilidad y límites de alcance; este archivo con flujo real, pendientes, esquema y verificación. |

Los archivos principales involucrados son `ConsultaController`, `ExpedienteController`, `BaseDatos`, `MigracionClinica`, `ValidacionClinica`, `TextoExpediente`, `ControladorPDF`, los modelos `Expediente`/`Consulta`/`Receta`/`Medicamento`, las vistas `consulta-panel.fxml`/`expediente-panel.fxml`, `cita.css` y las pruebas de expediente/formato clínico. En esta revisión de confirmación se actualiza documentación; no se corrigen ni se dan por resueltos los hallazgos de recepción.

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
- `[x]` Registrar un paciente nuevo desde el diálogo de cita y regresar a la agenda.

Interfaz conectada: `PacientesController`, `PacienteEditarController`, `AgendaController`.

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
- `[x]` Editar en un panel integrado, con guardar/cancelar, sin ventana de edición emergente.
- `[x]` Agrupar y estandarizar los campos clínicos y administrativos, con títulos resaltados y ayudas de captura.
- `[x]` Separar visualmente las pestañas de consultas, recetas e historia clínica.
- `[x]` Resaltar en celeste el encabezado y los cinco apartados de edición.
- `[x]` Mostrar confirmación verde al guardar y ocultarla a los cinco segundos, reiniciando el plazo en cada guardado.

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
- `[x]` Registrar un paciente nuevo al agendar y reabrir la cita conservando fecha, hora y paciente.

Interfaz conectada: `AgendaController`, `CitaController`, `PacienteEditarController`, `agenda-panel.fxml`, `cita-panel.fxml`, `paciente-editar-panel.fxml`.

### Receta

- `[x]` Conectar el panel de consulta con el guardado de recetas.
- `[x]` Guardar el diagnóstico, los medicamentos y los signos vitales desde la interfaz.
- `[x]` Actualizar una receta existente sin duplicar sus medicamentos.
- `[x]` Generar y exportar la receta en PDF desde la consulta.
- `[x]` Finalizar la consulta y marcar la cita como atendida.
- `[x]` Mostrar primero la identificación del paciente, automática y de solo lectura desde SQLite.
- `[x]` Cargar los datos fijos del prescriptor y establecimiento desde `Administrador`, sin pedirlos en cada receta.
- `[x]` Capturar presentación, concentración, dosis, vía, frecuencia, duración, cantidad y fracción regulatoria por medicamento.
- `[x]` Validar campos necesarios antes de emitir y excluir las fracciones I, II y III del formato ordinario.
- `[x]` Guardar nota clínica, receta, medicamentos y signos vitales en una sola transacción y conservar versiones anteriores.
- `[x]` Conservar la identificación original de recetas ya guardadas y preparar PDF para firma autógrafa.

Interfaz conectada: `ConsultaController`, `consulta-panel.fxml`, `ControladorPDF`.

#### Pendientes para cerrar el módulo de recetas ordinarias

Este listado define el cierre funcional del módulo. No equivale a afirmar cumplimiento normativo integral ni incluye recetarios especiales. Ver [alcance normativo y obligaciones operativas](docs/normativa-mexico.md).

- `[ ]` Corregir la transferencia y recarga del motivo de recepción hacia `Consulta.motivo`.
- `[ ]` Corregir y probar la prioridad de los signos recién capturados al reabrir una cita con receta existente.
- `[ ]` Validar los números de recepción sin convertir silenciosamente errores o ausencias en cero.
- `[ ]` Definir la regeneración del PDF al finalizar una atención modificada y la conservación de documentos emitidos anteriores.
- `[ ]` Configurar los datos reales de médico y establecimiento en `Administrador`; el registro de ejemplo no permite emitir. Completar nombre y apellidos, cédula, profesión, institución del título, domicilio, establecimiento y, cuando aplique, especialidad y registro.
- `[ ]` Añadir una pantalla para consultar y actualizar esa configuración fija sin editar SQLite manualmente; validar antes de guardar y mostrar qué dato falta.
- `[ ]` Conectar el inicio de sesión y los permisos al profesional responsable. Actualmente se usa el administrador predeterminado para las recetas nuevas.
- `[ ]` Completar el acceso a recetas desde el menú: buscar paciente/cita, listar recetas, abrir una receta previa y reimprimirla sin exigir volver a iniciar la atención.
- `[ ]` Separar guardar borrador de emitir la receta; definir cuándo puede corregirse y cómo registrar el motivo y autor de una corrección o cancelación sin perder el original.
- `[ ]` Completar el flujo de impresión y custodia del original firmado. El PDF actual está pendiente de firma autógrafa; escribir el nombre no firma el documento.
- `[ ]` Ofrecer consulta de revisiones y registro de accesos/exportaciones desde la interfaz, con permisos por rol. Las tablas de versiones ya conservan cambios, pero no hay una pantalla para consultarlos.
- `[ ]` Definir respaldo, recuperación, archivo y conservación de expedientes/recetas; revisar las bajas existentes para no perder documentación que deba conservarse.
- `[ ]` Validar el flujo completo con los datos reales del consultorio: cita registrada → captura clínica → receta → impresión → firma → consulta/reimpresión, incluidos errores de configuración y fallos de guardado.

**Fuera del alcance actual:** emisión de fracciones I, II y III, firma electrónica/digital e integración con dispensación de farmacia. Si se amplía a esos servicios, se requiere un flujo propio; no deben habilitarse solo quitando la validación. El catálogo regulatorio y la revisión clínica de dosis, unidades, alergias e interacciones siguen a cargo del profesional; no existe un motor de verificación clínica automática.

## Funcionalidades pendientes

### Consulta

- `[x]` Implementar métodos CRUD de consultas en `BaseDatos`.
- `[x]` Registrar la consulta al finalizar la atención mediante `finalizarConsulta`.
- `[x]` Asociar la consulta con la cita y el expediente correspondientes.
- `[x]` Resolver el expediente por CURP y crearlo vacío si aún no existe.
- `[x]` Guardar receta, consulta y estado `ATENDIDA` en una sola transacción.
- `[x]` Reutilizar la consulta y la receta existentes al finalizar de nuevo la misma cita.
- `[x]` Conectar `ConsultaController` con `guardarAtencion` para persistir la nota clínica y la receta en una sola transacción.
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

- `[~]` Existe el administrador por defecto y la lectura de sus datos fijos para recetas; la configuración visual del perfil sigue pendiente.
- `[ ]` Implementar registro, consulta, actualización y eliminación de administradores.
- `[ ]` Conectar el perfil del administrador con la interfaz.

### Horarios

- `[x]` Implementar CRUD de horarios en `BaseDatos`.
- `[x]` Conectar la configuración de horarios con la agenda médica.
- `[x]` Permitir varios tramos por día y generar citas de 60 minutos.
- `[x]` Restringir la configuración al rol administrador y abrirla desde la agenda.

Interfaz conectada: `HorarioConfigController`, `AgendaController`, `agenda-panel.fxml`.

### Horarios bloqueados

- `[x]` Implementar CRUD de horarios bloqueados en `BaseDatos`.
- `[x]` Bloquear periodos en la agenda y evitar citas en esos horarios.
- `[x]` Usar rangos de fecha y hora con motivo, independientes de un tramo semanal.
- `[x]` Rechazar bloqueos o cambios de horario que dejen citas activas en el periodo.

Interfaz conectada: `HorarioConfigController`, `AgendaController`, `PlanificadorAgenda`, `MigracionAgenda`.

## Roadmap por fases

| Fase | Alcance | Estado |
|---|---|---|
| Fase 1 | Asistentes | Completada |
| Fase 2 | Pacientes y expediente básico | Completada |
| Fase 3 | Agenda y citas | Completada |
| Fase 4 | Cita en curso y signos vitales | Parcial: captura y guardado con la receta; CRUD independiente pendiente |
| Fase 5 | Consulta, receta y medicamentos | Parcial: captura, guardado transaccional y PDF conectados; pendientes de configuración, acceso, emisión y custodia detallados en Receta |
| Fase 6 | Expediente completo | Parcial: edición, historial y PDF conectados; eliminación independiente pendiente |
| Fase 7 | Administrador y horarios | Parcial: horarios y bloqueos conectados; gestión de administradores pendiente |

## Registro de cambios

Las entradas antiguas describen el estado de cada fecha. Para el estado actual y sus límites, prevalecen la revisión de integración y los pendientes de este documento.

### 2026-11-09

- Se agregó la configuración de horario semanal y bloqueos desde la agenda, visible solo para administradores.
- La agenda pasó de horas fijas y citas de 45 minutos a tramos configurables con citas de 60 minutos.
- Los bloqueos ahora son rangos independientes de fecha/hora con motivo y se migró la tabla anterior.
- Se impide modificar horarios, eliminarlos o bloquear periodos cuando hay citas activas en el rango; las citas no se cancelan automáticamente.
- Se agregaron `PlanificadorAgendaTest` (5 pruebas) y `HorarioBaseDatosTest` (3 pruebas); ambas clases pasan de forma aislada.

### 2026-11-08

- Se agregó el alta de paciente desde el diálogo de cita para administradores y asistentes.
- Al guardar el paciente, la agenda reabre la cita con la fecha, la hora y el paciente preseleccionado; al cancelar, vuelve a la agenda sin reabrir el diálogo.
- El formulario de paciente conserva su retorno normal al listado cuando se abre desde el menú.
- Se agregó `AgendaRegistroPacienteTest`; total 22 pruebas.

### 2026-11-06

- Se auditó el flujo desde agenda hasta consulta, guardado e historial; se distinguieron las acciones de exportar y finalizar.
- Se documentaron 53 columnas nuevas en cinco tablas, cinco tablas de versiones y diez disparadores, además del guardado transaccional y la compatibilidad de fechas.
- Se comprobaron las 21 pruebas y se regeneraron/revisaron visualmente los PDF de receta y expediente con datos ficticios.
- Se registraron como pendientes la transferencia de motivo, la prioridad de signos al reabrir, la validación numérica de recepción y la sincronización/custodia de PDF.
- Se aplicó celeste al encabezado y a los cinco apartados de edición del expediente, con confirmación verde fija que desaparece a los cinco segundos.
- Se documentó el estado real y la lista de cierre del módulo de recetas ordinarias.
- La identificación del paciente se carga automáticamente al inicio de la receta y los datos fijos del prescriptor se toman de `Administrador`.
- La edición del expediente ahora utiliza un panel dentro de la aplicación, con campos agrupados, títulos destacados y errores de fecha/guardado en el propio panel.
- Se separaron visualmente las pestañas del expediente y se adaptaron las pruebas de guardar, cancelar y recuperar un expediente faltante.

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
