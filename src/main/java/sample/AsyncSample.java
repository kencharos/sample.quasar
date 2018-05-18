package sample;

import co.paralleluniverse.fibers.Fiber;
import co.paralleluniverse.fibers.FiberUtil;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;
import co.paralleluniverse.fibers.FiberAsync;
import co.paralleluniverse.fibers.SuspendExecution;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;


abstract  class LongTask extends FiberAsync<String, RuntimeException> {

    // 非同期処理の結果として、 result をセット。
    protected void successAfter(String result, int wait) {
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        asyncCompleted(result);
    }


    // 指定時間待機し、結果を記録
    public static LongTask of(String initialState, int wait) {
        return  new LongTask() {
            @Override
            protected void requestAsync(){
                successAfter(initialState + initialState, wait);
            }
        };
    }
}


/*
 OUTPUT
 TaskB, processed after Task1, result=B1B1
 TaskB, processed after Task2 result=B1B1B1B1
 TaskC, processed after Task1, result=C1C1
 TaskA, processed after Task1, result=A1A1
 TaskC, processed after Task2 result=C1C1C1C1
 TaskA, processed after Task2 result=A1A1A1A1
 A1A1A1A1
 B1B1B1B1
 C1C1C1C1
 */
public class AsyncSample {

    public static void main(String[] args) throws  Exception{

        for(Fiber<String> f : Arrays.asList(
                createTask("TaskA","A1", 500, 500), //4,5
                createTask("TaskB","B1", 10, 10), // 1,2
                createTask("TaskC","C1", 400, 1000))) //3,6
        {

            System.out.println(f.get());

        }

    }

    /**
     * async/awati ライク に、2つの非同期処理を組み合わせて、1つのタスクを作るサンプル。
     */
    private static Fiber<String> createTask(String id, String initialState, int wait1, int wait2) {
        return new Fiber<String>() {

            @Override
            protected String run() throws SuspendExecution, InterruptedException {

                // 非同期タスクを生成して、同期的な書き方で実行できる。
                String t1Res = LongTask.of(initialState, wait1).run();
                System.out.println(id + ", processed after Task1, result=" + t1Res);

                String t2Res = LongTask.of(t1Res, wait2).run();
                System.out.println( id +  ", processed after Task2, result=" + t2Res);

                return t2Res;

            }
        }.start();
    }

    /**
     * Sample two task composition by CompletebleFuteur and lambda.
     */
    private static CompletableFuture<String> composeTask(String id, String initialState, int wait1, int wait2) {
        return CompletableFuture.supplyAsync(() -> task(initialState))
                .thenApply(t1Res -> {
                    System.out.println(id + ", processed after Task1, result=" + t1Res);
                    return task((t1Res));
                })
                .thenApply(t2Res -> {
                    System.out.println(id + ", processed after Task2, result=" + t2Res);
                    return task((t2Res));
                });

    }

    private static String task(String s) {
        return s + s;
    }
}
