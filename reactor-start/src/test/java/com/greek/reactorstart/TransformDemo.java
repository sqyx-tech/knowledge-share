package com.greek.reactorstart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Operators;
import reactor.test.StepVerifier;
import reactor.util.context.Context;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author lanruqi
 * @date 2020/6/10
 */
@DisplayName("转化型操作符例子")
public class TransformDemo {


    @Test
    @DisplayName("map的使用")
    public void map() {
        Flux<String> map = Flux.just(1, 2, 3).map(k -> k.toString());

        StepVerifier.create(map)
                .expectNext("1", "2", "3")
                .verifyComplete();
    }


    @Test
    @DisplayName("cast的使用")
    public void cast() {
        Flux<String> cast = Flux.just("1", "2", "3").cast(String.class).log();

        StepVerifier.create(cast)
                .expectNext("1", "2", "3")
                .verifyComplete();
    }


    @Test
    @DisplayName("Flux.index的使用")
    public void index() {
        Flux<Tuple2<Long, String>> log = Flux.just("1", "2", "3").index().log();
        StepVerifier.create(log)
                .expectNextMatches(k -> k.getT1() == 0L && k.getT2().equals("1"))
                .expectNextMatches(k -> k.getT1() == 1L && k.getT2().equals("2"))
                .expectNextMatches(k -> k.getT1() == 2L && k.getT2().equals("3"))
                .verifyComplete();
    }


    @Test
    @DisplayName("flatMap正常使用")
    public void flatMap() {
        Flux<Integer> flux = Flux.range(1, 1000)
                .flatMap(v -> Flux.range(v, 2));

        StepVerifier.create(flux)
                .expectNextCount(2000)
                .verifyComplete();
    }

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

    @Test
    @DisplayName("flatMap外部错误")
    public void flatMapError() {
        Flux<Integer> flux = Flux.<Integer>error(new RuntimeException("forced failure"))
                .flatMap(v -> Flux.just(v));

        StepVerifier.create(flux)
                .expectErrorMessage("forced failure")
                .verify();
    }

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

    @Test
    @DisplayName("fluxMap内部队列")
    public void testMaxConcurrency2() {
        // concurrency 参数决定内部队列的大小，这个队列大小影响一次request的数量。concurrency 的值相当于request(concurrency)
        Flux.range(1, 128).flatMap(Flux::just, 64).log().subscribe();
    }

    @Test
    @DisplayName("fluxMap向下游一次推送的数量")
    public void testMaxConcurrency1() {
        // prefetch 一次推送的最大数量
        Flux.range(1, 64).flatMap(Flux::just, 1, 32).log().subscribe();
    }



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
                .log()
                .onErrorContinue((e,v)->{
                    if (v != null) {
                        Operators.onNextDropped(v, Context.empty());
                    }
                    if (e != null) {
                        Operators.onErrorDropped(e, Context.empty());
                    }
                }).log();

        StepVerifier.create(test)
                .expectNoFusionSupport()
                .expectNext(3)
                .expectComplete()
                .verifyThenAssertThat()
                .hasDropped(1, 2)
                .hasDroppedErrors(2);
    }


    @Test
    public void errorModeContinueLargerThanConcurrencySourceMappedCallableFails() {
        AtomicInteger continued = new AtomicInteger();
        Flux.range(1, 500)
                .flatMap(v -> Flux.error(new IllegalStateException("boom #" + v)), 203).log()
                .onErrorContinue(IllegalStateException.class, (ex, elem) ->
                        {
                            System.out.println("");
                        }
                )
                .as(StepVerifier::create)
                .expectComplete()
                .verify(Duration.ofSeconds(1));

        assertThat(continued).hasValue(500);
    }

    @Test
    public void t11() {
        AtomicInteger continued = new AtomicInteger();
        Flux.range(1, 500)
                .flatMap(v -> Flux.error(new IllegalStateException("boom #" + v)), 203)
                .onErrorResume(k-> Flux.just(k.getMessage())).log()
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();

    }
}
