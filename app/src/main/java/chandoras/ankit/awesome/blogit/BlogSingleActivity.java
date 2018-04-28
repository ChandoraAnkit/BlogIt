package chandoras.ankit.awesome.blogit;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

public class BlogSingleActivity extends AppCompatActivity {

    private  String post_key = null;


    private TextView mTitleField,mDescpField;
    private Button mRemoveButton;
    private ImageView mImageField;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blog_single);

        getSupportActionBar().setTitle("Post");

        mTitleField = (TextView)findViewById(R.id.singleblog_title);
        mDescpField = (TextView)findViewById(R.id.singleblog_descp);
        mImageField = (ImageView) findViewById(R.id.singleblog_image);
        mRemoveButton = (Button) findViewById(R.id.singleblog_remove_btn);


        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blogs");
        mDatabase.keepSynced(true);

         post_key = getIntent().getStringExtra("POST_ID");

        mDatabase.child(post_key).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String post_title = dataSnapshot.child("title").getValue().toString();
                    String post_descp = dataSnapshot.child("descp").getValue().toString();
                    final String post_image = dataSnapshot.child("image").getValue().toString();
                    String post_uid = dataSnapshot.child("uid").getValue().toString();

                    mTitleField.setText(post_title);
                    mDescpField.setText(post_descp);
                    Picasso.with(BlogSingleActivity.this).load(post_image).placeholder(R.drawable.default_image).networkPolicy(NetworkPolicy.OFFLINE).into(mImageField, new Callback() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError() {
                            Picasso.with(BlogSingleActivity.this).load(post_image).placeholder(R.drawable.default_image).into(mImageField);

                        }
                    });


                    if (mAuth.getCurrentUser().getUid().equals(post_uid)) {
                        mRemoveButton.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatabase.child(post_key).removeValue();

                Intent mainIntent = new Intent(BlogSingleActivity.this,MainActivity.class);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(mainIntent);
            }
        });
    }
}
