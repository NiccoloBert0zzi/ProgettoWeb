package com.example.mobile_memoryexplorer.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.mobile_memoryexplorer.Auth.Login;
import com.example.mobile_memoryexplorer.MemoriesListAdapter;
import com.example.mobile_memoryexplorer.Memory;
import com.example.mobile_memoryexplorer.MySharedData;
import com.example.mobile_memoryexplorer.R;
import com.example.mobile_memoryexplorer.databinding.FragmentProfileBinding;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


public class ProfileFragment extends Fragment {

  private FragmentProfileBinding binding;
  private DatabaseReference dbRef;
  private final List<Memory> list = new ArrayList<>();
  String email;
  MySharedData mySharedData;

  public View onCreateView(@NonNull LayoutInflater inflater,
                           ViewGroup container, Bundle savedInstanceState) {
    ProfileViewModel profileViewModel =
        new ViewModelProvider(this).get(ProfileViewModel.class);
    profileViewModel.loadDb(this.getContext());
    mySharedData = new MySharedData(getContext());
    email = MySharedData.getEmail();
    binding = FragmentProfileBinding.inflate(inflater, container, false);
    View root = binding.getRoot();

    if (MySharedData.getThemePreferences()) {
      binding.switchTheme.setChecked(true);
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
    }
    binding.switchTheme.setOnClickListener(v -> {
      if (!MySharedData.getThemePreferences()) {
        mySharedData.setThemePreferences(true);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
      } else {
        mySharedData.setThemePreferences(false);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
      }
    });

    dbRef = FirebaseDatabase.getInstance().getReference("memories");
    prepareItemData();
    //Set view
    profileViewModel.getName().observe(getViewLifecycleOwner(), binding.name::setText);
    profileViewModel.getSurnName().observe(getViewLifecycleOwner(), binding.surname::setText);
    profileViewModel.getAddress().observe(getViewLifecycleOwner(), binding.address::setText);
    profileViewModel.getBirthdate().observe(getViewLifecycleOwner(), binding.birdthday::setText);
    profileViewModel.getimageURI().observe(getViewLifecycleOwner(), s -> {
      if (s != null) {
        Glide.with(binding.getRoot().getContext())
            .load(Uri.parse(s))
            .into(binding.profileImage);
      } else {
        //todo set default image in drawable
        binding.profileImage.setImageResource(R.drawable.ic_baseline_account_circle_24);
      }
    });
    //logout
    binding.buttonLogout.setOnClickListener(v -> {
      MySharedData mySharedData = new MySharedData(this.getContext());
      mySharedData.setSharedpreferences("remember", "false");
      //start login activity
      Intent loginpage = new Intent(this.getContext(), Login.class);
      loginpage.setFlags(loginpage.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
      startActivity(loginpage);
    });
    return root;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  public void prepareItemData() {
    dbRef.addValueEventListener(new ValueEventListener() {
      @Override
      public void onDataChange(@NonNull DataSnapshot snapshot) {
        list.clear();
        for (DataSnapshot memorySnapshot : snapshot.getChildren()) {
          Memory m = memorySnapshot.getValue(Memory.class);
          if (m.getCreator().equals(email))
            list.add(m);
        }
        if (list.isEmpty()) {
          Toast.makeText(getContext(), "No memories found", Toast.LENGTH_SHORT).show();
        } else {
          if (binding != null) {
            //set GridLayoutManager in recyclerView and show items in grid with two columns
            binding.recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
            //set adapter ItemAdapter in recyclerView
            binding.recyclerView.setAdapter(new MemoriesListAdapter(list, getContext()));
          }
        }
      }

      @Override
      public void onCancelled(@NonNull DatabaseError error) {
        // calling on cancelled method when we receive
        // any error or we are not able to get the data.
        Toast.makeText(getContext(), "Fail to get data.", Toast.LENGTH_SHORT).show();
      }
    });

  }
}