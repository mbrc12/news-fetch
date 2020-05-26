package io.mbrc.newsfetch.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.mbrc.newsfetch.util.Hasher;
import io.mbrc.newsfetch.util.KeyValuePair;
import io.mbrc.newsfetch.util.NewsType;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static io.mbrc.newsfetch.util.KeyValuePair.pairOf;


@Slf4j
@Component
public class ApiClient implements DisposableBean {

    private final ApiConfig apiConfig;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Hasher hasher;

    private ApiClient(@Autowired ApiConfig apiConfig, @Autowired Gson gson, @Autowired Hasher hasher) {
        this.apiConfig = apiConfig;
        this.httpClient = new OkHttpClient();
        this.gson = gson;
        this.hasher = hasher;
    }

    HttpUrl buildURL(QueryParameters params) {
        HttpUrl.Builder builder = new HttpUrl.Builder()
                .scheme(apiConfig.getScheme())
                .host(apiConfig.getHost())
                .encodedPath("/" + apiConfig.getPath());

        String query = params.getQuery();
        QueryParameters.SortBy sortBy = params.getSortBy();
        QueryParameters.SortOrder sortOrder = params.getSortOrder();
        int limit = params.getLimit();

        if (query != null) builder.addQueryParameter("query", query);
        if (sortBy != null) builder.addQueryParameter("sortBy", sortBy.toString());
        if (sortOrder != null) builder.addQueryParameter("sortOrder", sortOrder.toString());
        if (limit > 100 || limit < 0) throw new IllegalArgumentException();
        if (limit > 0) builder.addQueryParameter("limit", Integer.toString(limit));

        return builder.build();
    }

    public Call requestBuilder(QueryParameters params) {
        HttpUrl url = buildURL(params);
        Request request = new Request.Builder()
                .addHeader("Authorization", apiConfig.getApiToken())
                .url(url)
                .build();
        return httpClient.newCall(request);
    }

    // We use a functional style here. The work of responseConsumer is to use two
    // functions, onSuccess and onRejectedRequest, to produce a new function that
    // works on a Response object, and invokes them suitably, after pre-processing
    // the Response object. This facilitates an unified approach of implementing
    // request and requestSync, with the same signature. Their implementations are
    // also considerably simplified.

    public Consumer<Response> responseConsumer(
            final Consumer<List<KeyValuePair<String, NewsType>>> onSuccess,
            final BiConsumer<Integer, BufferedSource> onRejectedRequest
    ) {
        return response -> {
            if (response.isSuccessful()) {
                try {
                    String body = response.body().source().readString(Charset.defaultCharset());
                    List<KeyValuePair<String, NewsType>> data = StreamSupport
                            .stream(gson.fromJson(body, JsonArray.class).spliterator(),
                                    false)
                            .map(jsonElement -> {
                                String jsonString = jsonElement.toString();
                                NewsType news = gson.fromJson(jsonString, NewsType.class);
                                String hash = hasher.hash(jsonString);
                                return pairOf(hash, news);
                            })
                            .collect(Collectors.toList());

                    onSuccess.accept(data);
                } catch (IOException e) {
                    onRejectedRequest.accept(response.code(), response.body().source());
                } catch (NullPointerException e) {
                    log.error("Response body is null.");
                    e.printStackTrace();
                }
            } else {
                try {
                    onRejectedRequest.accept(response.code(), response.body().source());
                } catch (NullPointerException e) {
                    log.error("Response body is null.");
                    e.printStackTrace();
                }
            }
        };
    }

    public void request(QueryParameters params,
                        final Consumer<List<KeyValuePair<String, NewsType>>>
                                onSuccess,
                        final BiConsumer<Integer, BufferedSource> onRejectedRequest,
                        final Consumer<IOException> onFailure) {

        Call request = requestBuilder(params);
        request.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                onFailure.accept(e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                responseConsumer(onSuccess, onRejectedRequest).accept(response);
            }
        });
    }

    // Same as request, but waits till request is done.
    // Rudimentary implementation. Revise later.
    public void requestSync(QueryParameters params,
                            final Consumer<List<KeyValuePair<String, NewsType>>> onSuccess,
                            final BiConsumer<Integer, BufferedSource> onRejectedRequest,
                            final Consumer<IOException> onFailure) {
        try (Response response = requestBuilder(params).execute()) {
            responseConsumer(onSuccess, onRejectedRequest).accept(response);
        } catch (IOException e) {
            onFailure.accept(e);
        }
    }

    // Needed to safely shutdown everything when all is done with
    // Otherwise this goes on infinitely
    @Override
    public void destroy() {
        this.httpClient.dispatcher().executorService().shutdown();
    }
}
