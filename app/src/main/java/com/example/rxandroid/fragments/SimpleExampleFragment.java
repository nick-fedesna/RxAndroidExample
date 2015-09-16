package com.example.rxandroid.fragments;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import java.util.HashSet;

import butterknife.*;
import com.example.rxandroid.R;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.trello.rxlifecycle.components.support.RxFragment;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

import static android.util.Patterns.EMAIL_ADDRESS;

public class SimpleExampleFragment extends RxFragment {

    @Bind(R.id.output) TextView output;

    public static SimpleExampleFragment newInstance() {
        return new SimpleExampleFragment();
    }

    public SimpleExampleFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simple, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @OnClick(R.id.start) void onRegisterClicked() {
        output.setText(null);

        Integer[] items = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Observable.from(items)
                .map(i -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return (i * i) + i + 1;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::itemEmitted,
                           this::handleError,
                           () -> {
                               output.setText(output.getText() + "\nSequence complete");
                               Timber.d("Sequence complete");
                           });
    }


    private void itemEmitted(int item) {
        output.setText(output.getText() + "\n" + item);
        Timber.d("%d", item);
    }

    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Failure!");
    }

}
