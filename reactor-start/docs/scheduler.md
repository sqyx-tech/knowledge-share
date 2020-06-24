# 4. 调度器

# 调度器


## 概述


Reactor的执行模式和执行过程取决于正在使用的**Scheduler**，**Scheduler**在Reactor库中是一个拥有很多实现类的接口，并且Reactor通过**Schedulers**工具类提供的静态方法可以搭建不同的线程执行环境。


## 常用的调度器


- **Schedulers.immediate() ：**当前线程
- **Schedulers.single()：**单一线程池
- **Schedulers.elastic()：**弹性线程池，会重用空闲线程。线程池有默认的空闲时间（默认60秒，空闲时间超过60秒会被废弃）
- **Schedulers.parallel()：**固定大小线程池，线程个数和CPU核数相同
- **Schedulers.fromExecutor(Executor executor)：**基于现有的线程池创建Scheduler
- **Schedulers.newParallel(String name)：**可以使用新的名称来创建一个固定线程池的Scheduler
- **Schedulers.new* ：**同上，只不过线程池类型不同



## 切换线程执行环境


Reactor可以通过**publishOn**和**subscribeOn**两个方法切换操作符的线程执行环境，它们都接受一个**Scheduler**作为参数。


- **publishOn：**会影响该操作符后面的操作符，比如使用publishOn将调度器设置为elastic，则publishOn后面的操作符执行环境就是在一个弹性线程池中执行。
- **subscribeOn：**会影响到源头的线程执行环境，并且subscribeOn出现在操作链中的什么位置，只有操作链中最早的subscribeOn调用才算数。



下面以一个案例来举例说明：


```java
@Test
public void testExecEnv() {
    Flux.range(0, 10)
            .map(item -> item * item)
            .log() // 打点日志1
            .publishOn(Schedulers.newParallel("Parallel"))
            .filter(item -> item % 2 != 0)
            .log() // 打点日志2
            .publishOn(Schedulers.newSingle("Single"))
            .take(3)
            .log() // 打点日志3
            .subscribeOn(Schedulers.newElastic("Elastic"))
            .log() // 打点日志4
            .blockLast();
}
```


运行这个测试用例，通过保留不同的打点日志观察，会得出下面的结论：


1. 只保留打点日志1时：会发现map操作符的执行线程是Elastic，原因是在操作链的最后调用的subscribeOn操作符。
1. 只保留打点日志2是：会发现filter操作符的执行线程是Parallel，原因是在调用filter之前调用了publishOn操作符。
1. 只保留打点日志3时：会发现take操作符的执行线程是Single，原因是在调用take操作符之前调用了publishOn操作符。
1. 只保留打点日志4时：会发现当前数据流的执行线程仍然Single。
整个操作链的线程执行环境如下图所示

![image.png](https://cdn.nlark.com/yuque/0/2020/png/1360602/1591966119733-b3f7381e-bd6a-416e-aee4-8c9a4731e28d.png#align=left&display=inline&height=129&margin=%5Bobject%20Object%5D&name=image.png&originHeight=230&originWidth=1335&size=38792&status=done&style=none&width=746)


## 调度器工作原理分析


现在我们已经知道**publishOn**和**subscribeOn**操作符是会影响操作链的线程执行环境，但是在Reactor中具体是怎么实现的，我们还是需要了解一下的！


首先一开始就说Scheduler是一个抽象接口，现在我们可以通过分析Scheduler接口的行为，可以大致了解调度器的职责和执行模式。


> 以下代码注释借鉴道与术这篇文章



```java
public interface Scheduler extends Disposable {
    // 调度执行Runnable任务task。
    Disposable schedule(Runnable task);
    // 延迟一段指定的时间后执行。
    Disposable schedule(Runnable task, long delay, TimeUnit unit);
    // 周期性地执行任务。
    Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit);
    // 创建一个工作线程。
    Worker createWorker();
    // 启动调度器
    void start();
    // 以下两个方法可以暂时忽略
    void dispose();
    long now(TimeUnit unit)

    // 一个Worker代表调度器可调度的一个工作线程，在一个Worker内，遵循FIFO（先进先出）的任务执行策略
    interface Worker extends Disposable {
        // 调度执行Runnable任务task。
        Disposable schedule(Runnable task);
        // 延迟一段指定的时间后执行。
        Disposable schedule(Runnable task, long delay, TimeUnit unit);
        // 周期性地执行任务。
        Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit);
    }
}
```


需要注意worker这个内部接口。


每个操作符即是发布者也是订阅者。
Flux.publishOn操作符的装饰类FluxPublishOn，当下游订阅FluxPublishOn（调用FluxPublishOn的subscribe方法）


```java
public void subscribe(CoreSubscriber<? super T> actual) {
	Worker worker;
	try {
	    // 通过scheduler获取一个worker
		worker = Objects.requireNonNull(scheduler.createWorker(),
				"The scheduler returned a null worker");
	}
	catch (Throwable e) {
        ...
	}

    ...
    // 将FluxPublishOn下游订阅者和自身封装成PublishOnSubscriber对FluxPublishOn上游操作符进行订阅
	source.subscribe(new PublishOnSubscriber<>(actual,
			scheduler,
			// 将获取到的worker传递给对应的订阅者
			worker,
			delayError,
			prefetch,
			lowTide,
			queueSupplier));
}
```


**PublishOnSubscriber**类实现Runnable接口方法和QueueSubscription接口方法


接下来我们可以观察一下当**PublishOnSubscriber**是怎样向上游请求数据的


```java
// PublishOnSubscriber的request方法
public void request(long n) {
	if (Operators.validate(n)) {
		Operators.addCap(REQUESTED, this, n);
		// 追踪该方法
		trySchedule(this, null, null);
	}
}

// PublishOnSubscriber的trySchedule方法
void trySchedule(
		@Nullable Subscription subscription,
		@Nullable Throwable suppressed,
		@Nullable Object dataSignal) {
    ...
	try {
	    // 在这里我们就可以知道其实获取上游数据的执行线程委托给worker执行了
		worker.schedule(this);
	}
	catch (RejectedExecutionException ree) {
        ...
	}
}

// PublishOnSubscriber的run方法
public void run() {
	if (outputFused) {
		...
	}
	// 这里只关注run**方法，里面执行的就是向上游request数据，再通过下游的Subscriber.onNext方法将数据传递给下游
	else if (sourceMode == Fuseable.SYNC) {
		runSync();
	}
	else {
		runAsync();
	}
}
```


经过上面的代码分析，可以总结一下**publishOn**操作符影响后续操作符的执行机制。


1. 在操作链某个地方调用**publishOn**并传入对应的**Subscriber**
1. **publishOn**内部会通过**Scheduler**获取一个工作**worker**
1. 将下游**subscriber**的**onNext**、**onComplete**、**onError**委托给另一个线程



> subscribeOn原理大同小异，subscribeOn操作符的装饰类FluxSubscribeOn将下游subscriber的subscribe方法委托给一个worker执行，然后会在一个新的线程环境里将subscribe一直传递到数据源头，至此就可以达到改变源头发布数据的线程执行环境了。



![image.png](https://cdn.nlark.com/yuque/0/2020/png/1360602/1591968419846-70959765-1733-4845-8674-f7545dbe29ac.png#align=left&display=inline&height=368&margin=%5Bobject%20Object%5D&name=image.png&originHeight=368&originWidth=1071&size=54503&status=done&style=none&width=1071)
如图所示，**Scheduler**像一个调度者，将task任务交给**worker**执行。
实际上，每个**worker**都是一个**ScheduledExecutorService**或者是**ScheduledExecutorService**的包装类。


至此，关于**Scheduler**的执行模式和**onSubscribe**、**onPublish**操作符如何改变操作符的线程执行环境的原理有了一个大概的了解。
