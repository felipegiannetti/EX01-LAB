package extra;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Benchmark comparativo entre os 4 modelos de concorrencia estudados no
 * roteiro (Partes A/C, D e E), executando a mesma quantidade de "tarefas"
 * (atendimentos) em cada um e medindo tempo total e o pico de threads de
 * plataforma (SO) que cada modelo manteve vivas ao mesmo tempo, via
 * {@link ThreadMXBean}.
 * <p>
 * Isto endereca diretamente o objetivo 04 do roteiro ("observar na pratica
 * o custo de criar milhares de threads de SO"), mas de forma comparativa:
 * em vez de rodar cada parte isolada, roda todas com a mesma carga de
 * trabalho, lado a lado.
 * <p>
 * Nota: {@code ThreadMXBean} conta threads de <b>plataforma</b> (SO). Virtual
 * Threads normalmente nao aparecem nessa contagem — o proprio resultado (o
 * pico quase nao mudar no cenario de Virtual Threads) e uma evidencia de que
 * elas nao sao threads de SO tradicionais.
 */
public class Comparativo {

    private static final int TOTAL_TAREFAS = 2000;
    private static final int DURACAO_TAREFA_MS = 200;
    private static final ThreadMXBean THREAD_BEAN = ManagementFactory.getThreadMXBean();

    public static void main(String[] args) throws InterruptedException {
        int baseline = THREAD_BEAN.getThreadCount();

        System.out.println("Tarefas por cenario: " + TOTAL_TAREFAS
                + " | duracao de cada tarefa: " + DURACAO_TAREFA_MS + "ms"
                + " | threads de plataforma ja ativas na JVM (baseline): " + baseline);
        System.out.println();
        System.out.printf("%-30s | %12s | %20s%n", "Modelo", "Tempo(ms)", "Pico threads SO (delta)");
        System.out.println("-".repeat(68));

        medir("1 thread de SO por tarefa", Comparativo::umaThreadPorTarefa, baseline);
        medir("ExecutorService fixo (200)", Comparativo::poolFixo, baseline);
        medir("ExecutorService cached", Comparativo::poolCached, baseline);
        medir("Virtual Threads", Comparativo::virtualThreads, baseline);
    }

    @FunctionalInterface
    private interface Cenario {
        void executar(AtomicInteger pico) throws InterruptedException;
    }

    private static void medir(String nome, Cenario cenario, int baseline) throws InterruptedException {
        AtomicInteger pico = new AtomicInteger(baseline);
        long inicio = System.currentTimeMillis();
        cenario.executar(pico);
        long fim = System.currentTimeMillis();
        System.out.printf("%-30s | %12d | %20d%n", nome, fim - inicio, pico.get() - baseline);
    }

    private static void atualizarPico(AtomicInteger pico) {
        pico.updateAndGet(atual -> Math.max(atual, THREAD_BEAN.getThreadCount()));
    }

    private static void umaThreadPorTarefa(AtomicInteger pico) throws InterruptedException {
        Thread[] threads = new Thread[TOTAL_TAREFAS];
        for (int i = 0; i < TOTAL_TAREFAS; i++) {
            threads[i] = new Thread(Comparativo::dormir);
            threads[i].start();
        }
        atualizarPico(pico);
        for (Thread t : threads) {
            t.join();
        }
    }

    private static void poolFixo(AtomicInteger pico) throws InterruptedException {
        try (ExecutorService pool = Executors.newFixedThreadPool(200)) {
            for (int i = 0; i < TOTAL_TAREFAS; i++) {
                pool.submit(Comparativo::dormir);
            }
            atualizarPico(pico);
        }
    }

    private static void poolCached(AtomicInteger pico) throws InterruptedException {
        try (ExecutorService pool = Executors.newCachedThreadPool()) {
            for (int i = 0; i < TOTAL_TAREFAS; i++) {
                pool.submit(Comparativo::dormir);
            }
            atualizarPico(pico);
        }
    }

    private static void virtualThreads(AtomicInteger pico) throws InterruptedException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < TOTAL_TAREFAS; i++) {
                executor.submit(Comparativo::dormir);
            }
            atualizarPico(pico);
        }
    }

    private static void dormir() {
        try {
            Thread.sleep(DURACAO_TAREFA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
