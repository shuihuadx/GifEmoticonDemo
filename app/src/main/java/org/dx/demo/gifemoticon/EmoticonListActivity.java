package org.dx.demo.gifemoticon;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EmoticonListActivity extends AppCompatActivity {
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emoticon_list);

        mRecyclerView = findViewById(R.id.emoticon_list_rl);
        LinearLayoutManager layoutManager = new LinearLayoutManager(EmoticonListActivity.this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(new EmoticonListAdapter(initStringList(50)));
    }
    private List<String> initStringList(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add("123456789");
        }
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        GifEmoticonHelper.getInstance().playGifEmoticon(mRecyclerView);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GifEmoticonHelper.getInstance().stopGifEmoticon(mRecyclerView);
    }

}
