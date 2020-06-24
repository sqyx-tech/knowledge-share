package reactor.core.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.io.Serializable;

/**
 * @author lanruqi
 * @date 2020/6/8
 */
public class FluxTest {

    @Test
    @DisplayName("创建空的flux")
    public void fluxEmpty() {
        Flux<Object> empty = Flux.empty()
                .log();
        StepVerifier.create(empty)
                .expectComplete()
                .verify();
    }

    @Test
    @DisplayName("创建错误流flux")
    public void fluxError() {
        Flux<Object> empty = Flux.error(new Throwable())
                .log();
        StepVerifier.create(empty)
                .expectError()
                .verify();
    }


    @Test
    @DisplayName("创建多个元素的Flux")
    public void fluxArray() {
        Flux<Integer> fluxArray = Flux.just(1,2,3,4,5,6)
                .log();
        StepVerifier.create(fluxArray)
                .expectNext(1,2,3,4,5,6)
                .verifyComplete();

    }

    @Test
    @DisplayName("订阅数据流Flux")
    public void fluxSubscribe() {
        Flux<Integer> flux = Flux.just(1, 2, 3, 4, 5, 6, 7, 8, 9);
        System.out.println("数据流未被处理");
        //订阅数据流
        flux.subscribe(System.out::println);
        //订阅数据流
        flux.subscribe(System.out::println);
    }

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
        flux.subscribe(System.out::println,System.err::println);
    }

    @Test
    @DisplayName("订阅数据流正常数据元素，错误信号,完成信号的处理")
    public void fluxSubscribeSuccessErrorCompleteHandle() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        System.out.println("数据流未被处理");
        flux.subscribe(System.out::println,System.err::println,()->System.out.println("complete"));
    }


    @Test
    @DisplayName("订阅并定义对正常数据元素、错误信号和完成信号的处理，以及订阅发生时的处理逻辑")
    public void fluxSubscribeSuccessErrorCompleteSubscriptHandle() {
        Flux<Integer> flux = Flux.just(1, 2, 3);
        System.out.println("订阅数据流---处理2个元素");
        flux.subscribe(System.out::println,null,()->System.out.println("complete"),subscription -> subscription.request(2));
        System.out.println("订阅数据流---处理3个元素");
        flux.subscribe(System.out::println,null,()->System.out.println("complete"),subscription -> subscription.request(3));
    }

}
