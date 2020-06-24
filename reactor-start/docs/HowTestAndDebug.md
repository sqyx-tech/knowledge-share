# 2. 如何测试和调试

# 2.1 概述
在Reactor中，对于Flux和Mono的测试官方有提供一套专门的工具。测试之前需要确保项目有添加**reactor-test**的maven依赖。


```xml
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <version>3.1.4.RELEASE</version>
    <scope>test</scope>
</dependency>
```


**reactor-test**的主要用途有两种：


- 使用StepVerifier一步步地测试一个给定场景的序列
- 使用 TestPublisher 生成数据来测试下游的操作符



# 2.2 了解StepVerifier
StepVerifier是官方**reactor-test**提供的一个基本单元测试工具。我们在测试中需要关注序列中的每一个事件时，就很容易转化为使用StepVerifier的测试场景。
直接上代码让大家感受一下！


```java
@Test
@DisplayName("简单的测试")
public void simpleVerify() {
    Flux<String> source = Flux.just("foo", "bar");

    StepVerifier
            // 创建一个StepVerifier构造器来包待校验的flux
            .create(source)
            // 预期下一个元素的值为foo
            .expectNext("foo")
            // 可以通过定义Predicate来判断元素是否符合预期
            .expectNextMatches(s -> s.equals("bar"))
            // 调用verify执行校验动作，如果不调用该方法，则校验不会开始
            .verifyComplete();
}
```


通过StepVerifier的简单使用，就可以轻松地校验一个Flux和Mono。 StepVerifier的测试API大致可归类如下：
# 2.3 断言值判断型: expect类方法
通过传入预期值，判断当前订阅的元素是否和预期值一致，如果不一致，则抛出（AssertionError）。


**一步步校验序列中的每一个元素**


1. expectNext(T t)：传入一个预期元素，判断当前元素是否和预期值一样。每一步的expectNext实际上是request一个序列的元素，然后判断是否符合期望。
1. expectNextMatches：入参是一个Predicate，可以自定义谓语判断逻辑
1. verifyComplete()：执行校验，并期望得到一个完成信号



```java
@Test
@DisplayName("一步步验证序列的元素是否符合预期")
public void testStepByStepExpect() {
    Flux<String> source = Flux.just("foo", "bar", "zoo");
    StepVerifier
            .create(source)
            .expectNext("foo")
            .expectNext("bar")
            // 可以在这里添加一个Predicate，使用自定义的方式去判断
            .expectNextMatches(s -> s.equals("zoo"))
            .verifyComplete();
}
```


**验证当前消费的序列元素个数**
场景：当需要验证下一个序列元素的个数是否符合预期时，可以使用expectNextCount方法


1. expectNextCount(long count)：传入一个期望数字，期望当前订阅序列的元素个数是否等于期望个数



```java
@Test
@DisplayName("验证序列的元素个数是否符合预期")
public void testNextCount() {
    Flux<Integer> source = Flux.range(1, 5);
    StepVerifier
            .create(source)
            .expectNextCount(5)
            .verifyComplete();
}
```


**验证序列中的异常信息**


在下面的例子中，我们仅能使用一个方法去预期异常信息。因为错误信号已经通知订阅者，所以在expectErrorMatches后面就不能增加expect*的方法了！


> 如果在使用StepVerifier的方法的过程中，该方法如果包含着终止信号的含义（completeEvent、ErrorEvent），则在这些方法后面是不可以再追加其他expect*的方法了。



1. expectErrorMatches(Predicate predicate)：入参为Predicate，可以自定义对错误的谓语判断逻辑



```java
@Test
@DisplayName("验证序列中的异常信息")
public void testExpectException() {
    Flux<String> source = Flux.just("foo", "bar");
    source = source.concatWith(Mono.error(new IllegalStateException("exception message!")));
    StepVerifier
            .create(source)
            .expectNext("foo")
            .expectNext("bar")
            .expectErrorMatches(t ->
                    t instanceof IllegalStateException && t.getMessage().equals("exception message!"))
            .verify();
}
```


# 2.4 自定义判断型：Consume类方法
当需要跳过部分元素或者想对元素的内容进行自定义的**assertion**处理时需要用到。
**自定义断言判断**

1. assertNext(Consumer<? super T> assertionConsumer)：入参为Consumer，可以用自定义的逻辑对序列中的元素进行断言判断。



```java
@Test
@DisplayName("自定义断言")
public void testAssertNext() {
    Mono<String> source = Mono.just("foo");
    StepVerifier
            .create(source)
            // assertNext可以传入一个Consumer来自定义消费元素的逻辑
            .assertNext(s -> Assertions.assertEquals("foo", s))
            .verifyComplete();
}
```


**自定义元素处理**

1. consumeNextWith(Consumer<? super T> consumer)：可以用自定义的逻辑去消费序列中的元素
```java
@Test
@DisplayName("自定义元素处理")
public void testConsumeNextWith() {
    Flux<String> source = Flux.just("foo", "bar");
    StepVerifier
            .create(source)
            // 用自定义的方式去期望测试元素
            .consumeNextWith(s -> Assertions.assertTrue(s.equals("foo")))
            .expectNext("bar")
            .verifyComplete();
}
```


**自定义异常处理**

1. consumeErrorWith(Consumer consumer)：自定义一个Consumer去处理错误信号
```java
@Test
@DisplayName("自定义异常处理")
public void testConsumerErrorWith() {
    Flux<String> source = Flux.just("foo", "bar");
    source = source.concatWith(Mono.error(new IllegalStateException("exception Message!")));
    StepVerifier
            .create(source)
            .expectNext("foo")
            .expectNextMatches(s -> s.equals("bar"))
            // 消费一个错误信号，并对该信号的异常做自定义的处理
            .consumeErrorWith(t -> System.out.println("error: " + t.getMessage()))
            .verify();
}
```


# 2.5 其他型
等待延时序列或者主动request序列的元素
**记录序列的元素并自定义消费**

1. recordWith(Supplier<? extends Collection> supplier)：可以存储 Subscriber.onNext(Object)中的元素可以在expectRecordedMatches中进行消费
```java
@Test
public void verifyRecordWith() {
    Flux<String> source = Flux.just("foo", "bar", "foobar");

    StepVerifier
            .create(source)
            .recordWith(ArrayList::new)
            .expectNextCount(3)
            .expectRecordedMatches(list -> list.contains("bar"))
            .verifyComplete();
}
```


**操控时间**
StepVerifier 可以用来测试基于时间的操作符，从而避免测试的长时间运行。可以使用构造器StepVerifier.withVirtualTime。
```java
@Test
@DisplayName("虚拟时间的测试")
public void testWithVirtualTime() {
    StepVerifier.withVirtualTime(() -> Mono.delay(Duration.ofSeconds(10)))
            // onSubscription也是一个事件，先把这个事件消耗掉
            .expectSubscription()
            // 期望在10秒内没有任何事件发生
            .expectNoEvent(Duration.ofSeconds(10))
            .expectNext(0L)
            .verifyComplete();
}
```
