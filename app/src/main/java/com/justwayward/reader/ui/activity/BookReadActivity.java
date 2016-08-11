package com.justwayward.reader.ui.activity;

import android.os.AsyncTask;
import android.support.v7.widget.ListPopupWindow;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.justwayward.reader.R;
import com.justwayward.reader.base.BaseActivity;
import com.justwayward.reader.bean.BookToc;
import com.justwayward.reader.bean.ChapterRead;
import com.justwayward.reader.component.AppComponent;
import com.justwayward.reader.component.DaggerBookReadActivityComponent;
import com.justwayward.reader.ui.adapter.BookReadPageAdapter;
import com.justwayward.reader.ui.adapter.TocListAdapter;
import com.justwayward.reader.ui.contract.BookReadContract;
import com.justwayward.reader.ui.presenter.BookReadPresenter;
import com.justwayward.reader.utils.BookPageFactory;
import com.justwayward.reader.utils.LogUtils;
import com.justwayward.reader.view.BookReadFrameLayout;
import com.yuyh.library.bookflip.FlipViewController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.OnClick;

/**
 * Created by lfh on 2016/8/7.
 */
public class BookReadActivity extends BaseActivity implements BookReadContract.View, BookReadFrameLayout.OnScreenClickListener {

    @Bind(R.id.iv_Back)
    ImageView mIvBack;
    @Bind(R.id.tvBookReadReading)
    TextView mTvBookReadReading;
    @Bind(R.id.tvBookReadCommunity)
    TextView mTvBookReadCommunity;
    @Bind(R.id.llBookReadTop)
    LinearLayout mLlBookReadTop;
    @Bind(R.id.tvBookReadMode)
    TextView mTvBookReadMode;
    @Bind(R.id.tvBookReadFeedBack)
    TextView mTvBookReadFeedBack;
    @Bind(R.id.tvBookReadSettings)
    TextView mTvBookReadSettings;
    @Bind(R.id.tvBookReadDownload)
    TextView mTvBookReadDownload;
    @Bind(R.id.tvBookReadToc)
    TextView mTvBookReadToc;
    @Bind(R.id.llBookReadBottom)
    LinearLayout mLlBookReadBottom;
    @Bind(R.id.rlBookReadRoot)
    RelativeLayout mRlBookReadRoot;
    @Bind(R.id.brflRoot)
    BookReadFrameLayout mBookReadFrameLayout;

    @Bind(R.id.flipView)
    FlipViewController flipView;
    int lineHeight = 0;

    @Inject
    BookReadPresenter mPresenter;

    String bookId;
    int currentChapter = 1;
    BookPageFactory factory;

    List<BookToc.mixToc.Chapters> mChapterList = new ArrayList<>();
    List<String> mContentList = new ArrayList<>();
    BookReadPageAdapter readPageAdapter;

    private ListPopupWindow mTocListPopupWindow;
    private TocListAdapter mTocListAdapter;

    boolean startRead = false;

    @Override
    public int getLayoutId() {
        return R.layout.activity_book_read;
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerBookReadActivityComponent.builder()
                .appComponent(appComponent)
                //.mainActivityModule(new MainActivityModule(this))
                .build()
                .inject(this);
    }

    @Override
    public void initToolBar() {

    }

    @Override
    public void initDatas() {
        bookId = getIntent().getStringExtra("bookId");
    }

    @Override
    public void configViews() {
        View view = getLayoutInflater().inflate(R.layout.item_book_read_page, null);
        final TextView tv = (TextView) view.findViewById(R.id.tvBookReadContent);
        lineHeight = tv.getLineHeight();
        factory = new BookPageFactory(bookId, lineHeight);

        mTocListAdapter = new TocListAdapter(this, mChapterList);
        mTocListPopupWindow = new ListPopupWindow(this);
        mTocListPopupWindow.setAdapter(mTocListAdapter);
        mTocListPopupWindow.setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        mTocListPopupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        mTocListPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mTocListPopupWindow.dismiss();

            }
        });

        mPresenter.attachView(this);
        mPresenter.getBookToc(bookId, "chapters");
        mBookReadFrameLayout.setOnScreenClickListener(this);


    }

    @Override
    public void showBookToc(List<BookToc.mixToc.Chapters> list) {
        mChapterList.clear();
        mChapterList.addAll(list);

        if(factory.getBookFile(1).length()>50)
            showChapterRead(null, 1);
        else
            mPresenter.getChapterRead(list.get(0).link, 1);
    }

    @Override
    public void showChapterRead(ChapterRead.Chapter data, int chapter) {
        if (chapter == currentChapter) {
            for (int j = currentChapter + 1; j <= currentChapter + 3; j++) {
                if (factory.getBookFile(j).length() < 20) { // 不存在
                    mPresenter.getChapterRead(mChapterList.get(j - 1).link, j);
                }
            }
        }
        if(data != null)
            factory.append(data, chapter);
        if(factory.getBookFile(currentChapter).length()>20 && !startRead){
            startRead = true;
            new BookPageTask().execute();
        }
    }



    @OnClick(R.id.iv_Back)
    public void onClickBack() {
        finish();
    }

    @OnClick(R.id.tvBookReadToc)
    public void onClickToc() {
        if (!mTocListPopupWindow.isShowing()) {
            mTocListPopupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
            mTocListPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            mTocListPopupWindow.show();
        }
        mTocListAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        flipView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        flipView.onPause();
    }


    @Override
    public void onSideClick(boolean isLeft) {
        if (isLeft) {
            Toast.makeText(this,"上一页",Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,"下一页",Toast.LENGTH_SHORT).show();
        }

        if (mLlBookReadBottom.getVisibility() == View.VISIBLE) {
            mLlBookReadBottom.setVisibility(View.GONE);
            mLlBookReadTop.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCenterClick() {
        if (mLlBookReadBottom.getVisibility() == View.VISIBLE) {
            mLlBookReadBottom.setVisibility(View.GONE);
            mLlBookReadTop.setVisibility(View.GONE);
        } else {
            mLlBookReadBottom.setVisibility(View.VISIBLE);
            mLlBookReadTop.setVisibility(View.VISIBLE);
        }
    }

    class BookPageTask extends AsyncTask<Integer, Integer, List<String>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LogUtils.i("分页前" + new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date(System.currentTimeMillis())));
        }

        @Override
        protected List<String> doInBackground(Integer... params) {
            List<String> list = factory.readPage(factory.readTxt(currentChapter));
            return list;
        }

        @Override
        protected void onPostExecute(List<String> list) {
            super.onPostExecute(list);
            LogUtils.i("分页后" + new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(new Date(System.currentTimeMillis())));
            mContentList.clear();
            mContentList.addAll(list);
            readPageAdapter = new BookReadPageAdapter(mContext, mContentList);
            flipView.setAdapter(readPageAdapter);
        }
    }
}