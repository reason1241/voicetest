package com.naver.naverspeech.client;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.naver.naverspeech.client.utils.AudioWriterPCM;
import com.naver.speech.clientapi.SpeechRecognitionResult;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import static android.speech.tts.TextToSpeech.ERROR;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();
    // 1. "내 애플리케이션"에서 Client ID를 확인해서 이곳에 적어주세요.
    // 2. build.gradle (Module:app)에서 패키지명을 실제 개발자센터 애플리케이션 설정의 '안드로이드 앱 패키지 이름'으로 바꿔 주세요

    private RecognitionHandler handler;
    private NaverRecognizer naverRecognizer;

    //말풍선들
    private TextView txtDest;
    private TextView txtSource;
    private TextView txtResult;
    private TextView txtDoor1;
    private TextView txtDoor2;

    //문은 2호관과 하이테크만 만들었음
    private static final String[] building = {"Error","본관 1호관","2호관","60주년기념관","4호관","5호관","6호관","7호관", "하이테크센터", "9호관", "학생회관"};//error는 글자를 못찾았을때
    private static final String[][] door = {{},{},{"Error","1문", "2문", "3문","4문"},{},{},{},{},{}, {"Error","1문", "2문","3문"},{},{}};
    private static final int[] doornum = {999,9,5,9,9,9,9,9,4,9,9};
    //tts
    private TextToSpeech myTTS;

    private int buildSource = 2;
    private int sourceDoorNum;
    private int buildDest;
    private int destDoorNum;

    private int proc;

    private Button btnStart;
    private String mResult;

    private AudioWriterPCM writer;

    // Handle speech recognition Messages.
    private void handleMessage(Message msg) {
        switch (msg.what) {
            case R.id.clientReady:
                // Now an user can speak.
                txtResult.setText("Connected");
                writer = new AudioWriterPCM(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/NaverSpeechTest");
                writer.open("Test");
                break;

            case R.id.audioRecording:
                writer.write((short[]) msg.obj);
                break;

            case R.id.partialResult:
                // Extract obj property typed with String.
                mResult = (String) (msg.obj);
                txtResult.setText(mResult);
                break;

            case R.id.finalResult:
                // Extract obj property typed with String array.
                // The first element is recognition result for speech.
            	SpeechRecognitionResult speechRecognitionResult = (SpeechRecognitionResult) msg.obj;
            	List<String> results = speechRecognitionResult.getResults();
            	StringBuilder strBuf = new StringBuilder();

            	for(String result : results) {
            		strBuf.append(result);
            		strBuf.append("\n");
            	}

                if(proc==0){
            	    proc=1;
            	    Intent intent = new Intent(this, DoorActivity.class);
                    int tok = cutTalk(strBuf.toString(), 0, 0);
                    txtSource.setText(building[tok]);
                    buildSource = tok;
                    intent.putExtra("buildNum", buildSource);
                    startActivityForResult(intent,1);
                    doorAsk();
                }
                else if(proc==1){
                    proc=2;
                    Intent intent = new Intent(this, DoorActivity.class);
                    int tok = cutTalk(strBuf.toString(), 0, 0);
                    txtDest.setText(building[tok]);
                    buildDest = tok;
                    intent.putExtra("buildNum", buildDest);
                    startActivityForResult(intent,1);
                    doorAsk();
                }
                break;

            case R.id.recognitionError:
                if (writer != null) {
                    writer.close();
                }

                mResult = "Error code : " + msg.obj.toString();
                txtResult.setText(mResult);
                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;

            case R.id.clientInactive:
                if (writer != null) {
                    writer.close();
                }

                btnStart.setText(R.string.str_start);
                btnStart.setEnabled(true);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==1){
            if(resultCode==RESULT_OK){
                if(proc==1) {
                    sourceDoorNum = data.getIntExtra("doorNum",0);
                    txtDoor1.setText(door[buildSource][sourceDoorNum]);
                    destAsk();
                }
                else if(proc==2){
                    destDoorNum = data.getIntExtra("doorNum",1);
                    txtDoor2.setText(door[buildDest][destDoorNum]);
                    finAsk();
                }
            }
            else if(resultCode == RESULT_CANCELED)
            {
                txtDoor1.setText("Error");
                txtDoor2.setText("Error");
            }
        }


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        proc = 0;

        txtResult = (TextView) findViewById(R.id.txt_result);
        btnStart = (Button) findViewById(R.id.btn_start);
        txtSource = (TextView) findViewById(R.id.txt_source);
        txtDest = (TextView) findViewById(R.id.txt_dest);
        txtDoor1 = (TextView) findViewById(R.id.txt_door1);
        txtDoor2 = (TextView) findViewById(R.id.txt_door2);


        handler = new RecognitionHandler(this);
        naverRecognizer = new NaverRecognizer(this, handler, APIKey.NAVER_CLIENT_ID);

        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    myTTS.setLanguage(Locale.KOREAN);
                }
                sourceAsk();
            }
        });

        sourceAsk();

        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(!naverRecognizer.getSpeechRecognizer().isRunning()) {
                    // Start button is pushed when SpeechRecognizer's state is inactive.
                    // Run SpeechRecongizer by calling recognize().
                    mResult = "";
                    txtResult.setText("Connecting...");
                    btnStart.setText(R.string.str_stop);
                    naverRecognizer.recognize();
                } else {
                    Log.d(TAG, "stop and wait Final Result");
                    btnStart.setEnabled(false);

                    naverRecognizer.getSpeechRecognizer().stop();
                }
            }
        });
    }

    @Override
    protected void onStart() {
    	super.onStart();
    	// NOTE : initialize() must be called on start time.
    	naverRecognizer.getSpeechRecognizer().initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mResult = "";
        txtResult.setText("");
        btnStart.setText(R.string.str_start);
        btnStart.setEnabled(true);
    }

    protected void onDestroy(){
        super.onDestroy();

        if(myTTS !=null){
            myTTS.stop();
            myTTS.shutdown();
            myTTS = null;
        }
    }
    @Override
    protected void onStop() {
    	super.onStop();
    	// NOTE : release() must be called on stop time.
    	naverRecognizer.getSpeechRecognizer().release();
    }


    // Declare handler for handling SpeechRecognizer thread's Messages.
    static class RecognitionHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        RecognitionHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                activity.handleMessage(msg);
            }
        }
    }

    public int cutTalk(String s, int mode, int build){//mode 0은 building, mode 1은 sourcedoor
        String[] sentence = new String[7];
        StringTokenizer st = new StringTokenizer(s,"\n");
        int a=0;
        for(; st.hasMoreTokens(); a++){
            sentence[a] = st.nextToken();
        }

        int[] result = new int[7];
        if(mode==0) {
            for (int d = 0; d < a; d++) {
                result[d] = findBuilding(sentence[d]);
            }
        } else{
            for(int d=0;d<a;d++){
                result[d] = findDoor(sentence[d]);
            }
        }
        int finresult = 0;
        for(int d=0;d<a;d++){
            if(finresult==0&&result[d]!=0){
                finresult=result[d];
            }
        }

        return finresult;
    }

    public int findBuilding(String s){
        int a = buildingCheck(s);
        return a;
    }

    public int findDoor(String s){
        int a = doorCheck(s);
        return a;
    }
    public int buildingCheck(String s){
        if(s.contains("1")|s.contains("본")|s.contains("일")) return 1;
        else if(s.contains("하")|s.contains("테")|s.contains("텍")|s.contains("택")|s.contains("핫")|s.contains("합")|s.contains("팩")) return 8;
        else if(s.contains("2")|s.contains("이")|s.contains("유")|s.contains("요")) return 2;
        else if(s.contains("주")|s.contains("년")) return 3;
        else if(s.contains("4")|s.contains("사")) return 4;
        else if(s.contains("5")|s.contains("오")) return 5;
        else if(s.contains("6")|s.contains("육")) return 6;
        else if(s.contains("7")|s.contains("칠")) return 7;
        else if(s.contains("9")|s.contains("구")) return 9;
        else if(s.contains("학")|s.contains("비")|s.contains("관")) return 10;
        else return 0;
    }

    public int doorCheck(String s){
        if(s.contains("1")|s.contains("일")) return 1;
        else if(s.contains("2")|s.contains("이")) return 2;
        else if(s.contains("3")|s.contains("삼")) return 3;
        else if(s.contains("4")|s.contains("사")) return 4;
        else if(s.contains("5")|s.contains("오")) return 5;
        else if(s.contains("6")|s.contains("육")) return 6;
        else if(s.contains("7")|s.contains("칠")) return 7;
        else return 0;
    }

    public void doorAsk(){
        String askDoor1 = "가까이에 있는 문을 버튼으로 선택해 주십시오.";
        myTTS.speak(askDoor1, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void destAsk(){
        String askDoor1 = "버튼을 선택 후 목적지를 말씀해 주십시오.";
        myTTS.speak(askDoor1, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void sourceAsk(){
        String askDoor1 = "버튼을 선택 후 출발지를 말씀해 주십시오.";
        myTTS.speak(askDoor1, TextToSpeech.QUEUE_FLUSH, null);
    }
    public void finAsk(){
        String askDoor1 = "완료되었습니다.";
        myTTS.speak(askDoor1, TextToSpeech.QUEUE_FLUSH, null);
    }
}
