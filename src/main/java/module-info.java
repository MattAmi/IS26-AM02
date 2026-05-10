module it.polimi.ingsw.am02 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.rmi;
    requires java.management;
    requires java.desktop;

    exports it.polimi.ingsw.am02.client.view.gui to javafx.graphics;

    // Fondamentale: permetti a RMI di vedere le tue interfacce e classi remote
    exports it.polimi.ingsw.am02.common.network.rmi;

    // RMI ha bisogno di accedere a questi package per gestire gli stub
    opens it.polimi.ingsw.am02.common.network.rmi to java.rmi;
    opens it.polimi.ingsw.am02.client.network.rmi to java.rmi;
    opens it.polimi.ingsw.am02.server.network.rmi to java.rmi;

    // Necessario per Jackson: deve accedere ai record via reflection
    opens it.polimi.ingsw.am02.common.messages.commands    to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.messages.events      to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.messages.events.game to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.messages.events.lobby to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.messages.events.error to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.dto                  to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.enumerations          to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am02.common.serialization         to com.fasterxml.jackson.databind;

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

    exports it.polimi.ingsw.am02.server.network.socket;
    exports it.polimi.ingsw.am02.client.network.socket;

}