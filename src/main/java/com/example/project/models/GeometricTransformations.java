package com.example.project.models;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class GeometricTransformations 
{
    public static Image scale(Image source, double factor) 
    {
        int width = (int) (source.getWidth() * factor);
        int height = (int) (source.getHeight() * factor);
        
        WritableImage newImage = new WritableImage(width, height);
        PixelReader reader = source.getPixelReader();
        PixelWriter writer = newImage.getPixelWriter();

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                // Mapping the new pixel back to the original source coordinates
                int srcX = (int) (x / factor);
                int srcY = (int) (y / factor);
                
                if (srcX < source.getWidth() && srcY < source.getHeight()) 
                {
                    writer.setArgb(x, y, reader.getArgb(srcX, srcY));
                }
            }
        }
        return newImage;
    }
}
