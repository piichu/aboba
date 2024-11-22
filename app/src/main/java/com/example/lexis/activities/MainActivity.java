package com.example.lexis.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.example.lexis.R;
import com.example.lexis.databinding.ActivityMainBinding;
import com.example.lexis.fragments.FeedFragment;
import com.example.lexis.fragments.PracticeFlashcardFragment;
import com.example.lexis.fragments.PracticeFragment;
import com.example.lexis.fragments.ProfileFragment;
import com.example.lexis.fragments.WordSearchFragment;
import com.example.lexis.models.WordSearch;
import com.example.lexis.utilities.TranslateUtils;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        TranslateUtils.getTranslateService(this);

        binding.bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            switchToSelectedFragment(item);
            return true;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.action_home);
    }

    private void switchToSelectedFragment(MenuItem item) {
        final FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;

        switch (item.getItemId()) {
            case R.id.action_home:
                fragment = new FeedFragment();
                break;
            case R.id.action_practice:
                fragment = new PracticeFragment();
                break;
            case R.id.action_profile:
            default:
                try {
                    fragment = ProfileFragment.newInstance(ParseUser.getCurrentUser());
                } catch (Exception e) {
                    Log.e("FragmentError", "Error creating ProfileFragment", e);

                }
                break;
        }

        if (fragment != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onBackPressed() {
        WordSearchFragment wordSearchFragment = (WordSearchFragment) getSupportFragmentManager().findFragmentByTag("WordSearchFragment");
        PracticeFlashcardFragment flashcardFragment = (PracticeFlashcardFragment) getSupportFragmentManager().findFragmentByTag("FlashcardFragment");
        if (wordSearchFragment != null && wordSearchFragment.isVisible()) {
            wordSearchFragment.returnToPracticeTab();
        } else if (flashcardFragment != null && flashcardFragment.isVisible()) {
            flashcardFragment.returnToPracticeTab();
        } else {
            super.onBackPressed();
        }
    }
}