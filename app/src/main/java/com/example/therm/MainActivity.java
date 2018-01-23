package com.example.therm;

import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.channels.SeekableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static com.example.therm.R.id.intervalSeekBar;
import static com.example.therm.R.id.nowTime;

public class MainActivity  extends AppCompatActivity {
    public Timer mTimer;
    public Handler mHandler;

    // 正時基準かどうかのチェックボックス
    public CheckBox referencedHour;
    // 鳴動間隔変更用シークバー
    public SeekBar intervalSeek;
    // 鳴動間隔表示テキストボックス
    public TextView intervalNum;
    // 正時のときの鳴動間隔
    public final int intervalList[] = {2, 3, 4, 5, 6, 10, 12, 15, 20, 30};
    // 最長鳴動間隔
    public final int intervalMax[]={60,intervalList[intervalList.length-1]};
    public final int intervalMin[]={2,intervalList[0]};

    // 音関係の変数
    public SoundPool SoundsPool;
    public final  String soundName[]={"hour","min15","min1"};
    public int soundId[]=new int[soundName.length];

    // 時刻表示のフォーマット
    final SimpleDateFormat mSimpleDataFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        // 鳴動間隔選択用NumberPicker初期化処理
        referencedHour=findViewById(R.id.ReferencedToTheHour);
        intervalSeek=findViewById(R.id.intervalSeekBar);
        intervalNum=findViewById(R.id.intervalNumber);
        intervalSwitchChange(-1);

        // 正時基準かどうかの変更
        referencedHour.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                intervalSwitchChange(b?1:0);
            }
        });

        intervalSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                intervalSwitchChange(-1);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        findViewById(R.id.intervalDecrease).setOnClickListener(intervalButton);
        findViewById(R.id.intervalIncrease).setOnClickListener(intervalButton);

        // 音読み込み
         if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

             //ロリポップより前のバージョンに対応するコード
             SoundsPool = new SoundPool(soundName.length, AudioManager.STREAM_ALARM, 0);
/*
             for (int i=0;i<soundName.length;i++) {
                 soundId[i] = SoundsPool.load(getBaseContext(), getResources().getIdentifier(soundName[i], "raw", getPackageName()), 1);
             }
             */
         } else {
             AudioAttributes attr = new AudioAttributes.Builder()
                     .setUsage(AudioAttributes.USAGE_MEDIA)
                     .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                     .build();

             SoundsPool = new SoundPool.Builder()
                     .setAudioAttributes(attr)
                     .setMaxStreams(soundName.length)
                     .build();
         }
         SoundsPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
             @Override
             public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                 Log.d("debug","sampleId="+sampleId);
                 Log.d("debug","status="+status);
             }
         });
         //あらかじめ音をロードする必要がある　※直前にロードしても間に合わないので早めに
         for (int i=0;i<soundName.length;i++) {
             soundId[i] = SoundsPool.load(getBaseContext(),
                     getResources().getIdentifier(soundName[i], "raw", getPackageName()),
                     1);
         }

         //現在時刻鳴動
         Button nowTimeButton=findViewById(R.id.nowTimeButton);
         nowTimeButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View view) {
                 soundTime();
              }
         });

        // 現在時刻表示
        mHandler = new Handler(getMainLooper());
        mTimer = new Timer();        // 一秒ごとに定期的に実行します。

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(new Runnable() {
                    public void run() {
                        Calendar calendar = Calendar.getInstance();
                        String nowDate = mSimpleDataFormat.format(calendar.getTime());
                        // 時刻表示をするTextView
                        ((TextView) findViewById(nowTime)).setText(nowDate);
                    }
                });}
        },0,1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 定期実行をcancelする
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
    private View.OnClickListener intervalButton = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            int progress=intervalSeek.getProgress();

            switch (view.getId()) {
                case R.id.intervalDecrease:
                    progress--;
                    break;
                case R.id.intervalIncrease:
                    progress++;
                    break;
            }
            intervalSeek.setProgress(progress);
            intervalSwitchChange(-1);
        }
    };

    public void intervalSwitchChange(int b) {
        // 鳴動間隔のチェック状態を取得します
        int unitTime;
        
        // シークバー周りの情報を変更
        if (b <0) {
            if (referencedHour.isChecked()) {
                unitTime = intervalList[intervalSeek.getProgress()];
                intervalSeek.setMax(intervalList.length-1);
            } else {
                unitTime = intervalSeek.getProgress() + intervalMin[0];
                intervalSeek.setMax(intervalMax[0]-intervalMin[0]);
            }
        } else { 
            if (b == 1) { // 正時基準に変更された場合
                unitTime =intervalSeek.getProgress()+intervalMin[0];
                if (unitTime>intervalMax[1]) unitTime=intervalMax[1];

                for (int i = 0; i < intervalList.length; i++) { // 分からintervalListのインデックスに変換
                    if (unitTime < intervalList[i]) {
                        intervalSeek.setProgress(i);
                        unitTime= intervalList[i];
                        break;
                    }
                }
                intervalSeek.setMax(intervalList.length-1);

            } else { 
                unitTime=intervalList[intervalSeek.getProgress()];
                intervalSeek.setMax(intervalMax[0]-intervalMin[0]);
                intervalSeek.setProgress(unitTime-intervalMin[0]);
            }
        }
        intervalNum.setText(String.valueOf(unitTime));
//        intervalNum.setText(String.valueOf(intervalSeek.getProgress()));

        //鳴動間隔の変更処理
        Calendar cal = Calendar.getInstance();
        int hour=cal.get(Calendar.HOUR_OF_DAY);
        int minute=cal.get(Calendar.MINUTE);

        // 第3引数は、表示期間（LENGTH_SHORT、または、LENGTH_LONG）
        TextView nextTime=findViewById(R.id.nextTime);

        // TODO 時間帯の開始時刻をint zoneStart[]とする
        // TODO 時間帯の終了時刻をint zoneEnd[]とする
        // TODO それぞれ、分単位の時刻を格納
        //TODO 時間帯が無効なときは-1を代入

        if (referencedHour.isChecked()) {
            int units = (minute-(minute % unitTime))/unitTime;
            minute = units  * unitTime;
        }
        minute += unitTime;
        if(minute>=60) {
            minute-=60;
            hour++;
            hour-= (hour>=24) ? 24:0;
        }

        String time = String.format(Locale.US,"%1$02d:%2$02d", hour,minute);
        ((TextView)findViewById(R.id.nextTime)).setText(time);
    }
    public void soundTime() {
        Calendar cal = Calendar.getInstance();
        int hour=cal.get(Calendar.HOUR_OF_DAY);
        int minute=cal.get(Calendar.MINUTE);
        int minuteDivide=15;
        int min_1=minute % minuteDivide;
        int min_15=(minute -min_1) / minuteDivide;

        if (hour>12) hour -=12;
        if (hour == 0) hour=12;

        for (int i=0;i<hour;i++) {
            SoundsPool.play(soundId[0],1.0f,1.0f,0,0,1.0f);
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (min_15>0) {
            for (int i=0;i<min_15;i++) {
                SoundsPool.play(soundId[1],1.0f,1.0f,0,0,1.0f);
                try {
                    TimeUnit.MILLISECONDS.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            TimeUnit.MILLISECONDS.sleep(400);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (min_1>0) {
            for (int i=0;i<min_1;i++) {
                SoundsPool.play(soundId[2],1.0f,1.0f,0,0,1.0f);
                try {
                    TimeUnit.MILLISECONDS.sleep(400);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
