# 2. Reactive Streams规范

## 2.1 概述
Reactive Streams由Netflix、Lightben和Piveotal(Spring背后的公司)的工程师在2013年年底开始定制的一种规范。目的是为具有非阻塞背压的异步流处理提供一个标准。


对于我们程序员而言，Reactive Streams实际上是一组API。但需要注意的是，具体的流操作（转换、分割、合并等）的细节在Reactive Streams中并不关心。Reactive Streams只关心数据流在不同角色之间的传递关系。


总之，反应流是面向流的JVM库的标准和规范。


- 处理可能无限多的元素
- 按序处理
- 异步传递数据
- 必须提供非阻塞的背压（backpressure）的机制



当每个响应式库都遵循这个规范时，大家就可以相互兼容，甚至不同的库之间可以进行交互。
## 2.2 响应式流规范中定义的API组件（可以理解为角色）
响应式流的实现需要实现下面定义的接口
> 理解响应式规范中定义的几个接口API之间的调用关系，可以站在一个更高的维度去思考响应式编程中对流中的元素（数据）的处理关系，对理解Reactor库的使用有很大的帮助。



### Publisher
发布者
```java
public interface Publisher<T> {
    // 订阅者向发布者发起订阅
    public void subscribe(Subscriber<? super T> s);
}
```
### Subscriber
订阅者
```java
public interface Subscriber<T> {
    // Publisher通过对onSubscribe方法的调用，让Subscriber接收到第一个事件
    public void onSubscribe(Subscription s);
    // Publisher发布的每个数据项都会通过调用Subscriber的onNext()方法递交给Subscriber
    public void onNext(T t);
    // 如果发布有任何错误，Publisher就会调用onError()方法。
    public void onError(Throwable t);
    // 如果Publisher没有更多的数据，也不会继续产生更多的数据，那么将会调用Subscriber的onComplete()方法来告知Subscriber它已经结束。
    public void onComplete();
}
```
### Subscription
Subscriber消费Publisher发布消息的生命周期，Subscriber通过Subscription可以管理其订阅的情况
```java
public interface Subscription {
    // Subscriber通过调用request()方法来请求Publisher发送数据,
    // 可以传入一个long类型的数值以表明它愿意接受多少数据。这也是回压能够发挥作用的地方，以避免Publisher发送多于Subscriber能够处理的数据量。
    public void request(long n);
    // Subscriber通过调用cancel()方法表明它不再对数据感兴趣并且取消订阅
    public void cancel();
}
```
### Processor
表示一个处理阶段，它既是订阅者也是发布者，并且遵守两者的契约
```java
public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```
## 2.3 每个角色之间简单的交互时序图
![image.png](https://cdn.nlark.com/yuque/0/2020/png/1360602/1591602872590-fd299593-a590-42b8-bb66-1dfd40f13b05.png#align=left&display=inline&height=751&margin=%5Bobject%20Object%5D&name=image.png&originHeight=751&originWidth=769&size=87429&status=done&style=none&width=769)
