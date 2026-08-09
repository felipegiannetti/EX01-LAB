package parteE;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Atende 100.000 clientes usando uma Virtual Thread por tarefa
 * ({@link Executors#newVirtualThreadPerTaskExecutor()}).
 * <p>
 * Repete o mesmo tipo de experimento da Parte C (que quebrava por volta de
 * 50.000 threads de SO), mas sem {@code OutOfMemoryError}, pois as Virtual
 * Threads sao gerenciadas pela JVM e multiplexadas sobre poucas threads de
 * SO reais (carrier threads).
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Padrão 100_000 (conforme o roteiro). Pode ser sobrescrito via argumento.
        int total = args.length > 0 ? Integer.parseInt(args[0]) : 100_000;

        AtomicBoolean eraVirtual = new AtomicBoolean(true);

        long inicio = System.currentTimeMillis();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < total; i++) {
                int idCliente = i + 1;
                executor.submit(() -> {
                    // Exercicio de fixacao: confirmar que a thread atual e uma Virtual Thread.
                    if (idCliente == 1) {
                        Thread atual = Thread.currentThread();
                        System.out.println("Thread.currentThread() = " + atual);
                        System.out.println("isVirtual() = " + atual.isVirtual());
                        eraVirtual.set(atual.isVirtual());
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } // o try-with-resources aguarda todas as tarefas terminarem antes de sair

        long fim = System.currentTimeMillis();
        System.out.println("Atendimentos: " + total);
        System.out.println("Era Virtual Thread? " + eraVirtual.get());
        System.out.println("Tempo total (Parte E - Virtual Threads): " + (fim - inicio) + "ms");
    }
}
