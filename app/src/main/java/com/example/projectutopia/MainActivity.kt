package com.example.projectutopia

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*


class MainActivity : AppCompatActivity()
{
    //UI Element
    var connected: Boolean = false;

    var txtAddressIP: EditText? = null;
    var txtAddressPort: EditText? = null;

    var wifiModuleIp: String? = null;
    var wifiModulePort: Int? = 0;
    var CMD = "0"

    //Progressbar
    var progressBar: ProgressBar? = null;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.prgBar) as ProgressBar;
        val btnConnect = findViewById(R.id.btn_connect) as Button;
        txtAddressIP = findViewById(R.id.ipAdr) as EditText;
        txtAddressPort = findViewById(R.id.portAdr) as EditText;

        btnConnect.setOnClickListener()
        {
            getIPandPort();
            ProgressStart();
        }
    }

    override fun onBackPressed()
    {
        super.onBackPressed();

        this.connected = false;

        Toast.makeText(this, "App closed", Toast.LENGTH_SHORT).show();
        finishAffinity();
    }

    fun ProgressStart()
    {
        var i = progressBar!!.progress
        var handler = Handler(Looper.getMainLooper());

       val tmpCoroutine = GlobalScope.async{
            while (i < 100)
            {
                i += 5
                handler.post(Runnable
                {
                    progressBar!!.progress = i
                })
                try
                {
                    Thread.sleep(100)
                }
                catch (e: InterruptedException)
                {
                    e.printStackTrace()
                }

            }
        }
        GlobalScope.launch(Dispatchers.Main) {
            tmpCoroutine.await();
            GoToViewActivity();
        }

    }
    fun GoToViewActivity()
    {
        println("connected = true");
        this.connected = true;
        val intent = Intent(baseContext, ViewActivity::class.java)
        println("putExtra");
        intent.putExtra("connected", connected);
        intent.putExtra("RPI_IP", wifiModuleIp);
        intent.putExtra("RPI_PORT", wifiModulePort);
        println("StartAct");
        startActivity(intent);
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    fun getIPandPort()
    {
        wifiModuleIp = txtAddressIP!!.text.toString();
        wifiModulePort = (txtAddressPort!!.text).toString().toIntOrNull();
    }
}


