package com.example.projectutopia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import io.github.controlwear.virtual.joystick.android.JoystickView


class ViewActivity : AppCompatActivity()
{

    var connected: Boolean? = null;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        //Variablen abgleichen
        this.connected = MainActivity().connected;

        //

        val btnGoBack = findViewById(R.id.btn_back) as Button;
        val videoViewer = findViewById(R.id.videoView) as VideoView;

        val mTextViewAngleLeft = findViewById(R.id.textView_angle_left) as TextView;
        val mTextViewStrengthLeft = findViewById(R.id.textView_strength_left) as TextView;
        val joystick = findViewById(R.id.joystickView) as JoystickView


        //VIDEO LIVESTREAM
        //val vidLink = "http://192.168.2.107:8080/video/mpjeg"
        val vidLink = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val video: Uri = Uri.parse(vidLink)
        videoViewer.setVideoURI(video)
        videoViewer.requestFocus()
        videoViewer.start()

        //LISTENER
        joystick.setOnMoveListener { angle, strength ->
            mTextViewAngleLeft.setText(angle.toString().plus("°"));
            mTextViewStrengthLeft.setText(strength.toString().plus("%"));
        }

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