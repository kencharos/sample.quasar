package sample;

import co.paralleluniverse.fibers.*;
import co.paralleluniverse.strands.Strand;
import co.paralleluniverse.strands.Stranded;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


abstract  class LongTask extends FiberAsync<String, RuntimeException> {

    private static final String LINE = IntStream.range(0, 2024).boxed()
            .map(i -> "0").collect(Collectors.joining());

    private static final List<String> LINES = IntStream.range(0, 5000)
            .boxed().map(i -> LINE).collect(Collectors.toList());


    // 非同期処理の結果として、 result をセット。
    protected void process(String input, int weight) {
        try {
            Files.write(Paths.get("/tmp/" + input),  LINES.subList(0, weight));
            Files.delete(Paths.get("/tmp/" + input));
            asyncCompleted(input + input);
        } catch (IOException e) {
            asyncFailed(e);
        }
    }


    // 指定時間待機し、結果を記録
    public static LongTask of(String initialState, int weight) {
        return  new LongTask() {
            @Override
            protected void requestAsync(){
                process(initialState, weight);
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



        Random r = new Random();
        for(Fiber<String> f : IntStream.range(0, 100).boxed()
                .map(i -> createTask("Task_" + i, "Ans_"+i, r.nextInt(5000) , r.nextInt(5000)))
                .collect(Collectors.toList()))
        {

            System.out.println("\t finished, " + f.get());

        }

    }

    /**
     * async/awati ライク に、2つの非同期処理を組み合わせて、1つのタスクを作るサンプル。
     */
    private static Fiber<String> createTask(String id, String initialState, int weight1, int weight2) {
        return new Fiber<String>() {

            @Override
            protected String run() throws SuspendExecution, InterruptedException {

                // 非同期タスクを生成して、同期的な書き方で実行できる。
                String t1Res = LongTask.of(initialState, weight1).run();
                System.out.println(id + " first finish, result=" + t1Res);

                String t2Res = LongTask.of(t1Res, weight2).run();
                System.out.println( id + " second finish, result=" + t2Res);

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
