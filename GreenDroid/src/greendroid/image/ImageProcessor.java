/*
 * Copyright (C) 2010 Cyril Mottier (http://www.cyrilmottier.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greendroid.image;

import android.graphics.Bitmap;

/**
 * An interface specifying a way to process an image prior storing it in the
 * application-wide cache. A great way to use this interface is to prepare a
 * Bitmap (resizing, adding rounded corners, changing the tint color, etc.) for
 * faster drawing.
 * 
 * @author Cyril Mottier
 */
public interface ImageProcessor {

	/**
	 * Called whenever the bitmap need to be processed. The returned may have
	 * been modified or completely different.
	 * 
	 * @param bitmap
	 *            The Bitmap to process
	 * @return A Bitmap that has been modified
	 */
	Bitmap processImage(Bitmap bitmap);

}
