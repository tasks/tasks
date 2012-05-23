package com.todoroo.aacenc;

import java.io.IOException;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;

public class AACRecorder implements AudioRecord.OnRecordPositionUpdateListener {

	private AudioRecord audioRecord;
	private AACEncoder encoder;
	private Context context;
	private String tempFile;
	
	private boolean recording;
	
	private static final int SAMPLE_RATE = 16000;
	private static final int NOTIFICATION_PERIOD = 160;
	private static final int MIN_BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, 
			AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT) * 3;

	private byte[] buffer = new byte[NOTIFICATION_PERIOD * 2];
	
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
		audioRecord.setRecordPositionUpdateListener(this);
		
		encoder.init(64000, 1, SAMPLE_RATE, 16, tempFile);
		
		recording = true;
		audioRecord.startRecording();
	}
	
	public synchronized void stopRecording() {
		if (!recording)
			return;
		
		audioRecord.stop();
		audioRecord.release();
		recording = false;
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
	
	@Override
	public void onMarkerReached(AudioRecord recorder) {
		//
	}

	@Override
	public void onPeriodicNotification(AudioRecord recorder) {
		int bytesRead = recorder.read(buffer, 0, buffer.length);
		if (bytesRead < 0) {
			System.err.println("ERROR " + bytesRead);
			stopRecording();
			return;
		}

		encoder.encode(buffer);
	}
	
}
