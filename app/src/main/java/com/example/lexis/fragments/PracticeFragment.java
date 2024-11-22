package com.example.lexis.fragments;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lexis.R;
import com.example.lexis.fragments.VocabularyFilterDialogFragment.Sort;
import com.example.lexis.adapters.VocabularyAdapter;
import com.example.lexis.databinding.FragmentPracticeBinding;
import com.example.lexis.models.Word;
import com.example.lexis.utilities.RoundedHighlightSpan;
import com.example.lexis.utilities.SwipeDeleteCallback;
import com.example.lexis.utilities.Utils;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class PracticeFragment extends Fragment implements VocabularyFilterDialogFragment.VocabularyFilterDialogListener, VocabularyNewDialogFragment.VocabularyNewDialogListener{

    private static final String TAG = "PracticeFragment";
    private static final int VOCABULARY_FILTER_REQUEST_CODE = 124;
    private static final int VOCABULARY_NEW_REQUEST_CODE = 342;

    public FragmentPracticeBinding binding;
    List<Word> vocabulary;
    VocabularyAdapter adapter;
    ArrayList<String> selectedLanguages;
    boolean starredOnly;
    Sort sortBy;

    public PracticeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPracticeBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        resetVocabularyFilters();
        vocabulary = new ArrayList<>();
        queryVocabulary();

        setUpRecyclerView();
        setUpToolbar();
        setUpPracticeButton();
        setUpSearchBar();
    }

    private void setUpRecyclerView() {
        adapter = new VocabularyAdapter(this, vocabulary);
        binding.rvVocabulary.setAdapter(adapter);
        binding.rvVocabulary.setLayoutManager(new LinearLayoutManager(getActivity()));

        binding.swipeContainer.setOnRefreshListener(() -> {
            adapter.clear();
            resetVocabularyFilters();
            queryVocabulary();
            binding.swipeContainer.setRefreshing(false);
        });
        binding.swipeContainer.setColorSchemeResources(R.color.tiffany_blue,
                R.color.light_cyan,
                R.color.orange_peel,
                R.color.mellow_apricot);

        SwipeDeleteCallback callback = new SwipeDeleteCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(binding.rvVocabulary);

        binding.rvVocabulary.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) binding.rvVocabulary.getLayoutManager();
                if (layoutManager != null) {
                    int position = layoutManager.findLastCompletelyVisibleItemPosition();
                    View lastVisibleView = layoutManager.findViewByPosition(position);
                    int lastItemIndex = binding.rvVocabulary.getAdapter().getItemCount() - 1;
                    if (position != -1 && position == lastItemIndex) {
                        if (Utils.viewsOverlap(lastVisibleView, binding.btnPractice)) {
                            binding.btnPractice.hide();
                        }
                    } else {
                        binding.btnPractice.show();
                    }
                }
            }
        });
    }

    private void setUpToolbar() {
        Utils.setLanguageLogo(binding.toolbar.ivLogo);
        binding.toolbar.ibFilter.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                FragmentManager fm = activity.getSupportFragmentManager();
                ArrayList<String> languageOptions = new ArrayList<>(Utils.getCurrentStudiedLanguages());
                VocabularyFilterDialogFragment dialog = VocabularyFilterDialogFragment.newInstance(
                        languageOptions, selectedLanguages, starredOnly, sortBy);
                dialog.setTargetFragment(PracticeFragment.this, VOCABULARY_FILTER_REQUEST_CODE);
                dialog.show(fm, "fragment_vocabulary_filter");
            }
        });
        binding.toolbar.ibAddNew.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                FragmentManager fm = activity.getSupportFragmentManager();
                VocabularyNewDialogFragment dialog = new VocabularyNewDialogFragment();
                dialog.setTargetFragment(PracticeFragment.this, VOCABULARY_NEW_REQUEST_CODE);
                dialog.show(fm, "fragment_vocabulary_new");
            }
        });
    }

    private void setUpPracticeButton() {
        binding.btnPractice.setOnClickListener(v -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity != null) {
                FragmentManager fragmentManager = activity.getSupportFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, new PracticeIntroFragment())
                        .addToBackStack(null) // add to back stack so we can return to this fragment
                        .commit();
            }
        });
    }

    private void setUpSearchBar() {
        binding.searchBar.setIconifiedByDefault(true);
        binding.searchBar.setOnClickListener(v -> binding.searchBar.onActionViewExpanded());
        binding.searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchVocabulary(query);
                binding.searchBar.clearFocus();
                binding.searchBar.setQuery("", false);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchVocabulary(newText);
                return false;
            }
        });

        ImageView clearButton = binding.searchBar.findViewById(androidx.appcompat.R.id.search_close_btn);
        clearButton.setOnClickListener(v -> {
            if (binding.searchBar.getQuery().length() == 0) {
                binding.searchBar.setIconified(true);
            } else {
                binding.searchBar.setQuery("", false);
                binding.searchBar.clearFocus();
                adapter.clear();
                queryVocabulary();
            }
        });
    }

    private void queryVocabulary() {
        showProgressBar();
        ParseQuery<Word> query = ParseQuery.getQuery(Word.class);
        query.include(Word.KEY_USER);
        query.whereEqualTo(Word.KEY_USER, ParseUser.getCurrentUser());
        query.whereContainedIn(Word.KEY_TARGET_LANGUAGE, selectedLanguages);
        if (starredOnly) {
            query.whereEqualTo(Word.KEY_STARRED, true);
        }
        if (sortBy == Sort.ALPHABETICALLY) {
            query.addAscendingOrder(Word.KEY_TARGET_WORD_SEARCH); // sort alphabetically
        } else {
            query.addDescendingOrder(Word.KEY_CREATED_AT); // sort by date added
        }
        query.findInBackground((words, e) -> {
            if (e != null) {
                Log.e(TAG, "Issue with getting vocabulary", e);
                return;
            }
            hideProgressBar();
            adapter.addAll(words);
            checkVocabularyEmpty(words);
        });
    }

    private void searchVocabulary(String searchQuery) {
        showProgressBar();
        searchQuery = searchQuery.toLowerCase();
        ParseQuery<Word> targetWord = ParseQuery.getQuery(Word.class);
        targetWord.whereStartsWith(Word.KEY_TARGET_WORD_SEARCH, searchQuery);

        ParseQuery<Word> englishWord = ParseQuery.getQuery(Word.class);
        englishWord.whereStartsWith(Word.KEY_ENGLISH_WORD_SEARCH, searchQuery);

        List<ParseQuery<Word>> queries = new ArrayList<>();
        queries.add(targetWord);
        queries.add(englishWord);

        ParseQuery<Word> query = ParseQuery.or(queries);
        query.include(Word.KEY_USER);
        query.whereEqualTo(Word.KEY_USER, ParseUser.getCurrentUser());
        query.whereContainedIn(Word.KEY_TARGET_LANGUAGE, selectedLanguages);
        if (starredOnly) {
            query.whereEqualTo(Word.KEY_STARRED, true);
        }
        if (sortBy == Sort.ALPHABETICALLY) {
            query.addAscendingOrder(Word.KEY_TARGET_WORD_SEARCH);
        } else {
            query.addDescendingOrder(Word.KEY_CREATED_AT);
        }
        query.findInBackground((words, e) -> {
            if (e != null) {
                Log.e(TAG, "Issue with getting vocabulary", e);
                return;
            }
            hideProgressBar();
            adapter.clear();
            adapter.addAll(words);
            checkSearchEmpty(words);
        });
    }

    private void styleEmptyVocabularyPrompt() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            String prompt = activity.getString(R.string.empty_vocabulary_prompt);
            int start = prompt.indexOf("выделенное");
            int end = start + "выделенное".length();

            SpannableString styledPrompt = new SpannableString(prompt);
            styledPrompt.setSpan(new RoundedHighlightSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.tvEmptyPrompt.setText(styledPrompt);
        }
    }

    private void checkSearchEmpty(List<Word> words) {
        binding.tvEmptyPrompt.setText(R.string.no_search_results);
        if (words.isEmpty()) {
            binding.tvEmptyPrompt.setVisibility(View.VISIBLE);
            binding.rvVocabulary.setVisibility(View.INVISIBLE);
        } else {
            binding.tvEmptyPrompt.setVisibility(View.INVISIBLE);
            binding.rvVocabulary.setVisibility(View.VISIBLE);
        }
    }

    public void checkVocabularyEmpty(List<Word> words) {
        styleEmptyVocabularyPrompt();
        if (words.isEmpty()) {
            binding.tvEmptyPrompt.setVisibility(View.VISIBLE);
            binding.rvVocabulary.setVisibility(View.INVISIBLE);
            binding.btnPractice.setVisibility(View.INVISIBLE);
            binding.searchBar.setVisibility(View.INVISIBLE);
            binding.toolbar.ibFilter.setVisibility(View.INVISIBLE);
        } else {
            binding.tvEmptyPrompt.setVisibility(View.INVISIBLE);
            binding.rvVocabulary.setVisibility(View.VISIBLE);
            binding.btnPractice.setVisibility(View.VISIBLE);
            binding.searchBar.setVisibility(View.VISIBLE);
            binding.toolbar.ibFilter.setVisibility(View.VISIBLE);
        }
    }

    private void resetVocabularyFilters() {
        selectedLanguages = new ArrayList<>(Utils.getCurrentStudiedLanguages());
        starredOnly = false;
        sortBy = Sort.DATE;
    }

    @Override
    public void onFinishDialog(ArrayList<String> selectedLanguages, boolean starredOnly, Sort sortBy) {
        this.selectedLanguages = selectedLanguages;
        this.starredOnly = starredOnly;
        this.sortBy = sortBy;

        adapter.clear();
        queryVocabulary();
    }

    @Override
    public void onFinishDialog(String targetLanguage, String targetWord, String englishWord) {
        Utils.addWordToDatabase(targetLanguage, targetWord, englishWord, binding.rvVocabulary);
    }

    public void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
    }

    public void hideProgressBar() {
        binding.progressBar.setVisibility(View.INVISIBLE);
    }
}