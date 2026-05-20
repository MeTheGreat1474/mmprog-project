package com.example.project.models;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GeometricTransformations 
{
    public static Image scale(Image source, double factor) 
    {
        // Ensure dimensions are at least 1 pixel to prevent crash on tiny scale factorss
        int width = Math.max(1, (int) (source.getWidth() * factor));
        int height = Math.max(1, (int) (source.getHeight() * factor));
        
        int srcWidth = (int) source.getWidth();
        int srcHeight = (int) source.getHeight();
        
        WritableImage newImage = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = newImage.getPixelWriter();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                // Backward mapping calculation
                int srcX = (int) (x / factor);
                int srcY = (int) (y / factor);
                
                // Fix: Use Math.min to force coordinates to stay inside the maximum valid index (size - 1)
                if (srcX >= srcWidth)  srcX = srcWidth - 1;
                if (srcY >= srcHeight) srcY = srcHeight - 1;
                
                // Double check for negative boundaries just in case factor is erratic
                if (srcX < 0) srcX = 0;
                if (srcY < 0) srcY = 0;

                writer.setArgb(x, y, reader.getArgb(srcX, srcY));
            }
        }
        return newImage;
    }   

    public static Image rotate(Image sourceImage, double angleDegrees) {
        int width = (int) sourceImage.getWidth();
        int height = (int) sourceImage.getHeight();
        
        // Convert degrees to radians for Math functions
        double radians = Math.toRadians(angleDegrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        // Calculate new bounding box dimensions so corners don't get cut off
        int newWidth = (int) Math.ceil(Math.abs(width * cos) + Math.abs(height * sin));
        int newHeight = (int) Math.ceil(Math.abs(width * sin) + Math.abs(height * cos));

        WritableImage resultImage = new WritableImage(newWidth, newHeight);
        PixelReader reader = sourceImage.getPixelReader();
        PixelWriter writer = resultImage.getPixelWriter();

        // Source image center
        double cx = width / 2.0;
        double cy = height / 2.0;

        // Destination image center
        double ncx = newWidth / 2.0;
        double ncy = newHeight / 2.0;

        // Backward mapping to prevent gaps/holes in the rotated output
        for (int dstY = 0; dstY < newHeight; dstY++) {
            for (int dstX = 0; dstX < newWidth; dstX++) {
                // Relocate coordinates relative to the new center
                double relX = dstX - ncx;
                double relY = dstY - ncy;

                // Rotate back to find the original pixel spot
                int srcX = (int) Math.round(relX * cos + relY * sin + cx);
                int srcY = (int) Math.round(-relX * sin + relY * cos + cy);

                // If the mapped coordinate lands inside the original bounds, copy it
                if (srcX >= 0 && srcX < width && srcY >= 0 && srcY < height) {
                    writer.setColor(dstX, dstY, reader.getColor(srcX, srcY));
                } else {
                    // Set out-of-bounds area to transparent canvas space
                    writer.setColor(dstX, dstY, Color.TRANSPARENT);
                }
            }
        }
        return resultImage;
    }
}
