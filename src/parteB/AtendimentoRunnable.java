package parteB;

/**
 * Representa a tarefa de atender um cliente, desacoplada de qual
 * {@link Thread} vai executa-la.
 * <p>
 * Por implementar apenas a interface {@link Runnable}, esta classe continua
 * livre para herdar de qualquer outra classe caso um dia precise (ex.:
 * {@code Funcionario}) — o que nao seria possivel no modelo da Parte A.
 */
public class AtendimentoRunnable implements Runnable {

    private final int idCliente;

    public AtendimentoRunnable(int idCliente) {
        this.idCliente = idCliente;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " atendendo cliente " + idCliente);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println(Thread.currentThread().getName() + " finalizou o atendimento do cliente " + idCliente);
    }
}
