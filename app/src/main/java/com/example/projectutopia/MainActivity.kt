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

    var connected: Boolean? = null;

    var txtAddress: EditText? = null

    var wifiModuleIp = ""
    var wifiModulePort = 0
    var CMD = "0"

    //Progressbar
    var progressBar: ProgressBar? = null;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progressBar = findViewById(R.id.prgBar) as ProgressBar;
        val btnConnect = findViewById(R.id.btn_connect) as Button;
        txtAddress = findViewById<EditText>(R.id.ipAdr);

        //Variablen abgleichen
        this.connected = ViewActivity().connected;

        //

        btnConnect.setOnClickListener()
        {
            ProgressStart();
        }
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
        connected = true;
        val intent = Intent(this, ViewActivity::class.java)
        startActivity(intent);
        Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
    }

    fun getIPandPort()
    {
        val iPandPort: String = txtAddress.toString()
        val temp = iPandPort.split(":".toRegex()).toTypedArray()
        wifiModuleIp = temp[0]
        wifiModulePort = Integer.valueOf(temp[1])
    }
}


