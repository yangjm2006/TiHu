package com.tihu.frontend;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.tihu.frontend.utils.AppContext;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        AppContext.getInstance().setPrimaryStage(stage);
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 520, 380);
        stage.setTitle("TiHu");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] arg) {
        launch();
    }
}
