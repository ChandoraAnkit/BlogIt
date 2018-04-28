package chandoras.ankit.awesome.blogit;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.Date;

public class PostActivity extends AppCompatActivity implements RewardedVideoAdListener {
    private static final int GALLERY_INTENT = 2;

    private EditText mTitleField,mDescpField;
    private Button mSubmitPostButton;
    private ImageView mUserImageView;

    private Uri mImageUri = null;

    private DatabaseReference mDatabase;
    private DatabaseReference mUserDatabase;
    private StorageReference mStorage;

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;

    private RewardedVideoAd mAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        MobileAds.initialize(getApplicationContext(),"ca-app-pub-8203960224001766~7142017992");

        mAd = MobileAds.getRewardedVideoAdInstance(this);
        mAd.setRewardedVideoAdListener(this);

        loadRewardedVideoAd();


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Posting...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        getSupportActionBar().setTitle("New post");

        mTitleField = (EditText)findViewById(R.id.post_title);
        mDescpField = (EditText)findViewById(R.id.post_desc);
        mUserImageView = (ImageView)findViewById(R.id.post_image);
        mSubmitPostButton = (Button)findViewById(R.id.post_button);





        mAuth = FirebaseAuth.getInstance();

        mStorage = FirebaseStorage.getInstance().getReference();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blogs");
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());

        mDatabase.keepSynced(true);
        mUserDatabase.keepSynced(true);



        mUserImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_INTENT);
            }
        });

        mSubmitPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                
            startPosting();
                
            }
        });

    }

    private void loadRewardedVideoAd() {
        mAd.loadAd("ca-app-pub-8203960224001766/7733044938", new AdRequest.Builder().build());

    }

    private void startPosting() {
        final String title = mTitleField.getText().toString().trim();
        final String descp = mDescpField.getText().toString().trim();


        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(descp) && mImageUri != null){

            progressDialog.show();

            StorageReference filepath = mStorage.child("Blog_photos").child(mImageUri.getLastPathSegment());
            filepath.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    final Uri downloadUri = taskSnapshot.getDownloadUrl();

                    final DatabaseReference newPostDatabase = mDatabase.push();

                    mUserDatabase.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {


                           long i =  -1* new Date().getTime();


                            newPostDatabase.child("title").setValue(title);
                            newPostDatabase.child("descp").setValue(descp);
                            newPostDatabase.child("image").setValue(downloadUri.toString());
                            newPostDatabase.child("time").setValue(i);
                            newPostDatabase.child("uid").setValue(mAuth.getCurrentUser().getUid());
                            newPostDatabase.child("username").setValue(dataSnapshot.child("name").getValue()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()){
                                        progressDialog.dismiss();
                                        startActivity(new Intent(PostActivity.this,MainActivity.class));
                                    }else {
                                        progressDialog.dismiss();
                                        Toast.makeText(PostActivity.this, "Failed to upload post", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }
            });

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

                mImageUri  = result.getUri();
                mUserImageView.setImageURI(mImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        if (mAd.isLoaded()) {
            mAd.show();
        }

    }

    @Override
    public void onRewardedVideoAdOpened() {

    }

    @Override
    public void onRewardedVideoStarted() {

    }

    @Override
    public void onRewardedVideoAdClosed() {

    }

    @Override
    public void onRewarded(RewardItem rewardItem) {

    }

    @Override
    public void onRewardedVideoAdLeftApplication() {

    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int i) {

    }
}
