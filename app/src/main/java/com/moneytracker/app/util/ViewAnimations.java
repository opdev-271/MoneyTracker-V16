package com.moneytracker.app.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.moneytracker.app.R;

/**
 * Small, reusable motion touches shared across screens so the app feels consistently
 * "alive" — unless the user turned animations off in Settings (weaker phones), in
 * which case every method here just snaps straight to the end state instead.
 *
 * <p>Everything here is built on plain {@link ValueAnimator}s rather than
 * {@code view.animate()}: RecyclerView's ItemAnimator drives the very same
 * ViewPropertyAnimator on item views and cancels it whenever a row is re-bound,
 * which used to leave rows frozen mid-animation (shifted down and clipped by the
 * list, balances stuck on their pre-mask text). Each animator is also parked in a
 * view tag so a rebind cancels the previous one, and every one of them lands on its
 * final state even when cancelled.
 */
public class ViewAnimations {

    private ViewAnimations() {
    }

    /** Scales a view down on press and springs it back on release, like a physical button. */
    public static void addPressFeedback(View... views) {
        if (views.length == 0 || !animationsEnabled(views[0])) {
            return;
        }
        View.OnTouchListener listener = (v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.94f).scaleY(0.94f)
                            .setDuration(120)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f)
                            .setDuration(180)
                            .setInterpolator(new OvershootInterpolator(1.4f))
                            .start();
                    break;
                default:
                    break;
            }
            return false;
        };
        for (View view : views) {
            if (view != null) {
                view.setOnTouchListener(listener);
            }
        }
    }

    /** Fades + lifts a screen's root content in; call once per fragment view creation. */
    public static void fadeInContent(View root) {
        if (!animationsEnabled(root)) {
            root.setAlpha(1f);
            root.setTranslationY(0f);
            return;
        }
        root.setAlpha(0f);
        root.setTranslationY(24f);
        root.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(40)
                .setDuration(260)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    /**
     * Fades + lifts a RecyclerView row in the first time it's bound (position beyond the
     * highest position already animated), staggered slightly by position. Subsequent rebinds
     * during scroll are left untouched so the effect only plays once per item.
     */
    public static void animateItemEntranceOnce(View itemView, int position, int[] highestAnimatedPosition) {
        // Always start from the settled state: a recycled view may still carry the offset
        // of an animation that was cancelled before it could finish.
        cancelTagged(itemView, R.id.tag_entrance_animator);
        itemView.setAlpha(1f);
        itemView.setTranslationY(0f);

        if (position <= highestAnimatedPosition[0] || !animationsEnabled(itemView)) {
            highestAnimatedPosition[0] = Math.max(highestAnimatedPosition[0], position);
            return;
        }
        highestAnimatedPosition[0] = position;

        final float offset = 40f;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setStartDelay(Math.min(position, 8) * 35L);
        animator.setDuration(260);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();
            itemView.setAlpha(fraction);
            itemView.setTranslationY(offset * (1f - fraction));
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Runs on cancel too, so an interrupted entrance can never leave the row
                // shifted down (which clipped its bottom edge against the list bounds).
                itemView.setTag(R.id.tag_entrance_animator, null);
                itemView.setAlpha(1f);
                itemView.setTranslationY(0f);
            }
        });
        itemView.setAlpha(0f);
        itemView.setTranslationY(offset);
        itemView.setTag(R.id.tag_entrance_animator, animator);
        animator.start();
    }

    /**
     * Progressively masks the TextView's current text left-to-right with {@code •}
     * (spaces are kept as visual anchors), then snaps to {@code finalMask} — used when
     * hiding balances so the digits visibly dissolve instead of vanishing at once.
     * However the animation ends — finished, cancelled, view detached — the TextView is
     * left showing {@code finalMask}, never the amount it was masking.
     */
    public static void animateTextMasking(TextView textView, String finalMask) {
        cancelTagged(textView, R.id.tag_text_mask_animator);

        CharSequence current = textView.getText();
        String original = current != null ? current.toString() : "";
        if (!animationsEnabled(textView) || original.isEmpty() || original.equals(finalMask)) {
            textView.setText(finalMask);
            return;
        }

        int length = original.length();
        ValueAnimator animator = ValueAnimator.ofInt(0, length);
        animator.setDuration(450);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            int maskedCount = (int) animation.getAnimatedValue();
            if (maskedCount >= length) {
                textView.setText(finalMask);
                return;
            }
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                char c = original.charAt(i);
                sb.append(i < maskedCount && c != ' ' && c != ' ' ? '•' : c);
            }
            textView.setText(sb);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                textView.setTag(R.id.tag_text_mask_animator, null);
                textView.setText(finalMask);
            }
        });
        textView.setTag(R.id.tag_text_mask_animator, animator);
        animator.start();
    }

    /** Stops a mask animation still running on a recycled TextView, so it can't overwrite
     * the amount the caller is about to put back. */
    public static void cancelTextMasking(TextView textView) {
        cancelTagged(textView, R.id.tag_text_mask_animator);
    }

    /**
     * Cancels the animator parked under {@code tagId}, without letting its end listener
     * force a final state the caller is about to replace anyway.
     */
    private static void cancelTagged(View view, int tagId) {
        Object tag = view.getTag(tagId);
        if (tag instanceof ValueAnimator) {
            ValueAnimator running = (ValueAnimator) tag;
            running.removeAllListeners();
            running.cancel();
        }
        view.setTag(tagId, null);
    }

    private static boolean animationsEnabled(View view) {
        return SettingsPrefs.isAnimationsEnabled(view.getContext());
    }
}
