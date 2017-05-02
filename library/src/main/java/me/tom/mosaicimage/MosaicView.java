package me.tom.mosaicimage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import android.graphics.Paint;
import android.graphics.Path;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import me.tom.mosaicimage.utils.ImageUtils;
import me.tom.mosaicimage.utils.ToastUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MosaicView extends View {

    protected Context mContext;

    protected int mMosaicAreaSize;

    protected Bitmap mSourceImage;
    protected Bitmap mMosaicImage;

    protected Canvas mMosaicCanvas;
    protected Bitmap mMosaicCanvasBitmap;

    protected Path mPath;
    protected Paint mPaint;
    protected Rect mImageRect;

    public MosaicView(Context context) {
        this(context, null);
    }

    public MosaicView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MosaicView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mMosaicAreaSize = 16;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mMosaicAreaSize);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPath = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mImageRect == null) {
            mImageRect = new Rect(
                    getPaddingLeft(),
                    getPaddingTop(),
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom()
            );
        }
        if (mMosaicImage == null || mSourceImage == null) {
            return;
        }
        if (mMosaicCanvasBitmap == null) {
            mMosaicCanvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        }
        if (mMosaicCanvas == null) {
            mMosaicCanvas = new Canvas(mMosaicCanvasBitmap);
        }
        canvas.drawBitmap(mMosaicImage, null, mImageRect, null);
        mMosaicCanvas.drawBitmap(mSourceImage, null, mImageRect, null);
        mMosaicCanvas.drawPath(mPath, mPaint);
        canvas.drawBitmap(mMosaicCanvasBitmap, 0, 0, null);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPath.moveTo(event.getX(), event.getY());
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(event.getX(), event.getY());
                invalidate();
                break;
            default:
                break;
        }
        return true;
    }

    public MosaicView setMosaicAreaSize(int mosaicAreaSize) {
        mMosaicAreaSize = mosaicAreaSize;
        mPaint.setStrokeWidth(mMosaicAreaSize);
        return this;
    }

    public void load(final String url) {
        RxPermissions rxPermissions = new RxPermissions((Activity) mContext);
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                                setSourceImageFromNework(url);
                            } else {
                                setSourceImageFromLocal(url);

                            }
                        }
                    }
                });
    }

    public void reset() {
        mPath.reset();
        invalidate();
    }

    protected void setSourceImageFromNework(String url) {
        ImageUtils.download(mContext, url)
                .flatMap(new Func1<String, Observable<Bitmap>>() {
                    @Override
                    public Observable<Bitmap> call(String path) {
                        return ImageUtils.compress(path);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(mContext, R.string.mosaic_image_load_failed);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        mSourceImage = bitmap;
                        setMosaicImage();
                    }
                });
    }

    protected void setSourceImageFromLocal(String path) {
        ImageUtils.compress(path)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(mContext, R.string.mosaic_image_load_failed);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        mSourceImage = bitmap;
                        setMosaicImage();
                    }
                });
    }

    protected void setMosaicImage() {
        Observable.just(mSourceImage)
                .map(new Func1<Bitmap, Bitmap>() {
                    @Override
                    public Bitmap call(Bitmap bitmap) {
                        return ImageUtils.mosaic(bitmap, mMosaicAreaSize);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Bitmap>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        ToastUtils.show(mContext, R.string.mosaic_image_load_failed);
                    }

                    @Override
                    public void onNext(Bitmap bitmap) {
                        mMosaicImage = bitmap;
                        invalidate();
                    }
                });
    }
}
