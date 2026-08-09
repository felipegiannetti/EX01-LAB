package parteC;

/**
 * Cria N threads de Sistema Operacional (uma por tarefa, sem pool) para
 * demonstrar, na pratica, o custo de memoria desse modelo e o limite que
 * ele encontra em escala.
 * <p>
 * O total padrao (10.000) e o mesmo citado no roteiro. Um valor maior pode
 * ser passado por argumento para tentar reproduzir o
 * {@code OutOfMemoryError: unable to create new native thread} — use com
 * cautela, pois isso consome memoria real da maquina.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Padrão 10_000 (conforme o roteiro). Pode ser sobrescrito via argumento,
        // ex.: java -cp out parteC.Main 50000 (cuidado: pode lançar OutOfMemoryError).
        int total = args.length > 0 ? Integer.parseInt(args[0]) : 10_000;

        Thread[] threads = new Thread[total];

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < total; i++) {
            threads[i] = new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        System.out.println("Threads de SO criadas: " + total);
        System.out.println("Tempo total (Parte C - muitas threads): " + (fim - inicio) + "ms");
    }
}
