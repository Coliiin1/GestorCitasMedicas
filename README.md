# Gestor de Citas Médicas

Aplicación de escritorio para consultorios médicos de consulta externa, desarrollada con **JavaFX 21**, **Maven** y **SQLite**. Cubre la agenda, la recepción del paciente, la consulta clínica, el expediente electrónico y la emisión de recetas médicas ordinarias en PDF, siguiendo como referencia la NOM-004-SSA3-2012 y el Reglamento de Insumos para la Salud.

> [!NOTE]
> El proyecto está en desarrollo activo. El flujo principal (cita → recepción → consulta → receta → expediente) ya funciona de punta a punta, pero aún hay pendientes antes de usarlo en un consultorio real. Consulta [funcionalidades.md](funcionalidades.md) y la [normativa aplicable](docs/normativa-mexico.md).

## Funcionalidades principales

| Módulo | Descripción |
|---|---|
| **Inicio de sesión** | Autenticación por ID numérico y contraseña contra SQLite; hay dos roles: administrador (médico) y asistente. |
| **Dashboard** | Indicadores del día y gráficas (`BarChart`, `PieChart`) de citas agendadas y atendidas. |
| **Agenda** | Vista mensual/semanal; crear, reprogramar y cancelar citas de 60 minutos dentro de los horarios activos. |
| **Horarios y bloqueos** | Varios tramos por día de la semana y bloqueos por rango de fecha/hora con motivo (solo administrador). |
| **Pacientes** | Alta, edición, búsqueda por nombre o CURP y baja; validación de CURP, teléfono y correo, con tipo de sangre y alergias opcionales. Se puede dar de alta a un paciente desde el diálogo de cita. |
| **Recepción (cita en curso)** | Registro de signos vitales y paso a la consulta. |
| **Consulta y receta** | Nota de evolución, diagnóstico y medicamentos estructurados; guardado transaccional y exportación de la receta a PDF. |
| **Expediente clínico** | Historia clínica ambulatoria, historial de consultas, recetas y evolución de signos vitales; edición integrada y exportación a PDF. |
| **Asistentes** | CRUD del personal de recepción (solo administrador). |
| **Configuración** | Datos del consultorio y del médico, credenciales de acceso y carpeta de destino de los PDF. |
| **Notificaciones por correo** | Avisos HTML de confirmación, reprogramación y cancelación de citas vía SMTP, enviados en segundo plano. |
| **Auditoría** | Tablas `Version_*` y triggers que guardan la versión anterior de expedientes, consultas y recetas antes de cada cambio o baja. |

### Permisos por rol

- **Administrador (`admin`)**: acceso completo (dashboard, agenda, horarios, atención clínica, pacientes, expedientes, asistentes y configuración).
- **Asistente (`asis`)**: agenda (agendar, reprogramar y cancelar) y alta de pacientes desde el agendamiento.

## Tecnologías

| Componente | Librería | Versión |
|---|---|---|
| UI | JavaFX (`controls`, `fxml`, `web`, `swing`) | 21.0.6 |
| Componentes extra | ControlsFX / FormsFX / TilesFX | 11.2.1 / 11.6.0 / 21.0.9 |
| Iconos | Ikonli + fuente Uicons (`FlaticonIcons`, SPI propio) | 12.3.1 |
| Base de datos | SQLite (`sqlite-jdbc`) | 3.50.3.0 |
| PDF | iText (`itextpdf`) | 5.4.1 |
| Correo | JavaMail (`javax.mail`) | 1.6.2 |
| Pruebas | JUnit 5 | 5.12.1 |
| Construcción | Maven Wrapper (`mvnw` / `mvnw.cmd`) | — |

El `pom.xml` compila con `source/target 9`, pero la ejecución requiere **JDK 17 o superior** (JavaFX 21).

## Arquitectura

Monolito de escritorio en capas con patrón **MVC**. El detalle completo (diagramas, modelo de datos y flujos) está en [arquitectura.md](arquitectura.md).

- **Vista**: archivos FXML y `cita.css` en `src/main/resources/com/gestor/gestordecitasmedicas/`.
- **Controladores** (`controlador/`): `MainController` hace de *shell* con menú lateral, caché de paneles y control de rol; los paneles implementan `PanelControlado` y `PanelRefrescable`.
- **Servicios** (`servicio/`): `ValidacionClinica`, `TextoExpediente`, `ControladorPDF` y `ControladorCorreo`.
- **Modelo y persistencia** (`modelo/`): entidades de dominio, `BaseDatos` (JDBC), `PlanificadorAgenda` y las migraciones automáticas `MigracionClinica` y `MigracionAgenda`.

## Estructura del proyecto

```
GestorCitasMedicas/
├── pom.xml
├── mvnw / mvnw.cmd                     # Maven Wrapper
├── db.properties                       # Conexión a SQLite (local, ignorado por git)
├── correo.properties                   # Configuración SMTP (opcional, ignorado por git)
├── arquitectura.md                     # Arquitectura del sistema
├── funcionalidades.md                  # Estado del flujo clínico, pendientes y registro de cambios
├── requerimientos.md                   # Requerimientos implementados y pendientes
├── DiagramasdeClase.md                 # Diagrama de clases
├── docs/
│   ├── normativa-mexico.md             # Formatos clínicos y trazabilidad normativa
│   ├── plan-implementacion-pendientes.md
│   └── stitch/                         # Prototipos de interfaz (HTML + PNG)
└── src/
    ├── main/
    │   ├── java/com/gestor/gestordecitasmedicas/
    │   │   ├── Launcher.java           # Punto de entrada
    │   │   ├── HelloApplication.java   # Application JavaFX (carga fuentes y abre el login)
    │   │   ├── controlador/            # Controladores de las vistas
    │   │   ├── servicio/               # PDF, correo y validación clínica
    │   │   ├── icons/                  # Iconos Ikonli personalizados
    │   │   └── modelo/                 # Entidades, BaseDatos, migraciones y planificador
    │   └── resources/
    │       ├── META-INF/services/      # Registro SPI de IkonHandler
    │       ├── Icon/ e Imagenes/       # Ícono, fondo y carrusel del login
    │       └── com/gestor/gestordecitasmedicas/
    │           ├── *.fxml              # 13 vistas y paneles
    │           ├── cita.css            # Estilos de la aplicación
    │           └── fonts/              # uicons-regular-rounded.ttf
    └── test/java/com/gestor/gestordecitasmedicas/   # Pruebas JUnit 5
```

## Requisitos

- **JDK 17 o superior** con `JAVA_HOME` configurado.
- No hace falta instalar Maven, porque se usa el wrapper.

## Configuración

### Base de datos (obligatorio)

Crea un archivo `db.properties` en la raíz del proyecto:

```properties
db.url=jdbc:sqlite:citas_medicas.db
db.password=<contraseña>
```

Si el archivo o la contraseña faltan, la aplicación se cierra por seguridad. En el primer arranque se crean automáticamente las tablas, se aplican las migraciones y se registran:

- un administrador por defecto (**ID `1`, contraseña `admin`**);
- el horario laboral inicial de lunes a viernes (09:00–13:00 y 16:00–18:00).

> [!IMPORTANT]
> El administrador por defecto tiene la cédula de ejemplo `00000000`, así que la validación no permite emitir recetas con él. Completa los datos reales del médico y del establecimiento en **Configuración** y cambia la contraseña.

### Correo electrónico (opcional)

Para enviar avisos de citas, crea `correo.properties` en la raíz o define las variables de entorno equivalentes (las variables tienen prioridad):

```properties
smtp.host=smtp.gmail.com     # SMTP_HOST (por defecto smtp.gmail.com)
smtp.port=587                # SMTP_PORT (por defecto 587)
smtp.user=usuario@dominio    # SMTP_USER
smtp.password=<contraseña>   # SMTP_PASSWORD (contraseña de aplicación)
smtp.from=usuario@dominio    # SMTP_FROM (por defecto smtp.user)
smtp.ssl=false               # SMTP_SSL
smtp.starttls=true           # SMTP_STARTTLS
```

Si no está configurado, la aplicación funciona igual, solo que no envía correos.

### Carpeta de PDF

Las recetas se guardan por defecto en `pdf/receta_<id>.pdf`, relativo al directorio de ejecución. La carpeta se puede cambiar desde **Configuración**. El expediente se exporta a la ruta que elijas en el selector de archivos.

## Ejecución

```powershell
# Windows
.\mvnw clean javafx:run
```

```bash
# Linux / macOS
./mvnw clean javafx:run
```

También puedes ejecutar `Launcher` directamente desde el IDE.

## Pruebas

```powershell
.\mvnw test
```

La suite tiene **33 pruebas** en 9 clases (agenda, horarios, migraciones, citas y recetas, consultas, expediente, formatos clínicos, iconos y alta de pacientes desde la agenda). Cada prueba usa una base SQLite temporal mediante la propiedad `gestor.db.url`, así que `citas_medicas.db` no se modifica.

## Documentación

- [Arquitectura del sistema](arquitectura.md)
- [Funcionalidades, flujo clínico y cambios de base de datos](funcionalidades.md)
- [Requerimientos](requerimientos.md)
- [Diagrama de clases](DiagramasdeClase.md)
- [Formatos clínicos y trazabilidad normativa (México)](docs/normativa-mexico.md)
- [Plan de implementación de pendientes](docs/plan-implementacion-pendientes.md)

## Alcance y limitaciones

- Solo se emiten **recetas ordinarias** (fracciones IV, V y VI). Las fracciones I–III, la firma electrónica y la integración con farmacias están fuera del alcance.
- El PDF queda listo para **firma autógrafa**; el nombre impreso no es una firma.
- La validación revisa la captura, pero no verifica la autenticidad de la cédula ni la idoneidad clínica del tratamiento.
- Los pendientes abiertos (transferencia del motivo de recepción, prioridad de signos al reabrir, custodia y versionado de PDF, entre otros) están en [funcionalidades.md](funcionalidades.md).
