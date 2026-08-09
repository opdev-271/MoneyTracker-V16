package com.moneytracker.app.util;

/**
 * Wraps a value that should only be delivered once, e.g. a navigation or
 * snackbar signal published through LiveData. Prevents the classic "same
 * event replays on rotation" bug.
 */
public class Event<T> {

    private final T content;
    private boolean hasBeenHandled = false;

    public Event(T content) {
        this.content = content;
    }

    /** Returns the content, or null if it has already been handled. */
    public T getContentIfNotHandled() {
        if (hasBeenHandled) {
            return null;
        }
        hasBeenHandled = true;
        return content;
    }

    public T peekContent() {
        return content;
    }
}
