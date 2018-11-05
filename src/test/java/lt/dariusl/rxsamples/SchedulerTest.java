package lt.dariusl.rxsamples;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.TestScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

public class SchedulerTest {

    interface Clock {
        long getTime();
    }

    interface TimeListener {
        void update(long time);
    }

    class Timer{
        private final Clock clock;
        private final TimeListener listener;
        private final Scheduler delayScheduler;
        private Disposable disposable;

        Timer(Clock clock, TimeListener listener, Scheduler delayScheduler) {
            this.clock = clock;
            this.listener = listener;
            this.delayScheduler = delayScheduler;
        }

        void start() {
            disposable = Observable.interval(500, TimeUnit.MILLISECONDS, delayScheduler)
                    .map(n -> clock.getTime())
                    .subscribe(listener::update);
        }

        void stop() {
            disposable.dispose();
        }
    }

    private long time = 0L;

    private Clock mockClock = () -> time;
    private CountingListener listener = new CountingListener();
    private TestScheduler scheduler = new TestScheduler(0, TimeUnit.MILLISECONDS);

    @Test
    void updates10TimesIn5Seconds() {
        Timer timer = new Timer(mockClock, listener, scheduler);
        timer.start();
        Assertions.assertEquals(0, listener.count);
        advanceTimeBy(5 * 1000);
        Assertions.assertEquals(10, listener.count);
    }

    private void advanceTimeBy(long millis) {
        time += millis;
        scheduler.advanceTimeTo(time, TimeUnit.MILLISECONDS);
    }

    class CountingListener implements TimeListener {
        int count = 0;

        @Override
        public void update(long time) {
            count++;
        }
    }
}
