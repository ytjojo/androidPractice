package com.ytjojo.http.download.multithread;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一个业务类
 */
public class Dao {
    private static volatile Dao instance = null;
    private Context mContext;

    private Dao(Context mContext) {
        this.mContext = mContext;
    }

    public static void init(Context c) {
        instance = new Dao(c);
    }

    private static volatile SQLiteDatabase sDatabase;
    private AtomicInteger mOpenCounter = new AtomicInteger();

    public static Dao getInstance() {
        if (instance == null) {
            new IllegalArgumentException("init(Context c");
        }
        return instance;
    }

    private SQLiteDatabase getConnection() {
//        if(sDatabase == null){
//            synchronized (Dao.class){
//                if(sDatabase ==null){
//                    try {
//                        sDatabase = new DBHelper(mContext).getWritableDatabase();
//                    } catch (Exception e) {
//                    }
//                }
//            }
//        }

        if (mOpenCounter.incrementAndGet() == 1) {
            if (sDatabase != null && sDatabase.isOpen()) {
            } else {
                // Opening new database
                sDatabase = new DBHelper(mContext).getWritableDatabase();
            }

        }
        return sDatabase;
    }

    public synchronized void closeDatabase() {

        if (mOpenCounter.get() == 0) {
            // Closing database
            sDatabase.close();
            sDatabase = null;
        }
    }

    /**
     * 查看数据库中是否有数据
     */
    public synchronized boolean isHasInfors(String url) {
        SQLiteDatabase database = getConnection();
        int count = -1;
        Cursor cursor = null;
        try {
            String sql = "select count(*)  from download_info where url=?";
            cursor = database.rawQuery(sql, new String[]{url});
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOpenCounter.decrementAndGet();
            if (null != cursor) {
                cursor.close();
            }
        }
        return count == 0;
    }

    /**
     * 保存 下载的具体信息
     */
    public synchronized void saveInfos(List<DownloadInfo> infos) {
        SQLiteDatabase database = getConnection();
        database.beginTransaction();
        try {
            for (DownloadInfo info : infos) {
                String sql = "insert into download_info(thread_id,start_pos, end_pos,compelete_size,url) values (?,?,?,?,?)";
                Object[] bindArgs = {info.getThreadId(), info.getStartPos(),
                        info.getEndPos(), info.getCompeleteSize(),
                        info.getUrl()};
                database.execSQL(sql, bindArgs);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
            mOpenCounter.decrementAndGet();
        }
    }

    /**
     * 得到下载具体信息
     */
    public synchronized List<DownloadInfo> getInfos(String url) {
        List<DownloadInfo> list = new ArrayList<DownloadInfo>();
        SQLiteDatabase database = getConnection();
        Cursor cursor = null;
        try {
            String sql = "select thread_id, start_pos, end_pos,compelete_size,url from download_info where url=?";
            cursor = database.rawQuery(sql, new String[]{url});
            while (cursor.moveToNext()) {
                DownloadInfo info = new DownloadInfo(cursor.getInt(0),
                        cursor.getLong(1), cursor.getLong(2), cursor.getLong(3),
                        cursor.getString(4), true);
                list.add(info);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOpenCounter.decrementAndGet();
            if (null != cursor) {
                cursor.close();
            }
        }
        return list;
    }

    /**
     * 更新数据库中的下载信息
     */
    public void updataInfos(int threadId, long compeleteSize, String urlstr) {
        SQLiteDatabase database = getConnection();
        try {
            String sql = "update download_info set compelete_size=? where thread_id=? and url=?";
            Object[] bindArgs = {compeleteSize, threadId, urlstr};
            database.execSQL(sql, bindArgs);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOpenCounter.decrementAndGet();
        }
    }

    /**
     * 下载完成后删除数据库中的数据
     */
    public synchronized void delete(String url) {
        SQLiteDatabase database = getConnection();
        try {
            database.delete("download_info", "url=?", new String[]{url});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mOpenCounter.decrementAndGet();
        }
    }
}  