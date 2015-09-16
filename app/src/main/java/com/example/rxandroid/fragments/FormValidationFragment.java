package com.example.rxandroid.fragments;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

import java.util.HashSet;

import butterknife.*;
import com.example.rxandroid.R;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.trello.rxlifecycle.components.support.RxFragment;
import rx.Observable;
import timber.log.Timber;

import static android.util.Patterns.EMAIL_ADDRESS;

public class FormValidationFragment extends RxFragment {

    @Bind(R.id.input_layout_firstname)        TextInputLayout firstnameInput;
    @Bind(R.id.input_firstname)               EditText        firstname;
    @Bind(R.id.input_layout_lastname)         TextInputLayout lastnameInput;
    @Bind(R.id.input_lastname)                EditText        lastname;
    @Bind(R.id.input_layout_email)            TextInputLayout emailInput;
    @Bind(R.id.input_email)                   EditText        email;
    @Bind(R.id.input_layout_password)         TextInputLayout pwd1Input;
    @Bind(R.id.input_password)                EditText        pwd1;
    @Bind(R.id.input_layout_password_confirm) TextInputLayout pwd2Input;
    @Bind(R.id.input_password_confirm)        EditText        pwd2;
    @Bind(R.id.button_signup)                 Button          register;
    @Bind(R.id.registration_complete)         TextView        registerOk;

    private HashSet<Integer> mChangedInputIds = new HashSet<>(5);

    public static FormValidationFragment newInstance() {
        FormValidationFragment fragment = new FormValidationFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public FormValidationFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_form, container, false);
        ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Observable<Boolean> firstnameInvalid = RxTextView.textChangeEvents(firstname)
                .doOnNext(tv -> mChangedInputIds.add(tv.view().getId()))
                .map(tv -> tv.text().length() == 0).distinctUntilChanged()
                .filter(v -> mChangedInputIds.contains(R.id.input_firstname) ? v : false)
                .doOnNext(v -> Timber.d("Invalid Lastname: %B", v));

        Observable<Boolean> lastnameInvalid = RxTextView.textChangeEvents(lastname)
                .doOnNext(tv -> mChangedInputIds.add(tv.view().getId()))
                .map(s -> s.text().length() == 0).distinctUntilChanged()
                .filter(v -> mChangedInputIds.contains(R.id.input_lastname) ? v : false);

        Observable<Boolean> emailInvalid = RxTextView.textChanges(email)
                .map(s -> !EMAIL_ADDRESS.matcher(s).matches()).distinctUntilChanged();
        Observable<Boolean> pwd1Invalid = RxTextView.textChanges(pwd1)
                .map(text -> text.length() <= 6).distinctUntilChanged();
        Observable<Boolean> pwd2Invalid = RxTextView.textChanges(pwd2)
                .map(text -> !TextUtils.equals(text, pwd1.getText())).distinctUntilChanged();

        firstnameInvalid.map(e -> e ? "Firstname must not be empty!" : null)
                .subscribe(firstnameInput::setError);
        firstnameInvalid.subscribe(firstnameInput::setErrorEnabled);

        lastnameInvalid.map(e -> e ? "Lastname must not be empty!" : null)
                .subscribe(lastnameInput::setError);
        lastnameInvalid.subscribe(lastnameInput::setErrorEnabled);

        emailInvalid.map(e -> e ? "Must be a valid email address!" : null)
                .subscribe(emailInput::setError);
        emailInvalid.subscribe(emailInput::setErrorEnabled);

        pwd1Invalid.map(e -> e ? "Password must be more than six characters!" : null)
                .subscribe(pwd1Input::setError);
        pwd1Invalid.subscribe(pwd1Input::setErrorEnabled);

        pwd2Invalid.map(e -> e ? "Passwords do not match!" : null)
                .subscribe(pwd2Input::setError);
        pwd2Invalid.subscribe(pwd2Input::setErrorEnabled);

        Observable<Boolean> pwdInvalid = Observable.combineLatest(pwd1Invalid, pwd2Invalid,
                                                                  (a, b) -> a || b);

        Observable.combineLatest(firstnameInvalid, lastnameInvalid, emailInvalid, pwdInvalid,
                                 (fname, lname, email, pwd) -> !fname && !lname && !email && !pwd)
                .subscribe(register::setEnabled);
    }

    @OnClick(R.id.button_signup) void onRegisterClicked() {
        registerOk.setText("Registration Completed OK.");
    }
}
