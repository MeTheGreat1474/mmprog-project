package com.example;

import javafx.scene.image.*;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;

public class ObjectSelector 
{
    public static Image extractByColor(Image sourceImage, Color targetColor, double threshold) 
    {
        int width = (int) sourceImage.getWidth();
        int height = (int) sourceImage.getHeight();
        WritableImage resultImage = new WritableImage(width, height);
        PixelReader reader = sourceImage.getPixelReader();
        PixelWriter writer = resultImage.getPixelWriter();

        // 1. Convert our target selection color into HSV channels
        double targetHue = targetColor.getHue();          // 0.0 to 360.0 degrees
        double targetSat = targetColor.getSaturation();   // 0.0 to 1.0
        double targetVal = targetColor.getBrightness();   // 0.0 to 1.0

        // Soft blend width boundary: makes edges smooth instead of sharp/pixelated
        double featherZone = 0.12; 

        for (int y = 0; y < height; y++) 
        {
            for (int x = 0; x < width; x++) 
            {
                Color pixelColor = reader.getColor(x, y);
                
                // 2. Convert current pixel to HSV channels
                double currentHue = pixelColor.getHue();
                double currentSat = pixelColor.getSaturation();
                double currentVal = pixelColor.getBrightness();

                // 3. Handle the 360-degree cylindrical wrap-around of the Hue wheel
                double hueDiff = Math.abs(currentHue - targetHue);
                if (hueDiff > 180.0) 
                {
                    hueDiff = 360.0 - hueDiff;
                }
                double normalizedHueDiff = hueDiff / 180.0; // Map to 0.0 - 1.0 range

                double satDiff = Math.abs(currentSat - targetSat);
                double valDiff = Math.abs(currentVal - targetVal);

                // 4. Perceptually weighted formula: Prioritize Hue (color) over Value (shadows/highlights)
                double calculatedDistance = (normalizedHueDiff * 0.65) + (satDiff * 0.20) + (valDiff * 0.15);

                // 5. Apply Soft Feathering segmentation boundary checks
                if (calculatedDistance <= threshold) 
                {
                    // Crisp, highly matching interior pixel remains untouched
                    writer.setColor(x, y, pixelColor);
                } 
                else if (calculatedDistance < (threshold + featherZone)) 
                {
                    // Anti-aliasing edge blend: calculate a smooth alpha transition gradient
                    double alpha = 1.0 - ((calculatedDistance - threshold) / featherZone);
                    
                    // Constrain alpha to safe rendering limits
                    alpha = Math.max(0.0, Math.min(1.0, alpha));
                    
                    // Create a feathered variant keeping its native RGB properties intact
                    Color featheredColor = new Color(pixelColor.getRed(), pixelColor.getGreen(), pixelColor.getBlue(), alpha * pixelColor.getOpacity());
                    writer.setColor(x, y, featheredColor);
                } 
                else 
                {
                    // Definite background space rendered completely transparent
                    writer.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        return resultImage;
    }

    public static void saveExtractedObject(Image img, String name) 
    {
        try 
        { 
            ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png", new File(name)); 
        }
        catch (Exception e) 
        { 
            e.printStackTrace(); 
        }
    }
}
