package com.mredrock.cyxbsmobile.ui.activity.social;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mredrock.cyxbsmobile.R;
import com.mredrock.cyxbsmobile.component.widget.CircleImageView;
import com.mredrock.cyxbsmobile.model.social.HotNews;
import com.mredrock.cyxbsmobile.model.social.HotNewsContent;
import com.mredrock.cyxbsmobile.model.social.PersonInfo;
import com.mredrock.cyxbsmobile.network.RequestManager;
import com.mredrock.cyxbsmobile.subscriber.SimpleSubscriber;
import com.mredrock.cyxbsmobile.subscriber.SubscriberListener;
import com.mredrock.cyxbsmobile.ui.activity.BaseActivity;
import com.mredrock.cyxbsmobile.ui.adapter.HeaderViewRecyclerAdapter;
import com.mredrock.cyxbsmobile.ui.adapter.NewsAdapter;
import com.mredrock.cyxbsmobile.ui.fragment.social.BaseNewsFragment;
import com.mredrock.cyxbsmobile.util.ImageLoader;
import com.mredrock.cyxbsmobile.util.ScreenTools;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by mathiasluo on 16-5-6.
 */
public class PersonInfoActivity extends BaseActivity implements SwipeRefreshLayout.OnRefreshListener {

    public static final String TAG = "PersonInfoActivity";
    public static final String PERSON_AVATAR = "userAvatar";
    public static final String PERSON_NICKNAME = "userNickName";
    public static final String PERSON_USER_ID = "userId";

    @Bind(R.id.mToolbar)
    Toolbar mToolbar;
    @Bind(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mSwipeRefreshLayout;

    private String mUserAvatar;
    private String mNickName;
    private String mUserId;

    @Bind(R.id.recyclerView)
    RecyclerView mRecyclerView;

    private List<HotNews> mDatas = null;
    private BaseNewsFragment.FooterViewWrapper mFooterViewWrapper;
    protected NewsAdapter mNewsAdapter;
    private HeaderViewRecyclerAdapter mHeaderViewRecyclerAdapter;
    private HeaderViewWrapper mHeaderViewWrapper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_info);
        mUserAvatar = getIntent().getStringExtra(PERSON_AVATAR);
        mNickName = getIntent().getStringExtra(PERSON_NICKNAME);
        mUserId = getIntent().getStringExtra(PERSON_USER_ID);
        mHeaderViewWrapper = new HeaderViewWrapper(this, R.layout.list_person_info_header);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        initToolbar();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mNewsAdapter = new NewsAdapter(mDatas);
        mHeaderViewRecyclerAdapter = new HeaderViewRecyclerAdapter(mNewsAdapter);
        mRecyclerView.setAdapter(mHeaderViewRecyclerAdapter);
        mHeaderViewRecyclerAdapter.addHeaderView(mHeaderViewWrapper.view);

        mSwipeRefreshLayout.setColorSchemeColors(R.color.colorAccent);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mHeaderViewWrapper.setData(mUserAvatar, mNickName);
        showLaoding();
        requestData();
    }

    private void requestData() {
        RequestManager.getInstance().getPersonInfo(mUserId)
                .subscribe(new SimpleSubscriber<>(this, new SubscriberListener<PersonInfo>() {
                    @Override
                    public void onNext(PersonInfo personInfo) {
                        super.onNext(personInfo);
                        mHeaderViewWrapper.setIntroduction(personInfo.getIntroduction());
                    }
                }));

        RequestManager.getInstance().getPersonLatestList(mUserId, mNickName, mUserAvatar)
                .doOnSubscribe(() -> showLaoding())
                .subscribeOn(AndroidSchedulers.mainThread()) // 指定主线程
                .subscribe(newses -> {
                    if (mDatas == null) {
                        initAdapter(newses);
                        if (newses.size() == 0) mFooterViewWrapper.showLoadingNoData();
                    } else mNewsAdapter.replaceDatas(newses);
                    closeLoading();
                }, throwable -> {
                    closeLoading();
                    getDataFailed(throwable.toString());
                });
    }

    private void initAdapter(List<HotNews> datas) {
        mDatas = datas;
        mNewsAdapter = new NewsAdapter(mDatas) {
            @Override
            public void setDate(ViewHolder holder, HotNewsContent mDataBean) {
                super.setDate(holder, mDataBean);
                CardView cardView = (CardView) holder.itemView;
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) cardView.getLayoutParams();
                params.setMargins(0, 0, 0, 10);
                cardView.setLayoutParams(params);

                ViewGroup.LayoutParams ps = holder.mImgAvatar.getLayoutParams();
                ps.width = ScreenTools.instance(PersonInfoActivity.this).dip2px(42);
                ps.height = ps.width;
                holder.mImgAvatar.setLayoutParams(ps);
                holder.enableClick = false;

            }
        };
        mHeaderViewRecyclerAdapter.setAdapter(mNewsAdapter);
        addFooterView(mHeaderViewRecyclerAdapter);
    }

    private void addFooterView(HeaderViewRecyclerAdapter mHeaderViewRecyclerAdapter) {
        mFooterViewWrapper = new BaseNewsFragment.FooterViewWrapper(this, mRecyclerView);
        mHeaderViewRecyclerAdapter.addFooterView(mFooterViewWrapper.getFooterView());
        mFooterViewWrapper.showLoadingNoMoreData();
        mFooterViewWrapper.onFailedClick(view -> requestData());
    }


    private void initToolbar() {
        mToolbar.setNavigationIcon(R.mipmap.ic_arrow_back_white_24dp);
        mToolbar.setTitle("");
        setSupportActionBar(mToolbar);
        mToolbar.setNavigationOnClickListener(view -> PersonInfoActivity.this.finish());
    }

    public static final void StartActivityWithData(Context context, String userAvatar, String nickName, String userId) {
        Intent intent = new Intent(context, PersonInfoActivity.class);
        intent.putExtra(PERSON_AVATAR, userAvatar);
        intent.putExtra(PERSON_NICKNAME, nickName);
        intent.putExtra(PERSON_USER_ID, userId);
        context.startActivity(intent);
    }

    @Override
    public void onRefresh() {
        requestData();
    }

    private void showLaoding() {
        if (!mSwipeRefreshLayout.isRefreshing()) mSwipeRefreshLayout.setRefreshing(true);
    }

    private void closeLoading() {
        if (mSwipeRefreshLayout.isRefreshing()) mSwipeRefreshLayout.setRefreshing(false);
    }


    private void getDataFailed(String reason) {
        Toast.makeText(this, getString(R.string.erro), Toast.LENGTH_SHORT).show();
        Log.e(TAG, reason);
    }

    class HeaderViewWrapper {
        View view;
        @Bind(R.id.person_info_avatar)
        CircleImageView mCircleImageView;
        @Bind(R.id.peron_info_nickname)
        TextView mTextNickName;
        @Bind(R.id.person_info_introduction)
        TextView mTextIntroduction;


        public HeaderViewWrapper(Context context, int layoutId) {
            view = LayoutInflater.from(context).inflate(layoutId, null, false);
            ButterKnife.bind(this, view);
        }


        public void setData(String avatar, String nickname) {
            ImageLoader.getInstance().loadAvatar(mUserAvatar, mCircleImageView);
            mTextNickName.setText(mNickName);
        }

        public void setIntroduction(String introduction) {
            mTextIntroduction.setText(introduction);
        }
    }

}