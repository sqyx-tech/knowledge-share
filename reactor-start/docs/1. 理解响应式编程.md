# 1. 理解响应式编程

### 1.1 什么是响应式编程
响应式编程是一种关注于数据流（data streams）和变化传递（propagation of change）的异步编程方式。
#### 1.1.1 数据流
数据流的来源有很多种，可以是请求时所传的参数，也可以来自数据源，或者点击鼠标时。通常我们会称创建流的行为为事件。事件的发生是有时间属性的，因此我们可以用时间轴来串联事件。
举例一个场景:
当添加一件商品到购物车的时候，发生一个事件。当从购物车删除/修改数量的时候，也发生一个事件。
那么在选购商品的时候，就会发生一连串的事件，也就形成了一串数据流，在时间轴上展示就如下图：
![](https://cdn.nlark.com/yuque/0/2020/png/1256055/1591352445975-d07c01f1-fc6b-45e7-8eae-8d06f6fd761b.png#align=left&display=inline&height=67&margin=%5Bobject%20Object%5D&originHeight=67&originWidth=920&size=0&status=done&style=none&width=920)


#### 1.1.2 变化传递
变化传递是指流处理方法的结果会传递给下一个操作方法或者订阅者，常见的流处理方法有`map`,`filter`。
举例一个场景：
当你在购物车选择一种商品的时候，产生一个事件。这个事件会进行两种操作，计算商品金额（单价*数量）和所有商品总价。
伪代码如下：
```java
        public void calculate(DataStream<CartEvent> cartEventStream) { 
            // cartEventStream是数据流
            double total = cartEventStream
                    // 分别计算商品金额
                    .map(cartEvent -> cartEvent.getProduct().getPrice() * cartEvent.getQuantity())  
                    // 计算总价
                    .sum();
        }
```
在上面伪代码中，map操作产生的数据流会传递到sum操作中，这个就是响应式编程的变化传递（propagation of change）。
#### 1.1.3 异步
异步编程本身是有很多优点，利用多核CPU的能力、让主线程不再阻塞。
响应式编程就是为了异步编程而生的，它实现异步非常简单。
举例一个场景：
当前有一个活动，每一件商品满200元，就会减20元，此时传递变化的伪代码就会变成以下:
```java
       public void calculate(DataStream<CartEvent> cartEventStream) { 
            // cartEventStream是数据流
            double total = cartEventStream
                    // 分别计算商品金额
                    .map(cartEvent -> cartEvent.getProduct().getPrice() * cartEvent.getQuantity())  
                    // 判断如果一件商品满200，则减20
                    .map(v->(v>=200)?(v-20):v)
                    // 计算总价
                    .sum();
        }
```
在上面伪代码中，只需要加多一个`map`操作方法，就可以实现满200-20了。但是这里有一个问题，整个流处理的过程都是在一个线程完成的，这个如果其中有一个操作流程是耗时久的，会影响整个过程。那么我们应该改成异步，伪代码如下：
```java
       public void calculate(DataStream<CartEvent> cartEventStream) { 
            // cartEventStream是数据流
            double total = cartEventStream
                    // 分别计算商品金额
                    .map(cartEvent -> cartEvent.getProduct().getPrice() * cartEvent.getQuantity())  
                    // 将下面的操作流程放在一个Schedulers调度器管理的线程中进行
                    .publishOn(Schedulers.elastic())
                    // 判断如果一件商品满200，则减20
                    .map(v->(v>=200)?(v-20):v)
                    // 计算总价
                    .sum();
        }
```
只需要使用`publishOn`就可以将下面操作放在一个新的线程中执行了。详细了解可以看下文的异步。
#### 1.1.4 特点
为什么使用响应式编程？抛弃命令式编程，转向响应式编程，目的为了用异步式代替阻塞式，并且解决了回调方式代码的难以阅读和维护的问题。此外，有一些响应式的库例如Reactor，还在这个基础上还提供了很多功能/特性。例如可读性、更丰富的操作符、订阅模式、背压、热流和冷流。
### 1.2 Lambdas，CompletableFuture和Stream
Java8引入了`Lambdas`和`CompletableFuture`。Lambda允许编写简洁的回调，而`CompletionStage`接口和`CompletableFuture`类最终允许以非阻塞方式和基于推送的方式处理future，同时提供链接此类延迟结果处理的功能。
Java 8还引入了Java 8 `Stream`，该Java 8 旨在有效处理无需延迟或延迟很小的数据流（包括原始类型）。它是基于拉的，只能使用一次，缺少与时间相关的操作，并且可以执行并行计算，但无法指定要使用的线程池。
### 1.3 响应式流
上文提到Java Stream，但是Java Stream对于响应式编程不是很合适，主要在两个方面，异步非阻塞和流量控制。
异步非阻塞：由于I/O阻塞会带来很大的性能损失和资源浪费，所以需要使用支持异步非阻塞的响应式库，但是Java Stream是一种同步API。
流量控制：在响应式流中，引入了一种概念叫背压（回压），目的就是为了实现流量控制，下文在介绍Reactor的时候会详细介绍。Java Stream在控制流量的能力还是比较弱。
![image.png](https://cdn.nlark.com/yuque/0/2020/png/1256055/1591509347926-61490dc9-d6c8-4a43-a9ee-b495fdfcd574.png#align=left&display=inline&height=89&margin=%5Bobject%20Object%5D&name=image.png&originHeight=89&originWidth=397&size=15747&status=done&style=none&width=397)
