package me.tom.mosaicimage.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import me.tom.mosaicimage.MosaicView;

public class MainActivity extends AppCompatActivity {

    private MosaicView mMosaicView;
    private Button mClearButton;

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

        mMosaicView = (MosaicView) findViewById(R.id.mosaicView);
        mMosaicView.setMosaicAreaSize(16);
        mMosaicView.setImage("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1493658093207&di=cc2d0d17ecb9273177299f27d5dedffc&imgtype=0&src=http%3A%2F%2Fi-2.yxdown.com%2F2015%2F7%2F11%2FKDYwMHgp%2Fb1165dc9-ca88-4f05-912d-eeefa01da9e8.jpg");
    }
}
