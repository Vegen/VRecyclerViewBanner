package com.vegen.open.library;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

/**
 * @author Vegen
 * @creation_time 2018/10/31
 * @description 可可见页为两页和三页轮播的控件，具体属性如下
 */

public class VRecyclerViewBanner extends FrameLayout {

    private int autoPlayDuration;//刷新间隔时间

    private boolean showIndicator;//是否显示指示器
    private RecyclerView indicatorContainer;
    private Drawable mSelectedDrawable;
    private Drawable mUnselectedDrawable;
    private IndicatorAdapter indicatorAdapter;
    private int indicatorMargin;//指示器间距
    private RecyclerView mRecyclerView;

    private BannerLayoutManager mLayoutManager;

    private int WHAT_AUTO_PLAY = 1000;

    private boolean hasInit;
    private int bannerSize = 1;
    private int currentIndex;
    private boolean isPlaying = false;

    private boolean isAutoPlaying = true;
    int itemSpace;
    float centerScale;
    float moveSpeed;

    private int showItemCount = 3;          // 可视范围内的 item 数
    private float firstItemMarginLeft = 0;  // 第一个 item 的左边距

    protected Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == WHAT_AUTO_PLAY) {
                if (currentIndex == mLayoutManager.getCurrentPosition()) {
                    ++currentIndex;
                    mRecyclerView.smoothScrollToPosition(currentIndex);
                    mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration);
                    refreshIndicator();
                }
            }
            return false;
        }
    });

    public VRecyclerViewBanner(Context context) {
        this(context, null);
    }

    public VRecyclerViewBanner(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VRecyclerViewBanner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    protected void initView(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VRecyclerViewBanner);
        showIndicator = a.getBoolean(R.styleable.VRecyclerViewBanner_showIndicator, false);
        autoPlayDuration = a.getInt(R.styleable.VRecyclerViewBanner_interval, 4000);
        isAutoPlaying = a.getBoolean(R.styleable.VRecyclerViewBanner_autoPlaying, true);
        itemSpace = dp2px(a.getInt(R.styleable.VRecyclerViewBanner_itemSpace, 20));
        centerScale = a.getFloat(R.styleable.VRecyclerViewBanner_centerScale, 1f);
        moveSpeed = a.getFloat(R.styleable.VRecyclerViewBanner_moveSpeed, 1.0f);
        showItemCount = a.getInt(R.styleable.VRecyclerViewBanner_showItemCount, showItemCount);
        firstItemMarginLeft = a.getDimension(R.styleable.VRecyclerViewBanner_firstItemMarginLeft, dp2px((int) firstItemMarginLeft));

        if (mSelectedDrawable == null) {
            //绘制默认选中状态图形
            GradientDrawable selectedGradientDrawable = new GradientDrawable();
            selectedGradientDrawable.setShape(GradientDrawable.OVAL);
            selectedGradientDrawable.setColor(Color.RED);
            selectedGradientDrawable.setSize(dp2px(5), dp2px(5));
            selectedGradientDrawable.setCornerRadius(dp2px(5) / 2);
            mSelectedDrawable = new LayerDrawable(new Drawable[]{selectedGradientDrawable});
        }
        if (mUnselectedDrawable == null) {
            //绘制默认未选中状态图形
            GradientDrawable unSelectedGradientDrawable = new GradientDrawable();
            unSelectedGradientDrawable.setShape(GradientDrawable.OVAL);
            unSelectedGradientDrawable.setColor(Color.GRAY);
            unSelectedGradientDrawable.setSize(dp2px(5), dp2px(5));
            unSelectedGradientDrawable.setCornerRadius(dp2px(5) / 2);
            mUnselectedDrawable = new LayerDrawable(new Drawable[]{unSelectedGradientDrawable});
        }

        indicatorMargin = dp2px(4);
        int marginLeft = dp2px(16);
        int marginRight = dp2px(0);
        int marginBottom = dp2px(11);
        int gravity = GravityCompat.START;
        int o = a.getInt(R.styleable.VRecyclerViewBanner_orientation, 0);
        int orientation = 0;
        if (o == 0) {
            orientation = OrientationHelper.HORIZONTAL;
        } else if (o == 1) {
            orientation = OrientationHelper.VERTICAL;
        }
        a.recycle();
        //轮播图部分
        mRecyclerView = new RecyclerView(context);
        LayoutParams vpLayoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        addView(mRecyclerView, vpLayoutParams);
        mLayoutManager = new BannerLayoutManager(getContext(), orientation, showItemCount);
        mLayoutManager.setItemSpace(itemSpace);
        mLayoutManager.setCenterScale(centerScale);
        mLayoutManager.setMoveSpeed(moveSpeed);
        mLayoutManager.setFirstItemMarginLeft(firstItemMarginLeft);
        mLayoutManager.setOnPageChangeListener(new BannerLayoutManager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (onPageChangeListener != null) {
                    Log.e("Banner2", "position:" + position);
                    onPageChangeListener.onPageSelected(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mRecyclerView.setLayoutManager(mLayoutManager);
        new CenterSnapHelper().attachToRecyclerView(mRecyclerView);


        //指示器部分
        indicatorContainer = new RecyclerView(context);
        LinearLayoutManager indicatorLayoutManager = new LinearLayoutManager(context, orientation, false);
        indicatorContainer.setLayoutManager(indicatorLayoutManager);
        indicatorAdapter = new IndicatorAdapter();
        indicatorContainer.setAdapter(indicatorAdapter);
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | gravity;
        params.setMargins(marginLeft, 0, marginRight, marginBottom);
        addView(indicatorContainer, params);
        if (!showIndicator) {
            indicatorContainer.setVisibility(GONE);
        }
    }

    /** 模式 **/
    public enum ITEM_COUNT{
        TWO, THREE
    }

    /**
     * 设置显示的可见范围最大 item 数量
     * @param count
     */
    public void setShowItemCount(ITEM_COUNT count) {
        this.showItemCount = (count == ITEM_COUNT.TWO ? 2 : 3);
        mLayoutManager.setMaxVisibleItemCount(this.showItemCount);
    }

    public void setFirstItemMarginLeft(float marginLeft) {
        this.firstItemMarginLeft = marginLeft;
        mLayoutManager.setFirstItemMarginLeft(marginLeft);
    }

    // 设置是否禁止滚动播放
    public void setAutoPlaying(boolean isAutoPlaying) {
        this.isAutoPlaying = isAutoPlaying;
        setPlaying(this.isAutoPlaying);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    //设置是否显示指示器
    public void setShowIndicator(boolean showIndicator) {
        this.showIndicator = showIndicator;
        indicatorContainer.setVisibility(showIndicator ? VISIBLE : GONE);
    }

    //设置当前图片缩放系数
    public void setCenterScale(float centerScale) {
        this.centerScale = centerScale;
        mLayoutManager.setCenterScale(centerScale);
    }

    //设置跟随手指的移动速度
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
        mLayoutManager.setMoveSpeed(moveSpeed);
    }

    //设置图片间距
    public void setItemSpace(int itemSpace) {
        this.itemSpace = dp2px(itemSpace);
        mLayoutManager.setItemSpace(itemSpace);
    }

    /**
     * 设置轮播间隔时间
     *
     * @param autoPlayDuration 时间毫秒
     */
    public void setAutoPlayDuration(int autoPlayDuration) {
        this.autoPlayDuration = autoPlayDuration;
    }

    public void setOrientation(int orientation) {
        mLayoutManager.setOrientation(orientation);
    }

    /**
     * 设置是否自动播放（上锁）
     *
     * @param playing 开始播放
     */
    public synchronized void setPlaying(boolean playing) {
        if (isAutoPlaying && hasInit) {
            if (!isPlaying && playing) {
                mHandler.sendEmptyMessageDelayed(WHAT_AUTO_PLAY, autoPlayDuration);
                isPlaying = true;
            } else if (isPlaying && !playing) {
                mHandler.removeMessages(WHAT_AUTO_PLAY);
                isPlaying = false;
            }
        }
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    /**
     * 设置当前位置
     *
     * @param index
     */
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
        mRecyclerView.scrollToPosition(index);
    }

    /**
     * 刷新轮播标识
     */
    public void refreshInfinite(){
        bannerSize = mRecyclerView.getAdapter().getItemCount();
        mLayoutManager.setInfinite(bannerSize >= showItemCount);
    }

    /**
     * 设置轮播数据集
     */
    public void setAdapter(RecyclerView.Adapter adapter) {
        hasInit = true;
        mRecyclerView.setAdapter(adapter);
        bannerSize = adapter.getItemCount();
        mLayoutManager.setInfinite(bannerSize >= showItemCount);
        setPlaying(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dx != 0) {
                    setPlaying(false);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                int first = mLayoutManager.getCurrentPosition();
                Log.d("xxx", "onScrollStateChanged");
                if (currentIndex != first) {
                    currentIndex = first;
                }
                if (newState == SCROLL_STATE_IDLE) {
                    setPlaying(true);
                }
                refreshIndicator();
            }
        });
//        hasInit = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setPlaying(false);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                setPlaying(true);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setPlaying(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setPlaying(false);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            setPlaying(true);
        } else {
            setPlaying(false);
        }
    }

    /**
     * 标示点适配器
     */
    protected class IndicatorAdapter extends RecyclerView.Adapter {

        int currentPosition = 0;

        public void setPosition(int currentPosition) {
            this.currentPosition = currentPosition;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            ImageView bannerPoint = new ImageView(getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(indicatorMargin, indicatorMargin, indicatorMargin, indicatorMargin);
            bannerPoint.setLayoutParams(lp);
            return new RecyclerView.ViewHolder(bannerPoint) {
            };
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ImageView bannerPoint = (ImageView) holder.itemView;
            bannerPoint.setImageDrawable(currentPosition == position ? mSelectedDrawable : mUnselectedDrawable);

        }

        @Override
        public int getItemCount() {
            return bannerSize;
        }
    }

    protected int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                Resources.getSystem().getDisplayMetrics());
    }

    /**
     * 改变导航的指示点
     */
    protected synchronized void refreshIndicator() {
        if (showIndicator && bannerSize > 1) {
            indicatorAdapter.setPosition(currentIndex % bannerSize);
            indicatorAdapter.notifyDataSetChanged();
        }
    }

    public void setOnPageChangeListener(OnPageChangeListener listener){
        this.onPageChangeListener = listener;
    }

    private OnPageChangeListener onPageChangeListener;

    public interface OnPageChangeListener{
        void onPageSelected(int position);
    }

    public interface OnBannerItemClickListener {
        void onItemClick(int position);
    }


}