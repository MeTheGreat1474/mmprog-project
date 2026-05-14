package com.example.project.services;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MosaicService {

    private static class TileImage {

        Mat image;

        Scalar averageColor;

        TileImage(Mat image, Scalar averageColor) {

            this.image = image;

            this.averageColor = averageColor;
        }
    }

    public void createMosaic(
            String targetImagePath,
            String tileFolderPath,
            String outputPath,
            int tileSize
    ) {

        Mat targetImage =
                Imgcodecs.imread(targetImagePath);

        if (targetImage.empty()) {

            throw new RuntimeException(
                    "Failed to load target image."
            );
        }

        List<TileImage> tileImages =
                loadTileImages(tileFolderPath, tileSize);

        if (tileImages.isEmpty()) {

            throw new RuntimeException(
                    "No valid tile images found."
            );
        }

        int rows = targetImage.rows() / tileSize;

        int cols = targetImage.cols() / tileSize;

        int mosaicWidth = cols * tileSize;

        int mosaicHeight = rows * tileSize;

        Mat resizedTarget = new Mat();

        Imgproc.resize(
                targetImage,
                resizedTarget,
                new Size(mosaicWidth, mosaicHeight)
        );

        Mat mosaicImage = new Mat(
                resizedTarget.size(),
                resizedTarget.type()
        );

        for (int y = 0; y < rows; y++) {

            for (int x = 0; x < cols; x++) {

                Rect region = new Rect(
                        x * tileSize,
                        y * tileSize,
                        tileSize,
                        tileSize
                );

                Mat targetBlock =
                        resizedTarget.submat(region);

                Scalar targetAverage =
                        Core.mean(targetBlock);

                TileImage bestTile =
                        findBestMatch(
                                targetAverage,
                                tileImages
                        );

                bestTile.image.copyTo(
                        mosaicImage.submat(region)
                );
            }
        }

        Imgcodecs.imwrite(outputPath, mosaicImage);
    }

    private List<TileImage> loadTileImages(
            String folderPath,
            int tileSize
    ) {

        List<TileImage> tileImages =
                new ArrayList<>();

        File folder = new File(folderPath);

        File[] files = folder.listFiles();

        if (files == null) {
            return tileImages;
        }

        for (File file : files) {

            String name =
                    file.getName().toLowerCase();

            if (!(name.endsWith(".png")
                    || name.endsWith(".jpg")
                    || name.endsWith(".jpeg"))) {

                continue;
            }

            Mat image =
                    Imgcodecs.imread(
                            file.getAbsolutePath()
                    );

            if (image.empty()) {
                continue;
            }

            Imgproc.resize(
                    image,
                    image,
                    new Size(tileSize, tileSize)
            );

            Scalar averageColor =
                    Core.mean(image);

            tileImages.add(
                    new TileImage(
                            image,
                            averageColor
                    )
            );
        }

        return tileImages;
    }

    private TileImage findBestMatch(
            Scalar targetColor,
            List<TileImage> tileImages
    ) {

        TileImage bestMatch = null;

        double minimumDistance =
                Double.MAX_VALUE;

        for (TileImage tile : tileImages) {

            double distance =
                    calculateColorDistance(
                            targetColor,
                            tile.averageColor
                    );

            if (distance < minimumDistance) {

                minimumDistance = distance;

                bestMatch = tile;
            }
        }

        return bestMatch;
    }

    private double calculateColorDistance(
            Scalar color1,
            Scalar color2
    ) {

        double blue =
                color1.val[0] - color2.val[0];

        double green =
                color1.val[1] - color2.val[1];

        double red =
                color1.val[2] - color2.val[2];

        return Math.sqrt(
                (blue * blue)
                        + (green * green)
                        + (red * red)
        );
    }
}