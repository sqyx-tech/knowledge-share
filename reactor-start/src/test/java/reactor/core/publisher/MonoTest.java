package reactor.core.publisher;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

/**
 * @author lanruqi
 * @date 2020/6/8
 */
public class MonoTest {

    @Test
    @DisplayName("创建空的Mono")
    public void monoEmpty() {
        Mono<Object> empty = Mono
                .empty()
                .log();
        StepVerifier.create(empty)
                .verifyComplete();
    }

    @Test
    @DisplayName("创建错误流Mono")
    public void monoError() {
        Mono<Object> empty = Mono
                .error(new Throwable())
                .log();
        StepVerifier.create(empty)
                .expectError()
                .verify();
    }
    @Test
    @DisplayName("创建有一个元素的Mono")
    public void monoElement() {
        Mono<String> data = Mono
                .just("data")
                .log();
        StepVerifier.create(data).expectNext("data")
                .verifyComplete();
    }
}
