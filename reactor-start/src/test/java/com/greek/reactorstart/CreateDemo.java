package com.greek.reactorstart;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author lanruqi
 * @date 2020/6/8
 */
@DisplayName("创建数据流")
public class CreateDemo {

    @Test
    @DisplayName("justOrEmpty方法接收Optional对象创建数据流")
    public void monoOptionalEmpty() {
        Mono<Object> monoOptionalEmpty = Mono.justOrEmpty(Optional.empty()).log();
        StepVerifier.create(monoOptionalEmpty).verifyComplete();
    }

    @Test
    @DisplayName("justOrEmpty方法接收Null值创建数据流")
    public void monoJustOrEmpty() {
        Mono<Object> monoEmpty = Mono.justOrEmpty(null).log();
        StepVerifier.create(monoEmpty).verifyComplete();
    }

    @Test
    @DisplayName("fromSupplier延迟创建一个数据流")
    public void monoFormSupplier() {
        Mono<Long> monoFormSupplier = Mono.fromSupplier(System::currentTimeMillis);
        System.out.println("订阅前时间："+System.currentTimeMillis());
        StepVerifier.create(monoFormSupplier)
                .assertNext(time-> System.out.println("订阅获取到的时间："+time))
                .verifyComplete();
    }

    @Test
    @DisplayName("fromSupplier延迟创建一个空的数据流")
    public void monoFormSupplierEmpty() {
        Mono<Long> monoFormSupplier = Mono.fromSupplier(()->null);
        StepVerifier.create(monoFormSupplier)
                .verifyComplete();
    }


    int a = 5;
    @Test
    @DisplayName("比较defer和just的区别")
    public void deferVSJust() {
        Mono<Integer> monoJust = Mono.just(a);
        Mono<Integer> monoDefer = Mono.defer(() -> Mono.just(a));
        monoJust.subscribe(System.out::print);
        monoDefer.subscribe(System.out::print);
        a = 7;
        monoJust.subscribe(System.out::print);
        monoDefer.subscribe(System.out::print);
    }

    @Test
    @DisplayName("defer延迟创建一个数据流")
    public void defer() {
        AtomicInteger c = new AtomicInteger();
        Mono<String> source = Mono.defer(() -> c.getAndIncrement() < 3 ? Mono.empty() : Mono.just("test-data"));
        List<Long> iterations = new ArrayList<>();
        source.repeatWhenEmpty(o -> o.doOnNext(iterations::add)).subscribe(System.out::println);

        Assertions.assertEquals(4, c.get());
        Assertions.assertEquals(3,iterations.size());
    }

    @Test
    @DisplayName("用Flux.just创建数据流")
    public void fluxJust() {
        Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5);

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
    }


    @Test
    @DisplayName("Flux.fromArray不允许传入Null")
    public void arrayNull() {
        Assertions.assertThrows(NullPointerException.class,()->Flux.fromArray((Integer[]) null));
    }

    @Test
    @DisplayName("Flux.fromArray创建流")
    public void array() {
        Flux<Integer> flux = Flux.fromArray(new Integer[]{1, 2, 3, 4, 5});
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .verifyComplete();
    }

    @Test
    @DisplayName("FromIterable不接收Null参数")
    public void nullFromIterable() {
        Flux<Integer> flux = Flux.<Integer>fromIterable(() -> null);
        StepVerifier.create(flux)
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("将Iterable迭代器换成数据流")
    public void formIterable() {
        final Iterable<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Flux<Integer> flux = Flux.fromIterable(source);
        StepVerifier.create(flux)
                .expectNextCount(10)
                .verifyComplete();
    }

    @Test
    @DisplayName("将Iterable迭代器换成数据流-背压")
    public void formIterableBackpressured() throws InterruptedException {
        final Iterable<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Subscription[] innerSubscriptions = new Subscription[1];
        Flux.fromIterable(source)
                .subscribe(System.out::print, null, null, subscription -> innerSubscriptions[0]=subscription);
        innerSubscriptions[0].request(5);
        System.out.println("睡眠1秒");
        Thread.sleep(1000);
        innerSubscriptions[0].request(5);
    }

    @Test
    @DisplayName("将Iterable迭代器换成数据流，有NulL元素会抛NullPointerException")
    public void iteratorReturnsNull() {
        Flux<Integer> flux = Flux.fromIterable(Arrays.asList(1, 2, 3, 4, 5, null, 7, 8, 9, 10));
        StepVerifier.create(flux)
                .expectNextCount(5)
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("将ArrayList换成数据流")
    public void lambdaIterableWithList() {
        List<Integer> iterable = new ArrayList<>(10);
        iterable.add(0);
        iterable.add(1);
        iterable.add(2);
        iterable.add(3);
        iterable.add(4);
        iterable.add(5);
        iterable.add(6);
        iterable.add(7);
        iterable.add(8);
        iterable.add(9);

        StepVerifier.create(Flux.fromIterable(iterable), 0)
                .expectSubscription()
                .thenRequest(5)
                .expectNext(0, 1, 2, 3, 4)
                .thenRequest(5)
                .expectNext(5, 6, 7, 8, 9)
                .expectComplete()
                .verify();
    }


    @Test
    @DisplayName("允许传入空的迭代器换成空的数据流")
    public void emptyMapped() {
        Flux<Integer> map = Flux.fromIterable(Collections.<Integer>emptyList())
                .map(v -> v + 1);
        StepVerifier.create(map)
                .verifyComplete();
    }

    @Test
    @DisplayName("创建一个自增数据源")
    public void range() {
        Flux<Integer> range = Flux.range(1, 10).log();
        StepVerifier.create(range)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    @DisplayName("Flux.range 不能是负数")
    public void rangeCountIsNegative() {
        Assertions.assertThrows(IllegalArgumentException.class,()->Flux.range(1, -1));
    }

    @Test
    @DisplayName("Flux.range 起始数可以是负数")
    public void rangeStartIsNegative() {
        Flux<Integer> range = Flux.range(-10, 2);
        StepVerifier.create(range)
                .expectNext(-10, -9)
                .verifyComplete();
    }


    @Test
    @DisplayName("stream 转成数据源")
    public void stream() {
        final List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Flux<Integer> flux = Flux.fromStream(source.stream()).log();

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    @DisplayName("不能接收Null")
    public void streamNull() {
        Assertions.assertThrows(NullPointerException.class, () -> Flux.fromStream((Stream<?>) null));
    }

    @Test
    @DisplayName("Stream中有null,会抛NullPointerException")
    public void streamIteratorReturnsNull() {
        Flux<Integer> flux = Flux.fromStream(Stream.of(1, 2, 3, 4, 5, null, 7, 8, 9, 10));

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5)
                .expectError(NullPointerException.class)
                .verify();
    }

    @Test
    @DisplayName("将已被消费的流转成数据流，在订阅处理元素时会抛出IllegalStateException异常")
    public void streamAlreadyConsumed() {
        Stream<Integer> integerStream = Stream.of(1, 2, 3, 4, 5,6, 7, 8, 9, 10);
        System.out.println("消费流-元素个数："+integerStream.count());
        Flux<Integer> flux = Flux.fromStream(integerStream);

        //将已消费的流转换成数据流，在订阅处理元素时会抛出IllegalStateException异常
        StepVerifier.create(flux)
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    @DisplayName("同一个steam 多次订阅消费，也会抛出IllegalStateException异常")
    public void streamConsumedBySubscription() {
        Stream<Integer> integerStream = Stream.of(1, 2, 3, 4, 5,6, 7, 8, 9, 10);
        Flux<Integer> flux = Flux.fromStream(integerStream);

        //将已消费的流转换成数据流，在订阅处理元素时会抛出IllegalStateException异常
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5,6, 7, 8, 9, 10)
                .verifyComplete();
        System.out.println("第二次订阅同一个数据流,也会抛出IllegalStateException异常");
        // 在第一次消费后，会将流关闭，以后的订阅会抛出抛出IllegalStateException异常并提示 stream has already been operated upon or closed
        StepVerifier.create(flux)
                .expectError(IllegalStateException.class)
                .verify();
    }


    @Test
    @DisplayName("延迟将steam转成数据流，每次订阅都会生成一个新的流")
    public void streamGeneratedPerSubscription() {
        Flux<Integer> flux = Flux.fromStream(()->Stream.of(1, 2, 3, 4, 5,6, 7, 8, 9, 10));
        //因为使用Supplier，每次订阅都会生成一个新的流"
        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5,6, 7, 8, 9, 10)
                .verifyComplete();

        StepVerifier.create(flux)
                .expectNext(1, 2, 3, 4, 5,6, 7, 8, 9, 10)
                .verifyComplete();
    }


    @Test
    @DisplayName("steam可以被中途关闭")
    public void streamClosedOnCancelNormal() {
        AtomicInteger closed = new AtomicInteger();
        Stream<String> integerStream = Stream.of("foo", "bar", "baz").onClose(closed::incrementAndGet);
        Flux<String> flux = Flux.fromStream(integerStream);

        // steam的处理底层是FluxIterable，订阅方法底层调用是：FluxIterable.subscribe(actual, it, stream::close);
        // 当iterator关闭 会执行stream::close，关闭流。
        StepVerifier.create(flux)
                .expectNext("foo")
                .thenCancel()
                .verify();

        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("steam转数据流，在订阅完会关闭流")
    public void streamClosedOnCancelSlowPathNormal() {
        AtomicInteger closed = new AtomicInteger();
        Stream<String> integerStream = Stream.of("foo", "bar", "baz").onClose(closed::incrementAndGet);
        Flux<String> flux = Flux.fromStream(integerStream);
        // steam的处理底层是FluxIterable，订阅方法底层调用是：FluxIterable.subscribe(actual, it, stream::close);
        // 当iterator关闭 会执行stream::close，关闭流。
        StepVerifier.create(flux)
                .expectNext("foo", "bar", "baz")
                .verifyComplete();

        assertThat(closed.get()).isEqualTo(1);
    }


    @Test
    @DisplayName("steam转数据流，错误会关闭流")
    public void streamClosedOnErrorNormal() {
        AtomicInteger closed = new AtomicInteger();
        Stream<String> integerStream = Stream.of("foo", "bar", "baz").onClose(closed::incrementAndGet);
        Flux<String> flux = Flux.fromStream(integerStream).concatWith(Mono.error(new IllegalStateException("boom")));

        // steam的处理底层是FluxIterable，订阅方法底层调用是：FluxIterable.subscribe(actual, it, stream::close);
        // 当iterator关闭 会执行stream::close，关闭流。
        StepVerifier.create(flux)
                .expectNext("foo", "bar", "baz")
                .verifyErrorMessage("boom");

        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("steam转数据流，steam有Null会关闭流")
    public void streamClosedOnNullContentSlowPathNormal() {
        AtomicInteger closed = new AtomicInteger();
        Stream<String> integerStream = Stream.of("foo", "bar",null, "baz").onClose(closed::incrementAndGet);
        Flux<String> flux = Flux.fromStream(integerStream);

        // steam的处理底层是FluxIterable，订阅方法底层调用是：FluxIterable.subscribe(actual, it, stream::close);
        // 当iterator关闭 会执行stream::close，关闭流。
        StepVerifier.create(flux,4)
                .expectNext("foo", "bar")
                .verifyErrorMessage("The iterator returned a null value");

        assertThat(closed.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("订阅后，关闭steam，也不会影响到数据流")
    public void intermediateCloseIdempotent() {
        AtomicInteger closed = new AtomicInteger();
        Stream<String> integerStream = Stream.of("foo", "bar","baz").onClose(closed::incrementAndGet);
        Flux<String> flux = Flux.fromStream(integerStream);

        // steam的处理底层是FluxIterable，订阅方法底层调用是：FluxIterable.subscribe(actual, it, stream::close);
        // 当iterator关闭 会执行stream::close，关闭流。
        // 订阅后，关闭steam，也不会影响到数据流,因为订阅的时候已经获取到steam的iterator,此时关闭流，已经与steam的iterator无关了。
        StepVerifier.create(flux,1)
                .expectNext("foo")
                .then(integerStream::close)  //主动关闭steam
                .then(()->Assertions.assertEquals(1,closed.get()))
                .thenRequest(2)
                .expectNext("bar")
                .expectNext("baz")
                .verifyComplete();

        assertThat(closed.get()).isEqualTo(1);
    }


    @Test
    @DisplayName("supplier创建数据流")
    public void supplier() {
        AtomicInteger n = new AtomicInteger();
        Mono<Integer> mono = Mono.fromSupplier(n::incrementAndGet);

        StepVerifier.create(mono)
                .expectNext(1)
                .verifyComplete();

        StepVerifier.create(mono)
                .expectNext(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("创建一个同步执行的任务数据流")
    public void runnable() {
        Mono<Object> mono = Mono.fromRunnable(() -> {
        });
        StepVerifier.create(mono)
                .verifyComplete();
    }


    @Test
    @DisplayName("创建一个异步执行的任务数据流")
    public void asyncRunnable() {
        AtomicReference<Thread> t = new AtomicReference<>();
        StepVerifier.create(Mono.fromRunnable(() -> t.set(Thread.currentThread()))
                .subscribeOn(Schedulers.single()))
                .verifyComplete();
        assertThat(t).isNotNull();
        assertThat(t).isNotEqualTo(Thread.currentThread());
    }


    @Test
    @DisplayName("创建同步任务数据流，测量订阅到完成时间")
    public void runnableSubscribeToCompleteMeasurement() {
        AtomicLong subscribeTs = new AtomicLong();
        Mono<Object> mono = Mono.fromRunnable(() -> {
            try {
                Thread.sleep(5000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        })
                .doOnSubscribe(sub -> subscribeTs.set(-1 * System.nanoTime()))
                .doFinally(fin -> subscribeTs.addAndGet(System.nanoTime()));

        StepVerifier.create(mono)
                .verifyComplete();

        assertThat(TimeUnit.NANOSECONDS.toMillis(subscribeTs.get())).isCloseTo(5000L, Offset.offset(500L));
    }


    @Test
    @DisplayName("创建flux的空的数据流")
    public void fluxEmpty() {
        Flux<Object> empty = Flux.empty();
        StepVerifier.create(empty)
                .verifyComplete();
    }

    @Test
    @DisplayName("创建mono的空的数据流")
    public void monoEmpty() {
        Mono<Object> empty = Mono.empty();
        StepVerifier.create(empty)
                .verifyComplete();
    }

    @Test
    @DisplayName("创建flux错误流")
    public void fluxError() {
        Flux<Object> runtimeError = Flux.error(new RuntimeException("runtimeError"));

        StepVerifier.create(runtimeError)
                .expectErrorMessage("runtimeError")
                .verify();
    }

    @Test
    @DisplayName("创建mono错误流")
    public void  monoError() {
        Mono<Object> runtimeError = Mono.error(new RuntimeException("runtimeError"));

        StepVerifier.create(runtimeError)
                .expectErrorMessage("runtimeError")
                .verify();
    }

    @Test
    @DisplayName("创建一个只有订阅步骤的数据流")
    public void fluxNever() {
        Flux<Object> never = Flux.never();

        never.subscribe(new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("onSubscribe");
            }

            @Override
            public void onNext(Object o) {
                System.out.println("onNext");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("onError");
            }

            @Override
            public void onComplete() {
                System.out.println("onComplete");
            }
        });

    }

    @Test
    @DisplayName("从一个可回收的资源中创建数据源")
    public void using() {
        AtomicInteger cleanup = new AtomicInteger();
        /**
         * Flux.using(resourceSupplier,sourceSupplier,resourceCleanup,eager)
         * resourceSupplier 在订阅时被调用以生成资源
         * sourceSupplier 提供一个从提供的创造资源的工厂
         * resourceCleanup 完成时调用的资源清理回调
         * eager  是否在终止下游订户之前进行清理
         */
        Flux<Integer> using = Flux.using(() -> 1, r -> Flux.range(r, 10), cleanup::set, false);

        StepVerifier.create(using)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
        Assertions.assertEquals(1, cleanup.get());
    }


    @Test
    @DisplayName("从一个可回收的资源中创建数据源-在终止下游订户之前进行清理")
    public void usingEager() {
        AtomicInteger cleanup = new AtomicInteger();
        /**
         * Flux.using(resourceSupplier,sourceSupplier,resourceCleanup,eager)
         * resourceSupplier 在订阅时被调用以生成资源
         * sourceSupplier 提供一个从提供的创造资源的工厂
         * resourceCleanup 完成时调用的资源清理回调
         * eager  是否在终止下游订户之前进行清理
         */
        Flux<Integer> using = Flux.using(() -> 1, r -> Flux.range(r, 10), cleanup::set);

        StepVerifier.create(using)
                .expectNext(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
        Assertions.assertEquals(1, cleanup.get());
    }


    @Test
    @DisplayName("generate官方示例")
    public void generate() {
        Flux<String> flux = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3*state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux.subscribe(System.out::println);

    }


    @Test
    @DisplayName("不允许状态值为null")
    public void generateStateNull() {
       Assertions.assertThrows(NullPointerException.class,() -> Flux.generate(null, (s, o) -> s, s -> {
        }));
    }

    @Test
    @DisplayName("不允许generator值为null")
    public void generateNull() {
        Assertions.assertThrows(NullPointerException.class,() -> Flux.generate(() -> 1, null, s -> {
        }));
    }


    @Test
    @DisplayName("generate添加元素")
    public void generateJust() {
        Flux<Integer> generate = Flux.<Integer>generate(o -> {
            o.next(1);
            o.complete();
        });

        StepVerifier.create(generate)
                .expectNext(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("generate添加错误信号")
    public void generateError() {
        Flux<Integer> generate = Flux.<Integer>generate(o -> {
            o.error(new RuntimeException("forced failure"));
        });

        StepVerifier.create(generate)
                .expectError(RuntimeException.class)
                .verify();
    }


    @Test
    @DisplayName("背压")
    public void generateRange() {
        Flux<Integer> generate = Flux.<Integer, Integer>generate(() -> 1, (s, o) -> {
            if (s < 11) {
                o.next(s);
            } else {
                o.complete();
            }
            return s + 1;
        }).log();

        StepVerifier.create(generate,0)
                .expectSubscription()
                .thenRequest(2)
                .expectNext(1,2)
                .thenRequest(10)
                .expectNext( 3, 4, 5, 6, 7, 8, 9, 10)
                .verifyComplete();
    }

    @Test
    @DisplayName("简单使用")
    public void fluxCreate() {
        Flux<Integer> source = Flux.<Integer>create(e -> {
            e.next(1);
            e.next(2);
            e.next(3);
            e.complete();
        });

        StepVerifier.create(source)
                .expectNext(1, 2, 3)
                .verifyComplete();
    }

    @Test
    @DisplayName("抛出错误信号")
    public void fluxCreateError() {
        Flux<Integer> source = Flux.<Integer>create(e -> {
            e.next(1);
            e.next(2);
            e.next(3);
            e.error(new Exception("test"));
        });

        StepVerifier.create(source)
                .expectNext(1, 2, 3)
                .verifyErrorMessage("test");
    }


    @Test
    @DisplayName("默认策略是缓存")
    public void fluxCreateBuffered() {
        AtomicInteger onDispose = new AtomicInteger();
        AtomicInteger onCancel = new AtomicInteger();
        Flux<String> created = Flux.create(s -> {
            s.onDispose(onDispose::getAndIncrement)
                    .onCancel(onCancel::getAndIncrement);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        });

        StepVerifier.create(created)
                .expectNext("test1", "test2", "test3")
                .verifyComplete();

        assertThat(onDispose.get()).isEqualTo(1);
        assertThat(onCancel.get()).isEqualTo(0);
    }



    @Test
    @DisplayName("FluxSink的Disposable用法")
    void fluxCreateDisposables() {
        AtomicInteger dispose1 = new AtomicInteger();
        AtomicInteger dispose2 = new AtomicInteger();
        AtomicInteger cancel1 = new AtomicInteger();
        AtomicInteger cancel2 = new AtomicInteger();
        AtomicInteger cancellation = new AtomicInteger();
        Flux<String> created = Flux.create(s -> {
            // onDispose() 方法，第一个终止信号的时候会调用。终止信号可能是 取消，完成或错误信号
            /**
             *  在FluxSink中，会存储第一个终止信号的处理函数。接下在重复的终止信号处理函数会马上执行方法
             */
            s.onDispose(dispose1::getAndIncrement)
                    .onCancel(cancel1::getAndIncrement);  // FluxSink会存储onDispose和onCancel的函数
            s.onDispose(dispose2::getAndIncrement);   //第二次调用onDispose，这一步会马上执行方法
            assertThat(dispose2.get()).isEqualTo(1);
            s.onCancel(cancel2::getAndIncrement);
            assertThat(cancel2.get()).isEqualTo(1);
            s.onDispose(cancellation::getAndIncrement);
            assertThat(cancellation.get()).isEqualTo(1);
            assertThat(dispose1.get()).isEqualTo(0);
            assertThat(cancel1.get()).isEqualTo(0);
            s.next("test1");
            s.complete();
        });

        StepVerifier.create(created)
                .expectNext("test1")
                .verifyComplete();

        assertThat(dispose1.get()).isEqualTo(1);
        assertThat(cancel1.get()).isEqualTo(0);
    }


    @Test
    @DisplayName("发送取消信号")
    void fluxCreateBufferedCancelled() {
        AtomicInteger onDispose = new AtomicInteger();
        AtomicInteger onCancel = new AtomicInteger();
        Flux<String> created = Flux.create(s -> {
            // onDispose() 方法，第一个终止信号的时候会调用。终止信号可能是 取消，完成或错误信号
            s.onDispose(() -> {
                onDispose.getAndIncrement();
                assertThat(s.isCancelled()).isTrue();
            });
            // 取消信号的时候会调用该函数
            s.onCancel(() -> {
                onCancel.getAndIncrement();
                assertThat(s.isCancelled()).isTrue();
            });
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        });

        StepVerifier.create(created)
                .expectNext("test1", "test2", "test3")
                .thenCancel()
                .verify();

        assertThat(onDispose.get()).isEqualTo(1);
        assertThat(onCancel.get()).isEqualTo(1);
    }


    @Test
    @DisplayName("onDispose()方法的使用")
    void fluxCreateOnDispose() {
        int count = 5;
        AtomicInteger onDispose = new AtomicInteger();
        AtomicInteger onCancel = new AtomicInteger();
        class Emitter {

            private final FluxSink<Integer> sink;

            private Emitter(FluxSink<Integer> sink) {
                this.sink = sink;
            }

            public void emit(long n) {
                for (int i = 0; i < n; i++)
                    sink.next(i);
                sink.complete();
            }
        }
        Flux<Integer> flux1 = Flux.create(s -> {
            Emitter emitter = new Emitter(s);
            // onDispose() 不会受命令位置的影响，因为只有当出现第一个终止信号的时候才会调用
            s.onDispose(() -> onDispose.incrementAndGet());
            s.onCancel(() -> onCancel.incrementAndGet());
            // 当订阅请求获取元素的时候，调用Emitter#emit方法
            s.onRequest(emitter::emit);
        });
        StepVerifier.create(flux1, count)
                .expectNextCount(count)
                .expectComplete()
                .verify();
        // 断言，因为订阅没有取消，所以并不会触发onCancel，但是有完成信号，所以会触发onDispose
        assertThat(onDispose.get()).isEqualTo(1);
        assertThat(onCancel.get()).isEqualTo(0);

        onDispose.set(0);
        onCancel.set(0);
        Flux<Integer> flux2 = Flux.create(s -> {
            Emitter emitter = new Emitter(s);
            // 当订阅请求获取元素的时候，调用Emitter#emit方法
            s.onRequest(emitter::emit);
            s.onDispose(() -> onDispose.incrementAndGet());
            s.onCancel(() -> onCancel.incrementAndGet());
        });
        StepVerifier.create(flux2, count)
                .expectNextCount(count)
                .expectComplete()
                .verify();

        // 断言，因为订阅没有取消，所以并不会触发onCancel，但是有完成信号，所以会触发onDispose
        assertThat(onDispose.get()).isEqualTo(1);
        assertThat(onCancel.get()).isEqualTo(0);
    }


    @Test
    @DisplayName("先发送取消信号再设置onCancel方法")
    void monoFirstCancelThenOnCancel() {
        AtomicInteger onCancel = new AtomicInteger();
        AtomicReference<FluxSink<Object>> sink = new AtomicReference<>();
        StepVerifier.create(Flux.create(sink::set).log())
                .thenAwait()
                .consumeSubscriptionWith(Subscription::cancel)
                //此处并不会生效,因为先发送取消信号，会预设一个Disposable对象。当第二次发送取消信号时，disposable是onCancel而不是Disposable对象，因此就不会调用了。
                //源码在：FluxCreate#cancel()
                .then(() -> sink.get().onCancel(onCancel::getAndIncrement))
                .thenCancel()
                .verify();
        assertThat(onCancel.get()).isEqualTo(0);
    }



    @Test
    @DisplayName("先发送关闭信号然后设置onDispose函数")
    void monoFirstCancelThenOnDispose() {
        AtomicInteger onDispose = new AtomicInteger();
        AtomicReference<FluxSink<Object>> sink = new AtomicReference<>();
        StepVerifier.create(Flux.create(sink::set).log())
                .thenAwait()
                .consumeSubscriptionWith(Subscription::cancel)
                //设置onDispose后会马上调用，因为任何终止信息都会设置一个Disposable对象，所以在发送终止信号后再设置onDispose都是第二次设置
                //源码在：FluxCreate#cancel()
                .then(() -> sink.get().onDispose(onDispose::getAndIncrement))
                .thenCancel()
                .verify();
        assertThat(onDispose.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("背压")
    public void fluxCreateBufferedBackpressured() {
        Flux<String> flux = Flux.create(s -> {
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        });

        StepVerifier.create(flux, 1)
                .expectNext("test1")
                .thenAwait()
                .thenRequest(2)
                .expectNext("test2", "test3")
                .verifyComplete();
    }


    @Test
    @DisplayName("在调度器线程中添加元素")
    public void fluxCreateSinkConcurrent() {
        Scheduler.Worker w1 = Schedulers.elastic().createWorker();
        Scheduler.Worker w2 = Schedulers.elastic().createWorker();
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        AtomicReference<Thread> ref = new AtomicReference<>();

        ref.set(Thread.currentThread());

        Flux<Object> created = Flux.create(fluxSink -> {
            System.out.println(Thread.currentThread().getName());
            //因为next是在work线程调用，相当于调用了publishOn()，在另外线程中执行接下来的任务
            w1.schedule(() -> {
                fluxSink.next("test1");
            });
            try {
                //等待StepVerifier步骤唤醒
                latch2.await();
            } catch (InterruptedException e) {
                fail("unexpected InterruptedException");
            }
            w2.schedule(() -> {
                fluxSink.next("test2");
                fluxSink.next("test3");
                fluxSink.complete();
                latch.countDown();
            });
        }, FluxSink.OverflowStrategy.IGNORE).log();

        try {
            StepVerifier.create(created)
                    .assertNext(s -> {
                        assertThat(s).isEqualTo("test1");
                        // 当前线程是elastic-2，ref存储的线程是main
                        assertThat(ref.get()).isNotEqualTo(Thread.currentThread());
                        ref.set(Thread.currentThread());
                        //唤醒latch2
                        latch2.countDown();
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            fail("Unexpected InterruptedException");
                        }
                    })
                    .assertNext(s -> {
                        assertThat(ref.get()).isEqualTo(Thread.currentThread());
                        assertThat(s).isEqualTo("test2");
                    })
                    .assertNext(s -> {
                        assertThat(ref.get()).isEqualTo(Thread.currentThread());
                        assertThat(s).isEqualTo("test3");
                    })
                    .verifyComplete();
        } finally {
            w1.dispose();
            w2.dispose();
        }
    }


    @Test
    @DisplayName("LATEST策略--背压实现")
    public void fluxCreateLatestBackpressured() {
        Flux<String> created = Flux.create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }, FluxSink.OverflowStrategy.LATEST);

        assertThat(created.getPrefetch()).isEqualTo(-1);

        StepVerifier.create(created,1)
                .expectNext("test1")
                .thenAwait()
                .thenRequest(2)
                .expectNext("test3")
                .verifyComplete();
    }
    @Test
    @DisplayName("DROP策略--背压实现")
    public void fluxCreateDropBackpressured() {
        Flux<String> created = Flux.create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }, FluxSink.OverflowStrategy.DROP);

        StepVerifier.create(created, 1)
                .expectNext("test1")
                .thenAwait()
                .thenRequest(2)
                .verifyComplete();
    }

    @Test
    @DisplayName("ERROR策略--背压实现")
    void fluxCreateErrorBackpressured() {
        Flux<String> created = Flux.create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }, FluxSink.OverflowStrategy.ERROR);

        StepVerifier.create(created, 1)
                .expectNext("test1")
                .thenAwait()
                .thenRequest(2)
                .verifyErrorMatches(Exceptions::isOverflow);
    }

    @Test
    @DisplayName("IGNORE策略--背压实现")
    void fluxCreateIgnoreBackpressured() {
        Flux<String> created = Flux.<String>create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }, FluxSink.OverflowStrategy.IGNORE).log();

        try {
            // 当推的数量大于请求数量，StepVerifier会终止流，并抛出request overflown by signal: onNext(test2) 的异常信息
            StepVerifier.create(created, 1)
                    .expectNext("test1")
                    .thenAwait()
                    .thenRequest(2)
                    .expectNext("test2")
                    .expectNext("test3")
                    .verifyComplete();
            fail("Expected AssertionError here");
        }
        catch (AssertionError error){
            assertThat(error).hasMessageContaining(
                    "request overflow (expected production of at most 1; produced: 2; request overflown by signal: onNext(test2))");
        }
    }

    @Test
    @DisplayName("IGNORE策略--与DROP策略对比")
    void fluxCreateIgnoreBackpressured1() {
        Flux.<String>create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }, FluxSink.OverflowStrategy.IGNORE)
                .subscribe(System.out::println, null, null, subscription -> subscription.request(1));


        Flux.<String>create(s -> {
            assertThat(s.requestedFromDownstream()).isEqualTo(1);
            s.next("test5");
            s.next("test6");
            s.next("test7");
            s.complete();
        }, FluxSink.OverflowStrategy.DROP)
                .subscribe(System.out::println, null, null, subscription -> subscription.request(1));
    }

    @Test
    @DisplayName("push方法实现推送数据")
    void fluxPush() {
        Flux<String> created = Flux.<String>push(s -> {
            s.next("test1");
            s.next("test2");
            s.next("test3");
            s.complete();
        }).log();

        assertThat(created.getPrefetch()).isEqualTo(-1);

        StepVerifier.create(created)
                .expectNext("test1", "test2", "test3")
                .verifyComplete();
    }



    @Test
    @DisplayName("Flux.push的onRequest推送元素")
    public void fluxPushOnRequest() {
        AtomicInteger index = new AtomicInteger(1);
        AtomicInteger onRequest = new AtomicInteger();
        /**
         * Flux.push 只有推送模式，默认是推送Long.MAX_VALUE个元素
         * Flux.push 创建的是BaseSink的子类对象，不是SerializedSink对象
         */
        Flux<Integer> created = Flux.<Integer>push(s -> {
            //当订阅的时候，触发onRequest请求。源码在：FluxCreate#onRequest()
            s.onRequest(n -> {
                onRequest.incrementAndGet();
                assertThat(n).isEqualTo(Long.MAX_VALUE);
                for (int i = 0; i < 5; i++) {
                    s.next(index.getAndIncrement());
                }
                s.complete();
            });
        }, FluxSink.OverflowStrategy.BUFFER).log();

        StepVerifier.create(created, 0)
                .expectSubscription()
                .thenAwait()
                .thenRequest(1)
                .expectNext(1)
                .thenRequest(2)
                .expectNext(2, 3)
                .thenRequest(2)
                .expectNext(4, 5)
                .expectComplete()
                .verify();
        assertThat(onRequest.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Flux.create的onRequest推拉混合模式")
    void fluxCreateGenerateOnRequest() {
        AtomicInteger index = new AtomicInteger(1);
        // Flux.create 是支持push pull 模式
        Flux<Integer> created = Flux.create(s -> {
            // n 默认是0，当订阅发送request请求N个元素时，推送N个元素
            s.onRequest(n -> {
                for (int i = 0; i < n; i++) {
                    s.next(index.getAndIncrement());
                }
            });
        });

        StepVerifier.create(created, 0)
                .expectSubscription()
                .thenAwait()
                .thenRequest(1)
                .expectNext(1)
                .thenRequest(2)
                .expectNext(2, 3)
                .thenCancel()
                .verify();
    }
}
