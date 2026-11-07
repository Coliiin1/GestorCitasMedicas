# Plan de implementación de funcionalidades pendientes

Fecha de elaboración: 6 de noviembre de 2026. Rama revisada: `shuha`.

**Estado: plan propuesto, sin implementar.** Los nombres de métodos, tablas y estados que este documento propone no deben confundirse con funciones ya disponibles. El estado real y el inventario de cambios existentes están en [funcionalidades.md](../funcionalidades.md); el alcance de los formatos está en [normativa-mexico.md](normativa-mexico.md).

## 1. Objetivo y alcance

Cerrar el flujo de consulta externa y receta ordinaria: registrar un paciente, agendar, capturar recepción sin perder datos, elaborar una nota, guardar borradores, finalizar, preparar el PDF correcto, imprimirlo, conservarlo y recuperarlo posteriormente. Completar también los pendientes de configuración, seguridad, agenda y archivo que afectan a ese flujo.

Se conserva JavaFX, SQLite, Maven e iText ya presentes. No se propone migrar de tecnología, añadir una API, microservicios o infraestructura en la nube. Los archivos Java se encuentran bajo `src/main/java/com/gestor/gestordecitasmedicas/`; las vistas y `cita.css`, bajo `src/main/resources/com/gestor/gestordecitasmedicas/`. Los nombres de archivos de las siguientes secciones son relativos a esos directorios.

La revisión anterior ejecutó 21 pruebas aprobadas. Son una base de regresión; los casos nuevos de este plan requieren pruebas adicionales. No se atribuye a esas 21 pruebas cobertura de funciones aún ausentes.

**Fuera del cierre inicial:** recetarios especiales, firma electrónica/digital, integración de dispensación, cálculo automático de tratamientos e intercambio con sistemas externos. Mantener sus límites visibles. Ampliarlos requiere un análisis y una implementación separados.

## 2. Orden de trabajo y dependencias

| Entrega | Prioridad | Resultado | Depende de |
|---|---|---|---|
| E0 | Previa | Base de pruebas y migración recuperable | Ninguna |
| E1 | Crítica | Motivo, signos y formulario conservados entre pantallas | E0 |
| E2 | Crítica | Validación de mediciones y ausencia de datos sin ceros inventados | E1 |
| E3 | Alta | Configuración visual del profesional/establecimiento | E0; integrar sesión en E4 |
| E4 | Alta | Autenticación real y permisos vinculados al responsable | E3 |
| E5 | Alta | Borradores, finalización, correcciones y unicidad | E1, E2, E4 |
| E6 | Alta | Documentos PDF versionados, emisión y reimpresión exacta | E5 |
| E7 | Alta | Listado de recetas y acceso a documentos desde el expediente | E6 |
| E8 | Alta | Archivo, versiones, bitácora y respaldos recuperables | E4; consolidar después de E6 |
| E9 | Media | Horarios y bloqueos conectados a agenda | E0, E4 |
| E10 | Media | Altas y bajas administrativas coherentes | E4, E8 |
| E11 | Cierre | Prueba integral, impresión y documentación final | Todas las entregas incluidas en la versión |

Priorizar E1 y E2 antes de añadir nuevas pantallas: hoy existe riesgo de descartar el motivo o una medición corregida. La regla general es una entrega pequeña con sus pruebas, en vez de cambiar todo el flujo al mismo tiempo.

## 3. E0 — Preparación y migraciones seguras

### Situación actual

`BaseDatos.inicializarBaseDatos` abre una transacción, crea tablas y ejecuta `MigracionClinica`. Esta agrega columnas faltantes y crea versiones/disparadores. No existe todavía un mecanismo explícito de versiones numeradas del esquema.

### Implementación

1. Registrar el punto de partida: esquema de una base vacía, una base anterior a la migración clínica y una base con las 53 columnas añadidas. Usar copias ficticias o anonimizadas para pruebas.
2. Ejecutar las pruebas actuales y guardar el resultado. No usar la base real del consultorio como fixture.
3. Añadir una tabla pequeña `Esquema_version(version INTEGER PRIMARY KEY, aplicada_en TEXT NOT NULL)` o utilizar `PRAGMA user_version`; escoger una sola estrategia. Se propone la tabla para registrar también cuándo se aplicó cada paso.
4. Mantener la migración existente como paso inicial compatible. Las siguientes se ejecutan una sola vez, en orden y dentro de la transacción correspondiente. Registrar una versión solo después de completar su paso.
5. Antes de una migración que reconstruya tablas, crear y validar un respaldo mediante el procedimiento de E8. No mezclar la generación del respaldo con una transacción abierta de modificación del esquema.
6. Si se agregan columnas a una tabla que ya tiene `Version_*`, añadir las columnas equivalentes al archivo de versiones antes de recrear sus disparadores. El `CREATE TRIGGER IF NOT EXISTS` actual no actualiza la definición de un disparador existente.
7. Si se reconstruye una tabla SQLite, inventariar claves, índices y disparadores; copiar los datos con correspondencia explícita de columnas y recrear las dependencias. No descartar registros que no encajen en una nueva restricción: producir un informe y dejar la migración pendiente de resolver ese conflicto.
8. Ejecutar `PRAGMA foreign_key_check` y una comprobación de integridad al terminar. Conservar el error real de migración y evitar que la aplicación trabaje con una conexión parcialmente inicializada.

### Pruebas y aceptación

- Migrar las tres bases de referencia y reabrirlas dos veces sin duplicar columnas ni perder versiones.
- Provocar un error intermedio y comprobar rollback y posibilidad de reintentar.
- Comparar registros antes/después y verificar que no se completa información clínica ausente.
- Confirmar que una actualización posterior sigue creando una versión anterior con todas las columnas vigentes.

## 4. E1 — Conservar el motivo y los signos entre recepción y consulta

### Problemas concretos

- `CitaController.onSiguiente` envía cita, paciente y signos, pero omite `txtMotivo`.
- `ConsultaController.guardarAtencion` asigna el texto fijo `Consulta externa` a `Consulta.motivo`.
- `CitaController.cargarCita` recupera el diagnóstico de receta como motivo de recepción.
- `ConsultaController.cargarConsulta` prioriza los signos de la receta anterior aunque reciba signos nuevos.
- Cargar de nuevo un formulario completo puede borrar texto o medicamentos todavía no guardados. Revisar también el retorno mediante `onVolver` y `refrescar`.

### Archivos y pasos

1. En `CitaController`, recuperar `Consulta.motivo` mediante `cargarConsultaPorCita`. Si no hay nota, iniciar el motivo vacío; no derivarlo del diagnóstico.
2. Transferir explícitamente el motivo a `ConsultaController`. Mantenerlo como estado de la atención y asignarlo a `nota.setMotivo(...)` al guardar. Evitar guardar un texto administrativo como si fuera el relato del paciente.
3. Diferenciar dos operaciones: **abrir una atención desde SQLite** y **aplicar una captura nueva de recepción**. Puede mantenerse `cargarConsulta` para la primera y añadir `actualizarRecepcion(motivo, signos)` para la segunda. No hace falta una capa genérica de navegación.
4. Al seleccionar una cita distinta, cargar su nota/receta y limpiar el estado de la anterior. Al volver de recepción a la misma cita ya abierta, actualizar únicamente motivo/signos y conservar diagnóstico, indicaciones y medicamentos no guardados.
5. Definir una regla inequívoca: una captura explícita de recepción tiene prioridad sobre la medición guardada. Abrir una receta solo para lectura no debe simular una captura nueva.
6. En `CitaController.refrescar`, actualizar cabecera y disponibilidad sin reinicializar indiscriminadamente la captura local. Reservar `cargarCita` para abrir otra atención o recargar expresamente desde la base.
7. Mantener en `BaseDatos.guardarAtencion` las verificaciones de pertenencia a cita y paciente, y el guardado conjunto. El motivo ya tiene columna; esta entrega no necesita una migración.

### Casos de aceptación

- Registrar motivo A y diagnóstico B; guardar, reabrir y comprobar A en recepción/nota y B en diagnóstico.
- Receta previa con presión A; corregir a B en recepción y comprobar B en consulta, SQLite y expediente.
- Escribir un medicamento sin guardar, volver a recepción, ajustar peso y regresar: el medicamento permanece.
- Cambiar de paciente: no reaparecen motivo, signos o medicamentos del anterior.
- Volver sin modificar recepción: no se borran datos válidos ni se crean mediciones vacías.

Agregar estos casos al ensayo JavaFX existente o a una prueba dedicada de navegación, usando una sola inicialización de `Platform` por proceso. Las pruebas actuales de persistencia por sí solas no demuestran esta transferencia.

## 5. E2 — Validación numérica y mediciones no realizadas

### Diseño propuesto

Los números inválidos deben producir un error de captura. Un campo opcional vacío debe representar ausencia, no cero. La aplicación no debe inventar una medición ni decidir automáticamente que un hallazgo es normal.

### Implementación

1. Sustituir el uso silencioso de `entero(..., 0)` y `decimal(..., 0f)` en recepción por validación que identifique el campo inválido y mantenga su texto para corregirlo.
2. Establecer unidades visibles y consistentes: guardar talla en centímetros y peso en kilogramos; convertir solamente en el límite de entrada. Elegir una única unidad de talla en el formulario, en lugar de interpretar valores pequeños/grandes como unidades distintas sin indicarlo.
3. Aceptar el separador decimal definido para la interfaz y normalizarlo antes del parseo. Rechazar texto ambiguo, valores no finitos y combinaciones de separadores que no puedan interpretarse con certeza.
4. Representar ausencia con valores anulables en `SignosVitales`: `Integer` y `Float` donde hoy hay primitivos, o un modelo equivalente mínimo que preserve esa diferencia. Actualizar constructor, getters, cálculo de IMC, UI, SQL y exportaciones de manera conjunta.
5. Las columnas numéricas existentes admiten `NULL`; escribir `setNull` cuando corresponda y leer sin convertir `NULL` en cero. Agregar `observaciones_medicion TEXT` si se necesita documentar por qué no se realizó una medición.
6. No convertir automáticamente todos los ceros históricos a `NULL`: el esquema anterior no permite saber su origen. Identificarlos como datos heredados que requieren revisión cuando corresponda.
7. Validar estructura de presión arterial y unidades. Los límites clínicos y excepciones deben acordarse con el profesional; distinguir un error de formato de una cifra clínica inusual. Una alerta de cifra inusual no debe modificar el dato ingresado.
8. Mostrar el error junto al campo y enfocarlo; conservar la captura completa. Si el campo se deja vacío de manera permitida, imprimir “No registrado” y no `0`.
9. Actualizar `Version_Signos_vitales` y sus disparadores si se añade una columna, conforme a E0.

### Archivos afectados

`CitaController`, `cita-panel.fxml`, `modelo/SignosVitales`, `modelo/BaseDatos`, `controlador/TextoExpediente`, validación clínica y cualquier generador que consuma signos. Mantener los métodos compartidos de presentación para que pantalla y PDF no diverjan.

### Pruebas y aceptación

- Valor vacío permitido → `NULL` → “No registrado” tras reabrir y exportar.
- Texto inválido → no navega ni guarda; identifica el campo y conserva el resto.
- Talla con la unidad acordada → conversión única; IMC correcto cuando hay peso y talla válidos.
- `NULL` no provoca errores por desempaquetado ni divisiones por cero.
- Valores históricos permanecen sin reinterpretación automática.

## 6. E3 — Configuración visual del profesional y establecimiento

### Qué falta

Los datos fijos ya se leen de `Administrador`, pero faltan una pantalla de configuración y su actualización validada. El registro de ejemplo no es una configuración lista para emitir.

### Implementación

1. Añadir `configuracion-consultorio-panel.fxml` y un controlador que siga el patrón de `PanelControlado`. Reutilizar los controles y colores existentes.
2. Mostrar nombre/apellidos, profesión, cédula, institución del título, domicilio del profesional, especialidad/registro y nombre/domicilio del establecimiento.
3. Agregar métodos de lectura y actualización por identificador a `BaseDatos`, con parámetros preparados. No exponer la contraseña en esa consulta ni editar otros administradores por un identificador recibido sin comprobar permisos.
4. Reutilizar las reglas de datos de prescriptor de `ValidacionClinica`, separándolas de la exigencia de paciente/medicamentos. No construir una receta falsa para validar un perfil.
5. Validar cédula y campos necesarios para emisión; si se indica especialidad, requerir su registro. Advertir que verificar formato no acredita el título ni la habilitación real.
6. Mostrar un estado de configuración completa/incompleta y señalar los campos pendientes. Permitir guardar configuración parcial solo si se identifica como tal y no habilita emisión.
7. Al guardar, confirmar el resultado y mantener los datos si falla SQLite. Cancelar debe descartar los cambios locales.
8. Después de E4, cargar el perfil del profesional autenticado. Hasta esa integración, declarar expresamente que la aplicación usa el administrador predeterminado; no afirmar soporte multiclínica/multimédico completo.
9. Definir el establecimiento del expediente: reutilizar los datos fijos al crear una historia nueva, pero mantener la copia histórica del establecimiento de una historia ya registrada. No sincronizar retroactivamente documentos anteriores.

### Base de datos y pruebas

Reutilizar las columnas existentes; no crear otra tabla de médico únicamente para duplicarlas. Si se necesita distinguir datos completos, calcular el estado mediante validación antes de añadir otra columna.

Comprobar guardar/reabrir, cancelación, configuración incompleta, especialidad sin registro, fallo de actualización y emisión posterior. Cambiar el perfil no debe modificar los datos impresos de una receta anterior.

## 7. E4 — Autenticación real, sesión y permisos

### Situación actual

`LoginController` acepta los textos `admin`/`asis` como rol sin validar contraseña. `MainController` limita navegación según ese rol, pero esto no autentica al profesional ni vincula la emisión con su identidad.

### Implementación propuesta

1. Añadir una tabla de acceso única, por ejemplo `Cuenta_acceso`, con identificador, usuario normalizado único, rol, estado activo, datos de hash de contraseña y referencia al administrador o asistente correspondiente. Usar claves foráneas y una restricción que impida enlazar simultáneamente ambos tipos de perfil.
2. Mantener los datos profesionales en sus tablas actuales. La cuenta controla acceso; el perfil conserva información administrativa/clínica. No guardar una segunda copia mutable de la cédula en la cuenta.
3. Crear una forma controlada de configurar la primera cuenta. No habilitar automáticamente una contraseña conocida de ejemplo al migrar. Las credenciales heredadas en texto plano requieren migración o restablecimiento explícito, sin imprimirlas en logs.
4. Guardar un hash de contraseña con sal aleatoria y parámetros del algoritmo. No usar SHA-256 directo ni cifrado reversible para contraseñas. Elegir el algoritmo y factor de trabajo con la guía vigente de OWASP; si se adopta PBKDF2 mediante JDK para evitar dependencias, documentar y medir sus parámetros. Mantenerlos por cuenta para poder actualizarlos.
5. En el login, buscar la cuenta activa, verificar la contraseña y crear una sesión en memoria con identificador de cuenta, rol e identificador del perfil. No permitir que el texto del usuario determine el rol.
6. Entregar esa sesión a `MainController` y a las operaciones que lo necesiten. No crear un administrador predeterminado dentro del proceso de emisión cuando existe una sesión real.
7. Aplicar permisos al ejecutar acciones, no solo al ocultar botones: asistente para agenda según política; profesional autorizado para nota, prescripción, expediente y emisión; administración de cuentas/configuración según rol.
8. Registrar actor en eventos de guardar/finalizar/emitir/cancelar; E8 define su almacenamiento. No registrar contraseñas ni texto clínico completo en bitácoras generales.
9. Añadir cerrar sesión y limpiar controladores cacheados, cita activa y formularios para que otra cuenta no herede información. Los documentos históricos mantienen al autor original.

### Aceptación

- Usuario o contraseña incorrectos no abren la aplicación; cuenta inactiva tampoco.
- El rol proviene de SQLite y una acción restringida falla aunque se invoque sin usar el menú.
- La receta nueva registra el perfil autenticado correcto, incluso con varios administradores en la base.
- Cerrar sesión elimina el estado de atención de la memoria visible.
- Los datos de contraseña no aparecen en consultas de perfil, capturas, archivos exportados ni logs.

## 8. E5 — Borradores, finalización, correcciones y unicidad

### Decisión funcional propuesta

La cita puede estar atendida aunque falle una exportación. El estado de atención, el de la receta y el de su documento son conceptos distintos; no usar `Cita.estado` como sustituto de todos ellos.

Proponer `Consulta.estado = BORRADOR | FINALIZADA` y `Receta.estado = BORRADOR | EMITIDA | CANCELADA`. La emisión genera una revisión congelada; no significa que ya tenga firma autógrafa. No cambiar la validación para permitir emitir documentos incompletos.

### Implementación

1. Añadir las columnas de estado/revisión necesarias con una migración de E0. No clasificar recetas antiguas como firmadas o emitidas sin evidencia; identificarlas como heredadas y mantener sus datos.
2. Separar acciones: **Guardar borrador**, **Finalizar atención**, **Preparar/emitir documento** y **Reimprimir**. Dejar claro qué acción solo guarda y cuál genera una versión documental.
3. Dividir validaciones: borrador exige pertenencia e integridad; finalización exige nota completa; emisión exige prescriptor/paciente/medicamentos completos para el formato ordinario. Una atención sin fármacos no debe obligar a emitir una receta vacía.
4. Reutilizar `guardarAtencion` y sus comprobaciones/rollback. Extenderlo con estados explícitos, sin eliminar la transacción existente ni los controles de paciente/cita.
5. Revisar si la fila de `Receta` vacía que hoy acompaña una atención sin fármacos seguirá como borrador interno o se eliminará como requisito de persistencia. Recomendación: permitir finalizar nota/signos sin una receta emitible y garantizar que no aparezca como una receta válida en el listado. Incorporar en esta misma entrega la separación de signos descrita en E10, antes de permitir `receta == null` en el guardado.
6. Para un borrador, permitir edición conservando versiones anteriores. Para una receta emitida, crear una nueva revisión y exigir motivo/autor; no sobrescribir la versión documental ya generada.
7. La cancelación debe registrar fecha, responsable y motivo, y conservar el documento. No borrar detalles de una receta emitida para simular que nunca existió.
8. Al abrir una atención atendida, mostrar modo de consulta. La corrección debe ser una acción explícita con permiso y motivo; no un guardado casual al intentar imprimir.
9. Evitar doble clic y escrituras repetidas: deshabilitar el botón durante la operación, reutilizar identificadores y garantizar unicidad de la nota y receta operativa por cita en SQLite.
10. Antes de agregar índices únicos, consultar duplicados por `id_cita` y expedientes por CURP. Si los hay, no escoger silenciosamente el último: informar, conservar ambos y resolver sus relaciones. Aplicar la restricción cuando el conjunto sea consistente.
11. Mantener las revisiones emitidas en un almacenamiento aparte (E6). Así no se contradice la regla de una receta operativa por cita con la necesidad de conservar varias revisiones documentales.

### Pruebas

- Doble guardado de la misma cita no crea otra nota/receta operativa.
- Dos intentos sobre la misma cita reciben un resultado coherente; no se reportan ambos como inserciones nuevas.
- Un borrador incompleto se recupera; no puede emitirse hasta completarlo.
- Atención sin medicamentos finaliza sin un PDF de receta vacío.
- Corrección y cancelación conservan revisión anterior, motivo y actor.
- Una migración con duplicados preexistentes se detiene de forma recuperable, sin perder filas.

## 9. E6 — PDF actualizado, archivo de emisiones y reimpresión fiel

### Problema que se resuelve

La base se guarda antes de escribir el PDF, Finalizar no lo regenera y la ruta actual reemplaza el archivo anterior. Debe ser posible distinguir la última captura de una versión efectivamente emitida y reimprimir la misma versión sin reconstruirla con datos nuevos.

### Diseño mínimo propuesto

Una tabla `Documento_clinico` para las emisiones de receta/expediente, sin servicios externos ni un sistema de colas. Campos propuestos:

| Campo | Propósito |
|---|---|
| `id_documento` | Identificador propio de la emisión |
| `tipo` | `RECETA` o `EXPEDIENTE` |
| `id_receta`, `id_expediente` | Referencia según tipo; restricción para exigir exactamente la referencia válida |
| `revision` | Versión documental creciente por origen, con índice único compuesto |
| `estado_generacion` | `PENDIENTE_PDF`, `GENERADO` o `ERROR` |
| `contenido_xml` | Copia congelada, versionada y completa de los datos usados para generar el documento |
| `pdf` | Bytes del PDF generado, para conservar el original exportable |
| `sha256` | Huella de esos bytes para verificar integridad; no se presenta como firma electrónica |
| `creado_en`, `id_cuenta` | Fecha y actor de la emisión |
| `error_generacion` | Error técnico acotado, sin credenciales o información innecesaria |

Para evitar introducir una librería solo para serializar la copia congelada, puede usarse XML con APIs estándar del JDK y un esquema pequeño que incluya versión de formato, fechas, identidad y medicamentos con índice explícito. Al leerlo, deshabilitar DTD y entidades externas, validar la versión del formato y rechazar contenido inválido. La reimpresión normal usa los bytes conservados, no vuelve a serializar ni a consultar el perfil actual. No utilizar serialización nativa de objetos Java.

El almacenamiento de bytes en SQLite simplifica respaldo y consistencia para la escala local actual. Medir tamaño con datos representativos; si posteriormente exige almacenamiento externo, conservar la misma identidad documental y un procedimiento de respaldo coordinado. No introducir ese segundo almacenamiento antes de necesitarlo.

### Secuencia de emisión y recuperación

1. Validar la acción y congelar los datos de la revisión que se quiere emitir. Registrar nota/receta y la solicitud documental `PENDIENTE_PDF` en la transacción de guardado. Reservar su identificador/revisión allí.
2. Confirmar SQLite. Generar el PDF en memoria a partir de la copia congelada, no a partir de un formulario que puede cambiar mientras se trabaja.
3. Adaptar `ControladorPDF` para aceptar una salida de bytes además de `File`, conservando el mismo contenido y estilo. Reutilizar ambos generadores, no escribir un segundo formato.
4. Guardar los bytes y su huella, y cambiar a `GENERADO` en una transacción corta. Si falla, registrar `ERROR` y ofrecer reintentar la misma solicitud, sin duplicar la revisión.
5. Si se cierra la aplicación en medio, listar las solicitudes pendientes al volver y permitir reintentarlas. Una fila pendiente nunca debe aparecer como documento generado correctamente.
6. Exportar una copia del PDF conservado a un archivo temporal en el destino y reemplazar el destino solo cuando la escritura haya terminado. Usar nombres con identificador/revisión; pedir confirmación si se elige sobrescribir una copia existente.
7. Finalizar con medicamentos debe ofrecer preparar el documento de la revisión final. Si falla el PDF, informar “Atención guardada; documento pendiente” y mantener una vía de reintento. No afirmar que SQLite y el archivo externo se confirmaron atómicamente.
8. Cambiar un borrador después de una exportación debe indicar que el documento anterior pertenece a otra revisión. No volver a mostrar ese archivo como si contuviera los cambios nuevos.
9. Reimprimir significa exportar los mismos bytes. Cambios de nombre, domicilio, prescriptor o plantilla no deben alterar una emisión anterior.
10. Para expediente, guardar la copia de los datos exportados y la revisión documental; permitir tanto una nueva exportación actualizada como recuperar una emisión anterior, distinguiendo ambas acciones.

### Impresión y firma

- Ofrecer abrir el PDF o imprimirlo con la aplicación del sistema. Si no puede iniciarse, mostrar la ruta y permitir reintentar sin volver a guardar la atención.
- Mantener “pendiente de firma” en los documentos no firmados. Un evento de impresión no acredita firma autógrafa.
- Si se incorpora un original firmado escaneado, registrar archivo, responsable y fecha como adjunto separado, sin reemplazar el PDF que se emitió. La política de custodia debe definir quién puede incorporarlo y consultarlo.

### Pruebas y aceptación

- Exportar A, modificar a B y finalizar: la interfaz identifica ambas revisiones y ofrece el PDF de B; A permanece recuperable.
- Fallo de generación y cierre de aplicación: la solicitud pendiente se recupera y reintenta sin duplicación.
- Cambio del perfil del profesional después de emitir: la huella y los bytes del documento anterior permanecen iguales.
- Fallo al escribir el archivo externo: el original SQLite permanece intacto y no se reporta éxito.
- Revisar visualmente una receta corta, una extensa, indicaciones largas, expediente completo e historia incompleta; verificar continuaciones, nombres, unidades, firma y numeración.

## 10. E7 — Listado de recetas y documentos desde el expediente

### Implementación

1. Crear un panel de listado (`recetas-panel.fxml`) y su controlador usando los patrones de pacientes/agenda. Cambiar el destino del menú de recetas a ese listado, respetando permisos.
2. Agregar consultas preparadas con filtros de paciente/CURP, fechas, folio y estado. Usar paginación o límite de resultados; no cargar todos los medicamentos/PDF al construir la tabla.
3. Mostrar folio, paciente, fecha, diagnóstico resumido, estado de receta y estado del último documento. No mostrar una fila sin medicamentos como receta emitida.
4. Abrir un detalle de solo lectura con los datos guardados. Diferenciar continuar borrador, corregir, cancelar, generar pendiente y reimprimir; habilitar cada acción según estado y permiso.
5. Permitir entrar sin una cita activa en `ConsultaController`. No inventar una cita para consultar documentos ni reabrir automáticamente una cita atendida al imprimir.
6. En el expediente, enlazar cada receta con su detalle y sus emisiones. Conservar los resúmenes actuales como información, pero ofrecer acceso al documento real de E6.
7. Ante un documento ausente o dañado, informar su estado. Si existen los bytes en SQLite, exportar otra copia; si solo existe una solicitud pendiente, generarla desde su contenido congelado.

### Aceptación

Buscar dos pacientes con nombres similares sin mezclar sus recetas; consultar e imprimir una receta anterior sin modificarla; impedir a un usuario no autorizado acceder invocando el controlador directamente; indicar claramente borrador, cancelación y PDF pendiente.

## 11. E8 — Archivo, revisiones, bitácora, respaldos y restauración

### Archivo y protección de bajas

1. Sustituir la baja física de pacientes con documentación clínica por archivo lógico. Añadir estado activo/archivado y fecha/actor de archivo cuando haga falta.
2. La baja de expediente no debe destruir documentación para “ocultarla”. Ofrecer archivar y filtrar, manteniendo las referencias y la consulta autorizada.
3. Revisar todas las rutas de `eliminarPaciente`, `eliminarCita`, `eliminarExpediente` y `eliminarReceta`. No basta con retirar un botón si otro método sigue ejecutando la cascada.
4. Mantener las cascadas históricas en consideración durante la migración; si se cambian a restricciones, reconstruir y verificar relaciones mediante E0. Las emisiones documentales deben impedir la eliminación accidental de su origen.
5. Configurar la política de conservación y acceso según las obligaciones aplicables ya documentadas; no implementar una eliminación automática por plazo sin revisar esa política.

### Revisión y correcciones

1. Añadir una vista de versiones por expediente, consulta y receta con fecha, operación y campos anteriores. Las filas antiguas sin autor se muestran como tales; no atribuirles un usuario nuevo.
2. No restaurar una versión sobrescribiendo silenciosamente la actual. Ofrecer crear una corrección nueva con motivo, actor y referencia a la versión recuperada.
3. Mostrar qué versión se está viendo y mantenerla en modo de solo lectura hasta iniciar una corrección permitida.

### Bitácora mínima

Crear `Evento_clinico` con identificador, fecha UTC, cuenta/rol, acción, tipo/id del recurso, resultado y referencia a corrección/documento. Registrar accesos a expediente, guardados, finalización, emisión, reimpresión, cancelación y archivo. No registrar contraseñas ni copiar toda la narración clínica en mensajes generales. Es trazabilidad local, no una garantía de inviolabilidad frente a alguien con acceso directo al archivo SQLite.

### Respaldo y recuperación

1. Permitir al administrador crear un respaldo consistente usando `VACUUM INTO` o la API de respaldo de SQLite. No copiar únicamente el archivo principal mientras hay escrituras activas o un WAL sin coordinar.
2. Ejecutar `VACUUM INTO` fuera de una transacción de escritura y con destino nuevo; comprobar el resultado y la integridad. SQLite documenta que el destino no puede ser un archivo existente con contenido.
3. Si E6 conserva los bytes en SQLite, quedan incluidos. Incluir también los adjuntos externos de originales firmados, si los hubiera, junto con un manifiesto de archivos/huellas.
4. Restringir el acceso a respaldos porque contienen la misma información sensible. No guardarlos dentro del repositorio Git ni en ubicaciones compartidas por defecto.
5. Restaurar con la aplicación sin escrituras activas: validar primero una copia, respaldar la instalación actual, restaurar y reabrir. No borrar el único respaldo si falla.
6. Probar la recuperación real: recuperar paciente, nota, signos, receta, versiones y bytes de un PDF y comparar su huella.

### Aceptación

Archivar no destruye información; no se puede borrar accidentalmente una emisión; se identifica quién realizó una corrección nueva; restaurar un respaldo devuelve un expediente y un documento utilizables. Tener un botón de “respaldar” sin una prueba de restauración no cierra esta entrega.

## 12. E9 — Horarios y bloqueos de agenda

### Situación actual y decisión inicial

Existen modelos y tablas de horario/bloqueo, pero el calendario utiliza horas predefinidas. El esquema actual de `Horario_bloqueado` usa `id_horario` como clave primaria, lo que limita varios bloqueos para un mismo horario.

Para la primera entrega, conservar la duración actual de 45 minutos como una única constante compartida por calendario y validación. No añadir duraciones variables hasta que sean un requisito.

### Implementación

1. Agregar CRUD de `Horario` y lectura de horarios activos por día. Validar inicio anterior a fin, día válido y ausencia de intervalos activos contradictorios.
2. Añadir un panel sencillo de configuración para administrador; usar los permisos de E4.
3. Generar los espacios de `AgendaController` desde esos datos. Evitar una lista de horas para mostrar y otra distinta para guardar.
4. Migrar `Horario_bloqueado` para tener identificador propio, referencia al horario, inicio, fin y motivo opcional; permitir varias filas por horario. Conservar y mapear cada bloqueo anterior.
5. Considerar intervalos de tipo `[inicio, fin)`: un bloqueo que termina justo cuando inicia una cita no se solapa con ella. Hay solapamiento cuando el inicio nuevo es menor que el fin existente y el fin nuevo es mayor que el inicio existente.
6. Validar al agendar y reprogramar, no solo al colorear el calendario: horario activo, duración completa dentro del horario, ausencia de bloqueo y ausencia de otra cita activa superpuesta. Excluir la propia cita cuando se reprograma.
7. Ejecutar comprobación y escritura en una misma transacción. Usar un formato de fecha/hora coherente; normalizar las representaciones heredadas antes de depender de comparaciones SQL numéricas o textuales.
8. Al bloquear un intervalo con citas existentes, mostrar el conflicto y exigir resolverlo; no cancelar pacientes automáticamente ni ocultar esas citas.

### Pruebas

Espacios fuera de horario no agendables; bloqueos múltiples; reprogramación a un espacio ocupado; citas contiguas válidas; cita cancelada no ocupa espacio; un bloqueo con conflicto no se aplica silenciosamente; recarga conserva exactamente los intervalos.

## 13. E10 — Altas coherentes y CRUD administrativos restantes

### Alta conjunta de paciente y expediente

`PacienteEditarController` realiza hoy dos guardados separados. Añadir una operación de `BaseDatos` que registre paciente y expediente inicial en la misma transacción, reutilizando la lógica interna existente. No hacer dos transacciones anidadas sobre la conexión estática. Si falla la creación del expediente, revertir el alta nueva y mantener el formulario para reintentar. Probar fallo de la segunda inserción y CURP duplicada.

### Cuentas y perfiles administrativos

Completar alta, consulta, modificación y desactivación de administradores/asistentes con E4. Desactivar un profesional no debe borrar sus recetas; impedir desactivar la última cuenta administradora utilizable sin dejar otra vía de acceso. Los cambios de perfil no alteran los documentos ya emitidos.

### Medicamentos individuales

El módulo ya edita la lista completa y la reemplaza transaccionalmente. Eso es suficiente para cerrar la edición de un borrador; el CRUD individual no es un requisito por sí mismo. Marcar este pendiente como cubierto por edición de receta, salvo que se solicite expresamente editar una fila de manera independiente. Si se implementa, comprobar siempre que `id_detalle_receta` pertenezca a la receta y que esta sea editable; conservar las versiones y no aplicarlo a una emisión histórica.

### Signos independientes de una receta

Separar la medición de la existencia de fármacos cuando E5 cierre la atención sin receta. Propuesta mínima: añadir `id_consulta` a `Signos_vitales`, migrar su relación a la nota de la cita y conservar `id_receta` para lectura heredada mientras sea necesario. `id_receta` es hoy `NOT NULL`: reconstruir esa tabla para permitir su ausencia en nuevas mediciones vinculadas a una consulta, con una restricción que impida filas sin ningún origen. Actualizar sus claves, versiones, disparadores y consultas en la misma migración.

Los datos nuevos deben vincularse a la consulta; si también tienen receta, comprobar que ambas pertenecen a la misma atención. Para filas antiguas sin una nota inequívoca, conservar la referencia original y documentar el caso. Si hay ambigüedad o varias notas, producir informe antes de asignar relaciones. No copiar una medición a otro paciente por tomar una coincidencia parcial.

Adaptar el guardado para que una nota sin medicamentos conserve signos y se muestre en el expediente. Modificar `TextoExpediente.signos` para leer mediciones de consultas en vez de recorrer solamente recetas. Solo añadir una pantalla de mediciones independientes si existe necesidad operativa; primero resolver la independencia de la persistencia.

## 14. Inventario propuesto de migraciones

Todas las siguientes son propuestas, no cambios ya aplicados. Dividirlas por entrega y ejecutar E0 en cada una.

| Entrega | Cambio esperado |
|---|---|
| E0 | Registro de versión de esquema y actualización coordinada de `Version_*`/disparadores |
| E1 | Sin columnas nuevas; utilizar `Consulta.motivo` existente |
| E2 | Lectura/escritura nullable de signos; `observaciones_medicion` solo si se necesita documentar ausencia |
| E3 | Reutilizar columnas actuales de `Administrador`; no duplicar el perfil |
| E4 | `Cuenta_acceso`, restricciones de vínculo a perfiles y desactivación |
| E5 | Estado/revisión y metadatos de corrección/cancelación; índices únicos tras resolver duplicados |
| E6 | `Documento_clinico`, contenido congelado, bytes PDF y metadatos de generación |
| E8 | Archivo lógico, `Evento_clinico` y referencias de versiones/correcciones cuando falten |
| E9 | CRUD de horarios; reconstrucción compatible de bloqueos con clave propia |
| E10 | Relación de signos con consulta y alta conjunta sin nuevas entidades duplicadas |

Conservar por separado fecha clínica local y fecha técnica UTC donde corresponda; documentar su interpretación y conversión. No reescribir fechas clínicas históricas para hacerlas coincidir con una emisión nueva.

## 15. Estrategia de pruebas y aceptación final

### Pruebas por entrega

Cada cambio funcional debe dejar al menos una prueba del problema que resuelve. Priorizar casos que detecten pérdida de datos, mezcla de pacientes, documentos desactualizados o fallos parciales. No añadir pruebas que solo repitan constantes de estilo.

Reutilizar `CitaRecetaTest`, `ConsultaTest`, `ExpedienteTest` y `FormatoClinicoTest`. Crear pruebas específicas de autenticación, emisiones y horarios cuando esas entregas existan. Mantener `gestor.db.url` apuntando a bases temporales y compartir correctamente el ciclo de vida de JavaFX.

### Escenario integral obligatorio

1. Crear profesional y cuenta de acceso; completar el establecimiento y cerrar/reabrir la aplicación.
2. Iniciar sesión con permisos correctos y registrar paciente con expediente.
3. Agendar en un horario válido y comprobar que un bloqueo impida otro intento.
4. Abrir la cita; capturar motivo y mediciones válidas, dejando una medición opcional ausente según la política definida.
5. Entrar a consulta; capturar diagnóstico, evolución, estudios, pronóstico, indicaciones y dos medicamentos.
6. Volver a recepción, corregir una medición y regresar: conservar todos los textos y medicamentos.
7. Guardar borrador, cerrar y reabrir: comprobar persistencia de motivo y mediciones, incluidos los ausentes.
8. Preparar documento A; corregir el borrador y finalizar con documento B; ambos quedan identificados y B contiene la revisión final.
9. Simular fallo al generar B y reintentarlo sin duplicar cita, nota, receta o emisión.
10. Abrir el listado sin cita activa; consultar y reimprimir A y B sin modificar sus bytes.
11. Abrir el expediente, editar historia y guardar: confirmar estado y desaparición del aviso verde a los cinco segundos; cancelar otra edición sin cambios persistidos.
12. Exportar el expediente y revisar historia, nota, medicamentos, signos, identificaciones y paginación.
13. Hacer una corrección autorizada con motivo; comprobar original, revisión y actor. Intentar la misma acción sin permiso y comprobar rechazo.
14. Archivar, respaldar y restaurar en una instalación de prueba; recuperar el expediente y comparar la huella del PDF.
15. Imprimir ejemplos y revisar legibilidad y espacio de firma con el profesional. Mantener pendiente de firma aquello que no tenga original firmado incorporado/custodiado según la política del consultorio.

### Definición de terminado

- [ ] Motivo y signos se transfieren y recargan sin pérdidas.
- [ ] Los datos ausentes no se inventan ni se convierten en cero silenciosamente.
- [ ] La configuración fija se gestiona desde la interfaz y el responsable está autenticado.
- [ ] Estados, permisos, correcciones y cancelaciones tienen reglas verificadas.
- [ ] Borrador, nota finalizada, documento generado y firma no se confunden.
- [ ] Se puede reintentar una generación fallida y reimprimir una emisión exacta.
- [ ] El expediente muestra los datos de la atención correcta y las mediciones sin receta.
- [ ] Horarios y bloqueos se respetan al mostrar y al guardar citas.
- [ ] Las bajas no destruyen accidentalmente documentación clínica.
- [ ] Un respaldo se restaura y devuelve datos y documentos comprobables.
- [ ] Pasan las pruebas actuales y las nuevas; se revisan los PDF y la impresión física.
- [ ] Se actualizan `funcionalidades.md`, el esquema documentado y los límites pendientes.

## 16. Secuencia sugerida de entregas revisables

| Cambio | Contenido revisable |
|---|---|
| 1 | E0 + E1: preparación, motivo y regreso entre pantallas con pruebas de pérdida de datos |
| 2 | E2: mediciones validadas y valores ausentes |
| 3 | E3: configuración fija del consultorio |
| 4 | E4: cuentas, sesión, permisos y responsable de emisión |
| 5 | E5: borradores, estados y correcciones; primera parte de E10 para atención sin receta |
| 6 | E6: emisiones persistentes, generación recuperable y archivos de revisión |
| 7 | E7: listado y reimpresión desde recetas/expediente |
| 8 | E8: archivo, bitácora, respaldos y restauración |
| 9 | E9 y resto de E10: horarios/bloqueos y operaciones administrativas |
| 10 | E11: aceptación integral, revisión de impresión y documentación de cierre |

No asignar fechas de entrega sin medir el primer cambio y conocer disponibilidad. El orden expresa dependencias y riesgo, no una estimación contractual. Los trabajos pueden dividirse más si un cambio deja de ser revisable, pero no marcar una entrega terminada sin su criterio de aceptación.

## 17. Referencias técnicas para implementar

- [SQLite: claves foráneas y acciones de borrado](https://www.sqlite.org/foreignkeys.html). Consultar al cambiar relaciones, restricciones e índices; verificar la conexión y los datos migrados.
- [SQLite: `VACUUM INTO` y respaldo de bases](https://www.sqlite.org/lang_vacuum.html#vacuuminto). Usar sus condiciones de ejecución y destino al implementar E8.
- [OWASP: almacenamiento de contraseñas](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Consultar parámetros actuales y migración de credenciales al implementar E4.
- [Estado real del proyecto](../funcionalidades.md) y [alcance de formatos mexicanos](normativa-mexico.md). Este plan no certifica cumplimiento jurídico ni habilita la emisión de recetarios especiales.
