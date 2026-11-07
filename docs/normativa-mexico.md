# Formatos clínicos para México

Revisión: 5 de noviembre de 2026. Alcance: consulta externa general y de especialidad, historia clínica, nota de evolución y receta ordinaria para impresión y firma autógrafa.

## Fuentes oficiales

- [NOM-004-SSA3-2012, Diario Oficial de la Federación](https://sidof.segob.gob.mx/notas/docFuente/5272787), numerales 5 y 6. [Ficha de vigencia de la Secretaría de Economía](https://platiica.economia.gob.mx/normalizacion/nom-004-ssa3-2012/).
- [Reglamento de Insumos para la Salud, compilación oficial de SEGOB a 31 de mayo de 2021](https://www.ordenjuridico.gob.mx/Documentos/Federal/html/wo88318.html), artículos 28 a 32 y 50 a 52; contrastados con el [texto alojado por COFEPRIS](https://sipot.cofepris.gob.mx/Archivos/juridico/PC/NORMATIVIDAD/rtoinsumos.pdf). También se revisó la [reforma de 24 de abril de 2026](https://sidof.segob.gob.mx/notas/docFuente/5785957), relativa a registros sanitarios y patentes, que no modifica esos artículos de prescripción.
- [Reglamento de prestación de servicios de atención médica, Cámara de Diputados](https://www.diputados.gob.mx/LeyesBiblio/regley/Reg_LGS_MPSAM_170718.pdf), artículos 64 y 65, texto con reforma de 17 de julio de 2018.
- [Acuerdo sobre venta y dispensación de antibióticos, DOF de 27 de mayo de 2010](https://sidof.segob.gob.mx/notas/docFuente/5144336).
- [COFEPRIS: manual de recetarios especiales](https://www.gob.mx/cofepris/documentos/manual-de-uso-de-la-plataforma-de-recetarios-electronicos).

## Trazabilidad de los formatos

| Referencia | Integración en la aplicación |
| --- | --- |
| NOM-004, 5.2 | Tipo, nombre, domicilio, institución y propietario del establecimiento; identificación del paciente. |
| NOM-004, 5.9 a 5.11 | Identidad y edad en la atención, fecha/hora, autor, espacio de firma e instrucciones de redacción. |
| NOM-004, 6.1 | Interrogatorio, antecedentes diferenciados, sustancias, exploración, signos, estudios, diagnóstico, pronóstico y terapéutica. |
| NOM-004, 6.2 | Nota de evolución separada del diagnóstico y vinculada con medicamentos y signos vitales. |
| RIS, 29 y 30 | Identificación del prescriptor, fecha, firma, presentación, dosis, vía, frecuencia y duración. |
| RIS, 31 | Denominación genérica y marca separadas. Para biotecnológicos, capturar la denominación común internacional en el campo de denominación. |
| Reglamento de atención médica, 64 y 65 | Profesión, institución que expidió el título, cédula, domicilio del establecimiento y registro de especialidad cuando se declare. |

La cantidad a dispensar, concentración, diagnóstico e identidad del paciente también se capturan explícitamente en la receta para evitar ambigüedades. No todos esos campos son exigencias literales del artículo 29: algunos son decisiones de estandarización de este proyecto.

## Uso

1. En el expediente, abrir el editor y completar los datos del establecimiento y la historia clínica. Los antecedentes personales existentes se conservan como patológicos; revisar esa clasificación en registros antiguos. Escribir de manera explícita los hallazgos negativos y los apartados no aplicables; el sistema no los inventa. Los datos demográficos se corrigen en la ficha del paciente.
2. La identificación del paciente aparece al inicio de la consulta, se carga automáticamente desde la base de datos y es de solo lectura. Las recetas existentes conservan su identificación histórica. Capturar por separado padecimiento, evolución, exploración, estudios, diagnóstico, pronóstico e indicaciones. Si no se solicitaron estudios, documentarlo.
3. Configurar una sola vez los datos reales del profesional y del establecimiento en la tabla `Administrador`: `nombre`, `apellido_paterno`, `apellido_materno`, `cedula`, `profesion`, `institución`, `domicilio`, `especialidad`, `registro_especialidad`, `nombre_establecimiento` y `domicilio_establecimiento`. Las nuevas recetas los cargan del administrador predeterminado; las existentes mantienen sus datos guardados. No hay campos de prescriptor o establecimiento en la pantalla de receta. Si faltan datos fijos, completar la configuración en la base antes de emitir; no se inventan valores ni se omiten las validaciones. La cédula de ejemplo `00000000` se rechaza. La validación del formato numérico no consulta el Registro Nacional de Profesionistas ni acredita la habilitación del prescriptor.
4. Para cada medicamento, completar los campos y seleccionar su fracción regulatoria según el registro/etiquetado autorizado. La clasificación se declara manualmente; el sistema no la deduce del nombre. Este formato admite IV, V y VI. I, II y III se bloquean como decisión de alcance del producto: I requiere recetario especial; II y III tienen controles adicionales y necesitan un flujo propio.
5. Exportar guarda conjuntamente la nota y la receta sin marcar atendida la cita. Finalizar guarda conjuntamente y marca atendida. Puede finalizarse una atención sin medicamentos; la exportación de receta exige al menos uno.
6. Imprimir, revisar y firmar de forma autógrafa la historia, cada nota y la receta. No se genera una firma electrónica y no existe estado de documento firmado. El PDF permanece identificado como pendiente de firma. Custodiar los originales firmados en el expediente de la clínica.

La historia incompleta puede guardarse para continuar su captura y se muestra como **BORRADOR** en pantalla y en la exportación. Las recetas incompletas no se exportan. Los datos faltantes en registros antiguos se muestran como no registrados. Las recetas anteriores sin identificación del paciente la toman automáticamente de su ficha; si no tienen cédula del prescriptor, cargan sus datos fijos de `Administrador`.

## Persistencia y revisiones

La migración es aditiva y se ejecuta al abrir la conexión; agrega columnas sin reemplazar datos anteriores. Las fechas de receta conservan la hora. El nombre, edad y sexo del paciente y los datos del prescriptor se guardan en la atención para que los cambios posteriores a la ficha no alteren esas identidades.

Las tablas `Version_Expediente`, `Version_Consulta`, `Version_Receta`, `Version_Detalle_receta` y `Version_Signos_vitales` conservan el contenido previo a actualizaciones o bajas, con fecha UTC y operación, mediante disparadores SQLite dentro de la misma transacción. No reconstruyen revisiones previas a esta migración ni constituyen una bitácora inviolable o una firma. La consulta de estas versiones requiere acceso técnico a la base; todavía no hay una pantalla para restaurarlas. Deben incluirse en las copias de seguridad.

## Obligaciones que requieren operación o desarrollo adicional

- Conservación mínima de cinco años desde el último acto médico, confidencialidad y entrega de información conforme a los numerales 5.4 a 5.7. Las versiones no caducan automáticamente. La clínica debe establecer respaldos, recuperación, custodia y control de acceso; los comandos de baja existentes siguen disponibles y la copia de versiones no sustituye una política de archivo.
- Identificación autenticada del profesional, permisos por rol, registro de accesos/exportaciones, control formal de correcciones y vinculación de los originales firmados. Los campos de autor y los respaldos de versiones no resuelven por sí solos estos puntos.
- Consentimientos, interconsulta, referencia/traslado, urgencias, hospitalización y otros documentos deben incorporarse cuando los servicios prestados los requieran. No se implementaron formatos fuera de consulta externa.
- Para un expediente electrónico con firma y efectos jurídicos se necesita una implementación adicional, incluida la evaluación de las disposiciones aplicables como NOM-024; esta modificación no certifica un sistema de expediente electrónico.
- La dispensación de antibióticos requiere los registros, sellado y retención que correspondan a la farmacia; generar este PDF no ejecuta esas tareas ni impone una vigencia universal a todas las recetas.
- El personal debe verificar congruencia clínica, unidades, dosis, alergias, contraindicaciones, autenticidad de la cédula y facultades para prescribir. Las validaciones de presencia no sustituyen esa revisión.

Estos cambios estandarizan la captura y la impresión dentro del alcance descrito; no equivalen a una certificación de cumplimiento integral de la clínica o del software.

## Verificación

Ejecutar `./mvnw.cmd test` en Windows con `JAVA_HOME` apuntando al JDK. Las pruebas cubren persistencia y recarga, migración de esquema anterior, versiones, rollback, aislamiento entre citas, captura JavaFX, recetas incompletas, fracciones excluidas y contenido de PDF de varias páginas. Los PDF y capturas de prueba se generan en `target/` con datos ficticios y no deben usarse para atención médica.
