/*
 * Copyright (C) 2017 True Software Scandinavia AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.truecaller.actorssampleapplication;

import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.truecaller.androidactors.ActorRef;
import com.truecaller.androidactors.ActorThread;
import com.truecaller.androidactors.ActorsThreads;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

@Module
public class AppModule {

    private static final String THREAD_NETWORK = "network";
    private static final String THREAD_DB = "db";

    private final Context mContext;

    public AppModule(Context context) {
        mContext = context;
    }

    @Provides
    @NonNull
    /* package */ Context provideContext() {
        return mContext;
    }

    @Provides
    @NonNull
    /* package */ HttpLoggingInterceptor.Logger provideLogger() {
        return s -> Log.i("NETWORK", s);
    }

    @Provides
    @NonNull
    /* package */ OkHttpClient provideClient(@NonNull HttpLoggingInterceptor.Logger logger) {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor(logger).setLevel(HttpLoggingInterceptor.Level.BODY))
                .build();
    }

    @Provides
    @NonNull
    /* package */ FeedParser provideFeedParser() {
        return new FeedParser();
    }

    @Provides
    @NonNull
    /* package */ NetworkManager provideNetworkManager(@NonNull OkHttpClient httpClient, @NonNull FeedParser feedParser) {
        return new NetworkManagerImpl(httpClient, feedParser);
    }

    @Singleton
    @Provides
    @NonNull
    /* package */ ActorsThreads provideActors() {
        return new ActorsBuilder().build();
    }

    @Singleton
    @Provides
    @Named(THREAD_NETWORK)
    @NonNull
    /* package */ ActorThread provideNetworkThread(@NonNull Context context, @NonNull ActorsThreads actors) {
        return actors.createThread(context, NetworkService.class);
    }

    @Singleton
    @NonNull
    @Provides
     /*package*/ ActorRef<NetworkManager> provideNetworkManagerRef(@NonNull @Named(THREAD_NETWORK) ActorThread thread,
                                                                   @NonNull NetworkManager manager) {
        return thread.bind(NetworkManager.class, manager);
    }

    @NonNull
    @Singleton
    @Provides
    /* package */ SQLiteOpenHelper provideOpenHelper(@NonNull Context context) {
        return new FeedOpenHelper(context);
    }

    @NonNull
    @Provides
    /* package */ FeedStorage provideFeedStorage(@NonNull Context context, @NonNull SQLiteOpenHelper helper) {
        return new FeedStorageImpl(context.getContentResolver(), helper);
    }

    @Singleton
    @Provides
    @Named(THREAD_DB)
    @NonNull
    /* package */ ActorThread databaseThread(@NonNull ActorsThreads actors) {
        return actors.createThread(THREAD_DB);
    }

    @Singleton
    @NonNull
    @Provides
    /* package */ ActorRef<FeedStorage> provideFeedStorageRef(@NonNull @Named(THREAD_DB) ActorThread thread,
                                                             @NonNull FeedStorage storage) {
        return thread.bind(FeedStorage.class, storage);
    }
}
