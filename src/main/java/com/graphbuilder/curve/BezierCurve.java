/*
* Copyright (c) 2005, Graph Builder
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* * Redistributions of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* * Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* * Neither the name of Graph Builder nor the names of its contributors may be
* used to endorse or promote products derived from this software without
* specific prior written permission.

* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
* AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
* IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
* FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
* DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
* SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
* CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
* OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.graphbuilder.curve;

import com.graphbuilder.math.PascalsTriangle;

/**
<p>General n-point Bezier curve implementation.  The Bezier curve defines itself using all the points
from the control-path specified by the group-iterator.  To compute a single point on the curve requires
O(n) multiplications where n is the group-size of the group-iterator.  Thus, the Bezier curve is
considered to be expensive, but it has several mathematical properties (not discussed here) that
make it appealing.  Figure 1 shows an example of a Bezier curve.

<p><center><img align="center" src="doc-files/bezier1.gif"/></center>

<p>The maximum number of points that the Bezier curve can use is 1030 because the evaluation of a point
uses the nCr (n-choose-r) function.  The computation uses double precision, and double precision cannot
represent the result of 1031 choose i, where i = [500, 530].

@see com.graphbuilder.curve.Curve
@see com.graphbuilder.math.PascalsTriangle
*/

public class BezierCurve extends ParametricCurve {

	private static final ThreadLocal<SharedData> SHARED_DATA = new ThreadLocal<SharedData>(){
		protected SharedData initialValue() {
			return new SharedData();
		}
	};
	private final SharedData sharedData = SHARED_DATA.get();
	private final PascalsTriangle pascalsTriangle = new PascalsTriangle();
	
	private static class SharedData {
		// a[] is required to compute (1 - t)^n starting from the last index.
		// The idea is that all Bezier curves can share the same array, which
		// is more memory efficient than each Bezier curve having its own array.
		private double[] a = new double[0];
	}
	

	private double t_min = 0.0;
	private double t_max = 1.0;
	private int sampleLimit = 1;

	public BezierCurve(ControlPath cp, GroupIterator gi) {
		super(cp, gi);
	}

	public void eval(double[] p) {
		double t = p[p.length - 1];

		int numPts = gi.getGroupSize();

		if (numPts > sharedData.a.length)
			sharedData.a = new double[2 * numPts];

		sharedData.a[numPts - 1] = 1;
		double b = 1.0;
		double one_minus_t = 1.0 - t;

		for (int i = numPts - 2; i >= 0; i--)
			sharedData.a[i] = sharedData.a[i+1] * one_minus_t;

		gi.set(0, 0);

		int i = 0;

		while (i < numPts) {
			double pt = pascalsTriangle.nCr(numPts - 1, i);

			if (Double.isInfinite(pt) || Double.isNaN(pt)) {
				// are there any techniques that can be used
				// to calculate past 1030 points?
				// 1031 choose 515 == infinity
			}
			else {
				double gravity = sharedData.a[i] * b * pt;
				double[] d = cp.getPoint(gi.next()).getLocation();

				for (int j = 0; j < p.length - 1; j++)
					p[j] = p[j] + d[j] * gravity;
			}

			b = b * t;
			i++;
		}
	}

	public int getSampleLimit() {
		return sampleLimit;
	}

	/**
	Sets the sample-limit.  For more information on the sample-limit, see the
	BinaryCurveApproximationAlgorithm class.  The default sample-limit is 1.

	@throws IllegalArgumentException If sample-limit < 0.
	@see com.graphbuilder.curve.BinaryCurveApproximationAlgorithm
	@see #getSampleLimit()
	*/
	public void setSampleLimit(int limit) {
		if (limit < 0)
			throw new IllegalArgumentException("Sample-limit >= 0 required.");

		sampleLimit = limit;
	}

	/**
	Specifies the interval that the curve should define itself on.  The default interval is [0.0, 1.0].

	@throws IllegalArgumentException If t_min > t_max.
	@see #t_min()
	@see #t_max()
	*/
	public void setInterval(double t_min, double t_max) {
		if (t_min > t_max)
			throw new IllegalArgumentException("t_min <= t_max required.");

		this.t_min = t_min;
		this.t_max = t_max;
	}

	/**
	Returns the starting interval value.

	@see #setInterval(double, double)
	@see #t_max()
	*/
	public double t_min() {
		return t_min;
	}

	/**
	Returns the finishing interval value.

	@see #setInterval(double, double)
	@see #t_min()
	*/
	public double t_max() {
		return t_max;
	}

	/**
	The only requirement for this curve is the group-iterator must be in range or this method returns quietly.
	*/
	public void appendTo(MultiPath mp) {
		if (!gi.isInRange(0, cp.numPoints()))
			throw new IllegalArgumentException("group iterator not in range");;

		int n = mp.getDimension();

		double[] d = new double[n + 1];
		d[n] = t_min;
		eval(d);

		if (connect)
			mp.lineTo(d);
		else
			mp.moveTo(d);

		BinaryCurveApproximationAlgorithm.genPts(this, t_min, t_max, mp);
	}

	public void resetMemory() {
		if (sharedData.a.length > 0)
			sharedData.a = new double[0];
	}
}
