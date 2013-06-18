package ru.sawim.view;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 28.02.13
 * Time: 19:44
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;
import sawim.ui.base.Scheme;
import ru.sawim.General;

import java.util.Random;
import java.util.Vector;

public class SlideSwitcher extends ViewGroup {

    public static final int ANIMATION_TYPE_CUBE = 0;
    public static final int ANIMATION_TYPE_FLIP_1 = 1;
    public static final int ANIMATION_TYPE_FLIP_2 = 2;
    public static final int ANIMATION_TYPE_FLIP_SIMPLE = 3;
    public static final int ANIMATION_TYPE_ICS = 7;
    public static final int ANIMATION_TYPE_ICS_2 = 10;
    public static final int ANIMATION_TYPE_ROTATE_1 = 4;
    public static final int ANIMATION_TYPE_ROTATE_2 = 5;
    public static final int ANIMATION_TYPE_ROTATE_3 = 6;
    public static final int ANIMATION_TYPE_ROTATE_4 = 9;
    public static final int ANIMATION_TYPE_SNAKE = 8;
    public static final int MODULATOR_SPEED = 10;
    private int currentScreen;
    public Drawable panel;
    OnSwithListener listener;
    //public BitmapDrawable highlight;
    private boolean ANIMATION_RANDOMIZED = false;
    private int ANIMATION_TYPE = 3;
    private int DIVIDER_WIDTH = 1;
    private float FADING_LENGTH = 16.0F;
    private int PANEL_HEIGHT = 48;
    private int SCROLLING_TIME = 280;
    private boolean animation;
    //public TypedArray attrs;
    private Vector blinks = new Vector();
    private int direction_ = 10;
    //private Paint effect;
    private Shader fade_shader;
    private Paint fade_shader_;
    private Matrix fade_shader_m;
    private boolean freezed = false;
    private boolean fully_locked = false;
    //private Paint highlight_;
    private Vector labels = new Vector();
    private TextPaint labels_;
    private float lastTouchX;
    private float lastTouchY;
    private boolean locked = false;
    private boolean mIsBeingDragged;
    private float scrollX;
    private Scroller scroller;
    private boolean show_panel = true;
    private int text_color;
    private int value_ = 0;
    private int wrap_direction = 0;
    private boolean wrap_mode = false;

    public SlideSwitcher(Context var1) {
        super(var1);
        init(var1);
    }

    public SlideSwitcher(Context var1, AttributeSet var2) {
        super(var1, var2);
        //attrs = var1.obtainStyledAttributes(var2, R.styleable.View);
        init(var1);
    }

    private void handleAnimationEnd() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                childAt.setDrawingCacheEnabled(false);
                childAt.setWillNotCacheDrawing(true);
            }
        }
    }

    private void handleAnimationStart() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; ++i) {
            View childAt = getChildAt(i);
            if (childAt != null) {
                childAt.setDrawingCacheEnabled(true);
                childAt.setWillNotCacheDrawing(false);
            }
        }

    }

    private void init(Context var1) {
        setWillNotDraw(true);
        setDrawingCacheEnabled(false);
        setWillNotCacheDrawing(true);
        setStaticTransformationsEnabled(true);
        labels_ = new TextPaint();
        labels_.setColor(-1);
        //    labels_.setShadowLayer(1.0F, 0.0F, 0.0F, -13421773);
        labels_.setAntiAlias(true);
        labels_.setStrokeWidth(3.4F);
    /*    effect = new TextPaint();
        effect.setAntiAlias(true);
        effect.setStyle(Style.STROKE);
        effect.setStrokeWidth(4.0F);
        effect.setAlpha(192);*/
        text_color = General.getColor(Scheme.THEME_TEXT);
        //    panel = getContext().getResources().getDrawable(R.drawable.abs__ab_bottom_solid_inverse_holo);
        Resources res = getContext().getResources();
        //highlight = convertToMyFormat(res.getDrawable(R.drawable.slide_switcher_tab_highlight));
        //    highlight_ = new Paint(2);
        //    highlight.setCustomPaint(highlight_);
        //resources.attachSlidePanel(this);
        setLayoutParams(new LayoutParams(-1, -1));
        scroller = new Scroller(var1, new DecelerateInterpolator());
        FADING_LENGTH *= 10;
        fade_shader = new LinearGradient(0.0F, 0.0F, 0.0F, FADING_LENGTH, -1, 16777215, TileMode.CLAMP);
        fade_shader_ = new Paint();
        fade_shader_.setShader(fade_shader);
        fade_shader_.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        fade_shader_m = new Matrix();
        updateConfig();
    }

    private final boolean isInDisplay(View var1) {
        Rect var3 = new Rect(var1.getLeft(), var1.getTop(), var1.getRight(), var1.getBottom());
        int var2 = getScrollX();
        return var3.intersect(new Rect(var2, 0, var2 + getWidth(), getHeight()));
    }

    private void prepareModulator() {
        value_ += direction_;
        if (value_ > 255) {
            value_ = 255;
            direction_ = -10;
        }

        if (value_ < 0) {
            value_ = 0;
            direction_ = 10;
        }

        //effect.setAlpha(value_);
    }

    private void setAnimationState(boolean var1) {
        if (!var1) {
            if (animation) {
                handleAnimationEnd();
                animation = false;
            }
        } else if (!animation) {
            handleAnimationStart();
            animation = true;
        }

    }

    private final void switchToNext() {
        if (!fully_locked) {
            if (currentScreen != getChildCount() - 1) {
                ++currentScreen;
                scroller.startScroll(getScrollX(), 0, 0, 0, SCROLLING_TIME);
                scroller.setFinalX(currentScreen * (getWidth() + DIVIDER_WIDTH));
            } else {
                currentScreen = 0;
                wrapToFirst();
            }
            listener.switchScreen(currentScreen);
            postInvalidate();
        }

    }

    public void setOnSwithListener(OnSwithListener l) {
        listener = l;
    }

    public int getCurrentItem() {
        return currentScreen;
    }

    private final void switchToPrev() {
        if (!fully_locked) {
            if (currentScreen != 0) {
                --currentScreen;
                scroller.startScroll(getScrollX(), 0, 0, 0, SCROLLING_TIME);
                scroller.setFinalX(currentScreen * (getWidth() + DIVIDER_WIDTH));
            } else {
                currentScreen = getChildCount() - 1;
                wrapToLast();
            }
            listener.switchScreen(currentScreen);
            postInvalidate();
        }

    }

    private void wrapToFirst() {
        wrap_mode = true;
        wrap_direction = 1;
        scroller.startScroll(getScrollX(), 0, 0, 0, SCROLLING_TIME);
        int var1 = getWidth() + DIVIDER_WIDTH;
        scroller.setFinalX(var1 * getChildCount());
    }

    private void wrapToLast() {
        wrap_mode = true;
        wrap_direction = -1;
        scroller.startScroll(getScrollX(), 0, 0, 0, SCROLLING_TIME);
        scroller.setFinalX(-(getWidth() + DIVIDER_WIDTH));
    }

    public void addView(View var1, String var2) {
        labels.add(var2);
        blinks.add(null);
        if (var1.getLayoutParams() == null) {
            var1.setLayoutParams(new android.widget.LinearLayout.LayoutParams(-1, -1));
        }

        super.addView(var1);
    }

    public void computeScroll() {
        if (!mIsBeingDragged) {
            if (!scroller.computeScrollOffset()) {
                if (!wrap_mode) {
                    setAnimationState(false);
                } else if (wrap_direction >= 0) {
                    if (wrap_direction <= 0) {
                        wrap_mode = false;
                    } else {
                        scrollTo(0, 0);
                        postInvalidate();
                        wrap_direction = 0;
                        setAnimationState(false);
                    }
                } else {
                    scrollTo((getWidth() + DIVIDER_WIDTH) * (getChildCount() - 1), 0);
                    wrap_direction = 0;
                    setAnimationState(false);
                }
            } else {
                scrollTo(scroller.getCurrX(), 0);
                postInvalidate();
            }
        }

    }

    public void dispatchDraw(Canvas canvas) {
        //if (!freezed) {
            int childCount = getChildCount();
            super.dispatchDraw(canvas);
        //    if (show_panel) {
                float scrollX1 = (float) getScrollX();
                float w = (float) (getWidth() + DIVIDER_WIDTH);
                float w2 = (float) ((getWidth() + DIVIDER_WIDTH) / 2);
                //    panel.setBounds((int)var8, 0, (int)(var8 + var6), PANEL_HEIGHT);
                //    panel.draw(var1);
                Paint p = new Paint();
                p.setColor(General.getColor(Scheme.THEME_BACKGROUND));
                canvas.drawRect((int) scrollX1, 0, (int) (scrollX1 + w), PANEL_HEIGHT, p);
                int var2 = canvas.saveLayer(scrollX1, 0.0F, scrollX1 + w, (float) PANEL_HEIGHT, null, 4);
                float var4 = (float) (-labels_.getFontMetricsInt().ascent - labels_.getFontMetricsInt().descent);
                int var9 = labels.size();
                int var3 = currentScreen;

                //while (true) {
                    int var10 = var9 + 1;
                    if (var3 > var10) {
                        fade_shader_m.setRotate(-90.0F);
                        fade_shader.setLocalMatrix(fade_shader_m);
                        canvas.translate(scrollX1, 0.0F);
                        canvas.drawRect(0.0F, 0.0F, FADING_LENGTH, (float) PANEL_HEIGHT, fade_shader_);
                        fade_shader_m.setRotate(90.0F);
                        fade_shader_m.postTranslate(FADING_LENGTH, 0.0F);
                        canvas.translate((float) getWidth() - FADING_LENGTH, 0.0F);
                        fade_shader.setLocalMatrix(fade_shader_m);
                        canvas.drawRect(0.0F, 0.0F, FADING_LENGTH, (float) PANEL_HEIGHT, fade_shader_);
                        canvas.restoreToCount(var2);
                        //break;
                        return;
                    }
                    String var18 = null;
                    boolean var12 = false;
                    int scrollX = getScrollX();
                    float var15 = (float) (scrollX / 2) + w2 * (float) var3;
                    if (var3 != -1) {
                        if (var3 >= 0 && var3 < var9) {
                            var18 = (String) labels.get(var3);
                            if (blinks.get(var3) == null) {
                                var12 = false;
                            } else {
                                var12 = true;
                            }

                            scrollX = getScrollX();
                            var15 = (float) (scrollX / 2) + w2 * (float) var3;
                        } else if (var3 != var9) {
                            if (var3 != -2) {
                                int var11 = var9 + 1;
                                if (var3 == var11) {
                                    if (childCount == 1) {
                                        //break;
                                        return;
                                    }

                                    var18 = (String) labels.get(1);
                                    if (blinks.get(1) == null) {
                                        var12 = false;
                                    } else {
                                        var12 = true;
                                    }

                                    scrollX = getScrollX();
                                    var15 = (float) (scrollX / 2) + w2 * (float) var3;
                                }
                            } else {
                                if (childCount == 1) {
                                    //break;
                                    return;
                                }

                                var18 = (String) labels.get(var9 - 2);
                                if (blinks.get(var9 - 2) == null) {
                                    var12 = false;
                                } else {
                                    var12 = true;
                                }

                                scrollX = getScrollX();
                                var15 = (float) (scrollX / 2) - 2.0F * w2;
                            }
                        } else {
                            if (childCount == 1) {
                                //break;
                                return;
                            }

                            var18 = (String) labels.get(0);
                            if (blinks.get(0) == null) {
                                var12 = false;
                            } else {
                                var12 = true;
                            }

                            scrollX = getScrollX();
                            var15 = (float) (scrollX / 2) + w2 * (float) var3;
                        }
                    } else {
                        if (childCount == 1) {
                            //break;
                            return;
                        }

                        var18 = (String) labels.get(var9 - 1);
                        if (blinks.get(var9 - 1) == null) {
                            var12 = false;
                        } else {
                            var12 = true;
                        }

                        scrollX = getScrollX();
                        var15 = (float) (scrollX / 2) - w2;
                    }

                    float var14 = labels_.measureText(var18);
                    float var17 = var15 + w2 - var14 / 2.0F;
                    if (var17 + var14 > (float) scrollX && var17 < w + (float) scrollX) {
                        int var20 = 255 - (int) (255.0F * Math.abs(w2 + (float) scrollX - var14 / 2.0F - var17) / (0.65F * w));
                        if (var20 > 255) {
                            var20 = 255;
                        }

                        if (var20 < 0) {
                            var20 = 0;
                        }

                        float var19 = (float) (PANEL_HEIGHT / 2) + var4 / 2.0F;
                        if (!var12) {
                            labels_.setStrokeWidth(4.0F);
                        } else {
                            //    Paint var16 = effect;
                            //    var1.drawText(var18, var17, var19, var16);
                            //    labels_.setStrokeWidth(1.0F);
                        }

                        //    highlight.setBounds((int) var15, 0, (int) (var15 + var6), PANEL_HEIGHT);
                        //    highlight_.setAlpha(var20);
                        //    highlight.draw(var1);
                        labels_.setColor(General.getColor(Scheme.THEME_TEXT));
                        TextPaint var22 = labels_;
                        int var24;
                        if (!var12) {
                            var24 = var20;
                        } else {
                            var24 = 255;
                        }

                        var22.setAlpha(var24);
                        labels_.setStyle(Style.STROKE);
                        TextPaint var26 = labels_;
                        canvas.drawText(var18, var17, var19, var26);
                        var26 = labels_;
                        int var21;
                        if (!var12) {
                            var21 = -1;
                        } else {
                            var21 = text_color;
                        }

                        var26.setColor(var21);
                        var26 = labels_;
                        int var23;
                        if (!var12) {
                            var23 = var20;
                        } else {
                            var23 = 255;
                        }

                        var26.setAlpha(var23);
                        labels_.setStyle(Style.FILL);
                        TextPaint var25 = labels_;
                        canvas.drawText(var18, var17, var19, var25);
                    }
                    //++var3;
                //}
         //   }
        //}

    }

    public boolean dispatchKeyEvent(KeyEvent var1) {
        View var2 = getChildAt(currentScreen);
        boolean var3;
        if (var2 != null) {
            var3 = var2.dispatchKeyEvent(var1);
            if (!var3 && var1.getAction() == 0 && scroller.isFinished()) {
                if (var1.getKeyCode() == 21 && !fully_locked) {
                    switchToPrev();
                    var3 = true;
                    return var3;
                }

                if (var1.getKeyCode() == 22 && !fully_locked) {
                    switchToNext();
                    var3 = true;
                    return var3;
                }
            }

            //Log.e("KEY_EVENT", "CODE: " + var1.getKeyCode() + "     EVENT: " + var1.getAction() + "     HANDLED:" + var3);
        } else {
            var3 = false;
        }

        return var3;
    }

    public boolean dispatchTouchEvent(MotionEvent var1) {
        boolean var4;
        if (!wrap_mode) {
            if (getChildCount() != 0) {
                float var2;
                switch (var1.getAction()) {
                    case 0:
                        locked = false;
                        scrollX = var1.getX();
                        lastTouchX = scrollX;
                        lastTouchY = var1.getY();
                        if (!scroller.isFinished()) {
                            mIsBeingDragged = true;
                            scroller.forceFinished(true);
                            var4 = true;
                            return var4;
                        }

                        mIsBeingDragged = false;
                        break;
                    case 1:
                    case 3:
                        if (mIsBeingDragged) {
                            mIsBeingDragged = false;
                            var2 = var1.getX() - lastTouchX;
                            getChildCount();
                            if (Math.abs(var2) > 96.0F && !fully_locked) {
                                if (var2 >= 0.0F) {
                                    switchToPrev();
                                } else {
                                    switchToNext();
                                }
                            } else {
                                scroller.startScroll(getScrollX(), 0, 0, 0, SCROLLING_TIME);
                                scroller.setFinalX(currentScreen * (getWidth() + DIVIDER_WIDTH));
                            }

                            postInvalidate();
                        }
                        break;
                    case 2:
                        if (!mIsBeingDragged) {
                            var2 = Math.abs(lastTouchX - var1.getX());
                            float var3 = Math.abs(var1.getY() - lastTouchY);
                            if (var2 > 32.0F && !locked && !fully_locked) {
                                if (var3 <= 32.0F) {
                                    mIsBeingDragged = true;
                                    scrollX = var1.getX();
                                    lastTouchX = scrollX;
                                    if (ANIMATION_RANDOMIZED) {
                                        ANIMATION_TYPE = (new Random(System.currentTimeMillis())).nextInt(8);
                                    }

                                    setAnimationState(true);
                                } else {
                                    locked = true;
                                }
                            }
                        } else {
                            scrollBy((int) (scrollX - var1.getX()), 0);
                            scrollX = var1.getX();
                        }
                }

                if (mIsBeingDragged) {
                    super.dispatchTouchEvent(MotionEvent.obtain(1L, 1L, 3, var1.getX(), var1.getY(), 0));
                    var4 = false;
                } else {
                    var4 = super.dispatchTouchEvent(var1);
                }
            } else {
                var4 = false;
            }
        } else {
            var4 = false;
        }

        return var4;
    }

    protected boolean drawChild(Canvas var1, View var2, long var3) {
        /*int var6 = getScrollX();
        int var7 = getChildCount();
        boolean var5;
        if (var6 >= 0) {
            var5 = false;
        } else {
            var5 = true;
        }

        boolean var9;
        if (var6 <= var7 * (getWidth() + DIVIDER_WIDTH) - getWidth()) {
            var9 = false;
        } else {
            var9 = true;
        }

        int var8 = indexOfChild(var2);
        boolean var10;
        if (var8 != var7 - 1) {
            var10 = false;
        } else {
            var10 = true;
        }

        boolean var11;
        if (var8 != 0) {
            var11 = false;
        } else {
            var11 = true;
        }

        if ((!var5 || !var10) && (!var9 || !var11)) {
            var5 = false;
        } else {
            var5 = true;
        }

        if (!isInDisplay(var2) && !var5) {
            var5 = false;
        } else {
            var5 = super.drawChild(var1, var2, var3);
        }
        return var5;*/
        return super.drawChild(var1, var2, var3);
    }

    public void freezeInvalidating(boolean var1) {
        freezed = var1;
        invalidate();
    }

    public int getAnimationType() {
        return ANIMATION_TYPE;
    }

    public void setAnimationType(int var1) {
        ANIMATION_TYPE = var1;
        invalidate();
    }

    /*protected boolean getChildStaticTransformation(View var1, Transformation var2) {
        Log.e("====================================", "--");
        int var7 = getScrollX();
        int var11 = getChildCount();
        boolean var9;
        if (var7 >= 0) {
            var9 = false;
        } else {
            var9 = true;
        }

        int var6 = var11 * (getWidth() + DIVIDER_WIDTH);
        boolean var8;
        if (var7 <= var6 - getWidth()) {
            var8 = false;
        } else {
            var8 = true;
        }

        int var3 = indexOfChild(var1);
        boolean var10;
        if (var3 != var11 - 1) {
            var10 = false;
        } else {
            var10 = true;
        }

        boolean var12;
        if (var3 != 0) {
            var12 = false;
        } else {
            var12 = true;
        }

        var3 = var1.getRight() - var1.getLeft();
        int var5 = var1.getBottom() - var1.getTop();
        int var4 = 0;
        if (var11 > 1) {
            if (var9 && var10) {
                var4 = -var6;
            }

            if (var8 && var12) {
                var4 = var6;
            }
        }

        var7 -= var4 + var1.getLeft();
        var2.clear();
        Matrix var14 = var2.getMatrix();
        switch (ANIMATION_TYPE) {
            case 0:
                var2.setTransformationType(Transformation.TYPE_MATRIX);
                Transform.applyPolyCube(var14, var3, var5, 180.0F * (float) var7 / (float) var3, var7);
                break;
            case 1:
                var2.setTransformationType(Transformation.TYPE_MATRIX);
                Transform.applyPolyCubeInv(var14, var3, var5, 180.0F * (float) var7 / (float) var3, var7);
                break;
            case 2:
                var2.setTransformationType(Transformation.TYPE_MATRIX);
                Transform.applyTransformationFlip2(180.0F * (float) var7 / (float) var3, (float) (var3 / 2), (float) (var5 / 2), var14);
            case 3:
            default:
                break;
            case 4:
                var14.postRotate(180.0F * (float) var7 / (float) var3, (float) (var3 / 2), (float) (var5 / 2));
                break;
            case 5:
                var14.postRotate(90.0F * (float) (-var7) / (float) var3, (float) (var3 / 2), (float) var5);
                break;
            case 6:
                var14.postRotate(90.0F * (float) var7 / (float) var3, (float) (var3 / 2), 0.0F);
                break;
            case 7:
                float var15 = Math.abs((float) var7 / (float) var3);
                if (var7 < 0) {
                    var2.setTransformationType(Transformation.TYPE_BOTH);
                    var2.setAlpha(1.0F - var15);
                    var15 = Math.abs((float) var7 / (float) var3) / 7.0F;
                    var14.postScale(1.0F - var15, 1.0F - var15, (float) (var3 / 2), (float) (var5 / 2));
                    var14.postTranslate((float) var7, 0.0F);
                } else {
                    var2.setTransformationType(Transformation.TYPE_BOTH);
                    var2.setAlpha(1.0F - var15);
                }
                break;
            case 8:
                var2.setTransformationType(Transformation.TYPE_MATRIX);
                Transform.applyPolySnake(var14, var3, var5, 180.0F * (float) var7 / (float) var3, var7);
                break;
            case 9:
                float var13 = 1.0F - Math.abs((float) var7 / (float) var3);
                var2.setTransformationType(Transformation.TYPE_BOTH);
                var2.setAlpha(var13);
                var14.postRotate(90.0F * (float) var7 / (float) var3, 0.0F, 0.0F);
                var14.postTranslate((float) var7, 0.0F);
                break;
            case 10:
                var2.setTransformationType(Transformation.TYPE_MATRIX);
                Transform.applyTransformationFlip2(20.0F * (float) var7 / (float) var3, (float) (var3 / 2), (float) (var5 / 2), var14);
        }

        var14.postTranslate((float) var4, 0.0F);
        return true;
    }*/

    protected void measureChild(View var1, int var2, int var3) {
    }

    protected void measureChildWithMargins(View var1, int var2, int var3, int var4, int var5) {
    }

    protected void measureChildren(int var1, int var2) {
    }

    protected void onLayout(boolean var1, int var2, int var3, int var4, int var5) {
        int var6 = 0;
        if (show_panel) {
            var6 = PANEL_HEIGHT;
        }

        int var7 = getChildCount();
        int var9 = getHeight();
        if (show_panel) {
        int var10 = var9 - PANEL_HEIGHT;
        for (int var8 = 0; var8 < var7; ++var8) {
            View var11 = getChildAt(var8);
            var9 = getWidth() + DIVIDER_WIDTH;
            var11.measure(MeasureSpec.makeMeasureSpec(getWidth(), 1073741824), MeasureSpec.makeMeasureSpec(var10, 1073741824));
            var11.layout(var2 + var9 * var8, var6, var4 + var9 * var8, getHeight());
        }
        }
    }

    public void onMeasure(int var1, int var2) {
        setMeasuredDimension(MeasureSpec.getSize(var1), MeasureSpec.getSize(var2));
    }

    protected final void onSizeChanged(int var1, int var2, int var3, int var4) {
        super.onSizeChanged(var1, var2, var3, var4);
        scrollTo(currentScreen * (getWidth() + DIVIDER_WIDTH), 0);
        requestLayout();
    }

    public void removeViewAt(int var1) {
        int var2 = getChildCount();
        if (var2 > 0 && var1 < var2) {
            super.removeViewAt(var1);
            labels.remove(var1);
            blinks.remove(var1);
            if (var1 >= currentScreen) {
                if (var1 == currentScreen && var2 > 1) {
                    --currentScreen;
                    if (!scroller.isFinished()) {
                        scrollTo((currentScreen - 1) * (getWidth() + DIVIDER_WIDTH), 0);
                    } else {
                        scrollTo(currentScreen * (getWidth() + DIVIDER_WIDTH), 0);
                    }
                }
            } else {
                --currentScreen;
                if (!scroller.isFinished()) {
                    scrollTo((currentScreen - 1) * (getWidth() + DIVIDER_WIDTH), 0);
                } else {
                    scrollTo(currentScreen * (getWidth() + DIVIDER_WIDTH), 0);
                }
            }
        }

    }

    public void scrollTo(int var1) {
        int var2 = getChildCount();
        if (var2 > 0 && var1 < var2) {
            scrollTo(var1 * (getWidth() + DIVIDER_WIDTH), 0);
            currentScreen = var1;
        }

    }

    public void setBlinkState(int var1, boolean var2) {
        if (var1 >= 0 && var1 < labels.size()) {
            Vector var4 = blinks;
            Object var3;
            if (!var2) {
                var3 = null;
            } else {
                var3 = new Object();
            }

            var4.set(var1, var3);
            invalidate();
        }

    }

    public void setLock(boolean var1) {
        fully_locked = var1;
    }

    public void setRandomizedAnimation(boolean var1) {
        ANIMATION_RANDOMIZED = var1;
    }

    public void togglePanel(boolean var1) {
        show_panel = var1;
        requestLayout();
    }

    public void updateConfig() {
        float var1 = (float) 3;
        var1 += 10.0F * (var1 / 100.0F);
        labels_.setTextSize(var1 * 8);
    /*    effect.setColor(0xFFFFFF);
        effect.setAlpha(160);
        effect.setTextSize(labels_.getTextSize());*/
        //    highlight_.setColorFilter(new LightingColorFilter(0, 0xFFFFFF));
        PANEL_HEIGHT = /*(int) ((var1 + 70.0F * (var1 / 100.0F)) * 10)*/(int) (var1 * 8);
        DIVIDER_WIDTH = (int) (0.0F * 10);
        requestLayout();
    }

    public void updateLabel(int var1, String var2) {
        if (var1 >= 0 && var1 < labels.size()) {
            labels.set(var1, var2);
            invalidate();
        }

    }

    public interface OnSwithListener {
        public void switchScreen(int pos);
    }
}

class Transform {
    private static Camera mCamera;

    public static void applyPolyCube(Matrix paramMatrix, int paramInt1, int paramInt2, float paramFloat, int paramInt3) {
        int j = Math.abs(paramInt3);
        int i = (int) Math.abs(Math.sin(3.141592653589793D * paramFloat / 180.0D / 2.0D) * (paramInt2 * 24 / 100));
        float[] arrayOfFloat1;
        float[] arrayOfFloat2;
        if (paramFloat <= 0.0F) {
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = 0.0F;
            arrayOfFloat1[1] = 0.0F;
            arrayOfFloat1[2] = paramInt1;
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = paramInt1;
            arrayOfFloat1[5] = paramInt2;
            arrayOfFloat1[6] = 0.0F;
            arrayOfFloat1[7] = paramInt2;
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = 0.0F;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = (paramInt1 - j);
            arrayOfFloat2[3] = i;
            arrayOfFloat2[4] = (paramInt1 - j);
            arrayOfFloat2[5] = (paramInt2 - i);
            arrayOfFloat2[6] = 0.0F;
            arrayOfFloat2[7] = paramInt2;
            paramMatrix.setPolyToPoly(arrayOfFloat1, 0, arrayOfFloat2, 0, 4);
        } else {
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = 0.0F;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = paramInt1;
            arrayOfFloat2[3] = 0.0F;
            arrayOfFloat2[4] = paramInt1;
            arrayOfFloat2[5] = paramInt2;
            arrayOfFloat2[6] = 0.0F;
            arrayOfFloat2[7] = paramInt2;
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = j;
            arrayOfFloat1[1] = i;
            arrayOfFloat1[2] = paramInt1;
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = paramInt1;
            arrayOfFloat1[5] = paramInt2;
            arrayOfFloat1[6] = j;
            arrayOfFloat1[7] = (paramInt2 - i);
            paramMatrix.setPolyToPoly(arrayOfFloat2, 0, arrayOfFloat1, 0, 4);
        }
    }

    public static void applyPolyCubeInv(Matrix paramMatrix, int paramInt1, int paramInt2, float paramFloat, int paramInt3) {
        int j = Math.abs(paramInt3);
        int i = (int) Math.abs(Math.sin(3.141592653589793D * paramFloat / 180.0D / 2.0D) * (paramInt2 * 24 / 100));
        float[] arrayOfFloat2;
        float[] arrayOfFloat1;
        if (paramFloat <= 0.0F) {
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = 0.0F;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = paramInt1;
            arrayOfFloat2[3] = 0.0F;
            arrayOfFloat2[4] = paramInt1;
            arrayOfFloat2[5] = paramInt2;
            arrayOfFloat2[6] = 0.0F;
            arrayOfFloat2[7] = paramInt2;
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = 0.0F;
            arrayOfFloat1[1] = i;
            arrayOfFloat1[2] = (paramInt1 - j);
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = (paramInt1 - j);
            arrayOfFloat1[5] = paramInt2;
            arrayOfFloat1[6] = 0.0F;
            arrayOfFloat1[7] = (paramInt2 - i);
            paramMatrix.setPolyToPoly(arrayOfFloat2, 0, arrayOfFloat1, 0, 4);
        } else {
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = 0.0F;
            arrayOfFloat1[1] = 0.0F;
            arrayOfFloat1[2] = paramInt1;
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = paramInt1;
            arrayOfFloat1[5] = paramInt2;
            arrayOfFloat1[6] = 0.0F;
            arrayOfFloat1[7] = paramInt2;
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = j;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = paramInt1;
            arrayOfFloat2[3] = i;
            arrayOfFloat2[4] = paramInt1;
            arrayOfFloat2[5] = (paramInt2 - i);
            arrayOfFloat2[6] = j;
            arrayOfFloat2[7] = paramInt2;
            paramMatrix.setPolyToPoly(arrayOfFloat1, 0, arrayOfFloat2, 0, 4);
        }
    }

    public static void applyPolySnake(Matrix paramMatrix, int paramInt1, int paramInt2, float paramFloat, int paramInt3) {
        int i = Math.abs(paramInt3);
        float[] arrayOfFloat2;
        float[] arrayOfFloat1;
        if (paramFloat <= 0.0F) {
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = 0.0F;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = paramInt1;
            arrayOfFloat2[3] = 0.0F;
            arrayOfFloat2[4] = paramInt1;
            arrayOfFloat2[5] = paramInt2;
            arrayOfFloat2[6] = 0.0F;
            arrayOfFloat2[7] = paramInt2;
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = 0.0F;
            arrayOfFloat1[1] = 0.0F;
            arrayOfFloat1[2] = (paramInt1 - i);
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = (paramInt1 - i);
            arrayOfFloat1[5] = (paramInt2 - 0);
            arrayOfFloat1[6] = 0.0F;
            arrayOfFloat1[7] = paramInt2;
            paramMatrix.setPolyToPoly(arrayOfFloat2, 0, arrayOfFloat1, 0, 4);
        } else {
            arrayOfFloat2 = new float[8];
            arrayOfFloat2[0] = 0.0F;
            arrayOfFloat2[1] = 0.0F;
            arrayOfFloat2[2] = paramInt1;
            arrayOfFloat2[3] = 0.0F;
            arrayOfFloat2[4] = paramInt1;
            arrayOfFloat2[5] = paramInt2;
            arrayOfFloat2[6] = 0.0F;
            arrayOfFloat2[7] = paramInt2;
            arrayOfFloat1 = new float[8];
            arrayOfFloat1[0] = i;
            arrayOfFloat1[1] = 0.0F;
            arrayOfFloat1[2] = paramInt1;
            arrayOfFloat1[3] = 0.0F;
            arrayOfFloat1[4] = paramInt1;
            arrayOfFloat1[5] = paramInt2;
            arrayOfFloat1[6] = i;
            arrayOfFloat1[7] = (paramInt2 - 0);
            paramMatrix.setPolyToPoly(arrayOfFloat2, 0, arrayOfFloat1, 0, 4);
        }
    }

    public static void applyTransformationFlip2(float paramFloat1, float paramFloat2, float paramFloat3, Matrix paramMatrix) {
        if (mCamera == null)
            mCamera = new Camera();
        Camera localCamera = mCamera;
        localCamera.save();
        localCamera.translate(0.0F, 0.0F, 0.0F);
        localCamera.translate(0.0F, 0.0F, 0.0F);
        localCamera.rotateY(paramFloat1);
        localCamera.getMatrix(paramMatrix);
        localCamera.restore();
        paramMatrix.preTranslate(-paramFloat2, -paramFloat3);
        paramMatrix.postTranslate(paramFloat2, paramFloat3);
    }
}
