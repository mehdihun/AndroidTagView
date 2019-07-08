package co.lujun.androidtagview;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.widget.ViewDragHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import static co.lujun.androidtagview.Utils.dp2px;
import static co.lujun.androidtagview.Utils.sp2px;

/**
 * Author: lujun(http://blog.lujun.co)
 * Date: 2015-12-31 11:47
 */
public class TagView extends View {

    /**
     * Selected status
     **/
    private boolean tagSelected = false;

    /**
     * Border width
     */
    private float mBorderWidth;

    /**
     * Border radius
     */
    private float mBorderRadius;

    /**
     * Text size
     */
    private float mTextSize;

    /**
     * Horizontal padding for this view, include left & right padding(left & right padding are equal
     */
    private int mHorizontalPadding;

    /**
     * Vertical padding for this view, include top & bottom padding(top & bottom padding are equal)
     */
    private int mVerticalPadding;

    /**
     * TagView border color
     */
    private int mBorderColor;

    /**
     * TagView background color
     */
    private int mBackgroundColor;

    /**
     * TagView text color
     */
    private int mTextColor;

    /**
     * TagView selected background color
     */
    private int mSelectedBackgroundColor;

    /**
     * TagView selected text color
     */
    private int mSelectedTextColor;

    /**
     * Whether this view clickable
     */
    private boolean isViewClickable;

    /**
     * The max width for this tag view
     */
    private int mTagMaxWidth;

    /**
     * OnTagClickListener for click action
     */
    private OnTagClickListener mOnTagClickListener;

    /**
     * Move slop(default 5dp)
     */
    private int mMoveSlop = 5;

    /**
     * Scroll slop threshold 4dp
     */
    private int mSlopThreshold = 4;

    /**
     * How long trigger long click callback(default 500ms)
     */
    private int mLongPressTime = 500;

    /**
     * Text direction(support:TEXT_DIRECTION_RTL & TEXT_DIRECTION_LTR, default TEXT_DIRECTION_LTR)
     */
    private int mTextDirection = View.TEXT_DIRECTION_LTR;

    /**
     * The distance between baseline and descent
     */
    private float bdDistance;

    private Paint mPaint, mRipplePaint;

    private RectF mRectF;

    private RectF mRectTagF;

    private String mAbstractText, mOriginText;

    private boolean isUp, isMoved, isExecLongClick;

    private int mLastX, mLastY;

    private float fontH, fontW;

    private float mTouchX, mTouchY;

    /**
     * The ripple effect duration(default 1000ms)
     */
    private int mRippleDuration = 1000;

    private float mRippleRadius;

    private int mRippleColor;

    private int mRippleAlpha;

    private Path mPath;

    private Typeface mTypeface;

    private ValueAnimator mRippleValueAnimator;

    private boolean mEnableCross;

    private float mCrossAreaWidth;

    private float mCrossAreaPadding;

    private int mCrossColor;

    private float mCrossLineWidth;

    /** BADGE **/

    private String mBadgeText = null;

    private int mBadgeBackgroundColor;
    private int mBadgeSelectedBackgroundColor;

    private int mBadgeTextColor;
    private int mBadgeSelectedTextColor;

    private int mBadgeStrokeColor;
    private int mBadgeSelectedStrokeColor;

    private Runnable mLongClickHandle = new Runnable() {
        @Override
        public void run() {
            if (!isMoved && !isUp) {
                int state = ((TagContainerLayout) getParent()).getTagViewState();
                if (state == ViewDragHelper.STATE_IDLE) {
                    isExecLongClick = true;
                    mOnTagClickListener.onTagLongClick((int) getTag(), tagSelected, getText());
                }
            }
        }
    };

    public TagView(Context context, String text) {
        super(context);
        init(context, text);
    }

    private void init(Context context, String text) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRipplePaint.setStyle(Paint.Style.FILL);
        mRectF = new RectF();
        mRectTagF = new RectF();
        mPath = new Path();
        mOriginText = text == null ? "" : text;
        mMoveSlop = (int) dp2px(context, mMoveSlop);
        mSlopThreshold = (int) dp2px(context, mSlopThreshold);
    }

    public boolean isTagSelected() {
        return tagSelected;
    }

    public void setTagSelected(boolean tagSelected) {
        this.tagSelected = tagSelected;
    }

    private void onDealText() {

        if (!TextUtils.isEmpty(mOriginText)) {
            int height = mVerticalPadding * 2 + (int) fontH;
            int maxW = mTagMaxWidth - mHorizontalPadding * 2 - (isEnableCross() ? height : 0);

            // I know, I know... Adding this empty space here is not the most elegant solution.
            // But on the other side, look how wonderful the rest of the do-while loop looks,
            // it wouldn't be possible without this small-little-minor-trick, so let's get over it...
            // AP :)
            mAbstractText = mOriginText + " ";

            do {
                mAbstractText = mAbstractText.substring(0, mAbstractText.length() - 1);
                if(mAbstractText.length() < mOriginText.length() && mAbstractText.length() > 3) {
                    mAbstractText = mAbstractText.substring(0, mAbstractText.length()-4) + "...";
                }

                mPaint.setTypeface(mTypeface);
                mPaint.setTextSize(mTextSize);
                final Paint.FontMetrics fontMetrics = mPaint.getFontMetrics();
                fontH = fontMetrics.descent - fontMetrics.ascent;
                if (mTextDirection == View.TEXT_DIRECTION_RTL) {
                    fontW = 0;
                    for (char c : mAbstractText.toCharArray()) {
                        String sc = String.valueOf(c);
                        fontW += mPaint.measureText(sc);
                    }
                } else {
                    fontW = mPaint.measureText(mAbstractText);
                }
            }
            while (fontW > maxW && mAbstractText.length() > 0);
        } else {
            mAbstractText = "";
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        float outterPadding = dp2px(getContext(), 5);
        float badgeStroke = dp2px(getContext(), 2);

        int combined = (int) outterPadding + (int) badgeStroke;

        int height = mVerticalPadding * 2 + (int) fontH + combined;
        int width = mHorizontalPadding * 2 + (int) fontW + (isEnableCross() ? height : 0);
        mCrossAreaWidth = Math.min(Math.max(mCrossAreaWidth, height), width);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mRectF.set(mBorderWidth, mBorderWidth, w - mBorderWidth, h - mBorderWidth);

        float outterPadding = dp2px(getContext(), 5);
        float badgeStroke = dp2px(getContext(), 2);

        int combined = (int) outterPadding + (int) badgeStroke;

        mRectTagF.set(mBorderWidth, mBorderWidth + combined, w - mBorderWidth - combined, h - mBorderWidth);
    }

    public final int getTagHeight() {
        return (int) mRectTagF.height();
    }

    public final int getTagWidth() {
        return (int) mRectTagF.width();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float outterPadding = dp2px(getContext(), 5);
        float badgeStroke = dp2px(getContext(), 2);
        int combined = (int) outterPadding + (int) badgeStroke;

        // draw background
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(tagSelected ? mSelectedBackgroundColor : mBackgroundColor);
        canvas.drawRoundRect(mRectTagF, mBorderRadius, mBorderRadius, mPaint);

        // draw border
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderWidth);
        mPaint.setColor(mBorderColor);
        canvas.drawRoundRect(mRectTagF, mBorderRadius, mBorderRadius, mPaint);

        // draw ripple for TagView
        drawRipple(canvas);

        // draw text
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(tagSelected ? mSelectedTextColor : mTextColor);

        if (mTextDirection == View.TEXT_DIRECTION_RTL) {
            float tmpX = (isEnableCross() ? getTagWidth() + getTagHeight() : getTagWidth()) / 2 + fontW / 2;
            for (char c : mAbstractText.toCharArray()) {
                String sc = String.valueOf(c);
                tmpX -= mPaint.measureText(sc);
                canvas.drawText(sc, tmpX, getTagHeight() / 2 + fontH / 2 - bdDistance + combined, mPaint);
            }
        } else {
            canvas.drawText(mAbstractText,
                    (isEnableCross() ? getTagWidth() - getTagHeight() : getTagWidth()) / 2 - fontW / 2,
                    getTagHeight() / 2 + fontH / 2 - bdDistance + combined, mPaint);
        }

        // draw cross
        drawCross(canvas);


        //// BADGE

        if(mBadgeText != null && mBadgeText.length() > 0) {

            Rect txtRect = new Rect();
            Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mTextPaint.setTextSize(sp2px(getContext(), 10));
            mTextPaint.getTextBounds(mBadgeText, 0, mBadgeText.length(), txtRect);

            float marginPercentageH = 0.1f;
            float marginPercentageV = 0.1f;
            float badgeWidth = badgeStroke * 2 + txtRect.width() * (1 + marginPercentageH * 2);
            float badgeHeight = badgeStroke * 2 + txtRect.height() * (1 + marginPercentageV * 2);
            float mBadgeBorderRadius = badgeHeight / 2;

            RectF mBadgeRectF = new RectF();
            mBadgeRectF.set(getWidth() - badgeWidth - badgeStroke * 2, badgeStroke, getWidth() - badgeStroke * 2, badgeHeight + badgeStroke);

            // Draw badge background
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(tagSelected ? mBadgeSelectedBackgroundColor : mBadgeBackgroundColor);
            canvas.drawRoundRect(mBadgeRectF, mBadgeBorderRadius, mBadgeBorderRadius, mPaint);

            // Draw badge stroke
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(badgeStroke);
            mPaint.setColor(tagSelected ? mBadgeSelectedStrokeColor : mBadgeStrokeColor);
            canvas.drawRoundRect(mBadgeRectF, mBadgeBorderRadius, mBadgeBorderRadius, mPaint);

            // Draw badge text
            mTextPaint.setStyle(Paint.Style.FILL);
            mTextPaint.setColor(tagSelected ? mBadgeSelectedTextColor : mBadgeTextColor);
            canvas.drawText(mBadgeText, mBadgeRectF.left + badgeStroke + txtRect.width() * marginPercentageH, mBadgeRectF.bottom - badgeStroke - txtRect.height() * marginPercentageV, mTextPaint);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (isViewClickable) {
            int y = (int) event.getY();
            int x = (int) event.getX();
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (getParent() != null) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    mLastY = y;
                    mLastX = x;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(mLastY - y) > mSlopThreshold
                            || Math.abs(mLastX - x) > mSlopThreshold) {
                        if (getParent() != null) {
                            getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        isMoved = true;
                        return false;
                    }
                    break;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            mRippleRadius = 0.0f;
            mTouchX = event.getX();
            mTouchY = event.getY();
        }
        if (isEnableCross() && isClickCrossArea(event) && mOnTagClickListener != null) {
            if (action == MotionEvent.ACTION_DOWN) {
                mOnTagClickListener.onTagCrossClick((int) getTag());
            }
            return true;
        } else if (isViewClickable && mOnTagClickListener != null) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mLastY = y;
                    mLastX = x;
                    isMoved = false;
                    isUp = false;
                    isExecLongClick = false;
                    postDelayed(mLongClickHandle, mLongPressTime);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isMoved) {
                        break;
                    }
                    if (Math.abs(mLastX - x) > mMoveSlop || Math.abs(mLastY - y) > mMoveSlop) {
                        isMoved = true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    isUp = true;
                    if (!isExecLongClick && !isMoved) {

                        splashRipple();
                        tagSelected = mOnTagClickListener.onTagClick((int) getTag(), !tagSelected, getText());
                        invalidate();
                        requestLayout();


                    }
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private boolean isClickCrossArea(MotionEvent event) {
        if (mTextDirection == View.TEXT_DIRECTION_RTL) {
            return event.getX() <= mCrossAreaWidth;
        }
        return event.getX() >= getWidth() - mCrossAreaWidth;
    }

    private void drawCross(Canvas canvas) {
        if (isEnableCross()) {
            mCrossAreaPadding = mCrossAreaPadding > getTagHeight() / 2 ? getTagHeight() / 2 :
                    mCrossAreaPadding;
            int ltX, ltY, rbX, rbY, lbX, lbY, rtX, rtY;
            ltX = mTextDirection == View.TEXT_DIRECTION_RTL ? (int) (mCrossAreaPadding) :
                    (int) (getTagWidth() - getTagHeight() + mCrossAreaPadding);
            ltY = mTextDirection == View.TEXT_DIRECTION_RTL ? (int) (mCrossAreaPadding) :
                    (int) (mCrossAreaPadding);
            lbX = mTextDirection == View.TEXT_DIRECTION_RTL ? (int) (mCrossAreaPadding) :
                    (int) (getTagWidth() - getTagHeight() + mCrossAreaPadding);
            lbY = mTextDirection == View.TEXT_DIRECTION_RTL ?
                    (int) (getTagHeight() - mCrossAreaPadding) : (int) (getTagHeight() - mCrossAreaPadding);
            rtX = mTextDirection == View.TEXT_DIRECTION_RTL ?
                    (int) (getTagHeight() - mCrossAreaPadding) : (int) (getTagWidth() - mCrossAreaPadding);
            rtY = mTextDirection == View.TEXT_DIRECTION_RTL ? (int) (mCrossAreaPadding) :
                    (int) (mCrossAreaPadding);
            rbX = mTextDirection == View.TEXT_DIRECTION_RTL ?
                    (int) (getTagHeight() - mCrossAreaPadding) : (int) (getTagWidth() - mCrossAreaPadding);
            rbY = mTextDirection == View.TEXT_DIRECTION_RTL ?
                    (int) (getTagHeight() - mCrossAreaPadding) : (int) (getTagHeight() - mCrossAreaPadding);

            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(mCrossColor);
            mPaint.setStrokeWidth(mCrossLineWidth);

            float outterPadding = dp2px(getContext(), 5);
            float badgeStroke = dp2px(getContext(), 2);

            int combined = (int) outterPadding + (int) badgeStroke;

            canvas.drawLine(ltX, ltY + combined, rbX, rbY + combined, mPaint);
            canvas.drawLine(lbX, lbY + combined, rtX, rtY + combined, mPaint);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void drawRipple(Canvas canvas) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && canvas != null) {
            canvas.save();
            mPath.reset();

            canvas.clipPath(mPath);
            mPath.addRoundRect(mRectTagF, mBorderRadius, mBorderRadius, Path.Direction.CCW);

            canvas.clipPath(mPath, Region.Op.DIFFERENCE);
            canvas.drawCircle(mTouchX, mTouchY, mRippleRadius, mRipplePaint);
            canvas.restore();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void splashRipple() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && mTouchX > 0 && mTouchY > 0) {
            mRipplePaint.setColor(mRippleColor);
            mRipplePaint.setAlpha(mRippleAlpha);
            final float maxDis = Math.max(Math.max(Math.max(mTouchX, mTouchY),
                    Math.abs(getMeasuredWidth() - mTouchX)), Math.abs(getMeasuredHeight() - mTouchY));

            mRippleValueAnimator = ValueAnimator.ofFloat(0.0f, maxDis).setDuration(mRippleDuration);
            mRippleValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float animValue = (float) animation.getAnimatedValue();
                    mRippleRadius = animValue >= maxDis ? 0 : animValue;
                    postInvalidate();
                }
            });
            mRippleValueAnimator.start();
        }
    }

    public String getText() {
        return mOriginText;
    }

    public boolean getIsViewClickable() {
        return isViewClickable;
    }

    public void setTagMaxWidth(int maxWidth) {
        this.mTagMaxWidth = maxWidth;
        onDealText();
    }

    public void setOnTagClickListener(OnTagClickListener listener) {
        this.mOnTagClickListener = listener;
    }

    public void setTagBackgroundColor(int color) {
        this.mBackgroundColor = color;
    }

    public void setTagBorderColor(int color) {
        this.mBorderColor = color;
    }

    public void setTagTextColor(int color) {
        this.mTextColor = color;
    }

    public void setTagSelectedBackgroundColor(int color) {
        this.mSelectedBackgroundColor = color;
    }

    public void setTagSelectedTextColor(int color) {
        this.mSelectedTextColor = color;
    }

    public void setBorderWidth(float width) {
        this.mBorderWidth = width;
    }

    public void setBorderRadius(float radius) {
        this.mBorderRadius = radius;
    }

    public void setTextSize(float size) {
        this.mTextSize = size;
        onDealText();
    }

    public void setHorizontalPadding(int padding) {
        this.mHorizontalPadding = padding;
    }

    public void setVerticalPadding(int padding) {
        this.mVerticalPadding = padding;
    }

    public void setIsViewClickable(boolean clickable) {
        this.isViewClickable = clickable;
    }

    public interface OnTagClickListener {
        boolean onTagClick(int position, boolean status, String text);

        void onTagLongClick(int position, boolean status, String text);

        void onTagCrossClick(int position);
    }

    public int getTextDirection() {
        return mTextDirection;
    }

    public void setTextDirection(int textDirection) {
        this.mTextDirection = textDirection;
    }

    public void setTypeface(Typeface typeface) {
        this.mTypeface = typeface;
        onDealText();
    }

    public void setRippleAlpha(int mRippleAlpha) {
        this.mRippleAlpha = mRippleAlpha;
    }

    public void setRippleColor(int mRippleColor) {
        this.mRippleColor = mRippleColor;
    }

    public void setRippleDuration(int mRippleDuration) {
        this.mRippleDuration = mRippleDuration;
    }

    public void setBdDistance(float bdDistance) {
        this.bdDistance = bdDistance;
    }

    public boolean isEnableCross() {
        return mEnableCross;
    }

    public void setEnableCross(boolean mEnableCross) {
        this.mEnableCross = mEnableCross;
    }

    public float getCrossAreaWidth() {
        return mCrossAreaWidth;
    }

    public void setCrossAreaWidth(float mCrossAreaWidth) {
        this.mCrossAreaWidth = mCrossAreaWidth;
    }

    public float getCrossLineWidth() {
        return mCrossLineWidth;
    }

    public void setCrossLineWidth(float mCrossLineWidth) {
        this.mCrossLineWidth = mCrossLineWidth;
    }

    public float getCrossAreaPadding() {
        return mCrossAreaPadding;
    }

    public void setCrossAreaPadding(float mCrossAreaPadding) {
        this.mCrossAreaPadding = mCrossAreaPadding;
    }

    public int getCrossColor() {
        return mCrossColor;
    }

    public void setCrossColor(int mCrossColor) {
        this.mCrossColor = mCrossColor;
    }


    public int getBadgeSelectedStrokeColor() {
        return mBadgeSelectedStrokeColor;
    }

    public void setBadgeSelectedStrokeColor(int mBadgeSelectedStrokeColor) {
        this.mBadgeSelectedStrokeColor = mBadgeSelectedStrokeColor;
    }

    public int getBadgeStrokeColor() {
        return mBadgeStrokeColor;
    }

    public void setBadgeStrokeColor(int mBadgeStrokeColor) {
        this.mBadgeStrokeColor = mBadgeStrokeColor;
    }

    public int getBadgeSelectedTextColor() {
        return mBadgeSelectedTextColor;
    }

    public void setBadgeSelectedTextColor(int mBadgeSelectedTextColor) {
        this.mBadgeSelectedTextColor = mBadgeSelectedTextColor;
    }

    public int getBadgeTextColor() {
        return mBadgeTextColor;
    }

    public void setBadgeTextColor(int mBadgeTextColor) {
        this.mBadgeTextColor = mBadgeTextColor;
    }

    public int getBadgeSelectedBackgroundColor() {
        return mBadgeSelectedBackgroundColor;
    }

    public void setBadgeSelectedBackgroundColor(int mBadgeSelectedBackgroundColor) {
        this.mBadgeSelectedBackgroundColor = mBadgeSelectedBackgroundColor;
    }

    public int getBadgeBackgroundColor() {
        return mBadgeBackgroundColor;
    }

    public void setBadgeBackgroundColor(int mBadgeBackgroundColor) {
        this.mBadgeBackgroundColor = mBadgeBackgroundColor;
    }

    public String getBadgeText() {
        return mBadgeText;
    }

    public void setBadgeText(String mBadgeText) {
        this.mBadgeText = mBadgeText;
    }
}
