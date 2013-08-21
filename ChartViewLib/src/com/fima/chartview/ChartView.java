package com.fima.chartview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class ChartView extends RelativeLayout {
	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE MEMBERS
	//////////////////////////////////////////////////////////////////////////////////////

	private static final String TAG = "ChartView";

	// View

	private Paint mPaint = new Paint();
	private Paint mTextPaint = new Paint();

	// Series

	private List<AbstractSeries> mSeries = new ArrayList<AbstractSeries>();

	// Labels

	private LabelAdapter mLeftLabelAdapter;
	private LabelAdapter mTopLabelAdapter;
	private LabelAdapter mBottomLabelAdapter;
	private LabelAdapter mRightLabelAdapter;

	private LinearLayout mLeftLabelLayout;
	private LinearLayout mTopLabelLayout;
	private LinearLayout mBottomLabelLayout;
	private LinearLayout mRightLabelLayout;

	private int mLeftLabelWidth;
	private int mTopLabelHeight;
	private int mRightLabelWidth;
	private int mBottomLabelHeight;

	private int mLabelTextColor;
	private float mLabelTextSize;

	// Range
	private RectD mValueBounds = new RectD();
	private double mMinX = Double.MAX_VALUE;
	private double mMaxX = Double.MIN_VALUE;
	private double mMinY = Double.MAX_VALUE;
	private double mMaxY = Double.MIN_VALUE;

	// Grid

	private Rect mGridBounds = new Rect();
	private int mGridLineColor;
	private int mGridLineWidth;
	private int mGridLinesHorizontal;
	private int mGridLinesVertical;
	private int mGridFixedXGap;
	private int mGridFixedYGap;
	private enum Axis { X, Y }

	//////////////////////////////////////////////////////////////////////////////////////
	// CONSTRUCTORS
	//////////////////////////////////////////////////////////////////////////////////////

	public ChartView(Context context) {
		this(context, null, 0);
	}

	public ChartView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ChartView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		setWillNotDraw(false);
		setBackgroundColor(Color.TRANSPARENT);

		final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ChartView);

		mGridLineColor = attributes.getInt(R.styleable.ChartView_gridLineColor, Color.BLACK);
		mGridLineWidth = attributes.getDimensionPixelSize(R.styleable.ChartView_gridLineWidth, 1);
		mGridLinesHorizontal = attributes.getInt(R.styleable.ChartView_gridLinesHorizontal, 5);
		mGridLinesVertical = attributes.getInt(R.styleable.ChartView_gridLinesVertical, 5);
		mGridFixedXGap = attributes.getInt(R.styleable.ChartView_gridLinesXGap, -1);
		mGridFixedYGap = attributes.getInt(R.styleable.ChartView_gridLinesYGap, -1);
		mLabelTextColor = attributes.getColor(R.styleable.ChartView_labelTextColor, Color.BLACK);
		mLabelTextSize = attributes.getDimension(R.styleable.ChartView_labelTextSize, 16.0F);
		mLeftLabelWidth = attributes.getDimensionPixelSize(R.styleable.ChartView_leftLabelWidth, 0);
		mTopLabelHeight = attributes.getDimensionPixelSize(R.styleable.ChartView_topLabelHeight, 0);
		mRightLabelWidth = attributes.getDimensionPixelSize(R.styleable.ChartView_rightLabelWidth, 0);
		mBottomLabelHeight = attributes.getDimensionPixelSize(R.styleable.ChartView_bottomLabelHeight, 0);

		// left label layout
		mLeftLabelLayout = new LinearLayout(context);
		mLeftLabelLayout.setLayoutParams(new LayoutParams(mLeftLabelWidth, LayoutParams.MATCH_PARENT));
		mLeftLabelLayout.setOrientation(LinearLayout.VERTICAL);

		// top label layout
		mTopLabelLayout = new LinearLayout(context);
		mTopLabelLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, mTopLabelHeight));
		mTopLabelLayout.setOrientation(LinearLayout.HORIZONTAL);

		// right label layout
		LayoutParams rightLabelParams = new LayoutParams(mRightLabelWidth, LayoutParams.MATCH_PARENT);
		rightLabelParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		mRightLabelLayout = new LinearLayout(context);
		mRightLabelLayout.setLayoutParams(rightLabelParams);
		mRightLabelLayout.setOrientation(LinearLayout.VERTICAL);

		// bottom label layout
		LayoutParams bottomLabelParams = new LayoutParams(LayoutParams.MATCH_PARENT, mBottomLabelHeight);
		bottomLabelParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		mBottomLabelLayout = new LinearLayout(context);
		mBottomLabelLayout.setLayoutParams(bottomLabelParams);
		mBottomLabelLayout.setOrientation(LinearLayout.HORIZONTAL);

		// Add label views
		addView(mLeftLabelLayout);
		addView(mTopLabelLayout);
		addView(mRightLabelLayout);
		addView(mBottomLabelLayout);

		// Apply the label text settings to the text painter
		mTextPaint.setColor(mLabelTextColor);
		mTextPaint.setStyle(Paint.Style.FILL);
		mTextPaint.setTextAlign(Paint.Align.CENTER);
		mTextPaint.setAntiAlias(true);
		mTextPaint.setTextSize(mLabelTextSize);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PUBLIC METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	/*
	 * Remove all lines from the chart
	 */
	public void clearSeries() {
		mSeries.clear();
		resetRange();
		invalidate();
	}

	/*
	 * Add a series to the chart
	 */
	public void addSeries(AbstractSeries series) {
		// Add the series
		mSeries.add(series);

		// Update labels
		if(mLeftLabelAdapter != null) setVerticalAdapterValues(mLeftLabelAdapter);
		if(mRightLabelAdapter != null) setVerticalAdapterValues(mRightLabelAdapter);
		if(mTopLabelAdapter != null) setHorizontalAdapterValues(mTopLabelAdapter);
		if(mBottomLabelAdapter!= null) setHorizontalAdapterValues(mBottomLabelAdapter);

		// Make sure the chart is the right size
		resetRange();

		// And redraw
		invalidate();
	}

	// Instruct label adapters which values to show a label at
	private void setVerticalAdapterValues(LabelAdapter adapter) {
		adapter.setValues(calculateLabelValues(mGridLinesVertical, mGridFixedYGap, mValueBounds.top, mValueBounds.bottom));
	}

	private void setHorizontalAdapterValues(LabelAdapter adapter) {
		adapter.setValues(calculateLabelValues(mGridLinesHorizontal, mGridFixedXGap, mValueBounds.left, mValueBounds.right));
	}

	// Calculate values at which to show a grid label
	private Double[] calculateLabelValues(int numLines, int fixedGap, double minValue, double maxValue) {
		Log.i(TAG, "Calculating label values with: numLines = "+numLines+", fixedGap = "+fixedGap+", minValue = "+minValue+", maxValue = "+maxValue);
		if(fixedGap <= 0) { // Fixed number of lines = numLines+2 (for each end)
			final Double[] values = new Double[numLines + 2];
			final double step = (maxValue - minValue) / (numLines + 1);
			for(int i = 0; i < numLines + 2; i++)
				values[i] = minValue + (step * i);
			Log.d(TAG, "Label values: " + Arrays.asList(values).toString());
			return values;
		} else {
			return new Double[0];
			// In this case, we don't use the LinearLayout to draw on the text, it gets drawn on with the grid so
			// that the positioning is correct
		}
	}

	public void setLeftLabelAdapter(LabelAdapter adapter) {
		mLeftLabelAdapter = adapter;

		setVerticalAdapterValues(mLeftLabelAdapter);
	}

	public void setTopLabelAdapter(LabelAdapter adapter) {
		mTopLabelAdapter = adapter;

		setHorizontalAdapterValues(mTopLabelAdapter);
	}

	public void setRightLabelAdapter(LabelAdapter adapter) {
		mRightLabelAdapter = adapter;

		setVerticalAdapterValues(mRightLabelAdapter);
	}

	public void setBottomLabelAdapter(LabelAdapter adapter) {
		mBottomLabelAdapter = adapter;

		setHorizontalAdapterValues(mBottomLabelAdapter);
	}

	// Grid properties
	public void setGridLineColor(int color) {
		mGridLineColor = color;
	}

	public void setGridLineWidth(int width) {
		mGridLineWidth = width;
	}

	public void setGridLinesHorizontal(int count) {
		mGridLinesHorizontal = count;
	}

	public void setGridLinesVertical(int count) {
		mGridLinesVertical = count;
	}

	public void setGridFixedXGap(int gap) {
		mGridFixedXGap = gap;
	}

	public void setGridFixedYGap(int gap) {
		mGridFixedYGap = gap;
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// OVERRIDDEN METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		final int gridLeft = mLeftLabelWidth + mGridLineWidth - 1;
		final int gridTop = mTopLabelHeight + mGridLineWidth - 1;
		final int gridRight = getWidth() - mRightLabelWidth - mGridLineWidth;
		final int gridBottom = getHeight() - mBottomLabelHeight - mGridLineWidth;

		mGridBounds.set(gridLeft, gridTop, gridRight, gridBottom);

		// Set sizes
		LayoutParams leftParams = (LayoutParams) mLeftLabelLayout.getLayoutParams();
		leftParams.height = mGridBounds.height();
		mLeftLabelLayout.setLayoutParams(leftParams);

		LayoutParams topParams = (LayoutParams) mTopLabelLayout.getLayoutParams();
		topParams.width = mGridBounds.width();
		mTopLabelLayout.setLayoutParams(topParams);

		LayoutParams rightParams = (LayoutParams) mRightLabelLayout.getLayoutParams();
		rightParams.height = mGridBounds.height();
		mRightLabelLayout.setLayoutParams(rightParams);

		LayoutParams bottomParams = (LayoutParams) mBottomLabelLayout.getLayoutParams();
		bottomParams.width = mGridBounds.width();
		mBottomLabelLayout.setLayoutParams(bottomParams);

		// Set layouts
		mLeftLabelLayout.layout(0, gridTop, gridLeft, gridBottom);
		mTopLabelLayout.layout(gridLeft, 0, gridRight, gridTop);
		mRightLabelLayout.layout(gridRight, gridTop, getWidth(), gridBottom);
		mBottomLabelLayout.layout(gridLeft, gridBottom, gridRight, getHeight());
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		// How much to scale values to drawing co-ordinates
		final float scaleX = (float) mGridBounds.width() / (float) mValueBounds.width();
		final float scaleY = (float) mGridBounds.height() / (float) mValueBounds.height();

		// Draw on the grid lines and labels
		Log.d(TAG, "Drawing grid lines");
		drawGrid(canvas, scaleX, scaleY);
		Log.d(TAG, "Drawing labels");
		drawLabels();

		Log.d(TAG, "Drawing series");
		// Draw on the series
		for (AbstractSeries series : mSeries)
			series.draw(canvas, mGridBounds, scaleX, scaleY);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// PRIVATE METHODS
	//////////////////////////////////////////////////////////////////////////////////////

	/****
	 * Range calculations
	 */

	// Reset the visible range to show nothing
	private void resetRange() {
		mMinX = Double.MAX_VALUE;
		mMaxX = Double.MIN_VALUE;
		mMinY = Double.MAX_VALUE;
		mMaxY = Double.MIN_VALUE;

		for(AbstractSeries series : mSeries) {
			extendRange(series.getMinX(), series.getMinY());
			extendRange(series.getMaxX(), series.getMaxY());
		}

		Log.d(TAG, "New chart range: [" + mMinX + "," + mMinY + "][" + mMaxX + "," + mMaxY + "]");
	}

	// Expand the range of values shown
	private void extendRange(double x, double y) {
		if (x < mMinX) mMinX = x;
		if (x > mMaxX) mMaxX = x;
		if (y < mMinY) mMinY = y;
		if (y > mMaxY) mMaxY = y;

		// Prevent the lines from actually touching the top and bottom of the grid window
		double yPadding = 0.05*(mMaxY - mMinY);
		mMinY -= yPadding;
		mMaxY += yPadding;

		mValueBounds.set(mMinX, mMinY, mMaxX, mMaxY);
	}

	/****
	 * Drawing methods
	 */

	// Draw the grid lines
	private void drawGrid(Canvas canvas, float scaleX, float scaleY) {
		// Draw the x-grid (i.e. vertical lines)
		if(mGridFixedXGap > 0) drawGridFixedGap(canvas, Axis.X, scaleX);
		else drawGridFixedEnds(canvas, Axis.X, scaleX);

		// Draw the y-grid (i.e. horizontal lines)
		if(mGridFixedYGap > 0) drawGridFixedGap(canvas, Axis.Y, scaleY);
		else drawGridFixedEnds(canvas, Axis.Y, scaleY);
	}

	// Draw a grid with lines at every point which == 0 modulo a fixed gap
	private void drawGridFixedGap(Canvas canvas, Axis axis, float scale) {
		mPaint.setColor(mGridLineColor);
		mPaint.setStrokeWidth(mGridLineWidth);

		final int step = axis == Axis.X ? mGridFixedXGap : mGridFixedYGap;
		final double minPoint = axis == Axis.X ? mValueBounds.left : mValueBounds.top;
		final double maxPoint = axis == Axis.X ? mValueBounds.right : mValueBounds.bottom;

		Double pointCoord;
		final int originPointCoord = axis == Axis.X ? mGridBounds.left : mGridBounds.top;

		Log.d(TAG, "Drawing fixed-gap lines between "+minPoint+" and "+maxPoint+" in steps of "+step);

		// Enclose the grid on both sides for neatness
		if(axis == Axis.X) {
			canvas.drawLine(mGridBounds.left, mGridBounds.top, mGridBounds.left, mGridBounds.bottom, mPaint);
			canvas.drawLine(mGridBounds.right, mGridBounds.top, mGridBounds.right, mGridBounds.bottom, mPaint);
		} else {
			canvas.drawLine(mGridBounds.left, mGridBounds.top, mGridBounds.right, mGridBounds.top, mPaint);
			canvas.drawLine(mGridBounds.left, mGridBounds.bottom, mGridBounds.right, mGridBounds.bottom, mPaint);
		}

		for(double point = minPoint % step == 0 ? minPoint : // The left bound magically should be a grid line!
				minPoint + (step - (minPoint % step)); // Move to the first grid line
					point <= maxPoint; // Go right up to the maximum point
					point += step // Move along by the specified amount each time
				) {
			// Get the drawing co-ordinate for this line: get the distance it should be in value from the left, scale
			// that to the drawing distance, and move it away from the origin co-ordinate
			pointCoord = originPointCoord + (scale * (point - minPoint));

			Log.v(TAG, "Drawing fixed-gap "+(axis == Axis.X ? "vertical" : "horizontal")+" line at value "+point+", drawing co-ordinate "+pointCoord);

			if(axis == Axis.X) {
				// Draw a vertical line at this x value
				canvas.drawLine(pointCoord.floatValue(), mGridBounds.top, pointCoord.floatValue(), mGridBounds.bottom, mPaint);
				// And the text label
				if(mBottomLabelAdapter != null)
					canvas.drawText(mBottomLabelAdapter.getLabel(point),
							pointCoord.floatValue(), // centre of the text below the grid line
							mGridBounds.bottom+mLabelTextSize, // right below the grid line
							mTextPaint);
				if(mTopLabelAdapter != null)
					canvas.drawText(mTopLabelAdapter.getLabel(point),
							pointCoord.floatValue(), // centre of the text above the grid line
							mLabelTextSize, // put it right at the top of the view
							mTextPaint);
			} else {
				// Draw a horizontal line at this y-value
				canvas.drawLine(mGridBounds.left, pointCoord.floatValue(), mGridBounds.right, pointCoord.floatValue(), mPaint);
				// And the text label
				if(mLeftLabelAdapter != null)
					canvas.drawText(mLeftLabelAdapter.getLabel(point),
							mLeftLabelWidth/2, // centre it in the left label gutter
							pointCoord.floatValue()+(mLabelTextSize/2), // since the text is drawn from the middle-bottom we need to push it down a little more
							mTextPaint);
				if(mTopLabelAdapter != null)
					canvas.drawText(mTopLabelAdapter.getLabel(point),
							mGridBounds.right+(mRightLabelWidth/2), // centre it in the right label gutter
							pointCoord.floatValue()+(mLabelTextSize/2), // centre of the text next to the line
							mTextPaint);
			}
		}
	}

	// Draw a grid with lines at each end and a fixed number of them in between
	private void drawGridFixedEnds(Canvas canvas, Axis axis, float scale) {
		mPaint.setColor(mGridLineColor);
		mPaint.setStrokeWidth(mGridLineWidth);

		final float step = axis == Axis.X ?
				mGridBounds.width() / (float) (mGridLinesHorizontal + 1) : //
				mGridBounds.height() / (float) (mGridLinesVertical + 1);

		final float left = mGridBounds.left;
		final float top = mGridBounds.top;
		final float bottom = mGridBounds.bottom;
		final float right = mGridBounds.right;

		if(axis == Axis.X)
			for (int i = 0; i < mGridLinesHorizontal + 2; i++) {
				Log.v(TAG, "Drawing vertical line at "+(((step*i)/scale)+mValueBounds.left));
				canvas.drawLine(left + (step * i), top, left + (step * i), bottom, mPaint);
			}
		else
			for (int i = 0; i < mGridLinesVertical + 2; i++) {
				Log.v(TAG, "Drawing horizontal line at "+(((step*i)/scale)+mValueBounds.top));
				canvas.drawLine(left, top + (step * i), right, top + (step * i), mPaint);
			}
	}

	// Draw all labels
	private void drawLabels() {
		if (mLeftLabelAdapter != null)
			drawLabels(mLeftLabelAdapter, mLeftLabelLayout, true);

		if (mTopLabelAdapter != null)
			drawLabels(mTopLabelAdapter, mTopLabelLayout, false);

		if (mRightLabelAdapter != null)
			drawLabels(mRightLabelAdapter, mRightLabelLayout, true);

		if (mBottomLabelAdapter != null)
			drawLabels(mBottomLabelAdapter, mBottomLabelLayout, false);
	}

	// Label drawing for a specific axis
	private void drawLabels(LabelAdapter labelAdapter, LinearLayout labelLayout, boolean isSide) {
		// Add views from adapter
		final int labelCount = labelAdapter.getCount();
		int i;
		for (i = 0; i < labelCount; i++) {
			View view = labelLayout.getChildAt(i); // Get the existing label

			if (view == null) { // Create a new label
				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(isSide ? LayoutParams.MATCH_PARENT : 0, isSide ? 0 : LayoutParams.MATCH_PARENT);
				if (i == 0 || i == labelCount - 1)
					params.weight = 0.5f;
				else
					params.weight = 1;

				view = labelAdapter.getView(isSide ? (labelCount - 1) - i : i, view, labelLayout);
				view.setLayoutParams(params);

				labelLayout.addView(view);
			} else // Adapt the existing one
				labelAdapter.getView((labelCount - 1) - i, view, labelLayout);
		}

		// Remove extra views
		while(i < labelLayout.getChildCount())
			labelLayout.removeViewAt(i);
	}

}