package net.es.oscars.app.util;

public interface AsyncCallback<T> {
    void onSuccess(T result);
    void onFailure(Throwable thrown);
}
