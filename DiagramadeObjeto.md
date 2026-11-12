```mermaid
objectDiagram
    direction LR

    object main {
        pacientes = HashMap<String, Paciente>
        citas = HashMap<Integer, Cita>
    }

    object pacienteJuan {
        curp = "GOMJ800101HDFRNN09"
        fechaNacimiento = 1980-01-01
        sexo = "Masculino"
        domicilio = "Av. Reforma 123, CDMX"
        telefono = "5551234567"
        correo = "juan.gomez@email.com"
    }

    object cita1 {
        idCita = 1001
        fechaCita = 2026-11-12 09:30:00
        estado = AGENDADA
        curp = "GOMJ800101HDFRNN09"
    }

    object estadoAgendada {
        <<enumeration>>
        AGENDADA
    }

    object administradorDrLopez {
        idAdministrador = 1
        cedulaProfesional = "12345678"
        institucionAcademica = "UNAM"
        domicilio = "Consultorio 5, CDMX"
        nombre = "Carlos"
        apellidoPaterno = "López"
        apellidoMaterno = "Hernández"
    }

    object asistenteAna {
        idAsistente = 101
        contrasena = "********"
        nombre = "Ana"
        apellidoPaterno = "Martínez"
        apellidoMaterno = "Ruiz"
    }

    object expedienteJuan {
        idExpediente = 5001
        fechaApertura = 2026-10-01
        antecedentesFamiliares = "Diabetes tipo 2 (padre)"
        antecedentesPersonales = "Hipertensión controlada"
        alergias = "Penicilina"
        observaciones = "Paciente requiere seguimiento anual"
    }

    object consulta1 {
        idConsulta = 2001
        idExpediente = 5001
        idCita = 1001
        fechaConsulta = 2026-11-12 09:40:00
        padecimiento = "Hipertensión arterial"
        motivo = "Control de presión arterial"
        observaciones = "Presión arterial elevada, se ajusta tratamiento"
    }

    object signosVitales1 {
        presionArterial = "140/90"
        frecuenciaCardiaca = 88
        frecuenciaRespiratoria = 18
        temperaturaCorporal = 36.5
        saturacionOxigeno = 98
        peso = 78.5
        talla = 175
    }

    object receta1 {
        idReceta = 3001
        idCita = 1001
        idAdministrador = 1
        curp = "GOMJ800101HDFRNN09"
        fecha = 2026-11-12
        diagnostico = "Hipertensión arterial"
    }

    object medicamento1 {
        idDetalleReceta = 4001
        idReceta = 3001
        nombreMedicamento = "Enalapril"
        dosis = "10 mg"
        concentracion = "10 mg/tableta"
        frecuencia = "Cada 24 horas"
        duradcion = "30 días"
    }

    object medicamento2 {
        idDetalleReceta = 4002
        idReceta = 3001
        nombreMedicamento = "Hidroclorotiazida"
        dosis = "25 mg"
        concentracion = "25 mg/tableta"
        frecuencia = "Cada 24 horas"
        duradcion = "30 días"
    }

    main --> pacienteJuan : administra
    main --> cita1 : gestiona
    cita1 --> estadoAgendada : utiliza
    cita1 --> pacienteJuan : agendada para
    administradorDrLopez --> asistenteAna : administra
    administradorDrLopez --> expedienteJuan : gestiona
    administradorDrLopez --> cita1 : registra y atiende
    administradorDrLopez --> receta1 : emite
    expedienteJuan o-- pacienteJuan : datos del paciente
    expedienteJuan *-- receta1 : contiene
    expedienteJuan *-- consulta1 : registra
    consulta1 --> receta1 : genera
    consulta1 --> signosVitales1 : registra
    consulta1 --> cita1 : corresponde a
    receta1 *-- medicamento1 : prescribe
    receta1 *-- medicamento2 : prescribe
    receta1 --> signosVitales1 : registra
    receta1 --> cita1 : pertenece a
```
