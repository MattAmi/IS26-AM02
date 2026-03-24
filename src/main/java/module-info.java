module it.polimi.ingsw.am02 {
    requires javafx.controls;
    requires javafx.fxml;


    opens it.polimi.ingsw.am02 to javafx.fxml;
    exports it.polimi.ingsw.am02;
}