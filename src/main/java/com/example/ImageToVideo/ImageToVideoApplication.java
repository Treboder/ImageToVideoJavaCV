package com.example.ImageToVideo;

import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;

@SpringBootApplication
public class ImageToVideoApplication {

	private static Logger logger = LoggerFactory.getLogger(ImageToVideoApplication.class);

	private static String imageFileDir = "data/input";
	private static String videoFileDir = "data/output";

	public static void main(String[] args) {
		SpringApplication.run(ImageToVideoApplication.class, args);
		logger.info("launch ImageToVideo");
		createImageSequence();
		convert();
	}

	private static void createImageSequence() {
		int width = 800;
		int height = 600;

		// Create a buffered image
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		// Get the graphics object to draw on the image
		Graphics2D g2d = bufferedImage.createGraphics();

		// Fill the background with white
		g2d.setColor(Color.WHITE);
		g2d.fillRect(0, 0, width, height);

		// Draw a rectangle
		g2d.setColor(Color.RED);
		g2d.fillRect(100, 100, 200, 200);

		// Draw some text
		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("Arial", Font.BOLD, 50));
		g2d.drawString("Hello, World!", 150, 400);

		// Dispose of the graphics context to release resources
		g2d.dispose();

		// Save the image to disk
		File file = new File(imageFileDir + "/image0.jpg");
		try {
			ImageIO.write(bufferedImage, "jpg", file);
			System.out.println("Image saved successfully to " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void convert() {
		//String inputDir = "path/to/your/images";
		String outputFile = "data/output/video.mp4";
		int width = 640;  // Set to your image width
		int height = 480; // Set to your image height
		int fps = 30;

		// Initialize the FFmpegFrameRecorder
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setFormat("mp4");
		recorder.setFrameRate(fps);
		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

		try {
			recorder.start();

			// Java2DFrameConverter to convert BufferedImage to Frame
			Java2DFrameConverter converter = new Java2DFrameConverter();

			// Process each image file
			for (int i = 0; ; i++) {
				File imgFile = new File(imageFileDir, "image" + i + ".jpg");
				if (!imgFile.exists()) {
					break;
				}

				BufferedImage img = ImageIO.read(imgFile);
				Frame frame = converter.convert(img);
				recorder.record(frame);
			}

			recorder.stop();
			recorder.release();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
