package com.github.benjchristensen.gist.perf;

import rx.Observer;


public class LongSumObserver implements Observer<Long> {

    public long sum = 0;

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        throw new RuntimeException(e);
    }

    @Override
    public void onNext(Long l) {
        sum += l;
    }
}