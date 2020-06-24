package reactor.core.processors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.DirectProcessor;
import reactor.core.subscriber.AssertSubscriber;
import reactor.test.StepVerifier;

/**
 * @author lianghong
 * @date 2020/6/12
 */
public class DirectProcessorTest {

    @Test
    @DisplayName("正常")
    public void normal() {
        DirectProcessor<Integer> directProcessor = DirectProcessor.create();
        StepVerifier
                .create(directProcessor)
                .then(() -> {
                    Assertions.assertTrue(directProcessor.hasDownstreams(), "缺少订阅者");
                    Assertions.assertFalse(directProcessor.hasCompleted(), "completed ?");
                    Assertions.assertFalse(directProcessor.hasError(), "error ?");
                })
                .then(() -> {
                    // 开始推送数据
                    directProcessor.onNext(1);
                    directProcessor.onNext(2);
                })
                .expectNext(1, 2)
                .then(() -> {
                    // 继续推送数据
                    directProcessor.onNext(3);
                    directProcessor.onComplete();
                })
                .expectNext(3)
                .expectComplete()
                .verify();
    }

    /**
     * DirectProcessor它的不足是无法处理背压。所以，当 DirectProcessor 推送的是 N 个元素，
     * 而至少有一个订阅者的请求个数少于 N 的时候，就会发出一个 IllegalStateException
     */
    @Test
    @DisplayName("没有足够的请求")
    public void notEnoughRequest() {
        DirectProcessor<Integer> directProcessor = DirectProcessor.create();

        StepVerifier
                .create(directProcessor, 1L)
                .then(() -> {
                    directProcessor.onNext(1);
                    directProcessor.onNext(2);
                    directProcessor.onComplete();
                })
                .expectNext(1)
                .expectError(IllegalStateException.class)
                .verify();
    }


    @Test
    @DisplayName("在DirectProcessor发出完成信号继续订阅")
    public void AfterOncompleteAddSubscribe() {
        DirectProcessor<Integer> directProcessor = DirectProcessor.create();
        AssertSubscriber ts_1 = AssertSubscriber.create();
        AssertSubscriber ts_2 = AssertSubscriber.create();

        directProcessor.subscribe(ts_1);
        Assertions.assertTrue(directProcessor.hasDownstreams(), "no subscriber?");

        // 开始发出型号
        directProcessor.onNext(1);
        // ts1预测信号
        ts_1.assertValues(1)
                .assertNoError()
                .assertNotComplete();

        // 发出complete信号
        directProcessor.onComplete();
        ts_1.assertComplete();

        // 再订阅一次
        directProcessor.subscribe(ts_2);
        ts_2.assertComplete();

        Assertions.assertTrue(directProcessor.hasCompleted(), "no Completed?");
    }

    @Test
    @DisplayName("在发出一个信号后增加订阅者")
    public void afterOnNextAddSubscriber() {

        DirectProcessor<Integer> directProcessor = DirectProcessor.create();
        AssertSubscriber ts_1 = AssertSubscriber.create();
        AssertSubscriber ts_2 = AssertSubscriber.create();

        directProcessor.subscribe(ts_1);
        Assertions.assertTrue(directProcessor.hasDownstreams(), "no subscriber?");
        directProcessor.onNext(1);

        ts_1.assertValues(1)
                .assertNoError()
                .assertNotComplete();

        directProcessor.subscribe(ts_2);

        directProcessor.onNext(2);
        directProcessor.onComplete();

        ts_1.assertValues(1, 2)
                .assertComplete();

        ts_2.assertValues(2)
                .assertComplete();

        Assertions.assertTrue(directProcessor.hasCompleted(), "no completed ?");
    }

}
