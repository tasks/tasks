package com.todoroo.astrid.service;

import com.todoroo.andlib.service.ContextManager;
import com.todoroo.andlib.utility.DateUtilities;
import com.todoroo.astrid.helper.ProgressBarSyncResultCallback;
import com.todoroo.astrid.sync.SyncResultCallback;
import com.todoroo.astrid.widget.TasksWidget;

public class SyncResultCallbackWrapper implements SyncResultCallback {
    private final SyncResultCallback wrapped;
    public SyncResultCallbackWrapper(SyncResultCallback wrap) {
        this.wrapped = wrap;
    }
    @Override
    public void incrementMax(int incrementBy) {
        wrapped.incrementMax(incrementBy);
    }
    @Override
    public void incrementProgress(int incrementBy) {
        wrapped.incrementProgress(incrementBy);
    }
    @Override
    public void started() {
        wrapped.started();
    }
    @Override
    public void finished() {
        wrapped.finished();
    }

    public static class WidgetUpdatingCallbackWrapper extends SyncResultCallbackWrapper {

        public WidgetUpdatingCallbackWrapper(SyncResultCallback wrap) {
            super(wrap);
        }

        @Override
        public void started() {
            super.started();
            TasksWidget.suppressUpdateFlag = DateUtilities.now();
        }

        @Override
        public void finished() {
            super.finished();
            TasksWidget.suppressUpdateFlag = 0L;
            TasksWidget.updateWidgets(ContextManager.getContext());
        }

    }

    /**
     * This class can be used if you want to claim a relative proportion of the SyncResultCallback,
     * but you may also want to increase the maximum size of data points as well. For example, there
     * may be 10 data points, and you want to make use of half of them to indicate that once you
     * are finished, you are half way through. But if you increase the maximum number of data
     * points, the remainder that you're not interacting with will need to increase by the same
     * proportion so that you still interact with half of the data points.
     *
     * Once you wrap this class around the original SyncResultCallback object and have finished
     * using it, you then invoke the getRemainderSize method afterwards to see how many data points are
     * left for you to use. You can then use the Rescaled wrapper to simplify caller logic afterwards.
     */
    public static class Partial extends SyncResultCallbackWrapper {

        protected final double factor;
        protected int fragment_size;
        protected int total_size;

        /**
         * Wraps a SyncResultCallback object.
         *
         * @param wrap Original object to wrap.
         * @param fragment_size Number of data points that you want to interact with.
         * @param total_size Total number of data points remaining and available in the original object.
         */
        public Partial(SyncResultCallback wrap,
                       int fragment_size, int total_size) {
            super(wrap);
            this.factor = Math.ceil((double)total_size / fragment_size);
            this.fragment_size = fragment_size;
            this.total_size = total_size;
        }

        @Override
        public void incrementMax(int incrementBy) {
            this.fragment_size += incrementBy;
            int new_total = (int)(this.factor * this.fragment_size);
            super.incrementMax(new_total - total_size);
            this.total_size = new_total;
        }

        public int getRemainderSize() {
            return this.total_size - this.fragment_size;
        }

    }

    /**
     * Given a SyncResultCallback object with a certain number of data points, treat it as if it
     * had a different number of data points, and allow the underlying wrapped object to receive a
     * proportionally equivalent amount of data points.
     */
    public static class Rescaled extends SyncResultCallbackWrapper {

        protected int input_size;
        protected int actual_size;
        protected int input_count;
        protected int actual_count;

        public Rescaled(SyncResultCallback wrap,
                        int input_size, int actual_size) {
            super(wrap);
            if (input_size <= 0) throw new IllegalArgumentException("input_size is not +ve");
            if (actual_size <= 0) throw new IllegalArgumentException("actual size is not +ve");
            this.input_size = input_size;
            this.actual_size = actual_size;
            this.input_count = 0;
            this.actual_count = 0;

            // If we have more input data points than output, we'll increase the number of data
            // points in the underlying one, because otherwise incremental updates fed into us
            // might not show up in the underlying object.
            if (actual_size < input_size) {
                 super.incrementMax(input_size - actual_size);
                 this.actual_size = input_size;
            }
        }

        @Override
        public void incrementProgress(int incrementBy) {

            // Shortcut if we're no longer rescaling.
            if (input_size == actual_size) {
                super.incrementProgress(incrementBy);
                return;
            }

            int new_input_count = input_count + incrementBy;
            int new_actual_count = (new_input_count * actual_size) / input_size;
            if (new_actual_count > actual_count) {
                // This should always be the case, but we just sanity check it.
                super.incrementProgress(new_actual_count - actual_count);
            }
            this.input_count = new_input_count;
            this.actual_count = new_actual_count;
        }

        @Override
        public void incrementMax(int incrementBy) {

            // Shortcut if we're not longer rescaling.
            if (input_size == actual_size) {
                super.incrementMax(incrementBy);
                return;
            }

            // If we increase the maximum used by the input, we want to rescale the values such
            // that the progress (proportionally) remains the same - otherwise we'll appear to
            // have made backward progress on the input side, which we can't indicate to the
            // underlying object.
            int[] rescaled_input = ProgressBarSyncResultCallback.rescaleProgressValues(
                input_count, input_size, incrementBy
            );

            this.input_count = rescaled_input[0];
            this.input_size = rescaled_input[1];

            // No need to rescale internally if no data points exist.
            if (input_count == 0) {return;}

            // The point of this class is to increase the size of input so that it increases the
            // amount of data points sent to the underlying object. Once the input size becomes the
            // same as underlying size, we don't need to do anything other than send the same
            // information to the underlying class.
            //
            // So:
            //   If input remains smaller than actual, we do nothing here.
            //   If input remains equal to actual, we just pass the same data along.
            //   If input goes from smaller to bigger, we need to catch up to make it equal.
            if (input_count > actual_count) {
                super.incrementMax(input_count - actual_count);
                this.actual_count = input_count; // Switches off rescaling.
            }

        }

    }

}
