package reactor.core.subscriber;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

/**
 * @author lianghong
 * @date 2020/6/9
 */
public class SchedulerTest {


    @Test
    public void testExecEnv() {
        Flux.range(0, 10)
                .map(item -> item * item)
                //.log() // 日志1
                .publishOn(Schedulers.newParallel("Parallel"))
                .filter(item -> item % 2 != 0)
                //.log() // 日志2
                .publishOn(Schedulers.newSingle("Single"))
                .take(3)
                //.log() // 日志3
                .subscribeOn(Schedulers.newElastic("Elastic"))
                .log() // 日志4
                .blockLast();
    }

    @Test
    public void fuseableTest() {
        Flux.range(0, 10)
                .map(item -> item * item)
                .filter(item -> item % 2 != 0)
                .blockLast();
    }


    @Test
    public void testPublishOn() {
        Flux.range(0, 10) //  FluxRange
                .publishOn(Schedulers.elastic()) // FluxPublishOn
                .map(item -> item + "") // FluxMapFuseable
                .blockLast();
    }

    @Test
    public void testScheduler() {
        Flux.range(0, 10) // FluxRange
                .map(item -> item + "") // FluxMapFuseable
                .subscribeOn(Schedulers.elastic())
                .blockLast();
    }

    @Test
    public void testDelayElements() {
        Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1))
                .log()
                .blockLast();
    }
}
