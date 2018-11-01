package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class SubscribeExample {


    class SomeApi {
        Random random = new Random();

        Observable<Integer> randomInt() {
            return Observable.just(random.nextInt());
        }
    }

    @Test
    void testGetRandom() {
        SomeApi someApi = new SomeApi();

        someApi.randomInt()
                .repeat(10)
                .subscribe(System.out::println);
    }


}
