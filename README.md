# Gestor de Citas Médicas

Aplicación de escritorio para la gestión de citas médicas, desarrollada con **JavaFX + Maven**.

La estandarización de historia clínica, notas de evolución y receta ordinaria para México se describe en [Formatos clínicos y trazabilidad normativa](docs/normativa-mexico.md), incluyendo alcance, fuentes oficiales, migración y requisitos operativos pendientes.

> **Estado actual: solo estructura base (scaffold).**
> El proyecto contiene únicamente la plantilla inicial generada por el IDE: una ventana
> "Hello!" con un botón que cambia un texto. Todavía no existen modelos, base de datos,
> lógica de negocio ni pantallas propias del dominio (citas, pacientes, médicos, etc.).

## Tecnologías

- Java (el `pom.xml` compila con `source/target 8`, pendiente de actualizar a 17+)
- JavaFX 21.0.6 (`controls`, `fxml`, `web`, `swing`)
- ControlsFX, FormsFX, Ikonli y TilesFX (dependencias agregadas, aún sin uso)
- JUnit 5 (dependencias de prueba, aún sin pruebas)
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Estructura

```
Gestor de Citas Medicas/
├── pom.xml
├── mvnw / mvnw.cmd                  # Maven Wrapper
└── src/
    └── main/
        ├── java/
        │   ├── module-info.java                     # Deshabilitado (comentado)
        │   └── com/gestor/gestordecitasmedicas/
        │       ├── Launcher.java                    # Punto de entrada
        │       ├── HelloApplication.java            # Application JavaFX (ventana principal)
        │       └── HelloController.java             # Controlador de la vista de ejemplo
        └── resources/
            └── com/gestor/gestordecitasmedicas/
                └── hello-view.fxml                  # Vista FXML de ejemplo
```

Notas:

- No existe todavía `src/test/`.
- `module-info.java` está comentado, por lo que el proyecto se ejecuta en el classpath.
- Los paquetes `com.gestor.gestordecitasmedicas` y los nombres `Hello*` son los de la
  plantilla; deben renombrarse al implementar el dominio.

## Requisitos

- JDK 17 o superior (JavaFX 21 lo requiere). No es necesario instalar Maven: se usa el wrapper.

## Ejecución

```powershell
# Windows
.\mvnw clean javafx:run
```

```bash
# Linux / macOS
./mvnw clean javafx:run
```

También se puede ejecutar `Launcher` o `HelloApplication` directamente desde el IDE.
