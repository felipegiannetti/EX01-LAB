/**
 * Parte E do roteiro — forma atual #2 (norma atual): Virtual Threads.
 * <p>
 * Threads leves gerenciadas pela propria JVM (Java 21+), multiplexadas sobre
 * um numero pequeno de "carrier threads" de SO. Permite centenas de milhares
 * de tarefas concorrentes sem o custo de memoria de threads de SO.
 */
package parteE;
