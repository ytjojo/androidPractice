package com.ytjojo.http.download.multithread;

import com.orhanobut.logger.Logger;
import com.ytjojo.http.subscriber.RetryWhenNetworkException;
import com.ytjojo.http.util.CollectionUtils;
import com.ytjojo.http.util.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static io.reactivex.Observable.create;
import static io.reactivex.plugins.RxJavaPlugins.onError;

/**
 * Created by Administrator on 2016/11/12 0012.
 */
public class Manager {

    public final int MAX_THREAD_COUNT = 3;
    public final int MIN_BLOCK_PERTASK = 1024 * 1024;
    final String mAbsDir;
    final String mExpectName;
    String mFileName;
    final String mRemoteUrl;
    long mContentLength;
    CountDownLatch mCountDownLatch;
    ProgressHandler mProgressHandler;


    public Manager(String absDir, String expectName, String url) {
        this.mAbsDir = absDir;
        this.mExpectName = expectName;
        this.mRemoteUrl = url;
        mProgressHandler = new ProgressHandler(this);
    }

    public void reStart(Observer<ProgressInfo> observer) {
        if (!isDonwLoading()) {
            subscribe(this, observer);

        }
    }

    private void reportResult() {

        ProgressInfo info = mProgressHandler.getCurProgressInfo();
        mProgresDisposable.dispose();
        boolean isFinish = mProgressHandler.isDownloadFinish();
        info.mState = isFinish ? ProgressInfo.State.FINISHED : ProgressInfo.State.STOPE;
        Throwable throwable = null;
        for(DownloadTask task:mArrayListTasks){
            throwable = task.mLastError;
            if(throwable !=null){
                break;
            }
        }
        if(throwable != null){
            mProgressHandler.mAtomicLong.set(ProgressHandler.ERROR);
            mProgressEmitter.onError(new DownLoadException("子线程下载任务出错",throwable));
            return;
        }else if (!isFinish) {
            mProgressHandler.mAtomicLong.set(ProgressHandler.STOPED);
        }else {
            mProgressHandler.mAtomicLong.set(ProgressHandler.FINISH);
        }
        mProgressEmitter.onNext(info);
        mProgressEmitter.onComplete();
    }

    public synchronized void stop() {
        long state = mProgressHandler.mAtomicLong.get();
        if(state==  ProgressHandler.STARTED||state==  ProgressHandler.DOWNLOAD){
            mProgressHandler.mAtomicLong.set(ProgressHandler.STOPING);

        }
    }

    private void onCancel() {
        if (mProgresDisposable != null && !mProgresDisposable.isDisposed()) {
            mProgresDisposable.dispose();
        }
    }

    public synchronized boolean isConnecting() {
        return mProgressHandler.mAtomicLong.get() == ProgressHandler.STARTED;

    }

    public synchronized boolean isDonwLoading() {
        long state = mProgressHandler.mAtomicLong.get();
        if (state == ProgressHandler.STARTED&& state == ProgressHandler.DOWNLOAD) {
            return true;
        }
        return false;

    }

    public synchronized boolean isStarted() {
        return mProgressHandler.mAtomicLong.get() > ProgressHandler.IDLE;

    }

    public void createFile(File file, long contentLength) throws IOException {
        if (!file.exists()) {
            File dir = file.getParentFile();
            if (dir.exists() || dir.mkdirs()) {
            }
        } else {
            file.delete();
        }
        Logger.e("file" + file.getAbsolutePath());
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.setLength(contentLength);
        raf.close();
    }

    public boolean checkFileFinish(File file, long contentLength) throws IOException {
        if (!file.exists()) {
            return false;
        } else {
            boolean isFinish = false;
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            if (raf.length() == contentLength) {
                isFinish = true;
            }
            raf.close();
            return isFinish;
        }
    }

    public boolean isValideFile(File file, long expextLength) {
        if (file.exists() && file.isFile()) {
            RandomAccessFile randomAccessFile = null;
            try {
                randomAccessFile = new RandomAccessFile(file, "rw");
                long fileLength = randomAccessFile.length();
                return expextLength == fileLength;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                okhttp3.internal.Util.closeQuietly(randomAccessFile);
            }
        }
        return false;
    }

    private void reset() {
        mContentLength = 0;
        mCountDownLatch = null;
        if (mProgressHandler != null) {
            mProgressHandler.mProgressInfo = null;
            if (mProgressHandler.mTaskProgress != null) {
                mProgressHandler.mTaskProgress.clear();
                mProgressHandler.mTaskProgress = null;
            }

        }
    }
    public void onStart(){
        mProgressHandler.mAtomicLong.set(ProgressHandler.STARTED);
    }
    public void excute() {
        try {
            call();
        } catch (IOException e) {
            onError(e);
        }
    }

    Disposable mProgresDisposable;

    private File call() throws IOException {
        reset();
        if(mProgressHandler.mAtomicLong.get()==ProgressHandler.STOPING){
            mProgressHandler.mAtomicLong.set(ProgressHandler.STOPED);
            mProgressEmitter.onNext(new ProgressInfo(0,0,ProgressInfo.State.STOPE));
            mProgressEmitter.onComplete();
            return null;
        }
        mProgressEmitter.onNext(new ProgressInfo(0, 0, ProgressInfo.State.CONNECT));
        Response response = getOkHttpClient().newCall(getRequest()).execute();
        ResponseBody responseBody = response.body();

        if(!response.isSuccessful()){
            mProgressHandler.mAtomicLong.set(ProgressHandler.ERROR);
            mProgressEmitter.onError(new DownLoadException(response.code(),"网络请求错误"+ response.code()));
            return null;
        }
        MediaType mediaType = responseBody.contentType();
        String type = mediaType.type();
        final long contentLength = responseBody.contentLength();
        mContentLength = contentLength;
        mProgressHandler.setContentLength(mContentLength);
        if (TextUtils.isEmpty(mExpectName)) {
            mFileName = getFileName(mRemoteUrl, response);
        } else {
            mFileName = mExpectName;
        }
        try{
            List<DownloadInfo> downloadInfos = getDownloadInfosFromDb(mRemoteUrl);
            if (downloadInfos == null) {
                downloadInfos = new ArrayList<>();
            }
            if (CollectionUtils.isEmpty(downloadInfos)) {
                boolean isSupportMultThread = isSurpportMultiThread(response);
                if (contentLength <= MIN_BLOCK_PERTASK * 1.5 || !isSupportMultThread) {
                    DownloadInfo downloadInfo = new DownloadInfo(0,mContentLength,
                            mContentLength, 0, mRemoteUrl,isSupportMultThread);
                    downloadInfo.isLastOne = true;
                    downloadInfos.add(downloadInfo);
                }else {
                    prepareNew(downloadInfos);
                }
            } else {
                prepareWithHistory(downloadInfos);
            }
            if(mProgressHandler.mAtomicLong.get()==ProgressHandler.STOPING){
                mProgressHandler.mAtomicLong.set(ProgressHandler.STOPED);
                mProgressEmitter.onNext(new ProgressInfo(0,0,ProgressInfo.State.STOPE));
                mProgressEmitter.onComplete();
                return null;
            }
            mProgressHandler.setTaskInfos(downloadInfos);
            dispatchExcuteTask(downloadInfos);
            mProgresDisposable = mProgressHandler.getProgress().subscribe(new Consumer<ProgressInfo>() {
                @Override
                public void accept(ProgressInfo progressInfo) {
                    if (!mProgressEmitter.isDisposed()) {
                        Manager.this.mProgressEmitter.onNext(progressInfo);
                    }
//                Logger.e("subscribe" + progressInfo.toString());
                }
            });
            mProgressHandler.mAtomicLong.set(mProgressHandler.DOWNLOAD);

            if (mCountDownLatch != null){
                mCountDownLatch.await();
            }

            if (checkFileFinish(new File(mAbsDir, mFileName + S_FILECACHE_NAME), mContentLength)) {
                Dao.getInstance().delete(mRemoteUrl);
                rename();
            }
            reportResult();
            Logger.e(mAbsDir + "  content " + contentLength);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            Dao.getInstance().closeDatabase();
        }
        return new File(mAbsDir, mFileName);
    }

    private void prepareWithHistory(List<DownloadInfo> downloadInfos) {
        long expectLength = 0;
        DownloadInfo theLastOne = null;
        for (DownloadInfo info : downloadInfos) {
            expectLength += info.getCompeleteSize();
            if (info.getCompeleteSize() == (info.getEndPos() - info.getStartPos() + 1)) {
                info.isFinished = true;
            } else {
                info.isFinished = false;
            }
            if (theLastOne == null) {
                theLastOne = info;
            } else {
                if (info.getEndPos() > theLastOne.getEndPos()) {
                    theLastOne = info;
                }
            }
        }
        theLastOne.isLastOne = true;
        File targetFile = new File(mAbsDir, mFileName);
        File cacheFile = new File(mAbsDir, mFileName + S_FILECACHE_NAME);
        if (!isValideFile(cacheFile, expectLength)) {
            Dao.getInstance().delete(mRemoteUrl);
            deleteFile(targetFile);
            deleteFile(cacheFile);
            prepareNew(downloadInfos);
        }
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    private void prepareNew(List<DownloadInfo> downloadInfos) {
        if (CollectionUtils.isEmpty(downloadInfos)) {
            long count = mContentLength / MIN_BLOCK_PERTASK;
            if (count > MAX_THREAD_COUNT) {
                count = MAX_THREAD_COUNT;
            }
            long perBlock = mContentLength / count;
            if (count < MAX_THREAD_COUNT) {
                long emainderBlock = mContentLength % perBlock;
                if (emainderBlock >= ((float) MIN_BLOCK_PERTASK) * 0.5f) {
                    count++;
                }
            }
            perBlock = mContentLength / count;
            for (int i = 0; i < count; i++) {
                long endPos = 0;
                if (i != count - 1) {
                    endPos = (i + 1) * perBlock - 1;
                } else {
                    endPos = (mContentLength - 1);
                }

                DownloadInfo downloadInfo = new DownloadInfo(i, i * perBlock, endPos, 0, mRemoteUrl,true);
                if (i == count - 1) {
                    downloadInfo.isLastOne = true;
                }
                downloadInfos.add(downloadInfo);
            }
            Dao.getInstance().saveInfos(downloadInfos);
            try {
                createFile(new File(mAbsDir, mFileName + S_FILECACHE_NAME), mContentLength);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    ArrayList<DownloadTask> mArrayListTasks;
    private void dispatchExcuteTask(List<DownloadInfo> infos) {
        if(mArrayListTasks == null){
            mArrayListTasks = new ArrayList<>();
        }
        mArrayListTasks.clear();
        int taskCount = 0;
        for (DownloadInfo info : infos) {
            mProgressHandler.setProgress(info);
            if (!info.isFinished) {
                taskCount++;
                DownloadTask task = new DownloadTask(new File(mAbsDir, mFileName + S_FILECACHE_NAME), mProgressHandler, info);
                mArrayListTasks.add(task);

            }
        }
        mCountDownLatch = new CountDownLatch(taskCount);
        for (DownloadTask task : mArrayListTasks) {
            excuteTask(task, mCountDownLatch);
        }
    }

    public final static String S_FILECACHE_NAME = ".cache";

    private void rename() {
        //更新文件
        File file = new File(mAbsDir, mFileName + S_FILECACHE_NAME);
        file.renameTo(new File(mAbsDir, mFileName));
    }

    public boolean isSurpportMultiThread(Response originalResponse) {
        String bytes = originalResponse.header("Accept-Ranges");
        String contentRange = originalResponse.header("Content-Range");
        if ("bytes".equals(bytes) || (contentRange != null && contentRange.startsWith("bytes"))) {
            return true;
        }
        return false;
    }


    public List<DownloadInfo> getDownloadInfosFromDb(String url) {
        return Dao.getInstance().getInfos(url);
    }

    public String getFileName(String url, Response response) {
        String filename = url.substring(url.lastIndexOf('/') + 1);
        if (filename == null || "".equals(filename.trim())) {//如果获取不到文件名称
            String mine = response.header("Content-Disposition");
            if (!TextUtils.isEmpty(mine)) {
                Matcher m = Pattern.compile(".*filename=(.*)").matcher(mine.toLowerCase());
                if (m.find()) return m.group(1);
            }
            filename = UUID.randomUUID() + ".tmp";//默认取一个文件名
            return filename;
        } else {
            return filename;
        }
    }

    /**
     * get file suffix by file path
     *
     * @param filePath file path
     * @return file suffix,return null means failed
     */
    public static String getFileSuffix(String filePath) {
        if (!TextUtils.isEmpty(filePath)) {
            int start = filePath.lastIndexOf(".");
            if (start != -1) {
                return filePath.substring(start + 1);
            }
        }
        return null;
    }
    private String getRedirectUrl(Response response) throws Exception {
        return response.header("Location");
    }

    public void excuteTask(DownloadTask task, CountDownLatch countDownLatch) {
        create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) {
                task.execute(getOkHttpClient(), getRequest());
                if (!e.isDisposed()) {
                    e.onNext(task.isFinish());
                    e.onComplete();
                }
            }
        }).subscribeOn(Schedulers.io()).retryWhen(new RetryWhenNetworkException())
                .subscribe(new Observer<Boolean>() {

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        countDownLatch.countDown();
                        Logger.e(e.toString() + "发生错误");
                        task.mLastError = e;
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean finish) {
                        Logger.e("onNext  finish"+ finish);
                        countDownLatch.countDown();
                    }
                });
    }

    ObservableEmitter<ProgressInfo> mProgressEmitter;

    public static void subscribe(Manager manager, Observer<ProgressInfo> subscriber) {
        if(manager.isDonwLoading()){
            return;
        }
       Observable<ProgressInfo> observable =  Observable.create(new ObservableOnSubscribe<ProgressInfo>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<ProgressInfo> e) throws Exception {
                manager.mProgressEmitter = e;
                manager.excute();
                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        manager.onCancel();
                        manager.mProgressEmitter = null;
                    }
                });
            }
        }).subscribeOn(Schedulers.io())
                .retryWhen(new RetryWhenNetworkException())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(subscriber);
        manager.onStart();



    }

    public Request getRequest() {
        Request.Builder builder = new Request.Builder();
        builder.url(mRemoteUrl);
        return builder.build();
    }

    static OkHttpClient sOkHttpClient;

    public static OkHttpClient getOkHttpClient() {
        if (sOkHttpClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            sOkHttpClient = builder.connectTimeout(30 * 1000, TimeUnit.MILLISECONDS)
                    .readTimeout(20 * 1000, TimeUnit.MILLISECONDS).writeTimeout(20 * 1000, TimeUnit.MILLISECONDS).build();
        }
        return sOkHttpClient;

    }
}
