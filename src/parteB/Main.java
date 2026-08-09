package parteB;

/**
 * Dispara 5 atendimentos concorrentes usando {@code implements Runnable}.
 * <p>
 * Cada {@link AtendimentoRunnable} (a tarefa) e entregue a uma
 * {@link Thread} nova ({@code new Thread(tarefa, nome)}) que apenas a
 * executa. Assim como na Parte A, {@code start()} e {@code join()} ficam em
 * lacos separados para manter os 5 atendimentos em paralelo.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        int totalClientes = 5;
        Thread[] threads = new Thread[totalClientes];

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < totalClientes; i++) {
            Runnable tarefa = new AtendimentoRunnable(i + 1);
            threads[i] = new Thread(tarefa, "Atendente-" + (i + 1));
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        System.out.println("Tempo total (Parte B - implements Runnable): " + (fim - inicio) + "ms");
    }
}
