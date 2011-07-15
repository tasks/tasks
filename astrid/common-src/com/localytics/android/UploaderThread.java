/**
 * UploaderThread.java
 * Copyright (C) 2009 Char Software Inc., DBA Localytics
 *
 *  This code is provided under the Localytics Modified BSD License.
 *  A copy of this license has been distributed in a file called LICENSE
 *  with this source code.
 *
 *  Please visit www.localytics.com for more information.
 */

package com.localytics.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

/**
 * The thread which handles uploading Localytics data.
 * @author Localytics
 */
@SuppressWarnings("nls")
public class UploaderThread extends Thread
{
	private final Runnable _completeCallback;
	private final File     _localyticsDir;
	private final String   _sessionFilePrefix;
	private final String   _uploaderFilePrefix;
	private final String   _closeFilePrefix;

	// The Tag used in logging.
    private final static String LOG_TAG = "Localytics_uploader";

	// The URL to send Localytics session data to
	private final static String ANALYTICS_URL = "http://analytics.localytics.com/api/datapoints/bulk";

	// The size of the buffer used for reading in files.
	private final static int BUFFER_SIZE = 1024;

	/**
	 * Creates a thread which uploads the session files in the passed Localytics
	 * Directory.  All files starting with sessionFilePrefix are renamed,
	 * uploaded and deleted on upload.  This way the sessions can continue
	 * writing data regardless of whether or not the upload succeeds.  Files
	 * which have been renamed still count towards the total number of Localytics
	 * files which can be stored on the disk.
	 * @param appContext The context used to access the disk
	 * @param completeCallback A runnable which is called notifying the caller that upload is complete.
	 * @param localyticsDir The directory containing the session files
	 * @param sessionFilePrefix The filename prefix identifying the session files.
	 * @param uploaderfilePrefix The filename prefixed identifying files to be uploaded.
	 */
	public UploaderThread(
			File localyticsDir,
			String sessionFilePrefix,
			String uploaderFilePrefix,
			String closeFilePrefix,
			Runnable completeCallback)
	{
		this._localyticsDir = localyticsDir;
		this._sessionFilePrefix = sessionFilePrefix;
		this._uploaderFilePrefix = uploaderFilePrefix;
		this._closeFilePrefix = closeFilePrefix;
		this._completeCallback = completeCallback;
	}

	/**
	 * Renames all the session files (so that other threads can keep writing
	 * datapoints without affecting the upload.  And then uploads them.
	 */
	@Override
    public void run()
	{
		int numFilesToUpload = 0;

		try
		{
			if(this._localyticsDir != null && this._localyticsDir.exists())
			{
				String basePath = this._localyticsDir.getAbsolutePath();

				// rename all the files, taking care to rename the session files
				// before the close files.
				renameOrAppendSessionFiles(basePath);
				renameOrAppendCloseFiles(basePath);

				// Grab all the files to be uploaded
				FilenameFilter filter = new FilenameFilter()
				{
					public boolean accept(File dir, String name)
					{
						return name.startsWith(_uploaderFilePrefix);
					}
				};

				String uploaderFiles[] = this._localyticsDir.list(filter);
				numFilesToUpload = uploaderFiles.length;
				String postBody = createPostBodyFromFiles(basePath, uploaderFiles);

				// Attempt to upload this data.  If successful, delete all the uploaderFiles.
				Log.v(UploaderThread.LOG_TAG, "Attempting to upload " + numFilesToUpload + " files.");
				if(uploadSessions(postBody.toString()) == true)
				{
					int currentFile;
					File uploadedFile;
					for(currentFile = 0; currentFile < uploaderFiles.length; currentFile++)
					{
						uploadedFile = new File(basePath + "/" + uploaderFiles[currentFile]);
						uploadedFile.delete();
					}
				}
			}

			// Notify the caller the upload is complete.
			if(this._completeCallback != null)
			{
				this._completeCallback.run();
			}
		}
		catch (Exception e)
		{
			Log.v(UploaderThread.LOG_TAG, "Swallowing exception: " + e.getMessage());
		}
	}

	/**
	 * Looks at every file whose name starts with the session file prefix
	 * and renamed or appends it to the appropriately named uploader file.
	 * @param basePath The full path to the directory containing the files to upload
	 */
	private void renameOrAppendSessionFiles(String basePath)
	{
		int currentFile;

		// Create a filter to only grab the session files.
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.startsWith(_sessionFilePrefix);
			}
		};

		// Go through each of the session files
		String[] originalFiles = this._localyticsDir.list(filter);
		for(currentFile = 0; currentFile < originalFiles.length; currentFile++)
		{
			String originalFileName = basePath + "/" + originalFiles[currentFile];
			String targetFileName = basePath + "/" + this._uploaderFilePrefix + originalFiles[currentFile];
			renameOrAppendFile(new File(originalFileName), new File(targetFileName));
		}
	}

	/**
	 * Looks at every close file in the directory and renames or appends it to
	 * the appropriate uploader file.  This is done separately from the session
	 * files because it makes life simpler on the webservice if the close events
	 * come after the session events
	 * @param basePath The full path to the directory containing the files to upload
	 */
	private void renameOrAppendCloseFiles(String basePath)
	{
		int currentFile;

		// Create a filter to only grab the session files.
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.startsWith(_closeFilePrefix);
			}
		};

		// Go through each of the session files
		String[] originalFiles = this._localyticsDir.list(filter);
		for(currentFile = 0; currentFile < originalFiles.length; currentFile++)
		{
			String originalFileName = basePath + "/" + originalFiles[currentFile];

			// In order for the close events to be appended to the appropriate files
			// remove the close prefix and prepend the session prefix
			String targetFileName = basePath + "/"
									   + this._uploaderFilePrefix
									   + getSessionFilenameFromCloseFile(originalFiles[currentFile]);
			renameOrAppendFile(new File(originalFileName), new File(targetFileName));
		}
	}

	/**
	 * Determines what the name of the session file matching this close file would be
	 * @param closeFilename Name of close file to be used as a guide
	 * @return The filename of the session which matches this close file
	 */
	private String getSessionFilenameFromCloseFile(String closeFilename)
	{
		return this._sessionFilePrefix + closeFilename.substring(this._closeFilePrefix.length());
	}

	/**
	 * Checks if destination file exists.  If so, it appends the contents of
	 * source to destination and deletes source.  Otherwise, it rename source
	 * to destination.
	 * @param source File containing the data to be moved
	 * @param destination Target for the data
	 */
	private static void renameOrAppendFile(File source, File destination)
	{
		if(destination.exists())
		{
			try
			{
				InputStream in = new FileInputStream(source);
				OutputStream out = new FileOutputStream(destination, true);

				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0)
				{
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
				source.delete();
			}
			catch (FileNotFoundException e)
			{
				Log.v(LOG_TAG, "File not found.");
			}
			catch (IOException e)
			{
				Log.v(LOG_TAG, "IO Exception: " + e.getMessage());
			}
		}
		else
		{
			source.renameTo(destination);
		}
	}

	/**
	 * Reads in the input files and cats them together in one big string which makes up the
	 * HTTP request body.
	 * @param basePath The directory to get the files from
	 * @param uploaderFiles the list of files to read
	 * @return A string containing a YML blob which can be uploaded to the webservice.
	 */
	private String createPostBodyFromFiles(final String basePath, final String[] uploaderFiles)
	{
		int currentFile;
		File inputFile;
		StringBuffer postBody = new StringBuffer();

		// Read each file in to one buffer.  This allows the upload to happen as one
		// large transfer instead of many smaller transfers which is preferable on
		// a mobile device in which the time required to make a connection is often
		// disproportionately large compared to the time to upload the data.
		for(currentFile = 0; currentFile < uploaderFiles.length; currentFile++)
		{
			inputFile = new File(basePath + "/" + uploaderFiles[currentFile]);

			try
			{
                BufferedReader reader = new BufferedReader(
                                            new InputStreamReader(
                                                new FileInputStream(inputFile),
                                                "UTF-8"),
                                            UploaderThread.BUFFER_SIZE);
				char[] buf = new char[1024];
				int numRead;
				while( (numRead = reader.read(buf)) > 0)
				{
					postBody.append(buf, 0, numRead);
				}
				reader.close();
			}
			catch (FileNotFoundException e)
			{
				Log.v(LOG_TAG, "File Not Found");
			}
			catch (IOException e)
			{
				Log.v(LOG_TAG, "IOException: " + e.getMessage());
			}
            catch (OutOfMemoryError e)
            {
                e.printStackTrace();
                Log.v(LOG_TAG, "OutOfMemoryError: " + e.getMessage());
            }
		}

		return postBody.toString();
	}

	/**
	 * Uploads the post Body to the webservice
	 * @param ymlBlob String containing the YML to upload
	 * @return True on success, false on failure.
	 */
	private boolean uploadSessions(String ymlBlob)
	{
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost method = new HttpPost(UploaderThread.ANALYTICS_URL);

		try
		{
			StringEntity postBody = new StringEntity(ymlBlob, "utf8");
			method.setEntity(postBody);
			HttpResponse response = client.execute(method);

			StatusLine status = response.getStatusLine();
			Log.v(UploaderThread.LOG_TAG, "Upload complete. Status: " + status.getStatusCode());

			// On any response from the webservice, return true so the local files get
			// deleted.  This avoid an infinite loop in which a bad file keeps getting
			// submitted to the webservice time and again.
			return true;
		}

		// return true for any transportation errors.
		catch (UnsupportedEncodingException e)
		{
			Log.v(LOG_TAG, "UnsuppEncodingException: " + e.getMessage());
			return false;
		}
		catch (ClientProtocolException e)
		{
			Log.v(LOG_TAG, "ClientProtocolException: " + e.getMessage());
			return false;
		}
		catch (IOException e)
		{
			Log.v(LOG_TAG, "IOException: " + e.getMessage());
			return false;
		}
	}
}
