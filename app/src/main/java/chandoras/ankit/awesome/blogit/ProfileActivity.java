package chandoras.ankit.awesome.blogit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import chandoras.ankit.awesome.blogit.Blog;
import chandoras.ankit.awesome.blogit.BlogSingleActivity;
import chandoras.ankit.awesome.blogit.GetTimeAgo;
import chandoras.ankit.awesome.blogit.R;
import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {
    private  boolean mProcessLike = false;

    private RecyclerView mBlogsList;

    private ImageView mEmptyImage;

    private Query mCurrentUserQuery;
    private DatabaseReference  mDatabase;
    private  DatabaseReference mDatabaseLikes;
    private DatabaseReference mDatabaseUnlikes;
    private DatabaseReference mUserDatabase;

    private FirebaseAuth mAuth;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        mAuth = FirebaseAuth.getInstance();

        MobileAds.initialize(getApplicationContext(),"ca-app-pub-8203960224001766~7142017992");
        mAdView = (AdView) findViewById(R.id.adView);

        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);



        getSupportActionBar().setTitle("Your posts");

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blogs");
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseUnlikes = FirebaseDatabase.getInstance().getReference().child("Unlikes");

        mDatabaseLikes = FirebaseDatabase.getInstance().getReference().child("Likes");

        mDatabase.keepSynced(true);
        mDatabaseLikes.keepSynced(true);

        mCurrentUserQuery = mDatabase.orderByChild("uid").equalTo(mAuth.getCurrentUser().getUid());


        mEmptyImage = (ImageView)findViewById(R.id.profile_default);
        mBlogsList = (RecyclerView) findViewById(R.id.profile_recycler_view);
        mBlogsList.setHasFixedSize(true);
        mBlogsList.setLayoutManager(new LinearLayoutManager(this));

        mCurrentUserQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    mEmptyImage.setVisibility(View.VISIBLE);
                }else {
                    mEmptyImage.setVisibility(View.GONE);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Blog, BlogViewHolder> adapter = new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(
                Blog.class,
                R.layout.single_blog_post,
                BlogViewHolder.class,
                mCurrentUserQuery


        ) {
            @Override
            protected void populateViewHolder(final BlogViewHolder viewHolder, Blog model, final int position) {

                final String  post_key = getRef(position).getKey();

                viewHolder.setTitle(model.getTitle());
                viewHolder.setDescp(model.getDescp());
                viewHolder.setName(model.getUsername());
                viewHolder.setImage(model.getImage());


                mDatabase.child(post_key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("uid")){
                            String uid = (String) dataSnapshot.child("uid").getValue();

                            String time = dataSnapshot.child("time").getValue().toString();

                            long lastTime = Long.parseLong(time);

                            String lastSeenTime = GetTimeAgo.getTimeAgo(-lastTime,getApplicationContext());
                            viewHolder.mTimeTextView.setText(lastSeenTime);

                            mUserDatabase.child(uid).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild("image")){
                                        final String image = (String) dataSnapshot.child("image").getValue();

                                        Picasso.with(ProfileActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(viewHolder.mUserImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(ProfileActivity.this).load(image).placeholder(R.drawable.default_image).into(viewHolder.mUserImageView);
                                            }
                                        });

                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });


                mDatabaseLikes.child(post_key).child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            viewHolder.mLikeImageView.setImageResource(R.drawable.red_thumb_up);
                            viewHolder.mUnlikeImageView.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
                mDatabaseUnlikes.child(post_key).child(mAuth.getCurrentUser().getUid()).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            viewHolder.mUnlikeImageView.setImageResource(R.drawable.red_thumb_down);
                            viewHolder.mLikeImageView.setEnabled(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });



                mDatabaseLikes.child(post_key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int num = (int) dataSnapshot.getChildrenCount();
                        viewHolder.mLikeCount.setText(String.valueOf(num));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                mDatabaseUnlikes.child(post_key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int num = (int) dataSnapshot.getChildrenCount();
                        viewHolder.mUnlikeCount.setText(String.valueOf(num));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent blogIntent = new Intent(ProfileActivity.this,BlogSingleActivity.class);
                        blogIntent.putExtra("POST_ID",post_key);
                        startActivity(blogIntent);
                    }
                });

                viewHolder.mLikeImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mProcessLike = true;

                        mDatabaseLikes.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProcessLike) {
                                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                        mDatabaseLikes.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();
                                        mProcessLike = false;
                                    } else {

                                        mDatabaseLikes.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("RandomValue");
                                        mProcessLike = false;
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }
                });

            }
        };

        mBlogsList.setAdapter(adapter);
    }


    public static class BlogViewHolder extends RecyclerView.ViewHolder {

        View mView;

        private ImageView mLikeImageView;
        private ImageView mUnlikeImageView;
        private TextView mLikeCount;
        private TextView mUnlikeCount;
        private CircleImageView mUserImageView;
        private TextView mTimeTextView;

        public BlogViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

            mUserImageView = (CircleImageView)mView.findViewById(R.id.single_user_image);
            mLikeImageView = (ImageView)mView.findViewById(R.id.single_like_btn);
            mUnlikeImageView = (ImageView)mView.findViewById(R.id.single_unlike);
            mLikeCount = (TextView) mView.findViewById(R.id.single_like_count);
            mUnlikeCount = (TextView) mView.findViewById(R.id.single_unlike_count);
            mTimeTextView = (TextView)mView.findViewById(R.id.single_time);
        }

        public void setTitle(String title) {
            TextView postTitle = (TextView) mView.findViewById(R.id.single_title);
            postTitle.setTypeface(ProfileActivity.loadRegular(mView.getContext()));
            postTitle.setText(title);
        }

        public void setName(String name) {
            TextView postName = (TextView) mView.findViewById(R.id.single_name);
            postName.setTypeface(ProfileActivity.loadRegular(mView.getContext()));
            postName.setText(name);
        }

        public void setDescp(String descp) {
            TextView postDescp = (TextView) mView.findViewById(R.id.single_desc);
            postDescp.setTypeface(ProfileActivity.loadRegular(mView.getContext()));
            postDescp.setText(descp);
        }

        public void setImage(final String image) {
            final ImageView postImage = (ImageView) mView.findViewById(R.id.single_image);

            Glide.with(mView.getContext()).load(image).placeholder(R.drawable.default_image).into(postImage);


        }


    }
    public static Typeface loadRegular(Context context){
        return Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.ttf");
    }
}
