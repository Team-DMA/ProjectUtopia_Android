package com.example.projectutopia

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import io.github.controlwear.virtual.joystick.android.JoystickView
import org.w3c.dom.Text

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt


class ViewActivity : AppCompatActivity()
{

    var connected: Boolean = false;

    var rpiIP: String? = null;
    var rpiPort: Int = 0;

    var cmd: String = "InitMsg";
    var sendFlag: Boolean = false;

    var LJoystickAngle : Int = 0;
    var LJoystickStrength: Int = 0;
    var RJoystickAngle: Int = 0;
    var RJoystickStrength: Int = 0;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        //INIT VALUES FROM INTENT
        val extras = intent.extras;
        if(extras != null)
        {
            this.connected = extras.getBoolean("connected");
            this.rpiPort = extras.getInt("RPI_PORT");
            this.rpiIP = extras.getString("RPI_IP");
        }

        val videoViewer = findViewById(R.id.videoView) as VideoView;
        val joystickFB = findViewById(R.id.joystickView1) as JoystickView;
        val joystickLR = findViewById(R.id.joystickView2) as JoystickView;
        val heightTxt = findViewById(R.id.height) as TextView;
        heightTxt.setText((100..1000).random().toString().plus("m"));

        //INIT
        cmd = "InitMsg";
        sendFlag = false;


        //VIDEO LIVESTREAM
        val vidLink = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        val video: Uri = Uri.parse(vidLink)
        videoViewer.setVideoURI(video)
        videoViewer.requestFocus()
        videoViewer.start()

        //LISTENER
        joystickFB.setOnMoveListener{ angle, strength ->
            this.LJoystickAngle = angle;
            this.LJoystickStrength = strength;
        }

        joystickLR.setOnMoveListener { angle, strength ->
            this.RJoystickAngle = angle;
            this.RJoystickStrength = strength;
        }

        //THREADS
        val WifiThread = Thread(Runnable {
            while(true)
            {
                SendPackets(cmd);
            }
        }).start();
        val RsvMsgs = Thread(Runnable {
            while(true)
            {
                ReceiveMsgs();
            }
        }).start();

        val UpdateThread = Thread(Runnable {
            while(true)
            {
                Update();
            }
        }).start();
    }

    override fun onBackPressed()
    {
        super.onBackPressed();

        BackToMainMenu();
    }

    fun ReceiveMsgs()
    {

    }

    fun Update()
    {
        Thread.sleep(200); //kurz warten
        if(sendFlag == false)
        {
            if ((LJoystickAngle != 0 && LJoystickStrength != 0) || (RJoystickAngle != 0 && RJoystickStrength != 0))
            {
                var Ldirection: String = "Unknown";
                var Rdirection: String = "Unknown";
                var LstrengthTmp: Float = (LJoystickStrength.toFloat() / 10);
                var RstrengthTmp: Float = (RJoystickStrength.toFloat() / 10);
                if (LJoystickAngle >= 0 && LJoystickAngle < 180) {
                    Ldirection = "F";
                }
                if (LJoystickAngle >= 180 && LJoystickAngle < 360) {
                    Ldirection = "B";
                    LstrengthTmp = -(LstrengthTmp);
                }
                if (RJoystickAngle >= 90 && RJoystickAngle < 270) {
                    Rdirection = "L";
                    RstrengthTmp = -(RstrengthTmp);
                }
                if ((RJoystickAngle >= 270 && RJoystickAngle < 360) || (RJoystickAngle >= 0 && RJoystickAngle < 90)) {
                    Rdirection = "R";
                }
                val Lstrength = (LstrengthTmp.roundToInt()).toString();
                val Rstrength = (RstrengthTmp.roundToInt()).toString();
                val sendString: String =
                    Lstrength.plus("|").plus(Ldirection).plus("|").plus(Rstrength).plus("|")
                        .plus(Rdirection);

                //NUR ZUM DEBUG
                /*this@ViewActivity.runOnUiThread(java.lang.Runnable {
                    val tmpTextView = findViewById(R.id.sendMsgText) as TextView;
                    tmpTextView.setText("Gesendeter Befehl: ".plus(sendString));
                }) */
                //////////////////////////

                SendCmds(sendString);
            }
        }
    }

    fun SendCmds(s: String)
    {
        this.cmd = s;
        this.sendFlag = true;
    }

    fun SendPackets(cmd: String)
    {
        if(connected == true)
        {
            if (cmd != "InitMsg")
            {
                if (sendFlag == true)
                {
                    val port: Int? = this.rpiPort;
                    //var port: Int? = 12345;
                    val message = cmd.toByteArray(Charsets.UTF_8)
                    var packet: DatagramPacket? = null
                    var address: InetAddress? = null
                    var socket: DatagramSocket? = null
                    // Create a datagram socket
                    socket = DatagramSocket()
                    // Get the internet address of the host
                    //address = InetAddress.getByName("192.168.2.114");
                    address = InetAddress.getByName(this.rpiIP) //  the address of the rpi
                    println("Msg: ".plus(cmd))
                    println("Addresse: ".plus(address).plus(":").plus(port));
                    packet = DatagramPacket(message, message.size, address, port!!.toInt())
                    println("Send bytes: ".plus(message));
                    try
                    {
                        //SEND
                        socket.send(packet)
                        sendFlag = false;
                    }
                    catch (e: Exception)
                    {
                        System.err.println(e)
                    }
                    finally {
                        socket.close();
                    }
                }
            }
        }
    }

    fun BackToMainMenu()
    {
        this.connected = false;
        MainActivity().connected = false;
        val intent = Intent(this, MainActivity::class.java);
        intent.putExtra("connected", connected);
        startActivity(intent);
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }
}