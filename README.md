Android Actors Library
=======================

Android Actors Library was inspired by the [Actor model](https://en.wikipedia.org/wiki/Actor_model), but in fact, it has nothing in common with it. The main purpose of this library is to help developers in creating a worker attached to a thread and make all interactions with this worker natural and simple.

## How to use the library

#### Configure your build

Add the following dependencies to your project:

```groovy
compile group: 'com.truecaller', name: 'android-actors-library', version: '1.1.5'
annotationProcessor group:'com.truecaller', name: 'android-actors-generator', version: '1.1.5'
```

#### Specify a package for ActorsBuilder

The Actors Library needs to know which package it should use as a root for the generated classes. To provide this information create a file named **package-info.java**. This file should contain the package name (similar to a regular java file), but annotated with the [@ActorsPackage](actors-library/src/main/java/com/truecaller/androidactors/ActorsPackage.java) annotation.

```java
@ActorsPackage
package your.application.package.name;

import com.truecaller.androidactors.ActorsPackage;
```

You can change the generation rules slightly using the values of this annotation. See the [source code](actors-library/src/main/java/com/truecaller/androidactors/ActorsPackage.java) for details.

#### Create a worker

Define an interface and mark it with the [@ActorInterface](actors-library/src/main/java/com/truecaller/androidactors/ActorInterface.java) annotation

```java
@ActorInterface
interface Storage {
    void save(@NonNull User data);

    @NonNull
    Promise<User> getById(long id);
}
```

##### Limitations

Actor's implementation will work on a separate thread with its own stack, thus the following is not allowed:

- Throw any Exceptions from actor's methods.
- Return values directly from the actor's methods. All return values must be wrapped with a [Promise](actors-library/src/main/java/com/truecaller/androidactors/Promise.java) class.
- Return null from actor's methods. All methods **must** be annotated with a `@NonNull` annotation, otherwise the compilation will fail during code generation.

##### Example implementation

```java
/* package */ class StorageImpl implements Storage {
    public void save(@NonNull User data) {
        // Save user to persistent storage
    }

    @NonNull
    public Promise<User> getById(long id) {
        if (!isValidId(id)) {
            return Promise.wrap(null);
        }
  
        final User data;
        // Fetch user from storage
        return Promise.wrap(data);
    }
}
```

In worker implementation, you should not care about threading. All methods will be executed on a single thread, so:
 - no synchronization issues
 - thread can be blocked as long as needed

#### Create an actor thread

During compilation, the Actors Library generates an `ActorsBuilder` class. It is always placed in the package marked by the `@ActorsPackage` annotation (name and access level can be changed)
You need an instance of this class for managing actors threads.

```java
ActorsBuilder actors = new ActorsBuilder().build();
ActorThread storageThread = actors.createThread("storage");
```

There are several types of threads. For more information, check the [ActorsThreads](actors-library/src/main/java/com/truecaller/androidactors/ActorsThreads.java) interface.

#### Bind your actor implementation to a thread

To use the actor, you need to bind it to a thread:

```java
ActorRef<Storage> storage = storageThread.bind(Storage.class, new StorageImpl());
```

Then, share this `ActorRef` across your application. It will take care of the calls to the actual actor implementation.

It is allowed to bind multiple actors to the same actor thread, but it is not allowed to:

- bind one actor's implementation to several threads
- bind one actor's implementation to the same thread more than once

#### Call methods from ActorRef

Invoking actor methods is as easy as telling the `ActorRef` to execute the required method:

```java
storage.tell().save(user);
```

The actor reference will take care of providing the data to the actual actor implementation on the proper thread.

When the actor's method returns a value, it gets a bit more complex. Before the call executes, you need to explicitly tell what needs be done with the result. There are three possibilities:

##### #1 Forget about the result

```java
storage.tell().getById(100L).thenNothing();
```

##### #2 Run actions on the actor's thread
Not recommended (all work should be done prior to returning a result), but it might be useful in certain cases.

```java
storage.tell().getById(100L).then(new ResultListener<User>() {
    @Override
    public void onResult(@Nullable User result) {
        // Some actions with the returned object or whatever you want
    }
});
```
##### #3 Run actions on another thread
Very useful for UI interaction. The following example updates the UI based on the returned result:

```java
storage.tell().getById(100L).then(actors.ui(), new ResultListener<User>() {
    @Override
    public void onResult(@Nullable User result) {
        if (result == null) {
            showEmptyView();
        } else {
            updateViews(result);
        }
    }
});
```
Using lambdas or method references will make this look even nicer:

```java
class MainActivity extends Activity {

    @NonNull
    private final ActorsThreads mActors;

    @NonNull
    private final ActorRef<Storage> mStorage;

    private long mUserId;

    @Override
    protected void onResume() {
        super.onResume();
        mStorage.tell().getById(mUserId).then(mActors.ui(), this::onUserData);
    }

    private void onUserData(@Nullaable User user) {
        if (user == null) {
            showEmptyView();
        } else {
            updateViews(user);
        }
    }
}
```

#### Action handle

Whenever you provide a result listener, the link to it is stored until the actual method call. This might cause temporal memory leaks, especially when you are doing it from your activity.

To deal with this problem, all calls to `ActorThread.then()` return an instance of the `ActionHandle` interface. It contains only one method - `ActionHandle.forget()`. By calling this method you are telling the library that you are not interested in result anymore, allowing it to forget all links to the listener. When the `forget()` method is called from the result listener's thread, you can be sure that the listener will never be triggered afterward.

```java
class MainActivity extends Activity {
  
    @NonNull
    private final ActorsThreads mActors;
  
    @NonNull
    private final ActorRef<Storage> mStorage;
  
    @Nullable
    private ActionHandle mUserHandle;
  
    private long mUserId;
  
    @Override
    protected void onResume() {
        super.onResume();
        mUserHandle = mStorage.tell().getById(mUserId).then(mActors.ui(), this::onUserData);
    }
  
    @Override
    protected void onPause() {
        super.onPause();
        if (mUserHandle != null) {
            mUserHandle.forget();
            mUserHandle = null;
        }
    }
  
    private void onUserData(@Nullable User user) {
        if (user == null) {
            showEmptyView();
        } else {
            updateViews(user);
        }
        
        mUserHandle = null;
    }
}
```

#### Android service as actor thread

You can wrap an actor thread in an Android Service. It allows you to ensure that all calls will be finished in the background if the user leaves the application.

First, define your service. It **must** be a subclass of ActorService. Then, pass proper parameters to the base constructor:

```java
public class StorageService extends ActorService {
    public StorageService() {
        super("storage-worker", 0, true);
    }
}
```

The base constructor asks for three parameters:

- name - the name of the worker thread
- stopDelay - interval in milliseconds after which the service will stop itself if no new commands are passed. By default, this value is zero, which means that the service stops itself each time the messages queue is empty. This may sometimes cause UI lags, especially if you are doing infrequent calls (allowing the queue to become empty) for updating lists or something similar based on the Activity/Fragment transitions or data updates. For dealing with this problem, just provide a wanted interval.
- useWakeLocks - obtain a partial wakelock to prevent the device from going to sleep while the methods execute. If you set it to true do not forget to ask for the `android.permission.WAKE_LOCK` permission in your manifest. Worker thread's name will be used as the wake lock's name.

Also, do not forget to add service in yor manifest:

```xml
<application>
    <service android:name=".StorageService" />
</application>
```

##### Limitation
Since all parameters to the actor implementation are passed as references, the service can **only** work in the main application process.

#### Useful crashes

In case you get an exception somewhere in the actor's implementation, the library will create a special throwable that contains:
- the place where the exception happened
- the place from where the actor's method was called
- the string representation of all parameters that were passed to the method

```java
Fatal Exception: ActorMethodInvokeException: uncaught exception from your.application.package.name.Storage.getById(100)
       at your.application.package.name.StorageProxy.getById(SourceFile:105)
       at your.application.package.name.MainActivity.onResume(SourceFile:923) <-- the place from where the method was called
       at android.app.Instrumentation.callActivityOnResume(Instrumentation.java:1269)
       at android.app.Activity.performResume(Activity.java:6791)
       at android.app.ActivityThread.performResumeActivity(ActivityThread.java:3477)
       at android.app.ActivityThread.handleResumeActivity(ActivityThread.java:3546)
       at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2795)
       at android.app.ActivityThread.-wrap12(ActivityThread.java)
       at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1527)
       at android.os.Handler.dispatchMessage(Handler.java:110)
       at android.os.Looper.loop(Looper.java:203)
       at android.app.ActivityThread.main(ActivityThread.java:6251)
       at java.lang.reflect.Method.invoke(Method.java)
       at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:1073)
       at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:934)
Caused by java.lang.NullPointerException
       at your.application.package.name.StorageImpl.getById(SourceFile:353) <-- the place where the exception actually occured
       at your.application.package.name.StorageGetByIdMessage.invoke(SourceFile:30)
       at your.application.package.name.StorageGetByIdMessage.invoke(SourceFile:7)
       at com.truecaller.androidactors.ActorService$ActorHandler.handleTransaction(SourceFile:131)
       at com.truecaller.androidactors.ActorService$ActorHandler.handleMessage(SourceFile:118)
       at android.os.Handler.dispatchMessage(Handler.java:110)
       at android.os.Looper.loop(Looper.java:203)
       at android.os.HandlerThread.run(HandlerThread.java:61)
```

## LICENSE

Copyright (C) 2017 True Software Scandinavia AB

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    <http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
