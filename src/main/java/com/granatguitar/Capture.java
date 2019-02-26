package com.granatguitar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Capture implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Capture.class.getName());
    
    TargetDataLine lineFromInput;
    SourceDataLine lineToOutput;
    
    AudioInputStream audioInputStream;
    double duration;
    SoundCard soundCard;

    Thread thread;

    public Capture(SoundCard soundCard) {
        this.soundCard = soundCard;
    }
    
    public void start() {
        logger.debug("Start thread to capture");
        thread = new Thread(this);
        thread.setName("Capture");
        thread.start();
    }

    public void stop() {
        logger.debug("Capture thread stopped");
        thread = null;
    }

    private void shutDown(String message) {
        thread = null;
        logger.error(message);
    }

    public void run() {

        duration = 0;
        audioInputStream = null;

        // Get line
        AudioFormat format = soundCard.getAudioFormat();
        lineFromInput = soundCard.getLineFromGuitar();
        lineToOutput = soundCard.getLineToAmp();

        // Captured audio data and write to output stream
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int frameSizeInBytes = format.getFrameSize();
        int bufferLengthInFrames = lineFromInput.getBufferSize() / 8;
        int bufferLengthInBytes = bufferLengthInFrames * frameSizeInBytes;
        byte[] data = new byte[bufferLengthInBytes];
        int numBytesRead;

        logger.debug("Start TargetDataLine");
        lineFromInput.start();

        while (thread != null) {
            if ((numBytesRead = lineFromInput.read(data, 0, bufferLengthInBytes)) == -1) {
                break;
            }
            out.write(data, 0, numBytesRead);
        }

        // we reached the end of the stream.
        // stop and close the line.
        logger.debug("Stop and close TargetDataLine");

        lineFromInput.stop();
        lineFromInput.close();
        lineFromInput = null;

        // stop and close the output stream
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // load bytes into the audio input stream for playback
        byte audioBytes[] = out.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
        audioInputStream = new AudioInputStream(bais, format, audioBytes.length / frameSizeInBytes);

        long milliseconds = (long) ((audioInputStream.getFrameLength() * 1000) / format
                .getFrameRate());
        duration = milliseconds / 1000.0;

        try {
            audioInputStream.reset();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

    }
} 

