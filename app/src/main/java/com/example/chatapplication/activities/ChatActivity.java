package com.example.chatapplication.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;


import com.example.chatapplication.R;
import com.example.chatapplication.adapters.ChatAdapter;
import com.example.chatapplication.databinding.ActivityChatBinding;
import com.example.chatapplication.models.ChatMessage;
import com.example.chatapplication.models.User;
import com.example.chatapplication.utilities.Constants;
import com.example.chatapplication.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

   private ActivityChatBinding binding;
   private User receiverUser;
   private List<ChatMessage> chatMessages;
   private ChatAdapter chatAdapter;
   private PreferenceManager preferenceManager;
   private FirebaseFirestore database;

   @Override
    protected void onCreate(Bundle savedInstance){
       super.onCreate(savedInstance);
       binding = ActivityChatBinding.inflate(getLayoutInflater());
       setContentView(binding.getRoot());
       loadReceiverDetail();
       setListeners();
       init();
       listenMessages();
   }

   private void init(){
       preferenceManager = new PreferenceManager(getApplicationContext());
       chatMessages = new ArrayList<>();
       chatAdapter = new ChatAdapter(
               chatMessages,
               getBitmapFromEncodedString(receiverUser.image),
               preferenceManager.getString(Constants.KEY_USER_ID)
       );
       binding.chatRecycleView.setAdapter(chatAdapter);
       database = FirebaseFirestore.getInstance();
   }

   private void sendMessage(){
       HashMap<String,Object> message = new HashMap<>();
       message.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
       message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
       message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
       message.put(Constants.KEY_TIMESTAMP, new Date());
       database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
       binding.inputMessage.setText(null);
   }

   private void listenMessages(){
       database.collection(Constants.KEY_COLLECTION_CHAT)
               .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
               .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
               .addSnapshotListener(eventListener);
       database.collection(Constants.KEY_COLLECTION_CHAT)
               .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
               .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
               .addSnapshotListener(eventListener);
   }

   private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
       if(error != null){
           return;
       }
       if(value != null){
           int count = chatMessages.size();
           for (DocumentChange documentChange : value.getDocumentChanges()){
                if (documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
           }
           Collections.sort(chatMessages, (obj1,obj2)-> obj1.dateObject.compareTo(obj2.dateObject));
           if(count == 0){
               chatAdapter.notifyDataSetChanged();
           }else{
               chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
               binding.chatRecycleView.smoothScrollToPosition(chatMessages.size()-1);
           }
           binding.chatRecycleView.setVisibility(View.VISIBLE);
       }
       binding.progressBar.setVisibility(View.GONE);
   };

   private Bitmap getBitmapFromEncodedString(String encodedImage){
       byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
       return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
   }
    private void loadReceiverDetail(){
       receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
       binding.textName.setText(receiverUser.name);
    }

    public void setListeners(){
            binding.imageBack.setOnClickListener(v->onBackPressed());
            binding.layoutSend.setOnClickListener(v->sendMessage());
    }
    private String getReadableDateTime(Date date){
       return new SimpleDateFormat("MMMM dd, yyyy - hh:mm",Locale.getDefault()).format(date);
    }
}