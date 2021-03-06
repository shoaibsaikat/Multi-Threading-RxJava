package com.example.rxjavawithcallback;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ProgressBar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final static int INVALID = -1;

    private interface ITaskCallback {
        void setUpdate(Integer progress);
    }

    /**
     * ProgressEmitter is basically the callback registered to get the progress
     * It also emits onNext, onComplete, onError for the Observer to get the update
     */
    private class ProgressEmitter implements ITaskCallback {
        final private ObservableEmitter<Integer> emitter;

        private ProgressEmitter(ObservableEmitter<Integer> emitter) {
            this.emitter = emitter;
        }

        @Override
        public void setUpdate(Integer progress) {
            if (progress.intValue() >= 0) {
                emitter.onNext(progress);
            } else if(progress.intValue() == 100) {
                emitter.onNext(progress);
                emitter.onComplete();
            } else {
                emitter.onError(new Exception(progress.toString()));
            }
        }
    }

    /**
     * ProgressObserver is the Observer responsible to update UI with the progress
     */
    private class ProgressObserver extends DisposableObserver<Integer> {

        private ProgressBar pb;

        private ProgressObserver(ProgressBar pb) {
            this.pb = pb;
        }

        @Override
        public void onNext(@NonNull Integer integer) {
            pb.setProgress(integer.intValue());
        }

        @Override
        public void onError(@NonNull Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    }

    private int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private class ObservableTask {

        /**
         * This function returns the Observable to the registered Observer and calls update function
         * It also creates and passes the Callback needed in update function
         * @return : Observable with progress amount
         */
        public Observable<Integer> observableUpdate() {
            return Observable.create(new ObservableOnSubscribe<Integer>() {
                @Override
                public void subscribe(@NonNull ObservableEmitter<Integer> emitter) throws Throwable {
                    update(new ProgressEmitter(emitter));
                }
            });
        }

        /**
         * This is the function, whose progress we need to show
         * @param callback : It's the callback, whom progress is returned
         */
        public void update(ITaskCallback callback) {
            for (int i = 0; i <= 100; i += 10) {
                try {
                    Thread.sleep(getRandomNumber(10, 1000));
                    callback.setUpdate(Integer.valueOf(i));
                } catch (Exception e) {
                    System.out.println(e.getStackTrace().toString());
                    callback.setUpdate(Integer.valueOf(INVALID));
                }
            }
        }
    }

    private ProgressBar pb1;
    private ProgressBar pb2;
    private ExecutorService executorService;

    private ObservableTask task1;
    private ObservableTask task2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pb1 = findViewById(R.id.progressBar1);
        pb2 = findViewById(R.id.progressBar2);

        pb1.setProgress(0);
        pb2.setProgress(0);

        executorService = new ThreadPoolExecutor(4, 5, 60L, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());

        task1 = new ObservableTask();
        task2 = new ObservableTask();

        task1.observableUpdate()
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ProgressObserver(pb1));

        task2.observableUpdate()
                .subscribeOn(Schedulers.from(executorService))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new ProgressObserver(pb2));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        executorService.shutdown();
    }
}