package com.example.myapplication;

import android.Manifest;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    int connect_flag=0;
    boolean sleep_flag = false;
    String command ="none";
    public String[] permissions = new String[]{
            Manifest.permission.INTERNET ,
            Manifest.permission.CHANGE_NETWORK_STATE ,
            Manifest.permission.ACCESS_NETWORK_STATE   ,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE   ,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
            //           Manifest.permission.READ_CONTACTS,  //我们不需要联系人
//            Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this, permissions,1);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connect_flag==0){
                    //connect Func
                    Snackbar.make(view, "connect success!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    connect_flag=1;
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                    sleep_flag=false;
                }else if(connect_flag==1){
                    if(sleep_flag){
                        command = "send_picture";
                        fab.setImageResource(android.R.drawable.ic_media_pause);
                    }else {
                        command = "none";
                        fab.setImageResource(android.R.drawable.ic_media_play);
                    }
                    sleep_flag=!sleep_flag;//取反

                }



            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.Item_recognition) {
            return true;
        }
        if (id == R.id.Item_trainning) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
