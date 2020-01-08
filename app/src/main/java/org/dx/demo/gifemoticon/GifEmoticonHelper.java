package org.dx.demo.gifemoticon;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.LruCache;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import pl.droidsonroids.gif.GifDrawable;

/**
 * @author dx
 * gif富文本表情的帮助类
 */
public class GifEmoticonHelper {
    /**
     * GifDrawable的创建过程为直接从assets/目录下读取gif表情文件,比较耗时
     * 如果每次都从/assets/目录下读取,容易造成界面卡顿
     * 所以这里需要缓存GifDrawable对象方便复用
     * <p>
     * 这里的Key值为图片所在的assets/路径,如"emoticon-res/e1.gif"
     */
    private volatile LruCache<String, GifDrawable> gifDrawableCache;
    /**
     * 由于{@link GifDrawable#setCallback(Drawable.Callback)}方法内部依赖的是弱引用
     * 当Callback被回收以后,gif的图片刷新会停止,所以这里为每个Callback做一个强引用,防止被系统回收
     * 这里的Key值为图片所在的assets/路径,如"emoticon-res/e1.gif"
     */
    private final Map<String, MultiDrawableCallback> CALLBACK_CACHE = new HashMap<>(32);

    /**
     * 一个Drawable只能设置一个Drawable.Callback,但是我们内部将GifDrawable设计为了复用的Drawable
     * 当TextView使用同一个GifDrawable时,容易导致图片的刷新和停止冲突
     * 所以MultiDrawableCallback内部将会持有多个TextView的引用,方便控制每个TextView内部的gif图刷新
     */
    private class MultiDrawableCallback implements Drawable.Callback {
        /**
         * Key值为TextView的hashcode值
         */
        private final SparseArray<WeakReference<TextView>> CACHE = new SparseArray<>(32);

        /**
         * 用于判断当前Callback是否还存TextView的引用
         */
        private boolean haveTextView() {
            if (CACHE.size() <= 0) {
                return false;
            }
            for (int i = 0; i < CACHE.size(); i++) {
                if (CACHE.valueAt(i).get() != null) {
                    return true;
                }
            }
            return false;
        }

        private void putTextView(TextView tv) {
            int key = tv.hashCode();
            CACHE.put(key, new WeakReference<>(tv));
        }

        private void removeTextView(TextView tv) {
            int key = tv.hashCode();
            CACHE.remove(key);
        }

        private void clear() {
            CACHE.clear();
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            if (CACHE.size() <= 0) {
                return;
            }

            for (int i = 0; i < CACHE.size(); i++) {
                WeakReference<TextView> tvWeakReference = CACHE.valueAt(i);
                if (tvWeakReference.get() != null) {
                    TextView tv = tvWeakReference.get();
                    if (isTextViewVisible(tv)) {
                        // 当view是可见状态时,才会刷新
                        tv.invalidate(who.getBounds());
                    }
                }
            }
        }

        @Override
        public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {
            if (CACHE.size() <= 0) {
                return;
            }
            for (int i = 0; i < CACHE.size(); i++) {
                WeakReference<TextView> tvWeakReference = CACHE.valueAt(i);
                if (tvWeakReference.get() != null) {
                    tvWeakReference.get().postDelayed(what, when);
                }
            }
        }

        @Override
        public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {
            if (CACHE.size() <= 0) {
                return;
            }
            for (int i = 0; i < CACHE.size(); i++) {
                WeakReference<TextView> tvWeakReference = CACHE.valueAt(i);
                if (tvWeakReference.get() != null) {
                    tvWeakReference.get().removeCallbacks(what);
                }
            }
        }
    }

    private static final GifEmoticonHelper INSTANCE = new GifEmoticonHelper();

    private Rect actualViewRect = new Rect();
    private Rect screenRect = new Rect();

    /**
     * 判断当前View是否可见
     * 当View被滑出屏幕,即便View为View.VISIBLE状态,但也将其视为不可见
     */
    public boolean isTextViewVisible(final TextView textView) {
        if (textView == null) {
            return false;
        }
        if (!textView.isShown()) {
            return false;
        }
        textView.getGlobalVisibleRect(actualViewRect);
        // 屏幕朝向改变时,屏幕分辨率会有变动,所以这里每次都重新设置screenRect
        int screenWidth = textView.getContext().getResources().getDisplayMetrics().widthPixels;
        int screenHeight = textView.getContext().getResources().getDisplayMetrics().heightPixels;
        screenRect.set(0, 0, screenWidth, screenHeight);

        return actualViewRect.intersect(screenRect);
    }

    private GifEmoticonHelper() {
        // 单例模式,私有构造方法
    }

    public static GifEmoticonHelper getInstance() {
        if (INSTANCE.gifDrawableCache == null) {
            synchronized (INSTANCE) {
                if (INSTANCE.gifDrawableCache == null) {
                    // 获取到当前App的最大可用内存
                    int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
                    // 设置LruCache的缓存大小
                    int cacheSize = maxMemory / 8;
                    INSTANCE.gifDrawableCache = new LruCache<String, GifDrawable>(cacheSize) {
                        @Override
                        protected int sizeOf(String key, GifDrawable value) {
                            return (int) (value.getAllocationByteCount() / 1024);
                        }

                        /**
                         *
                         * @param evicted true表示GifDrawable是释放空间被删除
                         *                false表示put或remove导致
                         */
                        @Override
                        protected void entryRemoved(boolean evicted, String key, GifDrawable oldValue, GifDrawable newValue) {
                            // 当移GifDrawable被回收时,应当移除持有的CallBack强引用
                            if (evicted) {
                                INSTANCE.CALLBACK_CACHE.remove(key);
                            }
                        }
                    };
                }
            }
        }
        return INSTANCE;
    }

    private int dp2px(Context context, int dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据gif表情路径获取GifDrawable对象
     * 注意点:
     * 1. 返回的GifDrawable默认宽高为0,需要调用{@link Drawable#setBounds(Rect)}方法重新设置
     * 2. 返回的GifDrawable默认没有刷新回调,需要调用{@link #playGifEmoticon(TextView)}方法播放
     *
     * @param emoticonAssetPath 表情的assets/路径,如"emoticon-res/e1.gif"
     * @return 实际的GifDrawable对象, 默认尺寸为30x30dp, 默认情况下是停止播放的
     * 以下几个方法可让gif表情开始播放:
     * 1. GifDrawable#start();
     * 2. {@link #playGifEmoticon(TextView)}
     * 3. {@link #playGifEmoticon(View)}
     */
    public GifDrawable getGifDrawable(Context context, String emoticonAssetPath) {
        return getGifDrawable(context, 30, emoticonAssetPath);
    }

    /**
     * 根据gif表情路径获取GifDrawable对象
     * 注意点:
     * 1. 返回的GifDrawable默认宽高为0,需要调用{@link Drawable#setBounds(Rect)}方法重新设置
     * 2. 返回的GifDrawable默认没有刷新回调,需要调用{@link #playGifEmoticon(TextView)}方法播放
     *
     * @param emoticonAssetPath 表情的assets/路径,如"emoticon-res/e1.gif"
     * @param emoticonDpSize    表情尺寸,单位为dp
     * @return 实际的GifDrawable对象, 默认情况下是停止播放的
     * 以下几个方法可让gif表情开始播放:
     * 1. GifDrawable#start();
     * 2. {@link #playGifEmoticon(TextView)}
     * 3. {@link #playGifEmoticon(View)}
     */
    public GifDrawable getGifDrawable(Context context, int emoticonDpSize, String emoticonAssetPath) {
        // 不同尺寸的表情需要分别缓存,否者当修改gifDrawable的尺寸时,其它ui会受到影响
        String cacheKey = emoticonAssetPath + "--" + emoticonDpSize;
        GifDrawable gifDrawableCache = this.gifDrawableCache.get(cacheKey);
        if (gifDrawableCache != null) {
            return gifDrawableCache;
        }
        try {
            GifDrawable gifDrawable = new GifDrawable(context.getAssets(), emoticonAssetPath);
            // 默认情况下,gifDrawable的宽高为0,这里给定一个默认宽高为20dp
            int emoticonSize = dp2px(context, emoticonDpSize);
            gifDrawable.setBounds(0, 0, emoticonSize, emoticonSize);

            MultiDrawableCallback drawableCallback = new MultiDrawableCallback();
            gifDrawable.setCallback(drawableCallback);
            // 由于gifDrawable的刷新回调比较耗性能,先将刷新停止,需要显示的时候再开启
            gifDrawable.stop();
            CALLBACK_CACHE.put(cacheKey, drawableCallback);
            this.gifDrawableCache.put(cacheKey, gifDrawable);
            return gifDrawable;
        } catch (IOException e) {
            /*
            当GIF图片解析失败时,有以下几种情况
            1. 找不到图片文件;
            2. 图片格式不是gif格式;
            3. GIF图片破损
            */
            BitmapFactory.Options options = new BitmapFactory.Options();
            // inJustDecodeBounds设置为true以后,无需将图片加载到内存
            options.inJustDecodeBounds = true;
            try {
                BitmapFactory.decodeStream(context.getAssets().open(emoticonAssetPath), null, options);
                if (options.outMimeType.contains("image/gif")) {
                    throw new RuntimeException("decode GIF image failed,path=" + emoticonAssetPath);
                } else {
                    throw new RuntimeException("Require GIF image file,but MimeType=\"" + options.outMimeType + "\",path=" + emoticonAssetPath);
                }
            } catch (IOException e1) {
                throw new RuntimeException("GIF image not found,path=" + emoticonAssetPath);
            }
        }
    }

    /**
     * 获取gif图片的指定帧
     */
    public Drawable getSingleFrameDrawable(Context context, String emoticonPath, int frameIndex) {
        Bitmap keyFrameBitmap = getSingleFrameBitmap(context, emoticonPath, frameIndex);
        return new BitmapDrawable(context.getResources(), keyFrameBitmap);
    }

    /**
     * 获取gif图片的指定帧
     */
    public Bitmap getSingleFrameBitmap(Context context, String emoticonPath, int frameIndex) {
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex=" + frameIndex);
        }
        GifDrawable gifDrawable = getGifDrawable(context, emoticonPath);
        int numberOfFrames = gifDrawable.getNumberOfFrames();
        if (frameIndex >= numberOfFrames) {
            throw new RuntimeException("Keyframe error,keyFrame=" + frameIndex + ",but numberOfFrames=" + numberOfFrames);
        }
        return gifDrawable.seekToFrameAndGet(frameIndex);
    }

    /**
     * 开始播放TextView富文本中的gif表情,通常在TextView可见的时候调用
     *
     * @param tv 由于EditText属于TextView的子类,所以也可以传入EditText
     */
    public void playGifEmoticon(TextView tv) {
        CharSequence charSequence = tv.getText();
        if (TextUtils.isEmpty(charSequence)) {
            return;
        }
        // 找出TextView中的图片富文本ImageSpan
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
        ImageSpan[] imageSpans = spannableStringBuilder.getSpans(0, charSequence.length(), ImageSpan.class);
        if (imageSpans == null || imageSpans.length <= 0) {
            return;
        }
        // 遍历TextView中的ImageSpan
        // 如果ImageSpan使用GifDrawable绘制的话,就让GifDrawable开始播放
        // 并将当前TextView加入MultiDrawableCallback的内部引用,方便MultiDrawableCallback回调刷新TextView
        for (ImageSpan imageSpan : imageSpans) {
            Drawable drawable = imageSpan.getDrawable();
            if (drawable instanceof GifDrawable) {
                GifDrawable gifDrawable = (GifDrawable) drawable;
                Drawable.Callback callback = gifDrawable.getCallback();
                if (callback instanceof MultiDrawableCallback) {
                    // 如果callback为null,将不会进入当前分支,instanceof运算支持null
                    MultiDrawableCallback multiDrawableCallback = (MultiDrawableCallback) callback;
                    multiDrawableCallback.putTextView(tv);
                    gifDrawable.start();
                }
            }
        }
    }

    /**
     * 开始播放View包含的TextView富文本中的gif表情
     *
     * @param view 请传入一个ViewGroup
     */
    public void playGifEmoticon(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            playGifEmoticon((TextView) view);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                playGifEmoticon(viewGroup.getChildAt(i));
            }
        }
    }

    /**
     * 停止播放TextView富文本中的gif表情,以节省cpu资源
     * 通常在TextView不可见的时候调用当前方法,例如RecyclerView#onViewRecycled()方法被调用时
     *
     * @param tv 由于EditText属于TextView的子类,所以也可以传入EditText
     */
    public void stopGifEmoticon(TextView tv) {
        CharSequence charSequence = tv.getText();
        if (TextUtils.isEmpty(charSequence)) {
            return;
        }
        // 找出TextView中的图片富文本ImageSpan
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
        ImageSpan[] imageSpans = spannableStringBuilder.getSpans(0, charSequence.length(), ImageSpan.class);
        if (imageSpans == null || imageSpans.length <= 0) {
            return;
        }
        // 遍历TextView中的ImageSpan
        // 如果ImageSpan使用GifDrawable绘制的话,就让GifDrawable停止播放
        // 并将MultiDrawableCallback内部持有的TextView引用移除
        for (ImageSpan imageSpan : imageSpans) {
            Drawable drawable = imageSpan.getDrawable();
            if (drawable instanceof GifDrawable) {
                GifDrawable gifDrawable = (GifDrawable) drawable;
                Drawable.Callback callback = gifDrawable.getCallback();
                if (callback instanceof MultiDrawableCallback) {
                    // 如果callback为null,将不会进入当前分支,instanceof运算支持null
                    MultiDrawableCallback multiDrawableCallback = (MultiDrawableCallback) callback;
                    multiDrawableCallback.removeTextView(tv);
                    if (!multiDrawableCallback.haveTextView()) {
                        gifDrawable.stop();
                    }
                }
            }
        }
    }

    /**
     * 停止播放View包含的TextView富文本中的gif表情,通常view不可见或者被回收的时候调用
     * 例如在Activity回调onDetachedFromWindow()方法或者onDestroy()方法时,调用以下方法停止播放gif,以节省cpu资源
     * GifEmoticonHelper.getInstance().stopViewGifEmoticon(getWindow().getDecorView());
     */
    public void stopGifEmoticon(View view) {
        if (view == null) {
            return;
        }
        if (view instanceof TextView) {
            stopGifEmoticon((TextView) view);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                stopGifEmoticon(viewGroup.getChildAt(i));
            }
        }
    }
}
