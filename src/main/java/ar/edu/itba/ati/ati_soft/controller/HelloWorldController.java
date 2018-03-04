package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.HelloWorldService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for the Hello World GUI.
 */
@FXMLController
public class HelloWorldController {

    private final HelloWorldService helloWorldService;

    @Autowired
    public HelloWorldController(HelloWorldService helloWorldService) {
        this.helloWorldService = helloWorldService;
    }

    @FXML
    public void closeApplication() {
        Platform.exit();
    }
}
