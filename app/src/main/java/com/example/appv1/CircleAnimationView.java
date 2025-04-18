package com.example.appv1;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class CircleAnimationView extends View {

    private Paint paint;  // Para dibujar el círculo
    private float radius = 0;  // Radio inicial del círculo
    private int centerX, centerY;  // Centro del círculo
    private ValueAnimator animator;  // Animador para expandir el círculo

    public CircleAnimationView(Context context) {
        super(context);
        init();
    }

    public CircleAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);  // Color del círculo
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;  // Centro horizontal
        centerY = h / 2;  // Centro vertical
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Dibujar el círculo en su radio actual
        canvas.drawCircle(centerX, centerY, radius, paint);
    }

    public void startAnimation(int maxRadius) {
        // Animador para expandir el círculo
        animator = ValueAnimator.ofFloat(0, maxRadius);
        animator.setDuration(2000);  // Duración de 2 segundos
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            radius = (float) animation.getAnimatedValue();
            invalidate();  // Redibujar la vista
        });
        animator.start();
    }
}