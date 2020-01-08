package org.dx.demo.gifemoticon;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

import pl.droidsonroids.gif.GifDrawable;

public class EmoticonListAdapter extends RecyclerView.Adapter<EmoticonListAdapter.ViewHolder> {
    private Random mRandom = new Random();
    private String[] emoticonRes = new String[]{
            "emoticon-res/e1.gif",
            "emoticon-res/e2.gif",
            "emoticon-res/e3.gif",
            "emoticon-res/e4.gif",
            "emoticon-res/e5.gif",
            "emoticon-res/e6.gif",
            "emoticon-res/e7.gif",
            "emoticon-res/e8.gif",
            "emoticon-res/e9.gif",
            "emoticon-res/e10.gif",
            "emoticon-res/e11.gif",
            "emoticon-res/e12.gif"
    };
    private List<String> mData;

    public EmoticonListAdapter(List<String> data) {
        this.mData = data;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView emoticonTv;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            emoticonTv = itemView.findViewById(R.id.emoticon_list_item_tv);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_emoticon_list, viewGroup, false);
        return new ViewHolder(view);
    }

    private String randomEmoticonPath() {
        int index = mRandom.nextInt(emoticonRes.length);
        return emoticonRes[index];
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String text = mData.get(position);
        SpannableString spannable = new SpannableString(text);
        for (int index = 0; index < text.length(); index++) {
            String emoticonPath = randomEmoticonPath();
            GifDrawable gifDrawable = GifEmoticonHelper.getInstance()
                    .getGifDrawable(holder.emoticonTv.getContext(), 30, emoticonPath);
            ImageSpan imageSpan = new ImageSpan(gifDrawable);
            spannable.setSpan(imageSpan, index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        holder.emoticonTv.setText(spannable);

        GifEmoticonHelper.getInstance().playGifEmoticon(holder.emoticonTv);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        GifEmoticonHelper.getInstance().stopGifEmoticon(holder.emoticonTv);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }
}
