package com.example.lokesh.amul;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


class details implements Serializable {
    public byte[] key,msg;
    public String algo;
    public byte[] sign;
    public PublicKey pubKey;

    details(byte[] msg,byte[] key,String algo,byte[] sign,PublicKey pubKey){
        this.key=key;
        this.msg=msg;
        this.algo=algo;
        this.sign = sign;
        this.pubKey = pubKey;
    }
}

public class ChatActivity extends AppCompatActivity {
    public static String deviceName;
    public static String deviceIp;
    private static  PublicKey publicKey;
    private static PrivateKey privateKey;
    private static String myIp;
    EditText messageView;
    private String macAdress;
    private static byte[] algoKey = new byte[8];
    private  static  byte[] algoKeyAES=new byte[16];
    public static MessageAdapter messageAdapter;
    public static ListView messagesListView;
    public static String ALGO;
    private byte[] encryptedByte;
    private byte[] encryptedKey;
    Key publicKeys;
    static Key privateKeys;
    String message,decrypted;
    KeyPairGenerator kpg;
    KeyPair kp;
    Cipher cipher;
    static Cipher cipher1;
    byte[] myencryptedByte;
    byte[] enc_msg;
    static byte[] decryptedBytes;
    byte[] encryptedBytes;
    byte[] signature;
    private String encryptedMessage;


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Intent chatIntent =getIntent();
        deviceName = chatIntent.getExtras().getString("device");
        deviceIp = chatIntent.getExtras().getString("ip");
        macAdress = chatIntent.getExtras().getString("mac");
        ALGO = chatIntent.getExtras().getString("algo");
        myIp = chatIntent.getExtras().getString("myIp");

        KeyPairGenerator keyPairGenerator = null;
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        publicKey = keyPair.getPublic();
        privateKey= keyPair.getPrivate();
        try {
            signature = createSignature(myIp,privateKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        }

        messageView = (EditText) findViewById(R.id.editText);
        messageAdapter = new MessageAdapter(this);
        messagesListView = (ListView) findViewById(R.id.messages_view);
        messagesListView.setAdapter(messageAdapter);

        if(deviceName.isEmpty())
            deviceName = "Anonymous";
        Log.e("Chat",deviceIp+" "+deviceName);
        int i;
        for( i=0;i<8;i++){
            algoKey[i] = (byte)macAdress.charAt(i);
        }
        for( i=0;i<8;i++){
            algoKeyAES[i] = (byte)macAdress.charAt(i);
        }
        for(i=8;i<16;i++) {
            algoKeyAES[i] = algoKey[i - 8];
        }


        Log.e("AlgoKey mac DES",algoKey+"");
    }
    @SuppressLint("LongLogTag")
    public void sendMessage(View view) throws Exception {
        String message = messageView.getText().toString();
        encryptedByte = encrypt(message,ALGO);
        Log.e("Encrypted Byte",encryptedByte+"");
//        encryptedMessage=encryptedByte+"";
        //encryptedMessage = Base64.encodeToString(encryptedByte,Base64.DEFAULT);
        //encryptedKey = RSAEncrypt(Base64.encodeToString(algoKey,Base64.DEFAULT);d
        if(ALGO.equals("DES"))
            myencryptedByte = RSAEncrypt(algoKey);
        else
            myencryptedByte = RSAEncrypt(algoKeyAES);
        Log.e("Algo codeKey & RSAencKey",Base64.encodeToString(algoKey,Base64.DEFAULT)+" "+myencryptedByte);
        MemberData memberData = new MemberData(deviceName,getRandomColor());
        messageAdapter.add(new Message(message,deviceName,memberData,true));
        messagesListView.setSelection(messagesListView.getCount() - 1);
        //Toast.makeText(ChatActivity.this,"Message Sent to "+deviceIp,Toast.LENGTH_SHORT).show();
//        BackgroundTask b=new BackgroundTask(deviceIp,encryptedByte,myencryptedByte,ALGO)
// ;
       if(ALGO.equals("DES")) {
           BackgroundTask b = new BackgroundTask(deviceIp, encryptedByte, algoKey, ALGO,signature,publicKey);
           b.execute();
       }
        else {
            BackgroundTask b = new BackgroundTask(deviceIp, encryptedByte, algoKeyAES, ALGO,signature,publicKey);
            b.execute();
        }


    }
    public static byte[] createSignature(String message,PrivateKey key) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException, InvalidKeyException, SignatureException, SignatureException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");

        byte[] signBytes = messageDigest.digest(message.getBytes("UTF-8"));
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initSign(key);
        signature.update(signBytes);
        return signature.sign();

    }

    public static boolean verifySignature(String message,PublicKey key,byte[] sign) throws NoSuchAlgorithmException, UnsupportedEncodingException, NoSuchProviderException, InvalidKeyException, SignatureException, SignatureException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        byte[] signBytes = messageDigest.digest(message.getBytes("UTF-8"));
        Signature signature = Signature.getInstance("NONEwithRSA");
        signature.initVerify(key);
        signature.update(signBytes);
         return signature.verify(sign);

    }

    public static byte[] encrypt(String value,String algo) {
        byte[] encrypted = null;
        try {
            Key skeySpec;
            if(algo.equals("AES"))
            skeySpec = new SecretKeySpec(algoKeyAES, algo);
            else
                skeySpec = new SecretKeySpec(algoKey, algo);
            Cipher cipher = Cipher.getInstance(algo+"/CBC/PKCS5Padding");
            byte[] iv = new byte[cipher.getBlockSize()];

            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec,ivParams);
            encrypted  = cipher.doFinal(value.getBytes("UTF-8"));
            System.out.println("encrypted string:" + encrypted.length);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return encrypted;
    }

    public static byte[]  decrypt(byte[] encrypted,byte[] algoKey,String algo) {
        byte[] original = null;
        Cipher cipher = null;
        try {

            Key key = new SecretKeySpec(algoKey, algo);
            cipher = Cipher.getInstance(algo+"/CBC/PKCS5Padding");
            //the block size (in bytes), or 0 if the underlying algorithm is not a block cipher
            byte[] ivByte = new byte[cipher.getBlockSize()];
            //This class specifies an initialization vector (IV). Examples which use
            //IVs are ciphers in feedback mode, e.g., DES in CBC mode and RSA ciphers with OAEP encoding operation.
            IvParameterSpec ivParamsSpec = new IvParameterSpec(ivByte);
            cipher.init(Cipher.DECRYPT_MODE, key, ivParamsSpec);
            original= cipher.doFinal(encrypted);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return original;
    }
//    public static byte[] RSADecrypt(byte[] encrypted) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
//        Cipher c= Cipher.getInstance("RSA");
//        c.init(Cipher.DECRYPT_MODE,privateKey);
//        byte[] decryptedBytes =c.doFinal(encrypted);
//        return decryptedBytes;
//
//    }
//    public static byte[] RSAEncrypt(String message) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
//        keyPairGenerator.initialize(2048);
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//        publicKey = keyPair.getPublic();
//        privateKey= keyPair.getPrivate();
//        Cipher c = Cipher.getInstance("RSA");
//        c.init(Cipher.ENCRYPT_MODE,publicKey);
//        byte[] encryptedByte = c.doFinal(message.getBytes("UTF-8"));
//        Log.e("Size",encryptedByte.length+"");
//        return encryptedByte;
//
//    }

    public byte[] RSAEncrypt(byte[] algoKey) throws Exception
    {
        kpg= KeyPairGenerator.getInstance("RSA");//generating a key pair
        kpg.initialize(2048); //keysize
        kp = kpg.genKeyPair(); //key pair

        publicKeys=kp.getPublic();
        privateKeys=kp.getPrivate();

        cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE,publicKeys);

        //String plain;

        encryptedBytes= cipher.doFinal(algoKey);
        return encryptedBytes;
    }

    public static byte[] RSADecrypt(final byte[] encryptedBytes) throws Exception
    {

        cipher1 = Cipher.getInstance("RSA");
        cipher1.init(Cipher.DECRYPT_MODE, privateKeys);
        decryptedBytes = cipher1.doFinal(encryptedBytes);
        //decrypted = new String(decryptedBytes);

        return decryptedBytes;
    }

    class BackgroundTask extends AsyncTask<String,Void,String>
    {

        Socket s;
        DataOutputStream dos;
        String ip,msg;
        byte[] encMessage;
        byte[] encKey;
        String algo;
        Context mContext=getApplicationContext();
        details detail;
        byte[] sign;
        PublicKey pubKey;

        public BackgroundTask(String deviceIp,byte[] encMessage,byte[] encKey,
                              String algo,byte[] sign,PublicKey pubKey) {
            super();
            ip = deviceIp;
            this.encMessage = encMessage;
            this.encKey = encKey;
            this.algo = algo;
            this.sign = sign;
            this.pubKey=pubKey;
            detail=new details(this.encMessage,this.encKey,this.algo,this.sign,this.pubKey);
        }

        @Override
        protected String doInBackground(String... strings) {

            Log.e("backg",ip);


            try{
                s=new Socket(ip,9700);
                Log.e("Algo send msg key",encMessage+" "+encKey+" "+algo);
                ObjectOutputStream os=new ObjectOutputStream(s.getOutputStream());
                os.writeObject(detail);
                //Toast.makeText(ChatActivity.this,detail.key+"",Toast.LENGTH_SHORT).show();
//                dos=new DataOutputStream(s.getOutputStream());
////                dos.writeUTF(Base64.encodeToString(encMessage,Base64.NO_PADDING));
////                dos.writeUTF(Base64.encodeToString(encKey,Base64.NO_PADDING));
////                dos.writeUTF(algo);
//                dos.writeInt(encMessage.length);
//                dos.write(encMessage);
//                dos.writeInt(encKey.length);
//                dos.write(encKey);
//                dos.writeUTF(algo);
                Log.e("Algo final send",detail.key+"");
//                Log.e("Algo out codedM codedK",Base64.encodeToString(encMessage,Base64.NO_PADDING)+" "+
//                        Base64.encodeToString(encKey,Base64.NO_PADDING)+" "+algo);
                s.close();

            }catch (IOException e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

        }


    }

    public static  String getRandomName() {
        String[] adjs = {"autumn", "hidden", "bitter", "misty", "silent", "empty", "dry", "dark", "summer", "icy", "delicate", "quiet", "white", "cool", "spring", "winter", "patient", "twilight", "dawn", "crimson", "wispy", "weathered", "blue", "billowing", "broken", "cold", "damp", "falling", "frosty", "green", "long", "late", "lingering", "bold", "little", "morning", "muddy", "old", "red", "rough", "still", "small", "sparkling", "throbbing", "shy", "wandering", "withered", "wild", "black", "young", "holy", "solitary", "fragrant", "aged", "snowy", "proud", "floral", "restless", "divine", "polished", "ancient", "purple", "lively", "nameless"};
        String[] nouns = {"waterfall", "river", "breeze", "moon", "rain", "wind", "sea", "morning", "snow", "lake", "sunset", "pine", "shadow", "leaf", "dawn", "glitter", "forest", "hill", "cloud", "meadow", "sun", "glade", "bird", "brook", "butterfly", "bush", "dew", "dust", "field", "fire", "flower", "firefly", "feather", "grass", "haze", "mountain", "night", "pond", "darkness", "snowflake", "silence", "sound", "sky", "shape", "surf", "thunder", "violet", "water", "wildflower", "wave", "water", "resonance", "sun", "wood", "dream", "cherry", "tree", "fog", "frost", "voice", "paper", "frog", "smoke", "star"};
        return (
                adjs[(int) Math.floor(Math.random() * adjs.length)] +
                        "_" +
                        nouns[(int) Math.floor(Math.random() * nouns.length)]
        );
    }

    public static  String getRandomColor() {
        Random r = new Random();
        StringBuffer sb = new StringBuffer("#");
        while(sb.length() < 7){
            sb.append(Integer.toHexString(r.nextInt()));
        }
        return sb.toString().substring(0, 7);
    }

    /**
     * Created by lokesh on 26/10/18.
     */
    public static class MyServer implements Runnable {

        ServerSocket ss;
        Socket mysocket;
        Context mContext;
        DataInputStream dis;
        byte[] msg;
        byte[] key = new byte[256];
        String algo;
        private byte[] decKey;
        private byte[] sign;
        private byte[] decMsgBytes;
        private String  decMsg;
        details detail = null;
        PublicKey pubKey;

        android.os.Handler handler=new android.os.Handler();

        MyServer(Context context)
        {
            mContext=context;
        }
        @Override
        public void run() {


            try {
                ss = new ServerSocket(9700);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "waiting for client", Toast.LENGTH_SHORT).show();
                    }
                });
                do {

                    mysocket = ss.accept();

                    ObjectInputStream oi = new ObjectInputStream(mysocket.getInputStream());
                    try {
                        detail = (details) oi.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

//                    dis = new DataInputStream(mysocket.getInputStream());
//                    int length=dis.readInt();
//                    if(length>0){
//                        msg=new byte[length];
//                        dis.readFully(msg,0,msg.length);
//                    }
//                    length=dis.readInt();
//                    if(length>0){
//                        key=new byte[length];
//                        dis.readFully(key,0,key.length);
//                        Log.e("Algo recieved final",decKey+" "+decKey.length);
//                    }
//
////                    String msgs =  dis.readUTF();
////                    String keys = dis.readUTF();
//                    algo = dis.readUTF();

                    msg = detail.msg;
                    key = detail.key;
                    algo = detail.algo;
                    boolean flag = false;
                    try {
                        flag= verifySignature(deviceIp,detail.pubKey,detail.sign);

                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    } catch (NoSuchProviderException e) {
                        e.printStackTrace();
                    } catch (InvalidKeyException e) {
                        e.printStackTrace();
                    } catch (SignatureException e) {
                        e.printStackTrace();
                    }


                    // Toast.makeText(mContext,detail.key+"",Toast.LENGTH_SHORT).show();
                    Log.e("Rec Key",detail.key+"");

//                    Log.e("AlgoR codem codek",msgs+"   "+keys+" "+algo);
//                    msg = Base64.decode(msgs.getBytes(),Base64.NO_PADDING);
//                    key = Base64.decode(keys.getBytes(),Base64.NO_PADDING);
//                    Log.e("Algo recieved dec",msg+" "+key+" "+algo);
                    try {
//                      decKey = RSADecrypt(key);
                        decKey = key;
                        Log.e("Algo  keyD", decKey + "");
//                    } catch (NoSuchPaddingException e) {
//                        e.printStackTrace();
//                    } catch (NoSuchAlgorithmException e) {
//                        e.printStackTrace();
//                    } catch (InvalidKeyException e) {
//                        e.printStackTrace();
//                    } catch (BadPaddingException e) {
//                        e.printStackTrace();
//                    } catch (IllegalBlockSizeException e) {
//                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    decMsgBytes = decrypt(msg, decKey, algo);
                    decMsg = new String(decMsgBytes, "UTF-8");
                    Log.e("Algo dec msg", decMsg);


                    final boolean finalFlag = flag;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "message Recieved from client " + decMsg, Toast.LENGTH_LONG).show();

                            MemberData memberData = new MemberData(deviceName, getRandomColor());
                            if(finalFlag){
                            messageAdapter.add(new Message(decMsg, deviceName, memberData, false));
                            messagesListView.setSelection(messagesListView.getCount() - 1);
                            }
                            else{
                                Toast.makeText(mContext, "Hacked ", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }while (true);


            }catch (IOException e){
                e.printStackTrace();
            }

        }
    }

}

