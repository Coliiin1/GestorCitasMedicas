package com.gestor.gestordecitasmedicas;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        Application.launch(HelloApplication.class, args);
        //Los warnings que  se  producen son debido a que se cargan las librerias de JavaFx pero NO  se usan, no es ningun error :D
    }
}