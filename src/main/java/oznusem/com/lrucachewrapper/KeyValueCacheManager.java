package oznusem.com.lrucachewrapper;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;

/**
 * Created by Oz Nusem on 12/1/13.
 */
public class KeyValueCacheManager {

    private static final String TAG = KeyValueCacheManager.class.getSimpleName();

    // Sets the size of the storage that's used to cache
    private static int CACHE_SIZE = Integer.MAX_VALUE;
    private static int KEY_JSON_CACHE_SNAPSHOT = 0;
    private static int numberOfValuesPerEntry = 1;
    private static int appVersionCode = 1;
    private static KeyValueCacheManager instance = null;
    private static String cacheFolderName = "/jsonCaching";
    private DiskLruCache cache;

    public KeyValueCacheManager(Context context) {
        Log.v(TAG,"KeyValueCacheManager - enter");
        init(context);
    }

    //region Singleton
    public KeyValueCacheManager(Context application,String cacheFolderName,int numberOfValuesPerEntry,
                                int appVersionCode) {
        Log.v(TAG,"KeyValueCacheManager - enter");


        KeyValueCacheManager.appVersionCode  = appVersionCode;
        KeyValueCacheManager.numberOfValuesPerEntry = numberOfValuesPerEntry;
        KeyValueCacheManager.cacheFolderName = cacheFolderName;

        init(application);
    }

    private void init(Context context) {
        try {
            setUp(context);
        } catch (Exception e) {
            Log.e(TAG,"KeyValueCacheManager - Failed with exception",e);
        }
    }

    public static KeyValueCacheManager INSTANCE(Context context) {
        if (instance == null) {
            instance = new KeyValueCacheManager(context,cacheFolderName,numberOfValuesPerEntry,appVersionCode);
        }
        return instance;
    }
    //endregion

    public void setUp(Context context) throws IOException {
        Log.e(TAG,"setUp - enter");
        File cacheDir = new File(context.getFilesDir().getPath() + cacheFolderName);
        cache = DiskLruCache.open(cacheDir, appVersionCode, numberOfValuesPerEntry,
                CACHE_SIZE);
    }


    public String getCacheForURL(Context context,String url) {
        if (!ensureCache(context)) {
            return null;
        }
        DiskLruCache.Snapshot snapshot;
        String json  = null;
        try {
            snapshot = cache.get(urlToKey(url));
            if (snapshot != null) {
                json = snapshot.getString(KEY_JSON_CACHE_SNAPSHOT);
            }
        } catch (IOException e) {
            Log.e(TAG, "getCacheForURL, Failed with exception", e);
        }

        if (json != null) {
            Log.d(TAG,"getCacheForURL, url - " + urlToKey(url)+  ", from cache - true");
        } else {
            Log.d(TAG,"getCacheForURL, url - " + urlToKey(url)+  ", from cache - false");
        }

        return json;
    }

    /**
     * @param url is the url request cache
     * @param json the json that being cache
     */

    public synchronized void addCache(String url,String json,Context context) {
        Log.v(TAG,"addCache - enter");
        if (!ensureCache(context)) {
            return;
        }
        try {
            DiskLruCache.Editor creator = cache.edit(urlToKey(url));
            creator.set(KEY_JSON_CACHE_SNAPSHOT, json);
            creator.commit();
        } catch (IOException e) {
            Log.e(TAG, "addCache, Failed with exception for key - " + url, e);
        }
    }

    private boolean ensureCache(Context context) {
        Log.v(TAG,"ensureCache - enter");
        if (cache.isClosed()) {
            try {
                setUp(context);
            } catch (Exception e) {
                Log.e(TAG, "ensureCache, Failed with exception", e);
                return false;
            }
        }
        return true;
    }

    public void clearJsonCache() {
        Log.e(TAG,"clearJsonCache - enter");
        //Clear LRU Cache
        try {
            cache.delete();
        } catch (IOException e) {
            Log.e(TAG, "clearJsonCache - failed to flush cache",e);
        }
    }

    String urlToKey(String url) {
        Log.v(TAG,"urlToKey - " + url);

        url = url.replaceAll("[^a-zA-Z0-9]","");
        if(url.length() > 62) {
            url = TextUtils.substring(url, 0, 62);
        }
        return url.toLowerCase();
    }

}
