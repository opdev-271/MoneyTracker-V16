package com.moneytracker.app.ui.widget;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import com.moneytracker.app.util.SettingsPrefs;

/**
 * Drives a sliding-pill segmented control: a colored indicator View that glides
 * under whichever of N equal-width TextViews is selected, instead of swapping
 * flat backgrounds. The caller is the single source of truth for which index
 * is selected (usually a ViewModel's LiveData) — {@link #setSelectedIndex} is
 * how that state is reflected here; both taps and drags only ever go out through
 * the listener, exactly the same way.
 */
public class SegmentedToggle {

    public interface OnSegmentSelectedListener {
        void onSegmentSelected(int index);
    }

    private final ViewGroup container;
    private final View indicator;
    private final TextView[] labels;
    private final int selectedColor;
    private final int unselectedColor;
    private final int touchSlop;
    private int currentIndex = -1;
    private OnSegmentSelectedListener listener;

    private int segmentWidthPx = -1;
    private float downRawX;
    private int dragStartIndex;
    private boolean dragging;

    public SegmentedToggle(ViewGroup container, View indicator, TextView[] labels,
                            int selectedColor, int unselectedColor) {
        this.container = container;
        this.indicator = indicator;
        this.labels = labels;
        this.selectedColor = selectedColor;
        this.unselectedColor = unselectedColor;
        this.touchSlop = ViewConfiguration.get(container.getContext()).getScaledTouchSlop();

        for (int i = 0; i < labels.length; i++) {
            int index = i;
            labels[i].setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSegmentSelected(index);
                }
            });
            labels[i].setOnTouchListener(this::onLabelTouch);
        }
    }

    public void setOnSegmentSelectedListener(OnSegmentSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedIndex(int index) {
        boolean firstLayout = currentIndex == -1;
        boolean changed = index != currentIndex;
        currentIndex = index;
        applyColors(index);

        if (!changed && !firstLayout) {
            return;
        }
        if (container.getWidth() > 0) {
            positionIndicator(index, !firstLayout);
        } else {
            container.post(() -> positionIndicator(index, false));
        }
    }

    /**
     * Additive on top of the plain OnClickListener above: ACTION_DOWN always returns
     * false so a simple tap still falls through to that listener untouched. Only once
     * the finger actually moves past the system's tap-vs-drag slop does this take over,
     * letting the pill follow the finger and commit whichever segment it's released over.
     */
    private boolean onLabelTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                dragStartIndex = currentIndex;
                dragging = false;
                return false;

            case MotionEvent.ACTION_MOVE: {
                float deltaX = event.getRawX() - downRawX;
                if (!dragging && Math.abs(deltaX) > touchSlop) {
                    dragging = true;
                    // Several of these toggles live inside a NestedScrollView, which
                    // would otherwise steal the gesture as soon as it looks horizontal-ish.
                    if (container.getParent() != null) {
                        container.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (dragging) {
                    dragIndicatorTo(deltaX);
                    return true;
                }
                return false;
            }

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    finishDrag(resolveDragIndex(event.getRawX() - downRawX));
                    return true;
                }
                return false;

            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    finishDrag(dragStartIndex);
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    private void dragIndicatorTo(float deltaX) {
        if (segmentWidthPx <= 0) {
            return;
        }
        float maxX = segmentWidthPx * (labels.length - 1);
        float target = clamp(dragStartIndex * segmentWidthPx + deltaX, 0f, maxX);
        indicator.animate().cancel();
        indicator.setTranslationX(target);
    }

    private int resolveDragIndex(float deltaX) {
        if (segmentWidthPx <= 0) {
            return dragStartIndex;
        }
        float maxX = segmentWidthPx * (labels.length - 1);
        float target = clamp(dragStartIndex * segmentWidthPx + deltaX, 0f, maxX);
        int index = Math.round(target / segmentWidthPx);
        return clamp(index, 0, labels.length - 1);
    }

    /**
     * Unlike the public setSelectedIndex, this always repositions and re-applies colors
     * even if the resolved index equals the one from before the drag - the live drag
     * already moved the indicator by directly setting translationX, outside of
     * setSelectedIndex's own "did the index change" bookkeeping, so without this the pill
     * could be left stranded wherever the finger last was instead of snapped to a segment.
     */
    private void finishDrag(int index) {
        dragging = false;
        currentIndex = index;
        applyColors(index);
        positionIndicator(index, true);
        if (listener != null) {
            listener.onSegmentSelected(index);
        }
    }

    private void positionIndicator(int index, boolean animate) {
        int usableWidth = container.getWidth() - container.getPaddingLeft() - container.getPaddingRight();
        if (usableWidth <= 0 || labels.length == 0) {
            return;
        }
        int segmentWidth = usableWidth / labels.length;
        segmentWidthPx = segmentWidth;
        ViewGroup.LayoutParams params = indicator.getLayoutParams();
        if (params.width != segmentWidth) {
            params.width = segmentWidth;
            indicator.setLayoutParams(params);
        }
        float targetX = segmentWidth * index;
        if (animate && SettingsPrefs.isAnimationsEnabled(indicator.getContext())) {
            indicator.animate()
                    .translationX(targetX)
                    .setDuration(260)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .start();
        } else {
            indicator.animate().cancel();
            indicator.setTranslationX(targetX);
        }
    }

    private void applyColors(int index) {
        for (int i = 0; i < labels.length; i++) {
            labels[i].setTextColor(i == index ? selectedColor : unselectedColor);
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
