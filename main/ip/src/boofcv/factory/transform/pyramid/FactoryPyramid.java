/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.factory.transform.pyramid;

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.transform.pyramid.PyramidDiscreteSampleBlur;
import boofcv.alg.transform.pyramid.PyramidFloatGaussianScale;
import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.factory.filter.kernel.FactoryKernelGaussian;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.convolve.Kernel1D;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidFloat;


/**
 * Factory for creating classes related to image pyramids.
 *
 * @author Peter Abeles
 */
public class FactoryPyramid {

	/**
	 * Creates an updater for discrete pyramids where a Gaussian is convolved across the input
	 * prior to sub-sampling.
	 *
	 * @param imageType Type of input image.
	 * @param sigma Gaussian sigma.  If < 0 then a sigma is selected using the radius.
	 * @param radius Radius of the Gaussian kernel.  If < 0 then the radius is selected using sigma.
	 * @return PyramidUpdaterDiscrete
	 */
	public static <T extends ImageSingleBand>
	PyramidDiscrete<T> discreteGaussian( int[] scaleFactors , double sigma , int radius ,
										 boolean saveOriginalReference, Class<T> imageType )
	{
		Class<Kernel1D> kernelType = FactoryKernel.getKernelType(imageType,1);

		Kernel1D kernel = FactoryKernelGaussian.gaussian(kernelType,sigma,radius);

		return new PyramidDiscreteSampleBlur<T>(kernel,sigma,imageType,saveOriginalReference,scaleFactors);
	}

	/**
	 * Creates an updater for float pyramids where each layer is blurred using a Gaussian with the specified
	 * sigma.  Bilinear interpolation is used when sub-sampling.
	 *
	 * @param imageType Type of image in the pyramid.
	 * @param sigmas Gaussian blur magnitude for each layer.
	 * @return PyramidUpdaterFloat
	 */
	public static <T extends ImageSingleBand>
	PyramidFloat<T> floatGaussian( double scaleFactors[], double []sigmas , Class<T> imageType ) {

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixel(imageType);

		return new PyramidFloatGaussianScale<T>(interp,scaleFactors,sigmas,imageType);
	}

	/**
	 * Constructs an image pyramid which is designed to mimic a {@link boofcv.struct.gss.GaussianScaleSpace}.  Each layer in the pyramid
	 * should have the equivalent amount of blur that a space-space constructed with the same parameters would have.
	 *
	 * @param scaleSpace The scale of each layer and the desired amount of blur relative to the original image
	 * @param imageType Type of image
	 * @return Image pyramid
	 */
	public static <T extends ImageSingleBand>
	PyramidFloat<T> scaleSpacePyramid( double scaleSpace[], Class<T> imageType ) {

		double[] sigmas = new double[ scaleSpace.length ];

		sigmas[0] = scaleSpace[0];
		for( int i = 1; i < scaleSpace.length; i++ ) {
			// the desired amount of blur
			double c = scaleSpace[i];
			// the effective amount of blur applied to the last level
			double b = scaleSpace[i-1];
			// the amount of additional blur which is needed
			sigmas[i] = Math.sqrt(c*c-b*b);
			// take in account the change in image scale
			sigmas[i] /= scaleSpace[i-1];
		}

		return floatGaussian(scaleSpace,sigmas,imageType);
	}

	public static <T extends ImageSingleBand>
	PyramidFloat<T> scaleSpace( double scaleSpace[], Class<T> imageType ) {

		double[] scaleFactors = new double[ scaleSpace.length ];

		for( int i = 0; i < scaleSpace.length; i++ ) {
			scaleFactors[i] = 1;
		}

		// find the amount of blue that it needs to apply at each layer
		double[] sigmas = new double[ scaleSpace.length ];

		sigmas[0] = scaleSpace[0];
		for( int i = 1; i < scaleSpace.length; i++ ) {
			// the desired amount of blur
			double c = scaleSpace[i];
			// the effective amount of blur applied to the last level
			double b = scaleSpace[i-1];
			// the amount of additional blur which is needed
			sigmas[i] = Math.sqrt(c*c-b*b);
		}

		return floatGaussian(scaleFactors,sigmas,imageType);
	}
}
