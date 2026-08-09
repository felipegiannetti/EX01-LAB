package parteD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Atende 10 clientes usando um pool fixo de 4 threads reais
 * ({@link Executors#newFixedThreadPool(int)}).
 * <p>
 * Como o pool tem no maximo 4 threads simultaneas, as 10 tarefas de 1s cada
 * sao processadas em ~3 "levas" (4 + 4 + 2), resultando em ~3s de tempo
 * total — bem diferente do ~1s obtido nas Partes A/B com 5 threads dedicadas,
 * e o preco pago para nao esgotar a memoria do processo como na Parte C.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);

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
        System.out.println("Tempo total (Parte D - ExecutorService fixo, 4 threads): " + (fim - inicio) + "ms");
    }
}
