package chandoras.ankit.awesome.blogit;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;

import de.hdodenhof.circleimageview.CircleImageView;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int GALLERY_INTENT = 3;

    private static final int MY_PERMISSIONS_REQUEST_EXTERNAL = 10;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private DatabaseReference mDatabase;
    private DatabaseReference mUserDatabase;
    private DatabaseReference mDatabaseLikes;
    private DatabaseReference mDatabaseUnlikes;


    private RecyclerView mBlogsList;
    private boolean mProcessLike = false;
    private boolean mProcessUnlike = false;
    private  TextView mNotConnected ;

    private CircleImageView mCircleImageView;
    private TextView mUserNameNav;
    private TextView mUserEmailNav;
    private String imageUri = null;
    String userName = null;

    private Uri mImageUri = null;
    String email, name;
    private AdView mAdView;

    private Query mDatabaseQuery;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(getApplicationContext(),"ca-app-pub-8203960224001766~7142017992");
        mAdView = (AdView) findViewById(R.id.adView);



        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAuth = FirebaseAuth.getInstance();

        mDatabase = FirebaseDatabase.getInstance().getReference().child("Blogs");
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mDatabaseLikes = FirebaseDatabase.getInstance().getReference().child("Likes");
        mDatabaseUnlikes = FirebaseDatabase.getInstance().getReference().child("Unlikes");

        mDatabaseQuery = mDatabase.orderByChild("time");

        mDatabaseLikes.keepSynced(true);
        mDatabaseUnlikes.keepSynced(true);
        mUserDatabase.keepSynced(true);
        mDatabase.keepSynced(true);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {


            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {


                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_EXTERNAL);


            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent postIntent = new Intent(MainActivity.this, PostActivity.class);
                startActivity(postIntent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mCircleImageView = (CircleImageView) navigationView.getHeaderView(0).findViewById(R.id.navigation_image);
        mUserNameNav = (TextView) navigationView.getHeaderView(0).findViewById(R.id.navigation_name);
        mUserEmailNav = (TextView) navigationView.getHeaderView(0).findViewById(R.id.navigation_email);
        mNotConnected = (TextView)findViewById(R.id.main_internet);



        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    Log.i("USER", "USER");
                    Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                    loginIntent.addCategory(Intent.CATEGORY_HOME);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                }

            }
        };


        mBlogsList = (RecyclerView) findViewById(R.id.main_recycler_view);
        mBlogsList.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        mBlogsList.setLayoutManager(linearLayoutManager);
        checkUserExist();



        if (mAuth.getCurrentUser() != null) {

            checkUserExist();
            email = mAuth.getCurrentUser().getEmail();
            name = mAuth.getCurrentUser().getDisplayName();
            mUserDatabase.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.child("image").exists() && dataSnapshot.child("name").exists()) {
                        imageUri = dataSnapshot.child("image").getValue().toString();
                        userName = dataSnapshot.child("name").getValue().toString();


                        Picasso.with(MainActivity.this).load(imageUri).placeholder(R.drawable.default_avatar).networkPolicy(NetworkPolicy.OFFLINE).into(mCircleImageView, new Callback() {
                            @Override
                            public void onSuccess() {

                            }

                            @Override
                            public void onError() {
                                Picasso.with(MainActivity.this).load(imageUri).placeholder(R.drawable.default_avatar).into(mCircleImageView);
                            }
                        });


                        mUserEmailNav.setText(email);

                        mUserNameNav.setText(userName);
                    }


                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }


        mCircleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, GALLERY_INTENT);
            }
        });


    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_EXTERNAL: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_INTENT && resultCode == RESULT_OK) {
            mImageUri = data.getData();
            mCircleImageView.setImageURI(mImageUri);
        }
    }

    private void checkUserExist() {
        if (mAuth.getCurrentUser() != null) {
            final String current_uid = mAuth.getCurrentUser().getUid();

            mUserDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.hasChild(current_uid)) {
                        Intent setUpIntent = new Intent(MainActivity.this, SetUpActivity.class);
                        setUpIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(setUpIntent);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);

        if (isNetworkAvailable()){
            mNotConnected.setVisibility(View.GONE);

        }else {


            mNotConnected.setVisibility(View.VISIBLE);
            Snackbar.make(findViewById(android.R.id.content), "Connection Error", Snackbar.LENGTH_LONG)

                    .setActionTextColor(Color.RED)
                    .show();
        }
        FirebaseRecyclerAdapter<Blog, BlogViewHolder> adapter = new FirebaseRecyclerAdapter<Blog, BlogViewHolder>(
                Blog.class,
                R.layout.single_blog_post,
                BlogViewHolder.class,
                mDatabaseQuery


        ) {
            @Override
            protected void populateViewHolder(final BlogViewHolder viewHolder, Blog model, final int position) {

                final String post_key = getRef(position).getKey();

                viewHolder.setTitle(model.getTitle());
                viewHolder.setDescp(model.getDescp());
                viewHolder.setName(model.getUsername());
                viewHolder.setImage(model.getImage());


                mNotConnected.setVisibility(View.GONE);



                mDatabase.child(post_key).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild("uid")) {
                            String uid = (String) dataSnapshot.child("uid").getValue();
                            String time = dataSnapshot.child("time").getValue().toString();

                            long lastTime = Long.parseLong(time);

                            String lastSeenTime = GetTimeAgo.getTimeAgo(-lastTime, getApplicationContext());
                            viewHolder.mTimeTextView.setText(lastSeenTime);

                            mUserDatabase.child(uid).addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.hasChild("image")) {
                                        final String image = (String) dataSnapshot.child("image").getValue();

                                        Picasso.with(MainActivity.this).load(image).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_avatar).into(viewHolder.mUserImageView, new Callback() {
                                            @Override
                                            public void onSuccess() {

                                            }

                                            @Override
                                            public void onError() {
                                                Picasso.with(MainActivity.this).load(image).placeholder(R.drawable.default_avatar).into(viewHolder.mUserImageView);
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
                        if (dataSnapshot.exists()) {
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
                        if (dataSnapshot.exists()) {
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

                viewHolder.mDownloadImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mDatabase.child(post_key).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                String imageUrl = dataSnapshot.child("image").getValue().toString();

                                saveImage(imageUrl);


                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                });


                viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                                        @Override
                                                        public void onClick(View view) {
                                                            Intent blogIntent = new Intent(MainActivity.this, BlogSingleActivity.class);
                                                            blogIntent.putExtra("POST_ID", post_key);
                                                            startActivity(blogIntent);
                                                        }
                                                    }
                );

                viewHolder.mLikeImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mProcessLike = true;

                        mDatabaseLikes.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProcessLike) {
                                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                        viewHolder.mLikeImageView.setImageResource(R.drawable.thumb_up);
                                        viewHolder.mUnlikeImageView.setEnabled(true);
                                        mDatabaseLikes.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();
                                        mProcessLike = false;
                                    } else {

                                        viewHolder.mLikeImageView.setImageResource(R.drawable.red_thumb_up);
                                        viewHolder.mUnlikeImageView.setEnabled(false);
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

                viewHolder.mUnlikeImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        mProcessUnlike = true;

                        mDatabaseUnlikes.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                if (mProcessUnlike) {
                                    if (dataSnapshot.child(post_key).hasChild(mAuth.getCurrentUser().getUid())) {
                                        viewHolder.mUnlikeImageView.setImageResource(R.drawable.thumb_down);
                                        viewHolder.mLikeImageView.setEnabled(true);
                                        mDatabaseUnlikes.child(post_key).child(mAuth.getCurrentUser().getUid()).removeValue();
                                        mProcessUnlike = false;
                                    } else {
                                        viewHolder.mUnlikeImageView.setImageResource(R.drawable.red_thumb_down);
                                        viewHolder.mLikeImageView.setEnabled(false);
                                        mDatabaseUnlikes.child(post_key).child(mAuth.getCurrentUser().getUid()).setValue("RandomValue");
                                        mProcessUnlike = false;
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

    private void saveImage(String imageUrl) {


        Picasso.with(MainActivity.this)
                .load(imageUrl)
                .into(new Target() {
                          @Override
                          public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                              try {


                                  String root = Environment.getExternalStorageDirectory().toString();

                                  File myDir = new File(root + "/Blog It");


                                  if (!myDir.exists()) {
                                      myDir.mkdir();

                                  }

                                  String name = new Date().toString() + ".jpg";
                                  myDir = new File(myDir, name);


                                  FileOutputStream out = new FileOutputStream(myDir);
                                    Log.i("FILE",out.toString());
                                  bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                                  Toast.makeText(MainActivity.this, "Successfully saved at "+myDir, Toast.LENGTH_SHORT).show();
                                  out.flush();

                                  out.close();
                              } catch (Exception e) {
                                  e.printStackTrace();
                              }
                          }

                          @Override
                          public void onBitmapFailed(Drawable errorDrawable) {
                              Toast.makeText(MainActivity.this, "Failed to save!", Toast.LENGTH_SHORT).show();
                          }

                          @Override
                          public void onPrepareLoad(Drawable placeHolderDrawable) {
                          }
                      }
                );


    }


    public static class BlogViewHolder extends RecyclerView.ViewHolder {

        View mView;
        private ImageView mLikeImageView;
        private ImageView mUnlikeImageView;
        private TextView mLikeCount;
        private TextView mUnlikeCount;
        private TextView mTimeTextView;
        private CircleImageView mUserImageView;
        private ImageView mDownloadImageView;

        public BlogViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

            mLikeImageView = (ImageView) mView.findViewById(R.id.single_like_btn);
            mUnlikeImageView = (ImageView) mView.findViewById(R.id.single_unlike);
            mLikeCount = (TextView) mView.findViewById(R.id.single_like_count);
            mUserImageView = (CircleImageView) mView.findViewById(R.id.single_user_image);
            mUnlikeCount = (TextView) mView.findViewById(R.id.single_unlike_count);
            mTimeTextView = (TextView) mView.findViewById(R.id.single_time);
            mDownloadImageView = (ImageView) mView.findViewById(R.id.single_download);
        }

        public void setTitle(String title) {
            TextView postTitle = (TextView) mView.findViewById(R.id.single_title);
            postTitle.setTypeface(MainActivity.loadRegular(mView.getContext()));
            postTitle.setText(title);

        }

        public void setName(String name) {
            TextView postName = (TextView) mView.findViewById(R.id.single_name);
            postName.setTypeface(MainActivity.loadRegular(mView.getContext()));
            postName.setText(name);
        }

        public void setDescp(String descp) {
            TextView postDescp = (TextView) mView.findViewById(R.id.single_desc);
            postDescp.setTypeface(MainActivity.loadRegular(mView.getContext()));
            postDescp.setText(descp);
        }

        public void setImage(final String image) {
            final ImageView postImage = (ImageView) mView.findViewById(R.id.single_image);


            Glide.with(mView.getContext()).load(image).placeholder(R.drawable.default_image).into(postImage);
//

        }


    }

    @Override
    protected void onStop() {
        super.onStop();

        mAuth.removeAuthStateListener(mAuthListener);
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        switch (id) {

            case R.id.action_logout:
                mAuth.signOut();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_profile:

                Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
                break;

            case R.id.nav_logout:
                mAuth.signOut();
                break;

            case R.id.nav_account_settings:

                Intent setUpIntent = new Intent(MainActivity.this, SetUpActivity.class);
                startActivity(setUpIntent);
                break;
            case R.id.nav_about:


                AlertDialog.Builder builder;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    builder = new AlertDialog.Builder(MainActivity.this, R.style.MyDialogTheme);
                } else {
                    builder = new AlertDialog.Builder(MainActivity.this);
                }


                builder.setTitle("About us!")
                        .setMessage("We provide you a place where you can connect to other people and can also share your thoughts to other.Developed by Ankit Chandora with love :)")

                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(R.drawable.back)
                        .show();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static Typeface loadRegular(Context context) {
        return Typeface.createFromAsset(context.getAssets(), "fonts/Montserrat-Regular.ttf");
    }



}
