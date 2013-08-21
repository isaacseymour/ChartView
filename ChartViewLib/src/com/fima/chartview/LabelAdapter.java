package com.fima.chartview;

import android.widget.BaseAdapter;

public abstract class LabelAdapter extends BaseAdapter {
	private Double[] mValues;

	void setValues(Double[] points) {
		mValues = points;
	}

	@Override
	public int getCount() {
		return mValues.length;
	}

	public Double getItem(int position) {
		return mValues[position];
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public abstract String getLabel(double position);
}