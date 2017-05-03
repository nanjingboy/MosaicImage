package me.tom.mosaicimage.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import me.tom.mosaicimage.MosaicView;
import me.tom.mosaicimage.utils.ToastUtils;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private MosaicView mMosaicView;
    private Button mClearButton;
    private Button mSaveButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mClearButton = (Button) findViewById(R.id.clearButton);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMosaicView.reset();
            }
        });

        mSaveButton = (Button) findViewById(R.id.saveButton);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMosaicView.saveToFile()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String filePath) {
                            if (filePath != null) {
                                ToastUtils.show(MainActivity.this, "File saved to:" + filePath);
                            }
                        }
                    });
            }
        });

        mMosaicView = (MosaicView) findViewById(R.id.mosaicView);
        mMosaicView.setMosaicAreaSize(16)
                .placeholder(R.drawable.loading)
                .load("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1493658093207&di=cc2d0d17ecb9273177299f27d5dedffc&imgtype=0&src=http%3A%2F%2Fi-2.yxdown.com%2F2015%2F7%2F11%2FKDYwMHgp%2Fb1165dc9-ca88-4f05-912d-eeefa01da9e8.jpg");
    }
}
