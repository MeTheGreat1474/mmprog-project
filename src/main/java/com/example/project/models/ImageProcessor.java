package com.example.project.models;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageProcessor
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

    public static Image convertToGrayscale(Image originalImage) {

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        WritableImage grayImage = new WritableImage(width, height);

        PixelReader pixelReader = originalImage.getPixelReader();
        PixelWriter pixelWriter = grayImage.getPixelWriter();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {

                Color color = pixelReader.getColor(x, y);

                Color grayColor = color.grayscale();

                pixelWriter.setColor(x, y, grayColor);
            }
        }

        return grayImage;
    }

    public static Image adjustContrast(Image originalImage, double contrast) 
    {

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        WritableImage outputImage = new WritableImage(width, height);

        PixelReader pixelReader = originalImage.getPixelReader();
        PixelWriter pixelWriter = outputImage.getPixelWriter();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {

                Color color = pixelReader.getColor(x, y);

                double red = ((color.getRed() - 0.5) * contrast) + 0.5;
                double green = ((color.getGreen() - 0.5) * contrast) + 0.5;
                double blue = ((color.getBlue() - 0.5) * contrast) + 0.5;

                red = clamp(red);
                green = clamp(green);
                blue = clamp(blue);

                pixelWriter.setColor(x, y, new Color(red, green, blue, color.getOpacity()));
            }
        }

        return outputImage;
    }

    private static double clamp(double value) 
    {

        if (value < 0.0) 
        {
            return 0.0;
        }

        if (value > 1.0) 
        {
            return 1.0;
        }

        return value;
    }

    public static Image addBorder(Image originalImage) 
    {

        int borderSize = 20;

        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();

        WritableImage borderedImage = new WritableImage(width + borderSize * 2, height + borderSize * 2);

        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = borderedImage.getPixelWriter();

        for (int y = 0; y < height + borderSize * 2; y++) 
        {
            for (int x = 0; x < width + borderSize * 2; x++) 
            {

                if (x < borderSize || y < borderSize || x >= width + borderSize || y >= height + borderSize) 
                {
                    writer.setColor(x, y, Color.BLACK);
                } 
                else
                {
                    Color color = reader.getColor(x - borderSize, y - borderSize);
                    writer.setColor(x, y, color);
                }
            }
        }

        return borderedImage;
    }
}
