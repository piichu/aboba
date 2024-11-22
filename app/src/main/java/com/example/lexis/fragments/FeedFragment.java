package com.example.lexis.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.codepath.asynchttpclient.AsyncHttpClient;
import com.codepath.asynchttpclient.RequestParams;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.example.lexis.R;
import com.example.lexis.adapters.ArticlesAdapter;
import com.example.lexis.databinding.FragmentFeedBinding;
import com.example.lexis.models.Article;
import com.example.lexis.utilities.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import okhttp3.Headers;

public class FeedFragment extends Fragment {

    FragmentFeedBinding binding;
    List<Article> articles;
    ArticlesAdapter adapter;

    private static final String TAG = "FeedFragment";
    private static final String TOP_HEADLINES_URL = "https://newsapi.org/v2/top-headlines";
    private static final String SHORT_STORIES_URL = "https://shortstories-api.herokuapp.com/stories";
    private static final String WIKI_ARTICLE_URL = "https://en.wikipedia.org/w/api.php";
    private static final String WIKI_TOP_ARTICLES_URL = "https://wikimedia.org/api/rest_v1/metrics/pageviews/top/en.wikipedia.org/all-access/%s/%s/%s";
    private static final int NUM_STORIES_TO_FETCH = 10;

    private int numCallsCompleted = 0;

    public FeedFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (articles == null) {
            articles = new ArrayList<>();
            showProgressBar();
            binding.rvArticles.setClickable(false);
            fetchContent();
        }

        setUpRecyclerView();
        Utils.setLanguageLogo(binding.toolbar.ivLogo);
    }

    private void setUpRecyclerView() {
        adapter = new ArticlesAdapter(this, articles);
        binding.rvArticles.setAdapter(adapter);
        binding.rvArticles.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.swipeContainer.setOnRefreshListener(() -> {
            adapter.clear();
            numCallsCompleted = 0;
            binding.rvArticles.setClickable(false);
            fetchContent();
        });
        binding.swipeContainer.setColorSchemeResources(R.color.tiffany_blue,
                R.color.light_cyan,
                R.color.orange_peel,
                R.color.mellow_apricot);
    }

    private void fetchContent() {
        fetchTopWikipediaArticles(Utils.getYesterday(), false);
        fetchTopHeadlines(Arrays.asList("bbc-news", "time", "wired", "the-huffington-post"));
        fetchShortStories(NUM_STORIES_TO_FETCH);
    }

    private void fetchTopWikipediaArticles(Calendar date, Boolean allDays) {
        String year = String.valueOf(date.get(Calendar.YEAR));
        String month = String.format(Locale.getDefault(), "%02d", date.get(Calendar.MONTH) + 1);
        String day = String.format(Locale.getDefault(), "%02d", date.get(Calendar.DAY_OF_MONTH));
        if (allDays) day = "all-days";

        AsyncHttpClient client = new AsyncHttpClient();
        String formattedUrl = String.format(WIKI_TOP_ARTICLES_URL, year, month, day);
        client.get(formattedUrl, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONObject jsonObject = json.jsonObject;
                try {
                    JSONObject items = jsonObject.getJSONArray("items").getJSONObject(0);
                    JSONArray jsonArticles = items.getJSONArray("articles");

                    for (int i = 2; i < 22; i++) {
                        JSONObject jsonArticle = jsonArticles.getJSONObject(i);
                        String title = jsonArticle.getString("article");
                        fetchWikipediaArticle(title, i);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure to fetch article", throwable);
            }
        });

    }

    private void fetchWikipediaArticle(String title, int index) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("action", "query");
        params.put("format", "json");
        params.put("titles", title);
        params.put("prop", "extracts|info");
        params.put("inprop", "url");
        params.put("explaintext", true);
        params.put("exintro", true);

        client.get(WIKI_ARTICLE_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONObject jsonObject = json.jsonObject;
                try {
                    if (!title.startsWith("Wikipedia:") && !title.startsWith("Special:")) {
                        JSONObject pages = jsonObject
                                .getJSONObject("query")
                                .getJSONObject("pages");
                        JSONObject articleObject = pages.getJSONObject(pages.keys().next());

                        String source = "Wikipedia";
                        String url = articleObject.getString("fullurl");
                        String title = articleObject.getString("title");
                        String intro = articleObject.getString("extract");
                        intro = intro.replace("\n", "\n\n");

                        Article article = new Article(title, intro, source, url);
                        articles.add(article);
                        adapter.notifyItemInserted(articles.size() - 1);
                    }

                    if (index == 21) {
                        numCallsCompleted++;
                        checkIfFetchingCompleted();
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "JSON Exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.d(TAG, "onFailure to fetch Wikipedia article");
            }
        });
    }

    private void fetchTopHeadlines(List<String> sources) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("apiKey", "442d09e979f34d49a66dadb7cfe46b5c");
        params.put("sources", TextUtils.join(",", sources));

        client.get(TOP_HEADLINES_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONObject jsonObject = json.jsonObject;
                try {
                    JSONArray articlesArray = jsonObject.getJSONArray("articles");
                    for (int i = 0; i < articlesArray.length(); i++) {
                        JSONObject articleObject = articlesArray.getJSONObject(i);

                        String title = articleObject.getString("title");
                        String content = articleObject.getString("content");
                        int truncatedIndex = content.indexOf("[+");
                        if (truncatedIndex != -1) {
                            content = content.substring(0, truncatedIndex);
                        }
                        String source = articleObject.getJSONObject("source").getString("name");
                        String url = articleObject.getString("url");

                        Article article = new Article(title, content, source, url);
                        articles.add(article);
                        adapter.notifyItemInserted(articles.size() - 1);
                    }

                    numCallsCompleted++;
                    checkIfFetchingCompleted();
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure to fetch top headlines", throwable);
            }
        });
    }

    private void fetchShortStories(int n) {
        AsyncHttpClient client = new AsyncHttpClient();
        Set<Integer> storiesAdded = new HashSet<>();

        client.get(SHORT_STORIES_URL, null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONArray jsonArray = json.jsonArray;
                try {
                    Random rand = new Random();
                    for (int i = 0; i < n; i++) {
                        // generate random index for story we haven't added yet
                        int index = rand.nextInt(jsonArray.length());
                        while (storiesAdded.contains(index)) index = rand.nextInt(jsonArray.length());
                        storiesAdded.add(index);

                        JSONObject story = jsonArray.getJSONObject(index);
                        String title = story.getString("title");
                        String content = story.getString("story") + " " + story.getString("moral");
                        String source = "Short stories";

                        Article article = new Article(title, content, source, null);
                        articles.add(article);
                        adapter.notifyItemInserted(articles.size() - 1);
                    }

                    numCallsCompleted++;
                    checkIfFetchingCompleted();
                } catch (JSONException e) {
                    Log.e(TAG, "JSON Exception", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure to fetch short stories", throwable);
            }
        });
    }

    private void checkIfFetchingCompleted() {
        // shuffle articles list for variety in source ordering
        adapter.shuffle();
        if (numCallsCompleted == 2) {
            hideProgressBar();
            binding.swipeContainer.setRefreshing(false);
            // user can only click on articles after everything has loaded to avoid crashes
            binding.rvArticles.setClickable(true);
        }
    }

    public void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        binding.progressBar.setVisibility(View.INVISIBLE);
    }
}