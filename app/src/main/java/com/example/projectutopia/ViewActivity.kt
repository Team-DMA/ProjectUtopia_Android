package com.example.projectutopia

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.tomtom.online.sdk.common.location.LatLng
import com.tomtom.online.sdk.map.*
import com.tomtom.online.sdk.map.gestures.GesturesConfiguration
import eo.view.batterymeter.BatteryMeterView
import io.github.controlwear.virtual.joystick.android.JoystickView
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.*
import kotlin.math.roundToInt


class ViewActivity : AppCompatActivity(), OnMapReadyCallback
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

    private lateinit var map: TomtomMap

    var checkConPingPort: Int = 0;
    val tcpPort: Int = 12347;
    val rcvUDP_Port: Int = 12348;

    lateinit var picCompass: ImageView;
    lateinit var heightTxt: TextView;
    lateinit var tempTxt: TextView;
    lateinit var batteryStatus: BatteryMeterView;
    lateinit var batteryLevel: TextView;
    lateinit var mapsImage: ImageView;

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
       // heightTxt.setText((100..1400).random().toString().plus("m"));
        tempTxt = findViewById(R.id.temp) as TextView;
        //tempTxt.setText((5..35).random().toString().plus("\u2103"));

        VidErrorTxt = findViewById(R.id.VideoErrorText) as TextView;
        VidErrorTxt.setText("No Error"); //DEFAULT
        VidErrorTxt.visibility = TextView.INVISIBLE; //INVISIBLE
        //blackscreen
        blackscreen = findViewById(R.id.blackscreenView) as ImageView;
        blackscreen.visibility = View.INVISIBLE;

        picCompass = findViewById(R.id.compassPic) as ImageView;

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as MapFragment
        mapFragment.getAsyncMap(this)

        batteryStatus = findViewById(R.id.batteryView) as BatteryMeterView;
        batteryLevel = findViewById(R.id.batteryLevel) as TextView;
        batteryStatus.isCharging = false;

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

                    val rcvMsgs = RcvPackets();

                    val splittedMsg = rcvMsgs?.split("|");

                    val compassValueString = splittedMsg?.elementAt(0);
                    val tempValueString = splittedMsg?.elementAt(1)
                    val altValueString = splittedMsg?.elementAt(2);
                    val pressValueString = splittedMsg?.elementAt(3);
                    val gpsLongString = splittedMsg?.elementAt(4);
                    val gpsLatString = splittedMsg?.elementAt(5);
                    val gpsAltString = splittedMsg?.elementAt(6);
                    val batteryValueString = splittedMsg?.elementAt(7);

                    val tempValue = ((tempValueString)?.toFloat())?.roundToInt();
                    val tempString = tempValue.toString();

                    val altValue = ((altValueString)?.toFloat())?.roundToInt();
                    val altString = altValue.toString();

                    val batteryValue = ((batteryValueString)?.toFloat())?.roundToInt();

                    //height
                    heightTxt.setText((altString + "m"));
                    //temp
                    tempTxt.setText(tempString.plus("\u2103"));
                    //battery
                    changeBatteryLevel(batteryValue);

                    //map
                    val compassValue = compassValueString?.toFloat();

                    val gpsLatValue = gpsLatString?.toDouble();
                    val gpsLongValue = gpsLongString?.toDouble();
                    if (compassValue != null && gpsLatValue != null && gpsLongValue != null) {
                        setUpMap(gpsLatValue, gpsLongValue, compassValue)
                    }

                    //compass
                    runOnUiThread {
                        val runnable: Runnable = object : Runnable {
                            override fun run() {
                                if (compassValue != null) {
                                    picCompass.animate().rotation(compassValue).withEndAction(this)
                                        .setDuration(
                                            950
                                        ).setInterpolator(LinearInterpolator()).start()
                                }
                            }
                        }
                        if (compassValue != null) {
                            picCompass.animate().rotation(compassValue).withEndAction(runnable)
                                .setDuration(
                                    950
                                ).setInterpolator(LinearInterpolator()).start()
                        }
                    }

                    Thread.sleep(1000);

                    /*
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
                    }*/

                } catch (e: Exception) {
                    println("RcvWifiDataThread-Error: " + e.toString())
                    e.printStackTrace()
                }
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

    fun getMapImage(latitude: Double, longitude: Double): Bitmap?
    {
        println("passt 1.")
        var bmp: Bitmap? = null
        var inputStream: InputStream? = null
        try {
            val mapUrl =
                URL("http://maps.google.com/maps/api/staticmap?center=$latitude,$longitude&zoom=15&size=200x200&sensor=false")
            val httpURLConnection = mapUrl.openConnection() as HttpURLConnection
            inputStream = BufferedInputStream(httpURLConnection.inputStream)
            bmp = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            httpURLConnection.disconnect()
        } catch (e: IllegalStateException) {
            println(e.toString())
        } catch (e: IOException) {
            println(e.toString())
        }
        return bmp
    }

    override fun onBackPressed()
    {
        super.onBackPressed();

        BackToMainMenu();
    }

    fun changeBatteryLevel(value: Int?)
    {
        batteryStatus.chargeLevel = value;
        batteryLevel.setText(value?.toString().plus("%"));
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

    fun RcvPackets(): String?
    {
        val buffer = ByteArray(1024)
        var socket: DatagramSocket? = null
        try
        {
            socket = DatagramSocket(this.rcvUDP_Port);
            socket.broadcast = true;
            val packet = DatagramPacket(buffer, buffer.size);
            socket.receive(packet);
            val rcvMsg = String(packet.data, Charsets.UTF_8);
            println("RcvPackets() packet received = " + rcvMsg);
            return rcvMsg;
        }
        catch (e: Exception)
        {
            println("RcvPackets-Error: " + e.toString())
            e.printStackTrace()
            return null;
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

    private fun setUpMap(lat: Double, long: Double, orientation: Float)
    {
        val currentLatLng = LatLng(lat, long)
        val balloon = SimpleMarkerBalloon("")
        map.removeMarkers()
        map.addMarker(MarkerBuilder(currentLatLng).markerBalloon(balloon).draggable(false))
        val focusArea =
            CameraPosition.builder()
                .focusPosition(currentLatLng)
                .zoom(19.0)
                .apply {
                    bearing(orientation.toDouble())
                    pitch(75.0)
                }
                .build()
        map.centerOn(focusArea);
    }
    override fun onMapReady(@NonNull tomtomMap: TomtomMap)
    {
        this.map = tomtomMap

        map.updateGesturesConfiguration(
            GesturesConfiguration.Builder()
                .zoomEnabled(false)
                .rotationEnabled(false)
                .tiltEnabled(false)
                .panningEnabled(false)
                .build()
        )

        map.isMyLocationEnabled = false
        map.uiSettings.compassView.hide()
        map.uiSettings.currentLocationView.hide()
        map.uiSettings.panningControlsView.hide()
        map.uiSettings.zoomingControlsView.hide()
        map.uiSettings.logoView.setGravity(0)
        map.uiSettings.logoView.setMargins(10000000, 10000000, 10000000, 10000000)
        map.set3DMode();
        //setUpMap(48.89080790898993, 8.695337466052832, 270.0f)
    }
}