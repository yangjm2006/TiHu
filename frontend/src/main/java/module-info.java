module com.tihu.frontend {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.tihu.frontend to javafx.fxml;
    exports com.tihu.frontend;
    exports com.tihu.frontend.controller1;
    opens com.tihu.frontend.controller1 to javafx.fxml;
}