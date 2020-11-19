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
import java.net.*
import java.security.SecureRandom
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

    lateinit var VidErrorTxt: TextView;

    var threadsStarted: Boolean = false;
    var WifiThread: Thread = Thread();
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

        WifiThread = Thread(Runnable {
            while (threadsStarted) {
                SendPackets(cmd);
            }
        })
        WifiThread!!.start();

        ConnectionChecker = Thread(Runnable {
            while (threadsStarted) {
                //ConnectionCheck();
                Thread.sleep(5000);
            }
        })
        ConnectionChecker!!.start();

        UpdateThread = Thread(Runnable {
            while (threadsStarted) {
                Update();
            }
        })
        UpdateThread!!.start();

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
            if (socket != null)
            {
                socket.close();
            }
        }
    }

   /* fun ConnectionCheck()
    {
        try
        {
            println("Check Connection...");
            connected = isHostAvailable(rpiIP, rpiPort, 10000);
        }
        catch (e: Exception)
        {
            print("Error: " + e.toString());
        }

    }*/

    fun ConnectionCheck()
    {
        try
        {
            val timeout = 10000; //10s
            val adr = InetAddress.getByName(rpiIP);
            val port = this.rpiPort;
            val buffer = ByteArray(4)
            val socket = DatagramSocket();
            socket.reuseAddress = true;
            var data: ByteArray? = null;
            val buffer2 = ByteArray(4);
            val packet = DatagramPacket(buffer2, buffer.size);

            SecureRandom.getInstanceStrong().nextBytes(buffer); //random bytes

            val out = DatagramPacket(buffer, buffer.size, adr, port)
            socket.send(out) // send to the server

            while (true)
            {
                try
                {
                    //val rcvPacket = receiveUDP(port,buffer.size, timeout);

                    var rcvPacket: DatagramPacket? = null;
                    val socket = DatagramSocket(this.checkConPingPort);
                    try
                    {
                        socket.soTimeout = timeout;
                        val text: String
                        val message = ByteArray(buffer.size)
                        val p = DatagramPacket(message, message.size)
                        socket.receive(p);
                        rcvPacket = p;
                    }
                    catch (e: SocketTimeoutException)
                    {
                        println("Timeout reached!!! $e")
                        socket.close()
                    }
                    catch (ex: IOException)
                    {
                        println(ex.message)
                    }
                    finally
                    {
                        socket.close()
                    }

                    if(rcvPacket == null) {
                        print("Ping nicht erhalten.");
                        connected = false;
                        break;
                    }
                    //val rcvd = "received from " + rcvPacket.getAddress().toString() + ", " + rcvPacket.getPort().toString() + ": " + String(rcvPacket.getData(), 0, rcvPacket.getLength());
                    //println(rcvd)
                    connected = rcvPacket.data.contentEquals(buffer);
                }
                catch (e: SocketTimeoutException)
                {
                    println("Timeout reached: $e")
                    socket.close()
                    connected = false;
                }
            }
        }
        catch (e1: SocketException)
        {
            System.out.println("Socket closed " + e1);
        }
        catch (e: IOException)
        {
            e.printStackTrace();
        }
    }

    fun receiveUDP(port: Int, bufferSize: Int, timeout: Int): DatagramPacket?
    {
        val socket = DatagramSocket(port);
        var tmp : DatagramPacket? = null;
        try
        {
            socket.soTimeout = timeout;
            val text: String
            val message = ByteArray(bufferSize)
            val p = DatagramPacket(message, message.size)
            socket.receive(p);
            tmp = p;
            text = String(message, 0, p.length)

        }
        catch (e: SocketTimeoutException)
        {
            println("Timeout reached!!! $e")
            socket.close()
        }
        catch (ex: IOException)
        {
            println(ex.message)
        }
        finally
        {
            socket.close()
            return tmp;
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
            //if ((LJoystickAngle != 0 && LJoystickStrength != 0) || (RJoystickAngle != 0 && RJoystickStrength != 0))
            //{
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
            //}
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
                    println("Addresse: ".plus(address).plus(":").plus(port));
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