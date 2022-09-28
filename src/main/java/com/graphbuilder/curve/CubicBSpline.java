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


/**
<p>The cubic B-spline is defined by third order polynomial basis functions.  Each point on the curve
is locally controlled by 4 control-points.  In general, the curve does not pass through the control points,
only near.  The exceptions to this are the first and last control-points and if there are duplicate sequential
control-points.

<p>The CubicBSpline is the same as the BSpline with degree 3.  However, the CubicBSpline is much faster
to compute than the BSpline.  The following table represents the approximate speed difference in computation
between the CubicBSpline and BSpline of degree 3.

<p>
<center>
<table border="1">
	<tr><td><b>Number of Points</b></td><td><b>Times Faster</b></td></tr>
	<tr><td>10</td><td>4.6</td></tr>
	<tr><td>20</td><td>6.9</td></tr>
	<tr><td>30</td><td>9.5</td></tr>
	<tr><td>40</td><td>11</td></tr>
</table>
Table 1: Efficiency of CubicBSpline
</center>

<p>As the number of points increases, the BSpline gets slower and slower.  The reason is the CubicBSpline is
built in segments, using 4 points at a time.  However, the BSpline is a single segment, and requires iteration
through all the points to compute a single point on the curve.  Unlike the BSpline, the CubicBSpline does not
have a knot vector, a definable sample limit, range or degree, which means the memory required for the
CubicBSpline is less than the BSpline.

<p>Relative to other curves, the cubic B-spline is computationally inexpensive, and easy to work with.
To create a closed cubic B-spline, use "0:n-1,0:2" as the control-string and set interpolateEndpoints
to false.  Figures 1, 2 & 3 show some examples of cubic B-splines.  See the appendTo method for more
information.

<p><center><img align="center" src="doc-files/cubicb1.gif"/></center>

<p><center><img align="center" src="doc-files/cubicb2.gif"/></center>

<p><center><img align="center" src="doc-files/cubicb3.gif"/></center>

@see BSpline
*/
public class CubicBSpline extends ParametricCurve {

	private static final ThreadLocal<SharedData> SHARED_DATA = new ThreadLocal<SharedData>(){
		protected SharedData initialValue() {
			return new SharedData();
		}
	};
	private final SharedData sharedData = SHARED_DATA.get();

	private static class SharedData {
		private int section = 0;
		private int numPoints = 0;
		private double[][] pt = new double[4][];
		private double[] b = new double[4];
	}

	private boolean interpolateEndpoints = false;

	public CubicBSpline(ControlPath cp, GroupIterator gi) {
		super(cp, gi);
	}

	protected void eval(double[] p) {

		double t = p[p.length - 1];
		double t2 = t * t;
		double t3 = t2 * t;

		double u = 1 - t;
		double u2 = u * u;
		double u3 = u2 * u;

		if (sharedData.numPoints == 4) {
			sharedData.b[0] = u2 * u;
			sharedData.b[1] = 3 * u2 * t;
			sharedData.b[2] = 3 * u * t2;
			sharedData.b[3] = t3;
		}
		else if (sharedData.numPoints == 5) {
			if (sharedData.section == 0) {
				sharedData.b[0] = u3;
				sharedData.b[1] = 7 * t3 / 4 - 9 * t2 / 2 + 3 * t;
				sharedData.b[2] = -t3 + 3 * t2 / 2;
				sharedData.b[3] = t3 / 4;
			}
			else {
				sharedData.b[0] = u3 / 4;
				sharedData.b[1] = -u3 + 3 * u2 / 2;
				sharedData.b[2] = 7 * u3 / 4 - 9 * u2 / 2 + 3 * u;
				sharedData.b[3] = t3;
			}
		}
		else if (sharedData.numPoints == 6) {
			if (sharedData.section == 0) {
				sharedData.b[0] = u3;
				sharedData.b[1] = 7 * t3 / 4 - 9 * t2 / 2 + 3 * t;
				sharedData.b[2] = -11 * t3 / 12 + 3 * t2 / 2;
				sharedData.b[3] = t3 / 6;
			}
			else if (sharedData.section == 1) {
				sharedData.b[0] = u3 / 4;
				sharedData.b[1] = 7 * t3 / 12 - 5 * t2 / 4 + t / 4 + 7.0 / 12;
				sharedData.b[2] = -7 * t3 / 12 + t2 / 2 + t / 2 + 1.0 / 6;
				sharedData.b[3] = t3 / 4;
			}
			else {
				sharedData.b[0] = u3 / 6;
				sharedData.b[1] = -11 * u3 / 12 + 3 * u2 / 2;
				sharedData.b[2] = 7 * u3 / 4 - 9 * u2 / 2 + 3 * u;
				sharedData.b[3] = t3;
			}
		}
		else { // 7 and >= 8 have the same basis functions
			if (sharedData.section == 0) {
				sharedData.b[0] = u3;
				sharedData.b[1] = 7 * t3 / 4 - 9 * t2 / 2 + 3 * t;
				sharedData.b[2] = -11 * t3 / 12 + 3 * t2 / 2;
				sharedData.b[3] = t3 / 6;
			}
			else if (sharedData.section == 1) {
				sharedData.b[0] = u3 / 4;
				sharedData.b[1] = 7 * t3 / 12 - 5 * t2 / 4 + t / 4 + 7.0 / 12;
				sharedData.b[2] = -t3 / 2 + t2 / 2 + t / 2 + 1.0 / 6;
				sharedData.b[3] = t3 / 6;
			}
			else if (sharedData.section == 2) { // if numPoints == 7 then section 2 gets skipped
				sharedData.b[0] = u3 / 6;
				sharedData.b[1] = t3 / 2 - t2 + 2.0 / 3;
				sharedData.b[2] = (-t3 + t2 + t) / 2 + 1.0 / 6;
				sharedData.b[3] = t3 / 6;
			}
			else if (sharedData.section == 3) {
				sharedData.b[0] = u3 / 6;
				sharedData.b[1] = -u3 / 2 + u2 / 2 + u / 2 + 1.0 / 6;
				sharedData.b[2] = 7 * u3 / 12 - 5 * u2 / 4 + u / 4 + 7.0 / 12;
				sharedData.b[3] = t3 / 4;
			}
			else {
				sharedData.b[0] = u3 / 6;
				sharedData.b[1] = -11 * u3 / 12 + 3 * u2 / 2;
				sharedData.b[2] = 7 * u3 / 4 - 9 * u2 / 2 + 3 * u;
				sharedData.b[3] = t3;
			}
		}

		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < p.length - 1; j++)
				p[j] = p[j] + sharedData.pt[i][j] * sharedData.b[i];
		}
	}

	/**
	Returns a value of 1.
	*/
	public int getSampleLimit() {
		return 1;
	}

	/**
	Sets the curve to start at the first control-point and end at the last control-point specified by
	the group-iterator.

	@see #getInterpolateEndpoints()
	*/
	public void setInterpolateEndpoints(boolean b) {
		interpolateEndpoints = b;
	}

	/**
	Returns the interpolateEndpoints value.  The default value is false.

	@see #setInterpolateEndpoints(boolean)
	*/
	public boolean getInterpolateEndpoints() {
		return interpolateEndpoints;
	}

	/**
	The requirements for this curve are the group-iterator must be in-range and have a group size of at least 4.
	If these requirements are not met then this method throws IllegalArgumentException
	*/
	public void appendTo(MultiPath mp) {
		if (!gi.isInRange(0, cp.numPoints()))
			throw new IllegalArgumentException("Group iterator not in range");
		int n = gi.getGroupSize();
		if (n < 4)
			throw new IllegalArgumentException("Group iterator size < 4");

		if (interpolateEndpoints) {
			sharedData.numPoints = n;
			sharedData.section = 0;
		}
		else {
			sharedData.numPoints = -1; // defaults to numPoints >= 7 in the eval method
			sharedData.section = 2;	// section doesn't change when interpolateEndpoints == false
		}

		gi.set(0, 0);
		int index_i = 0;
		int count_j = 0;

		for (int i = 0; i < 4; i++)
			sharedData.pt[i] = cp.getPoint(gi.next()).getLocation();

		double[] d = new double[mp.getDimension() + 1];
		eval(d);

		if (connect)
			mp.lineTo(d);
		else
			mp.moveTo(d);

		int j = 3;

		while (true) {
			BinaryCurveApproximationAlgorithm.genPts(this, 0.0, 1.0, mp);
			j++;
			if (j == n) break;

			gi.set(index_i, count_j);
			gi.next();
			index_i = gi.index_i();
			count_j = gi.count_j();

			for (int i = 0; i < 4; i++)
				sharedData.pt[i] = cp.getPoint(gi.next()).getLocation();

			if (interpolateEndpoints) {
				if (n < 7) {
					sharedData.section++;
				}
				else {
					if (sharedData.section != 2)
						sharedData.section++;

					if (sharedData.section == 2 && j == n - 2)
						sharedData.section++;
				}
			}
		}
	}
}
