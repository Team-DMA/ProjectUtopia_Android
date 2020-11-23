package com.example.projectutopia

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import br.com.simplepass.loadingbutton.customViews.CircularProgressButton
import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.security.SecureRandom
import java.util.*


class MainActivity : AppCompatActivity()
{
    //vars
    var connected: Boolean = false;

    var txtAddressIP: EditText? = null;
    //var txtAddressPort: EditText? = null;

    var wifiModuleIp: String? = null;
    var wifiModulePort: Int? = null;

    val pingPort: Int = 12346;

    lateinit var buttonConnect: CircularProgressButton;

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //INIT VALUES FROM INTENT
        val extras = intent.extras;
        if(extras != null)
        {
            this.connected = extras.getBoolean("connected");
            this.wifiModuleIp = null;
            this.wifiModulePort = null;
        }

        txtAddressIP = findViewById(R.id.ipAdr) as EditText;
        //txtAddressPort = findViewById(R.id.portAdr) as EditText;

        //DEBUG
        val txtViewTest = findViewById(R.id.textView2) as TextView;
        val btnDebug = findViewById(R.id.buttonDebug) as Button;
        txtViewTest.setText("Meine IP: ".plus(getLocalIpAddress()));
        btnDebug.setOnClickListener()
        {
            this.connected = true;
            getIPandPort();
            GoToViewActivity();
        }
        ////////
        
        buttonConnect = findViewById(R.id.btnConnect) as CircularProgressButton;
        buttonConnect.setOnClickListener()
        {
            if(txtAddressIP!!.text.isNotEmpty() /*&& txtAddressPort!!.text.isNotEmpty()*/) {
                buttonConnect.startAnimation();
                buttonConnect.isClickable = false;
                getIPandPort();

                val tmpCoroutine = GlobalScope.async {
                    ProgressStart();
                }

                GlobalScope.launch(Dispatchers.Main) {
                    tmpCoroutine.await();

                    val icon: Bitmap;
                    val color: Int;
                    if(connected == true)
                    {
                        icon = BitmapFactory.decodeResource(this@MainActivity.resources,R.drawable.ic_done_white_48dp)
                        color = Color.parseColor("#00FF00")
                        delay(3000);
                    }
                    else
                    {
                        icon = BitmapFactory.decodeResource(this@MainActivity.resources,R.drawable.ic_error_white_48dp)
                        color = Color.parseColor("#FF0000")
                    }
                    buttonConnect.doneLoadingAnimation(color,icon)
                    if(connected == false)
                    {
                        Toast.makeText(this@MainActivity, "No Connection", Toast.LENGTH_LONG).show();
                    }
                    delay(2000);
                    buttonConnect.isClickable = true;
                    GoToViewActivity();
                    buttonConnect.revertAnimation();
                }
            }
        }

        print("MainActivity() INIT FINISHED");
    }

    override fun onDestroy()
    {
        println("MainActivity OnDestroy ausgeführt.");
        super.onDestroy()
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
       val adr = InetAddress.getByName(wifiModuleIp);
       val port = wifiModulePort!!;

       println("Sende Ping an: " + adr + ":" + port);
       connected = SendReceiveUDP(adr, port, 10000)!!;
       println("connected = ".plus(connected));

    }

    fun SendReceiveUDP(adr: InetAddress, port: Int, timeout: Int): Boolean?
    {
        try
        {
            val buffer = ByteArray(6)
            val socket = DatagramSocket();
            socket.reuseAddress = true;
            var data: ByteArray? = null;
            val buffer2 = ByteArray(6);
            val packet = DatagramPacket(buffer2, buffer.size);

            SecureRandom.getInstanceStrong().nextBytes(buffer); //random bytes

            val out = DatagramPacket(buffer, buffer.size, adr, port)
            socket.send(out) // send to the server

            while (true)
            {
                try
                {
                    println("waiting for msg");

                    //val rcvPacket = receiveUDP(buffer.size, timeout);
                    var rcvPacket: DatagramPacket? = null;
                    val socket = DatagramSocket(pingPort);
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
                        println("Paket null");
                        break;
                    }
                    println("Msg: ".plus(rcvPacket!!.data));
                    val rcvd =
                        "received from " + rcvPacket.getAddress().toString() + ", " + rcvPacket.getPort()
                            .toString() + ": " + String(
                            rcvPacket.getData(),
                            0,
                            rcvPacket.getLength()
                        )
                    println(rcvd)
                    return rcvPacket.data.contentEquals(buffer);
                }
                catch (e: SocketTimeoutException)
                {
                    println("Timeout reached!!! $e")
                    socket.close()
                    return false;
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
        return false;
    }

    fun getLocalIpAddress(): String? {
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                val enumIpAddr: Enumeration<InetAddress> = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress: InetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }
        return null
    }

    fun GoToViewActivity()
    {
        if(this.connected == true)
        {
            val intent = Intent(baseContext, ViewActivity::class.java)
            intent.putExtra("PingPort", pingPort);
            intent.putExtra("connected", connected);
            intent.putExtra("RPI_IP", wifiModuleIp);
            intent.putExtra("RPI_PORT", wifiModulePort);
            startActivity(intent);
            finish();
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        }
        else
        {
            println("No Connection in GoToViewActivity()");
        }
    }

    fun getIPandPort()
    {
        wifiModuleIp = txtAddressIP!!.text.toString();
        //wifiModulePort = (txtAddressPort!!.text).toString().toIntOrNull();
        wifiModulePort = 12345;
    }
}


