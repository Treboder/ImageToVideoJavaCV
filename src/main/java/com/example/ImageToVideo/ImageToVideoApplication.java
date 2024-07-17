package com.example.ImageToVideo;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;
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

//import org.bytedeco.javacv.OpenCVFrameGrabber;

@SpringBootApplication
public class ImageToVideoApplication {

	private static Logger logger = LoggerFactory.getLogger(ImageToVideoApplication.class);

	private static String imageFileDir = "data/input";
	private static String videoFileDir = "data/output";
	private static String outputFile = "data/output/video.mp4";
	private static String audioFile = "data/audio/An-Epic-Story.mp3";

	private static int width = 640;  // Set to your image width
	private static int height = 480; // Set to your image height
	private static int fps = 30;
	private static int numberOfImages = 30 * 5;

	private static String videoFilename = "data/output/video.mp4";
	private static String audioFilename = "data/audio/An-Epic-Story.mp3";
	private static String outputFilename = "data/output/videoWithAudio.mp4";

	public static void main(String[] args) {
		SpringApplication.run(ImageToVideoApplication.class, args);
		logger.info("Launch ImageToVideo");

		//cleanUpDisk(imageFileDir);
		//createImageSequence();

		//cleanUpDisk(videoFileDir);
		//createVideoFromImageSequence();

		//addAudio1();
		//addAudio2();
		addAudio3();
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
							System.out.println("Deleted file: " + file.getName());
						} else {
							System.out.println("Failed to delete file: " + file.getName());
						}
					}
				}
			} else {
				System.out.println("The folder is empty or an error occurred.");
			}
		} else {
			System.out.println("The specified path is not a folder or does not exist.");
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
				System.out.println("Image saved successfully to " + file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void createVideoFromImageSequence() {

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
				System.out.println("Image read successfully from " + imgFile.getAbsolutePath());
				BufferedImage img = ImageIO.read(imgFile);
				Frame frame = converter.convert(img);
				recorder.record(frame);
			}

			recorder.stop();
			recorder.release();
			System.out.println("Video successfully created under " + videoFileDir);
		} catch (IOException e) {
			//e.printStackTrace();
		}
	}

	private static void addAudio1() {
		FrameGrabber grabber1 = new FFmpegFrameGrabber(videoFilename);
		FrameGrabber grabber2 = new FFmpegFrameGrabber(audioFilename);
		try {
			grabber1.start();
			grabber2.start();
			FrameRecorder recorder = new FFmpegFrameRecorder(outputFile,
				grabber1.getImageWidth(), grabber1.getImageHeight(),
				grabber2.getAudioChannels());
			recorder.setFrameRate(grabber1.getFrameRate());
			recorder.setSampleFormat(grabber2.getSampleFormat());
			recorder.setSampleRate(grabber2.getSampleRate());

			recorder.setAudioCodec(grabber2.getAudioCodec());
			int ac = grabber2.getAudioCodec();

			recorder.start();
			Frame frame1, frame2 = null;
			while ((frame1 = grabber1.grabFrame()) != null || (frame2 = grabber2.grabFrame()) != null) {
				recorder.record(frame1);
				recorder.record(frame2);
			}
			recorder.stop();
			grabber1.stop();
			grabber2.stop();
		} catch (FrameGrabber.Exception | FrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}

	private static void addAudio2() {

		FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoFilename);
		FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioFilename);
		FFmpegFrameRecorder recorder = null;

		try {
			// Initialize video grabber
			videoGrabber.start();
			int videoWidth = videoGrabber.getImageWidth();
			int videoHeight = videoGrabber.getImageHeight();

			// Initialize audio grabber
			audioGrabber.start();

			// Initialize recorder
			recorder = new FFmpegFrameRecorder(outputFilename, videoWidth, videoHeight, audioGrabber.getAudioChannels());
			//recorder.setVideoCodec(videoGrabber.getVideoCodec());
			//recorder.setVideoBitrate(videoGrabber.getVideoBitrate());
			recorder.setFormat("mp4");
			recorder.setFrameRate(fps);
			//recorder.setFrameRate(videoGrabber.getFrameRate());
			//recorder.setSampleRate(audioGrabber.getSampleRate());
			//recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

			recorder.start();

			// Record video frames
			while (true) {
				AVPacket videoPacket = videoGrabber.grabPacket();
				if (videoPacket == null || videoPacket.size() <= 0 || videoPacket.data() == null) {
					break;
				}
				recorder.recordPacket(videoPacket);
				avcodec.av_packet_unref(videoPacket);
			}

			// Record audio frames
			while (true) {
				AVPacket audioPacket = audioGrabber.grabPacket();
				if (audioPacket == null || audioPacket.size() <= 0 || audioPacket.data() == null) {
					break;
				}
				recorder.recordPacket(audioPacket);
				avcodec.av_packet_unref(audioPacket);
			}

			recorder.stop();
			videoGrabber.stop();
			audioGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (recorder != null) {
					recorder.release();
				}
				if (videoGrabber != null) {
					videoGrabber.release();
				}
				if (audioGrabber != null) {
					audioGrabber.release();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void addAudio3() {

		// OpenCV video handling
		VideoCapture videoCapture = new VideoCapture(videoFilename);
		if (!videoCapture.isOpened()) {
			System.err.println("Error opening video file");
			return;
		}

		// Get video properties
		int frameWidth = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_WIDTH);
		int frameHeight = (int) videoCapture.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		double fps = videoCapture.get(Videoio.CAP_PROP_FPS);
		int fourcc = VideoWriter.fourcc('M', 'J', 'P', 'G');

		// OpenCV to read frames and JavaCV to write frames with audio
		OpenCVFrameGrabber frameGrabber = new OpenCVFrameGrabber(videoFilename);
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFilename, frameWidth, frameHeight, 2); // 2 audio channels
		FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioFilename);

		try {
			// Initialize grabbers and recorder
			frameGrabber.start();
			audioGrabber.start();
			recorder.setFrameRate(fps);
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
			recorder.setFormat("mp4");
			recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			recorder.setSampleRate(audioGrabber.getSampleRate());
			recorder.setAudioChannels(audioGrabber.getAudioChannels());
			recorder.start();

			// Read video frames and record them
			while (true) {
				org.bytedeco.javacv.Frame frame = frameGrabber.grab();
				if (frame == null) {
					break;
				}
				recorder.record(frame);
			}

			// Read audio frames and record them
			while (true) {
				AVPacket audioPacket = audioGrabber.grabPacket();
				if (audioPacket == null || audioPacket.size() <= 0 || audioPacket.data() == null) {
					break;
				}
				recorder.recordPacket(audioPacket);
				avcodec.av_packet_unref(audioPacket);
			}

			recorder.stop();
			frameGrabber.stop();
			audioGrabber.stop();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (recorder != null) {
					recorder.release();
				}
				if (frameGrabber != null) {
					frameGrabber.release();
				}
				if (audioGrabber != null) {
					audioGrabber.release();
				}
				videoCapture.release();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}




}
