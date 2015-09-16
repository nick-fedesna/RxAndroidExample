package com.example.rxandroid.api;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import com.squareup.okhttp.*;
import retrofit.*;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.http.*;
import rx.Observable;
import timber.log.Timber;

public class ExampleApi {

    public static class Credentials {

        String username;
        String password;
    }

    public static class UserToken {

        String token;
    }

    public static class UserProfile {
        String username;
        String name;
        String birthdate;
    }

    interface RestInterface {

        @POST Call<UserToken> loginUser(@Body Credentials credentials);

        @GET Call<UserProfile> getUserProfile(@Header("Authenticaion") String token);

        @POST Observable<UserToken> loginUserRx(@Body Credentials credentials);

        @GET Observable<UserProfile> getUserProfileRx(@Header("Authenticaion") String token);

        @GET @Streaming Observable<Response<ResponseBody>> getFuzzworkFile(@Url String url);
    }

    private static ExampleApi    sInstance;
    private static Retrofit      sRetrofit;
    private static OkHttpClient  sOkClient;
    private static RestInterface sApi;

    private ExampleApi() {
    }

    public static ExampleApi getApi(Context context) {
        if (sInstance == null) {
            sRetrofit = new Retrofit.Builder()
                    .baseUrl("https://www.fuzzwork.co.uk/dump/")
                    .addConverterFactory(new ByteConverterFactory())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .client(getOkClient(context))
                    .build();

            sApi = sRetrofit.create(RestInterface.class);
            sInstance = new ExampleApi();
        }
        return sInstance;
    }

    public static OkHttpClient getOkClient(Context context) {
        if (sOkClient == null) {
            sOkClient = new OkHttpClient();
            sOkClient.setCache(new Cache(context.getCacheDir(), 25 * 1024 * 1024));
            sOkClient.interceptors().add(chain -> {
                Request request = chain.request();

                long t1 = System.nanoTime();
                Timber.i("---> %s %n%s", request.url(), request.headers());

                com.squareup.okhttp.Response response = chain.proceed(request);

                long t2 = System.nanoTime();
                Timber.i("<--- %d %s : %s in %.1fms%n%s", response.code(), response.message(),
                         response.request().url(), (t2 - t1) / 1e6d,
                         response.headers());

                return response;
            });
        }
        return sOkClient;
    }

    private static class ByteConverterFactory implements Converter.Factory {

        @Override
        public Converter<InputStream> get(Type type) {
            return new Converter<InputStream>() {
                @Override
                public InputStream fromBody(ResponseBody body) throws IOException {
                    return body.byteStream();
                }

                @Override
                public RequestBody toBody(InputStream value) {
                    return null;
                }
            };
        }
    }

    public Observable<Response<ResponseBody>> getFile(String file) {
        return sApi.getFuzzworkFile(file);
    }


    public Observable<Response<ResponseBody>> getDatabase() {
        return sApi.getFuzzworkFile("sqlite-latest.sqlite.bz2");
    }

    public Observable<Response<ResponseBody>> getDatabaseMD5() {
        return sApi.getFuzzworkFile("sqlite-latest.sqlite.bz2.md5");
    }

    public Observable<Response<ResponseBody>> getTypes() {
        return sApi.getFuzzworkFile("latest/invTypes.sql.bz2");
    }

    private void exampleOkHttpNested(Credentials credentials) {

        sApi.loginUser(credentials).enqueue(new Callback<UserToken>() {
            @Override
            public void onResponse(Response<UserToken> response) {
                sApi.getUserProfile(response.body().token).enqueue(new Callback<UserProfile>() {
                    @Override
                    public void onResponse(Response<UserProfile> response) {
                        // do something with Credentials
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        // notify error getting Credentials
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                // notify login failure
            }
        });
    }

    private void exampleRxAndroid(Credentials credentials) {
        sApi.loginUserRx(credentials)
                .map(response -> response.token)
                .doOnNext(this::saveUserToken)
                .flatMap(sApi::getUserProfileRx)
                .subscribe(this::saveUserProfile,
                           this::handleError);
    }

    private void saveUserToken(String token) {
        // save token
    }
    private void saveUserProfile(UserProfile profile) {
        // credentials logged in logic

    }

    private void handleError(Throwable throwable) {
        // notify some error
    }
}
