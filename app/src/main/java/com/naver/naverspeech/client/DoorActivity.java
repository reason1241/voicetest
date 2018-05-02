package com.naver.naverspeech.client;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import static android.speech.tts.TextToSpeech.ERROR;

/**
 * Created by choi on 2018-04-09.
 */


public class DoorActivity extends Activity {

    int[] doorNumber = {999,9,5,9,9,9,9,9,4,9,9};

    private TextToSpeech myTTS;

    Intent intent;
    private Button btnCancel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_door);

        btnCancel = (Button)findViewById(R.id.btn_cancel);
        intent = getIntent();

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        });


        int buildNum = (int)intent.getIntExtra("buildNum",0);
        int door = doorNumber[buildNum];
        myTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != ERROR) {
                    myTTS.setLanguage(Locale.KOREAN);
                }
            }
        });

        doorAsk();

        final LinearLayout lm = (LinearLayout) findViewById(R.id.ll);
        // linearLayout params 정의
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        for (int j = 1; j < door; j++) {
            // LinearLayout 생성
            LinearLayout ll = new LinearLayout(this);
            ll.setOrientation(LinearLayout.HORIZONTAL);

            // TextView 생성
            TextView tvProdc = new TextView(this);
            tvProdc.setText("문" + j + " ");
            ll.addView(tvProdc);

            // TextView 생성
            TextView tvAge = new TextView(this);
            tvAge.setText("   특징" + j + "  ");
            ll.addView(tvAge);

            // 버튼 생성

            final Button btn = new Button(this);

            // setId 버튼에 대한 키값
            btn.setId(j);
            btn.setText("선택");
            btn.setLayoutParams(params);

            final int position = j;

            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int temp = position;
                    Intent returnIntent = new Intent();
                    Log.d("log", "position :" + temp);
                    Toast.makeText(getApplicationContext(), "클릭한 문:" + temp, Toast.LENGTH_LONG).show();
                    returnIntent.putExtra("doorNum", temp);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            });

            //버튼 add
            ll.addView(btn);
            //LinearLayout 정의된거 add
            lm.addView(ll);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void doorAsk(){
        String askDoor1 = "가까이에 있는 문을 버튼으로 선택해 주십시오.";
        myTTS.speak(askDoor1, TextToSpeech.QUEUE_FLUSH, null);
    }

}
