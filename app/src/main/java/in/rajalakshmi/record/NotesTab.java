package in.rajalakshmi.record;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
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
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import static android.app.Activity.RESULT_OK;


public class NotesTab extends Fragment {

    FirebaseAuth firebaseAuth;
    DatabaseReference mRef;
    Button logout;
    View view;
    RecyclerView mRecycle;

    String id, mRoll, n, classid, subject,type;
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


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.notes_tab, container, false);

        firebaseAuth = FirebaseAuth.getInstance();

        storageReference = FirebaseStorage.getInstance().getReference();
        mRef = FirebaseDatabase.getInstance().getReference();

        mRecycle = (RecyclerView) view.findViewById(R.id.notes_recycler);
        mRecycle.setHasFixedSize(true);
        mRecycle.setLayoutManager(new GridLayoutManager(view.getContext(), 2));

        //getting views from layout

        buttonUpload = (FloatingActionButton) view.findViewById(R.id.buttonUpload);
        name = (EditText) view.findViewById(R.id.name);


        //attaching listener
        buttonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateuser()) {
                    showFileChooser();
                }
            }
        });
        return view;
    }

    boolean validateuser() {
        n = name.getText().toString().trim();
        if (n.isEmpty()) {
            name.setError("Field can't be empty");
            return false;
        } else {
            name.setError(null);
            return true;
        }
    }

    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select File"), PICK_IMAGE_REQUEST);
    }

    //handling the image chooser activity result

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
            final ProgressDialog progressDialog = new ProgressDialog(getContext());
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            validateuser();
            name.setText("");
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final StorageReference fileRef = storageReference.child(n);
            final StorageReference httpsReference = storage.getReferenceFromUrl("gs://record-b30a6.appspot.com/" + n);


                    fileRef.putFile(filePath)
                            .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onComplete(Task taskSnapshot) {
                                    fileRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                        @Override
                                        public void onSuccess(StorageMetadata storageMetadata) {
                                            type=storageMetadata.getContentType();
                                        }
                                    });
                                    Toast.makeText(getContext(), "" + classid, Toast.LENGTH_SHORT).show();
                                    httpsReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            mRef.child("notes").child(classid).child(subject).child(n).child("link").setValue(uri.toString());
                                            mRef.child("notes").child(classid).child(subject).child(n).child("type").setValue(type);          //uploding notes
                                        }
                                    });
                                    //if the upload is successfull
                                    //hiding the progress dialog
                                    progressDialog.hide();
                                    //and displaying a success toast
                                    Toast.makeText(getContext(), "File Uploaded ", Toast.LENGTH_LONG).show();
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
    public void onStart() {
        progressDialog = new ProgressDialog(getContext());
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
                        Toast.makeText(view.getContext(), "Success " + subject, Toast.LENGTH_SHORT).show();


                        mRef.child("staff/" + mRoll).child("classid").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }

                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                classid = (String) dataSnapshot.getValue();


                                firebaseRecyclerAdapter = new FirebaseRecyclerAdapter<Notes, NotesViewHolder>(
                                        Notes.class,
                                        R.layout.cardview,
                                        NotesViewHolder.class,
                                        mRef.child("notes").child(classid).child(subject)
                                ) {
                                    @Override
                                    protected void populateViewHolder(NotesViewHolder viewHolder, Notes model, int position) {
                                        viewHolder.setname(firebaseRecyclerAdapter.getRef(position).getKey());
                                    }
                                };
                                mRecycle.setAdapter(firebaseRecyclerAdapter);

                                progressDialog.hide();
                            }
                        });

                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        super.onStart();
    }

}
