package com.example.projectutopia

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ViewActivity : AppCompatActivity()
{

    var connected: Boolean? = null;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        //fullscreen
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION); //noch buggy
        //

        //Variablen abgleichen
        this.connected = MainActivity().connected;

        //

        val btnGoBack = findViewById(R.id.btn_back) as Button;

        btnGoBack.setOnClickListener()
        {
            BackToMainMenu();
        }
    }

    fun BackToMainMenu()
    {
        connected = false;
        val intent = Intent(this, MainActivity::class.java);
        startActivity(intent);
        Toast.makeText(this, "Disconnected", Toast.LENGTH_LONG).show();
    }
}