package me.tom.mosaicimage;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import android.util.AttributeSet;

import android.view.MotionEvent;
import android.view.View;

import com.tbruyelle.rxpermissions.RxPermissions;

import me.tom.mosaicimage.utils.FileUtils;
import me.tom.mosaicimage.utils.ImageUtils;
import me.tom.mosaicimage.utils.ToastUtils;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MosaicView extends View {

    public static final int SCALE_FIT_X = 0;
    public static final int SCALE_FIT_Y = 1;
    public static final int SCALE_FIT_XY = 2;

    protected Context mContext;

    protected int mMosaicAreaSize;
    protected int mScaleType;

    protected Bitmap mSourceImage;
    protected Bitmap mMosaicImage;
    protected Bitmap mPlaceholderImage;

    protected Canvas mMosaicCanvas;
    protected Bitmap mMosaicCanvasBitmap;

    protected Path mPath;
    protected Paint mPaint;

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
        mScaleType = SCALE_FIT_XY;
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
        if (mMosaicImage == null || mSourceImage == null) {
            if (mPlaceholderImage != null) {
                canvas.drawBitmap(mPlaceholderImage, null, getImageRect(mPlaceholderImage), null);
            }
            return;
        }
        if (mMosaicCanvasBitmap == null) {
            mMosaicCanvasBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        }
        if (mMosaicCanvas == null) {
            mMosaicCanvas = new Canvas(mMosaicCanvasBitmap);
        }
        Rect imageRect = getImageRect(mSourceImage);
        canvas.drawBitmap(mMosaicImage, null, imageRect, null);
        mMosaicCanvas.drawBitmap(mSourceImage, null, imageRect, null);
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

    public MosaicView scaleType(int scaleType) {
        mScaleType = scaleType;
        return this;
    }

    public MosaicView placeholder(int resId) {
        mPlaceholderImage = BitmapFactory.decodeResource(mContext.getResources(), resId);
        return this;
    }

    public void load(final String url) {
        RxPermissions rxPermissions = new RxPermissions((Activity) mContext);
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        if (granted) {
                            invalidate();
                            if (url.toLowerCase().startsWith("http://") || url.toLowerCase().startsWith("https://")) {
                                setSourceImageFromNetwork(url);
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

    public Observable<String> saveToFile() {
        return saveToFile(FileUtils.getTempFilePath(mContext, System.currentTimeMillis() + ".jpg"));
    }

    public Observable<String> saveToFile(final String filePath) {
        RxPermissions rxPermissions = new RxPermissions((Activity)mContext);
        return rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .map(new Func1<Boolean, String>() {
                    @Override
                    public String call(Boolean granted) {
                        if (granted) {
                            Bitmap bitmap = getBitmap();
                            boolean status = ImageUtils.saveToFile(bitmap, filePath);
                            bitmap.recycle();
                            return status ? filePath : null;
                        }
                        return null;
                    }
                });
    }

    public Bitmap getBitmap() {
        setDrawingCacheEnabled(true);
        Bitmap drawingCache = getDrawingCache();
        Rect rect = getImageRect(mSourceImage);
        Bitmap sourceBitmap = Bitmap.createBitmap(drawingCache, rect.left, rect.top, rect.width(), rect.height());
        Matrix matrix = new Matrix();
        matrix.postScale(
                ((float) mSourceImage.getWidth()) / sourceBitmap.getWidth(),
                ((float) mSourceImage.getHeight()) / sourceBitmap.getHeight()
        );
        Bitmap destBitmap = Bitmap.createBitmap(
                sourceBitmap, 0, 0,
                sourceBitmap.getWidth(), sourceBitmap.getHeight(),
                matrix, true
        );
        setDrawingCacheEnabled(false);
        sourceBitmap.recycle();
        return destBitmap;
    }

    protected Rect getImageRect(Bitmap bitmap) {
        Rect rect;
        int bitmapWidth;
        int bitmapHeight;
        int width = getWidth();
        int height = getHeight();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        float ratio = ((float) bitmap.getWidth()) / bitmap.getHeight();
        switch (mScaleType) {
            case SCALE_FIT_X:
                bitmapWidth = width - paddingLeft - paddingRight;
                bitmapHeight = (int) Math.ceil(bitmapWidth / ratio);
                if (bitmapHeight >= (height - paddingTop - paddingBottom)) {
                    rect = new Rect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
                } else {
                    rect = new Rect(paddingLeft, (height - bitmapHeight) / 2, width - paddingRight, (height + bitmapHeight) / 2);
                }
                break;
            case SCALE_FIT_Y:
                bitmapHeight = height - paddingTop - paddingBottom;
                bitmapWidth = (int) Math.ceil(bitmapHeight * ratio);
                if (bitmapWidth >= (width - paddingLeft - paddingRight)) {
                    rect = new Rect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
                } else {
                    rect = new Rect((width - bitmapWidth) / 2, paddingTop, (width + bitmapWidth) / 2, height - paddingBottom);
                }
                break;
            default:
                rect = new Rect(paddingLeft, paddingTop, width - paddingRight, height - paddingBottom);
                break;
        }
        return rect;
    }

    protected void setSourceImageFromNetwork(String url) {
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
