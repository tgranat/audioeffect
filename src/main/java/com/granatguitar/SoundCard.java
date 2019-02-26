package com.granatguitar;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
A SoundCard should be for example TASCAM US-122 MKII or C-Media USB Headphone Set 

It should have an AudioInputStream fromGuitar and one toAmp ??? Or fromInput and toOutput

A "module" (service) of some kind (volume control, fx, ...) gets input from fromInput, manipulates it, and write to toOutput ???


*/
public class SoundCard {
    private Mixer mixerFromGuitar;
    private TargetDataLine lineFromGuitar;
    private Mixer mixerToAmp;
    private SourceDataLine lineToAmp;
    private AudioFormat audioFormat;
    
    //private static final String EXTERNAL_SOUNDCARD = "TASCAM US-122 MKII";
    private static final String EXTERNAL_SOUNDCARD = "C-Media USB Headph";
//    private static final String EXTERNAL_SOUNDCARD = "C-Media USB Headphone";
    private static final Info EXTERNAL_SOUNDCARD_INPUT = Port.Info.LINE_IN;
    private static final Info EXTERNAL_SOUNDCARD_OUTPUT = Port.Info.SPEAKER;
    
    private static final Logger logger = LoggerFactory.getLogger(SoundCard.class.getName());
    
    public SoundCard() throws LineUnavailableException {
    	logger.info("Initiate soundcard");
        initAudioFormat();
        initMixers();
        }

    public Mixer getMixerFromGuitar() {
        return mixerFromGuitar;
    }

    public Mixer getMixerToAmp() {
        return mixerToAmp;
    }

    public TargetDataLine getLineFromGuitar() {
        return lineFromGuitar;
    }

    public SourceDataLine getLineToAmp() {
        return lineToAmp;
    }
    
    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    private void initAudioFormat() {
      //AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
      //float rate = 44100.0f;
      float rate = 8000.0f;
      int sampleSize = 8;
      //int sampleSize = 16;
      int channels = 1;
      boolean bigEndian = true;

      audioFormat = new AudioFormat(rate, sampleSize, channels, bigEndian, bigEndian);
     // audioFormat = new AudioFormat(encoding, rate, sampleSize, channels, (sampleSize / 8)
     //     * channels, rate, bigEndian);
      
    }
    
    private void initMixers() throws LineUnavailableException  {
        // Assuming that a found mixer with correct name AND that has "myMixer.isLineSupported(Port.Info.MICROPHONE))" (or Port.Info.LINE_IN)
        // can be the "mixerFromGuitar"
        // "myMixer.isLineSupported(Port.Info.SPEAKER))is "mixerToAmp"
    	//
    	// Note: target = where from audio data can be read
    	//       source = to which audio data may be writter
        
        mixerFromGuitar = null;
        mixerToAmp = null;
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
             if (mixerInfo.getName().contains(EXTERNAL_SOUNDCARD)) {
                if (mixerFromGuitar == null) {
                    Mixer mixerToCheck = AudioSystem.getMixer(mixerInfo);
                    logger.debug("Checking format for input from guitar: " + mixerToCheck.getMixerInfo().getName());
                    DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                    if (mixerToCheck.isLineSupported(targetInfo)) {
                        logger.info("Found line for input: " + mixerToCheck.getMixerInfo().getName());
                        mixerFromGuitar = mixerToCheck;
                        lineFromGuitar = (TargetDataLine) mixerFromGuitar.getLine(targetInfo);
                    }
                    /*
                    for (Line.Info info : mixerToCheck.getTargetLineInfo()) {
                        if (TargetDataLine.class.isAssignableFrom(info.getLineClass())) {
                            TargetDataLine.Info info2 = (TargetDataLine.Info) info;
                            System.out.println(info2);
                            System.out.printf("  max buffer size: \t%d\n", info2.getMaxBufferSize());
                            System.out.printf("  min buffer size: \t%d\n", info2.getMinBufferSize());
                            AudioFormat[] formats = info2.getFormats();
                            System.out.println("  Supported Audio formats: ");
                            for (AudioFormat format : formats) {
                                System.out.println("    " + format);
                            }
                            System.out.println();
                        }
                    }
                    */

                }
                if (mixerToAmp == null) {
                    Mixer mixerToCheck = AudioSystem.getMixer(mixerInfo);
                    logger.debug("Checking format for output to amp: " + mixerToCheck.getMixerInfo().getName());
                    DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, audioFormat);
                    if (mixerToCheck.isLineSupported(sourceInfo)) {
                        logger.info("Found line for output: " + mixerToCheck.getMixerInfo().getName());
                        mixerToAmp = mixerToCheck;
                        lineToAmp = (SourceDataLine) mixerToAmp.getLine(sourceInfo);
                    }
                    /*
                    for (Line.Info info : mixerToCheck.getSourceLineInfo()) {
                        if (SourceDataLine.class.isAssignableFrom(info.getLineClass())) {
                            SourceDataLine.Info info2 = (SourceDataLine.Info) info;
                            System.out.println(info2);
                            System.out.printf("  max buffer size: \t%d\n", info2.getMaxBufferSize());
                            System.out.printf("  min buffer size: \t%d\n", info2.getMinBufferSize());
                            AudioFormat[] formats = info2.getFormats();
                            System.out.println("  Supported Audio formats: ");
                            for (AudioFormat format : formats) {
                                System.out.println("    " + format);
                            }
                            System.out.println();

                        }
                    }
                    */
                }
            }
        }
    }

}

