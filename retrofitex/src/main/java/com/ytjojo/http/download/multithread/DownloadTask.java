package com.ytjojo.http.download.multithread;

import com.orhanobut.logger.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by Administrator on 2016/11/12 0012.
 */
public class DownloadTask {
    public static final int IDLE = 0;
    public static final int STARTED = 1;
    public static final int DOWNLOAD = 2;
    public static final int STOPING = 3;
    public static final int STOPED = 4;
    public static final int FINISH = 5;
    public static final int ERROR = 6;

    private DownloadInfo mDownloadInfo;
    private File mFile;
    final long startPos;
    final long endPos;
    long compeleteSize;
    private ProgressHandler mProgressHandler;
    private long contentLength;
    final public AtomicInteger mAtomicState;

    private long needDownloadLength;

    public DownloadTask(File file, ProgressHandler handler, DownloadInfo downloadInfo) {
        this.mFile = file;
        this.mProgressHandler = handler;
        this.mDownloadInfo = downloadInfo;
        this.startPos = mDownloadInfo.getStartPos();
        this.endPos = mDownloadInfo.getEndPos();
        this.compeleteSize = mDownloadInfo.getCompeleteSize();
        this.mAtomicState = new AtomicInteger(IDLE);
        this.needDownloadLength = downloadInfo.getEndPos() - (startPos + compeleteSize) + 1;
    }
    public Throwable mLastError;

    public void execute(OkHttpClient client, Request request) {
        mAtomicState.set(STARTED);
        mLastError = null;
        Request rangeRequest = null;
        if(mDownloadInfo.isLastOne){
            rangeRequest = request.newBuilder().header("Range", "bytes=" + (startPos + compeleteSize) + "-" ).build();
        }else {
            rangeRequest = request.newBuilder().header("Range", "bytes=" + (startPos + compeleteSize) + "-" + endPos).build();
        }
        RandomAccessFile raf = null;
        BufferedInputStream bis = null;
        ResponseBody responseBody = null;
        FileChannel channelOut = null;
        if (mProgressHandler.mAtomicLong.get() == ProgressHandler.STOPING) {
            mAtomicState.set(STOPED);
            return;
        }
        try {
            Response response = client.newCall(rangeRequest).execute();
            if (!response.isSuccessful()) {
                mAtomicState.set(ERROR);
                throw new DownLoadException(response.code(),"message = "+ response.message());
            }
            if (mProgressHandler.mAtomicLong.get() == ProgressHandler.STOPING) {
                mAtomicState.set(STOPED);
                return;
            }
            responseBody = response.body();
            contentLength = responseBody.contentLength();
            //Logger.e("contentLength = "+contentLength + "start"+startPos + " complete" + compeleteSize + "range" + (startPos + compeleteSize) + " start  end"+ endPos);
            File cacheFile = new File(mFile.getAbsolutePath());
            raf = new RandomAccessFile(cacheFile, "rwd");
            bis = new BufferedInputStream(responseBody.byteStream());
            channelOut = raf.getChannel();
            MappedByteBuffer mappedBuffer = channelOut.map(FileChannel.MapMode.READ_WRITE, startPos + compeleteSize, contentLength);
            raf.seek(startPos + compeleteSize);
            mAtomicState.set(DOWNLOAD);
            int bytesRead = -1;
            byte[] buff = new byte[4096];
            while ((bytesRead = bis.read(buff, 0, buff.length)) != -1) {
                mappedBuffer.put(buff, 0, bytesRead);//效率高
                mappedBuffer.force();
//                raf.write(buff, 0, bytesRead);//
                this.compeleteSize = this.compeleteSize + bytesRead;
                this.mDownloadInfo.setCompeleteSize(compeleteSize);
                this.mProgressHandler.setProgress(mDownloadInfo);
                Logger.e(mDownloadInfo.getThreadId()+ "  "+compeleteSize + " contentLength=" + contentLength);
                if(mDownloadInfo.needSaveToDb){
                    Dao.getInstance().updataInfos(mDownloadInfo.getThreadId(), compeleteSize, mDownloadInfo.getUrl());
                }
                if (mProgressHandler.mAtomicLong.get() == ProgressHandler.STOPING) {
                    mAtomicState.set(STOPED);
                    break;
                }
            }
            boolean isFinish = needDownloadLength == contentLength;
            mAtomicState.set(FINISH);
//            Logger.e(mDownloadInfo.getThreadId() + " id  " + contentLength + "完成 start" + mDownloadInfo.getStartPos() + "endpos =" + mDownloadInfo.getEndPos() + "comlete=" + mDownloadInfo.getCompeleteSize());

//            if(!forceStop&& mDownloadInfo.getStartPos() + compeleteSize != mDownloadInfo.getEndPos()+1){
//                throw new DownLoadException("保存文件出错");
//            }

        } catch (FileNotFoundException e) {
            mAtomicState.set(ERROR);
            throw new DownLoadException("找不到文件", e);
        } catch (IOException e) {
            mAtomicState.set(ERROR);
            throw new DownLoadException("下载任务出错",e);
        } finally {
            okhttp3.internal.Util.closeQuietly(raf);
            okhttp3.internal.Util.closeQuietly(bis);
            okhttp3.internal.Util.closeQuietly(channelOut);
            okhttp3.internal.Util.closeQuietly(responseBody);
        }
    }
    public boolean isFinish() {
        return compeleteSize == contentLength;
    }

    public void setHeaders(Request.Builder builder) {
        builder.header("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.3) Gecko/2008092510 Ubuntu/8.04 (hardy) Firefox/3.0.3");
        builder.header("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
        builder.header("Accept-Encoding", "utf-8");
        builder.header("connnection", "keep-alive");

//        builder.header("Accept-Encoding","identity");//禁止Gzip压缩
        /**
         * HTTP Header中Accept-Encoding 是浏览器发给服务器,声明浏览器支持的编码类型[1]
         常见的有
         Accept-Encoding: compress, gzip //支持compress 和gzip类型
         Accept-Encoding:　//默认是identity
         Accept-Encoding: *　//支持所有类型 Accept-Encoding: compress;q=0.5, gzip;q=1.0//按顺序支持 gzip , compress
         Accept-Encoding: gzip;q=1.0, identity; q=0.5, *;q=0 // 按顺序支持 gzip , identity
         服务器返回的对应的类型编码header是 content-encoding.服务器处理accept-encoding的规则如下所示　1. 如果服务器可以返回定义在Accept-Encoding 中的任何一种Encoding类型, 那么处理成功(除非q的值等于0, 等于0代表不可接受)　
         2. * 代表任意一种Encoding类型 (除了在Accept-Encoding中显示定义的类型)　
         3.如果有多个Encoding同时匹配, 按照q值顺序排列　
         4. identity总是可被接受的encoding类型(除非显示的标记这个类型q=0) ,
         如果Accept-Encoding的值是空, 那么只有identity是会被接受的类型
         如果Accept-Encoding中的所有类型服务器都没发返回, 那么应该返回406错误给客户Duan
         如果request中没有Accept-Encoding 那么服务器会假设所有的Encoding都是可以被接受的。
         如果Accept-Encoding中有identity 那么应该优先返回identity (除非有q值的定义,或者你认为另外一种类型是更有意义的)
         注意:
         如果服务器不支持identity 并且浏览器没有发送Accept-Encoding,那么服务器应该倾向于使用HTTP1.0中的 "gzip" and "compress" , 服务器可能按照客户Duan类型 发送更适合的encoding类型大部分HTTP1.0的客户Duan无法处理q值
         */

    }
}
