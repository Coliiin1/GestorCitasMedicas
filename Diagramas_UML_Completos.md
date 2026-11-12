# Diagramas UML - Gestor de Citas Médicas

## 1. S1: Inicio del Sistema, Autenticación de Personal y Carga de Agenda Semanal

### 1.1 Diagrama de Estados (DE-S1-Inicio_Autenticacion_CargaAgenda)
```mermaid
stateDiagram-v2
    [*] --> Apagado
    Apagado --> Inicializando : Ejecutar Launcher/HelloApplication
    Inicializando --> ConectandoBD : Cargar configuración
    ConectandoBD --> InicializandoTablas : PRAGMA foreign_keys = ON
    InicializandoTablas --> VerificandoAdministrador : Crear tablas / Aplicar migración
    VerificandoAdministrador --> CargandoDatosBase : Verificar/Insertar Administrador por defecto (RN-16)
    CargandoDatosBase --> PantallaLogin : Datos base listos
    PantallaLogin --> Autenticando : Ingresar credenciales
    Autenticando --> MenuPrincipal : Credenciales válidas (Asistente/Administrador)
    Autenticando --> AccesoDenegado : Credenciales inválidas
    AccesoDenegado --> PantallaLogin : Reintentar
    MenuPrincipal --> CargandoAgendaSemanal : Abrir módulo Agenda
    CargandoAgendaSemanal --> AgendaVisualizada : cargarCitasEntre(inicio,fin) exitoso
    CargandoAgendaSemanal --> ErrorCargaAgenda : Sin conexión/consulta fallida
    ErrorCargaAgenda --> MenuPrincipal : Regresar
    AgendaVisualizada --> [*] : Cerrar aplicación
    ConectandoBD --> ErrorConexion : Fallo de conexión
    InicializandoTablas --> ErrorConexion : Error en migración/rollback
    ErrorConexion --> [*] : Finalizar
```

### 1.2 Diagrama de Casos de Uso (DCU-S1-Inicio_Autenticacion_CargaAgenda)
```mermaid
flowchart TD
    Asistente((Asistente))
    Admin((Administrador))
    Sistema((Sistema))

    CU1[Iniciar Sistema]
    CU2[Autenticar Personal]
    CU3[Cargar Agenda Semanal]
    CU4[Visualizar Resumen Semanal]
    CU5[Gestionar Sesión]
    CU6[Conectar a Base de Datos]

    Asistente --> CU1
    Admin --> CU1
    CU1 --> CU2
    CU2 --> CU5
    CU2 -. <<include>> .-> CU6
    Admin --> CU3
    Asistente --> CU3
    CU3 --> CU4
    CU3 -. <<include>> .-> CU6
```

### 1.3 Diagrama de Secuencias (DS-S1-Inicio_Autenticacion_CargaAgenda)
```mermaid
sequenceDiagram
    participant L as Launcher/HelloApplication
    participant BD as BaseDatos
    participant LC as LoginController
    participant AC as AgendaController
    participant C as Cita

    L->>BD: getConexion()
    BD->>BD: PRAGMA foreign_keys = ON (RN-17)
    BD->>BD: Crear tablas / MigracionClinica.aplicar()
    BD->>BD: Verificar/insertar Administrador por defecto (RN-16)
    BD-->>L: Conexión establecida
    L->>LC: Mostrar Pantalla Login
    LC->>BD: Validar credenciales (parcial RF-01)
    BD-->>LC: Resultado de autenticación
    alt Autenticación válida
        LC->>AC: Abrir Agenda / Mostrar Menú Principal
        AC->>BD: cargarCitasEntre(inicio, fin)
        BD-->>AC: Listado de Citas
        AC->>AC: Generar resumen semanal (Agendadas/Atendidas/Canceladas/Libres)
        AC-->>LC: Agenda Semanal Visualizada
    else Autenticación inválida
        LC-->>LC: Mostrar AccesoDenegado
    end
```

### 1.4 Diagrama de Colaboraciones (DCO-S1-Inicio_Autenticacion_CargaAgenda)
```mermaid
sequenceDiagram
    participant L as Launcher
    participant BD as BaseDatos
    participant LC as LoginController
    participant AC as AgendaController

    1. L->>BD: Solicitar conexión
    2. BD->>BD: Activar FK / Migrar esquema
    3. BD-->>L: Conexión OK
    4. L->>LC: Desplegar login
    5. LC->>BD: Consultar credenciales (Asistente/Administrador)
    6. BD-->>LC: Autorización
    7. LC->>AC: Inicializar agenda
    8. AC->>BD: cargarCitasEntre(fechaInicio, fechaFin)
    9. BD-->>AC: Colección Cita
    10. AC-->>LC: Vista semanal + resumen
```

## 2. S2: Agendamiento, Validación de Horario y Registro de Cita Médica

### 2.1 Diagrama de Estados (DE-S2-Agendamiento_Validacion_RegistroCita)
```mermaid
stateDiagram-v2
    [*] --> SinRegistrar
    SinRegistrar --> ValidandoHorario : Seleccionar espacio libre
    ValidandoHorario --> Ocupado : Existe cita no cancelada (RN-08)
    ValidandoHorario --> Disponible : Sin conflictos / Horario permitido (RN-07)
    Ocupado --> SinRegistrar : Seleccionar otro horario
    Disponible --> AGENDADA : insertarCita() exitoso
    AGENDADA --> REPROGRAMADA : Reprogramar (RN-09)
    REPROGRAMADA --> ValidandoHorario : Validar nuevo horario
    AGENDADA --> CANCELADA : Cancelar cita
    CANCELADA --> AGENDADA : Reprogramar (libera horario RN-09)
    AGENDADA --> ATENDIDA : Iniciar/Finalizar atención
    ATENDIDA --> [*]
    CANCELADA --> [*]
```

### 2.2 Diagrama de Casos de Uso (DCU-S2-Agendamiento_Validacion_RegistroCita)
```mermaid
flowchart TD
    Asis((Asistente))
    Admin((Administrador))
    Correo((SistemaCorreo))

    CU1[Seleccionar Espacio Libre]
    CU2[Validar Horario]
    CU3[Registrar Cita]
    CU4[Reprogramar Cita]
    CU5[Cancelar Cita]
    CU6[Enviar Confirmación por Correo]

    Asis --> CU1
    Admin --> CU1
    CU1 --> CU2
    CU2 --> CU3
    CU3 -. <<include>> .-> CU2
    Asis --> CU4
    Admin --> CU4
    CU4 -. <<include>> .-> CU2
    Asis --> CU5
    Admin --> CU5
    CU3 --> CU6
    CU4 --> CU6
    CU6 -. <<extend>> .-> Correo
```

### 2.3 Diagrama de Secuencias (DS-S2-Agendamiento_Validacion_RegistroCita)
```mermaid
sequenceDiagram
    participant AC as AgendaController
    participant CC as CitaController
    participant BD as BaseDatos
    participant P as Paciente
    participant C as Cita
    participant MC as ControladorCorreo

    AC->>CC: Abrir formulario de cita (slot seleccionado)
    CC->>BD: Verificar paciente por CURP
    BD-->>CC: Datos de Paciente
    CC->>BD: Consultar citas en fecha/hora (validar disponibilidad)
    BD-->>CC: Citas existentes
    alt Horario libre y permitido (RN-07, RN-08)
        CC->>BD: insertarCita(Cita con estado AGENDADA)
        BD-->>CC: idCita generado
        CC->>C: Estado = AGENDADA
        par Envío de confirmación
            CC->>MC: enviarConfirmacionCita(Paciente, Cita)
            MC-->>MC: Enviar correo HTML en hilo secundario (RN-15)
        end
        CC-->>AC: Cita registrada exitosamente
    else Horario ocupado o no permitido
        CC-->>AC: Error de validación
    end
```

### 2.4 Diagrama de Colaboraciones (DCO-S2-Agendamiento_Validacion_RegistroCita)
```mermaid
sequenceDiagram
    participant AC as AgendaController
    participant CC as CitaController
    participant BD as BaseDatos
    participant P as Paciente
    participant C as Cita
    participant MC as ControladorCorreo

    1. AC->>CC: Solicitar agendamiento (fecha/hora)
    2. CC->>BD: Validar existencia de Paciente (CURP)
    3. BD-->>CC: Paciente válido
    4. CC->>BD: Verificar disponibilidad (no canceladas)
    5. BD-->>CC: Libre/Ocupado
    6. CC->>BD: insertarCita()
    7. BD-->>CC: Confirmación + idCita
    8. CC->>C: Asociar Paciente por CURP
    9. CC->>MC: Notificar confirmación (opcional si correo/config incompleto)
    10. MC-->>CC: Resultado envío
    11. CC-->>AC: Actualizar agenda
```

## 3. S3: Atención de Consulta Médica en Tiempo Real y Generación de Receta en PDF

### 3.1 Diagrama de Estados (DE-S3-Atencion_Consulta_RecetaPDF)
```mermaid
stateDiagram-v2
    [*] --> AGENDADA
    AGENDADA --> EnRecepcion : Iniciar Atención (solo si no CANCELADA RN-11)
    EnRecepcion --> EnConsulta : Capturar signos vitales
    EnConsulta --> Guardando : Guardar Atención / Finalizar
    Guardando --> ATENDIDA : Transacción exitosa (guardarAtencion/finalizarConsulta RN-12,RN-13)
    Guardando --> ErrorGuardado : Fallo en transacción -> Rollback
    ErrorGuardado --> EnConsulta : Corregir datos e intentar nuevamente
    ATENDIDA --> GenerandoPDF : Generar Receta/Expediente en PDF (opcional)
    GenerandoPDF --> ATENDIDA : PDF generado / Ruta informada
    GenerandoPDF --> ATENDIDA : Error PDF (atención ya guardada)
    ATENDIDA --> [*]
```

### 3.2 Diagrama de Casos de Uso (DCU-S3-Atencion_Consulta_RecetaPDF)
```mermaid
flowchart TD
    Admin((Administrador))
    PDF((SistemaPDF))
    BD((BaseDatos))

    CU1[Iniciar Atención]
    CU2[Capturar Signos Vitales]
    CU3[Registrar Consulta y Medicamentos]
    CU4[Guardar Atención]
    CU5[Finalizar Consulta]
    CU6[Generar Receta en PDF]
    CU7[Actualizar Expediente/Historial]

    Admin --> CU1
    CU1 --> CU2
    CU2 --> CU3
    CU3 --> CU4
    CU4 --> CU5
    CU4 -. <<include>> .-> CU7
    CU4 -. <<include>> .-> BD
    CU5 -. <<include>> .-> CU4
    CU5 --> CU6
    CU6 -. <<extend>> .-> PDF
```

### 3.3 Diagrama de Secuencias (DS-S3-Atencion_Consulta_RecetaPDF)
```mermaid
sequenceDiagram
    participant AC as AgendaController
    participant CC as CitaController
    participant COC as ConsultaController
    participant BD as BaseDatos
    participant EXP as Expediente
    participant CON as Consulta
    participant REC as Receta
    participant MED as Medicamento
    participant SV as SignosVitales
    participant PDF as ControladorPDF

    AC->>CC: onIniciarCita(cita)
    CC->>BD: Cargar paciente/expediente/signos previos
    BD-->>CC: Datos cargados
    CC->>COC: Abrir panel consulta (cita-panel/consulta-panel)
    COC->>COC: Capturar signos vitales y motivo/diagnóstico
    COC->>COC: Agregar medicamentos
    alt Guardar sin finalizar
        COC->>BD: guardarAtencion(cita, consulta, receta, finalizar=false, generarPdf=false)
        BD->>BD: Transacción (resolver/crear Expediente RN-14)
        BD->>CON: Guardar/Actualizar Consulta
        BD->>REC: Guardar/Actualizar Receta
        BD->>MED: Reemplazar detalles de receta
        BD->>SV: Guardar SignosVitales
        BD-->>COC: Guardado exitoso
    else Guardar y exportar PDF
        COC->>BD: guardarAtencion(..., finalizar=false, generarPdf=true)
        BD-->>COC: Guardado exitoso
        COC->>PDF: generarPDF(Receta)
        PDF-->>COC: Ruta pdf/receta_<id>.pdf
    else Finalizar consulta
        COC->>BD: guardarAtencion(..., finalizar=true)
        BD->>BD: Transacción completa
        BD->>C: UPDATE Cita SET estado = ATENDIDA (RN-13)
        BD-->>COC: Atención finalizada
        note over BD,COC: Nota: PDF no se genera automáticamente al finalizar (pendiente)
    end
```

### 3.4 Diagrama de Colaboraciones (DCO-S3-Atencion_Consulta_RecetaPDF)
```mermaid
sequenceDiagram
    participant CC as CitaController
    participant COC as ConsultaController
    participant BD as BaseDatos
    participant EXP as Expediente
    participant CON as Consulta
    participant REC as Receta
    participant MED as Medicamento
    participant SV as SignosVitales
    participant PDF as ControladorPDF
    participant ADM as Administrador

    1. CC->>COC: Iniciar atención (validar no CANCELADA)
    2. COC->>BD: Cargar datos clínicos previos
    3. BD-->>COC: Paciente/Expediente/Signos
    4. COC->>BD: guardarAtencion() coordinación transaccional
    5. BD->>EXP: Resolver/Crear Expediente
    6. BD->>CON: Persistir nota clínica
    7. BD->>REC: Persistir Receta (vinculada a Cita/Administrador)
    8. BD->>MED: Persistir Detalle_receta (reemplazo transaccional)
    9. BD->>SV: Persistir SignosVitales
    10. BD->>BD: Commit / Rollback si falla
    11. alt Finalizar
        12. BD->>BD: Marcar Cita ATENDIDA
    end
    13. alt Generar PDF
        14. COC->>PDF: generarPDF(Receta)
        15. PDF->>ADM: Obtener datos fijos prescriptor/establecimiento
        16. PDF->>REC: Obtener datos para impresión
        17. PDF-->>COC: Archivo PDF generado
    end
```

## Trazabilidad

| Elemento del código | Diagramas donde aparece |
| :-- | :-- |
| `BaseDatos` | S1, S2, S3 |
| `Launcher/HelloApplication` | S1 |
| `LoginController` | S1 |
| `AgendaController` | S1, S2, S3 |
| `CitaController` | S2, S3 |
| `ConsultaController` | S3 |
| `ControladorCorreo` | S2 |
| `ControladorPDF` | S3 |
| `Paciente` | S1, S2 |
| `Cita` / `Estado` | S1, S2, S3 |
| `Expediente` | S1, S3 |
| `Consulta` | S3 |
| `Receta` | S3 |
| `Medicamento` | S3 |
| `SignosVitales` | S3 |
| `Administrador` / `Asistente` | S1, S2, S3 |

## Notas

- La autenticación mostrada en S1 es **parcial** (RF-01). Hoy no hay validación contra base de datos.
- En S2, el envío por correo es **condicional**: si el paciente no tiene correo o la configuración SMTP está incompleta, la cita se guarda igualmente (RN-15).
- En S3, `guardarAtencion` actúa como operación coordinadora transaccional. Al finalizar se marca la cita como `ATENDIDA` (RN-12, RN-13), pero **la generación de PDF no es automática** (pendiente de implementación).
- Se consideran los pendientes documentados: transferencia del motivo de recepción, prioridad de signos al reabrir, validación numérica de recepción y regeneración/custodia de PDF.
