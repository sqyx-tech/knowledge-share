# 1. 订阅操作

## 1.1 只有订阅，数据流才会执行
上文提到的Flux/Mono都是创建数据流，在订阅前，什么都不会发生，只有当订阅了，每个数据才会被处理。
一个简单的订阅例子：
```java
    @Test
    @DisplayName("订阅数据流Flux")
    public void fluxSubscribe() {
        Flux<Integer> flux = Flux.just(1, 2, 3, 4);
        System.out.println("数据流未被处理");
        //订阅并指定对正常数据元素如何处理
        flux.subscribe(System.out::println); //1
        //订阅数据流
        flux.subscribe(System.out::println); //2
    }

输出结果：
数据流未被处理
1
2
3
4
1
2
3
4
```

1. 只有订阅数据流才会生成并且触发数据流处理，使用的方法是`subscribe(Consumer<? super T> consumer)`，订阅并指定对正常数据元素如何处理
1. 因为使用`Flux.Just()`创建数据流，`Flux.Just()`是当订阅的时候才会创建流，所以无论多少次订阅，流都是重新创建的。
## 1.2 更多的订阅方法
`subscribe`方法可以 使用lambda表达式，此外Flux和Mono都提供了多个`subscribe`方法的变体。
具体方法有：
```java
// 订阅并触发数据流
subscribe(); 
// 订阅并指定对正常数据元素如何处理
subscribe(Consumer<? super T> consumer); 
// 订阅并定义对正常数据元素和错误信号的处理
subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer); 
// 订阅并定义对正常数据元素、错误信号和完成信号的处理
subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer,
          Runnable completeConsumer); 
// 订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑
subscribe(Consumer<? super T> consumer,
          Consumer<? super Throwable> errorConsumer,
          Runnable completeConsumer,
          Consumer<? super Subscription> subscriptionConsumer); 
```
例子： 订阅并定义对正常数据元素和错误信号的处理
```java
    @Test
    @DisplayName("订阅数据流正常数据元素和错误信号的处理")
    public void fluxSubscribeSuccessErrorHandle() {
        Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5,6)
                .map(k -> {
                    if (k == 5) {
                        throw new RuntimeException("boom");
                    }
                    return k;
                });
        System.out.println("数据流未被处理");
        flux.subscribe(System.out::println,System.err::println); // 1
    }

输出结果：
数据流未被处理
1
2
3
4
java.lang.RuntimeException: boom

```

1. 调用`subscribe(Consumer<? super T> consumer , Consumer<? super Throwable> errorConsumer);` 方法。当数据流有异常的时候会，`errorConsumer`方法处理错误信号。代码中6没有输出，是因为在执行到5元素的时候抛出异常，错误信号终止了流处理。



例子：订阅并定义对正常数据元素、错误信号和完成信号的处理
```java
    @Test
    @DisplayName("订阅数据流正常数据元素，错误信号,完成信号的处理")
    public void fluxSubscribeSuccessErrorCompleteHandle() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        System.out.println("数据流未被处理");
        flux.subscribe(System.out::println,null,
                       ()->System.out.println("complete"));  //1
    }

输出结果：
数据流未被处理
1
2
3
complete
```

1. 调用`subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer,Runnable completeConsumer); `方法。错误信号和完成信号只会有一个，所以当流完成后，会发出完成信号，由`completeConsumer`方法处理完成信号。



例子：订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑
```java
    @Test
    @DisplayName("订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑")
    public void fluxSubscribeSuccessErrorCompleteSubscriptHandle() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        System.out.println("订阅数据流---处理2个元素");
        flux.subscribe(System.out::println,null,
                       ()->System.out.println("complete"),
                       subscription -> subscription.request(2)); //1
        System.out.println("订阅数据流---处理3个元素");
        flux.subscribe(System.out::println,null,
                       ()->System.out.println("complete"),
                       subscription -> subscription.request(3)); //2
    }

输出结果：
订阅数据流---处理2个元素
1
2
订阅数据流---处理3个元素
1
2
3
complete

```

1. 调用`subscribe(Consumer<? super T> consumer,Consumer<? super Throwable> errorConsumer, Runnable completeConsumer,Consumer<? super Subscription> subscriptionConsumer); `方法。`subscriptionConsumer`参数，消费者调用订阅信号，一般用于修改订阅获取多少个元素。由于Flux有3个元素，只获取3个元素，所以上游（Publish）没有Complete信号。
1. Flux有3个元素，获取3个元素，所以最后上游（Publish）发出Complete信号。
