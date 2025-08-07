package com.calcifer;

import com.calcifer.controller.MainController;
import com.calcifer.service.AdbService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * @author CYC
 */
public class MainApp extends Application {

    private static MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        primaryStage.getIcons().addAll(

                new Image(getClass().getResourceAsStream("/images/icon.ico"))
        );

        Scene scene = new Scene(root, 1300, 700);
        primaryStage.setTitle("SPD CHECK TOOL");
        primaryStage.setScene(scene);
        primaryStage.show();

        AdbService.initializeConfigDirectory();

        // 添加关闭钩子
        primaryStage.setOnCloseRequest(event -> {
            controller.shutdown();
        });
    }

    public static void main(String[] args) {
        System.out.println("JavaFX Version: " + System.getProperty("javafx.version"));
        Application.launch(args);
    }
}