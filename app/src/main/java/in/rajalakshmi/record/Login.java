package in.rajalakshmi.record;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class Login extends AppCompatActivity {
    TextInputEditText textInputUsername;
    TextInputEditText textInputPassword;
    Button login;

    ProgressDialog progressDialog;



    FirebaseAuth firebaseAuth;
    DatabaseReference mRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);
        firebaseAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference();
     //   String id=firebaseAuth.getCurrentUser().getUid();
      //  String status=mRef.child("login/"+id+"/isLogin").toString();
        if( firebaseAuth.getCurrentUser()!=null){
            startActivity(new Intent(Login.this,MainActivity.class));
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Logging In");
        progressDialog.setMessage("Please wait until logging you in");
        progressDialog.setCanceledOnTouchOutside(false);

        textInputUsername = findViewById(R.id.text_input_username);
        textInputPassword = findViewById(R.id.text_input_password);
        login = findViewById(R.id.login);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.show();
                loginUser(textInputUsername.getText().toString(), textInputPassword.getText().toString());
            }
        });
    }

    private void loginUser(final String id, final String pass) {
        mRef.child("login/"+id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists() && Objects.equals(dataSnapshot.child("type").getValue(),"staff")
                        &&Objects.equals(dataSnapshot.child("isLogin").getValue(),false) && pass.equals(dataSnapshot.child("pass").getValue())) {
                    Toast.makeText(Login.this, "Success", Toast.LENGTH_SHORT).show();
                    SignIn(id);
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(Login.this, "Failure", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                String db=databaseError.toString();
                Toast.makeText(Login.this,db, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void SignIn(final String id) {
        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        mRef.child("staff_id/"+firebaseAuth.getCurrentUser().getUid()).setValue(id)    //update db
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        mRef.child("login/"+id+"/isLogin").setValue(true)
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        progressDialog.dismiss();
                                                        startActivity(new Intent(Login.this,MainActivity.class));
                                                        finish();
                                                    }
                                                });
                                    }
                                });
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
        }
    }
}

