package org.dx.demo.gifemoticon;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private TextView gifEmoticonTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.test_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EmoticonListActivity.class);
                startActivity(intent);
            }
        });

        gifEmoticonTv = findViewById(R.id.gif_emoticon_tv);

        // 我将gif表情资源放在了assets/emoticon-res/目录下
        GifDrawable gifDrawable = GifEmoticonHelper.getInstance()
                .getGifDrawable(MainActivity.this, "emoticon-res/e1.gif");
        // 设置gif表情的长宽各为30dp
        int emoticonSize = dp2px(MainActivity.this, 30);
        gifDrawable.setBounds(0, 0, emoticonSize, emoticonSize);

//        Drawable gifDrawable=test(gifEmoticonTv);

        // 创建一个图片富文本对象
        ImageSpan gifImageSpan = new ImageSpan(gifDrawable);

        SpannableStringBuilder ssBuilder = new SpannableStringBuilder("123456789");
        // 将ssBuilder中的第4个字符设为图片富文本
        ssBuilder.setSpan(gifImageSpan, 3, 4, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        gifEmoticonTv.setText(ssBuilder);
    }

    private Drawable test(final TextView tv) {
        try {
            Drawable drawable = Drawable.createFromStream(getAssets().open("emoticon-res/e1.gif"), "e1");
            // 设置gif表情的长宽各为30dp
            int emoticonSize = dp2px(MainActivity.this, 30);
            drawable.setBounds(0, 0, emoticonSize, emoticonSize);
            // Drawable深拷贝
            Drawable newDrawable = drawable.mutate().getConstantState().newDrawable();
            drawable.setCallback(new Drawable.Callback() {
                @Override
                public void invalidateDrawable(@NonNull Drawable who) {
                    Log.d(TAG, "invalidateDrawable: ");
                    tv.invalidate(who.getBounds());
                }

                @Override
                public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
                    tv.postDelayed(what, when);
                }

                @Override
                public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
                    tv.removeCallbacks(what);
                }
            });
            return drawable;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private int dp2px(Context context, int dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // 开始播放gif表情,以节省系统资源
        GifEmoticonHelper.getInstance().playGifEmoticon(gifEmoticonTv);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 停止播放gif表情,以节省系统资源
        GifEmoticonHelper.getInstance().stopGifEmoticon(gifEmoticonTv);
    }
}
