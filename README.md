## 自定义handler

Handler：用来发送消息：sendMessage等多个方法，并实现handleMessage()方法处理回调（还可以使用Message或Handler的Callback进行回调处理，具体可以看看源码）。

Message：消息实体，发送的消息即为Message类型。

MessageQueue：消息队列，用于存储消息。发送消息时，消息入队列，然后Looper会从这个MessageQueen取出消息进行处理。

Looper：与线程绑定，不仅仅局限于主线程，绑定的线程用来处理消息。loop()方法是一个死循环，一直从MessageQueen里取出消息进行处理。

Looper.loop()一直无限循环为什么不会造成ANR 因为所有的事件都是由looper去驱动的。

一般线程创建时没有自己的消息列队，消息处理时就在主线程中完成，如果线程中使用Looper.prepare()和Looper.loop()创建了消息队列就可以让消息处理在该线程中完成，这样的话，那子线程就会一直处于活跃状态，不会随着run方法执行完结束，Looper.loop()之后的代码不会执行，因为Looper.loop()里面有一个无限循环for (;;) 。

```
/**
 * @author Liupengfei
 * @describe 消息类
 */
public class MyMessage {

    private int code;
    private String msg;
    MyHandler target;

    public MyMessage(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
    public int getCode() {
        return code;
    }
    public void setCode(int code) {
        this.code = code;
    }
    public String getMsg() {
        return msg;
    }
    public void setMsg(String msg) {
        this.msg = msg;
    }

}

/**
 * @author Liupengfei
 * @describe 消息队列的接口
 */
public interface IMyMessageQueue {
    MyMessage next() throws InterruptedException;
    void enqueueMsg(MyMessage msg) throws InterruptedException;
}

/**
 * @author Liupengfei
 * @describe 消息队列实例
 */
public class MyBlockingQueue implements IMyMessageQueue {
    private BlockingQueue<MyMessage> mQueue;  //消息队列

    public MyBlockingQueue(int init) {
        this.mQueue = new LinkedBlockingDeque<>(init);
    }

    @Override
    public MyMessage next() throws InterruptedException {
        return mQueue.take();
    }

    @Override
    public void enqueueMsg(MyMessage msg) throws InterruptedException {
        mQueue.put(msg);
    }
}


/**
 * @author Liupengfei
 * @describe 自定义handler
 */
public abstract class MyHandler {
    private IMyMessageQueue mQueue;

    public MyHandler(MyLooper looper) {
        mQueue = looper.mMessageQueue;
    }

    public MyHandler() {
        MyLooper.myLooper();
    }

    public void sendMessage(MyMessage msg) {
        msg.target = this;
        try {
            mQueue.enqueueMsg(msg);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public abstract void handleMessage(MyMessage msg);

}

/**
 * @author Liupengfei
 * @describe 自定义轮循器
 */
public class MyLooper {
    private static final ThreadLocal<MyLooper> THREAD_LOCAL = new ThreadLocal<>();
    IMyMessageQueue mMessageQueue;
    private static MyLooper mMainLooper;

    public MyLooper() {
        mMessageQueue = new MyBlockingQueue(4);
    }

    public static void prepare() {
        if (null != THREAD_LOCAL.get()) {
            throw new RuntimeException("Only one looper can be created per thread.");
        }
        THREAD_LOCAL.set(new MyLooper());
    }

    public static void prepareMainLooper() {
        prepare();
        synchronized (MyLooper.class) {
            if (null != mMainLooper) {
                throw new RuntimeException("MainLooper has already been prepared.");
            }
            mMainLooper = myLooper();
        }
    }

    public static void loop() {
        final MyLooper looper = myLooper();
        if (null == looper) {
            throw new RuntimeException("No looper! MyLooper.prepare() wasn't called on this thread.");
        }
        for (; ; ) {
            MyMessage msg = null;
            try {
                msg = looper.mMessageQueue.next();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (null != msg) {
                msg.target.handleMessage(msg);
            }
        }
    }

    public static MyLooper getMainLooper() {
        return mMainLooper;
    }

    public static MyLooper myLooper() {
        return THREAD_LOCAL.get();
    }
}

```
自定义的handler比较简单，只有一种消息，在Android的Handler里面，Message分为3种：==普通消息（同步消息）、屏障消息（同步屏障）和异步消息==。我们通常使用的都是普通消息，而屏障消息就是在消息队列中插入一个屏障，在屏障之后的所有普通消息都会被挡着，不能被处理。不过异步消息却例外，屏障不会挡住异步消息，因此可以这样认为：屏障消息就是为了确保异步消息的优先级，设置了屏障后，只能处理其后的异步消息，同步消息会被挡住，除非撤销屏障。

##### 屏障消息
同步屏障是通过MessageQueue的postSyncBarrier方法插入到消息队列的。
```
private int postSyncBarrier(long when) {
        synchronized (this) {
            final int token = mNextBarrierToken++;
            //1、屏障消息和普通消息的区别是屏障消息没有tartget。
            final Message msg = Message.obtain();
            msg.markInUse();
            msg.when = when;
            msg.arg1 = token;

            Message prev = null;
            Message p = mMessages;
            //2、根据时间顺序将屏障插入到消息链表中适当的位置
            if (when != 0) {
                while (p != null && p.when <= when) {
                    prev = p;
                    p = p.next;
                }
            }
            if (prev != null) { // invariant: p == prev.next
                msg.next = p;
                prev.next = msg;
            } else {
                msg.next = p;
                mMessages = msg;
            }
            //3、返回一个序号，通过这个序号可以撤销屏障
            return token;
        }
    }

```
postSyncBarrier方法就是用来插入一个屏障到消息队列的，可以看到它很简单，从这个方法我们可以知道如下：

>屏障消息和普通消息的区别在于屏障没有tartget(Handler)，普通消息有target是因为它需要将消息分发给对应的target，而屏障不需要被分发，它就是用来挡住普通消息来保证异步消息优先处理的。  
屏障和普通消息一样可以根据时间来插入到消息队列中的适当位置，并且只会挡住它后面的同步消息的分发。  
postSyncBarrier返回一个int类型的数值，通过这个数值可以撤销屏障。  
postSyncBarrier方法是私有的，如果我们想调用它就得使用反射。  
插入普通消息会唤醒消息队列，但是插入屏障不会。

##### 屏障消息的工作原理
```
Message next() {
			//1、如果有消息被插入到消息队列或者超时时间到，就被唤醒，否则阻塞在这。
            nativePollOnce(ptr, nextPollTimeoutMillis);

            synchronized (this) {        
                Message prevMsg = null;
                Message msg = mMessages;
                if (msg != null && msg.target == null) {//2、遇到屏障  msg.target == null
                    do {
                        prevMsg = msg;
                        msg = msg.next;
                    } while (msg != null && !msg.isAsynchronous());//3、遍历消息链表找到最近的一条异步消息
                }
                if (msg != null) {
                	//4、如果找到异步消息
                    if (now < msg.when) {//异步消息还没到处理时间，就在等会（超时时间）
                        nextPollTimeoutMillis = (int) Math.min(msg.when - now, Integer.MAX_VALUE);
                    } else {
                        //异步消息到了处理时间，就从链表移除，返回它。
                        mBlocked = false;
                        if (prevMsg != null) {
                            prevMsg.next = msg.next;
                        } else {
                            mMessages = msg.next;
                        }
                        msg.next = null;
                        if (DEBUG) Log.v(TAG, "Returning message: " + msg);
                        msg.markInUse();
                        return msg;
                    }
                } else {
                    // 如果没有异步消息就一直休眠，等待被唤醒。
                    nextPollTimeoutMillis = -1;
                }
			//。。。。
        }
    }

```
##### 移除屏障
```
//注释已经写的很清楚了，就是通过插入同步屏障时返回的token 来移除屏障

    public void removeSyncBarrier(int token) {
        // Remove a sync barrier token from the queue.
        // If the queue is no longer stalled by a barrier then wake it.
        synchronized (this) {
            Message prev = null;
            Message p = mMessages;
            //找到token对应的屏障
            while (p != null && (p.target != null || p.arg1 != token)) {
                prev = p;
                p = p.next;
            }
            final boolean needWake;
            //从消息链表中移除
            if (prev != null) {
                prev.next = p.next;
                needWake = false;
            } else {
                mMessages = p.next;
                needWake = mMessages == null || mMessages.target != null;
            }
            //回收这个Message到对象池中。
            p.recycleUnchecked();
			// If the loop is quitting then it is already awake.
            // We can assume mPtr != 0 when mQuitting is false.
            if (needWake && !mQuitting) {
                nativeWake(mPtr);//唤醒消息队列
            }
    }

```

##### 如何发送异步消息
Handler有几个构造方法，可以传入async标志为true，这样构造的Handler发送的消息就是异步消息。不过可以看到，这些构造函数都是hide的，正常我们是不能调用的，不过利用反射机制可以使用@hide方法。

### ThreadLocal
threadlocal而是一个线程内部的存储类，可以在指定线程内存储数据，数据存储以后，只有指定线程可以得到存储数据。


threadlocal使用方法很简单
```
static final ThreadLocal<T> sThreadLocal = new ThreadLocal<T>();
sThreadLocal.set()
sThreadLocal.get()
```
**ThreadLocal的静态内部类ThreadLocalMap为每个Thread都维护了一个数组table，ThreadLocal确定了一个数组下标，而这个下标就是value存储的对应位置。**

```
//set 方法
public void set(T value) {
      //获取当前线程
      Thread t = Thread.currentThread();
      //实际存储的数据结构类型
      ThreadLocalMap map = getMap(t);
      //如果存在map就直接set，没有则创建map并set
      if (map != null)
          map.set(this, value);
      else
          createMap(t, value);
  }
  
//getMap方法
ThreadLocalMap getMap(Thread t) {
      //thred中维护了一个ThreadLocalMap
      return t.threadLocals;
 }
 
//createMap
void createMap(Thread t, T firstValue) {
      //实例化一个新的ThreadLocalMap，并赋值给线程的成员变量threadLocals
      t.threadLocals = new ThreadLocalMap(this, firstValue);
}
```
#### ThreadLocal特性
ThreadLocal和Synchronized都是为了解决多线程中相同变量的访问冲突问题，不同的点是

Synchronized是通过线程等待，牺牲时间来解决访问冲突  
ThreadLocal是通过每个线程单独一份存储空间，牺牲空间来解决冲突，并且相比于Synchronized，ThreadLocal具有线程隔离的效果，只有在线程内才能获取到对应的值，线程外则不能访问到想要的值。  

正因为ThreadLocal的线程隔离特性，使他的应用场景相对来说更为特殊一些。在android中Looper、ActivityThread以及AMS中都用到了ThreadLocal。当某些数据是以线程为作用域并且不同线程具有不同的数据副本的时候，就可以考虑采用ThreadLocal。

### 子线程创建handler
一般在子线程创建时不会创建Looper，所以随着run方法执行完，子线程就结束了，里面创建handler也因为没有Looper报错，所以在想要创建handler，就先要准备好Looper，并且让Looper循环起来，这样子线程的handler就可以接收消息了，但是这样的话Looper.loop()之后的方法不会执行，因为Looper.loop()里面是一个无限循环，会一直在里面执行循环，子线程也不会结束。
```
public void testThreadHandler(){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Thread.currentThread().setName("子线程");
                Looper.prepare();
                handler = new Handler(){

                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        Log.e("lpf--","收到消息");
                        Log.e("lpf--",Thread.currentThread().getName());
                    }
                };

                Looper.loop();

                Log.e("lpf--","Looper.loop()之后的方法不会执行");
            }
        }).start();
    }

```
### 子线程创建主线程的Handler

只需要在创建Handler时传入主线程Looper,Looper.getMainLooper()即可。
```
new Thread(new Runnable() {
            @Override
            public void run() {

                Thread.currentThread().setName("子线程");

                handler = new Handler(Looper.getMainLooper()){

                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        Log.e("lpf--","收到消息");
                        Log.e("lpf--",Thread.currentThread().getName());
                    }
                };
            }
        }).start();
    }

```
