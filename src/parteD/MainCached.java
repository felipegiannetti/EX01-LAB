package parteD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Exercicio de fixacao: trocar o pool fixo por newCachedThreadPool().
public class MainCached {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newCachedThreadPool();

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            int idCliente = i + 1;
            pool.submit(() -> {
                System.out.println(Thread.currentThread().getName() + " atendendo cliente " + idCliente);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        long fim = System.currentTimeMillis();
        System.out.println("Tempo total (Parte D - newCachedThreadPool, ate 10 threads sob demanda): " + (fim - inicio) + "ms");
    }
}
