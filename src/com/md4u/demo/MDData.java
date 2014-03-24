package com.md4u.demo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import com.md4u.demo.MDModel.Bonus;
import com.md4u.demo.MDModel.Bonus.Record;
import com.md4u.demo.MDModel.Fan;
import com.md4u.demo.MDModel.Post;
import com.md4u.demo.MDModel.Post.Filter;
import com.md4u.demo.MDModel.Response;
import com.md4u.demo.MDService.MDServiceHandler;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class MDData extends IntentService {
    private ServiceBinder serviceBinder;

    public MDData() {
        super("Data Service");
        serviceBinder = new ServiceBinder();
    }

    // constants for the timer
    private static final int UPDATE_START_TIME = 0;
    private static final int UPDATE_INTERVAL = 1000 * 60 * 30; // 30 min

    @Override
    protected void onHandleIntent(Intent intent) {
        // use timer to update data periodically
        Timer updateTimer = new Timer();
        updateTimer.schedule(
            new TimerTask() {
                @Override
                public void run() {
                    loadPostList(null);
                    loadBonusHistory();
                    loadFansList();
                }
            },
            UPDATE_START_TIME, // start immediately
            UPDATE_INTERVAL // interval 30 min
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    // store data as class variable for now
    private static EnumMap<Filter, ArrayList<Post>> postLists = null;
    private static EnumMap<Filter, Integer> postListsCapacity = null;
    private static EnumMap<Filter, Semaphore> postUpdatePermits = null;
    private static Bonus bonusInfo = null;
    private static ArrayList<Record> bonusHistory = null;
    private static ArrayList<Fan> fansList = null;

    // data handler
    public interface MDDataHandler {
        public void onDataRecieved(Object... data);
    }

    // binder class to provide service interface to activities
    public class ServiceBinder extends Binder {
        public void getPostList(Filter filter, final MDDataHandler handler) {
            loadPostList(filter);
            handler.onDataRecieved(postLists.get(filter));
        }

        public void getBonusHistory(final MDDataHandler handler) {
            loadBonusHistory();
            handler.onDataRecieved(bonusInfo, bonusHistory);
        }

        public void getFansList(final MDDataHandler handler) {
            loadFansList();
            handler.onDataRecieved(fansList);
        }

        public MDData getService() {
            return MDData.this;
        }
    }

    // load decisions when start service from main activity
    private void loadPostList(Filter filter) {
        if (filter == null) {
            Filter[] filters = Filter.values();
            for (int i = 0; i < filters.length; ++i) {
                loadPostList(filters[i]);
            }
            return;
        }
        // TODO check local file system

        // TODO check the time stamp of local data

        // if not then update list
        updatePostList(filter);
    }

    // load bonus history when start service from main activity
    private void loadBonusHistory() {
        // TODO check local file system

        // TODO check the time stamp of local data

        // if not then update list
        updateBonusHistory();
    }

    // load fans list when start service from main activity
    private void loadFansList() {
        // TODO check local file system

        // TODO check the time stamp of local data

        // if not then update list
        updateFansList();
    }

    // fetch post from server
    public void updatePostList(final Filter filter) {
        if (postUpdatePermits == null) {
            postUpdatePermits = new EnumMap<Filter, Semaphore>(Filter.class);
        }
        if (!postUpdatePermits.containsKey(filter)) {
            postUpdatePermits.put(filter, new Semaphore(1));
        }
        if (!postUpdatePermits.get(filter).tryAcquire()) {
            return;
        }

        if (postLists == null) {
            postLists = new EnumMap<Filter, ArrayList<Post>>(Filter.class);
            postListsCapacity = new EnumMap<Filter, Integer>(Filter.class);
        }
        if (!postLists.containsKey(filter)) {
            postLists.put(filter, new ArrayList<Post>());
            postListsCapacity.put(filter, 20);
        }

        final ArrayList<Post> postList = postLists.get(filter);
        final int capacity = postListsCapacity.get(filter);
        MDService.MDServiceInstance.pullPost(
            postList.isEmpty() ? null : postList.get(postList.size() - 1),
            filter,
            new MDServiceHandler() {
                @Override
                public void onSuccess(Response response) {
                    if (response.code != 0) {
                        //TODO Generate error message
                    } else {
                        ArrayList<Post> morePosts = Post.toArrayList(response);
                        if (!morePosts.isEmpty()) {
                            postList.addAll(morePosts);
                            if (postList.size() < capacity) {
                                postUpdatePermits.get(filter).release();
                                updatePostList(filter);
                                return;
                            } else {
                                postListsCapacity.put(filter, capacity + 10);
                            }
                        }
                        // TODO Write to file
                    }
                    postUpdatePermits.get(filter).release();
                }
            }
        );
    }

    // fetch latest fans list from server
    private void updateFansList() {
        MDService.MDServiceInstance.loadFansList(
            new MDServiceHandler() {
                @Override
                public void onSuccess(Response response) {
                    if (response.code != 0) {
                        // TODO Generate error message
                    } else {
                        fansList = Fan.toArrayList(response);
                        // TODO Write to file
                    }
                }
            }
        );
    }

    // fetch latest bonus history from server
    private void updateBonusHistory() {
        MDService.MDServiceInstance.loadBonusHistory(
            new MDServiceHandler() {
                public void onSuccess(Response response) {
                    if (response.code != 0) {
                        //TODO Generate error message
                    } else {
                        bonusInfo = new Bonus(response);
                        bonusHistory = Record.toArrayList(response);
                        // TODO Write to file
                    }
                }
            }
        );
    }

    // TODO Write object to file system
    // private void saveObject(Object object, String fileName) {
    // }

    // TODO read object from file system
    // private Object loadObject(String fileName) {
    //     return null;
    // }
}
