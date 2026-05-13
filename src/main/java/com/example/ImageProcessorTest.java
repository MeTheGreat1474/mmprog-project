package com.example;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageProcessorTest 
{

    public static Image adjustBrightness(Image source, double value) 
    {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        WritableImage output = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = output.getPixelWriter();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                Color c = reader.getColor(x, y);
                // Clamp values between 0.0 and 1.0
                double r = Math.min(1.0, Math.max(0.0, c.getRed() + value));
                double g = Math.min(1.0, Math.max(0.0, c.getGreen() + value));
                double b = Math.min(1.0, Math.max(0.0, c.getBlue() + value));
                writer.setColor(x, y, new Color(r, g, b, c.getOpacity()));
            }
        }
        return output;
    }
}
