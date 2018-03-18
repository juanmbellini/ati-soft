package ar.edu.itba.ati.ati_soft.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Class containing several methods for helping the file operations.
 */
public class FileHelper {

    /**
     * Gets the extension of the given {@code file}.
     *
     * @param file The {@link File} whose extension will be returned.
     * @return The extension of the given {@link File}.
     */
    public static String getExtension(File file) {
        return FilenameUtils.getExtension(file.getName());
    }
}
