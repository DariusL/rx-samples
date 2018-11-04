package lt.dariusl.rxsamples.btexample;

import io.reactivex.Observable;

public interface Device {
    Observable<ConnectionState> observeConnection();
    Observable<DataPoint> readStream();
    Observable<Void> start();
    Observable<Void> stop();
    void release();
}

