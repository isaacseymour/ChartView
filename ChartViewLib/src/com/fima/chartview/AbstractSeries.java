package com.fima.chartview;

import java.util.SortedSet;
import java.util.Collections;
import java.util.Collection;
import java.util.TreeSet;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
	
public abstract class AbstractSeries {
	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS
	//////////////////////////////////////////////////////////////////////////////////////

	protected Paint mPaint = new Paint();

	private final SortedSet<AbstractPoint> mPoints = Collections.synchronizedSortedSet(new TreeSet<AbstractPoint>());

	private double mMinX = Double.MAX_VALUE;
	private double mMaxX = Double.MIN_VALUE;
	private double mMinY = Double.MAX_VALUE;
	private double mMaxY = Double.MIN_VALUE;

	private double mRangeX = 0;
	private double mRangeY = 0;
	protected abstract void drawPoint(Canvas canvas, AbstractPoint point, float scaleX, float scaleY, Rect gridBounds);


	//////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	//////////////////////////////////////////////////////////////////////////////////////
	public AbstractSeries() {
		mPaint.setAntiAlias(true);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	public SortedSet<AbstractPoint> getPoints() {
		return mPoints;
	}

	public void setPoints(Collection<? extends AbstractPoint> points) {
		mPoints.clear();
		mPoints.addAll(points);

		recalculateRange();
	}

	public void addPoint(AbstractPoint point) {
		extendRange(point.getX(), point.getY());
		mPoints.add(point);
	}

	// Remove a point from the series. Avoid using this as it's potentially very costly!
	public void removePoint(AbstractPoint point) {
		// Is this the min/max point?
		mPoints.remove(point);

		// Range corrections:
		// If this was at the very top or bottom we're in trouble. We have to entirely re-calculate the range to condense it in vertically
		if(point.getY() == mMinY || point.getY() == mMaxY) recalculateRange();

		// X-range corrections are much simpler as the points are sorted by x-value
		if(point.getX() == mMinX) { // condense in from the left
			mMinX = mPoints.first().getX();
			mRangeX = mMaxX - mMinX;
		} else if(point.getX() == mMaxX) { // condense in from the right
			mMaxX = mPoints.last().getX();
			mRangeX = mMaxX - mMinX;
		}
	}

	// Line properties

	public void setLineColor(int color) {
		mPaint.setColor(color);
	}

	public void setLineWidth(float width) {
		mPaint.setStrokeWidth(width);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	private void resetRange() {
		mMinX = Double.MAX_VALUE;
		mMaxX = Double.MIN_VALUE;
		mMinY = Double.MAX_VALUE;
		mMaxY = Double.MIN_VALUE;

		mRangeX = 0;
		mRangeY = 0;
	}

	private void extendRange(double x, double y) {
		if (x < mMinX) mMinX = x;
		if (x > mMaxX) mMaxX = x;
		if (y < mMinY) mMinY = y;
		if (y > mMaxY) mMaxY = y;

		mRangeX = mMaxX - mMinX;
		mRangeY = mMaxY - mMinY;
	}

	protected void recalculateRange() {
		resetRange();

		for (AbstractPoint point : mPoints)
			extendRange(point.getX(), point.getY());
	}

	public double getMinX() {
		return mMinX;
	}

	public double getMaxX() {
		return mMaxX;
	}

	public double getMinY() {
		return mMinY;
	}

	public double getMaxY() {
		return mMaxY;
	}

	public double getRangeX() {
		return mRangeX;
	}

	public double getRangeY() {
		return mRangeY;
	}

	void draw(Canvas canvas, Rect gridBounds, float scaleX, float scaleY) {
		for (AbstractPoint point : mPoints)
			drawPoint(canvas, point, scaleX, scaleY, gridBounds);

		onDrawingComplete();
	}

	protected void onDrawingComplete() {
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC CLASSES
	//////////////////////////////////////////////////////////////////////////////////////

	public static abstract class AbstractPoint implements Comparable<AbstractPoint> {
		private double mX;
		private double mY;

		public AbstractPoint() {
		}

		public AbstractPoint(double x, double y) {
			mX = x;
			mY = y;
		}

		public double getX() {
			return mX;
		}

		public double getY() {
			return mY;
		}

		public void set(double x, double y) {
			mX = x;
			mY = y;
		}

		@Override
		public int compareTo(AbstractPoint another) {
			return Double.compare(mX, another.mX);
		}

		@Override
		public String toString() {
			return "("+mX+", "+mY+")";
		}
	}

	public String toString() {
		return mPoints.toString();
	}
}