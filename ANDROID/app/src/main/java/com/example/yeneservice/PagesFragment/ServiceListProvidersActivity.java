package com.example.yeneservice.PagesFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.airbnb.lottie.LottieAnimationView;
import com.example.yeneservice.Adapters.ServiceProviderAdapter;
import com.example.yeneservice.Extra.ProviderMapActivity;
import com.example.yeneservice.MapsActivity;
import com.example.yeneservice.Models.ServicesProvider;
import com.example.yeneservice.R;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class ServiceListProvidersActivity extends AppCompatActivity {

    LinearLayout btn;
    List<ServicesProvider> lstBook ;
    private static final String TAG = "MyActivity";
    ServiceProviderAdapter serviceProviderAdapter;
    ExtendedFloatingActionButton fab;
    LottieAnimationView lottieAnimationView;
    TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Provider Lists");

        count = findViewById(R.id.bage_count);
        lottieAnimationView = findViewById(R.id.animation_view);
        fab = findViewById(R.id.fab_list);
        // Recieve data
        Intent intent = getIntent();
        final String tte = intent.getExtras().getString("name").toLowerCase();
        final String serviceId = intent.getExtras().getString("serviceID");

        if(tte != null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar.setTitle(tte);
            }
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent o = new Intent(ServiceListProvidersActivity.this, ProviderMapActivity.class);
                o.putExtra("category",tte);
                startActivity(o);
            }
        });

        lstBook = new ArrayList<>();
        serviceProviderAdapter = new ServiceProviderAdapter(this,lstBook);

        RecyclerView myrv = findViewById(R.id.recyclerview_id);
        myrv.setHasFixedSize(true);
//        ServiceProviderAdapter myAdapter = new ServiceProviderAdapter(this,lstBook);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        myrv.setLayoutManager(mLayoutManager);

        myrv.setAdapter(serviceProviderAdapter);

        loadData(tte);
    }

    private void loadData(final String catagory_service) {
        // Access a Cloud Firestore instance from your Activity
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("Service_Providers").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                if(e != null){
                    Log.d(TAG,"Error: "+ e.getMessage());
                }
                for(DocumentChange doc : queryDocumentSnapshots.getDocumentChanges()){
                    if(doc.getType() == DocumentChange.Type.ADDED){

                        final String id = doc.getDocument().getString("userID");
                        final String cty = doc.getDocument().getString("city");
                        final String work = doc.getDocument().getString("working_area");
                        final String add = doc.getDocument().getString("address");
                        final String me = doc.getDocument().getString("about_me");
                        final String email = doc.getDocument().getString("email");
                        final String type = doc.getDocument().getString("type");
                        final GeoPoint l = doc.getDocument().getGeoPoint("location");

                        if(catagory_service.equals(work)){
                            fab.setVisibility(View.VISIBLE);
                            lottieAnimationView.setVisibility(View.GONE);
                            Log.d(TAG,"file name: "+ cty);
                            final String documentId = doc.getDocument().getId();
                            db.collection("Users").document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                @Override
                                public void onSuccess(DocumentSnapshot documentSnapshot) {
                                    String image = documentSnapshot.getString("image");
                                    String first_name = documentSnapshot.getString("firstName");
                                    String last_name = documentSnapshot.getString("lastName");
                                    lstBook.add(new ServicesProvider(documentId,id,first_name,last_name,image,add,work,me,l));
                                    serviceProviderAdapter.notifyDataSetChanged();
                                }
                            });
                        } else {
                            fab.setVisibility(View.GONE);
                            lottieAnimationView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }
}