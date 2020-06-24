package reactor.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;

/**
 * @author lianghong
 * @date 2020/6/8
 */
public class StepVerifierTest {


    @Nested
    @DisplayName("通用测试")
    class CommonTest {

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

        @Test
        public void testAppBoomError() {
            Flux<String> source = Flux.just("foo", "bar");
            // appendBoomError返回的是一个新的flux
            source = appendBoomError(source);

            StepVerifier
                    .create(source) // 根据flux构建一个StepVerifier
                    .expectNext("foo") // 预测下一个值为foo
                    .expectNext("bar") // 预测下一个值为bar
                    .expectErrorMessage("boom") // 预测第三个元素为一个错误信息，值为boom
                    .verify(); // 使用verify触发测试
        }


        @Test
        @DisplayName("验证错误信号")
        public void testVerifyError() {
            StepVerifier
                    .create(Flux.error(new IllegalArgumentException("boom")))
                    .verifyError();
        }

        @Test
        public void testRequest() {
            Flux<Integer> source = Flux.range(1, 10);

            StepVerifier
                    .create(source)
                    .thenRequest(2)
                    .expectNextCount(2)
                    .thenRequest(8)
                    .thenCancel()
                    .verify();
        }

        private <T> Flux<T> appendBoomError(Flux<T> source) {
            return source.concatWith(Mono.error(new IllegalArgumentException("boom")));
        }
    }


    /**
     * recordWith可以存储 Subscriber.onNext(Object)中的元素
     * 可以在expectRecordedMatches中进行消费
     */
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


    @Nested
    @DisplayName("预测型API")
    class ExpectType {

        @Test
        public void testCreate() {
            StepVerifier.create(Flux.range(1, 2))
                    .expectNext(1)
                    .expectNextCount(1)
                    .verifyComplete();
        }

        /**
         * StepVerifier.crate方法的重载，新增对序列首次请求的个数的参数。
         *
         * ps：请试一下将下面thenRequest、expectNext方法去掉？
         *     看看会出现怎样的结果，再分析一波？
         */
        @Test
        @DisplayName("创建StepVerifier并设置首次请求元素个数")
        public void testCreateAppendRequest() {
            StepVerifier.create(Flux.range(1, 3).log(), 2)
                    .expectNext(1, 2)
                    .thenRequest(1)
                    .expectNext(3)
                    .verifyComplete();
        }


        /**
         * tips:
         * 1. 每一步的expectNext实际上是request一个序列的元素，然后判断是否符合期望
         * 2. 甚至可以使用expectNextMatches方法，使用一个自定义的Predicate去判断
         */
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


        /**
         * 场景：当需要验证下一个序列元素的个数是否符合预期时，可以使用expectNextCount方法
         */
        @Test
        @DisplayName("验证序列的元素个数是否符合预期")
        public void testNextCount() {
            Flux<Integer> source = Flux.range(1, 5);
            StepVerifier
                    .create(source)
                    .expectNextCount(5)
                    .verifyComplete();
        }

        /**
         * tips：在这里我们仅能使用一个方法去预期异常信息。
         *      因为错误信号已经通知订阅者，所以在expectErrorMatches后面就不能增加expect*的方法了！
         *
         * 拓展知识：如果在使用StepVerifier的方法的过程中，该方法如果包含着终止信号的含义（completeEvent、ErrorEvent），
         *          则在这些方法后面是不可以再追加其他expect*的方法了。
         */
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

    }
}
