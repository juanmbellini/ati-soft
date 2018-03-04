package ar.edu.itba.ati.ati_soft.controller;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controller class for the main view.
 */
@FXMLController
public class HomeController {

    private final ImageFileService imageFileService;

    @Autowired
    public HomeController(ImageFileService imageFileService) {
        this.imageFileService = imageFileService;
    }

    @FXML
    public void closeApplication() {
        Platform.exit();
    }
}
