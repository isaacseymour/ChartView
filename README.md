ChartView
=========
ChartView is a subclass of RelativeLayout which renders data by drawing `AbstractSeries` of `AbstractPoints`. As well as the general 
abstract versions of these classes, this library includes common implementations - `LinearSeries` and `LinearPoint`. The following 
example is a screenshot of the ChartViewExample app, which uses a `LinearSeries` of points drawing a sin curve

![ChartView](http://i.imgur.com/tyXQw.png)

Options
-------
As well as the standard `android:...` XML properties, ChartView has the following XML properties:
* `gridLineColor`: defines the colour of the background grid (default: black)
* `gridLineWidth`: thickness of grid lines (default: 1px)
* Either:
** `gridLinesHorizontal`: the number of horizontal grid lines to draw (default: 5)
** `gridLinesYGap`: if positive, a horizontal grid line is drawn at every y-value which is an integer multiple of this (default: -1)
* Either:
** `gridLinesVertical`: the number of vertical grid lines to draw (default: 5)
** `gridLinesXGap`: if positive, a vertical grid line is drawn at every x-value which is an integer multiple of this (default: -1)
* `leftLabelWidth`: space left on the left to draw axis labels
* `topLabelHeight`: space left at the top to draw axis labels
* `rightLabelWidth`: space left on the right to draw axis labels
* `bottomLabelHeight`: space left at the bottom to draw axis labels
* `labelTextColor`: if using `gridLinesXGap` or `gridLinesYGap`, axis labels will be drawn using this color (default: black)
* `labelTextSize`: if using `gridLinesXGap` or `gridLinesYGap`, axis labels will be drawn at this size

These all have corresponding setters and getters in the `ChartView` class.

Axis Labels
-----------
We also have four methods for controlling which axis labels to draw: `set[Left/Right/Top/Bottom]LabelAdapter(LabelAdapter adapter)`. You'll 
need to subclass `LabelAdapter` to pass to these methods. `LabelAdapter` is a subclass of `android.widget.BaseAdapter`, so should be 
relatively familiar, but there's a few things to note:

* The underlying values are `Double`s for each label.
* When implementing the `getView(int position, View convertView, ViewGroup parent)` method, note that this will only be called if you are 
using `gridLinesVertical` or `gridLinesHorizontal`. It is recommended that you set the `Gravity` to pull the first and last entries to the left/right 
or top/bottom
* When implementing the `getLabel(double item)` method, note that it is only used if you are using `gridLinesXGap` or `gridLinesYGap`. Its argument 
will be a raw value which should just be formatted as a `String`

Data Series
-----------
Data is stored in a subclass of AbstractSeries, which mostly just adds some convenient drawing helpers to a `SortedSet<AbstractPoint>`. There are 
getters for:
* the `SortedSet` of points
* min/max x/y values, and x/y range

and setters for:
* replacing all points in the series (`setPoints(Collection<? extends AbstractPoint>)`)
* adding a single point (`addPoint(AbstractPoint)`)
* the line color and width to be used.

If you're subclassing this, you can access the protected fields and overwrite the protected methods, which are:
* `mPaint`, which is used for drawing this series (and therefore respects `setLineWidth` and `setLineColor`)
* `drawPoint(Canvas, AbstractPoint, float scaleX, float scaleY, Rect)`: Draw the AbstractPoint onto the given Canvas. The scale variables define the 
ratio between distances between point values, and distances between point drawing co-ordinates, and the Rect defines the rectangle which is to be draw 
within on the screen
* `onDrawingComplete`

If you need to subclass `AbstractPoint`, you should note that it should implement `Comparable<AbstractPoint>` if your changes break the default implementation, 
so that the `SortedSet<AbstractPoint>` knows what order to draw points in.

For most cases, `LinearSeries` should be perfectly good. This draws the points from left to right, joining them with straight lines