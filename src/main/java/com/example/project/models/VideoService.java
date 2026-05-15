package com.example.project.models;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoWriter;

import java.util.List;

public class VideoService {

    private static final int VIDEO_WIDTH = 1280;

    private static final int VIDEO_HEIGHT = 720;

    private static final int FPS = 30;

    private static final int SECONDS_PER_IMAGE = 3;

    public void createVideo(
            List<String> imagePaths,
            String outputVideoPath,
            String overlayText
    ) {

        Size frameSize = new Size(
                VIDEO_WIDTH,
                VIDEO_HEIGHT
        );

        VideoWriter videoWriter = new VideoWriter(
                outputVideoPath,
                VideoWriter.fourcc('m', 'p', '4', 'v'),
                FPS,
                frameSize
        );

        if (!videoWriter.isOpened()) {

            throw new RuntimeException(
                    "Failed to create video writer."
            );
        }

        int totalFrames =
                FPS * SECONDS_PER_IMAGE;

        for (String imagePath : imagePaths) {

            Mat image =
                    Imgcodecs.imread(imagePath);

            if (image.empty()) {
                continue;
            }

            Imgproc.resize(
                    image,
                    image,
                    frameSize
            );

            addOverlay(image, overlayText);

            for (int i = 0; i < totalFrames; i++) {

                videoWriter.write(image);
            }
        }

        videoWriter.release();
    }

    private void addOverlay(
            Mat image,
            String overlayText
    ) {

        if (overlayText == null
                || overlayText.isBlank()) {

            return;
        }

        int rectangleHeight = 120;

        Imgproc.rectangle(
                image,
                new Point(0, VIDEO_HEIGHT - rectangleHeight),
                new Point(VIDEO_WIDTH, VIDEO_HEIGHT),
                new Scalar(0, 0, 0),
                -1
        );

        Imgproc.putText(
                image,
                overlayText,
                new Point(40, VIDEO_HEIGHT - 50),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                1.2,
                new Scalar(255, 255, 255),
                2
        );
    }
}
