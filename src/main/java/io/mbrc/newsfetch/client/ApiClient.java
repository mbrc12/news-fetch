package io.mbrc.newsfetch.client;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


@Slf4j
@Component
public class ApiClient implements DisposableBean {

    private final ApiConfig apiConfig;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private ApiClient(ApiConfig apiConfig, Gson gson) {
        this.apiConfig = apiConfig;
        this.httpClient = new OkHttpClient();
        this.gson = gson;
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

    public void request(QueryParameters params,
                        final Consumer<List<NewsType>> onSuccess,
                        final BiConsumer<Integer, BufferedSource> onRejectedRequest,
                        final Consumer<IOException> onFailure) {

        Call request = requestBuilder(params);
        request.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                onFailure.accept(e);
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    try {
                        String body = response.body().source().readString(Charset.defaultCharset());
                        List<NewsType> data = gson.fromJson(body, NewsType.newsCollectionType);
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
            }
        });
    }

    // Needed to safely shutdown everything when all is done with
    // Otherwise this goes on infinitely
    @Override
    public void destroy() {
        this.httpClient.dispatcher().executorService().shutdown();
    }
}