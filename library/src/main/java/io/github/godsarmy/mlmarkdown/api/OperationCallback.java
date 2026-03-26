package io.github.godsarmy.mlmarkdown.api;

public interface OperationCallback {
    void onSuccess();

    void onFailure(Exception error);
}
