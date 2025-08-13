package com.android.android.tencent;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAnchor;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.android.R;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;

//主播
public class ScreenAnchorActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "ScreenAnchorActivity";
    private TextView mTextScreenCaptureInfo;
    private Button mButtonStartCapture;
    private TRTCCloud mTRTCCloud;
    private String mRoomId;
    private String mUserId;
    private boolean mIsCapturing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        setContentView(R.layout.screenshare_activity_anchor);
        handleIntent();
        initView();
        enterRoom();
        ToolKitService.startToolKitService(this);
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (null != intent) {
            if (intent.getStringExtra(Constant.USER_ID) != null) {
                mUserId = intent.getStringExtra(Constant.USER_ID);
            }
            if (intent.getStringExtra(Constant.ROOM_ID) != null) {
                mRoomId = intent.getStringExtra(Constant.ROOM_ID);
            }
        }
    }

    private void initView() {
        mButtonStartCapture = findViewById(R.id.bt_start_capture);
        mTextScreenCaptureInfo = findViewById(R.id.tv_watch_tips);
        mButtonStartCapture.setOnClickListener(this);
    }

    private void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(ScreenAnchorActivity.this));

        TRTCCloudDef.TRTCParams screenParams = new TRTCCloudDef.TRTCParams();
        screenParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        screenParams.userId = mUserId;
        screenParams.roomId = Integer.parseInt(mRoomId);
        screenParams.userSig = GenerateTestUserSig.genTestUserSig(screenParams.userId);
        screenParams.role = TRTCRoleAnchor;

        mTRTCCloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_DEFAULT);
        mTRTCCloud.enterRoom(screenParams, TRTC_APP_SCENE_VIDEOCALL);
        mTRTCCloud.muteLocalAudio(true);//静音
        String text = getString(R.string.screenshare_room_id) + mRoomId + "\n" + getString(R.string.screenshare_username) + mUserId + "\n" + getString(R.string.screenshare_resolution) + "\n" + getString(R.string.screenshare_watch_tips);
        mTextScreenCaptureInfo.setVisibility(View.VISIBLE);
        mTextScreenCaptureInfo.setText(text);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitRoom();
        ToolKitService.stopToolKitService(this);
    }

    private void exitRoom() {
        if (mTRTCCloud != null) {
            mTRTCCloud.stopLocalAudio();
            mTRTCCloud.stopLocalPreview();
            mTRTCCloud.exitRoom();
            mTRTCCloud.setListener(null);
        }
        mTRTCCloud = null;
        TRTCCloud.destroySharedInstance();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bt_start_capture) {
            if (mIsCapturing) {
                stopScreenCapture();
            } else {
                screenCapture();
            }
        }
    }

    private void screenCapture() {
        TRTCCloudDef.TRTCVideoEncParam encParams = new TRTCCloudDef.TRTCVideoEncParam();
        encParams.videoResolution = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_1280_720;
        encParams.videoResolutionMode = TRTCCloudDef.TRTC_VIDEO_RESOLUTION_MODE_PORTRAIT;
        encParams.videoFps = 10;
        encParams.enableAdjustRes = false;
        encParams.videoBitrate = 1200;

        TRTCCloudDef.TRTCScreenShareParams params = new TRTCCloudDef.TRTCScreenShareParams();
        mTRTCCloud.startScreenCapture(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, encParams, params);
        mIsCapturing = true;
        mButtonStartCapture.setText(getString(R.string.screenshare_stop));
    }

    private void stopScreenCapture() {
        mTRTCCloud.stopScreenCapture();
        mIsCapturing = false;
        mButtonStartCapture.setText(getString(R.string.screenshare_start));
    }


    private class TRTCCloudImplListener extends TRTCCloudListener {

        private final WeakReference<ScreenAnchorActivity> mContext;

        public TRTCCloudImplListener(ScreenAnchorActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            ScreenAnchorActivity activity = mContext.get();
            if (activity != null) {
                Toast.makeText(activity, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    activity.exitRoom();
                } else if (errCode == -1308) {
                    Toast.makeText(ScreenAnchorActivity.this, getString(R.string.screenshare_start_failed), Toast.LENGTH_SHORT).show();
                    stopScreenCapture();
                }
            }
        }
    }
}
