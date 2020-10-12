package com.example.projectutopia

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.system.Os.socket
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException


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
            Thread.sleep(200);
            GoToViewActivity();
        }
    }

    fun ProgressStart()
    {
        var temp = false;
        var handler = Handler();
        var i = progressBar!!.progress
        Thread(Runnable {
            while (i < 100) {
                i += 5
                // Update the progress bar and display the current value
                handler.post(Runnable
                {
                    progressBar!!.progress = i
                })
                try {
                    Thread.sleep(50)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
            temp = true;
        }).start()
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

    /*public class Socket_AsyncTask : AsyncTask<Void, Void, Void>()
    {
        override fun doInBackground(vararg params: Void?): Void?
        {
            try
            {
                val inetAddress: InetAddress = InetAddress.getByName(MainActivity.wifiModuleIp)
                socket = Socket(inetAddress, MainActivity.wifiModulePort)
                val dataOutputStream = DataOutputStream(socket.getOutputStream())
                dataOutputStream.writeBytes(CMD)
                dataOutputStream.close()
                socket.close()
            }
            catch (e: UnknownHostException)
            {
                e.printStackTrace()
            }
            catch (e: IOException)
            {
                e.printStackTrace()
            }
            return null
        }
    }*/

}


