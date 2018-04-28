package chandoras.ankit.awesome.blogit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private EditText mEmailField,mPasswordField,mNameField;
    private TextView mTitle;
    private Button mSignUpButton;


    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;

    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        progressDialog =  new ProgressDialog(this);
        progressDialog.setMessage("Signing up...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        getSupportActionBar().setTitle("Create a new account");

        mEmailField = (EditText) findViewById(R.id.register_email);
        mTitle = (TextView)findViewById(R.id.register_tv);
        mPasswordField = (EditText) findViewById(R.id.register_password);
        mNameField = (EditText) findViewById(R.id.register_name);
        mSignUpButton = (Button) findViewById(R.id.register_sign_up_button);



        mAuth = FirebaseAuth.getInstance();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");

        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewUser();
            }
        });
    }

    private void createNewUser() {
        String email = mEmailField.getText().toString().trim();
        String password = mPasswordField.getText().toString().trim();
        final String name = mNameField.getText().toString().trim();

        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(name)) {

            if (password.length() >= 6){

                progressDialog.show();
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        progressDialog.dismiss();

                        String userId = mAuth.getCurrentUser().getUid();

                        DatabaseReference mCurrentUserDatabase = mUserDatabase.child(userId);

                        mCurrentUserDatabase.child("name").setValue(name);
                        mCurrentUserDatabase.child("image").setValue("default");


                        Intent mainIntent = new Intent(RegisterActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainIntent);

                    } else {

                        progressDialog.dismiss();
                        Toast.makeText(RegisterActivity.this, "Check your email and password", Toast.LENGTH_SHORT).show();

                    }

                }
            });
        }else {
                Toast.makeText(this, "Password lenght must equal or grater than 6", Toast.LENGTH_SHORT).show();
            }

        }else {
            Toast.makeText(this, "Input fields must not be empty", Toast.LENGTH_SHORT).show();
        }


    }
}
