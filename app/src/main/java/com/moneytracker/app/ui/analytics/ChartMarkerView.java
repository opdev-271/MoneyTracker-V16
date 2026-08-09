package com.moneytracker.app.ui.analytics;

import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.moneytracker.app.R;
import com.moneytracker.app.util.CurrencyFormatter;

/**
 * Floating tooltip shown while a finger drags across the cash-flow line chart, so the
 * user can scrub through the period and see the income/expense figures at whichever
 * point in time their finger is over. {@link #setSeries} is called once whenever the
 * chart's data (re)loads; {@link #refreshContent} then just looks up that bucket.
 */
public class ChartMarkerView extends MarkerView {

    private final TextView textLabel;
    private final TextView textIncome;
    private final TextView textExpense;

    private float[] incomeSeries = new float[0];
    private float[] expenseSeries = new float[0];
    private String[] bucketLabels = new String[0];

    public ChartMarkerView(Context context) {
        super(context, R.layout.view_chart_marker);
        textLabel = findViewById(R.id.text_marker_label);
        textIncome = findViewById(R.id.text_marker_income);
        textExpense = findViewById(R.id.text_marker_expense);
    }

    public void setSeries(float[] incomeSeries, float[] expenseSeries, String[] bucketLabels) {
        this.incomeSeries = incomeSeries;
        this.expenseSeries = expenseSeries;
        this.bucketLabels = bucketLabels;
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = Math.round(e.getX());
        float income = index >= 0 && index < incomeSeries.length ? incomeSeries[index] : 0f;
        float expense = index >= 0 && index < expenseSeries.length ? expenseSeries[index] : 0f;
        String label = index >= 0 && index < bucketLabels.length ? bucketLabels[index] : "";

        textLabel.setText(label);
        textLabel.setVisibility(label.isEmpty() ? GONE : VISIBLE);
        textIncome.setText("↓ " + CurrencyFormatter.format(income, null));
        textExpense.setText("↑ " + CurrencyFormatter.format(expense, null));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight() - 16f);
    }
}
