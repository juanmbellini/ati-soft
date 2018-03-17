package ar.edu.itba.ati.ati_soft.models;

import java.awt.image.BufferedImage;

/**
 * Represents a picture.
 */
public class Image {

    /**
     * The content of the image (i.e an array of pixels).
     */
    private final BufferedImage content;

    /**
     * Constructor.
     *
     * @param content The content of the image (i.e an array of pixels).
     */
    public Image(BufferedImage content) {
        this.content = content;
    }


    /**
     * @return The content of the image (i.e an array of pixels).
     */
    public BufferedImage getContent() {
        return content;
    }
}
