# 3. Reactor 介绍

### 3.1 概述
Reactor是建立在反应式流规范的基础上（Reactive Stream 规范）。Reactor 引入了实现 `Publisher` 的响应式类 `Flux` 和 `Mono`，以及丰富的操作方式。 一个 `Flux` 对象代表一个包含 0..N 个元素的响应式序列，而一个 `Mono` 对象代表一个包含 零/一个（0..1）元素的结果。
### 3.2 环境准备
#### 3.2.1 Maven 配置
在`Spring boot`项目中只需要引入以下两个包
```xml
		<dependency>
			<groupId>io.projectreactor</groupId>
			<artifactId>reactor-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>io.projectreactor</groupId>
			<artifactId>reactor-core</artifactId>
		</dependency>
```
`reactor-test`提供了对 reactive streams 的单元测试。
`reactor-core`是核心的包
#### 3.2.2 GitLab仓库地址
文章所有代码都可在该仓库中找到,[gitlab地址](http://gitlab.inner.6ght.com:9090/6549/reactor-study)
### 3.3 Flux与Mono的使用
在Reactor库中，提供了两个异步序列API，`Flux`和`Mono。`
`Flux`是用于N个元素的数据流，`Mono`用于0|1个元素的数据流。
在非空数据流中，数据流就是一个按时间排序的 元素序列,它可以放射三种不同的 信号：(某种类型的)Value、Error 或者一个" Completed" 信号，这三种信号翻译为面向下游（订阅者）的`onNext()`,`onComplete()`和`onError()` 方法。
错误（error）信号和完成（completion）信号都是终止信号，二者不可能同时共存，数据流会被一个“错误（error）” 或“完成（completion）”信号终止。
#### 3.3.1 Flux
![flux.png](https://cdn.nlark.com/yuque/0/2020/png/1256055/1591514342630-d0c86a3c-04e6-47d9-8539-a7ea6ab3e6fb.png#align=left&display=inline&height=227&margin=%5Bobject%20Object%5D&name=flux.png&originHeight=227&originWidth=640&size=75677&status=done&style=none&width=640)
上图的意思就是，`Flux<T>` 是一个能够发出 0 到 N 个元素的标准的 `Publisher<T>。`
下面展示代码。[代码地址](http://gitlab.inner.6ght.com:9090/6549/reactor-study)
1.1 创建空元素的Flux类型的数据流，并发出complete信号
```java
    @Test
    @DisplayName("创建空的flux")
    public void testFlux_empty() {
        Flux<Object> empty = Flux.empty() //1
                .log();
        // 利用StepVerifier 断言测试
        StepVerifier.create(empty)
                .expectComplete() //2
                .verify();
    }
```

1. 创建一个空的数据流Flux。
1. 利用StepVerifier断言这个empty数据流会发出完成（completion）信号。



1.2 创建空元素的错误流，它会发出错误的终止信号
```java
    @Test
    @DisplayName("创建错误流flux")
    public void testFlux_error() {
        Flux<Object> empty = Flux.error(new Throwable()) //1
                .log();
        StepVerifier.create(empty)
                .expectError()  //2
                .verify();
    }
```

1. 创建一个错误数据流，在订阅的时候会抛出Throwable异常
1. 利用StepVerifier断言这个数据流有错误信号。



1.3 创建多个元素的flux
```java
    @Test
    @DisplayName("创建多个元素的Flux")
    public void testFlux_array() {
        Flux<Integer> fluxArray = Flux.just(1,2,3,4,5,6) //1
                .log();
        StepVerifier.create(fluxArray)
                .expectNext(1,2,3,4,5,6) //2
                .verifyComplete();

    }
```

1. 创建多个元素的数据流。
1. 利用StepVerifier断言这个数据流有1，2，3，4，5，6元素。在非空数据流中，（上游）Publisher会调用`onNext()`给下游（订阅者），`expectNext(1,2,3,4,5,6)`断言上游Publish会调用onNext(1),onNext(2) .....onNext(6) 。



#### 3.3.2 Mono
![](https://raw.githubusercontent.com/reactor/reactor-core/v3.0.7.RELEASE/src/docs/marble/mono.png#align=left&display=inline&height=227&margin=%5Bobject%20Object%5D&originHeight=227&originWidth=640&status=done&style=none&width=640)
`Mono<T>` 是一种特殊的 `Publisher<T>`， 它最多发出一个元素，然后终止于一个 `onComplete` 信号或一个 `onError` 信号。
1.1 创建空元素的Mono类型的数据流，并发出complete信号。
```java
    @Test
    @DisplayName("创建空的Mono")
    public void testMono_empty() {
        Mono<Object> empty = Mono.empty();
        StepVerifier.create(empty)
                .verifyComplete();
    }
```
1.2 创建空元素的错误流，它会发出错误的终止信号。
```java
    @Test
    @DisplayName("创建错误流Mono")
    public void testMonoError() {
        Mono<Object> empty = Mono
                .error(new Throwable())
                .log();
        StepVerifier.create(empty)
                .expectError()
                .verify();
    }
```
1.3 创建有一个元素的Mono。
```java
    @Test
    @DisplayName("创建有一个元素的Mono")
    public void testMonoElement() {
        Mono<String> data = Mono
                .just("data")
                .log();
        StepVerifier.create(data).expectNext("data")
                .verifyComplete();
    }
```
