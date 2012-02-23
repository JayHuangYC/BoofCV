/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.epipolar;

import boofcv.abst.geo.epipolar.RefineEpipolarMatrix;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.ParameterizeModel;
import boofcv.alg.geo.d3.epipolar.f.ParamFundamentalEpipolar;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedMinimization;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Improves upon the initial estimate of the Fundamental matrix by minimizing the sampson error.
 *
 * Found to be much slower and doesn't produce as good of an answer as least squares.
 *
 * @author Peter Abeles
 */
public class QuasiNewtonFundamentalSampson implements RefineEpipolarMatrix {
	ParameterizeModel<DenseMatrix64F> paramModel;
	FunctionSampsonFundamental func = new FunctionSampsonFundamental();
	double param[];

	UnconstrainedMinimization minimizer;

	DenseMatrix64F found = new DenseMatrix64F(3,3);
	int maxIterations;

	public QuasiNewtonFundamentalSampson(double convergenceTol, int maxIterations) {
		this( new ParamFundamentalEpipolar() , convergenceTol, maxIterations);
	}

	public QuasiNewtonFundamentalSampson(ParameterizeModel<DenseMatrix64F> paramModel,
										 double convergenceTol, int maxIterations) {
		this.paramModel = paramModel;
		this.maxIterations = maxIterations;

		param = new double[paramModel.numParameters()];

		minimizer = FactoryOptimization.unconstrained(convergenceTol,convergenceTol,0);
	}

	@Override
	public boolean process(DenseMatrix64F F, List<AssociatedPair> obs) {
		func.set(paramModel, obs);
		
		paramModel.modelToParam(F, param);

		minimizer.setFunction(func,null);

		minimizer.initialize(param);

		for( int i = 0; i < maxIterations; i++ ) {
			if( minimizer.iterate() )
				break;
		}

		paramModel.paramToModel(minimizer.getParameters(),found);

		return true;
	}

	@Override
	public DenseMatrix64F getRefinement() {
		return found;
	}
}