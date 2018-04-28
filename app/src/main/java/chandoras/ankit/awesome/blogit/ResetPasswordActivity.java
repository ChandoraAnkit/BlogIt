package chandoras.ankit.awesome.blogit;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
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
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private Button mSubmitButton;
    private TextView mTitle;
    private EditText mEmailField;

    private ProgressDialog progressDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        getSupportActionBar().setTitle("Reset your password");


        mSubmitButton = (Button)findViewById(R.id.reset_submit_btn);
        mTitle = (TextView)findViewById(R.id.reset_title);
        mEmailField = (EditText)findViewById(R.id.reset_email);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Wait...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);


        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = mEmailField.getText().toString().trim();
                if (!TextUtils.isEmpty(email)) {
                    progressDialog.show();
                    FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        progressDialog.dismiss();
//
                                        AlertDialog.Builder builder;
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                            builder = new AlertDialog.Builder(ResetPasswordActivity.this, R.style.MyDialogTheme);
                                        } else {
                                            builder = new AlertDialog.Builder(ResetPasswordActivity.this);
                                        }



                                                builder.setMessage("Password reset link has been sent to your entered email and now you can reset your password and login again. Have a good luck :)")

                                                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        finish();
                                                    }
                                                })
                                                .setIcon(R.drawable.blog)
                                                .show();



                                    }else {
                                        progressDialog.dismiss();
                                        Toast.makeText(ResetPasswordActivity.this, "Check your email and try again", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }else {
                    Toast.makeText(ResetPasswordActivity.this, "Input field must not be empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
