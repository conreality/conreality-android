/* This is free and unencumbered software released into the public domain. */

package org.conreality.sdk.android;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;

/** AudioPlayerThread */
public final class AudioPlayerThread extends Thread {
  private static final String TAG = "ConrealitySDK";
  private static final int SAMPLE_RATE = 44100; // Hz
  private static final int CHANNEL_MASK = AudioFormat.CHANNEL_OUT_MONO;
  private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

  final InputStream input;
  byte[] buffer;
  AudioTrack player;

  public AudioPlayerThread(final File path) throws FileNotFoundException {
    this(new FileInputStream(path));
  }

  public AudioPlayerThread(final InputStream input) {
    super("AudioPlayerThread");
    final int bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_MASK, AUDIO_FORMAT);
    this.input = input;
    this.buffer = new byte[bufferSize];
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // Android 6.0+
      this.player = new AudioTrack.Builder()
          .setTransferMode(AudioTrack.MODE_STREAM)
          .setBufferSizeInBytes(bufferSize)
          .setAudioAttributes(new AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
              .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
              .build())
          .setAudioFormat(new AudioFormat.Builder()
              .setSampleRate(SAMPLE_RATE)
              .setChannelMask(CHANNEL_MASK)
              .setEncoding(AUDIO_FORMAT)
              .build())
          .build();
    }
    else { // Android 5.0+
      this.player = null; // TODO: legacy support
    }
    if (Log.isLoggable(TAG, Log.DEBUG)) {
      Log.d(TAG, "AudioPlayerThread: bufferSize=" + bufferSize);
    }
  }

  @Override
  public void run() {
    Log.d(TAG, "AudioPlayerThread.start");
    Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

    try {
      this.player.play();

      int len;
      while (!this.isInterrupted() && (len = this.input.read(this.buffer)) > 0) {
        this.player.write(this.buffer, 0, len); // FIXME: check return value
      }
    }
    catch (final IOException error) {
      Log.e(TAG, "Failed to play audio.", error);
    }
    //catch (final InterruptedException error) {}
    //catch (final ClosedByInterruptException error) {}
    finally {
      Log.d(TAG, "AudioPlayerThread.stop");
      this.player.stop();
      try {
        this.input.close();
      }
      catch (final IOException error) {}
      this.player.release();
      this.player = null;
      this.buffer = null;
    }
  }
}