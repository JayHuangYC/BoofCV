/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.io.video;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.images.ImageStreamSequence;
import boofcv.io.wrapper.images.JpegByteImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * This video interface attempts to load a native reader.  If that fails, jcodec, if that fails it just
 * uses the built in video types.
 *
 * @author Peter Abeles
 */
public class DynamicVideoInterface implements VideoInterface {

	VideoInterface xuggler;
	VideoInterface jcodec;
	BoofMjpegVideo mjpeg = new BoofMjpegVideo();

	public DynamicVideoInterface() {
		try {
			xuggler = loadManager("boofcv.io.wrapper.xuggler.XugglerVideoInterface");
		} catch( RuntimeException e ) {}

		try {
			jcodec = loadManager("boofcv.io.jcodec.JCodecVideoInterface");
		} catch( RuntimeException e ) {}
	}

	@Override
	public <T extends ImageBase> SimpleImageSequence<T> load(String fileName, ImageType<T> imageType) {

		if( xuggler != null ) {
			return xuggler.load(fileName,imageType);
		} else if( jcodec != null ) {
			if( fileName.endsWith(".mp4") || fileName.endsWith(".MP4")) {
				return jcodec.load(fileName,imageType);
			}
		}
		if( fileName.endsWith("mjpeg") || fileName.endsWith("MJPEG") ) {
			try {
				VideoMjpegCodec codec = new VideoMjpegCodec();
				List<byte[]> data = codec.read(new FileInputStream(fileName));
				return new JpegByteImageSequence<T>(imageType,data,false);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		} else if( fileName.endsWith("mpng") || fileName.endsWith("MPNG")) {
			try {
				return new ImageStreamSequence<T>(fileName,true,imageType);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		throw new IllegalArgumentException("Unsupported video type!");
	}

	/**
	 * Loads the specified default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static VideoInterface loadManager( String pathToManager ) {
		try {
			Class c = Class.forName(pathToManager);
			return (VideoInterface) c.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found.  Is it included in the class path?");
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
