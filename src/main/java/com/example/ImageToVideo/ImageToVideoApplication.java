package com.example.ImageToVideo;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

//import org.bytedeco.javacv.OpenCVFrameGrabber;

@SpringBootApplication
public class ImageToVideoApplication {

	private static Logger logger = LoggerFactory.getLogger(ImageToVideoApplication.class);

	private static String imageFileDir = "data/input";
	private static String videoFileDir = "data/output";

	private static String outputFile = "data/output/video_raw.mp4";
	private static String audioFile = "data/audio/An-Epic-Story.mp3";
	private static String finalFile = "data/output/video_sound.mp4";

	private static int width = 640;  // Set to your image width
	private static int height = 480; // Set to your image height
	private static int fps = 30;

	private static int videoLengthSeconds;
	private static int numberOfImages;

	public static void main(String[] args) {
		SpringApplication.run(ImageToVideoApplication.class, args);
		logger.info("Launch ImageToVideo");

		//getMP3Duration();
		videoLengthSeconds = getMP3Duration()+1;
		numberOfImages = fps * videoLengthSeconds;
		logger.info("Need to create {} images", numberOfImages);

		cleanUpDisk(imageFileDir);
		createImageSequence();

		cleanUpDisk(videoFileDir);
		createVideoWithoutSound();
		createVideoWithSound();

	}

	private static void cleanUpDisk(String folderPath) {
		// Specify the folder path
		//String folderPath = "path/to/your/folder";

		// Create a File object for the folder
		File folder = new File(folderPath);

		// Check if the folder exists and is a directory
		if (folder.exists() && folder.isDirectory()) {
			// Get all files in the folder
			File[] files = folder.listFiles();

			if (files != null) {
				for (File file : files) {
					// Check if the file is a file (not a directory)
					if (file.isFile()) {
						// Delete the file
						if (file.delete()) {
							logger.debug("Deleted file: " + file.getName());
						} else {
							logger.error("Failed to delete file: " + file.getName());
						}
					}
				}
			} else {
				logger.error("The folder is empty or an error occurred.");
			}
		} else {
			logger.error("The specified path is not a folder or does not exist.");
		}
	}

	private static void createImageSequence() {

		for(int i=0; i<numberOfImages; i++) {
			// Create a buffered image
			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			// Get the graphics object to draw on the image
			Graphics2D g2d = bufferedImage.createGraphics();

			// Fill the background with white
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, width, height);

			// Draw a rectangle
			g2d.setColor(Color.RED);
			g2d.fillRect(i, 100, 100, 100);

			// Draw some text
			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Arial", Font.BOLD, 50));
			g2d.drawString("Hello, World!", 150, 400);

			// Dispose of the graphics context to release resources
			g2d.dispose();

			// Save the image to disk
			File file = new File(imageFileDir + "/image" + i +".jpg");
			try {
				ImageIO.write(bufferedImage, "jpg", file);
				logger.debug("Image saved successfully to " + file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		logger.info("Created {} images", numberOfImages);
	}

	private static void createVideoWithoutSound() {

		// Initialize the FFmpegFrameRecorder
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, width, height);
		recorder.setFormat("mp4");
		recorder.setFrameRate(fps);
		//recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		//recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);

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
				logger.debug("Image read successfully from " + imgFile.getAbsolutePath());
				BufferedImage img = ImageIO.read(imgFile);
				Frame frame = converter.convert(img);
				recorder.record(frame);
			}

			recorder.stop();
			recorder.release();
			logger.info("Video without sound successfully created under " + videoFileDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createVideoWithSound() {
		try {
			// Create the recorder
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(finalFile, width, height, 2);
			recorder.setFormat("mp4");
			recorder.setFrameRate(fps);
			//recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			// recorder.setVideoQuality(0); // Maximum quality
			// recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			// recorder.setAudioBitrate(192000);
			// recorder.setSampleRate(44100);
			recorder.start();

			// create audio grabber
			FrameGrabber grabber = new FFmpegFrameGrabber(audioFile);
			grabber.start();

			// Add video frames
			Java2DFrameConverter converter = new Java2DFrameConverter();
			for (int i = 0; ; i++) {
				// read file
				File imgFile = new File(imageFileDir, "image" + i + ".jpg");
				if (!imgFile.exists()) {
					break;
				}
				logger.debug("Image read successfully from " + imgFile.getAbsolutePath());
				// create image and add as new frame
				BufferedImage img = ImageIO.read(imgFile);
				Frame frame = converter.convert(img);
				recorder.record(frame);
			}

			// Add audio frames
			Frame audioFrame;
			while ((audioFrame = grabber.grab()) != null) {
				recorder.record(audioFrame);
			}

			grabber.stop();
			recorder.stop();
			recorder.release();

			logger.info("Video with sound created successfully!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static int getMP3Duration() {
		try {
			// create audio grabber
			FrameGrabber grabber = new FFmpegFrameGrabber(audioFile);
			grabber.start();

			logger.info("MP3 frameRate = {}", grabber.getFrameRate());
			logger.info("MP3 frameNumber = {}", grabber.getFrameNumber());
			logger.info("MP3 lengthInFrames = {}", grabber.getLengthInFrames());
			logger.info("MP3 lengthInTime = {} seconds", grabber.getLengthInTime() / 1000000);
			int mp3DurationSeconds = (int) grabber.getLengthInTime() / 1000000;

			// loop through audio frames
			double loop = 0;
			while (grabber.grab() != null) {
				loop++;
			}
			grabber.stop();
			logger.info("Grabber looped through {} times", loop);

			return mp3DurationSeconds;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to count frames");
			return 0;
		}
	}

}
