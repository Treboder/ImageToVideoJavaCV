package com.example.ImageToVideo;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

@SpringBootApplication
public class ImageToVideoApplication {

	private static Logger logger = LoggerFactory.getLogger(ImageToVideoApplication.class);

	// ToDo: create images in memory (save optionally)

	private static String imageFileDir = "data/images";

	private static String videoFileMutedDir = "data/video/muted";
	private static String videoFileSoundDir = "data/video/sound";

	private static String audioFileOriginalDir = "data/audio/original";
	private static String audioFileWithoutCoverDir = "data/audio/woCover";

	private static String audioFileNamePostfixWithoutCover = "-woc";

	private static String videoFormat = "mp4";

	private static int width = 640;  // Set to your image width
	private static int height = 480; // Set to your image height
	private static int fps = 2;

	private static int videoLengthSeconds;
	private static int numberOfImages;

	public static void main(String[] args) {
		SpringApplication.run(ImageToVideoApplication.class, args);
		logger.info("Launch ImageToVideo");

		// create copies of mp3 without cover image
		cleanUpDirectory(audioFileWithoutCoverDir);
		ArrayList<String> originalAudioFiles = getListOfOriginalAudioFiles();
		for(String audioFile: originalAudioFiles)
			createMP3withoutCoverImage(audioFile);

		// delete video files
		cleanUpDirectory(videoFileMutedDir);
		cleanUpDirectory(videoFileSoundDir);

		// loop through mp3s and create videos for each
		ArrayList<String> audioFilesWithoutCover = getListOfAudioFilesWithoutCover();
		for(int i=0; i< audioFilesWithoutCover.size(); i++) {
			String audioFileName = audioFilesWithoutCover.get(i);
			cleanUpDirectory(imageFileDir);
			createImageSequence(i, audioFileName);
			createVideoWithoutSound(i);
			createVideoWithSound(i, audioFileName);
		}
	}

	private static void cleanUpDirectory(String folderPath) {

		logger.info("Delete all files in {}", folderPath);

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

	private static void createMP3withoutCoverImage(String audioOriginalFileName) {

		logger.info("Remove mp3 cover image from ({})", audioOriginalFileName);
		try {
			String audioFilePathOriginal = audioFileOriginalDir + "/" + audioOriginalFileName;
			String audioFilePathWithoutCover = audioFileWithoutCoverDir + "/" + audioOriginalFileName.substring(0, audioOriginalFileName.length()-4) + audioFileNamePostfixWithoutCover +".mp3";
			Mp3File mp3File = new Mp3File(audioFilePathOriginal);

			if (mp3File.hasId3v2Tag()) {
				ID3v2 id3v2Tag = mp3File.getId3v2Tag();
				// Remove the cover image
				id3v2Tag.clearAlbumImage();

				// Save the modified MP3 file
				mp3File.save(audioFilePathWithoutCover);
			} else {
				logger.warn("The MP3 file does not have an ID3v2 tag ({})", audioOriginalFileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to remove ID3v2 tag from MP3 ({})", audioOriginalFileName);
		}
	}

	private static void createImageSequence(int index, String audioFileWithoutCover) {

		// determine number of images
		videoLengthSeconds = getMP3Duration(audioFileWithoutCover);
		numberOfImages = fps * videoLengthSeconds;
		logger.info("Create {} images based on {} ({})", numberOfImages, audioFileWithoutCover, index);

		// create images
		int x = 0;
		for(int i=0; i<numberOfImages; i++) {
			// Create a buffered image
			BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			// Get the graphics object to draw on the image
			Graphics2D g2d = bufferedImage.createGraphics();

			// Fill the background with white
			g2d.setColor(Color.WHITE);
			g2d.fillRect(0, 0, width, height);

			// determine next position
			x = x + 2;
			if(x>width) x = 0;

			// Draw a rectangle
			g2d.setColor(Color.RED);
			g2d.fillRect(x, 50, 100, 100);

			// Draw some text
			g2d.setColor(Color.BLACK);
			g2d.setFont(new Font("Arial", Font.BOLD, 50));
			g2d.drawString("Hello "+i, 150, 400);

			// Dispose of the graphics context to release resources
			g2d.dispose();

			// Save the image to disk
			File file = new File(imageFileDir + "/image" + i +".jpg");
			try {
				ImageIO.write(bufferedImage, "jpg", file);
				logger.debug("Save image: " + file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static void createVideoWithoutSound(int index) {
		logger.info("Create video without sound ({})", index);
		try {
			// Initialize the FFmpegFrameRecorder
			String fileNamePath = videoFileMutedDir + "/" + index + "." + videoFormat;
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(fileNamePath, width, height);
			recorder.setFormat(videoFormat);
			recorder.setFrameRate(fps);
			//recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			//recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
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
			logger.info("Save video: " + fileNamePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void createVideoWithSound(int index, String audioWithoutCoverFileName) {
		logger.info("Create video with sound from {} ({})", audioWithoutCoverFileName, index);
		try {
			// Create the recorder
			String fileNamePath = videoFileSoundDir + "/" + index + "." + videoFormat;
			FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(fileNamePath, width, height, 2);
			recorder.setFormat(videoFormat);
			recorder.setFrameRate(fps);
			// recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			// recorder.setVideoQuality(0); // Maximum quality
			// recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			// recorder.setAudioBitrate(192000);
			// recorder.setSampleRate(44100);


			// create audio grabber
			String audioFilePathWithoutCover = audioFileWithoutCoverDir + "/" + audioWithoutCoverFileName.substring(0, audioWithoutCoverFileName.length()-4) +".mp3";
			FrameGrabber grabber = new FFmpegFrameGrabber(audioFilePathWithoutCover);

			grabber.start();

			// set meta data
			setMetaData(recorder, grabber, "Treboder");

			recorder.start();


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

			// stop recording and save file
			grabber.stop();
			recorder.stop();
			recorder.release();
			logger.info("Save video: {} ({})", fileNamePath, index);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void setMetaData(FFmpegFrameRecorder recorder, FrameGrabber audioGrabber, String title) {
		recorder.setMetadata("title", title);
		recorder.setMetadata("comment", "Treboder");
		recorder.setMetadata("genre", "Treboder");
		recorder.setMetadata("artist", "Treboder");
		recorder.setMetadata("composer", "Music by "+ audioGrabber.getMetadata("artist"));

//		all keys below do not work
//		recorder.setMetadata("sub title", "Treboder");
//		recorder.setMetadata("tag", "Treboder");
//		recorder.setMetadata("tags", "Treboder");
//		recorder.setMetadata("contributing artist", "Treboder");
//		recorder.setMetadata("year", "2024");
//		recorder.setMetadata("time", "2024");
//		recorder.setMetadata("origin", "2024");
//		recorder.setMetadata("owner", "2024");
//		recorder.setMetadata("author", "2024");
//		recorder.setMetadata("director", "Treboder");
//		recorder.setMetadata("producer", "Treboder");
//		recorder.setMetadata("writer", "Treboder");
//		recorder.setMetadata("publisher", "Treboder");
//		recorder.setMetadata("content provider", "Treboder");
//		recorder.setMetadata("media created", "Treboder");
//		recorder.setMetadata("encoded by", "Treboder");
//		recorder.setMetadata("author URL", "Treboder");
//		recorder.setMetadata("author url", "Treboder");
//		recorder.setMetadata("author", "Treboder");
//		recorder.setMetadata("url", "Treboder");
//		recorder.setMetadata("promotion URL", "Treboder");
//		recorder.setMetadata("copyright", "Treboder");
	}

	private static int getMP3Duration(String audioFileWithoutCover) {
		try {
			// create audio grabber
			String audioFilePathOriginal = audioFileWithoutCoverDir + "/" + audioFileWithoutCover;
			FrameGrabber grabber = new FFmpegFrameGrabber(audioFilePathOriginal);
			grabber.start();

			// get track duration in seconds
			int mp3DurationSeconds = (int) grabber.getLengthInTime() / 1000000;
			logger.info("Return {} s track length for {}", mp3DurationSeconds, audioFileWithoutCover);
			return mp3DurationSeconds;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to get mp3 duration");
			return 0;
		}
	}

	private static ArrayList<String> getListOfOriginalAudioFiles() {
		ArrayList<String> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(audioFileOriginalDir))) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					files.add(path.getFileName().toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Found {} audio files without cover", files.size());
		return files;
	}

	private static ArrayList<String> getListOfAudioFilesWithoutCover() {
		ArrayList<String> files = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(audioFileWithoutCoverDir))) {
			for (Path path : stream) {
				if (!Files.isDirectory(path)) {
					files.add(path.getFileName().toString());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Found {} audio files without cover", files.size());
		return files;
	}

}
