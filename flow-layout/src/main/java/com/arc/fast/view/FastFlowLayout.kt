package com.arc.fast.view


import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arc.fast.flowlayout.R
import kotlin.math.max
import kotlin.math.min

/**
 * 流布局
 * 可实现精准控制间距和大小、最大行数、展开缩小等功能
 */
open class FastFlowLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    // true允许流动。false将所有子视图限制在一行中。默认值为true
    private var mFlow = DEFAULT_FLOW

    // 子视图之间的水平间距。auto、固定大小。默认值为 0dp。
    private var mChildSpacing = DEFAULT_CHILD_SPACING
    private var mMinChildSpacing = DEFAULT_CHILD_SPACING

    // 最后一行的子视图之间的水平间距。auto、align、固定大小。如果没有设置，childSpacing将被使用。
    private var mChildSpacingForLastRow = DEFAULT_CHILD_SPACING_FOR_LAST_ROW

    // 行之间的垂直间距。auto、固定大小。默认值为 0dp。
    private var mRowSpacing = DEFAULT_ROW_SPACING
    private var mAdjustedRowSpacing = DEFAULT_ROW_SPACING

    // true从右到左布局子视图。false从左到右布局。默认值为false.
    private var mRtl = DEFAULT_RTL

    // 收缩时最大行数
    private var mShrinkRows = DEFAULT_MAX_ROWS

    // 展开时最大行数
    private var mExpandRows = DEFAULT_MAX_ROWS

    // 展开后显示收缩view
    private var mEnableShrinkView = true

    private var mGravity = UNSPECIFIED_GRAVITY
    private var mRowVerticalGravity = ROW_VERTICAL_GRAVITY_AUTO
    private var mExactMeasuredHeight = 0

    // 保存每一行布局时所需的数据
    private val mHorizontalSpacingForRow by lazy { ArrayList<Float>() }
    private val mHeightForRow by lazy { ArrayList<Int>() }
    private val mWidthForRow by lazy { ArrayList<Int>() }
    private val mChildIndexForRow by lazy { ArrayList<ArrayList<Int>>() }
    private val mChildWidths by lazy { HashMap<Int, Int>() }


    /**
     * true 允许流动
     * false 将所有子视图限制在一行中
     * 默认值为true
     */
    var isFlow: Boolean
        get() = mFlow
        set(flow) {
            mFlow = flow
            requestLayout()
        }

    /**
     * 子视图之间的水平间距。
     * auto、固定大小。
     * 默认值为 0dp。
     */
    var childSpacing: Int
        get() = mChildSpacing
        set(childSpacing) {
            mChildSpacing = childSpacing
            requestLayout()
        }

    /**
     * 最后一行的子视图之间的水平间距。
     * auto、align、固定大小。
     * 如果没有设置，childSpacing将被使用。
     */
    var childSpacingForLastRow: Int
        get() = mChildSpacingForLastRow
        set(childSpacingForLastRow) {
            mChildSpacingForLastRow = childSpacingForLastRow
            requestLayout()
        }

    /**
     * 行之间的垂直间距。
     * auto、固定大小。
     * 默认值为 0dp。
     */
    var rowSpacing: Float
        get() = mRowSpacing
        set(rowSpacing) {
            mRowSpacing = rowSpacing
            requestLayout()
        }

    /**
     * 展开时最大行数
     */
    var expandRows: Int
        get() = mExpandRows
        set(value) {
            mExpandRows = value
            if (isExpand)
                requestLayout()
        }

    /**
     * 收缩时最大行数
     */
    var shrinkRows: Int
        get() = mShrinkRows
        set(value) {
            mShrinkRows = value
            if (!isExpand)
                requestLayout()
        }

    /**
     * 展开后显示收缩view
     */
    var enableShrinkView: Boolean
        get() = mEnableShrinkView
        set(value) {
            mEnableShrinkView = value
            if (isExpand) requestLayout()
        }


    /**
     * 是否展开全部
     */
    var isExpand = false
        set(value) {
            field = value
            requestLayout()
        }

    /**
     * 设置重力
     */
    fun setGravity(gravity: Int) {
        if (mGravity != gravity) {
            mGravity = gravity
            requestLayout()
        }
    }

    /**
     * 设置垂直重力
     */
    fun setRowVerticalGravity(rowVerticalGravity: Int) {
        if (mRowVerticalGravity != rowVerticalGravity) {
            mRowVerticalGravity = rowVerticalGravity
            requestLayout()
        }
    }

    /**
     * 是否从右到左布局子视图
     */
    var isRtl: Boolean
        get() = mRtl
        set(rtl) {
            mRtl = rtl
            requestLayout()
        }

    /**
     * 最小子间距
     */
    var minChildSpacing: Int
        get() = mMinChildSpacing
        set(minChildSpacing) {
            mMinChildSpacing = minChildSpacing
            requestLayout()
        }

    /**
     * 当前行数
     */
    val rowsCount: Int
        get() = mChildIndexForRow.size

    /**
     * 适配器
     */
    var adapter: FastFlowAdapter<*>? = null
        set(value) {
            field = value
            field?.fastFlowLayout = this
            field?.notifyDataSetChanged()
        }

    // 是否要刷新view
    internal var isRefreshView = false

    init {
        if (!isInEditMode) {
            val attributes = context.theme.obtainStyledAttributes(
                attrs, R.styleable.FastFlowLayout, 0, 0
            )
            try {
                mFlow =
                    attributes.getBoolean(
                        R.styleable.FastFlowLayout_fastFlowLayout_flow,
                        DEFAULT_FLOW
                    )
                mChildSpacing = getDimensionOrInt(
                    attributes,
                    R.styleable.FastFlowLayout_fastFlowLayout_childSpacing,
                    DEFAULT_CHILD_SPACING.dp
                )
                mMinChildSpacing = getDimensionOrInt(
                    attributes,
                    R.styleable.FastFlowLayout_fastFlowLayout_minChildSpacing,
                    DEFAULT_CHILD_SPACING.dp
                )
                mChildSpacingForLastRow = getDimensionOrInt(
                    attributes,
                    R.styleable.FastFlowLayout_fastFlowLayout_childSpacingForLastRow,
                    SPACING_UNDEFINED
                )
                mRowSpacing = getDimensionOrInt(
                    attributes,
                    R.styleable.FastFlowLayout_fastFlowLayout_rowSpacing,
                    DEFAULT_ROW_SPACING.dp
                ).toFloat()
                mExpandRows = attributes.getInt(
                    R.styleable.FastFlowLayout_fastFlowLayout_expandRows,
                    DEFAULT_MAX_ROWS
                )
                mShrinkRows = attributes.getInt(
                    R.styleable.FastFlowLayout_fastFlowLayout_shrinkRows,
                    DEFAULT_MAX_ROWS
                )
                mEnableShrinkView = attributes.getBoolean(
                    R.styleable.FastFlowLayout_fastFlowLayout_enableShrinkView,
                    true
                )
                mRtl = attributes.getBoolean(
                    R.styleable.FastFlowLayout_fastFlowLayout_rtl,
                    DEFAULT_RTL
                )
                mGravity =
                    attributes.getInt(
                        R.styleable.FastFlowLayout_android_gravity,
                        UNSPECIFIED_GRAVITY
                    )
                mRowVerticalGravity =
                    attributes.getInt(
                        R.styleable.FastFlowLayout_fastFlowLayout_rowVerticalGravity,
                        ROW_VERTICAL_GRAVITY_AUTO
                    )
            } finally {
                attributes.recycle()
            }
        }
    }

    // 查找子项所在的行
    fun findRowByChildIndex(childIndex: Int): Int {
        if (childIndex < 0 || childIndex >= childCount) return -1
        mChildIndexForRow.forEachIndexed { row, childs ->
            childs.forEach {
                if (it == childIndex) return row
            }
        }
        return -1
    }

    // 预添加view，但不计算实际的布局
    fun addPreView(child: View?) {
        isRefreshView = false
        super.addView(child)
    }

    // 刷新view
    fun refreshView() {
        isRefreshView = true
        requestLayout()
    }

    private fun getDimensionOrInt(typedArray: TypedArray, index: Int, defValue: Int): Int {
        val typedValue = TypedValue()
        typedArray.getValue(index, typedValue)
        return if (typedValue.type == TypedValue.TYPE_DIMENSION) {
            typedArray.getDimensionPixelSize(index, defValue)
        } else {
            typedArray.getInt(index, defValue)
        }
    }

    private fun getChildSize(
        child: View?,
        heightUsed: Int,
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ): FastFlowLayoutChildSize? {
        if (child == null) {
            return null
        }
        val childParams = child.layoutParams
        var horizontalMargin = 0
        var verticalMargin = 0
        if (childParams is MarginLayoutParams) {
            measureChildWithMargins(
                child,
                widthMeasureSpec,
                0,
                heightMeasureSpec,
                heightUsed
            )
            horizontalMargin = childParams.leftMargin + childParams.rightMargin
            verticalMargin = childParams.topMargin + childParams.bottomMargin
        } else {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
        return FastFlowLayoutChildSize(
            width = child.measuredWidth + horizontalMargin,
            height = child.measuredHeight + verticalMargin
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!isRefreshView || isInEditMode) {
            super.onMeasure(0, 0)
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 获取宽度高度
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        // 初始化数据
        mHorizontalSpacingForRow.clear()
        mHeightForRow.clear()
        mWidthForRow.clear()
        mChildIndexForRow.clear()
        mChildWidths.clear()
        var measuredHeight = 0
        var measuredWidth = 0
        // 当前行已使用的宽度
        var rowWidth = 0
        //当前行已使用的宽度（不含间距）
        var rowTotalChildWidth = 0
        // 当前行最大的子项高度
        var maxChildHeightInRow = 0
        // 当前行的子项数量
        var childNumInRow = 0
        // 每行可用的宽度
        val rowSize = widthSize - paddingLeft - paddingRight
        // 是否启用流
        val allowFlow = widthMode != MeasureSpec.UNSPECIFIED && mFlow
        // 每行的子项间距
        val childSpacing =
            if (mChildSpacing == SPACING_AUTO && widthMode == MeasureSpec.UNSPECIFIED) 0 else mChildSpacing
        val tmpSpacing =
            if (childSpacing == SPACING_AUTO) mMinChildSpacing.toFloat() else childSpacing.toFloat()
        // 扩展按钮
        val toggleView = findViewWithTag<View>(EXPAND_VIEW_TAG)
        var existToggleView = false
        val enableToggleView =
            toggleView != null && !(mShrinkRows == DEFAULT_MAX_ROWS || (isExpand && !enableShrinkView) || mShrinkRows >= mExpandRows)
        var expandSize: FastFlowLayoutChildSize? = null
        // 当前循环下标
        var forIndex = 0
        val childCount = childCount
        var isForFinish = false
        // 当前最大行数
        val currentMaxRows = if (isExpand) mExpandRows else mShrinkRows
        // 循环所有子项
        while (forIndex < childCount) {
            if (isForFinish) break
            var childIndex = forIndex
            val child = getChildAt(childIndex)
            if (child == toggleView) {
                if (existToggleView || !enableToggleView) break
                if (mShrinkRows == DEFAULT_MAX_ROWS) break
                if (!isExpand && mChildIndexForRow.size <= mShrinkRows) break
            }
            if (child.visibility == GONE) {
                forIndex++
                continue
            }
            // 计算子项的宽度与高度
            val childSize =
                getChildSize(child, measuredHeight, widthMeasureSpec, heightMeasureSpec)
            if (childSize == null) {
                forIndex++
                continue
            }
            var childWidth = childSize.width
            var childHeight = childSize.height

            // 检查是否需要换行
            var isNextRow = false
            if (allowFlow) {
                var currentRow = mHorizontalSpacingForRow.size + 1
                if (childIndex != 0 && currentRow == currentMaxRows - 1 && rowWidth + childWidth >= rowSize) {
                    currentRow++
                    isNextRow = true
                }
                if (currentMaxRows != DEFAULT_MAX_ROWS &&
                    currentRow == currentMaxRows &&
                    toggleView != null &&
                    !existToggleView &&
                    enableToggleView
                ) {
                    val currentRowWidth = if (isNextRow) 0 else rowWidth
                    // 到达收缩的最后一行，判断是否需要显示展开view
                    if (expandSize == null) {
                        expandSize = getChildSize(
                            toggleView,
                            measuredHeight,
                            widthMeasureSpec,
                            heightMeasureSpec
                        )!!
                    }

                    if (currentRowWidth + childWidth + tmpSpacing > (rowSize - expandSize.width)) {
                        if (childIndex == childCount - 2 && currentRowWidth + childWidth <= rowSize) {
                            // 若刚好是最后一个项则直接结束，不需要显示展开view
                            isForFinish = true
                        } else {
                            if (isNextRow) {
                                childWidth = (childWidth - expandSize.width - tmpSpacing).toInt()
                            } else {
                                // 显示展开view
                                childWidth = expandSize.width
                                childHeight = expandSize.height
                                childIndex = indexOfChild(toggleView)
                                existToggleView = true
                            }
                        }
                    }
                } else if (childIndex != 0) {
                    isNextRow = rowWidth + childWidth >= rowSize
                }
            }

            if (isNextRow) {
                // 需要换行
                mHorizontalSpacingForRow.add(
                    getSpacingForRow(
                        childSpacing,
                        rowSize,
                        rowTotalChildWidth,
                        childNumInRow
                    )
                )
                mHeightForRow.add(maxChildHeightInRow)
                mWidthForRow.add(rowWidth - tmpSpacing.toInt())
                if (mHorizontalSpacingForRow.size <= currentMaxRows) {
                    measuredHeight += maxChildHeightInRow
                }
                measuredWidth = max(measuredWidth, rowWidth)

                childNumInRow = 1
                rowWidth = childWidth + tmpSpacing.toInt()
                rowTotalChildWidth = childWidth
                maxChildHeightInRow = childHeight
            } else {
                // 不需要换行
                childNumInRow++
                rowWidth += (childWidth + tmpSpacing).toInt()
                rowTotalChildWidth += childWidth
                maxChildHeightInRow = max(maxChildHeightInRow, childHeight)
            }

            // 保存每一行的子项下标
            val row = mHeightForRow.size
            val childIndexs = mChildIndexForRow.getOrNull(row)
            if (childIndexs != null) {
                childIndexs.add(childIndex)
            } else {
                mChildIndexForRow.add(arrayListOf(childIndex))
            }

            // 保存每一个子项的宽度
            mChildWidths[childIndex] = childWidth

            // 循环累加下标
            if (childIndex == forIndex) {
                forIndex++
            }
        }

        // 计算间距
        if (mChildSpacingForLastRow == SPACING_ALIGN) {
            if (mHorizontalSpacingForRow.size >= 1) {
                mHorizontalSpacingForRow.add(
                    mHorizontalSpacingForRow[mHorizontalSpacingForRow.size - 1]
                )
            } else {
                mHorizontalSpacingForRow.add(
                    getSpacingForRow(childSpacing, rowSize, rowTotalChildWidth, childNumInRow)
                )
            }
        } else if (mChildSpacingForLastRow != SPACING_UNDEFINED) {
            mHorizontalSpacingForRow.add(
                getSpacingForRow(
                    mChildSpacingForLastRow,
                    rowSize,
                    rowTotalChildWidth,
                    childNumInRow
                )
            )
        } else {
            mHorizontalSpacingForRow.add(
                getSpacingForRow(childSpacing, rowSize, rowTotalChildWidth, childNumInRow)
            )
        }
        mHeightForRow.add(maxChildHeightInRow)
        mWidthForRow.add(rowWidth - tmpSpacing.toInt())
        if (mHorizontalSpacingForRow.size <= currentMaxRows) {
            measuredHeight += maxChildHeightInRow
        }
        measuredWidth = max(measuredWidth, rowWidth)
        measuredWidth = if (childSpacing == SPACING_AUTO) {
            widthSize
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            measuredWidth + paddingLeft + paddingRight
        } else {
            min(measuredWidth + paddingLeft + paddingRight, widthSize)
        }
        measuredHeight += paddingTop + paddingBottom
        val rowNum = min(mHorizontalSpacingForRow.size, currentMaxRows)
        val rowSpacing: Float =
            if (mRowSpacing == SPACING_AUTO.toFloat() && heightMode == MeasureSpec.UNSPECIFIED) 0f else mRowSpacing
        if (rowSpacing == SPACING_AUTO.toFloat()) {
            mAdjustedRowSpacing = if (rowNum > 1) {
                ((heightSize - measuredHeight) / (rowNum - 1)).toFloat()
            } else {
                0f
            }
            measuredHeight = heightSize
        } else {
            mAdjustedRowSpacing = rowSpacing
            if (rowNum > 1) {
                measuredHeight =
                    if (heightMode == MeasureSpec.UNSPECIFIED) (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)).toInt() else min(
                        (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)).toInt(),
                        heightSize
                    )
            }
        }
        mExactMeasuredHeight = measuredHeight
        measuredWidth = if (widthMode == MeasureSpec.EXACTLY) widthSize else measuredWidth
        measuredHeight = if (heightMode == MeasureSpec.EXACTLY) heightSize else measuredHeight
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (!isRefreshView) {
            return
        }
        val paddingLeft = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        var x = if (mRtl) width - paddingRight else paddingLeft
        var y = paddingTop
        val verticalGravity = mGravity and Gravity.VERTICAL_GRAVITY_MASK
        val horizontalGravity = mGravity and Gravity.HORIZONTAL_GRAVITY_MASK
        when (verticalGravity) {
            Gravity.CENTER_VERTICAL -> {
                val offset = (b - t - paddingTop - paddingBottom - mExactMeasuredHeight) / 2
                y += offset
            }
            Gravity.BOTTOM -> {
                val offset = b - t - paddingTop - paddingBottom - mExactMeasuredHeight
                y += offset
            }
            else -> {}
        }
        val horizontalPadding = paddingLeft + paddingRight
        val layoutWidth = r - l
        x += getHorizontalGravityOffsetForRow(horizontalGravity, layoutWidth, horizontalPadding, 0)
        val verticalRowGravity = mRowVerticalGravity and Gravity.VERTICAL_GRAVITY_MASK
        val rowCount = mHeightForRow.size
        var childs = ArrayList<Int>()
        val currentMaxRows = if (isExpand) mExpandRows else mShrinkRows
        // 循环布局每一行的子项
        for (row in 0 until min(rowCount, currentMaxRows)) {
            val rowHeight = mHeightForRow[row]
            val spacing = mHorizontalSpacingForRow[row]
            var i = 0
            mChildIndexForRow.getOrNull(row)?.forEach {
                if (it >= childCount) return@forEach
                val child = getChildAt(it)
                if (child.visibility == GONE) {
                    return@forEach
                } else {
                    childs.add(it)
                    i++
                }
                val childParams = child.layoutParams
                var marginLeft = 0
                var marginTop = 0
                var marginBottom = 0
                var marginRight = 0
                if (childParams is MarginLayoutParams) {
                    val marginParams = childParams
                    marginLeft = marginParams.leftMargin
                    marginRight = marginParams.rightMargin
                    marginTop = marginParams.topMargin
                    marginBottom = marginParams.bottomMargin
                }
//                val childWidth = child.measuredWidth
                val childWidth = mChildWidths[it] ?: child.measuredWidth
                val childHeight = child.measuredHeight
                var tt = y + marginTop
                if (verticalRowGravity == Gravity.BOTTOM) {
                    tt = y + rowHeight - marginBottom - childHeight
                } else if (verticalRowGravity == Gravity.CENTER_VERTICAL) {
                    tt = y + marginTop + (rowHeight - marginTop - marginBottom - childHeight) / 2
                }
                val bb = tt + childHeight
                if (mRtl) {
                    val l1 = x - marginRight - childWidth
                    val r1 = x - marginRight
                    child.layout(l1, tt, r1, bb)
                    x -= (childWidth + spacing + marginLeft + marginRight).toInt()
                } else {
                    val l2 = x + marginLeft
                    val r2 = x + marginLeft + childWidth
                    child.layout(l2, tt, r2, bb)
                    x += (childWidth + spacing + marginLeft + marginRight).toInt()
                }
            }
            x = if (mRtl) width - paddingRight else paddingLeft
            x += getHorizontalGravityOffsetForRow(
                horizontalGravity, layoutWidth, horizontalPadding, row + 1
            )
            y += (rowHeight + mAdjustedRowSpacing).toInt()
        }
        // 布局不显示的子项
        for (i in 0 until childCount) {
            if (childs.contains(i)) continue
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                child.layout(0, 0, 0, 0)
            }
        }
    }

    private fun getHorizontalGravityOffsetForRow(
        horizontalGravity: Int,
        parentWidth: Int,
        horizontalPadding: Int,
        row: Int
    ): Int {
        if (mChildSpacing == SPACING_AUTO || row >= mWidthForRow.size || row >= mChildIndexForRow.size || mChildIndexForRow[row].size <= 0) {
            return 0
        }
        var offset = 0
        when (horizontalGravity) {
            Gravity.CENTER_HORIZONTAL -> offset =
                (parentWidth - horizontalPadding - mWidthForRow[row]) / 2
            Gravity.RIGHT -> offset = parentWidth - horizontalPadding - mWidthForRow[row]
            else -> {}
        }
        return offset
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    private fun getSpacingForRow(
        spacingAttribute: Int,
        rowSize: Int,
        usedSize: Int,
        childNum: Int
    ): Float = if (spacingAttribute == SPACING_AUTO) {
        if (childNum > 1) {
            ((rowSize - usedSize) / (childNum - 1)).toFloat()
        } else {
            0f
        }
    } else {
        spacingAttribute.toFloat()
    }


    private val Float.dp: Int
        get() {
            return (context.resources.displayMetrics.density * this).toInt()
        }
    private val Int.dp: Int
        get() {
            return (context.resources.displayMetrics.density * this).toInt()
        }

    companion object {
        const val SPACING_AUTO = -65536
        const val SPACING_ALIGN = -65537
        const val SPACING_UNDEFINED = -65538
        const val UNSPECIFIED_GRAVITY = -1
        const val ROW_VERTICAL_GRAVITY_AUTO = -65536
        const val DEFAULT_FLOW = true
        const val DEFAULT_CHILD_SPACING = 0
        const val DEFAULT_CHILD_SPACING_FOR_LAST_ROW = SPACING_UNDEFINED
        const val DEFAULT_ROW_SPACING = 0f
        const val DEFAULT_RTL = false
        const val DEFAULT_MAX_ROWS = Int.MAX_VALUE
        internal const val EXPAND_VIEW_TAG = "EXPAND_VIEW_TAG"
    }
}

data class FastFlowLayoutChildSize(val width: Int, val height: Int)


/**
 * 流布局适配器
 */
open class FastFlowAdapter<Data> @JvmOverloads constructor(
    var data: MutableList<Data>? = null,
    var onCreateItem: (layoutInflater: LayoutInflater, parent: FastFlowLayout, item: Data, position: Int) -> View,
    var onItemClick: ((layout: View, item: Data, position: Int) -> Unit)? = null,
    var expandRes: Int? = null,
    var onCreateExpand: ((layoutInflater: LayoutInflater, parent: FastFlowLayout) -> View)? = null,
    var onExpand: ((expand: View, isExpand: Boolean) -> Boolean)? = null
) {
    internal var fastFlowLayout: FastFlowLayout? = null

    constructor(
        layoutRes: Int,
        data: MutableList<Data>? = null,
        convert: (layout: View, item: Data, position: Int) -> Unit,
        onItemClick: ((layout: View, item: Data, position: Int) -> Unit)? = null,
        expandRes: Int? = null,
        onCreateExpand: ((layoutInflater: LayoutInflater, parent: FastFlowLayout) -> View)? = null,
        onExpand: ((expand: View, isExpand: Boolean) -> Boolean)? = null
    ) : this(
        data = data,
        onCreateItem = { layoutInflater, parent, item, position ->
            val layout = layoutInflater.inflate(layoutRes, parent, false)
            convert(layout, item, position)
            layout
        },
        onItemClick = onItemClick,
        expandRes = expandRes,
        onCreateExpand = onCreateExpand,
        onExpand = onExpand
    )

    fun notifyDataSetChanged() {
        val fastFlowLayout = fastFlowLayout ?: return
        fastFlowLayout.isRefreshView = false
        fastFlowLayout.removeAllViews()
        val layoutInflater = LayoutInflater.from(fastFlowLayout.context)
        data?.forEachIndexed { index, item ->
            val layout = onCreateItem(layoutInflater, fastFlowLayout, item, index)
            if (onItemClick != null) {
                layout.setOnClickListener { _ ->
                    onItemClick?.invoke(layout, item, index)
                }
            }
            fastFlowLayout.addPreView(layout)
        }
        if ((expandRes != null || onCreateExpand != null) && fastFlowLayout.shrinkRows != FastFlowLayout.DEFAULT_MAX_ROWS && fastFlowLayout.expandRows > fastFlowLayout.shrinkRows) {
            val expand = onCreateExpand?.invoke(layoutInflater, fastFlowLayout)
                ?: layoutInflater.inflate(expandRes!!, fastFlowLayout, false)
            expand.tag = FastFlowLayout.EXPAND_VIEW_TAG
            expand.setOnClickListener {
                val isExpand = !fastFlowLayout.isExpand
                if (onExpand?.invoke(expand, isExpand) != true) {
                    expand.isSelected = isExpand
                    fastFlowLayout.isExpand = isExpand
                }
            }
            fastFlowLayout.addPreView(expand)
            onExpand?.invoke(expand, false)
        }
        fastFlowLayout.refreshView()
    }
}