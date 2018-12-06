package com.android.data.settings.Fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.data.settings.R;


public class FormsFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "FormsFragment";
    private Button button;
    private TextInputEditText txtFirstName,txtLastName,txtEmail, txtSchool, txtCollege,
            txtSpecial, txtCollegeName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_form, container, false);
        Spinner spinner = view.findViewById(R.id.cities_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.cities_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        button = view.findViewById(R.id.btnSubmit);
        button.setOnClickListener(this);

        txtFirstName = view.findViewById(R.id.txtFirstName);
        txtLastName = view.findViewById(R.id.txtLastName);
        txtEmail = view.findViewById(R.id.txtEmail);
        txtSchool = view.findViewById(R.id.txtSchool);
        txtCollege = view.findViewById(R.id.txtCollege);
        txtSpecial = view.findViewById(R.id.txtSpecial);
        txtCollegeName = view.findViewById(R.id.txtCollegeName);

        return view;
    }

    private boolean validateForm() {
        boolean isValid = true;

        if(txtFirstName.getText().toString().isEmpty() ||
                txtLastName.getText().toString().isEmpty()||
                txtEmail.getText().toString().isEmpty() ||
                txtSchool.getText().toString().isEmpty() ||
                txtCollege.getText().toString().isEmpty() ||
                txtSpecial.getText().toString().isEmpty() ||
                txtCollegeName.getText().toString().isEmpty()) {
            isValid = false;
        }

        return isValid;
    }

    private void validateAndProceed() {
        if(validateForm()) {
            Toast.makeText(getActivity(), "All Set ", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), "Enter all the details", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSubmit:
                validateAndProceed();
                break;
        }
    }
}
