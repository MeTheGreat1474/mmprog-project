package com.example;

import java.io.File;

import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ObjectSelector 
{

    /**
     * Logic for Object Selection and Extraction based on color similarity.
     */
    public static Image extractByColor(Image source, Color targetColor, double threshold) {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        WritableImage output = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        boolean foundMatch = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color currentPixel = reader.getColor(x, y);
                
                // Euclidean distance logic 
                double distance = Math.sqrt(
                    Math.pow(currentPixel.getRed() - targetColor.getRed(), 2) +
                    Math.pow(currentPixel.getGreen() - targetColor.getGreen(), 2) +
                    Math.pow(currentPixel.getBlue() - targetColor.getBlue(), 2)
                );

                if (distance <= threshold) {
                    writer.setColor(x, y, currentPixel);
                    foundMatch = true;
                } else {
                    // Make background transparent for the "extraction" effect 
                    writer.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }

        if (!foundMatch) {
            System.out.println("DEBUG: No pixels found within threshold " + threshold + " of color " + targetColor);
        }
        
        return output;
    }

    public static void saveExtractedObject(Image img, String name) {
        try { ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(name)); }
        catch (Exception e) { e.printStackTrace(); }
    }
}
