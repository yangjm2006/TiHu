module com.tihu.frontend {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;

    opens com.tihu.frontend to javafx.fxml;
    exports com.tihu.frontend;

    opens com.tihu.frontend.controller to javafx.fxml;
    exports com.tihu.frontend.controller;

    opens com.tihu.frontend.service to javafx.fxml;
    exports com.tihu.frontend.service;

    opens com.tihu.frontend.utils to javafx.fxml;
    exports com.tihu.frontend.utils;

    opens com.tihu.frontend.request to javafx.fxml;
    exports com.tihu.frontend.request;
}