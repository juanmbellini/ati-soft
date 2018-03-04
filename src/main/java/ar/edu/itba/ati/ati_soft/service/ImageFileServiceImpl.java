package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageFileService;
import ar.edu.itba.ati.ati_soft.interfaces.UnsupportedImageFileException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

/**
 * A concrete implementation of the {@link ImageFileService}.
 */
@Service
public class ImageFileServiceImpl implements ImageFileService {

    @Override
    public BufferedImage openImage(File imageFile) throws IOException, UnsupportedImageFileException {
        Assert.notNull(imageFile, "The image file must not be null");
        return Optional.ofNullable(ImageIO.read(imageFile))
                .orElseThrow(() -> new UnsupportedImageFileException("Could not decode the given file into an image"));
    }

    @Override
    public void saveImage(String fileName) {
        // TODO: implement
    }
}
