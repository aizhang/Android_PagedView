package com.ai.pagedview;

import com.doodleapp.pagedview.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

/**
 * 
 * @author Isaiah Cheung
 *
 */
public class PagedViewIndicator extends View implements PagedViewListener,
		AnimationListener {
	private static final int INVALID_INDEX = -1;

	Animation indicatorFadeOut;
	Animation indicatorFadeIn;

	private PagedView mPagedView;

	private int mPageCount;
	private int mActivePageIndex = INVALID_INDEX;
	private int mDestinationPageIndex = INVALID_INDEX;;
	private int mPressedPageIndex = INVALID_INDEX;;

	private Paint mPaint;

	private Drawable mIndicatorNormal;
	private Drawable mIndicatorActive;
	private Drawable mIndicatorPressed;

	protected int mIndicatorWidth = -1;
	protected int mIndicatorHeight = -1;
	protected int mIndicatorPadding = 0;

	public PagedViewIndicator(Context context) {
		this(context, null);
	}

	public PagedViewIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.PagedViewIndicator);

		mIndicatorWidth = a.getDimensionPixelSize(
				R.styleable.PagedViewIndicator_indicator_width, -1);
		mIndicatorHeight = a.getDimensionPixelSize(
				R.styleable.PagedViewIndicator_indicator_height, -1);
		mIndicatorPadding = a.getDimensionPixelOffset(
				R.styleable.PagedViewIndicator_indicator_padding, 0);

		mIndicatorNormal = a
				.getDrawable(R.styleable.PagedViewIndicator_indicator_normal_bg);
		mIndicatorActive = a
				.getDrawable(R.styleable.PagedViewIndicator_indicator_active_bg);
		mIndicatorPressed = a
				.getDrawable(R.styleable.PagedViewIndicator_indicator_pressed_bg);

		if (mIndicatorWidth == -1 && null != mIndicatorNormal) {
			mIndicatorWidth = mIndicatorNormal.getIntrinsicWidth();
		}
		if (mIndicatorHeight == -1 && null != mIndicatorNormal) {
			mIndicatorHeight = mIndicatorNormal.getIntrinsicHeight();
		}

		a.recycle();

		mPaint = new Paint();
	}

	public void setPagedView(PagedView pagedView) {
		mPagedView = pagedView;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		int paddingTop = getPaddingTop();
		int paddingBottom = getPaddingBottom();
		if (heightMode == MeasureSpec.UNSPECIFIED
				|| heightMode == MeasureSpec.AT_MOST) {
			heightSize = mIndicatorHeight + paddingBottom + paddingTop;
			setMeasuredDimension(widthSize, heightSize);
		} else {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		mPaint.setTextSize((float) (0.7 * (bottom - top)));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (mPageCount <= 1 || mIndicatorNormal == null
				|| mIndicatorActive == null || mIndicatorPressed == null) {
			return;
		}
		int width = getWidth();
		int totalWidth = mPageCount * mIndicatorWidth + (mPageCount - 1)
				* mIndicatorPadding;
		int height = getHeight();
		int totalHeight = mIndicatorHeight;
		float scaleX = (float) width / (float) totalWidth;

		int indicatorWidth = mIndicatorWidth;
		int indicatorPadding = mIndicatorPadding;
		if (scaleX < 1) {
			indicatorWidth = (int) (mIndicatorWidth * scaleX);
			indicatorPadding = (int) (mIndicatorPadding * scaleX);
			totalWidth = mPageCount * indicatorWidth + (mPageCount - 1)
					* indicatorPadding;
		}

		int startX = (width - totalWidth) / 2;
		int startY = (height - totalHeight) / 2;
		int endX = startX + indicatorWidth;
		int endY = startY + mIndicatorHeight;

		int firstX = startX;
		for (int i = 0; i < mPageCount; i++) {
			Drawable drawable = null;
			// if (i == mActivePageIndex) {
			// // mIndicatorNormal.getConstantState().newDrawable(res);
			// drawable = mIndicatorActive;
			// } else if (i == mPressedPageIndex) {
			// drawable = mIndicatorPressed;
			// } else {
			drawable = mIndicatorNormal;
			// }
			drawable.setBounds(startX, startY, endX, endY);
			drawable.draw(canvas);

			startX += (indicatorPadding + indicatorWidth);
			endX = startX + indicatorWidth;
		}

		int lastX = startX - (indicatorPadding + indicatorWidth);

		float pageWidth = (float) (mPagedView.getScaledMeasuredWidth(mPagedView
				.getCurrentPage()) + mPagedView.getHorizontalSpacing());
		float pageHeight = (float) (mPagedView
				.getScaledMeasuredHeight(mPagedView.getCurrentPage()) + mPagedView
				.getVerticalSpacing());
		float percent = mPagedView.getIsHorizontal() ? (float) ((mPagedView
				.getScrollX() % pageWidth) / pageWidth) : (float) ((mPagedView
				.getScrollY() % pageHeight) / pageHeight);
		int pageIndex = mPagedView.getIsHorizontal() ? (int) ((mPagedView
				.getScrollX() + pageWidth / 2) / pageWidth)
				: (int) ((mPagedView.getScrollY() + pageHeight / 2)/ pageHeight);
//		int leftX = (int) ((width - totalWidth) / 2
//				+ (pageIndex * (indicatorPadding + indicatorWidth)) + percent
//				* indicatorWidth);
		int leftX = (int) ((width - totalWidth) / 2
				+ (pageIndex * (indicatorPadding + indicatorWidth)));
		if (leftX < firstX) {
			leftX = firstX;
		} else if (leftX > lastX) {
			leftX = lastX;
		}
		mIndicatorActive.setBounds(leftX, startY, leftX + indicatorWidth,
				startY + mIndicatorHeight);
		mIndicatorActive.draw(canvas);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int) event.getX();
		int y = (int) event.getY();
		int index = getIndex(x, y);
		if (mPagedView != null && index >= 0 && index < mPageCount) {
			mPagedView.snapToPage(index);
		}
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
		case MotionEvent.ACTION_MOVE:
			mPressedPageIndex = index;
			break;
		case MotionEvent.ACTION_UP:
			mPressedPageIndex = INVALID_INDEX;
		}
		invalidate();
		// return super.onTouchEvent(event);
		return true;
	}

	private int getIndex(int x, int y) {
		int index = INVALID_INDEX;

		int width = getWidth();
		int totalWidth = mPageCount * mIndicatorWidth + (mPageCount - 1)
				* mIndicatorPadding;
		float scaleX = (float) width / (float) totalWidth;

		int indicatorWidth = mIndicatorWidth;
		int indicatorPadding = mIndicatorPadding;
		if (scaleX < 1) {
			indicatorWidth = (int) (mIndicatorWidth * scaleX);
			indicatorPadding = (int) (mIndicatorPadding * scaleX);
			totalWidth = mPageCount * indicatorWidth + (mPageCount - 1)
					* indicatorPadding;
		}

		int startX = (width - totalWidth) / 2;

		index = (x - startX) / (indicatorWidth + indicatorPadding);

		return index;
	}

	private int[] getXY(int index) {
		int[] XY = new int[2];

		int width = getWidth();
		int totalWidth = mPageCount * mIndicatorWidth + (mPageCount - 1)
				* mIndicatorPadding;
		int height = getHeight();
		int totalHeight = mIndicatorHeight;

		int startX = (width - totalWidth) / 2 + index
				* (mIndicatorWidth + mIndicatorPadding);
		int startY = (height - totalHeight) / 2;

		XY[0] = startX;
		XY[1] = startY;

		return XY;
	}

	@Override
	public void onSetToPage(int curPage, int destPage) {
		mActivePageIndex = destPage;
		invalidate();
	}

	@Override
	public void onScrollToPage(int curPage, int destPage) {
		mActivePageIndex = curPage;
		mDestinationPageIndex = destPage;

		invalidate();
	}

	public void onPageCountChanged(int pages) {
		mPageCount = pages;
		if (mPagedView != null)
			mActivePageIndex = mPagedView.getCurrentPage();
		// if(mPageCount <= 1) {
		// setVisibility(View.GONE);
		// } else {
		// setVisibility(View.VISIBLE);
		// }
		//
		invalidate();
	}

	public void fadeIn() {
		if (indicatorFadeIn == null)
			indicatorFadeIn = AnimationUtils.loadAnimation(getContext(),
					R.anim.fade_in_fast);
		setVisibility(View.VISIBLE);
		startAnimation(indicatorFadeIn);
	}

	public void fadeOut() {
		if (indicatorFadeOut == null) {
			indicatorFadeOut = AnimationUtils.loadAnimation(getContext(),
					R.anim.fade_out_fast);
			indicatorFadeOut.setAnimationListener(this);
		}
		startAnimation(indicatorFadeOut);
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (indicatorFadeOut == animation)
			setVisibility(View.INVISIBLE);
	}

	public void onAnimationRepeat(Animation animation) {

	}

	public void onAnimationStart(Animation animation) {
		if (indicatorFadeOut == animation)
			setVisibility(View.VISIBLE);
	}

	public void setIndicatorNormal(Drawable d) {
		mIndicatorNormal = d;
		mIndicatorHeight = d.getIntrinsicHeight();
		mIndicatorWidth = d.getIntrinsicWidth();
		requestLayout();
		invalidate();
	}

	public void setIndicatorActive(Drawable d) {
		mIndicatorActive = d;
	}

	public void setIndicatorPressed(Drawable d) {
		mIndicatorPressed = d;
	}
}
