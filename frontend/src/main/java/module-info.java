module com.tihu.frontend {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.tihu.frontend to javafx.fxml;
    exports com.tihu.frontend;
    exports com.tihu.frontend.Controller;
    opens com.tihu.frontend.Controller to javafx.fxml;
}