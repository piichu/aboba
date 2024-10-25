package com.example.lexis.adapters;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lexis.databinding.ItemVocabularyBinding;
import com.example.lexis.fragments.PracticeFragment;
import com.example.lexis.models.Word;
import com.example.lexis.utilities.Utils;
import com.google.android.material.snackbar.Snackbar;
import com.like.LikeButton;
import com.like.OnLikeListener;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VocabularyAdapter extends RecyclerView.Adapter<VocabularyAdapter.VocabularyViewHolder> {

    List<Word> vocabulary;
    PracticeFragment fragment;

    public VocabularyAdapter(PracticeFragment fragment, List<Word> vocabulary) {
        this.fragment = fragment;
        this.vocabulary = vocabulary;
    }

    @NonNull
    @Override
    public VocabularyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVocabularyBinding binding = ItemVocabularyBinding.inflate(LayoutInflater.from(fragment.getActivity()), parent, false);
        return new VocabularyViewHolder(binding, fragment.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull VocabularyViewHolder holder, int position) {
        Word word = vocabulary.get(position);
        holder.bind(word);
    }

    @Override
    public int getItemCount() {
        return vocabulary.size();
    }

    public void addAll(List<Word> words) {
        vocabulary.addAll(words);
        notifyDataSetChanged();
    }

    public void insertAt(int position, Word word) {
        vocabulary.add(position, word);
        notifyItemInserted(position);
        fragment.checkVocabularyEmpty(vocabulary);
    }

    public void clear() {
        vocabulary.clear();
        notifyDataSetChanged();
    }

    /*
    Delete word at given position from user's vocabulary.
    */
    public void deleteWord(int position) {
        Word word = vocabulary.get(position);
        String targetWord = word.getTargetWord();
        word.deleteInBackground();
        vocabulary.remove(position);
        notifyItemRemoved(position);
        fragment.checkVocabularyEmpty(vocabulary);

        Snackbar.make(fragment.binding.btnPractice, "Deleted word: " + targetWord, Snackbar.LENGTH_LONG)
                .setAction("Undo", view -> {
                    Word newWord = Word.copyWord(word);
                    newWord.saveInBackground();

                    vocabulary.add(position, word);
                    notifyItemInserted(position);
                    fragment.binding.rvVocabulary.scrollToPosition(position);
                }).show();
    }

    public class VocabularyViewHolder extends RecyclerView.ViewHolder {
        ItemVocabularyBinding binding;
        private Context context;
        public VocabularyViewHolder(@NonNull ItemVocabularyBinding binding, Context context) {
            super(binding.getRoot());
            this.binding = binding;
            this.context=context;
        }

        /*
        Bind Word data into the ViewHolder.
        */
        public void bind(Word word) {
            binding.tvTargetLanguage.setText(word.getTargetWord());
            binding.tvEnglish.setText(word.getEnglishWord());
            binding.tvFlag.setText(Utils.getFlagEmoji(word.getTargetLanguage()));

            binding.btnStar.setLiked(word.getIsStarred());
            binding.btnStar.setOnLikeListener(new OnLikeListener() {
                @Override
                public void liked(LikeButton likeButton) {
                    toggleStarred(word);
                }

                @Override
                public void unLiked(LikeButton likeButton) {
                    toggleStarred(word);
                }
            });
            binding.btnSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    speakWord(word);
                }
            });
        }

        /*
        Toggle whether the word is starred and save to Parse.
        */
        private void toggleStarred(Word word) {
            word.toggleIsStarred();
            word.saveInBackground();
        }

        private void speakWord(Word word){
            OkHttpClient client = new OkHttpClient();
            String apiKey = "504e91695b8441dfa4144fbb08f1652f";
            String url = "http://api.voicerss.org/";
            HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
            urlBuilder.addQueryParameter("key", apiKey);
            urlBuilder.addQueryParameter("src", word.getTargetWord());
            urlBuilder.addQueryParameter("f", "48khz_16bit_stereo");
            urlBuilder.addQueryParameter("hl", word.getTargetLanguage() + '-' + word.getTargetLanguage());
            String finalUrl = urlBuilder.build().toString();
            Request request = new Request.Builder()
                    .url(finalUrl)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("VoiceAPI", "Request failed: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        File tempFile = new File(context.getCacheDir(),"audio.wav");

                        // Записываем InputStream в временный файл
                        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                        }

                        // Воспроизведение аудио с помощью MediaPlayer
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(tempFile.getAbsolutePath());
                        mediaPlayer.prepare();
                        mediaPlayer.setVolume(1, 1);
                        mediaPlayer.start();

                        // Удаление временного файла после воспроизведения
                        mediaPlayer.setOnCompletionListener(mp -> {
                            mp.release();
                            tempFile.delete(); // Удаляем временный файл
                        });
                    } else {
                        Log.e("VoiceAPI", "Response failed: " + response.message());
                    }
                }
            });
        }
    }
}
