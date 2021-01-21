package com.example.projectutopia

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import eo.view.batterymeter.BatteryMeterView
import io.github.controlwear.virtual.joystick.android.JoystickView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.IOException
import java.net.*
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

    lateinit var picCompass: ImageView;
    lateinit var heightTxt: TextView;
    lateinit var tempTxt: TextView;
    lateinit var batteryStatus: BatteryMeterView;
    lateinit var batteryLevel: TextView;

    lateinit var VidErrorTxt: TextView;

    var threadsStarted: Boolean = false;
    var SendWifiDataThread: Thread = Thread();
    var RcvWifiDataThread: Thread = Thread();
    var ConnectionChecker: Thread = Thread();
    var UpdateThread: Thread = Thread();

    //blackscreen
    lateinit var blackscreen: ImageView;


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
        if(this.rpiIP.isNullOrEmpty())
        {
            this.rpiIP = null;
        }

        //val videoViewer = findViewById(R.id.videoView) as VideoView;
        val webViewer = findViewById(R.id.webView) as WebView;
        val joystickFB = findViewById(R.id.joystickView1) as JoystickView;
        val joystickLR = findViewById(R.id.joystickView2) as JoystickView;
        heightTxt = findViewById(R.id.height) as TextView;
        heightTxt.setText((100..1400).random().toString().plus("m"));
        tempTxt = findViewById(R.id.temp) as TextView;
        tempTxt.setText((5..35).random().toString().plus("\u2103"));

        VidErrorTxt = findViewById(R.id.VideoErrorText) as TextView;
        VidErrorTxt.setText("No Error"); //DEFAULT
        VidErrorTxt.visibility = TextView.INVISIBLE; //INVISIBLE
        //blackscreen
        blackscreen = findViewById(R.id.blackscreenView) as ImageView;
        blackscreen.visibility = View.INVISIBLE;

        picCompass = findViewById(R.id.compassPic) as ImageView;

        batteryStatus = findViewById(R.id.batteryView) as BatteryMeterView;
        batteryLevel = findViewById(R.id.batteryLevel) as TextView;
        batteryStatus.isCharging = false;
        changeBatteryLevel((1..99).random().toInt());

        //INIT
        cmd = "InitMsg";
        sendFlag = false;


        //VIDEO LIVESTREAM
        try
        {
            if(this.rpiIP.isNullOrEmpty())
            {
                blackscreen.visibility = View.VISIBLE;
                VidErrorTxt.setText("Can't load video.");
                VidErrorTxt.visibility = TextView.VISIBLE;
            }
            else
            {
                val vidLink = "http://"+this.rpiIP+":8080/stream/video.mjpeg"
                //val url = URL(vidLink)
                //val connection = url.openConnection() as HttpURLConnection
                //val code = connection.responseCode

                //if (code == 200)
                //{
                // reachable
                webViewer.getSettings().setLoadWithOverviewMode(true);
                webViewer.getSettings().setUseWideViewPort(true);
                webViewer.getSettings().setSupportZoom(false);
                webViewer.getSettings().setJavaScriptEnabled(true);
                webViewer.setVerticalScrollBarEnabled(false);
                webViewer.setHorizontalScrollBarEnabled(false);
                webViewer.setBackgroundColor(Color.TRANSPARENT);
                webViewer.isClickable = false;
                webViewer.loadUrl(vidLink);
                /*}
                else
                {
                    blackscreen.visibility = View.VISIBLE;
                    VidErrorTxt.setText("Can't load video.");
                    VidErrorTxt.visibility = TextView.VISIBLE;
                }*/
            }
            /* val vidLink = "http://"+this.rpiIP+":8080/stream/video.mjpeg"
             VidErrorTxt.setText(vidLink);
             VidErrorTxt.visibility = TextView.VISIBLE;
             val video: Uri = Uri.parse(vidLink);
             videoViewer.setVideoURI(video);
             videoViewer.requestFocus();

             videoViewer.setOnPreparedListener(OnPreparedListener {
                 videoViewer.start()
             })*/
        }
        catch (e: Exception)
        {
            //debug
            errorInVideo(e);
        }
        /*GlobalScope.async {
            try
            {
                if(rpiIP.isNullOrEmpty())
                {
                    blackscreen.visibility = View.VISIBLE;
                    VidErrorTxt.setText("Can't load video.");
                    VidErrorTxt.visibility = TextView.VISIBLE;
                }
                else
                {
                    val vidLink = "http://www.google.com/"
                    //val vidLink = "http://"+this.rpiIP+":8080/stream/video.mjpeg"
                    //val url = URL(vidLink)
                    //val connection = url.openConnection() as HttpURLConnection
                    //val code = connection.responseCode

                    //if (code == 200)
                    //{
                    // reachable
                    webViewer.getSettings().setLoadWithOverviewMode(true);
                    webViewer.getSettings().setUseWideViewPort(true);
                    webViewer.getSettings().setSupportZoom(false);
                    webViewer.getSettings().setJavaScriptEnabled(true);
                    webViewer.setVerticalScrollBarEnabled(false);
                    webViewer.setHorizontalScrollBarEnabled(false);
                    webViewer.setBackgroundColor(Color.TRANSPARENT);
                    webViewer.isClickable = false;
                    webViewer.loadUrl(vidLink);
                    /*}
                    else
                    {
                        blackscreen.visibility = View.VISIBLE;
                        VidErrorTxt.setText("Can't load video.");
                        VidErrorTxt.visibility = TextView.VISIBLE;
                    }*/
                }
                /* val vidLink = "http://"+this.rpiIP+":8080/stream/video.mjpeg"
                 VidErrorTxt.setText(vidLink);
                 VidErrorTxt.visibility = TextView.VISIBLE;
                 val video: Uri = Uri.parse(vidLink);
                 videoViewer.setVideoURI(video);
                 videoViewer.requestFocus();

                 videoViewer.setOnPreparedListener(OnPreparedListener {
                     videoViewer.start()
                 })*/
            }
            catch (e: Exception)
            {
                //debug
                errorInVideo(e);
            }
        }*/


        //LISTENER
        joystickFB.setOnMoveListener{ angle, strength ->
            this.LJoystickAngle = angle;
            this.LJoystickStrength = strength;
        }

        joystickLR.setOnMoveListener { angle, strength ->
            this.RJoystickAngle = angle;
            this.RJoystickStrength = strength;
        }

        /*videoViewer.setOnErrorListener(MediaPlayer.OnErrorListener { mediaPlayer, i, i1 ->
            errorInVideo(null);
            Thread.sleep(5000);
            BackToMainMenu();
            true
        })*/

        //THREADS
        threadsStarted = true;

        SendWifiDataThread = Thread(Runnable {
            while (threadsStarted) {
                SendPackets(cmd);
            }
        })
        SendWifiDataThread.start();

        RcvWifiDataThread = Thread(Runnable {
            while (threadsStarted) {

                //debug SIMULATED VALUES
                try {
                    Thread.sleep(1000);

                    //height simulation
                    val valHeight =
                        ((heightTxt.text.toString().split("m").toTypedArray())[0]).toInt();
                    var rdmHeight = 0;
                    if (valHeight >= 140 && valHeight <= 1360) {
                        rdmHeight = (-10..10).random().toInt();
                    } else if (valHeight < 140) {
                        rdmHeight = (1..20).random().toInt();
                    } else if (valHeight > 1360) {
                        rdmHeight = (-20..-1).random().toInt();
                    }
                    heightTxt.setText((valHeight + rdmHeight).toString().plus("m"));

                    //temperature simulation
                    val valTemp =
                        ((tempTxt.text.toString().split("\u2103").toTypedArray())[0]).toInt();
                    var rdmTemp = 0;
                    if (valTemp >= 5 && valTemp <= 35) {
                        rdmTemp = (-3..3).random().toInt();
                    } else if (valTemp < 5) {
                        rdmTemp = (3..6).random().toInt();
                    } else if (valTemp > 35) {
                        rdmTemp = (-6..-3).random().toInt();
                    }
                    tempTxt.setText((valTemp + rdmTemp).toString().plus("\u2103"));

                    //compass simulation
                    val rdmFloat = (-40..40).random().toFloat();
                    runOnUiThread {
                        val runnable: Runnable = object : Runnable {
                            override fun run() {
                                picCompass.animate().rotationBy(rdmFloat).withEndAction(this)
                                    .setDuration(
                                        950
                                    ).setInterpolator(LinearInterpolator()).start()
                            }
                        }
                        picCompass.animate().rotationBy(rdmFloat).withEndAction(runnable)
                            .setDuration(
                                950
                            ).setInterpolator(LinearInterpolator()).start()
                    }

                } catch (e: Exception) {
                    println("RcvWifiDataThread-Error: " + e.toString())
                    e.printStackTrace()
                }


                //RcvPackets();
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
            blackscreen.visibility = View.VISIBLE;
            VidErrorTxt.setText("Error: " + error.toString());
            VidErrorTxt.visibility = TextView.VISIBLE;
            println("Video-Error: " + error.toString());
        }
        else
        {
            blackscreen.visibility = View.VISIBLE;
            VidErrorTxt.setText("Unbekannter Error beim Laden.");
            VidErrorTxt.visibility = TextView.VISIBLE;
            println("Video-Error.");
        }
        VidErrorTxt.visibility = TextView.VISIBLE;
    }

    override fun onBackPressed()
    {
        super.onBackPressed();

        BackToMainMenu();
    }

    fun changeBatteryLevel(value: Int)
    {
        batteryStatus.chargeLevel = value;
        batteryLevel.setText(value.toString().plus("%"));
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
            connected = true; //debug
            //connected = isHostAvailable(this.rpiIP, this.tcpPort, 5000);
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
            }
            if (LJoystickAngle >= 180 && LJoystickAngle < 360) {
                LstrengthTmp = -(LstrengthTmp);
            }
            if (RJoystickAngle >= 90 && RJoystickAngle < 270) {
                RstrengthTmp = -(RstrengthTmp);
            }
            if ((RJoystickAngle >= 270 && RJoystickAngle < 360) || (RJoystickAngle >= 0 && RJoystickAngle < 90)) {
            }
            val Lstrength = (LstrengthTmp.roundToInt()).toString();
            val Rstrength = (RstrengthTmp.roundToInt()).toString();
            val sendString: String =
                Lstrength.plus("|").plus(Rstrength);

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
                    var packet: DatagramPacket?;
                    var address: InetAddress?;
                    var socket: DatagramSocket?;
                    // Create a datagram socket
                    socket = DatagramSocket()
                    // Get the internet address of the host
                    //address = InetAddress.getByName("192.168.2.114");
                    address = InetAddress.getByName(this.rpiIP) //  the address of the rpi
                    //println("Msg: ".plus(cmd))
                    //println("Addresse: ".plus(address).plus(":").plus(port));
                    packet = DatagramPacket(message, message.size, address, port!!.toInt())
                    //println("Send bytes: ".plus(message));
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