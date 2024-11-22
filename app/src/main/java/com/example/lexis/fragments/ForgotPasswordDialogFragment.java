package com.example.lexis.fragments;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.example.lexis.databinding.FragmentForgotPasswordBinding;

import org.apache.commons.validator.routines.EmailValidator;

import javax.annotation.Nullable;

public class ForgotPasswordDialogFragment extends DialogFragment {
    FragmentForgotPasswordBinding binding;

    public ForgotPasswordDialogFragment() {}

    public interface ForgotPasswordDialogListener {
        void onFinishDialog(String email);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnSend.setOnClickListener(v -> {
            ForgotPasswordDialogListener listener = (ForgotPasswordDialogListener) getActivity();
            if (listener != null) {
                String email = binding.etEmail.getText().toString();
                if (email.isEmpty()) {
                    Toast.makeText(getActivity(), "E-mail cannot be empty!", Toast.LENGTH_SHORT).show();
                    return;
                } else if (!EmailValidator.getInstance().isValid(email)) {
                    Toast.makeText(getActivity(), "E-mail is not valid!", Toast.LENGTH_SHORT).show();
                    return;
                }
                listener.onFinishDialog(email);
                dismiss();
            }
        });
        binding.btnCancel.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
