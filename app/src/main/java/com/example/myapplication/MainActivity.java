package com.example.myapplication;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;

public class MainActivity extends AppCompatActivity {
    private final ReentrantLock lock = new ReentrantLock();

    ZContext context = new ZContext();
    ZMQ.Socket socket = context.createSocket(ZMQ.REP);
    String human_name;
    Mat change_mat;
    String name;
    Mat[] receive_mat = new Mat[6];
    Mat cap;

    int connect_flag=0;
    boolean sleep_flag = false;
    String command ="send_picture";
    public String[] permissions = new String[]{
            Manifest.permission.INTERNET ,
            Manifest.permission.CHANGE_NETWORK_STATE ,
            Manifest.permission.ACCESS_NETWORK_STATE   ,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE   ,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET,
//           Manifest.permission.READ_CONTACTS,  //我们不需要联系人
//            Manifest.permission.MODIFY_AUDIO_SETTINGS,
//            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA};
    public void send_msg(ZMQ.Socket socket,String str){
        socket.send(str.getBytes(ZMQ.CHARSET),0);
    };
    public String recv_msg(ZMQ.Socket socket){
        byte[] received = socket.recv(0);
        String str = new String(received,ZMQ.CHARSET);
        return str;
    };
    public void send_pic(ZMQ.Socket socket, Mat img){
//        int nFlag = img.channels() * 8;//一个像素的bits
//        int nHeight = img.height();
//        int nWidth = img.width();
//        int nBytes = nHeight * nWidth * nFlag / 8;//图像总的字节
          byte[] a =mat2Byte(img,".jpg");
          socket.send(a);
    };
    public Mat receive_pic(ZMQ.Socket socket){
        byte[] received = socket.recv(0);
        Mat mat = Imgcodecs.imdecode(new MatOfByte(received), Imgcodecs.IMREAD_COLOR);
//        Imgcodecs.imwrite(failName, mat);
        return mat;
    }
    public void start_pause(){
        FloatingActionButton fab = findViewById(R.id.fab);
        if(sleep_flag){
            command = "send_picture";
            fab.setImageResource(android.R.drawable.ic_media_pause);
        }else {
            command = "none";
            fab.setImageResource(android.R.drawable.ic_media_play);
        }
        sleep_flag=!sleep_flag;//取反
    }

//    @Override
//    public void onResume()
//    {
//        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.i("cv", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//        } else {
//            Log.i("cv", "OpenCV library found inside package. Using it!");
//        }
//    }
    public void stand() throws InterruptedException {
        Log.i("zmq"," void stand in");
//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        while (true) {
//            if (sleep_flag) {
//                Thread.sleep(100);
//                continue;
//            }
            send_msg(socket, command);
            if (command.equals("send_picture")) {
                //下位机发图，上位机收图
                //收人脸数
                String tmp = recv_msg(socket);
                int face_num = Integer.valueOf(tmp).intValue();
                send_msg(socket, "received_face_num");
                //人脸名字
                name  = recv_msg(socket);
                send_msg(socket, "received_face_name");
                //收图片
                cap = receive_pic(socket);
                cvtColor(cap,cap,COLOR_BGR2RGB);
                send_msg(socket, "reveice_cap_picture");

                lock.lock();
                try {
                    for (int i = 0; i < face_num; i++) {
                        if(i<6){receive_mat[i]=receive_pic(socket);cvtColor(receive_mat[i],receive_mat[i],COLOR_BGR2RGB);}
                        send_msg(socket, "reveice_picture_i");
                    }
                } finally {
                    lock.unlock();
                }

                socket.recv(0);
                onSuccess2(1,1);
            } else if (command.equals( "none")) {
                socket.recv(0);
            } else if (command.equals( "start_traning")) {
                //收
                socket.recv(0);

                //只执行一次命令 自动切换
                // command = "none";
//                start_pause();
                command="send_picture";

            } else if (command.equals("change_train_set")) {
                socket.recv(0);
                //发人名
                //                human_name = "hhh";
                send_msg(socket, human_name);
                //收
                socket.recv(0);

                //发送图片
                send_pic(socket, change_mat);
                //收
                socket.recv(0);

                //防止重复发送 执行完change_train_set 下一个命令自己切换
                // command = "none";
//                start_pause();
                command="send_picture";

            } else {
                Log.i("zmq","GGGGGGGGGGGGGGGGGGGGGGGGGGGGG");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(MainActivity.this, permissions,1);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.loadLibrary("opencv_java4");

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(connect_flag==0){
                    connect_flag=1;//flag 提前 防止二次点击 zmq崩溃
                    socket.bind("tcp://*:5555");
                    byte[] request = socket.recv(0);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("zmq ", "zmq thread: start /*/////////*/");
                            //                        onSuccess2(0,0);
                            try {
                                stand();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    Snackbar.make(view, "connect success!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    command = "send_picture";
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                    sleep_flag=false;
                }else if(connect_flag==1){
                    start_pause();
                }
            }
        });

        ImageView face0 = (ImageView) findViewById(R.id.face0);
        face0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit0 =(EditText)findViewById(R.id.edit0);
                human_name=edit0.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                         change_mat=receive_mat[0].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
        ImageView face1 = (ImageView) findViewById(R.id.face1);
        face1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit1 =(EditText)findViewById(R.id.edit1);
                human_name=edit1.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                        change_mat=receive_mat[1].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
        ImageView face2 = (ImageView) findViewById(R.id.face2);
        face2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit2 =(EditText)findViewById(R.id.edit2);
                human_name=edit2.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                        change_mat=receive_mat[2].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
        ImageView face3 = (ImageView) findViewById(R.id.face3);
        face3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit3 =(EditText)findViewById(R.id.edit3);
                human_name=edit3.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                        change_mat=receive_mat[3].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
        ImageView face4 = (ImageView) findViewById(R.id.face4);
        face4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit4 =(EditText)findViewById(R.id.edit4);
                human_name=edit4.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                        change_mat=receive_mat[4].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
        ImageView face5 = (ImageView) findViewById(R.id.face5);
        face5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*****修改 human_name 和 change_mat ****/
                command="none";
                EditText edit5 =(EditText)findViewById(R.id.edit5);
                human_name=edit5.getText().toString();
                if(human_name!=null){
                    lock.lock();
                    try {
                        change_mat=receive_mat[5].clone();
                    } finally {
                        lock.unlock();
                    }
                    command="change_train_set";
                }else{
                    command="send_picture";
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.Item_trainning) {
            command="start_traning";
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Mat 转 bitmap
    public static Bitmap matToBitmap(Mat mat) {
        Bitmap resultBitmap = null;
        if (mat != null) {
            resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            if (resultBitmap != null)
                Utils.matToBitmap(mat, resultBitmap);
        }
        return resultBitmap;
    }

    //Bitmap转Mat
    public static Mat bitmapToMat(Bitmap bm) {
        Bitmap bmp32 = bm.copy(Bitmap.Config.RGB_565, true);
        Mat imgMat = new Mat ( bm.getHeight(), bm.getWidth(), CvType.CV_8UC2, new Scalar(0));
        Utils.bitmapToMat(bmp32, imgMat);
        return imgMat;
    }
    //Byte转Bitmap
    public Bitmap ByteArray2Bitmap(byte[] data, int width, int height) {
        int Size = width * height;
        int[] rgba = new int[Size];

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                rgba[i * width + j] = 0xff000000;
            }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0 , width, 0, 0, width, height);
        return bmp;
    }
    //Bitmap转byte
    public static byte[] bitmapToByteArray(Bitmap image) {
        //calculate how many bytes the image consists of.
        int bytes = image.getByteCount();

        ByteBuffer buffer = ByteBuffer.allocate(bytes); //Create a new buffer
        image.copyPixelsToBuffer(buffer); //Move the byte data to the buffer
        return buffer.array(); //Get the underlying array containing the data.
    }

    public static Uri saveBitmap(Bitmap bm, String picName) {
        try {
            String dir= Environment.getExternalStorageDirectory().getAbsolutePath()+"/renji/"+picName+".jpg";
            File f = new File(dir);
            if (!f.exists()) {
                f.getParentFile().mkdirs();
                f.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();
            Uri uri = Uri.fromFile(f);
            return uri;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();    }
        return null;
    }
    public void save_bitmap(Bitmap bitmap,String save_name){
        FileOutputStream fos;
        try {
            // 判断手机设备是否有SD卡
            boolean isHasSDCard = Environment.getExternalStorageState().equals(
                    android.os.Environment.MEDIA_MOUNTED);
            if (isHasSDCard) {
                // SD卡根目录
                File sdRoot = Environment.getExternalStorageDirectory();
                File file = new File(sdRoot, save_name);//  "test.jpg"
                fos = new FileOutputStream(file);
            } else
                throw new Exception("创建文件失败!");

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);

            fos.flush();
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Bitmap read_bitmap(String save_name){
        Bitmap bit;
        bit = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/"+save_name);
        return bit;
    }

    public void onSuccess(int i, String json) {
        Log.i("Channel", "onSuccess");
        Message message = Message.obtain();
        message.what = i;
        Bundle bundle = new Bundle();
        bundle.putString("json", json);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
    public void onSuccess2(int i, int json) {
        Log.i("Channel", "onSuccess");
        Message message = Message.obtain();
        message.what = i;
        Bundle bundle = new Bundle();
        bundle.putInt("json", json);
        message.setData(bundle);
        mHandler.sendMessage(message);
    }
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    break;
//                case 0:
//                    TextView tv_yes_id = (TextView)findViewById(R.id.tv_yes_id);
//                    //完成主界面更新,拿到数据
//                    Bundle bundle0 = msg.getData();
//                    int data0 = bundle0.getInt("json");
//                    tv_yes_id.setText(data0+"");
//                    break;
//                case 1:
//                    TextView tv_yes_list = (TextView)findViewById(R.id.tv_yes_list);
//                    //完成主界面更新,拿到数据
//                    Bundle bundle1 = msg.getData();
//                    String data1 = bundle1.getString("json");
//                    tv_yes_list.setText(data1);
//                    break;
                //人脸识别 UI 更新
                case 1:
                    Log.i("UI","handler UI case 1");
                    Bitmap bit_cap=matToBitmap(cap);
                    ImageView cap0=findViewById(R.id.cap0);
                    cap0.setImageBitmap(bit_cap);
                    Bitmap face_0=matToBitmap(receive_mat[0]);
                    ImageView face0=findViewById(R.id.face0);
                    face0.setImageBitmap(face_0);
                    Bitmap face_1=matToBitmap(receive_mat[1]);
                    ImageView face1=findViewById(R.id.face1);
                    face1.setImageBitmap(face_1);
                    Bitmap face_2=matToBitmap(receive_mat[2]);
                    ImageView face2=findViewById(R.id.face2);
                    face2.setImageBitmap(face_2);
                    Bitmap face_3=matToBitmap(receive_mat[3]);
                    ImageView face3=findViewById(R.id.face3);
                    face3.setImageBitmap(face_3);
                    Bitmap face_4=matToBitmap(receive_mat[4]);
                    ImageView face4=findViewById(R.id.face4);
                    face4.setImageBitmap(face_4);
                    Bitmap face_5=matToBitmap(receive_mat[5]);
                    ImageView face5=findViewById(R.id.face5);
                    face5.setImageBitmap(face_5);

                    TextView name0 = (TextView)findViewById(R.id.name0);
                    TextView name1 = (TextView)findViewById(R.id.name1);
                    TextView name2 = (TextView)findViewById(R.id.name2);
                    TextView name3 = (TextView)findViewById(R.id.name3);
                    TextView name4 = (TextView)findViewById(R.id.name4);
                    TextView name5 = (TextView)findViewById(R.id.name5);

//                    name = "dadada/dadada/dadadaddddd/dddddddddddddddf/fffffff/";
                    int cnt = 0 ;
                    String[] name_label = new String[10];
                    String tmp="";
                    for (int i = 0; i < name.length(); i++) {
                        if (name.charAt(i) != '/') {

                            tmp+=name.charAt(i);
                        } else {
                            name_label[cnt++]=tmp;
                            tmp="";
                        }
                    }
                    name0.setText(name_label[0]);
                    name1.setText(name_label[1]);
                    name2.setText(name_label[2]);
                    name3.setText(name_label[3]);
                    name4.setText(name_label[4]);
                    name5.setText(name_label[5]);

                    break;
            }
        }
    };

    public static byte[] mat2Byte(Mat matrix, String fileExtension) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(fileExtension, matrix, mob);
        byte[] byteArray = mob.toArray();
        return byteArray;
    }/*
byte[] byteString = mat2Byte(mat, ".jpg");

Mat mat = Imgcodecs.imdecode(new MatOfByte(byteString), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED)；

Imgcodecs.imwrite(failName, mat);
    */

}
