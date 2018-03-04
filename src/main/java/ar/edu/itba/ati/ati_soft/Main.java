package ar.edu.itba.ati.ati_soft;

import ar.edu.itba.ati.ati_soft.view.HelloWorldView;
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main class.
 */
@SpringBootApplication
public class Main extends AbstractJavaFxApplicationSupport {

    /**
     * Entry point.
     *
     * @param args Execution arguments.
     */
    public static void main(String[] args) {
        AbstractJavaFxApplicationSupport.launchApp(Main.class, HelloWorldView.class, args);
    }
}
