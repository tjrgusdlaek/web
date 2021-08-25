package com.example.webrtctest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class RoomMake extends AppCompatActivity {

    Button btn_make , btn_join ;
    EditText et_roomName ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_make);
        btn_make = findViewById(R.id.btn_make);
        btn_join = findViewById(R.id.btn_join);
        et_roomName = findViewById(R.id.et_roomName);

        btn_make.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = et_roomName.getText().toString();
                Intent intent = new Intent(RoomMake.this ,MainActivity.class);
                intent.putExtra("roomName",roomName);
                startActivity(intent);
            }
        });

        btn_join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = et_roomName.getText().toString();
                Intent intent = new Intent(RoomMake.this ,JoinActivity.class);
                intent.putExtra("roomName",roomName);
                startActivity(intent);
            }
        });
    }
}