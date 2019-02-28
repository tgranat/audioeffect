package com.granatguitar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Control;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.core.joran.spi.JoranException;


public class GuitarEffect {

    private static final Logger logger = LoggerFactory.getLogger(GuitarEffect.class.getName());

    private SoundCard soundCard;
    private ByteArrayOutputStream out;
    private PipedInputStream pipedIn;
    private PipedOutputStream pipedOut;
    private boolean captureRunning;
    
    public GuitarEffect(SoundCard soundCard) {
        logger.info("Create GuitarEffect");
        this.soundCard = soundCard;
    }

    public void start() {
        logger.debug("Start GuitarEffect");
        try {
            captureAudio();
        } catch (IOException ex) {
        	logger.error(null, ex);
        }
    }

    private void captureAudio() throws IOException {
        try {
            AudioFormat format = soundCard.getAudioFormat();
            TargetDataLine targetDataLine = soundCard.getLineFromGuitar();
            SourceDataLine sourceDataLine = soundCard.getLineToAmp();

 
            logger.debug("Open line from input");
            targetDataLine.open(format);
            targetDataLine.start();
            
            sourceDataLine.open(format);
            sourceDataLine.start();

    		Control[] inputControls = targetDataLine.getControls();
    		logger.debug("Input controls");
    		for (Control control : inputControls) {
    			logger.debug("   " +control.getType().toString());
    		}
    		
    		Control[] outputControls = sourceDataLine.getControls();
    		logger.debug("Output controls");
    		for (Control control : outputControls) {
    			logger.debug("   " + control.getType().toString());
    		}
    		FloatControl fc = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
    		BooleanControl bc = (BooleanControl) sourceDataLine.getControl(BooleanControl.Type.MUTE);
    	
    		logger.debug(fc.toString());
    		logger.debug(bc.toString());


            out = new ByteArrayOutputStream();
            pipedOut = new PipedOutputStream();
            pipedIn = new PipedInputStream(pipedOut);

            Runnable captureRunner = new Runnable() {
                int bufferSize = ((int) format.getSampleRate() * format.getFrameSize()) ;
                byte[] bufferCapture = new byte[bufferSize];

                public void run() {
                    logger.debug("Start thread capturing input");
                	logger.debug("buffer size = " + bufferSize);
                    captureRunning = true;
                    try {
                        while (captureRunning) {
                            // det här funkar, men ganska kasst. Latency osv. Men ljudet kommer ut ( brusigt, halv sekund efter)
                            // - Why the noise?
                        	// - improve bandwidth?
                        	// - reduce latency
                            int countCapture = targetDataLine.read(bufferCapture, 0, 1000);
                           // int countCapture = targetDataLine.read(bufferCapture, 0, bufferCapture.length);
                            if (countCapture > 0) {
                                //logger.debug("capturing data, bytes written to buffer: " + countCapture);

                                //
                                sourceDataLine.write(bufferCapture, 0, countCapture);
                                
                                //pipedOut.write(bufferCapture, 0, countCapture);
                                //out.flush();
                            }

                        }
                        pipedOut.close();
                        targetDataLine.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-1);
                    }
                }
            };
            
            Thread captureThread = new Thread(captureRunner);
            captureThread.start();

            // wait a while let capture begin to avoid NPE
            try {
            	Thread.sleep(4000);
            } catch (InterruptedException ex) {
                logger.error(null, ex);
            }

            /*
            // change this. this reads a limited length. how do we read forever? change to piped
            byte audio[] = out.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            
            // can't use AudioInputStream since it has a specified length!
            AudioInputStream ais
            //        = new AudioInputStream(pipedIn, format, audio.length / format.getFrameSize()); // this only reads a limited number of bytes. How should we do???
              = new AudioInputStream(targetDataLine);  // read directly from tarteg line??? is this possible?
            // end change this
            //System.out.println(ais.getFrameLength());
            logger.log(Level.FINE, "Open line to output");
            sourceDataLine.open(format);
            sourceDataLine.start();

            Runnable playRunner = new Runnable() {
                int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                
                public void run() {
                    try {
                        boolean playRunning = true;
                        while (playRunning) {
                            int count;
                            count = ais.read(buffer);
                            //count = ais.read(buffer, 0, buffer.length);
                            System.out.println("count = " + count);
                            while (count != -1) {

                                if (count > 0) {
                                    System.out.println("write to buffer: " + count);

                                    sourceDataLine.write(buffer, 0, count);
                                }
                                count = ais.read(buffer);
                               //  count = ais.read(buffer, 0, buffer.length);
                                System.out.println("bytes read from AudioInputStream: " + count);
                            }
                        }
                        sourceDataLine.drain();
                        sourceDataLine.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems: " + e);
                        System.exit(-3);
                    }
                }
            };

 
            //Thread playThread = new Thread(playRunner);
            //logger.log(Level.FINE, "Start thread play output");
            //playThread.start();
            */
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-2);
        }
    }

    
    public void listMixers() {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            System.out.println("Mixer: " + mixerInfo.getName());
 
            //System.out.println("  " +  mixer.getDescription());
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] sourceLines = mixer.getSourceLineInfo();
            for (Line.Info sourceLineInfo : sourceLines) {
                int maxLines = mixer.getMaxLines(sourceLineInfo);
                if (maxLines != AudioSystem.NOT_SPECIFIED) {
                    System.out.println("  Source lines: " + maxLines);
                }
            }
            Line.Info[] targetLines = mixer.getTargetLineInfo();
            for (Line.Info targetLineInfo : targetLines) {
                int maxLines = mixer.getMaxLines(targetLineInfo);
                if (maxLines != AudioSystem.NOT_SPECIFIED) {
                    System.out.println("  Target lines: " + maxLines);
                }
            }
        }
    }

    /**
     * @param args the command line arguments
     * @throws JoranException 
     * @throws IOException 
     * @throws LineUnavailableException 
     */
    public static void main(String[] args) throws JoranException, IOException, LineUnavailableException {
    	
    	/*
        Handler systemOutHandler = new StreamHandler(System.out, new SimpleFormatter());
        systemOutHandler.setLevel(Level.FINEST);
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(systemOutHandler);
        rootLogger.setLevel(Level.FINEST);
        */

        SoundCard sc = null;
        try {
            sc = new SoundCard();
        } catch (LineUnavailableException ex) {
            logger.error(null, ex);
        }
        
        GuitarEffect ge = new GuitarEffect(sc);
        //ge.listMixers();
        SoundCard.printMixers();

        ge.start();
        logger.debug("thread started");;
       
        
    }

}
