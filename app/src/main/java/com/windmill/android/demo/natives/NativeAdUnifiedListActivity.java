package com.windmill.android.demo.natives;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.windmill.android.demo.Constants;
import com.windmill.android.demo.R;
import com.windmill.android.demo.view.ILoadMoreListener;
import com.windmill.android.demo.view.LoadMoreListView;
import com.windmill.sdk.WMConstants;
import com.windmill.sdk.WindMillError;
import com.windmill.sdk.natives.WMNativeAdContainer;
import com.windmill.sdk.natives.WMNativeAdData;
import com.windmill.sdk.natives.WMNativeAdDataType;
import com.windmill.sdk.natives.WMNativeAdRequest;
import com.windmill.sdk.natives.WMNativeUnifiedAd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class NativeAdUnifiedListActivity extends AppCompatActivity {

    private static final int LIST_ITEM_COUNT = 10;
    private LoadMoreListView mListView;
    private MyAdapter myAdapter;
    private WMNativeUnifiedAd windNativeUnifiedAd;
    private int userID = 0;
    private String placementId;

    private List<WMNativeAdData> mData;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_native_ad_unified_list);
        updatePlacement();
        initListView();
    }

    private void updatePlacement() {

        SharedPreferences sharedPreferences = this.getSharedPreferences("setting", 0);

        placementId = sharedPreferences.getString(Constants.CONF_UNIFIED_NATIVE_PLACEMENTID, Constants.native_unified_placement_id);

        Toast.makeText(this, "updatePlacement", Toast.LENGTH_SHORT).show();
    }

    private void initListView() {
        mListView = (LoadMoreListView) findViewById(R.id.unified_native_ad_list);
        mData = new ArrayList<>();
        myAdapter = new MyAdapter(this, mData);
        mListView.setAdapter(myAdapter);
        mListView.setLoadMoreListener(new ILoadMoreListener() {
            @Override
            public void onLoadMore() {
                loadListAd();
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loadListAd();
            }
        }, 500);
    }

    /**
     * 加载feed广告
     */
    private void loadListAd() {
        Log.d("lance", "-----------loadListAd-----------");
        userID++;
        Map<String, Object> options = new HashMap<>();
        options.put("user_id", String.valueOf(userID));
        if (windNativeUnifiedAd == null) {
            windNativeUnifiedAd = new WMNativeUnifiedAd(this, new WMNativeAdRequest(placementId, String.valueOf(userID), 3, options));
        }

        windNativeUnifiedAd.loadAd(new WMNativeUnifiedAd.NativeAdLoadListener() {
            @Override
            public void onError(WindMillError error, String placementId) {
                Log.d("lance", "onError:" + error.toString() + ":" + placementId);
                Toast.makeText(NativeAdUnifiedListActivity.this, "onError:" + error.toString(), Toast.LENGTH_SHORT).show();
                if (mListView != null) {
                    mListView.setLoadingFinish();
                }
            }

            @Override
            public void onFeedAdLoad(String placementId) {
                if (mListView != null) {
                    mListView.setLoadingFinish();
                }

                List<WMNativeAdData> unifiedADData = windNativeUnifiedAd.getNativeADDataList();

                if (unifiedADData != null && unifiedADData.size() > 0) {
                    Log.d("lance", "onFeedAdLoad:" + unifiedADData.size());
                    for (final WMNativeAdData adData : unifiedADData) {

                        for (int i = 0; i < LIST_ITEM_COUNT; i++) {
                            mData.add(null);
                        }

                        int count = mData.size();
                        mData.set(count - 1, adData);
                    }

                    myAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mData != null) {
            for (WMNativeAdData ad : mData) {
                if (ad != null) {
                    ad.destroy();
                }
            }
        }
        mData = null;
    }

    private static class MyAdapter extends BaseAdapter {

        private static final int ITEM_VIEW_TYPE_NORMAL = 0;
        private static final int ITEM_VIEW_TYPE_UNIFIED_AD = 1;
        private static final int ITEM_VIEW_TYPE_EXPRESS_AD = 2;
        private List<WMNativeAdData> mData;
        private Activity mActivity;

        public MyAdapter(Activity activity, List<WMNativeAdData> data) {
            this.mActivity = activity;
            this.mData = data;
        }

        @Override
        public int getCount() {
            return mData.size(); // for test
        }

        @Override
        public WMNativeAdData getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        //信息流广告的样式，有大图、小图、组图和视频，通过ad.getImageMode()来判断
        @Override
        public int getItemViewType(int position) {
            WMNativeAdData ad = getItem(position);
            if (ad == null) {
                return ITEM_VIEW_TYPE_NORMAL;
            } else {
                if (ad.isExpressAd()) {
                    return ITEM_VIEW_TYPE_EXPRESS_AD;
                } else {
                    return ITEM_VIEW_TYPE_UNIFIED_AD;
                }
            }
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            WMNativeAdData ad = getItem(position);
            switch (getItemViewType(position)) {
                case ITEM_VIEW_TYPE_UNIFIED_AD:
                    return getUnifiedADView(convertView, parent, ad);
                case ITEM_VIEW_TYPE_EXPRESS_AD:
                    return getExpressADView(convertView, parent, ad);
                default:
                    return getNormalView(convertView, parent, position);
            }
        }

        //渲染视频广告，以视频广告为例，以下说明
        @SuppressWarnings("RedundantCast")
        private View getUnifiedADView(View convertView, ViewGroup viewGroup, @NonNull final WMNativeAdData ad) {
            final UnifiedAdViewHolder adViewHolder;
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(mActivity).inflate(R.layout.listitem_ad_native, viewGroup, false);
                    adViewHolder = new UnifiedAdViewHolder(convertView);
                    convertView.setTag(adViewHolder);
                } else {
                    adViewHolder = (UnifiedAdViewHolder) convertView.getTag();
                }
                //绑定广告数据、设置交互回调
                bindListener(ad);
                //将容器和view链接起来
                ad.connectAdToView(mActivity, adViewHolder.windContainer, adViewHolder.adRender);
                //添加进容器
                if (adViewHolder.adContainer != null) {
                    adViewHolder.adContainer.removeAllViews();
                    if (adViewHolder.windContainer != null) {
                        ViewGroup parent = (ViewGroup) adViewHolder.windContainer.getParent();
                        if (parent != null) {
                            parent.removeView(adViewHolder.windContainer);
                        }
                        adViewHolder.adContainer.addView(adViewHolder.windContainer);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }

        private View getExpressADView(View convertView, ViewGroup viewGroup, @NonNull final WMNativeAdData ad) {
            final ExpressAdViewHolder adViewHolder;
            try {
                if (convertView == null) {
                    convertView = LayoutInflater.from(mActivity).inflate(R.layout.listitem_ad_native, viewGroup, false);
                    adViewHolder = new ExpressAdViewHolder(convertView);
                    convertView.setTag(adViewHolder);
                } else {
                    adViewHolder = (ExpressAdViewHolder) convertView.getTag();
                }
                //绑定广告数据、设置交互回调
                bindListener(ad);
                ad.render();
                View expressAdView = ad.getExpressAdView();
                //添加进容器
                if (adViewHolder.adContainer != null) {
                    adViewHolder.adContainer.removeAllViews();
                    if (expressAdView != null) {
                        ViewGroup parent = (ViewGroup) expressAdView.getParent();
                        if (parent != null) {
                            parent.removeView(expressAdView);
                        }
                        adViewHolder.adContainer.addView(expressAdView);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return convertView;
        }

        /**
         * 非广告list
         *
         * @param convertView
         * @param parent
         * @param position
         * @return
         */
        @SuppressWarnings("RedundantCast")
        @SuppressLint("SetTextI18n")
        private View getNormalView(View convertView, ViewGroup parent, int position) {
            NormalViewHolder normalViewHolder;
            if (convertView == null) {
                normalViewHolder = new NormalViewHolder();
                convertView = LayoutInflater.from(mActivity).inflate(R.layout.listitem_normal, parent, false);
                normalViewHolder.idle = (TextView) convertView.findViewById(R.id.text_idle);
                convertView.setTag(normalViewHolder);
            } else {
                normalViewHolder = (NormalViewHolder) convertView.getTag();
            }
            normalViewHolder.idle.setText("ListView item " + position);
            return convertView;
        }

        private void bindListener(final WMNativeAdData nativeAdData) {
            //设置广告交互监听
            nativeAdData.setInteractionListener(new WMNativeAdData.NativeAdInteractionListener() {
                @Override
                public void onADExposed() {
                    Log.d("lance", "----------onADExposed----------");
                }

                @Override
                public void onADClicked() {
                    Log.d("lance", "----------onADClicked----------");
                }

                @Override
                public void onRenderFail(View view, String msg, int code) {
                    Log.d("lance", "----------onRenderFail----------:" + msg + ":" + code);
                }

                @Override
                public void onRenderSuccess(View view, float width, float height) {
                    Log.d("lance", "----------onRenderSuccess----------:" + width + ":" + height);
                }

                @Override
                public void onADError(WindMillError error) {
                    Log.d("lance", "----------onADError----------:" + error.toString());
                }

            });

            //设置media监听
            if (nativeAdData.getAdPatternType() == WMNativeAdDataType.NATIVE_VIDEO_AD) {
                nativeAdData.setMediaListener(new WMNativeAdData.NativeADMediaListener() {
                    @Override
                    public void onVideoLoad() {
                        Log.d("lance", "----------onVideoLoad----------");
                    }

                    @Override
                    public void onVideoError(WindMillError error) {
                        Log.d("lance", "----------onVideoError----------:" + error.toString());
                    }

                    @Override
                    public void onVideoStart() {
                        Log.d("lance", "----------onVideoStart----------");
                    }

                    @Override
                    public void onVideoPause() {
                        Log.d("lance", "----------onVideoPause----------");
                    }

                    @Override
                    public void onVideoResume() {
                        Log.d("lance", "----------onVideoResume----------");
                    }

                    @Override
                    public void onVideoCompleted() {
                        Log.d("lance", "----------onVideoCompleted----------");
                    }
                });
            }

            if (nativeAdData.getInteractionType() == WMConstants.INTERACTION_TYPE_DOWNLOAD) {
                nativeAdData.setDownloadListener(new WMNativeAdData.AppDownloadListener() {
                    @Override
                    public void onIdle() {
                        Log.d("lance", "----------onIdle----------");
                    }

                    @Override
                    public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                        Log.d("lance", "----------onADExposed----------");
                    }

                    @Override
                    public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                        Log.d("lance", "----------onDownloadActive----------");
                    }

                    @Override
                    public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                        Log.d("lance", "----------onDownloadFailed----------");
                    }

                    @Override
                    public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                        Log.d("lance", "----------onDownloadFinished----------");
                    }

                    @Override
                    public void onInstalled(String fileName, String appName) {
                        Log.d("lance", "----------onInstalled----------");
                    }
                });
            }

            //设置dislike弹窗
            nativeAdData.setDislikeInteractionCallback(mActivity, new WMNativeAdData.DislikeInteractionCallback() {
                @Override
                public void onShow() {
                    Log.d("lance", "----------onShow----------");
                }

                @Override
                public void onSelected(int position, String value, boolean enforce) {
                    Log.d("lance", "----------onSelected----------:" + position + ":" + value + ":" + enforce);
                    //用户选择不喜欢原因后，移除广告展示
                    mData.remove(nativeAdData);
                    notifyDataSetChanged();
                }

                @Override
                public void onCancel() {
                    Log.d("lance", "----------onCancel----------");
                }
            });
        }

        private static class ExpressAdViewHolder extends AdViewHolder {
            public ExpressAdViewHolder(View convertView) {
                super(convertView);
            }
        }

        private static class UnifiedAdViewHolder extends AdViewHolder {
            //创建一个装整个自渲染广告的容器
            WMNativeAdContainer windContainer;
            //媒体自渲染的View
            NativeAdDemoRender adRender;

            public UnifiedAdViewHolder(View convertView) {
                super(convertView);
                windContainer = new WMNativeAdContainer(convertView.getContext());
                adRender = new NativeAdDemoRender();
            }
        }

        private static class AdViewHolder {

            FrameLayout adContainer;

            public AdViewHolder(View convertView) {
                adContainer = (FrameLayout) convertView.findViewById(R.id.iv_list_item_container);

            }
        }

        private static class NormalViewHolder {
            TextView idle;
        }
    }
}