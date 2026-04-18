module it.polimi.ingsw.am02 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens it.polimi.ingsw.am02 to javafx.fxml;
    exports it.polimi.ingsw.am02;

    exports it.polimi.ingsw.am02.server.model;
    exports it.polimi.ingsw.am02.server.model.buildingeffects;
    exports it.polimi.ingsw.am02.server.model.listeners;
    exports it.polimi.ingsw.am02.common.enumerations;
    exports it.polimi.ingsw.am02.server.model.enumerations;
    exports it.polimi.ingsw.am02.server.controller;
    exports it.polimi.ingsw.am02.common.dto;
}