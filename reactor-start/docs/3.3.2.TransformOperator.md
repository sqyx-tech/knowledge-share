# 3.2 转化操作符

## 3.2.1 一对一地转化类型 `map/cast/Flux.index`
### 3.2.1.1 类型转化: map
可以将一种类型转换成另一种类型，测试例子如下：
```java
    @Test
    @DisplayName("map的使用")
    public void map() {
        Flux<String> map = Flux.just(1, 2, 3).map(k -> k.toString());

        StepVerifier.create(map)
                .expectNext("1", "2", "3")
                .verifyComplete();
    }
```
### 3.2.1.2 类型转化: cast
可以将一种类型转换成另一种类型，底层也是用map方法，测试例子如下：
```java
    @Test
    @DisplayName("cast的使用")
    public void cast() {
        Flux<String> map = Flux.just("1", "2", "3").cast(String.class);

        StepVerifier.create(map)
                .expectNext("1", "2", "3")
                .verifyComplete();
    }
```
### 3.2.1.3 为了获得每个元素的序号: **Flux#index**
```java
    @Test
    @DisplayName("Flux.index的使用")
    public void index() {
        Flux<Tuple2<Long, String>> log = Flux.just("1", "2", "3").index().log();

        StepVerifier.create(log)
                .expectNextMatches(k->k.getT1()==0L&&k.getT2()=="1")
                .expectNextMatches(k->k.getT1()==1L&&k.getT2()=="2")
                .expectNextMatches(k->k.getT1()==2L&&k.getT2()=="3")
                .verifyComplete();
    }
```
## 3.2.2 1对N地转化 flatMap/handle
### 3.2.2.1 1对N地转化 flatMap
`Flux.flatMap()`方法可以将每个元素转换成流，然后将这些流操作后的结果合并为一个大的数据流。
`Flux.flatMap(publisher,concurrency,prefetch)`方法:

- `publisher`：元素转成流的函数。
- `concurrency`：决定内部队列的大小，这个队列大小影响一次request的数量。concurrency 的值相当于request(concurrency)。
- `prefetch`：_一_次推送到下游的最大数量。
#### `Flux.flatMap`简单使用
```java
    @Test
    @DisplayName("flatMap正常使用")
    public void flatMap() {
        Flux<Integer> flux = Flux.range(1, 1000)
                .flatMap(v -> Flux.range(v, 2));

        StepVerifier.create(flux)
                .expectNextCount(2000)
                .verifyComplete();
    }
```


#### `Flux.flatMap`背压实现
```java
    @Test
    @DisplayName("flatMap-背压实现")
    public void flatMapBackpreesured() {
        Flux<Integer> flux = Flux.range(1, 1000)
                .flatMap(v -> Flux.range(v, 2));

        StepVerifier.create(flux,0)
                .thenRequest(1000)
                .expectNextCount(1000)
                .thenRequest(1000)
                .expectNextCount(1000)
                .verifyComplete();
    }
```
#### `Flux.flatMap`外部错误
```java
    @Test
    @DisplayName("flatMap外部错误")
    public void flatMapError() {
        Flux<Integer> flux = Flux.<Integer>error(new RuntimeException("forced failure"))
                .flatMap(v -> Flux.just(v));

        StepVerifier.create(flux)
                .expectErrorMessage("forced failure")
                .verify();
    }
```
#### `Flux.flatMap`内部出现错误


```java
    @Test
    @DisplayName("flatMap内部出现错误")
    public void flatMapInnerError() {
        //任何错误 都会发出终止流信号
        Flux<String> flux = Flux.<Integer>just(1, 2, 3, 4,5,6)
                .flatMap(v -> {
                    if (v < 4) {
                        return Flux.just("OK");
                    }
                    return Flux.error(new RuntimeException("forced failure"));
                });

        StepVerifier.create(flux)
                .expectNextCount(3)
                .expectErrorMessage("forced failure")
                .verify();
    }
```
#### `Flux.flatMap`内部队列
`concurrency `参数决定内部队列的大小，这个队列大小影响一次request的数量。concurrency 的值相当于request(concurrency)
```java
    @Test
    @DisplayName("fluxMap内部队列")
    public void testMaxConcurrency2() {
        // concurrency 参数决定内部队列的大小，这个队列大小影响一次request的数量。concurrency 的值相当于request(concurrency)
        Flux.range(1, 128).flatMap(Flux::just, 64).log().subscribe();
    }
```
#### `Flux.flatMap`向下游一次推送的数量
```java
    @Test
    @DisplayName("fluxMap向下游一次推送的数量")
    public void testMaxConcurrency1() {
        // prefetch 一次推送的最大数量
        Flux.range(1, 64).flatMap(Flux::just, 1, 32).log().subscribe();
    }
```


#### `Flux.flatMap`将发生错误的元素抛弃
`onErrorContinue`方法可以处理错误信号，让流继续执行。
```java
   @Test
    @DisplayName("将发生错误的元素抛弃")
    public void errorModeContinueNullPublisher() {
        Flux<Integer> test = Flux
                .just(1, 2,3)
                .hide()
                .<Integer>flatMap(f -> {
                    if (f==3){
                        return Flux.just(f);
                    }
                    return null;
                })
                .onErrorContinue((e,v)->{
                    if (v != null) {
                        Operators.onNextDropped(v, Context.empty());
                    }
                    if (e != null) {
                        Operators.onErrorDropped(e, Context.empty());
                    }
                });

        StepVerifier.create(test)
                .expectNoFusionSupport()
                .expectNext(3)
                .expectComplete()
                .verifyThenAssertThat()
                .hasDropped(1, 2)
                .hasDroppedErrors(2);
    }
```
### 3.2.2.2 1对n地转化可自定义转化方法和/或状态 handle
