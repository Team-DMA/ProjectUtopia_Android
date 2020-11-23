package com.example.projectutopia

import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import io.github.controlwear.virtual.joystick.android.JoystickView
import java.io.IOException
import java.io.PrintWriter
import java.net.*
import java.security.SecureRandom
import java.util.*
import kotlin.concurrent.thread
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

    var checkConPingPort: Int = 0;
    val tcpPort: Int = 12347;
    val rcvUDP_Port: Int = 12348;

    lateinit var VidErrorTxt: TextView;

    var threadsStarted: Boolean = false;
    var SendWifiDataThread: Thread = Thread();
    var RcvWifiDataThread: Thread = Thread();
    var ConnectionChecker: Thread = Thread();
    var UpdateThread: Thread = Thread();

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view)

        //INIT VALUES FROM INTENT
        val extras = intent.extras;
        if(extras != null)
        {
            this.checkConPingPort = extras.getInt("PingPort");
            this.connected = extras.getBoolean("connected");
            this.rpiPort = extras.getInt("RPI_PORT");
            this.rpiIP = extras.getString("RPI_IP");
        }

        val videoViewer = findViewById(R.id.videoView) as VideoView;
        val joystickFB = findViewById(R.id.joystickView1) as JoystickView;
        val joystickLR = findViewById(R.id.joystickView2) as JoystickView;
        val heightTxt = findViewById(R.id.height) as TextView;
        heightTxt.setText((100..1000).random().toString().plus("m"));
        VidErrorTxt = findViewById(R.id.VideoErrorText) as TextView;
        VidErrorTxt.setText("No Error"); //DEFAULT
        VidErrorTxt.visibility = TextView.INVISIBLE; //INVISIBLE

        //INIT
        cmd = "InitMsg";
        sendFlag = false;


        //VIDEO LIVESTREAM
        try
        {
            val vidLink = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            val video: Uri = Uri.parse(vidLink);
            videoViewer.setVideoURI(video);
            videoViewer.requestFocus();

            videoViewer.setOnPreparedListener(OnPreparedListener {
                videoViewer.start()
            })
        }
        catch (e: Exception)
        {
            errorInVideo(e);
        }

        //LISTENER
        joystickFB.setOnMoveListener{ angle, strength ->
            this.LJoystickAngle = angle;
            this.LJoystickStrength = strength;
        }

        joystickLR.setOnMoveListener { angle, strength ->
            this.RJoystickAngle = angle;
            this.RJoystickStrength = strength;
        }

        videoViewer.setOnErrorListener(MediaPlayer.OnErrorListener { mediaPlayer, i, i1 ->
            errorInVideo(null);
            Thread.sleep(2000);
            BackToMainMenu();
            true
        })

        //THREADS
        threadsStarted = true;

        SendWifiDataThread = Thread(Runnable {
            while (threadsStarted) {
                SendPackets(cmd);
            }
        })
        SendWifiDataThread.start();

        RcvWifiDataThread = Thread(Runnable {
            while(threadsStarted)
            {
                RcvPackets();
            }
        })
        RcvWifiDataThread.start();

        ConnectionChecker = Thread(Runnable {
            while (threadsStarted) {
                ConnectionCheck();
                Thread.sleep(1500);
            }
        })
        ConnectionChecker.start();

        UpdateThread = Thread(Runnable {
            while (threadsStarted) {
                Update();
            }
        })
        UpdateThread.start();

        print("ViewActivity() INIT FINISHED");
    }
    override fun onDestroy()
    {
        println("ViewActivity OnDestroy ausgeführt.");
        super.onDestroy()
    }

    fun errorInVideo(error: Exception?)
    {
        if(error != null)
        {
            VidErrorTxt.setText("Error beim Laden: " + error.toString());
            println("Video-Error: " + error.toString());
        }
        else
        {
            VidErrorTxt.setText("Error beim Laden.");
            println("Video-Error.");
        }
        VidErrorTxt.visibility = TextView.VISIBLE;
    }

    override fun onBackPressed()
    {
        super.onBackPressed();

        BackToMainMenu();
    }

    fun isHostAvailable(host: String?, port: Int, timeout: Int): Boolean
    {
        val socket = Socket();
        try
        {
            val inetAddress = InetAddress.getByName(host);
            val inetSocketAddress = InetSocketAddress(inetAddress, port);
            println("Check Con of: " + inetSocketAddress);
            socket.connect(inetSocketAddress, timeout);
            println("Erfolgreiche Con.");
            return true;
        }
        catch (e: IOException)
        {
            println("HostCheck Error: " + e.toString())
            e.printStackTrace()
            return false
        }
        finally
        {
            socket.close()
        }
    }

    fun ConnectionCheck()
    {
        try
        {
            println("Check Connection...");
            connected = isHostAvailable(this.rpiIP, this.tcpPort, 5000);
        }
        catch (e: Exception)
        {
            print("ConnectionCheck-Error: " + e.toString());
        }
    }

    fun RcvPackets()
    {
        val buffer = ByteArray(1024)
        var socket: DatagramSocket? = null
        try
        {
            socket = DatagramSocket(this.rcvUDP_Port)
            socket.broadcast = true
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)
            val rcvMsg = String(packet.data, Charsets.UTF_8);
            println("RcvPackets() packet received = " + rcvMsg);

        }
        catch (e: Exception)
        {
            println("RcvPackets-Error: " + e.toString())
            e.printStackTrace()
        }
        finally
        {
            socket?.close()
        }
    }

    fun Update()
    {
        Thread.sleep(200); //kurz warten
        if(connected == false)
        {
            this@ViewActivity.runOnUiThread(java.lang.Runnable {
                BackToMainMenu();
            })
        }
        if(sendFlag == false)
        {
            var Ldirection: String = "Unknown";
            var Rdirection: String = "Unknown";
            var LstrengthTmp: Float = 0.0F;
            var RstrengthTmp: Float = 0.0F;
            if(LJoystickStrength  != 0)
            {
                LstrengthTmp = (LJoystickStrength.toFloat() / 10);
            }
            if(RJoystickStrength != 0)
            {
                RstrengthTmp = (RJoystickStrength.toFloat() / 10);
            }

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

            SendCmds(sendString);
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
                    //println("Addresse: ".plus(address).plus(":").plus(port));
                    packet = DatagramPacket(message, message.size, address, port!!.toInt())
                    println("Send bytes: ".plus(message));
                    try
                    {
                        //SEND
                        socket.send(packet)
                    }
                    catch (e: Exception)
                    {
                        System.err.println("Socket-Send-Error: " + e.toString());
                    }
                    finally {
                        sendFlag = false;
                        socket.close();
                    }
                }
            }
        }
    }

    fun BackToMainMenu()
    {
        this.threadsStarted = false;
        this.connected = false;
        val intent = Intent(this, MainActivity::class.java);
        intent.putExtra("connected", connected);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
    }
}