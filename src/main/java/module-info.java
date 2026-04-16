module it.polimi.ingsw.am02 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens it.polimi.ingsw.am02 to javafx.fxml;
    exports it.polimi.ingsw.am02;

    exports it.polimi.ingsw.am02.model;
    exports it.polimi.ingsw.am02.model.enumerations;
    exports it.polimi.ingsw.am02.model.buildingeffects;
    exports it.polimi.ingsw.am02.common.enumerations;
    exports it.polimi.ingsw.am02.server.model.listeners;
}