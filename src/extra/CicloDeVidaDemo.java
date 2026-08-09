package extra;

/**
 * Demonstra, com {@link Thread#getState()} real, as transicoes de estado do
 * ciclo de vida de uma thread descritas na revisao conceitual do roteiro:
 * {@code NEW -> RUNNABLE -> TIMED_WAITING -> TERMINATED}.
 * <p>
 * Nao faz parte das Partes A-E do roteiro; e um complemento para provar na
 * pratica o diagrama de estados apresentado nos slides.
 */
public class CicloDeVidaDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Atendente-demo");

        System.out.println("1) Antes de start()........... " + t.getState());

        t.start();
        System.out.println("2) Logo apos start()........... " + t.getState());

        // da tempo da thread entrar no Thread.sleep(1000) do run()
        Thread.sleep(200);
        System.out.println("3) Durante o sleep(1000) dela.. " + t.getState());

        t.join();
        System.out.println("4) Depois de join()............ " + t.getState());
    }
}
