package parteA;

/**
 * Representa o atendimento de um cliente no guiche, executado como uma
 * thread de Sistema Operacional dedicada.
 * <p>
 * Por herdar diretamente de {@link Thread}, esta classe ja "e" uma thread
 * (nao apenas uma tarefa) e por isso nao pode herdar de nenhuma outra
 * classe alem de {@code Thread} — essa e a principal desvantagem frente
 * ao modelo {@code implements Runnable} da Parte B.
 */
public class AtendimentoThread extends Thread {

    private final int idCliente;

    public AtendimentoThread(int idCliente) {
        this.idCliente = idCliente;
    }

    /** Simula o atendimento: identifica a thread e "trabalha" por 1 segundo. */
    @Override
    public void run() {
        System.out.println(getName() + " atendendo cliente " + idCliente);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(getName() + " finalizou o atendimento do cliente " + idCliente);
    }
}
