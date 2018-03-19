package ar.edu.itba.ati.ati_soft.service;

import ar.edu.itba.ati.ati_soft.interfaces.ImageChangerService;
import ar.edu.itba.ati.ati_soft.models.Image;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Concrete implementation of {@link ImageChangerService}.
 */
@Service
public class ImageChangerServiceImpl implements ImageChangerService {

    @Override
    public Image getNegative(Image image) {
        final BufferedImage content = image.getContent();
        final WritableRaster newRaster = getNegativeRaster(content);
        final ColorModel colorModel = content.getColorModel();
        final boolean preMultiplied = content.isAlphaPremultiplied();
        final Hashtable<String, Object> properties = getProperties(content);
        final BufferedImage newContent = new BufferedImage(colorModel, newRaster, preMultiplied, properties);
        return new Image(newContent);
    }

    /**
     * Calculates the negative raster for the given {@link BufferedImage}.
     *
     * @param bufferedImage The {@link BufferedImage} whose negative must be calculated.
     * @return A {@link WritableRaster} with the negative of the given {@code bufferedImage}.
     */
    private static WritableRaster getNegativeRaster(BufferedImage bufferedImage) {
        final Raster raster = bufferedImage.getRaster();
        final WritableRaster newRaster = Raster.createWritableRaster(raster.getSampleModel(), null);
        for (int x = 0; x < raster.getWidth(); x++) {
            for (int y = 0; y < raster.getHeight(); y++) {
                final int[] pixel = new int[newRaster.getNumDataElements()];
                for (int i = 0; i < pixel.length; i++) {
                    final int value = raster.getSample(x, y, i);
                    pixel[i] = 0xFF - value;
                }
                newRaster.setPixel(x, y, pixel);
            }
        }
        return newRaster;
    }

    /**
     * Gets the properties of the given {@link BufferedImage}.
     *
     * @param bufferedImage The {@link BufferedImage} whose properties must be calculated.
     * @return The properties {@link Hashtable} of the given {@code bufferedImage}.
     */
    private static Hashtable<String, Object> getProperties(BufferedImage bufferedImage) {
        return Optional.ofNullable(bufferedImage.getPropertyNames())
                .map(Arrays::stream)
                .orElse(Stream.empty())
                .map(name -> new AbstractMap.SimpleEntry<>(name, bufferedImage.getProperty(name)))
                .collect(Hashtable::new, (t, e) -> t.put(e.getKey(), e.getValue()), Hashtable::putAll);
    }

}
