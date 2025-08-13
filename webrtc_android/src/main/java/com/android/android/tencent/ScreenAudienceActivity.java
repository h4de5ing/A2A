package com.android.android.tencent;

import static com.tencent.trtc.TRTCCloudDef.TRTCRoleAudience;
import static com.tencent.trtc.TRTCCloudDef.TRTC_APP_SCENE_LIVE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.android.R;
import com.tencent.liteav.TXLiteAVCode;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;

import java.lang.ref.WeakReference;

//观众
public class ScreenAudienceActivity extends Activity {
    private static final String TAG = "ScreenAudienceActivity";

    private TRTCCloud mTRTCCloud;
    private TXCloudVideoView mScreenShareView;
    private LinearLayout mLLRoomInfo;

    private String mRoomId;
    private String mUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screenshare_activity_audience);
        handleIntent();
        initView();
        enterRoom();
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (null != intent) {
            if (intent.getStringExtra(Constant.USER_ID) != null) {
                mUserId = intent.getStringExtra(Constant.USER_ID);
            } else {
                finish();
            }
            if (intent.getStringExtra(Constant.ROOM_ID) != null) {
                mRoomId = intent.getStringExtra(Constant.ROOM_ID);
            } else {
                finish();
            }
        }
    }

    private void initView() {
        mScreenShareView = findViewById(R.id.live_cloud_remote_screenshare);
        ((TextView) findViewById(R.id.trtc_tv_room_number)).setText(mRoomId);
        mLLRoomInfo = findViewById(R.id.ll_room_info);
        findViewById(R.id.trtc_ic_back).setOnClickListener(view -> finish());
    }

    protected void enterRoom() {
        mTRTCCloud = TRTCCloud.sharedInstance(getApplicationContext());
        mTRTCCloud.setListener(new TRTCCloudImplListener(ScreenAudienceActivity.this));

        TRTCCloudDef.TRTCParams mTRTCParams = new TRTCCloudDef.TRTCParams();
        mTRTCParams.sdkAppId = GenerateTestUserSig.SDKAPPID;
        mTRTCParams.userId = mUserId;
        mTRTCParams.roomId = Integer.parseInt(mRoomId);
        mTRTCParams.userSig = GenerateTestUserSig.genTestUserSig(mTRTCParams.userId);
        mTRTCParams.role = TRTCRoleAudience;

        mTRTCCloud.enterRoom(mTRTCParams, TRTC_APP_SCENE_LIVE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitRoom();
    }

    private class TRTCCloudImplListener extends TRTCCloudListener {
        private final WeakReference<ScreenAudienceActivity> mContext;

        public TRTCCloudImplListener(ScreenAudienceActivity activity) {
            super();
            mContext = new WeakReference<>(activity);
        }

        @Override
        public void onFirstVideoFrame(String s, int i, int i1, int i2) {
            super.onFirstVideoFrame(s, i, i1, i2);
        }

        @Override
        public void onUserVideoAvailable(String userId, boolean available) {
            Log.d(TAG, "onUserVideoAvailable userId " + userId + ",available " + available);
            if (available) {
                mLLRoomInfo.setVisibility(View.GONE);
                mScreenShareView.setVisibility(View.VISIBLE);
                mTRTCCloud.startRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, mScreenShareView);
            } else {
                mLLRoomInfo.setVisibility(View.VISIBLE);
                mScreenShareView.setVisibility(View.GONE);
                mTRTCCloud.stopRemoteView(userId, TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG);
            }
        }

        @Override
        public void onError(int errCode, String errMsg, Bundle extraInfo) {
            Log.d(TAG, "sdk callback onError");
            ScreenAudienceActivity activity = mContext.get();
            if (activity != null) {
                Toast.makeText(activity, "onError: " + errMsg + "[" + errCode + "]", Toast.LENGTH_SHORT).show();
                if (errCode == TXLiteAVCode.ERR_ROOM_ENTER_FAIL) {
                    exitRoom();
                }
            }
        }
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
}
