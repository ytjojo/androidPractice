package com.ytjojo.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.google.gson.JsonObject;
import com.orhanobut.logger.Logger;
import com.ytjojo.domin.request.LoginRequest;
import com.ytjojo.domin.response.OrganAddrArea;
import com.ytjojo.domin.vo.LoginResponse;
import com.ytjojo.http.GitApiInterface;
import com.ytjojo.http.RetrofitClient;
import com.ytjojo.http.download.multithread.Manager;
import com.ytjojo.http.download.multithread.ProgressInfo;
import com.ytjojo.practice.R;
import com.ytjojo.rx.RxHttpHelper;
import com.ytjojo.utils.DensityUtil;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            Window window = getWindow();
            View v = window.findViewById(android.R.id.content);
            // Translucent status bar
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        Manager manager = new Manager(getExternalCacheDir().getAbsolutePath(),"",Constant.BIG_FILE_URLS[0]);
        Manager.subscribe(manager, new Observer<ProgressInfo>() {

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Logger.e(e.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(ProgressInfo progressInfo) {
                Logger.e(progressInfo.speed+" ---------------下载-----------------" +progressInfo.bytesRead);
            }
        });


        mDecorView = (ViewGroup) getWindow().getDecorView();
        mContentView = ((ViewGroup) mDecorView.getChildAt(0));

        mDecorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    //systemUI is visible
//                    mActionBar.show();
                } else {
                    //systemUI is invisible
//                    mActionBar.hide();
                }
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testGetDic();
                //testArray();
                if ((mDecorView.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    hideSystemUI();

                } else {
                    showSystemUI();
                }
            }
        });
        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setShowHideAnimationEnabled(true);
//        showSystemUI();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            }
        });
//        login();
    }

    GitApiInterface mGitApiInterface;

    public void setApi(GitApiInterface service) {
        mGitApiInterface = service;
    }

    private void login() {
        GitApiInterface gitApiInterface = RetrofitClient.getDefault().create(com.ytjojo.http.GitApiInterface.class);
        LoginRequest request = new LoginRequest();
        gitApiInterface.loginAttr(request.uid, request.pwd, request.rid, request.forAccessToken).
                compose(RxHttpHelper.applySchedulers()).subscribe(new Observer<LoginResponse>() {

            @Override
            public void onError(Throwable e) {
                Log.e("onError", e.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(LoginResponse response) {

//                RetrofitClient.TOKEN = loginResponse.properties.getAccessToken();
//                testArray();
                testGetDic();
            }
        });
    }

    private void getAddrArea() {
        GitApiInterface gitApiInterface = RetrofitClient.getDefault().create(GitApiInterface.class);
        LoginRequest request = new LoginRequest();
        gitApiInterface.getAddrArea(null, 0)
                .compose(RxHttpHelper.applySchedulers())
                .subscribe(new Observer<JsonObject>() {

                    @Override
                    public void onError(Throwable e) {
                        Log.e("onError", e.getMessage());
                        e.printStackTrace();
                        Log.e("onError", e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(JsonObject response) {

                        Logger.e(response.toString());
                    }
                });
    }

    private void testGetDic() {
        GitApiInterface gitApiInterface = RetrofitClient.getDefault().create(GitApiInterface.class);
        gitApiInterface.getHealthCardTypeDict1().compose(RxHttpHelper.applySchedulers())
                .subscribe(new Observer<Void>() {

                    @Override
                    public void onError(Throwable e) {
                        Logger.e("----------------getHealthCardTypeDict1" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(Void ss) {
                        Logger.e("----------------getHealthCardTypeDict1");
                    }
                });
    }

    long mBegin;

    private void testArray() {
        mBegin = System.currentTimeMillis();
        GitApiInterface gitApiInterface = RetrofitClient.getDefault().create(GitApiInterface.class);
        gitApiInterface.loginWithArray("http://ngaribata.ngarihealth.com:8480/ehealth-base-devtest/*.jsonRequest", 1).compose(RxHttpHelper.applySchedulers()).subscribe(new Observer<OrganAddrArea>() {

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
                Log.e("onError", e.getMessage());
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(OrganAddrArea organAddrArea) {
                Logger.e(organAddrArea.addrArea + "   " + organAddrArea.addrAreaText + ((System.currentTimeMillis() - mBegin)));
            }
        });
    }

    public static void setStyle1(AppBarLayout appBarLayout, Toolbar toolbar) {
        int statusBarHeight = DensityUtil.getStatusBarHeight(toolbar.getContext());
        int toolBarHeight = DensityUtil.getToolbarHeight(toolbar.getContext());
        if (appBarLayout.getChildCount() > 1) {
            View statusHolder = appBarLayout.getChildAt(0);
            appBarLayout.removeView(statusHolder);
        }
        toolbar.getLayoutParams().height = toolBarHeight + statusBarHeight;
        toolbar.setPadding(toolbar.getPaddingLeft(), statusBarHeight, toolbar.getPaddingRight(), toolbar.getPaddingBottom());
        appBarLayout.setFitsSystemWindows(false);
        toolbar.setFitsSystemWindows(false);
    }

    public static void setStyle2(AppBarLayout appBarLayout, Toolbar toolbar) {
        int statusBarHeight = DensityUtil.getStatusBarHeight(toolbar.getContext());
        int toolBarHeight = DensityUtil.getToolbarHeight(toolbar.getContext());
        if (appBarLayout.getChildCount() > 1) {
            View statusHolder = appBarLayout.getChildAt(0);
            statusHolder.setFitsSystemWindows(true);
            statusHolder.getLayoutParams().height = statusBarHeight;
        } else {
            View statusHolder = new View(toolbar.getContext());
            AppBarLayout.LayoutParams params = new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
            statusHolder.setFitsSystemWindows(true);
            statusHolder.setLayoutParams(params);
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            appBarLayout.addView(statusHolder, 0);
        }
        toolbar.getLayoutParams().height = toolBarHeight;
        appBarLayout.setFitsSystemWindows(true);
        toolbar.setFitsSystemWindows(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, DetailActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_status) {
            Intent intent = new Intent(this, StatusBarActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }

    private static final int INITIAL_DELAY = 1500;
    private ViewGroup mDecorView;
    private ActionBar mActionBar;
    private View mContentView;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            hideSystemUI();
        }
    };

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
//        if (hasFocus){
//            delayedHide(INITIAL_DELAY);
//        }else {
//            handler.removeMessages(0);
//            showSystemUI();
//        }
    }

    private void delayedHide(int delay) {
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, delay);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void hideSystemUI() {
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        getWindow().setStatusBarColor(Color.RED);
    }

    private void showSystemUI() {
//
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}
