package com.todoroo.aacenc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class AACRecorder {

	private AudioRecord audioRecord;
	private AACEncoder encoder;
	private Context context;
	private String tempFile;
	
	private boolean recording;
	private AACRecorderCallbacks listener;
	
	private static final int SAMPLE_RATE = 8000;
	private static final int NOTIFICATION_PERIOD = 160;
	private static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;
	
	public interface AACRecorderCallbacks {
		public void encodingFinished();
	}
	
	private Thread readerThread = new Thread() {
		private byte[] readBuffer = new byte[NOTIFICATION_PERIOD * 2];
		public void run() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int bytesRead = 0;
			while(recording) {
				bytesRead = audioRecord.read(readBuffer, 0, readBuffer.length);
				System.err.println("Bytes read: " + bytesRead);
				try {
					baos.write(readBuffer);
				} catch (IOException e) {
					//
				}
				if (bytesRead <= 0)
					break;
			}
			encoder.encode(baos.toByteArray());
			finishRecording();
			baos.reset();
		}
	};
	
	
	public AACRecorder(Context context) {
		encoder = new AACEncoder();
	}
	
	public synchronized void startRecording(String tempFile) {
		if (recording)
			return;
		
		this.tempFile = tempFile;
		audioRecord = new AudioRecord(AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, MIN_BUFFER_SIZE);
		
		
		audioRecord.setPositionNotificationPeriod(NOTIFICATION_PERIOD);
		
		encoder.init(64000, 1, SAMPLE_RATE, 16, tempFile);
		
		recording = true;
		audioRecord.startRecording();

		readerThread.start();
	}
	
	public synchronized void stopRecording() {
		if (!recording)
			return;

		audioRecord.stop();
	}
	
	public synchronized void finishRecording() {
		recording = false;
		audioRecord.release();
		System.err.println("Uninit");
		encoder.uninit();
		if (listener != null)
			listener.encodingFinished();
	}
	
	public void setListener(AACRecorderCallbacks listener) {
		this.listener = listener;
	}
	
	public synchronized boolean convert(String outFile) {
		if (recording || tempFile == null)
			return false;
		
		try {
			new AACToM4A().convert(context, tempFile, outFile);
			tempFile = null;
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
}
