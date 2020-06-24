package reactor.core.processors;

/**
 *
 * EmitterProcessor <br>
 * 1. 可以向多个订阅者发送数据，并对每一个订阅者进行背压处理 <br>
 * 2. 本身可以订阅Publisher 并同步数据 <br>
 * 3. 支持缓存 <br>
 * 4. 推送过的数据不再重复推送给后来的订阅者 <br>
 * 5. 所有订阅者的订阅都取消后，默认清空内部缓存并不再接收订阅者 <br>
 * @author lianghong
 * @date 2020/6/12
 */
public class EmitterProcessorTest {

    // TODO

}
