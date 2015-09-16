package com.example.rxandroid.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

import butterknife.*;
import com.example.rxandroid.R;
import com.example.rxandroid.api.ExampleApi;
import com.squareup.okhttp.ResponseBody;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.observables.StringObservable;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class FileProcessFragment extends Fragment {

    private float mFileSize;
    private long  mBytesDownloaded;

    public static Fragment newInstance() {
        return new FileProcessFragment();
    }

    @Bind(R.id.progress_meter) ProgressBar  progressBar;
    @Bind(R.id.output_layout)  LinearLayout outputLayout;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.start_button)
    public void startDownload(Button startButton) {
        startButton.setEnabled(false);

        displaySql();
//        writeSqliteToDisk();
    }

    private void displaySql() {
        ExampleApi.getApi(getActivity()).getFile("latest/chrRaces.sql.bz2")
                .subscribeOn(Schedulers.io())
                .doOnNext(this::getFileLength)
                .map(FileProcessFragment::responseToByteStream)
                .map(bytes -> StringObservable.decode(bytes, Charset.forName("UTF-8")))
                .concatMap(StringObservable::byLine)
                .filter(string -> !isCommentOrEmpty(string))
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(sql -> {
                              Timber.d("SQL: %s", sql);
                               TextView tv = new TextView(getActivity());
                               tv.setText(sql);
                               outputLayout.addView(tv);
                           }, e -> Timber.e(e, "Bad things man!"),
                           () -> Timber.d("SQL Complete."));
    }

    boolean isCommentOrEmpty(String string) {
        return string.startsWith("--") || string.startsWith("/*") || string.trim().isEmpty();
    }

    private void writeSqliteToDisk() {
        try {
            File file = new File(getActivity().getExternalFilesDir(null), "sqlite-latest.sqlite");
            final FileOutputStream out = new FileOutputStream(file);

            ExampleApi.getApi(getActivity()).getDatabase()
                    .observeOn(Schedulers.io())
                    .doOnNext(this::getFileLength)
                    .flatMap(FileProcessFragment::responseToByteStream)
                    .onBackpressureBuffer()
                    .doOnNext(bytes -> {
                        try {
                            out.write(bytes);
                        } catch (IOException e) {
                            Timber.e(e, "Failed writing bytes!");
                        }
                    })
                    .doOnTerminate(() -> {
                        Timber.d("Complete.");
                        try {
                            out.flush();
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::updateProgress,
                               e -> Timber.e(e, "Bad things man!"));
        } catch (FileNotFoundException e) {
            Timber.e(e, "Failed to create output file!");
        }
    }

    private void getFileLength(Response<ResponseBody> responseBodyResponse) {
        try {
            mFileSize = responseBodyResponse.body().contentLength();
            Timber.d("File size: %.1fkb", mFileSize / 1024f);
        } catch (IOException e) {
            Timber.e(e, "Failed to get download size!");
        }
    }

    private void updateProgress(byte[] bytes) {
        if (mFileSize > 0) {
            mBytesDownloaded += bytes.length;
            int progress = (int) ((mBytesDownloaded / mFileSize) * 1000);
            Timber.d("Progress: %.1f%%", progress / 10f);
            progressBar.setProgress(progress);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    public static Observable<byte[]> responseToByteStream(Response<ResponseBody> response) {
        return Observable.create(new Observable.OnSubscribe<byte[]>() {
            @Override public void call(Subscriber<? super byte[]> subscriber) {
                int bufferSize = 0x1000;
                byte[] buffer = new byte[bufferSize]; // 4K
                InputStream in = null;
                try {
                    int r;
                    in = response.body().byteStream();
                    in = new BZip2CompressorInputStream(in);
                    while ((r = in.read(buffer)) != -1) {
                        if (r < bufferSize) {
                            subscriber.onNext(Arrays.copyOf(buffer, r));
                        } else {
                            subscriber.onNext(buffer);
                        }
                    }
                    subscriber.onCompleted();
                } catch (IOException e) {
                    subscriber.onError(e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        });
    }

}
