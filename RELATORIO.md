# Relatório — Roteiro de Laboratório: Threads em Java

Respostas às perguntas de cada parte do roteiro, aos exercícios de fixação e
um guia de preparação para perguntas do professor. O código correspondente
está explicado em detalhe no [README.md](README.md).

## Revisão conceitual — Processo x Thread

O SO cria um **processo** para cada programa (no nosso caso, a JVM). Toda
**thread** que criamos em Java é mapeada para uma thread nativa do SO
(exceto as Virtual Threads da Parte E, que são gerenciadas pela JVM).

| Aspecto | Processo | Thread |
|---|---|---|
| Memória | Espaço de endereçamento próprio | Compartilha a memória do processo pai |
| Criação | Custosa (nova tabela de páginas, novo PID) | Mais leve |
| Comunicação | Precisa de IPC (pipes, sockets) | Variáveis compartilhadas |
| Isolamento | Falha em um processo não derruba outro | Exceção não tratada pode afetar o processo todo |
| Escalonamento | Unidade de alocação de recursos | Unidade que a CPU de fato escalona |

**Analogia do guichê de atendimento** (exercício de fixação): o
**processo** é o prédio da agência — tem um endereço próprio, paredes
próprias (memória isolada), e se o prédio pegar fogo (crash), só aquela
agência para. As **threads** são os atendentes dentro do prédio — todos
compartilham o mesmo espaço, os mesmos arquivos e o mesmo balcão (memória
compartilhada do processo), conseguem se comunicar rapidamente só falando
entre si (sem precisar de "correio interno" como fariam dois prédios
diferentes), mas se um atendente entrar em pânico e causar um incêndio
dentro do prédio (exceção não tratada / corrupção de estado), ele pode
prejudicar o atendimento dos outros atendentes daquele mesmo prédio.
"Contratar" um atendente novo (criar uma thread) é bem mais rápido e barato
do que abrir uma agência nova (criar um processo).

## Parte A — `extends Thread`

**Pergunta: o tempo total ficou perto de 1s ou de 5s com 5 atendimentos?
Por quê?**

Ficou perto de **1s** (medido: ~1018ms). Isso acontece porque o `Main`
primeiro chama `start()` em todas as 5 threads, **e só depois** chama
`join()` em cada uma delas, em um segundo laço separado. Como `start()`
pede ao SO para criar uma thread nova imediatamente, as 5 threads passam a
existir e a dormir (`Thread.sleep(1000)`) **ao mesmo tempo** — em paralelo.
O tempo total do programa é então o tempo da thread mais lenta, não a soma
de todas. Se `start()` e `join()` estivessem no mesmo laço (chamando
`join()` logo depois de cada `start()`), o programa seria efetivamente
sequencial — esperaria a thread 1 terminar antes de sequer criar a thread
2 — e o tempo total seria perto de **5s**.

## Parte B — `implements Runnable`

**Pergunta: qual das duas classes (Parte A ou B) você poderia fazer herdar
de outra classe hoje?**

Somente a `AtendimentoRunnable` (Parte B). Como ela apenas *implementa* a
interface `Runnable`, ela continua livre para *estender* qualquer outra
classe (Java permite implementar várias interfaces, mas só herdar de uma
única superclasse). Já `AtendimentoThread` (Parte A) já gastou sua única
herança disponível estendendo `Thread`, então não pode herdar de mais
nenhuma outra classe — por exemplo, ela não poderia herdar de
`Funcionario` ao mesmo tempo.

## Parte C — Muitas threads de Sistema Operacional

**Pergunta: por que criar uma thread de SO é mais caro do que criar um
objeto comum em Java?**

Um objeto comum em Java é só memória alocada no heap da própria JVM —
rápido e barato. Uma thread de SO, por outro lado, exige que o **kernel**
faça um trabalho bem mais pesado: reservar uma pilha (stack) própria
(tipicamente 512KB–1MB), criar estruturas internas de controle do
escalonador do SO (registradores, contexto de execução, prioridade etc.) e
registrar essa thread na tabela de threads do sistema. Trocar de contexto
entre threads de SO também tem custo (salvar/restaurar registradores),
enquanto criar um objeto Java comum não envolve o kernel em nada.

**Pergunta: o que esse limite sugere sobre usar 1 thread por requisição em
um servidor web?**

Sugere que esse modelo **não escala** para servidores com muitas conexões
simultâneas. Se cada requisição HTTP criar (ou consumir) uma thread de SO
dedicada, um servidor sob carga alta (milhares de conexões simultâneas,
comum em sistemas web modernos) rapidamente esgota a memória disponível
para pilhas de thread e pode lançar
`OutOfMemoryError: unable to create new native thread` — derrubando o
servidor mesmo que a CPU e a memória "de negócio" ainda tivessem folga. É
exatamente esse problema que motivou a evolução para pools de threads
(Parte D) e, mais recentemente, Virtual Threads (Parte E), que permitem
"1 thread por requisição" **sem** o custo de "1 thread de SO por
requisição".

## Parte D — `ExecutorService` (pool de threads)

**Pergunta: com 4 threads atendendo 10 clientes, o tempo total ficou perto
de 1s, 2s ou 3s?**

Ficou perto de **3s** (medido: ~3032ms). Com um pool fixo de 4 threads e 10
tarefas de 1s cada, as tarefas são processadas em levas: a primeira leva
usa as 4 threads para atender os clientes 1–4 (1s), a segunda leva atende
5–8 (mais 1s) e a terceira leva atende 9–10 (mais 1s) — totalizando ~3
"rodadas" de 1s, ou seja, ~3s. Isso é o comportamento esperado de
`Executors.newFixedThreadPool(4)`: no máximo 4 tarefas rodam ao mesmo
tempo, as demais ficam enfileiradas até uma thread do pool ficar livre.

## Parte E — Virtual Threads (Java 21+)

**Pergunta: uma Virtual Thread é uma thread de Sistema Operacional? Se
não, o que ela é?**

Não. Uma Virtual Thread é um objeto leve **gerenciado pela própria JVM**,
não pelo kernel do SO. Ela ainda é representada por uma instância de
`java.lang.Thread` (por isso o código de negócio não muda — continuamos
usando `Runnable`, `Thread.sleep()` etc.), mas por baixo dos panos a JVM
mantém um número pequeno de threads de SO reais chamadas **carrier
threads**, que "emprestam" tempo de execução para as Virtual Threads
quando elas realmente precisam rodar código. Quando uma Virtual Thread
bloqueia em uma operação como `Thread.sleep()` ou uma chamada de I/O, a
JVM desmonta essa Virtual Thread da carrier thread, liberando a carrier
thread para executar outra Virtual Thread — por isso é possível ter
centenas de milhares (ou milhões) de Virtual Threads "vivas" ao mesmo
tempo, mesmo com poucas threads de SO reais no total. Confirmamos isso no
código executando `Thread.currentThread().isVirtual()`, que retornou
`true`, e rodando 100.000 tarefas concorrentes (o mesmo volume que
quebraria o modelo da Parte C) sem `OutOfMemoryError`, em ~1,4s.

## Exercícios de fixação

**1. Trocar o pool fixo por `newCachedThreadPool()` — o comportamento
muda?**

Sim. Implementado em [`parteD/MainCached.java`](src/parteD/MainCached.java).
Enquanto `newFixedThreadPool(4)` limita a **no máximo 4** threads
simultâneas (forçando as 10 tarefas a rodar em ~3 levas, ~3s no total),
`newCachedThreadPool()` **não tem limite fixo**: ele cria uma thread nova
para cada tarefa sempre que não há uma thread ociosa disponível para
reaproveitar, e mantém threads ociosas por até 60 segundos antes de
descartá-las. Com as 10 tarefas do exercício, isso resultou em até 10
threads (`pool-1-thread-1` até `pool-1-thread-10`) rodando **todas ao
mesmo tempo**, e o tempo total caiu para ~1s. A troca mostra na prática o
trade-off: `newFixedThreadPool` protege o sistema de criar threads demais
(bom sob carga alta e imprevisível), enquanto `newCachedThreadPool` prioriza
throughput quando as tarefas são curtas e o volume é controlado — mas sob
uma rajada muito grande de tarefas, `newCachedThreadPool` pode reproduzir o
mesmo problema de escala da Parte C, já que continua criando threads de SO
reais.

**2. Imprimir `Thread.currentThread()` na Parte E — é uma
`VirtualThread`?**

Sim. A saída observada foi:

```
Thread.currentThread() = VirtualThread[#28]/runnable@ForkJoinPool-1-worker-1
isVirtual() = true
```

O nome da classe (`VirtualThread`) e o retorno `true` de `isVirtual()`
confirmam que a tarefa está de fato rodando dentro de uma Virtual Thread,
não de uma thread de SO tradicional (que apareceria como algo do tipo
`Thread[#N,...]` e retornaria `isVirtual() == false`). O sufixo
`@ForkJoinPool-1-worker-1` mostra qual **carrier thread** (thread de SO
real) está emprestando tempo de execução para essa Virtual Thread naquele
instante.

**3. Explicar processo x thread com a analogia do guichê de
atendimento**

Ver seção "Revisão conceitual" acima.

**4. Escolher, com justificativa, a abordagem ideal para um servidor com
milhares de conexões**

**Virtual Threads** (o modelo da Parte E) é a escolha ideal hoje para esse
cenário. Um servidor com milhares de conexões simultâneas precisa de
milhares de "linhas de execução" concorrentes (idealmente uma por
conexão/requisição, para manter o código simples e sequencial de ler). O
modelo `extends Thread` / `implements Runnable` "puro" (Partes A e B) não
escala — replicaria o problema visto na Parte C, arriscando
`OutOfMemoryError: unable to create new native thread` bem antes de chegar
a "milhares" de conexões. Um `ExecutorService` com pool fixo (Parte D)
resolve o problema de estourar a memória, mas troca isso por **filas de
espera**: com um pool pequeno e milhares de requisições, a maioria delas
ficaria enfileirada, aumentando a latência percebida pelo cliente. Virtual
Threads entregam o melhor dos dois mundos: continuamos escrevendo código
simples (uma Virtual Thread por requisição, estilo "thread por conexão"),
mas sem o custo de memória de threads de SO, porque a JVM multiplexa um
número pequeno de carrier threads reais entre centenas de milhares de
Virtual Threads, aproveitando os momentos em que elas estão bloqueadas
(esperando I/O, banco de dados, etc.) para dar espaço a outras.

## Diferenciais em relação ao roteiro

Além das 5 partes e dos 2 exercícios de fixação pedidos, o projeto inclui
dois extras (pacote [`extra`](src/extra)), documentados em detalhe no
[README.md](README.md#extras-indo-além-do-roteiro):

- **`CicloDeVidaDemo`**: prova em código, com `Thread.getState()` real, o
  diagrama de estados `NEW → RUNNABLE → TIMED_WAITING → TERMINATED` que o
  roteiro apresenta só como conceito.
- **`Comparativo`**: benchmark que roda a mesma carga de trabalho (2.000
  tarefas) nos 4 modelos de concorrência do roteiro e mede tempo total +
  pico de threads de SO (via `ThreadMXBean`) de cada um, lado a lado —
  dados empíricos reais desta máquina, não só a citação do roteiro.

A motivação de ambos foi atacar o objetivo 04 do roteiro ("observar na
prática o custo de criar milhares de threads de SO") de forma mais
completa e quantitativa do que rodar cada parte isoladamente.

## Checklist de entrega

- [x] As 5 partes (A a E) compilam e executam sem erro (`javac`, testado
      com JDK 26).
- [x] As perguntas de cada parte estão respondidas neste relatório.
- [x] Ver seção seguinte para se preparar a explicar processo x thread sem
      consultar o roteiro.
- [x] Extras opcionais (ciclo de vida ao vivo + benchmark comparativo)
      implementados e com resultados documentados.

---

## Guia rápido para perguntas do professor

Perguntas curtas e diretas que costumam ser feitas em cima desse roteiro,
com a resposta objetiva:

**P: Qual a diferença entre processo e thread?**
R: Processo tem memória própria e isolada; thread compartilha a memória do
processo que a criou. Processo é a unidade de alocação de recursos do SO;
thread é a unidade que o SO efetivamente escalona na CPU.

**P: Por que `start()` e não `run()`?**
R: `start()` pede ao SO para criar uma thread de execução nova e, dentro
dela, o `run()` é chamado. Chamar `run()` diretamente só executa aquele
código como um método comum, na thread atual — sem nenhuma concorrência
nova.

**P: Por que `implements Runnable` é preferível a `extends Thread`?**
R: Porque separa "o que fazer" (a tarefa) de "quem executa" (a thread),
evita gastar a única herança disponível da classe, permite reaproveitar a
mesma tarefa em várias threads e é compatível com `ExecutorService`
(que espera um `Runnable`/`Callable`, não uma subclasse de `Thread`).

**P: Para que serve `join()`?**
R: Faz a thread que chamou `join()` esperar a outra thread terminar antes
de continuar. Sem isso, o programa principal pode terminar (ou seguir em
frente) antes que o trabalho da(s) outra(s) thread(s) esteja concluído.

**P: Por que criar muitas threads de SO quebra o programa?**
R: Cada thread de SO reserva memória de pilha própria (centenas de KB a
1MB) e estruturas de kernel. Milhares delas somam vários GB de memória
reservada, até estourar o limite do processo/SO e lançar
`OutOfMemoryError: unable to create new native thread`.

**P: O que um `ExecutorService` resolve?**
R: Evita criar uma thread de SO nova por tarefa. Um número fixo (ou
controlado) de threads reais é reaproveitado para processar quantas
tarefas forem submetidas, enfileirando o excedente.

**P: Por que é preciso chamar `shutdown()` num `ExecutorService`?**
R: Porque, por padrão, o pool fica vivo esperando novas tarefas
indefinidamente. `shutdown()` avisa que não virão mais tarefas novas (mas
deixa terminar as que já estão na fila); sem chamar isso, o programa nunca
encerra sozinho.

**P: O que é uma Virtual Thread e por que ela resolve o problema da Parte
C?**
R: É uma thread leve gerenciada pela JVM (não pelo SO). Muitas Virtual
Threads compartilham um número pequeno de threads de SO reais (carrier
threads), que só são "ocupadas" enquanto a Virtual Thread está realmente
executando código — quando ela bloqueia (ex.: `sleep`, I/O), a carrier
thread fica livre para outra Virtual Thread. Isso permite centenas de
milhares de Virtual Threads sem esgotar a memória do processo.

**P: Uma Virtual Thread é uma `Thread` de verdade (do ponto de vista da
API Java)?**
R: Sim — `Thread.currentThread()` retorna um objeto `Thread` normalmente,
e o método `isVirtual()` indica se aquela instância é uma Virtual Thread
ou uma thread de plataforma (SO) tradicional. O código de negócio
(`Runnable`, `run()`, `Thread.sleep()`) não muda entre os dois modelos.

**P: Em que ordem evoluiu a concorrência em Java?**
R: `extends Thread` (Java 1.0) → `implements Runnable` (desde sempre,
recomendado a partir do Java 5) → `ExecutorService`/pool de threads (Java
5+) → Virtual Threads (Java 21+, norma atual).

**P: Por que você fez mais do que o roteiro pedia?**
R: Para provar os conceitos na prática em vez de só descrevê-los. O
roteiro deixa o ciclo de vida da thread como diagrama conceitual — o
`CicloDeVidaDemo` mostra os estados reais via `Thread.getState()`. E, em
vez de rodar cada parte isolada com N diferentes (10 mil na C, 100 mil na
E), o `Comparativo` roda a mesma carga nos 4 modelos e mede tempo e
threads de SO lado a lado, o que deixa o trade-off "rápido vs. custoso em
memória" visível em uma única tabela.

**P: Como o `Comparativo` mede quantas threads de SO cada modelo usa?**
R: Com `ManagementFactory.getThreadMXBean().getThreadCount()`, que
retorna a contagem de threads de **plataforma** (SO) vivas na JVM naquele
instante. É por isso que o cenário de Virtual Threads aparece com um pico
tão baixo (~14): a API não enxerga as milhares de Virtual Threads, só as
poucas carrier threads reais por trás delas — o que é, em si, uma prova de
que Virtual Threads não são threads de SO tradicionais.

**P: Por que o `ExecutorService` fixo foi o mais lento no comparativo, se
ele é a alternativa "profissional" à Parte C?**
R: Porque ele troca velocidade por controle de recursos: com só 200
threads para 2.000 tarefas, o excedente fica enfileirado e é processado em
rodadas (~10 rodadas de 200ms = ~2s). Ele nunca teria o problema de
memória da Parte C, mas também não vai ser o mais rápido quando o número de
tarefas simultâneas passa muito do tamanho do pool — é exatamente essa
limitação que motiva Virtual Threads em cargas muito paralelas e
bloqueantes (como um servidor web com milhares de conexões).
