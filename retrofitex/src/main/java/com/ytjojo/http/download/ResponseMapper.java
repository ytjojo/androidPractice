package com.ytjojo.http.download;

import android.os.SystemClock;

import com.ytjojo.http.download.multithread.Manager;
import com.ytjojo.http.download.multithread.ProgressInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;

/**
 * Created by Administrator on 2016/11/11 0011.
 */
public class ResponseMapper  implements Function<ResponseBody,File> {
    final String mAbsDir;
    final String mFileName;
    ObservableEmitter<ProgressInfo> mGenerator ;
    public void subscribe(Observer<ProgressInfo> subscriber){


        Observable.defer(new Callable<ObservableSource<ProgressInfo>>() {
            @Override
            public ObservableSource<ProgressInfo> call() throws Exception {
                return Observable.create(new ObservableOnSubscribe<ProgressInfo>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<ProgressInfo> e) throws Exception {
                        mGenerator = e;
                        e.setCancellable(new Cancellable() {
                            @Override
                            public void cancel() throws Exception {
                                mGenerator = null;
                            }
                        });
                    }
                });
            }
        }).sample(30, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe(subscriber);
    }

    public ResponseMapper(String absDir,String  fileName){
       this.mAbsDir = absDir;
        this.mFileName = fileName;
    }
    private long offset = 0;
    ProgressInfo mProgressInfo ;
    long contentLength;
    long lastTime;
    @Override
    public File apply(ResponseBody responseBody) {
        RandomAccessFile raf = null;
        contentLength = responseBody.contentLength();
        mProgressInfo = new ProgressInfo(0,contentLength, ProgressInfo.State.DOWNLOADING);
        if(mGenerator !=null){
            mGenerator.onNext(mProgressInfo);
        }
        lastTime = SystemClock.uptimeMillis();
        File target = null;
        try {
            raf = new RandomAccessFile(new File(mAbsDir,mFileName+ Manager.S_FILECACHE_NAME), "rw");
            BufferedInputStream bis = new BufferedInputStream(responseBody.byteStream());
            int bytesRead;
            byte[] buff = new byte[4096];
            while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
                raf.seek(this.offset);
                raf.write(buff, 0, bytesRead);
                this.offset = this.offset + bytesRead;

                if(SystemClock.uptimeMillis() >30 +lastTime){
                    mProgressInfo.bytesRead = offset;
                    mProgressInfo.contentLength = contentLength;
                    mProgressInfo.mState = ProgressInfo.State.DOWNLOADING;
                    if(mGenerator !=null){
                        mGenerator.onNext(mProgressInfo);
                    }
                }
                File file = new File(mAbsDir,mFileName +Manager.S_FILECACHE_NAME);
                target = new File(mAbsDir,mFileName);
                file.renameTo(target);

            }
            raf.close();
            bis.close();
            responseBody.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
        }

//        return new File(mFile.getAbsolutePath(),mFile.getName());
        return target;
    }
}
