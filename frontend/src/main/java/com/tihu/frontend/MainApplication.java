package com.tihu.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader=new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene =new Scene(fxmlLoader.load(),400,300);
        stage.setTitle("TiHu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] arg) {
        launch();
    }
}
