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
<p>The natural-cubic-spline is constructed using piecewise third order polynomials which pass through all the
control-points specified by the group-iterator.  The curve can be open or closed.  Figure 1 shows an open
curve and figure 2 shows a closed curve.

<p><center><img align="center" src="doc-files/natcubic1.gif"/></center>

<p><center><img align="center" src="doc-files/natcubic2.gif"/></center>

*/
public class NaturalCubicSpline extends ParametricCurve {

/*
The pt array stores the points of the control-path.
The data array is used to store the result of the many calculations.

d[0] = w1  For each dimension, 4 arrays are required to store the
d[1] = x1  results of the calculations.
d[2] = y1  The length of each array is >= to the number of points.
d[3] = z1
d[4] = w2
d[5] = x2
d[6] = y2
d[7] = z2
d[8] = a   // a, b & c are used (by both open and closed) to store
d[9] = b   // the results of the calculations.
d[10] = c
d[11] = d // only used for closed cubic curves
*/

	private static final ThreadLocal<SharedData> SHARED_DATA = new ThreadLocal<SharedData>(){
		protected SharedData initialValue() {
			return new SharedData();
		}
	};
	private final SharedData sharedData = SHARED_DATA.get();

	private static class SharedData {
		private double[][] pt = new double[0][];
		private double[][] data = new double[0][];
		private int ci = 0;
	}

	private boolean closed = false;

	public NaturalCubicSpline(ControlPath cp, GroupIterator gi) {
		super(cp, gi);
	}

	protected void eval(double[] p) {
		int n = p.length - 1; // dimension

		double t = p[n];
		double t2 = t * t;
		double t3 = t2 * t;

		int j = 0;
		for (int i = 0; i < n; i++)
			p[i] = sharedData.data[j++][sharedData.ci] + sharedData.data[j++][sharedData.ci] * t + sharedData.data[j++][sharedData.ci] * t2 + sharedData.data[j++][sharedData.ci] * t3;
	}

	// n is the # of points
	// dim is the dimension
	private void precalc(int n, int dim, boolean closed) {
		n--;

		double[] a = sharedData.data[4 * dim];
		double[] b = sharedData.data[4 * dim + 1];
		double[] c = sharedData.data[4 * dim + 2];
		int k = 0;

		if (closed) {
			double[] d = sharedData.data[4 * dim + 3];
			double e, f, g, h;

			for (int j = 0; j < dim; j++) {
				d[1] = a[1] = e = 0.25;
				b[0] = e * 3 * (sharedData.pt[1][j] - sharedData.pt[n][j]);
				h = 4;
				f = 3 * (sharedData.pt[0][j] - sharedData.pt[n-1][j]);
				g = 1;
				for (int i = 1; i < n; i++) {
					a[i+1] = e = 1.0 / (4.0 - a[i]);
					d[i+1] = -e * d[i];
					b[i] = e * (3.0 * (sharedData.pt[i+1][j] - sharedData.pt[i-1][j]) - b[i-1]);
					h = h - g * d[i];
					f = f - g * b[i-1];
					g = -a[i] * g;
				}
				h = h - (g + 1) * (a[n] + d[n]);
				b[n] = f - (g + 1) * b[n-1];

				c[n] = b[n] / h;
				c[n-1] = b[n-1] - (a[n] + d[n]) * c[n];
				for (int i = n-2; i >= 0; i--) {
					c[i] = b[i] - a[i+1] * c[i+1] - d[i+1] * c[n];
				}

				double[] w = sharedData.data[k++];
				double[] x = sharedData.data[k++];
				double[] y = sharedData.data[k++];
				double[] z = sharedData.data[k++];

				for (int i = 0; i < n; i++) {
					w[i] = sharedData.pt[i][j];
					x[i] = c[i];
					y[i] = 3 * (sharedData.pt[i+1][j] - sharedData.pt[i][j]) - 2 * c[i] - c[i+1];
					z[i] = 2 * (sharedData.pt[i][j] - sharedData.pt[i+1][j]) + c[i] + c[i+1];
				}

				w[n] = sharedData.pt[n][j];
				x[n] = c[n];
				y[n] = 3 * (sharedData.pt[0][j] - sharedData.pt[n][j]) - 2 * c[n] - c[0];
				z[n] = 2 * (sharedData.pt[n][j] - sharedData.pt[0][j]) + c[n] + c[0];
			}
		}
		else {
			for (int j = 0; j < dim; j++) {
				a[0] = 0.5;
				for (int i = 1; i < n; i++) {
					a[i] = 1.0 / (4 - a[i-1]);
				}
				a[n] = 1.0 / (2.0 - a[n-1]);

				b[0] = a[0] * (3 * (sharedData.pt[1][j] - sharedData.pt[0][j]));
				for (int i = 1; i < n; i++) {
					b[i] = a[i] * (3 * (sharedData.pt[i+1][j] - sharedData.pt[i-1][j]) - b[i-1]);
				}
				b[n] = a[n] * (3 * (sharedData.pt[n][j] - sharedData.pt[n-1][j]) - b[n-1]);

				c[n] = b[n];
				for (int i = n-1; i >= 0; i--) {
					c[i] = b[i] - a[i] * c[i+1];
				}

				double[] w = sharedData.data[k++];
				double[] x = sharedData.data[k++];
				double[] y = sharedData.data[k++];
				double[] z = sharedData.data[k++];

				for (int i = 0; i < n; i++) {
					w[i] = sharedData.pt[i][j];
					x[i] = c[i];
					y[i] = 3 * (sharedData.pt[i+1][j] - sharedData.pt[i][j]) - 2 * c[i] - c[i+1];
					z[i] = 2 * (sharedData.pt[i][j] - sharedData.pt[i+1][j]) + c[i] + c[i+1];
				}

				w[n] = sharedData.pt[n][j];
				x[n] = 0;
				y[n] = 0;
				z[n] = 0;
			}
		}
	}


	/**
	The closed attribute determines which tri-diagonal matrix to solve.

	@see #getClosed()
	*/
	public void setClosed(boolean b) {
		closed = b;
	}

	/**
	Returns the value of closed.  The default value is false.

	@see #setClosed(boolean)
	*/
	public boolean getClosed() {
		return closed;
	}

	/**
	Returns a value of 1.
	*/
	public int getSampleLimit() {
		return 1;
	}

	/**
	The requirements for this curve are the group-iterator must be in-range and have a group size of at least 2.
	If these requirements are not met then this method raises IllegalArgumentException
	*/
	public void appendTo(MultiPath mp) {
		if (!gi.isInRange(0, cp.numPoints()))
			throw new IllegalArgumentException("Group iterator not in range");

		final int n = gi.getGroupSize();
		if (n < 2)
			throw new IllegalArgumentException("Group iterator size < 2");

		int dim = mp.getDimension();

		// make sure there is enough room
		//-------------------------------------------------------
		int x = 3 + 4 * dim + 1;

		if (sharedData.data.length < x) {
			double[][] temp = new double[x][];

			for (int i = 0; i < sharedData.data.length; i++)
				temp[i] = sharedData.data[i];

			sharedData.data = temp;
		}

		if (sharedData.pt.length < n) {
			int m = 2 * n;

			sharedData.pt = new double[m][];

			for (int i = 0; i < sharedData.data.length; i++)
				sharedData.data[i] = new double[m];
		}
		//-------------------------------------------------------

		gi.set(0, 0);

		for (int i = 0; i < n; i++)
			sharedData.pt[i] = cp.getPoint(gi.next()).getLocation(); // assign the used points to pt

		precalc(n, dim, closed);

		sharedData.ci = 0; // do not remove

		double[] p = new double[dim + 1];
		eval(p);

		if (connect)
			mp.lineTo(p);
		else
			mp.moveTo(p);

		// Note: performing a ci++ or ci = ci + 1 results in funny behavior
		for (int i = 0; i < n; i++) {
			sharedData.ci = i;
			BinaryCurveApproximationAlgorithm.genPts(this, 0.0, 1.0, mp);
		}
	}

	public void resetMemory() {
		if (sharedData.pt.length > 0)
			sharedData.pt = new double[0][];

		if (sharedData.data.length > 0)
			sharedData.data = new double[0][];
	}
}
