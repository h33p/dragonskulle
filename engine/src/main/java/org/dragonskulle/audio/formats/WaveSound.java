/* (C) 2021 DragonSkulle */
package org.dragonskulle.audio.formats;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import lombok.extern.java.Log;
import org.lwjgl.openal.AL11;

/**
 * This class is used to load .wav files and create OpenAL buffers with them
 *
 * @author Harry Stoltz
 *     <p>Wave files are loaded by calling the static method loadWave and passing in a File that is
 *     to be parsed. If the file is of the correct format and can be read, a new WaveSound object is
 *     created and all of the fields will be filled in. The audio bytes will be read, processed and
 *     then buffered using alBufferData.
 */
@Log
public class WaveSound extends Sound {

    private int mSampleRate;
    private int mFormat;
    private int mBits;
    private int mChannels;

    /**
     * Gets the correct value of the openAL format from the number of channels and bits of each
     * sample.
     */
    private void setALFormat() {
        switch (mBits) {
            case 16:
                if (mChannels > 1) {
                    mFormat = AL11.AL_FORMAT_STEREO16;
                } else {
                    mFormat = AL11.AL_FORMAT_MONO16;
                }
                break;
            case 8:
                if (mChannels > 1) {
                    mFormat = AL11.AL_FORMAT_STEREO8;
                } else {
                    mFormat = AL11.AL_FORMAT_MONO8;
                }
        }
    }

    /**
     * Fix up the raw audio bytes and get them into a format that OpenAL can play.
     *
     * @param rawBytes Raw audio bytes to process
     * @param eightBitAudio Whether the sample size is 8 bits
     * @param order The endianness of the audio bytes
     * @return ByteBuffer containing the fixed bytes
     */
    private static ByteBuffer processRawBytes(
            byte[] rawBytes, boolean eightBitAudio, ByteOrder order) {
        ByteBuffer dst = ByteBuffer.allocateDirect(rawBytes.length);
        dst.order(ByteOrder.nativeOrder());
        ByteBuffer src = ByteBuffer.wrap(rawBytes);
        src.order(order);

        if (eightBitAudio) {
            while (src.hasRemaining()) {
                dst.put(src.get());
            }
        } else {
            ShortBuffer srcBuffer = src.asShortBuffer();
            ShortBuffer dstBuffer = dst.asShortBuffer();

            while (srcBuffer.hasRemaining()) {
                dstBuffer.put(srcBuffer.get());
            }
        }
        dst.rewind();
        return dst;
    }

    /**
     * Parses a .wav file from a FileInputStream. This is really slow so ideally all sounds should
     * be loaded straight away instead of during gameplay
     *
     * @param file .wav File to parse
     * @return A WaveSound object if file could be parsed, null otherwise
     */
    public static WaveSound loadWave(File file) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

            WaveSound sound = new WaveSound();
            AudioFormat format = audioInputStream.getFormat();

            sound.mSampleRate = (int) format.getSampleRate();

            sound.mBits = format.getSampleSizeInBits();
            sound.mChannels = format.getChannels();
            sound.setALFormat();

            int audioLength = (int) audioInputStream.getFrameLength() * format.getFrameSize();

            byte[] audioBytes = new byte[audioLength];

            // TODO: Probably isn't the best way to do this
            int bytesRead = audioInputStream.read(audioBytes);
            if (audioLength != bytesRead) {
                log.warning("Failed to read in expected number of audio bytes");
                return null;
            }

            sound.mLength = (float) bytesRead / format.getSampleRate();

            if (sound.mBits == 16) {
                sound.mLength /= 2;
            }

            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer buffer = processRawBytes(audioBytes, sound.mBits == 8, order);

            sound.mBuffer = AL11.alGenBuffers();
            AL11.alBufferData(sound.mBuffer, sound.mFormat, buffer, sound.mSampleRate);

            return sound;
        } catch (UnsupportedAudioFileException e) {
            log.warning("Attempted to load unsupported audio file " + file.getAbsolutePath());
        } catch (FileNotFoundException e) {
            log.warning("Attempted to load file that doesn't exist: " + file.getAbsolutePath());
        } catch (IOException e) {
            log.warning("IOException when reading audio file " + file.getAbsolutePath());
        }
        return null;
    }

    /** Default constructor. */
    private WaveSound() {}

    /**
     * Parses a .wav file from a byte array. This is slow so should only be done at program start.
     *
     * @param data byte array containing the wave data
     */
    public WaveSound(byte[] data) {
        try {
            AudioInputStream audioInputStream =
                    AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));

            AudioFormat format = audioInputStream.getFormat();

            mSampleRate = (int) format.getSampleRate();

            mBits = format.getSampleSizeInBits();
            mChannels = format.getChannels();
            setALFormat();

            int audioLength = (int) audioInputStream.getFrameLength() * format.getFrameSize();

            byte[] audioBytes = new byte[audioLength];

            // TODO: Probably isn't the best way to do this
            int bytesRead = audioInputStream.read(audioBytes);
            if (audioLength != bytesRead) {
                log.warning("Failed to read in expected number of audio bytes");
            }

            mLength = (float) bytesRead / format.getSampleRate();

            if (mBits == 16) {
                mLength /= 2;
            }

            ByteOrder order = format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            ByteBuffer buffer = processRawBytes(audioBytes, mBits == 8, order);

            mBuffer = AL11.alGenBuffers();
            AL11.alBufferData(mBuffer, mFormat, buffer, mSampleRate);
        } catch (UnsupportedAudioFileException e) {
            log.warning("Attempted to load unsupported audio file");
        } catch (FileNotFoundException e) {
            log.warning("Attempted to load file that doesn't exist");
        } catch (IOException e) {
            log.warning("IOException when reading audio file");
        }
    }
}
