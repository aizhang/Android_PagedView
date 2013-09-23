package com.ai.pagedview;

/**
 * Copyright 2013 Isaiah(2013)

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.doodleapp.pagedview.R;

/**
 * 
 * @author Isaiah Cheung
 *
 */
public class PagedView extends ViewGroup implements
		ViewGroup.OnHierarchyChangeListener {
	protected static final String TAG = "PagedView";

	protected static final float RETURN_TO_ORIGINAL_PAGE_THRESHOLD = 0.33f;
	// The page is moved more than halfway, automatically move to the next page
	// on touch up.
	protected static final float SIGNIFICANT_MOVE_THRESHOLD = 0.4f;
	// the min drag distance for a fling to register, to prevent random page
	// shifts
	protected static final int MIN_LENGTH_FOR_FLING = 25;
	protected static final int ENSURE_CAPACITY = 32;

	protected final static int TOUCH_STATE_REST = 0;
	protected final static int TOUCH_STATE_SCROLLING = 1;
	protected final static int TOUCH_STATE_PREV_PAGE = 2;
	protected final static int TOUCH_STATE_NEXT_PAGE = 3;

	public static final int PAGE_SNAP_ANIMATION_DURATION = 300;
	public static final int MAX_PAGE_SNAP_DURATION = 750;
	protected static final int SLOW_PAGE_SNAP_ANIMATION_DURATION = 950;
	protected static final float NANOTIME_DIV = 1000000000.0f;

	protected static final int FLING_THRESHOLD_VELOCITY = 500;
	protected static final int MIN_SNAP_VELOCITY = 1500;
	protected static final int MIN_FLING_VELOCITY = 250;

	protected static final int INVALID_POINTER = -1;
	public static final int INVALID_INDEX = -2;
	protected static final int AUTOMATIC_PAGE_SPACING = -1;

	public static final boolean ALLOW_LONG_PRESS_DEFAULT = true;
	public static final boolean ALLOW_SCROLL_DEFAULT = true;
	public static final boolean AUTO_HEIGHT_DEFAULT = false;
	public static final boolean ALLOW_CIRCULATE = false;

	protected Scroller mScroller;

	protected int mCurrentPage;
	protected int mNextPage = INVALID_INDEX;
	protected int mDefaultPage = 0;

	protected float mDensity;
	protected float mSmoothingTime;

	protected int mFlingThresholdVelocity;
	protected int mMinFlingVelocity;
	protected int mMinSnapVelocity;

	protected int mTouchState = TOUCH_STATE_REST;
	protected boolean mScrolling = false;
	protected boolean mForceScreenScrolled = false;

	protected int mActivePointerId = INVALID_POINTER;

	protected boolean mHorizontalMode = true;
	protected boolean mAllowLongPress;
	protected boolean mAllowScroll = ALLOW_SCROLL_DEFAULT;
	protected boolean mAutoHeight = AUTO_HEIGHT_DEFAULT;
	protected boolean mAllowCirculate = ALLOW_CIRCULATE;
	// If true, the subclass should directly update scrollX itself in its
	// computeScroll method
	protected boolean mDeferScrollUpdate = false;
	// If set, will defer loading associated pages until the scrolling settles
	protected boolean mDeferLoadAssociatedPagesUntilScrollCompletes;
	protected boolean mCenterPagesVertically = false;
	protected boolean mAllowOverScroll = true;

	protected int mTouchSlop;
	protected int mMaximumVelocity;
	protected int mPageSpacing = AUTOMATIC_PAGE_SPACING;

	protected int mUnboundedScrollX, mUnboundedScrollY;
	protected float mDownMotionX, mDownMotionY;
	protected float mLastMotionX, mLastMotionY;
	protected float mTouchX, mTouchY;
	protected float mTotalMotionX, mTotalMotionY;
	// protected float mLastMotionXRemainder, mLastMotionYRemainder;

	protected int mSnapDuration = PAGE_SNAP_ANIMATION_DURATION;
	protected PagedViewListener mPagedViewListener;

	protected boolean mAutoHideIndicator = false;
	// Everything about Page Moving
	protected boolean mIsPageMoving = false;

	// parameter that adjusts the layout to be optimized for pages with that
	// scale factor
	protected float mLayoutScale = 1.0f;

	// mOverScrollX is equal to getScrollX() when we're within the normal scroll
	// range. Otherwise
	// it is equal to the scaled overscroll position. We use a separate value so
	// as to prevent
	// the screens from continuing to translate beyond the normal bounds.
	protected int mOverScrollX;
	protected int mMaxScrollX;
	protected int mOverScrollY;
	protected int mMaxScrollY;
	protected static final float OVERSCROLL_ACCELERATE_FACTOR = 2;
	protected static final float OVERSCROLL_DAMP_FACTOR = 0.14f;

	public PagedView(Context context) {
		this(context, null);
	}

	public PagedView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PagedView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		getAttrFromXml(context, attrs, defStyle);
		init();
	}

	/** Initializes various states for this workspace. */
	@SuppressLint("NewApi")
	protected void init() {
		// mScroller = new Scroller(getContext());
		mScroller = new Scroller(getContext(), new ScrollInterpolator());
		// mScroller = new Scroller(getContext(), new AccelerateInterpolator());

		mCenterPagesVertically = true;

		final ViewConfiguration configuration = ViewConfiguration
				.get(getContext());
		mTouchSlop = configuration.getScaledTouchSlop();
		mPagingTouchSlop = configuration.getScaledPagingTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		mDensity = getResources().getDisplayMetrics().density;

		mFlingThresholdVelocity = (int) (FLING_THRESHOLD_VELOCITY);
		mMinFlingVelocity = (int) (MIN_FLING_VELOCITY);
		mMinSnapVelocity = (int) (MIN_SNAP_VELOCITY);
		setHapticFeedbackEnabled(false);
		setOnHierarchyChangeListener(this);
	}

	protected void getAttrFromXml(Context context, AttributeSet attrs,
			int defStyle) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.PagedView, defStyle, 0);

		mAllowScroll = a.getBoolean(R.styleable.PagedView_pagedview_scrollable,
				ALLOW_SCROLL_DEFAULT);
		mAllowLongPress = a.getBoolean(
				R.styleable.PagedView_pagedview_long_pressable,
				ALLOW_LONG_PRESS_DEFAULT);
		mAutoHeight = a.getBoolean(R.styleable.PagedView_pagedview_auto_height,
				AUTO_HEIGHT_DEFAULT);
		mAllowCirculate = a.getBoolean(
				R.styleable.PagedView_pagedview_circular, ALLOW_CIRCULATE);
		mHorizontalMode = a.getBoolean(
				R.styleable.PagedView_pagedview_horizontal, true);
		mCurrentPage = a.getInteger(
				R.styleable.PagedView_pagedview_default_page, 0);

		mPageSpacing = a.getDimensionPixelSize(
				R.styleable.PagedView_pagedview_page_spacing, 0);

		a.recycle();
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		final int contentHeight = getMeasuredHeight()
				- (getPaddingTop() + getPaddingBottom());

		final int childCount = getChildCount();
		int childPaddingLeft = getRelativeChildPaddingLeft(0);
		int childPaddingTop = getRelativeChildPaddingTop(0);

		for (int i = 0; i < childCount; i++) {
			final View child = getChildAt(i);
			if (child.getVisibility() != View.GONE) {
				final int childWidth = child.getMeasuredWidth();
				final int childHeight = child.getMeasuredHeight();

				int childTop = getPaddingTop();

				if (mHorizontalMode) {
					if (mCenterPagesVertically) {
						childTop += (contentHeight - getScaledMeasuredHeight(child)) / 2;
					}
					child.layout(childPaddingLeft, childTop, childPaddingLeft
							+ childWidth, childTop + childHeight);
					childPaddingLeft += getScaledMeasuredWidth(child)
							+ mPageSpacing;
				} else {
					if (mCenterPagesVertically) {
						childTop += (contentHeight - getScaledMeasuredHeight(child)) / 2;
					}
					child.layout(childPaddingLeft, childPaddingTop,
							childPaddingLeft + childWidth, childPaddingTop
									+ childHeight);
					childPaddingTop += getScaledMeasuredHeight(child)
							+ mPageSpacing;
				}
			}
		}
		updateCurrentPageScroll();

		for (int i = 0; i < getChildCount(); i++) {
			View child = getChildAt(i);
			child.setDrawingCacheEnabled(false);
		}
	}

	@Override
	public void addView(View child, int index, LayoutParams params) {
		index = Math.min(getChildCount(), index);
		super.addView(child, index, params);
		child.setDrawingCacheEnabled(false);
		// child.buildDrawingCache(false);
		// child.getDrawingCache(false);
		onPageCountChanged(getPageCount());
	}

	@Override
	public void removeView(View view) {
		super.removeView(view);
		onPageCountChanged(getPageCount());
	}

	@Override
	public void onChildViewAdded(View parent, View child) {
		// This ensures that when children are added, they get the correct
		// transforms / alphas
		// in accordance with any scroll effects.
		mForceScreenScrolled = true;
		invalidate();
	}

	@Override
	public void onChildViewRemoved(View parent, View child) {
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);

		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		// Return early if we aren't given a proper dimension
		if (widthSize <= 0 || heightSize <= 0) {
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			return;
		}

		/*
		 * Allow the height to be set as WRAP_CONTENT. This allows the
		 * particular case of the All apps view on XLarge displays to not take
		 * up more space then it needs. Width is still not allowed to be set as
		 * WRAP_CONTENT since many parts of the code expect each page to have
		 * the same width.
		 */

		final int verticalPadding = getPaddingTop() + getPaddingBottom();
		final int horizontalPadding = getPaddingLeft() + getPaddingRight();

		final int childCount = getChildCount();

		int maxChildWidth = childCount >= 0 ? 0 : widthSize;
		int maxChildHeight = childCount >= 0 ? 0 : heightSize;

		if (mScrollIndicator != null)
			mScrollIndicator.onPageCountChanged(childCount);

		for (int i = 0; i < childCount; i++) {
			// disallowing padding in paged view (just pass 0)
			final View child = getChildAt(i);
			if (child != null) {
				measureChild(child, widthMeasureSpec, heightMeasureSpec);
				if (mHorizontalMode) {
					maxChildHeight = Math.max(maxChildHeight,
							child.getMeasuredHeight());

				} else {
					maxChildWidth = Math.max(maxChildWidth,
							child.getMeasuredWidth());
				}
			}
		}

		if (mHorizontalMode) {
			if (heightMode == MeasureSpec.AT_MOST)
				heightSize = maxChildHeight + horizontalPadding;
		} else {
			if (widthMode == MeasureSpec.AT_MOST)
				widthSize = maxChildWidth + verticalPadding;
		}

		setMeasuredDimension(widthSize, heightSize);

		if (childCount > 0) {
			// Calculate the variable page spacing if necessary
			if (mPageSpacing == AUTOMATIC_PAGE_SPACING) {
				// The gap between pages in the PagedView should be equal to the
				// gap from the page
				// to the edge of the screen (so it is not visible in the
				// current screen). To
				// account for unequal padding on each side of the paged view,
				// we take the maximum
				// of the left/right gap and use that as the gap between each
				// page.
				if (mHorizontalMode) {
					int offset = getRelativeChildPaddingLeft(0);
					View view = getChildAt(0) == null ? this : getChildAt(0);
					int spacing = Math.max(offset,
							widthSize - offset - view.getMeasuredWidth());
					if (spacing < 0) {
						spacing = 0;
					}
					setPageSpacing(spacing);
				} else {
					int offset = getRelativeChildPaddingTop(0);
					View view = getChildAt(0) == null ? this : getChildAt(0);
					int spacing = Math.max(offset,
							heightSize - offset - view.getMeasuredHeight());
					if (spacing < 0) {
						spacing = 0;
					}
					setPageSpacing(spacing);
				}
			}
		}

		if (getPageCount() <= mCurrentPage) {
			mCurrentPage = getPageCount() - 1;
		}
		if (mHorizontalMode) {
			scrollTo(mCurrentPage * getMeasuredWidth(), 0);
		} else {
			scrollTo(0, mCurrentPage * getMeasuredHeight());
		}

		final int pageCount = getPageCount();
		onPageCountChanged(pageCount);

		if (pageCount > 0) {
			if (mHorizontalMode) {
				mMaxScrollX = getChildWidthOffset(pageCount - 1)
						- getRelativeChildPaddingLeft(pageCount - 1)
						+ getWidth();
			} else {
				mMaxScrollY = getChildHeightOffset(pageCount - 1)
						- getRelativeChildPaddingTop(pageCount - 1)
						+ getHeight();
			}
		} else {
			mMaxScrollX = 0;
			mMaxScrollY = 0;
		}
	}

	public void setAutoHideIndicator(boolean autoHideIndicator) {
		mAutoHideIndicator = autoHideIndicator;
	}

	public void autoShowIndicator() {
		if (!mAutoHideIndicator)
			return;

		mScrollIndicator.fadeIn();
	}

	public void autoHideIndicator() {
		if (!mAutoHideIndicator)
			return;

		mScrollIndicator.fadeOut();
	}

	public void setSnapDuration(int duration) {
		mSnapDuration = duration;
	}

	@Override
	public void scrollBy(int x, int y) {
		scrollTo(mUnboundedScrollX + x, mUnboundedScrollY + y);
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);

		mUnboundedScrollX = x;
		mUnboundedScrollY = y;

		if (getIsHorizontal()) {
			if (x < 0 && !isAllowCirculate()) {
				// super.scrollTo(0, y);
				if (mAllowOverScroll) {
					overScroll(x);
				}
			} else if (x > mMaxScrollX && !isAllowCirculate()) {
				// super.scrollTo(mMaxScrollX, y);
				if (mAllowOverScroll) {
					overScroll(x - mMaxScrollX);
				}
			} else {
				mOverScrollX = x;
				// super.scrollTo(x, y);
			}
		} else {
			if (y < 0 && !isAllowCirculate()) {
				// super.scrollTo(x, 0);
				if (mAllowOverScroll) {
					overScroll(y);
				}
			} else if (y > mMaxScrollY && !isAllowCirculate()) {
				// super.scrollTo(x, mMaxScrollY);
				if (mAllowOverScroll) {
					overScroll(y - mMaxScrollY);
				}
			} else {
				mOverScrollY = y;
				// super.scrollTo(x, y);
			}
		}

		mTouchX = x;
		mTouchY = y;
		mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

		if (mScrollIndicator != null)
			mScrollIndicator.invalidate();
	}

	// we moved this functionality to a helper function so SmoothPagedView can
	// reuse it
	protected boolean computeScrollHelper() {
		if (mScroller.computeScrollOffset()) {
			// Don't bother scrolling if the page does not need to be moved
			if (getScrollX() != mScroller.getCurrX()
					|| getScrollY() != mScroller.getCurrY()
					|| mOverScrollX != mScroller.getCurrX()) {
				scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			}
			invalidate();
			return true;
		} else if (mNextPage != INVALID_INDEX) {
			if (!isAllowCirculate()) {
				mCurrentPage = Math.max(0,
						Math.min(getPageCount() - 1, mNextPage));
			} else {
				if (mNextPage < 0) {
					mCurrentPage = getPageCount() - 1;
				} else if (mNextPage >= getPageCount()) {
					mCurrentPage = 0;
				} else {
					mCurrentPage = mNextPage;
				}
			}
			// mCurrPage = Math.max(0, Math.min(mNextPage, getChildCount() -
			// 1));
			mNextPage = INVALID_INDEX;

			// Load the associated pages if necessary
			if (mDeferLoadAssociatedPagesUntilScrollCompletes) {
				loadAssociatedPages(mCurrentPage);
				mDeferLoadAssociatedPagesUntilScrollCompletes = false;
			}

			// We don't want to trigger a page end moving unless the page has
			// settled
			// and the user has stopped scrolling
			if (mTouchState == TOUCH_STATE_REST) {
				pageEndMoving();
				setCurrPage(getCurrentPage());
			}
			// setCurrPage(getCurrPage());
			return true;
		}
		return false;
	}

	@Override
	public void computeScroll() {
		computeScrollHelper();
	}

	public View getCurrentChild() {
		return getChildAt(getCurrentPage()) == null ? null
				: getChildAt(getCurrentPage());
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (!mAllowScroll || getPageCount() <= 0)
			return super.onTouchEvent(ev);
		acquireVelocityTrackerAndAddMovement(ev);

		final int action = ev.getAction();
		switch (action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
				System.out.println("set current page: " + mNextPage);
				setCurrPage(mNextPage);
			}
			// Remember where the motion event started
			mDownMotionX = mLastMotionX = ev.getX();
			mDownMotionY = mLastMotionY = ev.getY();
			// mLastMotionXRemainder = mLastMotionYRemainder = 0;
			mTotalMotionX = mTotalMotionY = 0;
			mActivePointerId = ev.getPointerId(0);
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				pageBeginMoving();
			}

			break;

		case MotionEvent.ACTION_MOVE:
			handleTouchMove(ev);
			mScrolling = true;
			break;

		case MotionEvent.ACTION_UP:
			handleTouchUp(ev);
			mTouchState = TOUCH_STATE_REST;
			mActivePointerId = INVALID_POINTER;
			mScrolling = false;
			releaseVelocityTracker();
			break;

		case MotionEvent.ACTION_CANCEL:
			if (mTouchState == TOUCH_STATE_SCROLLING) {
				snapToDestination();
			}
			mTouchState = TOUCH_STATE_REST;
			mActivePointerId = INVALID_POINTER;
			mScrolling = false;
			releaseVelocityTracker();
			break;

		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}
		return true;
	}

	protected void handleTouchMove(MotionEvent ev) {
		if (mTouchState == TOUCH_STATE_SCROLLING) {
			// Scroll to follow the motion event
			final int pointerIndex = ev.findPointerIndex(mActivePointerId);
			if (pointerIndex == INVALID_POINTER)
				return;

			if (mHorizontalMode) {
				final float x = ev.getX(pointerIndex);
				float deltaX = mLastMotionX - x;
				mTotalMotionX += Math.abs(deltaX);

				// Only scroll and update mLastMotionX if we have moved some
				// discrete amount. We
				// keep the remainder because we are actually testing if we've
				// moved from the last
				// scrolled position (which is discrete).
				if (Math.abs(deltaX) >= 1.0f) {
					if (deltaX > 30)
						deltaX = 30f;
					else if (deltaX < -30) {
						deltaX = -30f;
					}
					scrollBy((int) deltaX, 0);

					// if (isAllowCirculate()) {
					// scrollBy((int) deltaX, 0);
					// } else {
					// if (deltaX < 0) {
					// if (getScrollX() > 0) {
					// scrollBy(Math.max(-getScrollX(), (int) deltaX),
					// 0);
					// }
					// } else if (deltaX > 0) {
					// int availableToScroll = 0;
					// if (null != getChildAt(getChildCount() - 1)) {
					// availableToScroll = getChildAt(
					// getChildCount() - 1).getRight()
					// - getScrollX() - getWidth();
					// } else {
					// availableToScroll = (getPageCount() - 1)
					// * getWidth() - getScrollX()
					// - getWidth();
					// }
					// if (availableToScroll > 0) {
					// scrollBy(Math.min(availableToScroll,
					// (int) deltaX), 0);
					// }
					// }
					// }

					mTouchX += deltaX;
					mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
					mLastMotionX = x;
				} else {
					awakenScrollBars();
				}
			} else {
				final float y = ev.getY(pointerIndex);
				final float deltaY = mLastMotionY - y;

				mTotalMotionY += Math.abs(deltaY);

				if (Math.abs(deltaY) >= 1.0f) {
					scrollBy(0, (int) deltaY);
					// if (isAllowCirculate()) {
					// scrollBy(0, (int) deltaY);
					// } else {
					// if (deltaY < 0) {
					// if (getScrollY() > 0) {
					// scrollBy(0,
					// Math.max(-getScrollY(), (int) deltaY));
					// }
					// } else if (deltaY > 0) {
					// int availableToScroll = 0;
					// if (null != getChildAt(getChildCount() - 1)) {
					// availableToScroll = getChildAt(
					// getChildCount() - 1).getBottom()
					// - getScrollY() - getHeight();
					// } else {
					// availableToScroll = (getPageCount() - 1)
					// * getHeight() - getScrollY()
					// - getHeight();
					// }
					// if (availableToScroll > 0) {
					// scrollBy(0, Math.min(availableToScroll,
					// (int) deltaY));
					// }
					// }
					// }

					mTouchY += deltaY;
					mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
					mLastMotionY = y;
				} else {
					awakenScrollBars();
				}
			}
		} else {
			determineScrollingStart(ev);
		}
	}

	protected void handleTouchUp(MotionEvent ev) {
		if (mTouchState == TOUCH_STATE_SCROLLING) {
			final int activePointerId = mActivePointerId;
			final int pointerIndex = ev.findPointerIndex(activePointerId);
			if (pointerIndex == INVALID_POINTER)
				return;
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);

			if (mHorizontalMode) {
				final float x = ev.getX(pointerIndex);
				final int velocityX = (int) velocityTracker
						.getXVelocity(activePointerId);
				final int deltaX = (int) (x - mDownMotionX);
				final int pageWidth = getScaledMeasuredWidth(getPageAt(mCurrentPage) == null ? this
						: getPageAt(mCurrentPage));
				boolean isSignificantMove = Math.abs(deltaX) > pageWidth
						* SIGNIFICANT_MOVE_THRESHOLD;
				mTotalMotionX += Math.abs(mLastMotionX - x);

				boolean isFling = mTotalMotionX > MIN_LENGTH_FOR_FLING
						&& Math.abs(velocityX) > mFlingThresholdVelocity;

				// In the case that the page is moved far to one direction
				// and
				// then is flung
				// in the opposite direction, we use a threshold to
				// determine
				// whether we should
				// just return to the starting page, or if we should skip
				// one
				// further.
				boolean returnToOriginalPage = false;
				if (Math.abs(deltaX) > pageWidth
						* RETURN_TO_ORIGINAL_PAGE_THRESHOLD
						&& Math.signum(velocityX) != Math.signum(deltaX)
						&& isFling) {
					returnToOriginalPage = true;
				}

				int finalPage;
				// We give flings precedence over large moves, which is why
				// we
				// short-circuit our
				// test for a large move if a fling has been registered.
				// That
				// is, a large
				// move to the left and fling to the right will register as
				// a
				// fling to the right.
				if (((isSignificantMove && deltaX > 0 && !isFling) || (isFling && velocityX > 0))
						&& mCurrentPage >= 0) {
					finalPage = returnToOriginalPage ? mCurrentPage
							: mCurrentPage - 1;
					snapToPageWithVelocity(finalPage, velocityX);
					System.out.println("snap to page: " + finalPage);
					// snapToPage(finalPage);
				} else if (((isSignificantMove && deltaX < 0 && !isFling) || (isFling && velocityX < 0))
						&& mCurrentPage <= getPageCount() - 1) {
					finalPage = returnToOriginalPage ? mCurrentPage
							: mCurrentPage + 1;
					snapToPageWithVelocity(finalPage, velocityX);
					System.out.println("snap to page1: " + finalPage);
					// snapToPage(finalPage);
				} else {
					snapToDestination();
				}
			} else {
				final float y = ev.getY(pointerIndex);
				final int velocityY = (int) velocityTracker
						.getYVelocity(activePointerId);
				final int deltaY = (int) (y - mDownMotionY);
				final int pageHeight = getScaledMeasuredHeight(getPageAt(mCurrentPage) == null ? this
						: getPageAt(mCurrentPage));
				boolean isSignificantMove = Math.abs(deltaY) > pageHeight
						* SIGNIFICANT_MOVE_THRESHOLD;
				mTotalMotionY += Math.abs(mLastMotionY - y);

				boolean isFling = mTotalMotionY > MIN_LENGTH_FOR_FLING
						&& Math.abs(velocityY) > mFlingThresholdVelocity;

				// In the case that the page is moved far to one direction
				// and
				// then is flung
				// in the opposite direction, we use a threshold to
				// determine
				// whether we should
				// just return to the starting page, or if we should skip
				// one
				// further.
				boolean returnToOriginalPage = false;
				if (Math.abs(deltaY) > pageHeight
						* RETURN_TO_ORIGINAL_PAGE_THRESHOLD
						&& Math.signum(velocityY) != Math.signum(deltaY)
						&& isFling) {
					returnToOriginalPage = true;
				}

				int finalPage;
				// We give flings precedence over large moves, which is why
				// we
				// short-circuit our
				// test for a large move if a fling has been registered.
				// That
				// is, a large
				// move to the left and fling to the right will register as
				// a
				// fling to the right.
				if (((isSignificantMove && deltaY > 0 && !isFling) || (isFling && velocityY > 0))
						&& mCurrentPage >= 0) {
					finalPage = returnToOriginalPage ? mCurrentPage
							: mCurrentPage - 1;
					// snapToPageWithVelocity(finalPage, velocityY);
					snapToPage(finalPage);
				} else if (((isSignificantMove && deltaY < 0 && !isFling) || (isFling && velocityY < 0))
						&& mCurrentPage <= getPageCount() - 1) {
					finalPage = returnToOriginalPage ? mCurrentPage
							: mCurrentPage + 1;
					// snapToPageWithVelocity(finalPage, velocityY);
					snapToPage(finalPage);
				} else {
					snapToDestination();
				}
			}

		} else if (mTouchState == TOUCH_STATE_PREV_PAGE) {
			// at this point we have not moved beyond the touch slop
			// (otherwise mTouchState would be TOUCH_STATE_SCROLLING), so
			// we can just page
			int nextPage = Math.max(-1, mCurrentPage - 1);
			if (nextPage != mCurrentPage) {
				snapToPage(nextPage);
			} else {
				snapToDestination();
			}
		} else if (mTouchState == TOUCH_STATE_NEXT_PAGE) {
			// at this point we have not moved beyond the touch slop
			// (otherwise mTouchState would be TOUCH_STATE_SCROLLING), so
			// we can just page
			int nextPage = Math.min(getPageCount(), mCurrentPage + 1);
			if (nextPage != mCurrentPage) {
				snapToPage(nextPage);
			} else {
				snapToDestination();
			}
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		acquireVelocityTrackerAndAddMovement(ev);

		if (!mAllowScroll || getPageCount() <= 0)
			return super.onInterceptTouchEvent(ev);
		/*
		 * This method JUST determines whether we want to intercept the motion.
		 * If we return true, onTouchEvent will be called and we do the actual
		 * scrolling there.
		 */

		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE)
				&& (mTouchState == TOUCH_STATE_SCROLLING)) {
			return true;
		}

		final float x = ev.getX();
		final float y = ev.getY();

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			/*
			 * mIsBeingDragged == false, otherwise the shortcut would have
			 * caught it. Check whether the user has moved far enough from his
			 * original down touch.
			 */
			if (mActivePointerId != INVALID_POINTER) {
				determineScrollingStart(ev);
				break;
			}
			// if mActivePointerId is INVALID_POINTER, then we must have missed
			// an ACTION_DOWN
			// event. in that case, treat the first occurence of a move event as
			// a ACTION_DOWN
			// i.e. fall through to the next case (don't break)
			// (We sometimes miss ACTION_DOWN events in Workspace because it
			// ignores all events
			// while it's small- this was causing a crash before we checked for
			// INVALID_POINTER)
		case MotionEvent.ACTION_DOWN: {
			// Remember location of down touch
			mDownMotionX = x;
			mDownMotionY = y;
			mLastMotionX = x;
			mLastMotionY = y;
			mTotalMotionX = mTotalMotionY = 0;
			mActivePointerId = ev.getPointerId(0);
			mAllowLongPress = true;

			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			final int dist = mHorizontalMode ? Math.abs(mScroller.getFinalX()
					- mScroller.getCurrX()) : Math.abs(mScroller.getFinalY()
					- mScroller.getCurrY());
			final boolean finishedScrolling = (mScroller.isFinished() || dist < mTouchSlop);
			if (finishedScrolling) {
				mTouchState = TOUCH_STATE_REST;
				mScroller.abortAnimation();
				System.out.println("finishedScrolling");
			} else {
				System.out.println("not finishedScrolling");
				mTouchState = TOUCH_STATE_SCROLLING;
			}

			// check if this can be the beginning of a tap on the side of the
			// pages
			// to scroll the current page
			if (mTouchState != TOUCH_STATE_PREV_PAGE
					&& mTouchState != TOUCH_STATE_NEXT_PAGE) {
				if (getPageCount() > 0) {
					if (hitsPreviousPage(x, y)) {
						mTouchState = TOUCH_STATE_PREV_PAGE;
					} else if (hitsNextPage(x, y)) {
						mTouchState = TOUCH_STATE_NEXT_PAGE;
					}
				}
			}
			break;
		}

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			mTouchState = TOUCH_STATE_REST;
			mAllowLongPress = false;
			mActivePointerId = INVALID_POINTER;
			releaseVelocityTracker();
			mScrolling = false;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			releaseVelocityTracker();
			break;
		}
		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag mode.
		 */

		return mTouchState != TOUCH_STATE_REST;
	}

	@Override
	public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
		if (disallowIntercept) {
			// We need to make sure to cancel our long press if
			// a scrollable widget takes over touch events
			final View currentPage = getChildAt(mCurrentPage) == null ? this
					: getChildAt(mCurrentPage);
			currentPage.cancelLongPress();
		}
		super.requestDisallowInterceptTouchEvent(disallowIntercept);
	}

	public boolean isScrolling() {
		return (mTouchState == TOUCH_STATE_SCROLLING);
	}

	protected void onSecondaryPointerUp(MotionEvent ev) {
		final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId(pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			// TODO: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = mDownMotionX = ev.getX(newPointerIndex);
			mLastMotionY = ev.getY(newPointerIndex);
			mActivePointerId = ev.getPointerId(newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	public int getRelativeChildPaddingLeft(int index) {
		View view = getChildAt(0) == null ? this : getChildAt(0);

		final int padding = getPaddingLeft() + getPaddingRight();
		final int offset = getPaddingLeft()
				+ (getMeasuredWidth() - padding - view.getMeasuredWidth()) / 2;
		// final int offset = getPaddingLeft()
		// + (getMeasuredWidth() - padding -
		// getScaledMeasuredWidth(getChildAt(0))
		// ) / 2;
		return offset;
	}

	public int getRelativeChildPaddingTop(int index) {
		View view = getChildAt(0) == null ? this : getChildAt(0);
		final int padding = getPaddingTop() + getPaddingBottom();
		final int offset = getPaddingTop()
				+ (getMeasuredHeight() - padding - view.getMeasuredHeight())
				/ 2;
		return offset;
	}

	public int getScaledMeasuredWidth(View child) {
		if (child == null)
			return getMeasuredWidth();

		final int measuredWidth = child.getMeasuredWidth();
		return mLayoutScale == 1.0f ? measuredWidth : (int) (measuredWidth
				* mLayoutScale + 0.5f);
	}

	public int getScaledMeasuredHeight(View child) {
		if (child == null)
			return getMeasuredHeight();

		final int measuredHeight = child.getMeasuredHeight();
		return mLayoutScale == 1.0f ? measuredHeight : (int) (measuredHeight
				* mLayoutScale + 0.5f);
	}

	public int getChildWidthOffset(int index) {
		int offset = getRelativeChildPaddingLeft(0);
		for (int i = 0; i < index; ++i) {
			offset += getScaledMeasuredWidth(getChildAt(i)) + mPageSpacing;
		}
		if (index < 0) {
			for (int i = 0; i > index; --i) {
				offset -= getScaledMeasuredWidth(getChildAt(0)) + mPageSpacing;
			}
		}
		return offset;
	}

	public int getChildHeightOffset(int index) {
		int offset = getRelativeChildPaddingTop(0);
		for (int i = 0; i < index; ++i) {
			offset += getScaledMeasuredHeight(getChildAt(i)) + mPageSpacing;
		}
		if (index < 0) {
			for (int i = 0; i > index; --i) {
				offset -= getScaledMeasuredHeight(getChildAt(0)) + mPageSpacing;
			}
		}
		return offset;
	}

	/**
	 * Return true if a tap at (x, y) should trigger a flip to the previous
	 * page.
	 */
	protected boolean hitsPreviousPage(float x, float y) {
		if (mHorizontalMode) {
			return (x < getRelativeChildPaddingLeft(mCurrentPage)
					- mPageSpacing);
		} else {
			return (y < getRelativeChildPaddingTop(mCurrentPage) - mPageSpacing);
		}
	}

	/** Return true if a tap at (x, y) should trigger a flip to the next page. */
	protected boolean hitsNextPage(float x, float y) {
		if (mHorizontalMode) {
			return (x > (getMeasuredWidth()
					- getRelativeChildPaddingLeft(mCurrentPage) + mPageSpacing));
		} else {
			return (y > (getMeasuredHeight()
					- getRelativeChildPaddingTop(mCurrentPage) + mPageSpacing));
		}
	}

	/**
	 * Set scale to the {@link PagedView}, notice that the scale will also apply
	 * to the pages. A layout scale of 1.0f assumes that the pages, in their
	 * unshrunken state, have a scale of 1.0f. A layout scale of 0.8f assumes
	 * the pages have a scale of 0.8f, and tightens the layout accordingly
	 * 
	 * @param childrenScale
	 */
	public void setLayoutScale(float childrenScale) {
		mLayoutScale = childrenScale;

		// Trigger a full re-layout (never just call onLayout directly!)
		int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
				MeasureSpec.EXACTLY);
		int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
				MeasureSpec.EXACTLY);
		requestLayout();
		measure(widthSpec, heightSpec);
		layout(getLeft(), getTop(), getRight(), getBottom());
		setCurrPage(mCurrentPage);
	}

	public float getLayoutScale() {
		return mLayoutScale;
	}

	public void setPageSpacing(int pageSpacing) {
		mPageSpacing = pageSpacing;
	}

	public int getPageSpacing() {
		return mPageSpacing < 0 ? 0 : mPageSpacing;
	}

	public void snapToDestination() {
		int destPage = getCurrentPage();
		if (mHorizontalMode) {
			final int screenWidth = getWidth();
			destPage = (getScrollX() + screenWidth / 2) / screenWidth;
		} else {
			final int screenHeight = getHeight();
			destPage = (getScrollY() + screenHeight / 2) / screenHeight;
		}
		snapToPage(destPage);
	}

	protected void snapToPageWithVelocity(int whichPage, int velocity) {
		int delta = 0;
		int duration = 0;
		if (getIsHorizontal()) {
			// whichPage = Math.max(0, Math.min(whichPage, getChildCount() -
			// 1));
			int halfScreenSize = getMeasuredWidth() / 2;

			final int newX = getChildWidthOffset(whichPage)
					- getRelativeChildPaddingLeft(whichPage);
			delta = newX - mUnboundedScrollX;
			duration = 0;

			if (Math.abs(velocity) < mMinFlingVelocity) {
				// If the velocity is low enough, then treat this more as an
				// automatic page advance
				// as opposed to an apparent physical response to flinging
				snapToPage(whichPage, mSnapDuration);
				return;
			}

			// Here we compute a "distance" that will be used in the computation
			// of
			// the overall
			// snap duration. This is a function of the actual distance that
			// needs
			// to be traveled;
			// we keep this value close to half screen size in order to reduce
			// the
			// variance in snap
			// duration as a function of the distance the page needs to travel.
			float distanceRatio = Math.min(1f, 1.0f * Math.abs(delta)
					/ (2 * halfScreenSize));
			float distance = halfScreenSize + halfScreenSize
					* distanceInfluenceForSnapDuration(distanceRatio);

			velocity = Math.abs(velocity);
			velocity = Math.max(mMinSnapVelocity, velocity);

			// we want the page's snap velocity to approximately match the
			// velocity
			// at which the
			// user flings, so we scale the duration by a value near to the
			// derivative of the scroll
			// interpolator at zero, ie. 5. We use 4 to make it a little slower.
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
			duration = Math.min(duration, MAX_PAGE_SNAP_DURATION);
		} else {
			// whichPage = Math.max(0, Math.min(whichPage, getChildCount() -
			// 1));
			int halfScreenSize = getMeasuredHeight() / 2;

			final int newY = getChildHeightOffset(whichPage)
					- getRelativeChildPaddingTop(whichPage);
			delta = newY - mUnboundedScrollY;
			duration = 0;

			if (Math.abs(velocity) < mMinFlingVelocity) {
				// If the velocity is low enough, then treat this more as an
				// automatic page advance
				// as opposed to an apparent physical response to flinging
				snapToPage(whichPage, mSnapDuration);
				return;
			}

			// Here we compute a "distance" that will be used in the computation
			// of
			// the overall
			// snap duration. This is a function of the actual distance that
			// needs
			// to be traveled;
			// we keep this value close to half screen size in order to reduce
			// the
			// variance in snap
			// duration as a function of the distance the page needs to travel.
			float distanceRatio = Math.min(1f, 1.0f * Math.abs(delta)
					/ (2 * halfScreenSize));
			float distance = halfScreenSize + halfScreenSize
					* distanceInfluenceForSnapDuration(distanceRatio);

			velocity = Math.abs(velocity);
			velocity = Math.max(mMinSnapVelocity, velocity);

			// we want the page's snap velocity to approximately match the
			// velocity
			// at which the
			// user flings, so we scale the duration by a value near to the
			// derivative of the scroll
			// interpolator at zero, ie. 5. We use 4 to make it a little slower.
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
			duration = Math.min(duration, MAX_PAGE_SNAP_DURATION);
		}
		snapToPage(whichPage, delta, duration);
	}

	public void snapToPage(int whichPage) {
		snapToPage(whichPage, mSnapDuration);
	}

	protected void snapToPage(int whichPage, int duration) {
		snapToPage(whichPage, 0, duration);
	}

	protected void snapToPage(int whichPage, int delta, int duration) {
		final int childCount = getPageCount();
		if (childCount == 0)
			return;

		if (!isAllowCirculate()) {
			whichPage = Math.max(0, Math.min(getPageCount() - 1, whichPage));
		} else {
			if (whichPage < 0) {
				whichPage = -1;
			} else if (whichPage >= getPageCount()) {
				whichPage = getPageCount();
			}
		}

		if (delta <= 0) {
			if (mHorizontalMode) {
				int newX = getChildWidthOffset(whichPage)
						- getRelativeChildPaddingLeft(whichPage);
				delta = newX - mUnboundedScrollX;

			} else {
				int newY = getChildHeightOffset(whichPage)
						- getRelativeChildPaddingTop(whichPage);
				delta = newY - mUnboundedScrollY;
			}
		}

		int scrollToPage = whichPage;
		if (whichPage >= getPageCount()) {
			scrollToPage = 0;
		} else if (whichPage < 0) {
			scrollToPage = getPageCount() - 1;
		}
		onScrollToPage(getCurrentPage(), scrollToPage);

		mNextPage = whichPage;

		View focusedChild = getFocusedChild();
		if (focusedChild != null && whichPage != mCurrentPage
				&& focusedChild == getChildAt(mCurrentPage)) {
			focusedChild.clearFocus();
		}

		pageBeginMoving();
		awakenScrollBars(duration);
		if (duration == 0) {
			duration = Math.abs(delta);
		}

		if (!mScroller.isFinished())
			mScroller.abortAnimation();
		if (mHorizontalMode) {
			mScroller.startScroll(mUnboundedScrollX, 0, delta, 0, duration);
		} else {
			mScroller.startScroll(0, mUnboundedScrollY, 0, delta, duration);
		}

		// Load associated pages immediately if someone else is handling the
		// scroll, otherwise defer
		// loading associated pages until the scroll settles
		if (mDeferScrollUpdate) {
			loadAssociatedPages(mNextPage);
		} else {
			mDeferLoadAssociatedPagesUntilScrollCompletes = true;
		}
		invalidate();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);

		// if (mAllowCirculate) {
		// canvas.save();
		// canvas.translate((getWidth()+ mPageSpacing) * getChildCount() , 0);
		// drawChild(canvas, getChildAt(0), getDrawingTime());
		// canvas.restore();
		//
		// canvas.save();
		// canvas.translate(-(getWidth()+ mPageSpacing) * getChildCount() , 0);
		// drawChild(canvas, getChildAt(getChildCount() - 1), getDrawingTime());
		// canvas.restore();
		//
		// }
		computeCirculate(canvas);
	}

	protected void computeCirculate(Canvas canvas) {
		if (isAllowCirculate()) {
			canvas.save();

			if (mHorizontalMode)
				canvas.translate(
						-(getWidth() + mPageSpacing) * getChildCount(), 0);
			else
				canvas.translate(0, -(getHeight() + mPageSpacing)
						* getChildCount());
			drawPage(canvas, getPageCount() - 1);

			canvas.restore();

			canvas.save();

			if (mHorizontalMode)
				canvas.translate((getWidth() + mPageSpacing) * getChildCount(),
						0);
			else
				canvas.translate(0, (getHeight() + mPageSpacing)
						* getChildCount());
			drawPage(canvas, 0);
			canvas.restore();
		}
	}

	// We want the duration of the page snap animation to be influenced by the
	// distance that
	// the screen has to travel, however, we don't want this duration to be
	// effected in a
	// purely linear fashion. Instead, we use this method to moderate the effect
	// that the distance
	// of travel has on the overall snap duration.
	float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) Math.sin(f);
	}

	/**
	 * Scroll to the previous page.
	 */
	public void scrollToPrevious() {
		if (mScroller.isFinished()) {
			if (mCurrentPage > 0)
				snapToPage(mCurrentPage - 1);
		} else {
			if (mNextPage > 0)
				snapToPage(mNextPage - 1);
		}
	}

	/**
	 * Scroll to the behind page.
	 */
	public void scrollToBehind() {
		if (mScroller.isFinished()) {
			if (mCurrentPage < getChildCount() - 1)
				snapToPage(mCurrentPage + 1);
		} else {
			if (mNextPage < getChildCount() - 1)
				snapToPage(mNextPage + 1);
		}
	}

	/**
	 * Updates the scroll of the current page immediately to its final scroll
	 * position. We use this in CustomizePagedView to allow tabs to share the
	 * same PagedView while resetting the scroll of the previous tab page.
	 */
	protected void updateCurrentPageScroll() {
		// If the current page is invalid, just reset the scroll position to
		// zero
		if (getIsHorizontal()) {
			int newX = 0;
			if (0 <= mCurrentPage && mCurrentPage < getPageCount()) {
				int offset = getChildWidthOffset(mCurrentPage);
				int relOffset = getRelativeChildPaddingLeft(mCurrentPage);
				newX = offset - relOffset;
			}
			scrollTo(newX, 0);
			mScroller.setFinalX(newX);
			mScroller.forceFinished(true);
		} else {
			int newY = 0;
			if (0 <= mCurrentPage && mCurrentPage < getPageCount()) {
				int offset = getChildHeightOffset(mCurrentPage);
				int relOffset = getRelativeChildPaddingTop(mCurrentPage);
				newY = offset - relOffset;
			}
			scrollTo(0, newY);
			mScroller.setFinalY(newY);
			mScroller.forceFinished(true);
		}
	}

	public int getDefaultPage() {
		return mDefaultPage;
	}

	public void setDefaultPage(int defaultPage) {
		defaultPage = Math.min(getPageCount() - 1, Math.max(0, defaultPage));
		mDefaultPage = defaultPage;
	}

	public void setCurrPage(int whichPage) {
		// if (!mScroller.isFinished()) {
		// mScroller.abortAnimation();
		// }
		// don't introduce any checks like mCurrentPage == currentPage here-- if
		// we change the
		// the default
		int childCount = getPageCount();
		if (childCount == 0) {
			return;
		}

		if (mAllowCirculate) {
			if (whichPage < 0) {
				whichPage = getChildCount() - 1;
			} else if (mNextPage > getChildCount() - 1) {
				whichPage = 0;
			}
			mCurrentPage = whichPage;
		} else {
			mCurrentPage = Math.max(0, Math.min(whichPage, getPageCount() - 1));
		}
		updateCurrentPageScroll();
		updateScrollingIndicator();
		if (mPagedViewListener != null) {
			mPagedViewListener.onSetToPage(-1, whichPage);
		}
		invalidate();
	}

	public int getCurrentPage() {
		return mCurrentPage;
	}

	public int getNextPage() {
		return (mNextPage != INVALID_INDEX) ? mNextPage : mCurrentPage;
	}

	public void snapNext() {
		snapToPage(getCurrentPage() + 1);
	}

	public void snapPrevious() {
		snapToPage(getCurrentPage() - 1);
	}

	public boolean canSnapToNext() {
		int newPage = getCurrentPage() + 1;
		return (newPage < getPageCount() - 1);
	}

	public boolean canSnapToPrevious() {
		int newPage = getCurrentPage() - 1;
		return (newPage >= 0);
	}

	public void enableScroll() {
		mAllowScroll = true;
	}

	public void disableScroll() {
		mAllowScroll = false;
	}

	public boolean getIsHorizontal() {
		return mHorizontalMode;
	}

	public boolean isAllowCirculate() {
		if (getPageCount() <= 1)
			return false;
		return mAllowCirculate;
	}

	public void setAllowCirculate(boolean allowCirculate) {
		mAllowCirculate = allowCirculate;
	}

	public boolean drawPage(Canvas canvas, int pageIndex) {
		int pageSpacing = getPageSpacing();
		View currView = getCurrentChild();
		float scaledWidth = getScaledMeasuredWidth(currView);
		float scaleHeight = getScaledMeasuredHeight(currView);
		int eachWidthSlidingDistance = (int) (scaledWidth + pageSpacing);
		int eachHeightSlidingDistance = (int) (scaleHeight + pageSpacing);
		if (isAllowCirculate()) {
			if (pageIndex < 0) {
				canvas.save();
				if (mHorizontalMode)
					canvas.translate(
							-eachWidthSlidingDistance * getPageCount(), 0);
				else
					canvas.translate(0, -eachHeightSlidingDistance
							* getPageCount());
				drawChild(canvas, getChildAt(getPageCount() + pageIndex),
						getDrawingTime());
				canvas.restore();
			} else if (pageIndex >= getChildCount()) {
				canvas.save();
				if (mHorizontalMode)
					canvas.translate(eachWidthSlidingDistance * getPageCount(),
							0);
				else
					canvas.translate(0, eachHeightSlidingDistance
							* getPageCount());
				drawChild(canvas, getChildAt(pageIndex - getChildCount()),
						getDrawingTime());
				canvas.restore();
			} else {
				if (getChildAt(pageIndex) != null)
					return drawChild(canvas, getChildAt(pageIndex),
							getDrawingTime());
			}
			return true;
		} else {
			if (getChildAt(pageIndex) != null)
				return drawChild(canvas, getChildAt(pageIndex),
						getDrawingTime());
			else
				return false;
		}
	}

	public boolean drawPagePart(Canvas canvas, int pageIndex, int x, int y,
			Rect rect) {
		canvas.clipRect(rect);
		boolean result = drawPage(canvas, pageIndex);
		return result;
	}

	@Override
	public boolean drawChild(Canvas canvas, View child, long drawingTime) {
		return super.drawChild(canvas, child, drawingTime);
	}

	protected void loadAssociatedPages(int page) {
		loadAssociatedPages(page, false);
	}

	protected void loadAssociatedPages(int page, boolean immediateAndOnly) {
	}

	protected static final int TEMP_PAGE_COUNT = 1;

	protected int getAssociatedLowerPageBound(int page) {
		// final int count = getChildCount();
		// return (page - TEMP_PAGE_COUNT + count) % count;
		// return Math.max(0, page - TEMP_PAGE_COUNT);
		return page - TEMP_PAGE_COUNT;
	}

	protected int getAssociatedUpperPageBound(int page) {
		// final int count = getChildCount();
		// return (page + TEMP_PAGE_COUNT + count) % count;
		// return Math.min(page + TEMP_PAGE_COUNT, count - 1);
		return page + TEMP_PAGE_COUNT;
	}

	protected void pageBeginMoving() {
		if (!mIsPageMoving) {
			mIsPageMoving = true;
			onPageBeginMoving();
		}
	}

	protected void pageEndMoving() {
		if (mIsPageMoving) {
			mIsPageMoving = false;
			onPageEndMoving();
		}
	}

	protected boolean isPageMoving() {
		return mIsPageMoving;
	}

	// a method that subclasses can override to add behavior
	protected void onPageBeginMoving() {
	}

	// a method that subclasses can override to add behavior
	protected void onPageEndMoving() {
	}

	protected OnLongClickListener mLongClickListener;

	/**
	 * Registers the specified listener on each page contained in this
	 * workspace.
	 * 
	 * @param l
	 *            The listener used to respond to long clicks.
	 */
	@Override
	public void setOnLongClickListener(OnLongClickListener l) {
		mLongClickListener = l;
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).setOnLongClickListener(l);
		}
	}

	protected void overScroll(float amount) {
		dampedOverScroll(amount);
	}

	protected void dampedOverScroll(float amount) {
		if (getIsHorizontal()) {
			int screenSize = getMeasuredWidth();

			float f = (amount / screenSize);

			if (f == 0)
				return;
			f = f / (Math.abs(f)) * (overScrollInfluenceCurve(Math.abs(f)));

			// Clamp this factor, f, to -1 < f < 1
			if (Math.abs(f) >= 1) {
				f /= Math.abs(f);
			}

			int overScrollAmount = (int) Math.round(OVERSCROLL_DAMP_FACTOR * f
					* screenSize);
			if (amount < 0) {
				mOverScrollX = overScrollAmount;
				// super.scrollTo(0, getScrollY());
			} else {
				mOverScrollX = mMaxScrollX + overScrollAmount;
				// super.scrollTo(mMaxScrollX, getScrollY());
			}
		} else {
			int screenSize = getMeasuredHeight();

			float f = (amount / screenSize);

			if (f == 0)
				return;
			f = f / (Math.abs(f)) * (overScrollInfluenceCurve(Math.abs(f)));

			// Clamp this factor, f, to -1 < f < 1
			if (Math.abs(f) >= 1) {
				f /= Math.abs(f);
			}

			int overScrollAmount = (int) Math.round(OVERSCROLL_DAMP_FACTOR * f
					* screenSize);
			if (amount < 0) {
				mOverScrollY = overScrollAmount;
				// super.scrollTo(getScrollX(), 0);
			} else {
				mOverScrollY = mMaxScrollY + overScrollAmount;
				// super.scrollTo(getScrollX(), mMaxScrollY);
			}
		}
		invalidate();
	}

	// This curve determines how the effect of scrolling over the limits of the
	// page dimishes
	// as the user pulls further and further from the bounds
	protected float overScrollInfluenceCurve(float f) {
		f -= 1.0f;
		return f * f * f + 1.0f;
	}

	protected VelocityTracker mVelocityTracker;

	protected VelocityTracker acquireVelocityTrackerAndAddMovement(
			MotionEvent ev) {
		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);
		return mVelocityTracker;
	}

	protected void releaseVelocityTracker() {
		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	protected PagedViewIndicator mScrollIndicator;

	public PagedViewIndicator findAndSetIndicator(int indicatorResId) {
		ViewGroup parent = (ViewGroup) getParent();
		if (parent == null)
			return null;

		PagedViewIndicator pagedViewIndicator = (PagedViewIndicator) parent
				.findViewById(indicatorResId);
		return setIndicator(pagedViewIndicator);
	}

	public PagedViewIndicator getIndicator() {
		return mScrollIndicator;
	}

	public PagedViewIndicator setIndicator(PagedViewIndicator pagedViewIndicator) {
		mScrollIndicator = pagedViewIndicator;
		mScrollIndicator.setPagedView(this);
		onPageCountChanged(getPageCount());
		return pagedViewIndicator;
	}

	public void onPageCountChanged(int newCount) {
		if (mScrollIndicator != null) {
			mScrollIndicator.onPageCountChanged(newCount);
			updateScrollingIndicatorPosition();
		}
	}

	protected void updateScrollingIndicator() {
		if (getPageCount() <= 1)
			return;

		if (mScrollIndicator != null) {
			updateScrollingIndicatorPosition();
		}
	}

	protected void updateScrollingIndicatorPosition() {
		if (mScrollIndicator != null)
			mScrollIndicator.invalidate();
	}

	protected void onScrollToPage(int cur, int dest) {
		if (mScrollIndicator != null) {
			mScrollIndicator.onScrollToPage(cur, dest);
		}
		if (mPagedViewListener != null) {
			mPagedViewListener.onScrollToPage(cur, dest);
		}
	}

	protected void determineScrollingStart(MotionEvent ev) {
		determineScrollingStart(ev, 1.0f);
	}

	// It true, use a different slop parameter (pagingTouchSlop = 2 * touchSlop)
	// for deciding
	// to switch to a new page
	protected boolean mUsePagingTouchSlop = true;
	protected int mPagingTouchSlop;

	protected void determineScrollingStart(MotionEvent ev, float touchSlopScale) {
		/*
		 * Locally do absolute value. mLastMotionX is set to the y value of the
		 * down event.
		 */
		final int pointerIndex = ev.findPointerIndex(mActivePointerId);
		if (pointerIndex == -1) {
			return;
		}
		final float x = ev.getX(pointerIndex);
		final float y = ev.getY(pointerIndex);

		final int xDiff = (int) Math.abs(x - mLastMotionX);
		final int yDiff = (int) Math.abs(y - mLastMotionY);

		final int touchSlop = Math.round(touchSlopScale * mTouchSlop);
		boolean xPaged = xDiff > mPagingTouchSlop;
		boolean yPaged = yDiff > mPagingTouchSlop;
		boolean xMoved = xDiff > touchSlop;
		boolean yMoved = yDiff > touchSlop;

		if (getIsHorizontal()) {
			if (xMoved || xPaged) {
				if (mUsePagingTouchSlop ? xPaged : xMoved) {
					// Scroll if the user moved far enough along the X axis
					mTouchState = TOUCH_STATE_SCROLLING;
					mTotalMotionX += Math.abs(mLastMotionX - x);
					mLastMotionX = x;
					mTouchX = getScrollX();
					mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
					pageBeginMoving();
				}
				// Either way, cancel any pending longpress
				cancelCurrentPageLongPress();
			}
		} else {
			if (yMoved || yPaged) {
				if (mUsePagingTouchSlop ? yPaged : yMoved) {
					// Scroll if the user moved far enough along the X axis
					mTouchState = TOUCH_STATE_SCROLLING;
					mTotalMotionY += Math.abs(mLastMotionY - y);
					mLastMotionY = y;
					mTouchY = getScrollY();
					mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
					pageBeginMoving();
				}
				// Either way, cancel any pending longpress
				cancelCurrentPageLongPress();
			}
		}
	}

	protected void cancelCurrentPageLongPress() {
		if (mAllowLongPress) {
			mAllowLongPress = false;
			// Try canceling the long press. It could also have been scheduled
			// by a distant descendant, so use the mAllowLongPress flag to block
			// everything
			final View currentPage = getChildAt(mCurrentPage);
			if (currentPage != null) {
				currentPage.cancelLongPress();
			}
		}
	}

	public int getPageCount() {
		return getChildCount();
	}

	public View getPageAt(int position) {
		View v = getChildAt(position);
		if (null == v) {
			return getChildAt(0);
		}
		return getChildAt(position);
	}

	public int getScaledMeasuredWidth(int pageIndex) {
		return getScaledMeasuredWidth(getCurrentChild());
	}

	public int getScaledMeasuredHeight(int pageIndex) {
		return getScaledMeasuredHeight(getCurrentChild());
	}

	public int getHorizontalSpacing() {
		return getPageSpacing();
	}

	public int getVerticalSpacing() {
		return getPageSpacing();
	}

	public void setHorizontalSpacing(int horizontalSpacing) {
		setPageSpacing(horizontalSpacing);
	}

	public void setVerticalSpacing(int verticalSpacing) {
		setPageSpacing(verticalSpacing);
	}

	public void setPagedViewListener(PagedViewListener listener) {
		mPagedViewListener = listener;
	}

	public PagedViewListener getPagedViewListener() {
		return mPagedViewListener;
	}

	private static class ScrollInterpolator implements Interpolator {
		public ScrollInterpolator() {
		}

		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1;
		}
	}
}