package com.example.lexis.fragments;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import javax.annotation.Nullable;

import com.example.lexis.R;
import com.example.lexis.databinding.FragmentArticleBinding;
import com.example.lexis.models.Article;
import com.example.lexis.utilities.RoundedHighlightSpan;
import com.example.lexis.utilities.Utils;

import org.parceler.Parcels;

public class ArticleFragment extends Fragment {

    FragmentArticleBinding binding;
    Article article;

    public ArticleFragment() {}

    public static ArticleFragment newInstance(Article article) {
        ArticleFragment frag = new ArticleFragment();
        Bundle args = new Bundle();
        args.putParcelable("article", Parcels.wrap(article));
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentArticleBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            article = Parcels.unwrap(getArguments().getParcelable("article"));

            SpannableStringBuilder styledContent = styleTranslatedWords(article);
            displayArticle(styledContent);
            setUpToolbar();
        }
    }

    private void displayArticle(SpannableStringBuilder content) {
        binding.tvTitle.setText(article.getTitle());
        binding.tvBody.setText(content);
        // needed so that translated words are clickable
        binding.tvBody.setMovementMethod(LinkMovementMethod.getInstance());
        if (article.getUrl() != null) {
            binding.tvUrl.setVisibility(View.VISIBLE);
            binding.tvUrl.setText(Html.fromHtml(String.format("<a href=\"%s\">Read more on %s...</a>", article.getUrl(), article.getSource())));
            binding.tvUrl.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            binding.tvUrl.setVisibility(View.GONE);
        }
    }

    private void setUpToolbar() {
        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        if (activity != null) {
            activity.setSupportActionBar(binding.toolbar.getRoot());
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle("");
            }

            binding.toolbar.getRoot().setNavigationIcon(R.drawable.back_arrow);
            Drawable navigationIcon = binding.toolbar.getRoot().getNavigationIcon();
            if (navigationIcon != null) {
                navigationIcon.setTint(getResources().getColor(R.color.black));
            }
            binding.toolbar.getRoot().setNavigationOnClickListener(v -> activity.onBackPressed());
        }
    }

    private SpannableStringBuilder styleTranslatedWords(Article article) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String[] words = article.getWordList();

        int curr = 0;
        for (int i = 0; i < words.length; i++) {
            int start = spannableStringBuilder.length();
            if (curr < article.getTranslatedIndices().size() && article.getTranslatedIndices().get(curr) == i) {
                final int originalWordIndex = curr;
                String targetLanguage = words[i];
                String englishWithPunctuation = article.getOriginalWords().get(originalWordIndex);
                int[] targetStartEnd = new int[2];
                String english = Utils.stripPunctuation(englishWithPunctuation, targetStartEnd);

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View textView) {
                        Rect wordPosition = Utils.getClickedWordPosition((TextView) textView, this);
                        launchWordDialog(targetLanguage, english, wordPosition.left, wordPosition.top, wordPosition.width());
                        Utils.addWordToDatabase(Utils.getCurrentTargetLanguage(), targetLanguage, english, null);
                    }

                    @Override
                    public void updateDrawState(TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(getResources().getColor(R.color.black));
                        ds.setUnderlineText(false);
                    }
                };

                String translationWithPunctuation = (
                        englishWithPunctuation.substring(0, targetStartEnd[0]) +
                                words[i] +
                                englishWithPunctuation.substring(targetStartEnd[1]));
                spannableStringBuilder.append(translationWithPunctuation).append(" ");

                int actualWordStart = start + targetStartEnd[0];
                int actualWordEnd = actualWordStart + words[i].length();

                spannableStringBuilder.setSpan(clickableSpan, actualWordStart, actualWordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableStringBuilder.setSpan(new RoundedHighlightSpan(), actualWordStart, actualWordEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                curr++;
            } else {
                spannableStringBuilder.append(words[i]).append(" ");
            }
        }

        return spannableStringBuilder;
    }

    private void launchWordDialog(String targetLanguage, String english, int left, int top, int width) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            FragmentManager fm = activity.getSupportFragmentManager();
            WordDialogFragment wordDialogFragment = WordDialogFragment.newInstance(
                    targetLanguage, english, left, top, width);
            wordDialogFragment.show(fm, "fragment_dialog");
        }
    }
}