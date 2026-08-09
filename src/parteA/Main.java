package parteA;

/**
 * Dispara 5 atendimentos concorrentes usando {@code extends Thread}.
 * <p>
 * O programa usa deliberadamente dois lacos separados: o primeiro apenas
 * chama {@code start()} em todas as threads (dispara os 5 atendimentos em
 * paralelo); o segundo chama {@code join()} em cada uma (espera todas
 * terminarem). Se {@code start()} e {@code join()} estivessem juntos no
 * mesmo laco, o comportamento seria sequencial em vez de paralelo — veja a
 * resposta detalhada no RELATORIO.md.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        int totalClientes = 5;
        AtendimentoThread[] threads = new AtendimentoThread[totalClientes];

        long inicio = System.currentTimeMillis();

        for (int i = 0; i < totalClientes; i++) {
            threads[i] = new AtendimentoThread(i + 1);
            threads[i].setName("Atendente-" + (i + 1));
            threads[i].start(); // nunca t.run()
        }

        for (AtendimentoThread t : threads) {
            t.join();
        }

        long fim = System.currentTimeMillis();
        System.out.println("Tempo total (Parte A - extends Thread): " + (fim - inicio) + "ms");
    }
}
