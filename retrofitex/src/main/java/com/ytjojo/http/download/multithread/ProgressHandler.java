package com.ytjojo.http.download.multithread;

import android.os.SystemClock;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiPredicate;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;


/**
 * Created by Administrator on 2016/11/12 0012.
 */
public class ProgressHandler {
    enum AsyncAction {IDLE, STARTED, STOPE, FINISHED, FAILED}

    public static final int IDLE = 0;
    public static final int STARTED = 1;
    public static final int DOWNLOAD = 2;
    public static final int STOPING = 3;
    public static final int STOPED = 4;
    public static final int FINISH = 5;
    public static final int ERROR = 6;

    ConcurrentHashMap<Integer, Long> mTaskProgress;
    long contentLength;
    ProgressInfo mProgressInfo;
    long lastCompletSize;
    public volatile AsyncAction mSignal = AsyncAction.IDLE;
    AtomicLong mAtomicLong = new AtomicLong(IDLE);
    private Manager mManager;

    public ProgressHandler(Manager manager) {
        this.mManager = manager;
    }

    public void setTaskInfos(List<DownloadInfo> infos) {
        this.mDownloadInfos = infos;
        if (mTaskProgress == null) {
            this.mTaskProgress = new ConcurrentHashMap<>();
        }
        for (DownloadInfo info : infos) {
            this.mTaskProgress.put(info.getThreadId(), new Long(info.getCompeleteSize()));
        }

    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public synchronized boolean isAllTaskStoped() {
        final long state =  mAtomicLong.get();
        return state == STOPING||state == STOPED;
    }

    List<DownloadInfo> mDownloadInfos;

    public void setProgress(DownloadInfo info) {
        if (mTaskProgress == null) {
            mTaskProgress = new ConcurrentHashMap<>();
        }
        mTaskProgress.put(info.getThreadId(), info.getCompeleteSize());
    }

    public static final int DELAY = 30;

    public Observable<ProgressInfo> getProgress() {
        mLastMillis = SystemClock.uptimeMillis();
        return Observable.interval(DELAY, TimeUnit.MILLISECONDS).map(new Function<Long, ProgressInfo>() {
            @Override
            public ProgressInfo apply(Long value) {
                int compeleteSize = 0;
                for (DownloadInfo info : mDownloadInfos) {
                    compeleteSize += mTaskProgress.get(info.getThreadId());
                }
                if (mProgressInfo == null) {
                    mProgressInfo = new ProgressInfo(compeleteSize, contentLength);
                }
                mProgressInfo.bytesRead = compeleteSize;
                mProgressInfo.contentLength = contentLength;
                final int state = (int) mAtomicLong.get();
                switch (state) {
                    case 0:
                        mProgressInfo.mState = ProgressInfo.State.CONNECT;
                        break;
                    case 1:
                        mProgressInfo.mState = ProgressInfo.State.CONNECT;
                        break;
                    case 2:
                        mProgressInfo.mState = ProgressInfo.State.CONNECT;
                        break;
                    case 3:
                        mProgressInfo.mState = ProgressInfo.State.DOWNLOADING;

                        break;
                    case 4:
                        if (contentLength == compeleteSize) {
                            mProgressInfo.mState = ProgressInfo.State.FINISHED;
                        } else {
                            mProgressInfo.mState = ProgressInfo.State.STOPE;
                        }

                        break;
                }

                mProgressInfo.speed = 0;
                if(mProgressInfo.bytesRead != lastCompletSize){

                    long curMillis = SystemClock.uptimeMillis();
                    mProgressInfo.speed = (mProgressInfo.bytesRead - lastCompletSize) * 1000 / (curMillis - mLastMillis);
                    lastCompletSize = mProgressInfo.bytesRead;
                    mLastMillis = curMillis;
                }
//                Logger.e("map"+mProgressInfo.toString());
                return mProgressInfo;
            }
        }).distinctUntilChanged(new BiPredicate<ProgressInfo, ProgressInfo>() {
            @Override
            public boolean test(@NonNull ProgressInfo progressInfo, @NonNull ProgressInfo progressInfo2) throws Exception {
                if(progressInfo.speed ==0){
                    return true;
                }
                return false;
            }
        }).takeUntil(new Predicate<ProgressInfo>() {
            @Override
            public boolean test(@NonNull ProgressInfo progressInfo) throws Exception {
                if (progressInfo.mState != ProgressInfo.State.CONNECT && progressInfo.mState != ProgressInfo.State.DOWNLOADING) {
                    return true;
                }
                return false;
            }
        });

    }
    long mLastMillis;

    public ProgressInfo getCurProgressInfo() {
        int compeleteSize = 0;
        for (DownloadInfo info : mDownloadInfos) {
            compeleteSize += mTaskProgress.get(info.getThreadId());
        }
        if (mProgressInfo == null) {
            mProgressInfo = new ProgressInfo(compeleteSize, contentLength);
        }
        mProgressInfo.bytesRead = compeleteSize;
        mProgressInfo.contentLength = contentLength;
        mProgressInfo.speed = (mProgressInfo.contentLength - lastCompletSize) * 1000 / DELAY;
        return mProgressInfo;
    }

    public boolean isDownloadFinish() {
        int compeleteSize = 0;
        for (DownloadInfo info : mDownloadInfos) {
            compeleteSize += mTaskProgress.get(info.getThreadId());
        }
        return compeleteSize == contentLength;
    }

}
