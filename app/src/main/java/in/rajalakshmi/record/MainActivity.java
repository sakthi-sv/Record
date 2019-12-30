package in.rajalakshmi.record;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.Objects;

import in.rajalakshmi.record.Login;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    DatabaseReference mRef;
    Button logout;
    View mView;
    RecyclerView mRecycle;

    String id, mRoll, n, classid, subject;

    private static final String TAG = "MainActivity";
    private SectionsPageAdapter mSectionsPageAdapter;
    private ViewPager mViewPager;
    Toolbar toolbar;

    //a constant to track the file chooser intent
    private static final int PICK_IMAGE_REQUEST = 234;

    //Buttons

    private FloatingActionButton buttonUpload;
    private EditText name;
    ProgressDialog progressDialog;
    //ImageView
    private ImageView imageView;

    private FirebaseRecyclerAdapter<Notes, NotesViewHolder> firebaseRecyclerAdapter;

    //a Uri object to store file path
    private Uri filePath;

    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        mSectionsPageAdapter = new SectionsPageAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


        storageReference = FirebaseStorage.getInstance().getReference();
        mRef = FirebaseDatabase.getInstance().getReference();

        //  mRecycle = findViewById(R.id.recycler);
//        mRecycle.setHasFixedSize(true);
        // mRecycle.setLayoutManager(new GridLayoutManager(this,2));

        mView = findViewById(R.id.main_activity);
        //getting views from layout

        // buttonUpload = (FloatingActionButton) findViewById(R.id.buttonUpload);
        // name = (EditText) findViewById(R.id.name);


        //attaching listener
      /*  buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(validateuser()) {
                    showFileChooser();
                }
            }
        });*/


    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPageAdapter adapter = new SectionsPageAdapter(getSupportFragmentManager());
        adapter.addFragment(new NotesTab(), "Notes");
        adapter.addFragment(new AssignmentTab(), "Assignment");

        viewPager.setAdapter(adapter);
    }

   /* boolean validateuser() {
        n = name.getText().toString().trim();
        if (n.isEmpty()) {
            name.setError("Field can't be empty");
            return false;
        } else {
            name.setError(null);
            return true;
        }
    }*/


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.logout) {
            logoutuser();
        }
        return true;
    }

    private void logoutuser() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging Out");
        progressDialog.setMessage("Please wait while Logging Out.......");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        firebaseAuth.getCurrentUser().delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateDb();
                        }
                    }
                });
    }

    private void updateDb() {

        mRef.child("staff_id").child(Objects.requireNonNull(id)).removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            mRef.child("login").child(mRoll).child("isLogin").setValue(false)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                progressDialog.dismiss();

                                                startActivity(new Intent(MainActivity.this, Login.class));
                                                finish();
                                            }
                                        }
                                    });

                        }
                    }
                });
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_IMAGE_REQUEST);
    }

    //handling the image chooser activity result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            uploadFile();
        }
    }


    //this method will upload the file
    private void uploadFile() {

        //if there is a file to upload
        if (filePath != null) {
            //displaying a progress dialog while upload is going on
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            // validateuser();
            name.setText("");
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final StorageReference riversRef = storageReference.child(n);
            final StorageReference httpsReference = storage.getReferenceFromUrl("gs://record-b30a6.appspot.com/" + n);

            riversRef.putFile(filePath)
                    .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(Task taskSnapshot) {
                            Toast.makeText(MainActivity.this, "" + classid, Toast.LENGTH_SHORT).show();
                            httpsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    mRef.child("notes").child(classid).child(subject).child(n).child("link").setValue(uri.toString());             //uploding notes
                                }
                            });
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.hide();
                            //and displaying a success toast
                            Toast.makeText(getApplicationContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
                        }
                    }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //calculating progress percentage
                    double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                    //displaying percentage in progress dialog
                    progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                }
            });
        }
//if there is not any file
        else {
            //you can display an error toast
        }
    }


    // @Override
    protected void onStart() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loding");
        progressDialog.setMessage("Please wait while Logging Content.......");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        id = firebaseAuth.getCurrentUser().getUid();

        mRef.child("staff_id").child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mRoll = (String) dataSnapshot.getValue();


                mRef.child("staff/" + mRoll).child("subject").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        subject = (String) dataSnapshot.getValue();
                        Toast.makeText(MainActivity.this, "Success " + subject, Toast.LENGTH_SHORT).show();


                        mRef.child("staff/" + mRoll).child("classid").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                classid = (String) dataSnapshot.getValue();
                                progressDialog.hide();
                            }
                        });
                    }
                });

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            };
        });
        super.onStart();
    }
}