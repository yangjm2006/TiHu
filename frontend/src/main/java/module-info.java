module com.tihu.frontend {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.tihu.frontend to javafx.fxml;
    exports com.tihu.frontend;
}