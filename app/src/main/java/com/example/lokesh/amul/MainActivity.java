package com.example.lokesh.amul;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    EditText eip,emsg;
    ListView slv;
    String macAddress;
    String myIp;
    String[] algo={"AES","DES"};
    ArrayList<Ip> arrayList = new ArrayList<Ip>();
    Button send;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

//        StrictMode.setThreadPolicy(policy);
        //            InetAddress localhost = InetAddress.getLocalHost();
//            myIp=localhost.getHostAddress().trim();
        myIp = getIPAddress(true);
       // long ip = Long.parseLong(myIp, 16);

             Log.e("My Ip",myIp);
        slv = (ListView)findViewById(R.id.list);
        ExtractLocalIps extractLocalIps=new ExtractLocalIps();
        extractLocalIps.execute();
        Thread myThread=new Thread(new ChatActivity.MyServer(getApplicationContext()));
        myThread.start();





    }
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }




    public String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                ((i >> 24) & 0xFF);
    }


    private class ExtractLocalIps extends AsyncTask<Void,Void,Void>{
        public String   s_dns1 ;
        public String   s_dns2;
        public String   s_gateway;
        public String   s_ipAddress;
        public String   s_leaseDuration;
        public String   s_netmask;
        public String   s_serverAddress;
        TextView info;
        DhcpInfo d;
        WifiManager wifii;
        @Override

        protected Void doInBackground(Void... voids) {
            wifii = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifii.getConnectionInfo().getIpAddress();
            macAddress = wifii.getConnectionInfo().getMacAddress();
            d = wifii.getDhcpInfo();
            s_dns1 = "DNS 1: " + String.valueOf(d.dns1);
            s_dns2 = "DNS 2: " + String.valueOf(d.dns2);
            s_gateway = "Default Gateway: " + String.valueOf(d.gateway);
            s_ipAddress = "IP Address: " + String.valueOf(d.ipAddress);
            s_leaseDuration = "Lease Time: " + String.valueOf(d.leaseDuration);
            s_netmask = "Subnet Mask: " + String.valueOf(d.netmask);
            s_serverAddress = "Server IP: " + String.valueOf(d.serverAddress);
            String connections = "";
            InetAddress host;
            try
            {
                host = InetAddress.getByName(intToIp(d.dns1));
                byte[] ip = host.getAddress();

                for(int i = 1; i <= 254; i++)
                {
                    ip[3] = (byte) i;
                    InetAddress address = InetAddress.getByAddress(ip);
                    if(address.isReachable(100))
                    {
                        System.out.println(address.getCanonicalHostName() + " machine is turned on and can be pinged");
                        connections+= address.getCanonicalHostName()+"\n";
                        System.out.println(address.getCanonicalHostName()+"\t"+address);

                        Log.e("ip",address+"");
                        arrayList.add(new Ip(address.getCanonicalHostName(),address.toString().substring(1)+""));
                    }
//                else if(!address.getHostAddress().equals(address.getHostName()))
//                {
//                    System.out.println(address + " machine is known in a DNS lookup");
//                }

                }
            }
            catch(UnknownHostException e1)
            {
                e1.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(arrayList.size()>0){
                String[] from = {"name", "image"};
                int[] to = {R.id.add, R.id.ip};
                IpAdapter ipAdapter = new IpAdapter(MainActivity.this,R.layout.list_item,arrayList);
                slv.setAdapter(ipAdapter);

                slv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                        TextView deviceView = (TextView)view.findViewById(R.id.add);
                        TextView ipView = (TextView) view.findViewById(R.id.ip);
                        final String device = deviceView.getText().toString();
                        final String ip = ipView.getText().toString();
                        Log.e("ips",ip);
                        final Intent chatIntent = new Intent(MainActivity.this,ChatActivity.class);
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Pick an encryption option");
                        builder.setItems(algo, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // the user clicked on colors[which]
                                chatIntent.putExtra("algo",algo[which]);
                                chatIntent.putExtra("device",device);
                                chatIntent.putExtra("ip",ip);
                                chatIntent.putExtra("myIp",myIp);
                                chatIntent.putExtra("mac",macAddress);
                                startActivity(chatIntent);
                            }
                        });
                        builder.show();



                    }
                });


            }
        }
    }



    private class ExternalIP extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... urls) {
            String ip = "Empty";

            try {
                HttpClient httpclient = new DefaultHttpClient();
                HttpGet httpget = new HttpGet("https://api.ipify.org/?format=json");
                HttpResponse response;

                response =httpclient.execute(httpget);

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    long len = entity.getContentLength();
                    if (len != -1 && len < 1024) {
                        String str = EntityUtils.toString(entity);
                        ip = str.replace("\n", "");
                    } else {
                        ip = "Response too long or error.";
                    }
                } else {
                    ip = "Null:" + response.getStatusLine().toString();
                }

            } catch (Exception e) {
                ip = "Error";
            }

            return ip;
        }

        protected void onPostExecute(String result) {

            // External IP
            Toast.makeText(getApplicationContext(),result,Toast.LENGTH_LONG).show();
            System.out.println(result);
        }
    }
}
