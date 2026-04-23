module it.polimi.ingsw.am02 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.rmi;


    // Fondamentale: permetti a RMI di vedere le tue interfacce e classi remote
    exports it.polimi.ingsw.am02.common.network.rmi;

    // RMI ha bisogno di accedere a questi package per gestire gli stub
    opens it.polimi.ingsw.am02.common.network.rmi to java.rmi;
    opens it.polimi.ingsw.am02.client.network.rmi to java.rmi;
    opens it.polimi.ingsw.am02.server.network.rmi to java.rmi;

    // Esporta anche i messaggi, altrimenti RMI non sa come leggerli
    exports it.polimi.ingsw.am02.common.messages;
    exports it.polimi.ingsw.am02.common.messages.commands;
    exports it.polimi.ingsw.am02.common.messages.events;
    opens it.polimi.ingsw.am02 to javafx.fxml;
    exports it.polimi.ingsw.am02;

    exports it.polimi.ingsw.am02.server.model;
    exports it.polimi.ingsw.am02.server.model.buildingeffects;
    exports it.polimi.ingsw.am02.server.model.listeners;
    exports it.polimi.ingsw.am02.common.enumerations;
    exports it.polimi.ingsw.am02.server.model.enumerations;
    exports it.polimi.ingsw.am02.server.controller;
    exports it.polimi.ingsw.am02.common.dto;
    exports it.polimi.ingsw.am02.server.network;
    exports it.polimi.ingsw.am02.common.interfaces;
    exports it.polimi.ingsw.am02.server.controller.persistence;

}