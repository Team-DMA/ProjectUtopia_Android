package com.example.projectutopia

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.*
import java.security.SecureRandom
import java.util.*


class MainActivity : AppCompatActivity()
{
    //UI Element
    var connected: Boolean = false;

    lateinit var btnConnect: Button;

    var txtAddressIP: EditText? = null;
    var txtAddressPort: EditText? = null;

    var wifiModuleIp: String? = null;
    var wifiModulePort: Int? = 0;
    var CMD = "0"

    val pingPort: Int = 12346;

    //Progressbar
    var progressBar: ProgressBar? = null;

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

        progressBar = findViewById(R.id.prgBar) as ProgressBar;
        btnConnect = findViewById(R.id.btn_connect) as Button;
        txtAddressIP = findViewById(R.id.ipAdr) as EditText;
        txtAddressPort = findViewById(R.id.portAdr) as EditText;

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

        btnConnect.setOnClickListener()
        {
            if(txtAddressIP!!.text.isNotEmpty() && txtAddressPort!!.text.isNotEmpty())
            {
                btnConnect.isClickable = false;
                getIPandPort();
                ProgressStart();
            }
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

           progressBar!!.progress = 0; //PROGRESSBAR
           Thread.sleep(100);

           progressBar!!.progress = 10; //PROGRESSBAR
           Thread.sleep(100);

           val adr = InetAddress.getByName(wifiModuleIp);
           val port = wifiModulePort!!;
           progressBar!!.progress = 20; //PROGRESSBAR
           Thread.sleep(100);
           progressBar!!.progress = 30; //PROGRESSBAR
           Thread.sleep(100);
           progressBar!!.progress = 40; //PROGRESSBAR
           val sock = Socket();
           Thread.sleep(100);
           progressBar!!.progress = 50; //PROGRESSBAR
           Thread.sleep(100);
           progressBar!!.progress = 60; //PROGRESSBAR

           println("Sende Ping an: " + adr + ":" + port);
           connected = SendReceiveUDP(adr, port, 10000)!!;

           progressBar!!.progress = 70; //PROGRESSBAR
           Thread.sleep(100);
           progressBar!!.progress = 80; //PROGRESSBAR
           Thread.sleep(100);
           progressBar!!.progress = 90; //PROGRESSBAR
           Thread.sleep(100);
           println("var connected = ".plus(connected));
           progressBar!!.progress = 100; //PROGRESSBAR
           Thread.sleep(50);
        }
        GlobalScope.launch(Dispatchers.Main) {
            tmpCoroutine.await();
            btnConnect.isClickable = true;
            GoToViewActivity();
        }

    }

    fun receiveUDP(bufferSize: Int, timeout: Int): DatagramPacket?
    {
        print("receiveUDP.");
        print("Warte auf Nachrichten zum Port: "+ pingPort);
        val socket = DatagramSocket(pingPort);
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
                            .toString() + ": " + String(rcvPacket.getData(), 0, rcvPacket.getLength())
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
        println("this.connected = ".plus(this.connected));
        if(this.connected == true)
        {
            val intent = Intent(baseContext, ViewActivity::class.java)
            intent.putExtra("PingPort", pingPort);
            intent.putExtra("connected", connected);
            intent.putExtra("RPI_IP", wifiModuleIp);
            intent.putExtra("RPI_PORT", wifiModulePort);
            startActivity(intent);
            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        }
        else
        {
            this.progressBar!!.progress = 0;
            Toast.makeText(this, "No Connection", Toast.LENGTH_LONG).show();
        }
    }

    fun getIPandPort()
    {
        wifiModuleIp = txtAddressIP!!.text.toString();
        wifiModulePort = (txtAddressPort!!.text).toString().toIntOrNull();
    }
}


