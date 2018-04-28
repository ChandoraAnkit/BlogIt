package chandoras.ankit.awesome.blogit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

public class SetUpActivity extends AppCompatActivity {
    private static final int GALLERY_INTENT = 2;

    private ImageView mUserImageView;
    private EditText mNameField;
    private ProgressDialog progressDialog;
    private Button mSubmitButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mUserDatabase;
    private StorageReference mStorage;

    private Uri imageUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_up);
        progressDialog = new ProgressDialog(this);

        Typeface mont = Typeface.createFromAsset(getAssets(), "fonts/Montserrat-Regular.ttf");

        mUserImageView = (ImageView)findViewById(R.id.setup_image);
        mNameField = (EditText)findViewById(R.id.setup_name);

        mSubmitButton = (Button)findViewById(R.id.setup_submit_btn);


        mNameField.setTypeface(mont);
        mSubmitButton.setTypeface(mont);
        progressDialog.setMessage("Updating your account details...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mAuth = FirebaseAuth.getInstance();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mStorage = FirebaseStorage.getInstance().getReference().child("Blog_photos");

        mUserDatabase.child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(final DataSnapshot dataSnapshot) {

                    String name = (String) dataSnapshot.child("name").getValue();
                    final String image = (String) dataSnapshot.child("image").getValue();

                    mNameField.setText(name);

                    Picasso.with(SetUpActivity.this).load(image).placeholder(R.drawable.default_avatar).networkPolicy(NetworkPolicy.OFFLINE).into(mUserImageView, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {String name = (String) dataSnapshot.child("name").getValue();
                            Picasso.with(SetUpActivity.this).load(image).placeholder(R.drawable.default_avatar).into(mUserImageView);
                        }
                    });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUserImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent =  new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_INTENT);
            }
        });
        
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                setUpAccount();
            }
        });
    }

    private void setUpAccount() {
        final String user_id = mAuth.getCurrentUser().getUid();
        final String name = mNameField.getText().toString().trim();
        if (imageUri != null) {
            progressDialog.show();
            if (!TextUtils.isEmpty(name) && user_id != null && imageUri != null) {
                StorageReference filepath = mStorage.child(imageUri.getLastPathSegment());

                filepath.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        progressDialog.dismiss();
                        String downloadUri = taskSnapshot.getDownloadUrl().toString();

                        mUserDatabase.child(user_id).child("name").setValue(name);
                        mUserDatabase.child(user_id).child("image").setValue(downloadUri);

                        Intent mainIntent = new Intent(SetUpActivity.this, MainActivity.class);
                        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(mainIntent);


                    }
                });
            }
        }else {
            Toast.makeText(this, "Attach your profile image", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK){
            CropImage.activity(data.getData())
                    .setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1)
                    .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imageUri = result.getUri();
                mUserImageView.setImageURI(imageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }
}
