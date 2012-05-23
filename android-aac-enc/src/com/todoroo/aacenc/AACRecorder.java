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
	
	private static final int SAMPLE_RATE = 8000;
	private static final int NOTIFICATION_PERIOD = 160;
	private static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10;

	private byte[] buffer = new byte[NOTIFICATION_PERIOD * 2];
	
	private Thread readerThread = new Thread() {
		private byte[] readBuffer = new byte[NOTIFICATION_PERIOD * 2];
		public void run() {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			while(recording) {
				int bytesRead = audioRecord.read(readBuffer, 0, readBuffer.length);
				System.err.println("Bytes read: " + bytesRead);
				try {
					baos.write(readBuffer);
				} catch (IOException e) {
					//
				}
			}
			encoder.encode(baos.toByteArray());
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
		readerThread.start();
		
		audioRecord.startRecording();
	}
	
	public synchronized void stopRecording() {
		if (!recording)
			return;
		
		recording = false;
		audioRecord.stop();
		audioRecord.release();
		System.err.println("Uninit");
		encoder.uninit();
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
